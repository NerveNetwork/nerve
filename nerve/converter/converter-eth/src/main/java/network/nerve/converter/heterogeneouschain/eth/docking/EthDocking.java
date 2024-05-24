/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package network.nerve.converter.heterogeneouschain.eth.docking;

import io.nuls.base.data.Transaction;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.FormatValidUtils;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.register.interfaces.IHeterogeneousChainRegister;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.eth.callback.EthCallBackManager;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import network.nerve.converter.heterogeneouschain.eth.helper.EthAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.eth.helper.EthCommonHelper;
import network.nerve.converter.heterogeneouschain.eth.helper.EthERC20Helper;
import network.nerve.converter.heterogeneouschain.eth.helper.EthParseTxHelper;
import network.nerve.converter.heterogeneouschain.eth.listener.EthListener;
import network.nerve.converter.heterogeneouschain.eth.model.*;
import network.nerve.converter.heterogeneouschain.eth.storage.*;
import network.nerve.converter.heterogeneouschain.eth.utils.EthUtil;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgUpgradeContractSwitchHelper;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.utils.ConverterUtil;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static io.protostuff.ByteString.EMPTY_STRING;
import static network.nerve.converter.heterogeneouschain.eth.constant.EthConstant.ZERO_ADDRESS;


/**
 * @author: Mimi
 * @date: 2020-02-20
 */
public class EthDocking implements IHeterogeneousChainDocking {

    private static HeterogeneousAssetInfo ethereum;
    private static final EthDocking DOCKING = new EthDocking();
    protected ETHWalletApi ethWalletApi;
    protected EthListener ethListener;
    protected EthERC20Helper ethERC20Helper;
    protected ConverterConfig converterConfig;
    protected EthTxRelationStorageService ethTxRelationStorageService;
    protected EthUnconfirmedTxStorageService ethUnconfirmedTxStorageService;
    protected EthMultiSignAddressHistoryStorageService ethMultiSignAddressHistoryStorageService;
    protected EthTxStorageService ethTxStorageService;
    protected EthAccountStorageService ethAccountStorageService;
    protected EthParseTxHelper ethParseTxHelper;
    protected EthCallBackManager ethCallBackManager;
    protected EthAnalysisTxHelper ethAnalysisTxHelper;
    protected EthCommonHelper ethCommonHelper;
    protected HtgUpgradeContractSwitchHelper ethUpgradeContractSwitchHelper;
    protected ReentrantLock reAnalysisLock = new ReentrantLock();
    private String keystorePath;


    protected EthDocking() {
    }

    private NulsLogger logger() {
        return EthContext.logger();
    }

    public static EthDocking getInstance() {
        return DOCKING;
    }

    @Override
    public int version() {
        return EthConstant.VERSION;
    }

    @Override
    public boolean isSupportContractAssetByCurrentChain() {
        return true;
    }

    @Override
    public Integer getChainId() {
        return EthContext.getConfig().getChainId();
    }

    @Override
    public String getChainSymbol() {
        return EthContext.getConfig().getSymbol();
    }

    @Override
    public String getCurrentSignAddress() {
        return EthContext.ADMIN_ADDRESS;
    }

    @Override
    public String getCurrentMultySignAddress() {
        return EthContext.MULTY_SIGN_ADDRESS;
    }

    @Override
    public String generateAddressByCompressedPublicKey(String compressedPublicKey) {
        return EthUtil.genEthAddressByCompressedPublickey(compressedPublicKey);
    }

    @Override
    public HeterogeneousAccount importAccountByPriKey(String priKey, String password) throws NulsException {
        if (StringUtils.isNotBlank(EthContext.ADMIN_ADDRESS)) {
            EthAccount account = ethAccountStorageService.findByAddress(EthContext.ADMIN_ADDRESS);
            account.decrypt(EthContext.ADMIN_ADDRESS_PASSWORD);
            if (Arrays.equals(account.getPriKey(), HexUtil.decode(priKey))) {
                account.setPriKey(new byte[0]);
                return account;
            }
        }
        if (!FormatValidUtils.validPassword(password)) {
            logger().error("password format wrong");
            throw new NulsException(ConverterErrorCode.PASSWORD_FORMAT_WRONG);
        }
        EthAccount account = EthUtil.createAccount(priKey);
        account.encrypt(password);
        try {
            ethAccountStorageService.save(account);
            // Overwrite this address as the virtual bank administrator address
            EthContext.ADMIN_ADDRESS = account.getAddress();
            EthContext.ADMIN_ADDRESS_PUBLIC_KEY = account.getCompressedPublicKey();
            EthContext.ADMIN_ADDRESS_PASSWORD = password;
            logger().info("towardsETHHeterogeneous component import node block address information, address: [{}]", account.getAddress());
            return account;
        } catch (Exception e) {
            throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR, e);
        }
    }

    @Override
    public boolean validateAccountPassword(String address, String password) throws NulsException {
        if (!FormatValidUtils.validPassword(password)) {
            logger().error("password format wrong");
            throw new NulsException(ConverterErrorCode.PASSWORD_FORMAT_WRONG);
        }
        HeterogeneousAccount account = this.getAccount(address);
        if (account == null) {
            logger().error("account not exist");
            throw new NulsException(ConverterErrorCode.ACCOUNT_NOT_EXIST);
        }
        return account.validatePassword(password);
    }

    @Override
    public HeterogeneousAccount getAccount(String address) {
        return ethAccountStorageService.findByAddress(address);
    }

    @Override
    public boolean validateAddress(String address) {
        return WalletUtils.isValidAddress(address);
    }

    @Override
    public BigDecimal getBalance(String address) {
        BigDecimal ethBalance = null;
        try {
            ethBalance = ethWalletApi.getBalance(address);
        } catch (Exception e) {
            logger().error(e);
        }
        if (ethBalance == null) {
            return BigDecimal.ZERO;
        }
        return ETHWalletApi.convertWeiToEth(ethBalance.toBigInteger());
    }

    @Override
    public void updateMultySignAddress(String multySignAddress) throws Exception {
        logger().info("ETHUpdate multiple contract addresses, old: {}, new: {}", EthContext.MULTY_SIGN_ADDRESS, multySignAddress);
        multySignAddress = multySignAddress.toLowerCase();
        // Listening for multi signature address transactions
        ethListener.removeListeningAddress(EthContext.MULTY_SIGN_ADDRESS);
        ethListener.addListeningAddress(multySignAddress);
        // Update multiple signed addresses
        EthContext.MULTY_SIGN_ADDRESS = multySignAddress;
        // Save the current multi signature address to the multi signature address history list
        ethMultiSignAddressHistoryStorageService.save(multySignAddress);
        // Process switching operation after contract upgrade
        ethUpgradeContractSwitchHelper.switchProcessor(multySignAddress);
    }

    protected void txConfirmedCompleted(String ethTxHash, Long blockHeight) throws Exception {
        if (StringUtils.isBlank(ethTxHash)) {
            logger().warn("Empty ethTxHash warning");
            return;
        }
        // updatedbinpoChange the status of todeleteConfirm in the queue task`ROLLBACK_NUMER`Remove after each block to facilitate state rollback
        EthUnconfirmedTxPo txPo = ethUnconfirmedTxStorageService.findByTxHash(ethTxHash);
        if (txPo == null) {
            txPo = new EthUnconfirmedTxPo();
            txPo.setTxHash(ethTxHash);
        }
        txPo.setDelete(true);
        txPo.setDeletedHeight(blockHeight + EthConstant.ROLLBACK_NUMER);
        logger().info("NerveNetwork impact[{}]ETHtransaction[{}]Confirm completion, nerveheight: {}, nerver hash: {}", txPo.getTxType(), txPo.getTxHash(), blockHeight, txPo.getNerveTxHash());
        boolean delete = txPo.isDelete();
        Long deletedHeight = txPo.getDeletedHeight();
        ethUnconfirmedTxStorageService.update(txPo, update -> {
            update.setDelete(delete);
            update.setDeletedHeight(deletedHeight);
        });
    }

    @Override
    public void txConfirmedCompleted(String ethTxHash, Long blockHeight, String nerveTxHash, byte[] confirmTxRemark) throws Exception {
        logger().info("NerveNetwork confirmationETHtransaction Nerver hash: {}", nerveTxHash);
        this.txConfirmedCompleted(ethTxHash, blockHeight);
    }

    @Override
    public void txConfirmedRollback(String txHash) throws Exception {
        EthUnconfirmedTxPo txPo = ethUnconfirmedTxStorageService.findByTxHash(txHash);
        if (txPo != null) {
            txPo.setDelete(false);
            txPo.setDeletedHeight(null);
            ethUnconfirmedTxStorageService.update(txPo, update -> {
                update.setDelete(txPo.isDelete());
                update.setDeletedHeight(txPo.getDeletedHeight());
            });
        }
    }

    @Override
    public HeterogeneousAssetInfo getMainAsset() {
        return ethereum();
    }

    @Override
    public HeterogeneousAssetInfo getAssetByContractAddress(String contractAddress) {
        EthERC20Po erc20Po = ethERC20Helper.getERC20ByContractAddress(contractAddress);
        if (erc20Po == null) {
            return null;
        }
        HeterogeneousAssetInfo assetInfo = new HeterogeneousAssetInfo();
        assetInfo.setChainId(EthConstant.ETH_CHAIN_ID);
        assetInfo.setAssetId(erc20Po.getAssetId());
        assetInfo.setDecimals((byte) erc20Po.getDecimals());
        assetInfo.setSymbol(erc20Po.getSymbol());
        assetInfo.setContractAddress(erc20Po.getAddress());
        return assetInfo;
    }

    @Override
    public HeterogeneousAssetInfo getAssetByAssetId(int assetId) {
        if (EthConstant.ETH_ASSET_ID == assetId) {
            return this.getMainAsset();
        }
        EthERC20Po erc20Po = ethERC20Helper.getERC20ByAssetId(assetId);
        if (erc20Po == null) {
            return null;
        }
        HeterogeneousAssetInfo assetInfo = new HeterogeneousAssetInfo();
        assetInfo.setChainId(EthConstant.ETH_CHAIN_ID);
        assetInfo.setAssetId(erc20Po.getAssetId());
        assetInfo.setDecimals((byte) erc20Po.getDecimals());
        assetInfo.setSymbol(erc20Po.getSymbol());
        assetInfo.setContractAddress(erc20Po.getAddress());
        return assetInfo;
    }

    private static HeterogeneousAssetInfo ethereum() {
        if (ethereum == null) {
            ethereum = new HeterogeneousAssetInfo();
            ethereum.setChainId(EthConstant.ETH_CHAIN_ID);
            ethereum.setAssetId(EthConstant.ETH_ASSET_ID);
            ethereum.setDecimals((byte) EthConstant.ETH_DECIMALS);
            ethereum.setSymbol(EthConstant.ETH_SYMBOL);
            ethereum.setContractAddress(EMPTY_STRING);
        }
        return ethereum;
    }

    @Override
    public boolean validateHeterogeneousAssetInfoFromNet(String contractAddress, String symbol, int decimals) throws Exception {
        List<Type> symbolResult = ethWalletApi.callViewFunction(contractAddress, EthUtil.getSymbolERC20Function());
        if (symbolResult.isEmpty()) {
            return false;
        }
        String _symbol = symbolResult.get(0).getValue().toString();
        if (!_symbol.equals(symbol)) {
            return false;
        }
        List<Type> decimalsResult = ethWalletApi.callViewFunction(contractAddress, EthUtil.getDecimalsERC20Function());
        if (decimalsResult.isEmpty()) {
            return false;
        }
        int _decimals = Integer.parseInt(decimalsResult.get(0).getValue().toString());
        if (_decimals != decimals) {
            return false;
        }
        return true;
    }

    @Override
    public void saveHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception {
        if (assetInfos == null || assetInfos.isEmpty()) {
            return;
        }
        ethERC20Helper.saveHeterogeneousAssetInfos(assetInfos);
    }

    @Override
    public void rollbackHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception {
        if (assetInfos == null || assetInfos.isEmpty()) {
            return;
        }
        ethERC20Helper.rollbackHeterogeneousAssetInfos(assetInfos);
    }

    @Override
    public HeterogeneousTransactionInfo getDepositTransaction(String txHash) throws Exception {
        // fromDBObtain data from, if unable to obtain, then go toETHObtained from the network
        HeterogeneousTransactionInfo txInfo = ethTxStorageService.findByTxHash(txHash);
        if (txInfo != null && StringUtils.isNotBlank(txInfo.getFrom())) {
            txInfo.setTxType(HeterogeneousChainTxType.DEPOSIT);
        } else {
            txInfo = ethParseTxHelper.parseDepositTransaction(txHash);
            if (txInfo == null) {
                return null;
            }
        }
        if (txInfo.getTxTime() == null) {
            EthBlock.Block block = ethWalletApi.getBlockHeaderByHeight(txInfo.getBlockHeight());
            txInfo.setTxTime(block.getTimestamp().longValue());
        }
        return txInfo;
    }

    @Override
    public HeterogeneousTransactionInfo getWithdrawTransaction(String txHash) throws Exception {
        // fromDBObtain data from, if unable to obtain, then go toETHObtained from the network
        HeterogeneousTransactionInfo txInfo = ethTxStorageService.findByTxHash(txHash);
        if (txInfo != null) {
            txInfo.setTxType(HeterogeneousChainTxType.WITHDRAW);
            Long txTime = txInfo.getTxTime();
            if (StringUtils.isBlank(txInfo.getFrom())) {
                txInfo = ethParseTxHelper.parseWithdrawTransaction(txHash);
                txInfo.setTxTime(txTime);
            }
        } else {
            txInfo = ethParseTxHelper.parseWithdrawTransaction(txHash);
            if (txInfo == null) {
                return null;
            }
        }
        if (txInfo.getTxTime() == null) {
            EthBlock.Block block = ethWalletApi.getBlockHeaderByHeight(txInfo.getBlockHeight());
            txInfo.setTxTime(block.getTimestamp().longValue());
        }
        return txInfo;
    }

    @Override
    public HeterogeneousConfirmedInfo getConfirmedTxInfo(String txHash) throws Exception {
        HeterogeneousConfirmedInfo info = new HeterogeneousConfirmedInfo();
        // fromDBObtain data from, if unable to obtain, then go toETHObtained from the network
        HeterogeneousTransactionInfo txInfo = ethTxStorageService.findByTxHash(txHash);
        Long txTime = null;
        Long blockHeight;
        List<HeterogeneousAddress> signers = null;
        if (txInfo != null) {
            blockHeight = txInfo.getBlockHeight();
            signers = txInfo.getSigners();
            txTime = txInfo.getTxTime();
        } else {
            org.web3j.protocol.core.methods.response.Transaction tx = ethWalletApi.getTransactionByHash(txHash);
            if (tx == null || tx.getBlockNumber() == null) {
                return null;
            }
            blockHeight = tx.getBlockNumber().longValue();
        }
        if(signers == null || signers.isEmpty()) {
            TransactionReceipt txReceipt = ethWalletApi.getTxReceipt(txHash);
            signers = ethParseTxHelper.parseSigners(txReceipt);
        }
        if (txTime == null) {
            EthBlock.Block block = ethWalletApi.getBlockHeaderByHeight(blockHeight);
            if (block == null) {
                return null;
            }
            txTime = block.getTimestamp().longValue();
        }
        info.setMultySignAddress(EthContext.MULTY_SIGN_ADDRESS);
        info.setTxTime(txTime);
        info.setSigners(signers);
        return info;
    }

    @Override
    public HeterogeneousChangePendingInfo getChangeVirtualBankPendingInfo(String nerveTxHash) throws Exception {
        List changeInfo = getPendingManagerChangeInfo(nerveTxHash);
        HeterogeneousChangePendingInfo pendingInfo = new HeterogeneousChangePendingInfo();
        pendingInfo.setNerveTxHash(nerveTxHash);
        pendingInfo.setOrginTxCount(Integer.parseInt(changeInfo.get(0).toString()));
        return pendingInfo;
    }


    public String createOrSignWithdrawTx(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, boolean checkGas) throws NulsException {
        try {
            logger().info("Preparing to send withdrawalETHTransactions,nerveTxHash: {}", nerveTxHash);
            if (checkGas) {
                if (EthContext.getEthGasPrice().compareTo(EthConstant.HIGH_GAS_PRICE) > 0) {
                    if (EthContext.getConverterCoreApi().isWithdrawalComfired(nerveTxHash)) {
                        logger().info("[Withdrawal]transaction[{}]Completed", nerveTxHash);
                        return EMPTY_STRING;
                    }
                    logger().warn("Ethereum network Gas Price Above {} weiSuspend withdrawal", EthConstant.HIGH_GAS_PRICE);
                    throw new NulsException(ConverterErrorCode.HIGH_GAS_PRICE_OF_ETH);
                }
            }
            // Transaction preparation
            EthAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.WITHDRAW);
            if (account.isEmpty()) {
                return EMPTY_STRING;
            }
            // Obtain administrator account
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            // Business validation
            EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            boolean isContractAsset = assetId > 1;
            String contractAddressERC20;
            if (isContractAsset) {
                contractAddressERC20 = ethERC20Helper.getContractAddressByAssetId(assetId);
                ethERC20Helper.loadERC20(contractAddressERC20, po);
            } else {
                contractAddressERC20 = ZERO_ADDRESS;
                po.setDecimals(EthConstant.ETH_DECIMALS);
                po.setAssetId(EthConstant.ETH_ASSET_ID);
            }
            // Convert the address to lowercase
            toAddress = toAddress.toLowerCase();
            po.setTo(toAddress);
            po.setValue(value);
            po.setIfContractAsset(isContractAsset);
            if (isContractAsset) {
                po.setContractAddress(contractAddressERC20);
            }
            // Verify if transaction data is consistent, from the contractviewExtract from function
            List pendingWithdrawInfo = getPendingWithdrawInfo(nerveTxHash);
            if (!pendingWithdrawInfo.isEmpty() && !validatePendingWithdrawInfo(pendingWithdrawInfo, po)) {
                logger().error("Inconsistent withdrawal transaction data, pendingWithdrawInfo: {}, po: {}", Arrays.toString(pendingWithdrawInfo.toArray()), po.toString());
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_DATA_INCONSISTENCY);
            }
            Function createOrSignWithdrawFunction = EthUtil.getCreateOrSignWithdrawFunction(nerveTxHash, toAddress, value, isContractAsset, contractAddressERC20);
            // Send out transactions after verifying the contract
            return this.createTxComplete(nerveTxHash, po, fromAddress, priKey, createOrSignWithdrawFunction, HeterogeneousChainTxType.WITHDRAW);
        } catch (Exception e) {
            logger().error(e);
            if (e instanceof NulsException) {
                throw (NulsException) e;
            }
            throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
        }
    }

    @Override
    public String createOrSignWithdrawTx(String nerveTxHash, String toAddress, BigInteger value, Integer assetId) throws NulsException {
        return this.createOrSignWithdrawTx(nerveTxHash, toAddress, value, assetId, true);
    }

    @Override
    public String createOrSignManagerChangesTx(String nerveTxHash, String[] addAddresses, String[] removeAddresses, int orginTxCount) throws NulsException {
        logger().info("Preparing to send virtual bank changesETHTransactions,nerveTxHash: {}", nerveTxHash);
        try {
            // Business validation
            if (addAddresses == null) {
                addAddresses = new String[0];
            }
            if (removeAddresses == null) {
                removeAddresses = new String[0];
            }
            // Prepare data to verify if transaction data is consistent, starting from the contractviewExtract from function
            EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            po.setAddAddresses(addAddresses);
            po.setRemoveAddresses(removeAddresses);
            po.setOrginTxCount(orginTxCount);
            // Transaction preparation
            EthAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.CHANGE);
            if (account.isEmpty()) {
                // Data consistency check
                this.checkChangeDataInconsistency(nerveTxHash, po, account.getOrder());
                return EMPTY_STRING;
            }
            Set<String> addSet = new HashSet<>();
            List<Address> addList = new ArrayList<>();
            for (int a = 0, addSize = addAddresses.length; a < addSize; a++) {
                String add = addAddresses[a];
                add = add.toLowerCase();
                addAddresses[a] = add;
                if (!addSet.add(add)) {
                    logger().error("Duplicate list of addresses to be added");
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_2);
                }
                addList.add(new Address(add));
            }
            Set<String> removeSet = new HashSet<>();
            List<Address> removeList = new ArrayList<>();
            for (int r = 0, removeSize = removeAddresses.length; r < removeSize; r++) {
                String remove = removeAddresses[r];
                remove = remove.toLowerCase();
                removeAddresses[r] = remove;
                if (!removeSet.add(remove)) {
                    logger().error("Duplicate list of pending exits");
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_4);
                }
                removeList.add(new Address(remove));
            }
            // Data consistency check
            this.checkChangeDataInconsistency(nerveTxHash, po, account.getOrder());
            // If you have not joined or exited, send a confirmation transaction directly
            if (addAddresses.length == 0 && removeAddresses.length == 0) {
                logger().info("Virtual banking changes have not been added or exited, and confirmation transactions have been sent directly, nerveTxHash: {}", nerveTxHash);
                try {
                    ethCallBackManager.getTxConfirmedProcessor().txConfirmed(
                            HeterogeneousChainTxType.CHANGE,
                            nerveTxHash,
                            null, //ethTxHash,
                            null, //ethTx blockHeight,
                            null, //ethTx tx time,
                            EthContext.MULTY_SIGN_ADDRESS,
                            null,  //ethTx signers
                            null
                    );
                } catch (Exception e) {
                    if (e instanceof NulsException) {
                        throw (NulsException) e;
                    }
                    throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
                }
                return EMPTY_STRING;
            }
            // Obtain administrator account
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            if (removeSet.contains(fromAddress)) {
                logger().error("Logged out administrators cannot participate in administrator change transactions");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_6);
            }
            Function createOrSignManagerChangeFunction = EthUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, orginTxCount);
            // Send out transactions after verifying the contract
            return this.createTxComplete(nerveTxHash, po, fromAddress, priKey, createOrSignManagerChangeFunction, HeterogeneousChainTxType.CHANGE);
        } catch (Exception e) {
            logger().error(String.format("adds: %s, removes: %s", addAddresses != null ? Arrays.toString(addAddresses) : "[]", removeAddresses != null ? Arrays.toString(removeAddresses) : "[]"), e);
            if (e instanceof NulsException) {
                throw (NulsException) e;
            }
            throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
        }
    }

    @Override
    public String createOrSignUpgradeTx(String nerveTxHash) throws NulsException {
        logger().info("Preparing to send virtual banking contract upgrade authorization transactions,nerveTxHash: {}", nerveTxHash);
        try {
            // Transaction preparation
            EthAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.UPGRADE);
            if (account.isEmpty()) {
                return EMPTY_STRING;
            }
            // Obtain administrator account
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);

            Function function = EthUtil.getCreateOrSignUpgradeFunction(nerveTxHash);
            // Send out transactions after verifying the contract
            return this.createTxComplete(nerveTxHash, po, fromAddress, priKey, function, HeterogeneousChainTxType.UPGRADE);
        } catch (Exception e) {
            logger().error(e);
            if (e instanceof NulsException) {
                throw (NulsException) e;
            }
            throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
        }
    }

    private boolean checkManagerInconsistency(Set<String> allManagerSet, Set<String> allManagersInContract) {
        if (allManagerSet.size() != allManagersInContract.size()) {
            return false;
        }
        for (String manager : allManagerSet) {
            if (!allManagersInContract.contains(manager)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String forceRecovery(String nerveTxHash, String[] seedManagers, String[] allManagers) throws NulsException {
        try {
            boolean sendRecoveryII = nerveTxHash.startsWith(EthConstant.ETH_RECOVERY_II);
            if (sendRecoveryII) {
                nerveTxHash = nerveTxHash.substring(EthConstant.ETH_RECOVERY_II.length());
            }
            EthRecoveryDto recoveryDto = new EthRecoveryDto(nerveTxHash, seedManagers, allManagers);
            Set<String> allManagersInContract = getAllManagers();
            if (logger().isDebugEnabled()) {
                logger().debug("Recovery mechanism data: allManagers is {}, allManagersInContract is {}", Arrays.toString(allManagers), Arrays.toString(allManagersInContract.toArray()));
            }
            if (sendRecoveryII) {
                return this.recoveryII(recoveryDto, allManagersInContract);
            }
            Set<String> removeSet = new HashSet<>(allManagersInContract);
            for(String seed : seedManagers) {
                removeSet.remove(seed);
            }
            // Remove existing contract administrators
            String[] removes = new String[removeSet.size()];
            if (removeSet.size() != 0) {
                // Perform the first step of recovery
                removeSet.toArray(removes);
                String txHash = this.recoveryI(nerveTxHash, removes, recoveryDto);
                // The first step has been completed
                if (StringUtils.isBlank(txHash)) {
                    // Perform the second step of recovery
                    return this.recoveryII(recoveryDto, allManagersInContract);
                }
                return txHash;
            } else {
                // Perform the second step of recovery
                return this.recoveryII(recoveryDto, allManagersInContract);
            }
        } catch (Exception e) {
            logger().error(e);
            if (e instanceof NulsException) {
                throw (NulsException) e;
            }
            throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
        }
    }

    @Override
    public Boolean reAnalysisDepositTx(String ethTxHash) throws Exception {
        if (ethCommonHelper.constainHash(ethTxHash)) {
            logger().info("Repeated collection of recharge transactionshash: {}No more repeated parsing[0]", ethTxHash);
            return true;
        }
        reAnalysisLock.lock();
        try {
            if (ethCommonHelper.constainHash(ethTxHash)) {
                logger().info("Repeated collection of recharge transactionshash: {}No more repeated parsing[1]", ethTxHash);
                return true;
            }
            logger().info("Re analyze recharge transactions: {}", ethTxHash);
            org.web3j.protocol.core.methods.response.Transaction tx = ethWalletApi.getTransactionByHash(ethTxHash);
            if (tx == null || tx.getBlockNumber() == null) {
                return false;
            }
            HeterogeneousTransactionInfo txInfo = ethParseTxHelper.parseDepositTransaction(tx);
            if (txInfo == null) {
                return false;
            }
            Long blockHeight = tx.getBlockNumber().longValue();
            EthBlock.Block block = ethWalletApi.getBlockHeaderByHeight(blockHeight);
            if (block == null) {
                return false;
            }
            Long txTime = block.getTimestamp().longValue();
            ethAnalysisTxHelper.analysisTx(tx, txTime, blockHeight);
            ethCommonHelper.addHash(ethTxHash);
            return true;
        } catch (Exception e) {
            throw e;
        } finally {
            reAnalysisLock.unlock();
        }
    }

    @Override
    public Boolean reAnalysisTx(String ethTxHash) throws Exception {
        if (ethCommonHelper.constainHash(ethTxHash)) {
            logger().info("Repeated collection of transactionshash: {}No more repeated parsing[0]", ethTxHash);
            return true;
        }
        reAnalysisLock.lock();
        try {
            if (ethCommonHelper.constainHash(ethTxHash)) {
                logger().info("Repeated collection of transactionshash: {}No more repeated parsing[1]", ethTxHash);
                return true;
            }
            logger().info("Re analyze transactions: {}", ethTxHash);
            org.web3j.protocol.core.methods.response.Transaction tx = ethWalletApi.getTransactionByHash(ethTxHash);
            if (tx == null || tx.getBlockNumber() == null) {
                return false;
            }
            Long blockHeight = tx.getBlockNumber().longValue();
            EthBlock.Block block = ethWalletApi.getBlockHeaderByHeight(blockHeight);
            if (block == null) {
                return false;
            }
            Long txTime = block.getTimestamp().longValue();
            ethAnalysisTxHelper.analysisTx(tx, txTime, blockHeight);
            ethCommonHelper.addHash(ethTxHash);
            return true;
        } catch (Exception e) {
            throw e;
        } finally {
            reAnalysisLock.unlock();
        }
    }

    @Override
    public long getHeterogeneousNetworkChainId() {
        return EthContext.getConfig().getChainIdOnHtgNetwork();
    }

    @Override
    public void setRegister(IHeterogeneousChainRegister register) {
        throw new RuntimeException("Unsupport Function");
    }

    @Override
    public void closeChainPending() {
        throw new RuntimeException("Unsupport Function");
    }

    @Override
    public void closeChainConfirm() {
        throw new RuntimeException("Unsupport Function");
    }

    private String recovery(String nerveTxKey, String[] adds, String[] removes, EthRecoveryDto recoveryDto) throws NulsException {
        try {
            // When the current virtual bank triggers the recovery mechanism, persistently save the call parameters to provide for use in the second step of recovery
            ethTxStorageService.saveRecovery(nerveTxKey, recoveryDto);
            // Prepare data to verify if transaction data is consistent, starting from the contractviewExtract from function
            EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
            po.setRecoveryDto(recoveryDto);
            po.setNerveTxHash(nerveTxKey);
            po.setAddAddresses(adds);
            po.setRemoveAddresses(removes);
            po.setOrginTxCount(1);
            // Transaction preparation
            EthAccount account = this.createTxStart(nerveTxKey, HeterogeneousChainTxType.RECOVERY);
            if (account.isEmpty()) {
                // Data consistency check
                this.checkChangeDataInconsistency(nerveTxKey, po, account.getOrder());
                return EMPTY_STRING;
            }
            // Business validation
            if (adds == null) {
                adds = new String[0];
            }
            if (removes == null) {
                removes = new String[0];
            }
            List<Address> addList = new ArrayList<>();
            for (int a = 0, addSize = adds.length; a < addSize; a++) {
                String add = adds[a];
                add = add.toLowerCase();
                addList.add(new Address(add));
            }
            List<Address> removeList = new ArrayList<>();
            for (int r = 0, removeSize = removes.length; r < removeSize; r++) {
                String remove = removes[r];
                remove = remove.toLowerCase();
                removeList.add(new Address(remove));
            }
            // Data consistency check
            this.checkChangeDataInconsistency(nerveTxKey, po, account.getOrder());
            // Obtain administrator account
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            Function createOrSignManagerChangeFunction = EthUtil.getCreateOrSignManagerChangeFunction(nerveTxKey, addList, removeList, 1);
            // Send out transactions after verifying the contract
            return this.createTxComplete(nerveTxKey, po, fromAddress, priKey, createOrSignManagerChangeFunction, HeterogeneousChainTxType.RECOVERY);
        } catch (Exception e) {
            logger().error(String.format("removes: %s", Arrays.toString(removes)), e);
            if (e instanceof NulsException) {
                throw (NulsException) e;
            }
            throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
        }
    }

    private String recoveryI(String nerveTxHash, String[] removes, EthRecoveryDto recoveryDto) throws NulsException {
        return this.recovery(EthConstant.ETH_RECOVERY_I + nerveTxHash, null, removes, recoveryDto);
    }

    private String recoveryII(EthRecoveryDto recoveryDto, Set<String> allManagersInContract) throws NulsException {
        // Add Currentnerveadministrators
        Set<String> allManagerSet = new HashSet<>(Arrays.asList(recoveryDto.getAllManagers()));
        // Check if the administrator in the contract is compatible with thenerveAdministrator consensus
        if (checkManagerInconsistency(allManagerSet, allManagersInContract)) {
            if (checkManagerInconsistency(new HashSet<>(Arrays.asList(recoveryDto.getSeedManagers())), allManagersInContract)) {
                logger().info("[confirm]There are only seed administrators in the network, and the second step of recovery will no longer be sent. Confirmation will be initiated to reset the virtual bank heterogeneous chain(contract)");
                try {
                    String nerveTxHash = recoveryDto.getRealNerveTxHash();
                    Transaction nerveTx = EthContext.getConverterCoreApi().getNerveTx(nerveTxHash);
                    ethCallBackManager.getTxConfirmedProcessor().txConfirmed(
                            HeterogeneousChainTxType.RECOVERY,
                            nerveTxHash,
                            null, //ethTxHash,
                            null, //txPo.getBlockHeight(),
                            nerveTx.getTime(), //txPo.getTxTime(),
                            EthContext.MULTY_SIGN_ADDRESS,
                            null,  //txPo.getSigners()
                            null
                    );
                } catch (Exception e) {
                    logger().error(e);
                    if (e instanceof NulsException) {
                        throw (NulsException) e;
                    }
                    throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
                }
                return null;
            }
            logger().info("[finish]The contract has been restored, administrator");
            return null;
        }
        for(String seed : recoveryDto.getSeedManagers()) {
            allManagerSet.remove(seed);
        }
        String[] adds = new String[allManagerSet.size()];
        allManagerSet.toArray(adds);
        return this.recovery(EthConstant.ETH_RECOVERY_II + recoveryDto.getRealNerveTxHash(), adds, null, recoveryDto);
    }

    private void checkChangeDataInconsistency(String nerveTxHash, EthUnconfirmedTxPo po, int order) throws Exception {
        // fromETHObtain change data for current transactions online
        List pendingManagerChangeInfo = getPendingManagerChangeInfo(nerveTxHash);
        if (pendingManagerChangeInfo.isEmpty() && order > 1) {
            // When the order of transactions is not in the first place and no query is foundETHThe change data of the current transaction on the network indicates that the node that initiated the transaction first is slightly slower than the node in the last order bit. Therefore, the node in the last order bit will wait for an interval period to obtain it again
            if (logger().isDebugEnabled()) {
                logger().debug("Non leading node did not receive change data, waiting{}Retrieve in seconds", EthContext.getIntervalWaitting());
            }
            TimeUnit.SECONDS.sleep(EthContext.getIntervalWaitting());
            pendingManagerChangeInfo = getPendingManagerChangeInfo(nerveTxHash);
        }
        if (logger().isDebugEnabled()) {
            logger().debug("Consistency check preparation data, change from contract: {}, sending tx po: [count:{},hash:{},adds:{},removes:{}]",
                    Arrays.toString(pendingManagerChangeInfo.toArray()),
                    po.getOrginTxCount(),
                    nerveTxHash,
                    Arrays.toString(po.getAddAddresses()),
                    Arrays.toString(po.getRemoveAddresses())
            );
        }
        // Check consistency
        if (!pendingManagerChangeInfo.isEmpty() && !validatePendingManagerChangeInfo(pendingManagerChangeInfo, po)) {
            logger().error("Inconsistent transaction data changed by administrator, change from contract: {}, sending tx po: [count:{},hash:{},adds:{},removes:{}]",
                    Arrays.toString(pendingManagerChangeInfo.toArray()),
                    po.getOrginTxCount(),
                    nerveTxHash,
                    Arrays.toString(po.getAddAddresses()),
                    Arrays.toString(po.getRemoveAddresses())
            );
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_DATA_INCONSISTENCY);
        }
    }

    private EthAccount createTxStart(String nerveTxHash, HeterogeneousChainTxType txType) throws Exception {
        // Local initiated transactions, savingnervetransactionhash, indicates that this transaction has been sent out at this node and may not have been successfully sent outethTransaction, only represents being triggered
        ethTxRelationStorageService.saveNerveTxHash(nerveTxHash);
        String realNerveTxHash = nerveTxHash;
        if (nerveTxHash.startsWith(EthConstant.ETH_RECOVERY_I) || nerveTxHash.startsWith(EthConstant.ETH_RECOVERY_II)) {
            realNerveTxHash = nerveTxHash.substring(EthConstant.ETH_RECOVERY_I.length());
        }
        // according tonervetransactionhashCalculate the sequential seed for the first two digits
        int seed = new BigInteger(realNerveTxHash.substring(0, 1), 16).intValue() + 1;
        int bankSize = EthContext.getConverterCoreApi().getVirtualBankSize();
        if (bankSize > 16) {
            seed += new BigInteger(realNerveTxHash.substring(1, 2), 16).intValue() + 1;
        }
        int mod = seed % bankSize + 1;
        // Wait for a fixed time in sequence before sending outETHtransaction
        int bankOrder = EthContext.getConverterCoreApi().getVirtualBankOrder();
        if (bankOrder < mod) {
            bankOrder += bankSize - (mod - 1);
        } else {
            bankOrder -= mod - 1;
        }
        int waitting = (bankOrder - 1) * EthContext.getIntervalWaitting();
        if (logger().isDebugEnabled()) {
            logger().debug("Sequential calculation parameters bankSize: {}, seed: {}, mod: {}, orginBankOrder: {}, bankOrder: {}, waitting: {}",
                    bankSize,
                    seed,
                    mod,
                    EthContext.getConverterCoreApi().getVirtualBankOrder(),
                    bankOrder,
                    waitting);
        }
        logger().info("wait for{}Send out in seconds[{}]ofETHtransaction, nerveTxHash: {}", waitting, txType, nerveTxHash);
        TimeUnit.SECONDS.sleep(waitting);
        // towardsETHNetwork request verification
        boolean isCompleted = ethParseTxHelper.isCompletedTransaction(nerveTxHash);
        if (isCompleted) {
            logger().info("[{}]transaction[{}]Completed", txType, nerveTxHash);
            return EthAccount.newEmptyAccount(bankOrder);
        }
        // Obtain administrator account
        EthAccount account = (EthAccount) this.getAccount(EthContext.ADMIN_ADDRESS);
        account.decrypt(EthContext.ADMIN_ADDRESS_PASSWORD);
        account.setOrder(bankOrder);
        return account;
    }

    private String createTxComplete(String nerveTxHash, EthUnconfirmedTxPo po, String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType) throws Exception {
        // Verify the legality of contract transactions
        EthCall ethCall = ethWalletApi.validateContractCall(fromAddress, EthContext.MULTY_SIGN_ADDRESS, txFunction);
        if (ethCall.isReverted()) {
            // When the verification is a duplicate signature, it indicates that the current node has signed the transaction, and returns EMPTY_STRING Indicates that it will no longer be executed
            if (ConverterUtil.isDuplicateSignature(ethCall.getRevertReason())) {
                logger().info("[{}]The current node has signed this transaction and will no longer execute it, nerveTxHash: {}", txType, nerveTxHash);
                return EMPTY_STRING;
            }
            logger().error("[{}]Transaction verification failed, reason: {}", txType, ethCall.getRevertReason());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, ethCall.getRevertReason());
        }
        // estimateGasLimit
        BigInteger estimateGas = ethWalletApi.ethEstimateGas(fromAddress, EthContext.MULTY_SIGN_ADDRESS, txFunction);
        if (logger().isDebugEnabled()) {
            logger().debug("Transaction type: {}, EstimatedGasLimit: {}", txType, estimateGas);
        }
        if (estimateGas.compareTo(BigInteger.ZERO) == 0) {
            logger().error("[{}]Transaction verification failed, reason: estimateGasLimitfail", txType);
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, "estimateGasLimitfail");
        }
        BigInteger hardGasLimit = EthConstant.ETH_GAS_LIMIT_OF_MULTY_SIGN;
        if (txType == HeterogeneousChainTxType.CHANGE) {
            hardGasLimit = EthConstant.ETH_GAS_LIMIT_OF_MULTY_SIGN_CHANGE;
        } else if (txType == HeterogeneousChainTxType.RECOVERY) {
            hardGasLimit = EthConstant.ETH_GAS_LIMIT_OF_MULTY_SIGN_RECOVERY;
        }
        BigInteger gasLimit = hardGasLimit.compareTo(estimateGas) > 0 ? hardGasLimit : estimateGas;
        if (txType == HeterogeneousChainTxType.CHANGE) {
            gasLimit = gasLimit.compareTo(EthConstant.ETH_GAS_LIMIT_OF_MULTY_SIGN_CHANGE_THRESHOLD) >= 0 ? EthConstant.ETH_GAS_LIMIT_OF_MULTY_SIGN_CHANGE_MAX : gasLimit;
        }
        EthSendTransactionPo ethSendTransactionPo = ethWalletApi.callContract(fromAddress, priKey, EthContext.MULTY_SIGN_ADDRESS, gasLimit, txFunction);
        String ethTxHash = ethSendTransactionPo.getTxHash();
        // dockinglaunchethRecord transaction relationships during transactionsdbIn, and save the currently usednonceIn the relationship table, if there is any reasonpriceIf there is a need to resend the transaction without packaging it too low, the current one used will be taken outnonceResend transaction
        ethTxRelationStorageService.save(ethTxHash, nerveTxHash, ethSendTransactionPo);

        // Save unconfirmed transactions
        po.setTxHash(ethTxHash);
        po.setFrom(fromAddress);
        po.setTxType(txType);
        ethUnconfirmedTxStorageService.save(po);
        EthContext.UNCONFIRMED_TX_QUEUE.offer(po);
        // Monitor the packaging status of this transaction
        ethListener.addListeningTx(ethTxHash);
        logger().info("NerveNetwork orientedETHNetwork transmission[{}]transaction, nerveTxHash: {}, details: {}", txType, nerveTxHash, po.toString());
        return ethTxHash;
    }

    private boolean validatePendingWithdrawInfo(List pendingWithdrawInfo, EthUnconfirmedTxPo po) {
        int i = 0;
        String to = pendingWithdrawInfo.get(i++).toString();
        BigInteger value = new BigInteger(pendingWithdrawInfo.get(i++).toString());
        Boolean isERC20 = Boolean.parseBoolean(pendingWithdrawInfo.get(i++).toString());
        String erc20 = pendingWithdrawInfo.get(i++).toString();
        if (!to.equals(po.getTo())) {
            return false;
        }
        if (value.compareTo(po.getValue()) != 0) {
            return false;
        }
        if (!isERC20.equals(po.isIfContractAsset())) {
            return false;
        }
        String contractAddress = po.getContractAddress();
        if (StringUtils.isBlank(contractAddress) || "null".equals(contractAddress)) {
            contractAddress = ZERO_ADDRESS;
        }
        if (!erc20.equals(contractAddress)) {
            return false;
        }
        return true;
    }

    private List getPendingWithdrawInfo(String nerveTxHash) throws Exception {
        Function pendingWithdrawTransactionFunction = EthUtil.getPendingWithdrawTransactionFunction(nerveTxHash);
        List<Type> valueTypes = ethWalletApi.callViewFunction(EthContext.MULTY_SIGN_ADDRESS, pendingWithdrawTransactionFunction);
        List<Object> list = valueTypes.stream().map(type -> type.getValue()).collect(Collectors.toList());
        if (!list.isEmpty() && Integer.parseInt(list.get(list.size() - 1).toString()) == 0) {
            return Collections.emptyList();
        }
        return list;
    }

    private boolean validatePendingManagerChangeInfo(List pendingManagerChangeInfo, EthUnconfirmedTxPo po) {
        String nerveTxHash = po.getNerveTxHash();
        int i = 0;
        int orginTxCount = Integer.parseInt(pendingManagerChangeInfo.get(i++).toString());
        if (po.getOrginTxCount() != orginTxCount) {
            return false;
        }
        String key = (String) pendingManagerChangeInfo.get(i++);
        if (!nerveTxHash.equals(key)) {
            return false;
        }
        // Check Join List
        int poAddSize;
        String[] addAddresses = po.getAddAddresses();
        if (addAddresses != null) {
            poAddSize = addAddresses.length;
        } else {
            poAddSize = 0;
        }
        List<Address> adds = (List<Address>) pendingManagerChangeInfo.get(i++);
        if (adds.size() != poAddSize) {
            return false;
        }
        if (poAddSize != 0) {
            Set<String> poAddSet = Arrays.asList(addAddresses).stream().collect(Collectors.toSet());
            Set<String> addSet = adds.stream().map(address -> address.getValue()).collect(Collectors.toSet());
            for (String add : addSet) {
                if (!poAddSet.contains(add)) {
                    return false;
                }
            }
        }
        // Check exit list
        int poRemoveSize;
        String[] removeAddresses = po.getRemoveAddresses();
        if (removeAddresses != null) {
            poRemoveSize = removeAddresses.length;
        } else {
            poRemoveSize = 0;
        }
        List<Address> removes = (List<Address>) pendingManagerChangeInfo.get(i++);
        if (removes.size() != poRemoveSize) {
            return false;
        }
        if (poRemoveSize != 0) {
            Set<String> poRemoveSet = Arrays.asList(removeAddresses).stream().collect(Collectors.toSet());
            Set<String> removeSet = removes.stream().map(address -> address.getValue()).collect(Collectors.toSet());
            for (String remove : removeSet) {
                if (!poRemoveSet.contains(remove)) {
                    return false;
                }
            }
        }
        return true;
    }

    private List getPendingManagerChangeInfo(String nerveTxHash) throws Exception {
        Function pendingManagerChangeTransactionFunction = EthUtil.getPendingManagerChangeTransactionFunction(nerveTxHash);
        List<Type> valueTypes = ethWalletApi.callViewFunction(EthContext.MULTY_SIGN_ADDRESS, pendingManagerChangeTransactionFunction);
        List<Object> list = valueTypes.stream().map(type -> type.getValue()).collect(Collectors.toList());
        // When querying change transactions from the contract, the merged originalnerveTransaction quantitytxCount、Number of multiple signatures within the contractsignedCountAll of them are0When, it is a non-existent transaction, that is, returning the empty set
        if (!list.isEmpty() && Integer.parseInt(list.get(0).toString()) == 0 && Integer.parseInt(list.get(list.size() - 1).toString()) == 0) {
            return Collections.emptyList();
        }
        return list;
    }

    private Set<String> getAllManagers() throws Exception {
        Function allManagersFunction = EthUtil.getAllManagersFunction();
        List<Type> typeList = ethWalletApi.callViewFunction(EthContext.MULTY_SIGN_ADDRESS, allManagersFunction);
        List<String> results = new ArrayList();
        for(Type type : typeList) {
            results.add(type.getValue().toString());
        }
        String resultStr = results.get(0).substring(1, results.get(0).length() - 1);
        String[] resultArr = resultStr.split(",");
        Set<String> resultList = new HashSet<>();
        for(String result : resultArr) {
            resultList.add(result.trim().toLowerCase());
        }
        return resultList;
    }

    private String getKeystorePath() {
        if (StringUtils.isBlank(keystorePath)) {
            keystorePath = converterConfig.getDataPath() + File.separator + "keystore" + File.separator + "backup" + File.separator + "eth";
        }
        return keystorePath;
    }

    public void setConverterConfig(ConverterConfig converterConfig) {
        this.converterConfig = converterConfig;
    }

    public void setEthTxRelationStorageService(EthTxRelationStorageService ethTxRelationStorageService) {
        this.ethTxRelationStorageService = ethTxRelationStorageService;
    }

    public void setEthWalletApi(ETHWalletApi ethWalletApi) {
        this.ethWalletApi = ethWalletApi;
    }

    public void setEthUnconfirmedTxStorageService(EthUnconfirmedTxStorageService ethUnconfirmedTxStorageService) {
        this.ethUnconfirmedTxStorageService = ethUnconfirmedTxStorageService;
    }

    public void setEthListener(EthListener ethListener) {
        this.ethListener = ethListener;
    }

    public void setEthAccountStorageService(EthAccountStorageService ethAccountStorageService) {
        this.ethAccountStorageService = ethAccountStorageService;
    }

    public void setEthERC20Helper(EthERC20Helper ethERC20Helper) {
        this.ethERC20Helper = ethERC20Helper;
    }

    public void setEthTxStorageService(EthTxStorageService ethTxStorageService) {
        this.ethTxStorageService = ethTxStorageService;
    }

    public void setEthParseTxHelper(EthParseTxHelper ethParseTxHelper) {
        this.ethParseTxHelper = ethParseTxHelper;
    }

    public EthCallBackManager getEthCallBackManager() {
        return ethCallBackManager;
    }

    public void setEthCallBackManager(EthCallBackManager ethCallBackManager) {
        this.ethCallBackManager = ethCallBackManager;
    }

    public void setEthMultiSignAddressHistoryStorageService(EthMultiSignAddressHistoryStorageService ethMultiSignAddressHistoryStorageService) {
        this.ethMultiSignAddressHistoryStorageService = ethMultiSignAddressHistoryStorageService;
    }

    public void setEthAnalysisTxHelper(EthAnalysisTxHelper ethAnalysisTxHelper) {
        this.ethAnalysisTxHelper = ethAnalysisTxHelper;
    }

    public void setEthUpgradeContractSwitchHelper(HtgUpgradeContractSwitchHelper ethUpgradeContractSwitchHelper) {
        this.ethUpgradeContractSwitchHelper = ethUpgradeContractSwitchHelper;
    }

    public void setEthCommonHelper(EthCommonHelper ethCommonHelper) {
        this.ethCommonHelper = ethCommonHelper;
    }
}

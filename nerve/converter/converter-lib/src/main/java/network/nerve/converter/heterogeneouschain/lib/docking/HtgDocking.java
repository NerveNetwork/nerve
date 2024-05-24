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
package network.nerve.converter.heterogeneouschain.lib.docking;

import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.FormatValidUtils;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.register.interfaces.IHeterogeneousChainRegister;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.lib.callback.HtgCallBackManager;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.helper.*;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.*;
import network.nerve.converter.heterogeneouschain.lib.storage.*;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.HeterogeneousSign;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.utils.ConverterUtil;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static io.protostuff.ByteString.EMPTY_STRING;
import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.ZERO_BYTES;


/**
 * @author: Mimi
 * @date: 2020-08-28
 */
public class HtgDocking implements IHeterogeneousChainDocking, BeanInitial {

    private HeterogeneousAssetInfo mainAsset;

    protected HtgWalletApi htgWalletApi;
    protected HtgListener htgListener;
    protected HtgERC20Helper htgERC20Helper;
    protected ConverterConfig converterConfig;
    protected HtgTxRelationStorageService htgTxRelationStorageService;
    protected HtgUnconfirmedTxStorageService htgUnconfirmedTxStorageService;
    protected HtgMultiSignAddressHistoryStorageService htgMultiSignAddressHistoryStorageService;
    protected HtgTxStorageService htgTxStorageService;
    protected HtgAccountStorageService htgAccountStorageService;
    protected HtgCommonHelper htgCommonHelper;
    protected HtgUpgradeContractSwitchHelper htgUpgradeContractSwitchHelper;
    protected ReentrantLock reAnalysisLock = new ReentrantLock();
    private String keystorePath;

    protected HtgCallBackManager htgCallBackManager;
    protected HtgInvokeTxHelper htgInvokeTxHelper;
    protected HtgParseTxHelper htgParseTxHelper;
    protected HtgAnalysisTxHelper htgAnalysisTxHelper;
    protected HtgResendHelper htgResendHelper;
    protected HtgPendingTxHelper htgPendingTxHelper;
    private HtgContext htgContext;
    private HeterogeneousChainGasInfo gasInfo;
    protected boolean closePending = false;
    protected IHeterogeneousChainRegister register;

    private NulsLogger logger() {
        return htgContext.logger();
    }

    @Override
    public int version() {
        return htgContext.VERSION();
    }

    @Override
    public boolean isSupportContractAssetByCurrentChain() {
        return true;
    }

    @Override
    public Integer getChainId() {
        return htgContext.getConfig().getChainId();
    }

    @Override
    public String getChainSymbol() {
        return htgContext.getConfig().getSymbol();
    }

    @Override
    public String getCurrentSignAddress() {
        return htgContext.ADMIN_ADDRESS();
    }

    @Override
    public String getCurrentMultySignAddress() {
        return htgContext.MULTY_SIGN_ADDRESS();
    }

    @Override
    public String generateAddressByCompressedPublicKey(String compressedPublicKey) {
        return HtgUtil.genEthAddressByCompressedPublickey(compressedPublicKey);
    }

    @Override
    public HeterogeneousAccount importAccountByPriKey(String priKey, String password) throws NulsException {
        if (htgContext.getConverterCoreApi().isLocalSign()) {
            return _importAccountByPriKey(priKey, password);
        } else {
            return _importAccountByPubKey(priKey, password);
        }
    }

    private HeterogeneousAccount _importAccountByPriKey(String priKey, String password) throws NulsException {
        if (StringUtils.isNotBlank(htgContext.ADMIN_ADDRESS())) {
            HtgAccount account = htgAccountStorageService.findByAddress(htgContext.ADMIN_ADDRESS());
            account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
            if (Arrays.equals(account.getPriKey(), HexUtil.decode(priKey))) {
                account.setPriKey(new byte[0]);
                return account;
            }
        }
        if (!FormatValidUtils.validPassword(password)) {
            logger().error("password format wrong");
            throw new NulsException(ConverterErrorCode.PASSWORD_FORMAT_WRONG);
        }
        HtgAccount account = HtgUtil.createAccount(priKey);
        account.encrypt(password);
        try {
            htgAccountStorageService.save(account);
            // Overwrite this address as the virtual bank administrator address
            htgContext.SET_ADMIN_ADDRESS(account.getAddress());
            htgContext.SET_ADMIN_ADDRESS_PUBLIC_KEY(account.getCompressedPublicKey());
            htgContext.SET_ADMIN_ADDRESS_PASSWORD(password);
            logger().info("towards {} Heterogeneous component import node block address information, address: [{}]", htgContext.getConfig().getSymbol(), account.getAddress());
            return account;
        } catch (Exception e) {
            throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR, e);
        }
    }

    private HeterogeneousAccount _importAccountByPubKey(String pubKey, String password) throws NulsException {
        if (StringUtils.isNotBlank(htgContext.ADMIN_ADDRESS())) {
            HtgAccount account = htgAccountStorageService.findByAddress(htgContext.ADMIN_ADDRESS());
            if (Arrays.equals(account.getPubKey(), HexUtil.decode(pubKey))) {
                account.setPriKey(ZERO_BYTES);
                account.setEncryptedPriKey(ZERO_BYTES);
                return account;
            }
        }
        if (!FormatValidUtils.validPassword(password)) {
            logger().error("password format wrong");
            throw new NulsException(ConverterErrorCode.PASSWORD_FORMAT_WRONG);
        }
        HtgAccount account = HtgUtil.createAccountByPubkey(pubKey);
        try {
            htgAccountStorageService.save(account);
            // Overwrite this address as the virtual bank administrator address
            htgContext.SET_ADMIN_ADDRESS(account.getAddress());
            htgContext.SET_ADMIN_ADDRESS_PUBLIC_KEY(account.getCompressedPublicKey());
            htgContext.SET_ADMIN_ADDRESS_PASSWORD(password);
            logger().info("towards {} Heterogeneous component import node block address information, address: [{}]", htgContext.getConfig().getSymbol(), account.getAddress());
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
        return htgAccountStorageService.findByAddress(address);
    }

    @Override
    public boolean validateAddress(String address) {
        return WalletUtils.isValidAddress(address);
    }

    @Override
    public BigDecimal getBalance(String address) {
        BigDecimal ethBalance = null;
        try {
            ethBalance = htgWalletApi.getBalance(address);
        } catch (Exception e) {
            logger().error(e);
        }
        if (ethBalance == null) {
            return BigDecimal.ZERO;
        }
        return HtgWalletApi.convertWeiToMainAsset(ethBalance.toBigInteger());
    }

    @Override
    public void updateMultySignAddress(String multySignAddress) throws Exception {
        logger().info("{}Update multiple contract addresses, old: {}, new: {}", htgContext.getConfig().getSymbol(), htgContext.MULTY_SIGN_ADDRESS(), multySignAddress);
        multySignAddress = multySignAddress.toLowerCase();
        // Listening for multi signature address transactions
        htgListener.removeListeningAddress(htgContext.MULTY_SIGN_ADDRESS());
        htgListener.addListeningAddress(multySignAddress);
        // Update multiple signed addresses
        htgContext.SET_MULTY_SIGN_ADDRESS(multySignAddress);
        // Save the current multi signature address to the multi signature address history list
        htgMultiSignAddressHistoryStorageService.save(multySignAddress);
        // Process switching operation after contract upgrade
        htgUpgradeContractSwitchHelper.switchProcessor(multySignAddress);
    }

    @Override
    public void updateMultySignAddressProtocol16(String multySignAddress, byte version) throws Exception {
        updateMultySignAddress(multySignAddress);
        htgContext.SET_VERSION(version);
        htgMultiSignAddressHistoryStorageService.saveVersion(version);
        logger().info("Update signature version number: {}", version);
    }

    @Override
    public void txConfirmedRollback(String txHash) throws Exception {
        HtgUnconfirmedTxPo txPo = htgUnconfirmedTxStorageService.findByTxHash(txHash);
        if (txPo != null) {
            txPo.setDelete(false);
            txPo.setDeletedHeight(null);
            htgUnconfirmedTxStorageService.update(txPo, update -> {
                update.setDelete(txPo.isDelete());
                update.setDeletedHeight(txPo.getDeletedHeight());
            });
        }
    }

    @Override
    public HeterogeneousAssetInfo getMainAsset() {
        return mainAsset();
    }

    @Override
    public HeterogeneousAssetInfo getAssetByContractAddress(String contractAddress) {
        if (StringUtils.isBlank(contractAddress)) {
            return mainAsset();
        }
        HtgERC20Po erc20Po = htgERC20Helper.getERC20ByContractAddress(contractAddress);
        if (erc20Po == null) {
            return null;
        }
        HeterogeneousAssetInfo assetInfo = new HeterogeneousAssetInfo();
        assetInfo.setChainId(htgContext.getConfig().getChainId());
        assetInfo.setAssetId(erc20Po.getAssetId());
        assetInfo.setDecimals((byte) erc20Po.getDecimals());
        assetInfo.setSymbol(erc20Po.getSymbol());
        assetInfo.setContractAddress(erc20Po.getAddress());
        return assetInfo;
    }

    @Override
    public HeterogeneousAssetInfo getAssetByAssetId(int assetId) {
        if (htgContext.HTG_ASSET_ID() == assetId) {
            return this.getMainAsset();
        }
        HtgERC20Po erc20Po = htgERC20Helper.getERC20ByAssetId(assetId);
        if (erc20Po == null) {
            return null;
        }
        HeterogeneousAssetInfo assetInfo = new HeterogeneousAssetInfo();
        assetInfo.setChainId(htgContext.getConfig().getChainId());
        assetInfo.setAssetId(erc20Po.getAssetId());
        assetInfo.setDecimals((byte) erc20Po.getDecimals());
        assetInfo.setSymbol(erc20Po.getSymbol());
        assetInfo.setContractAddress(erc20Po.getAddress());
        return assetInfo;
    }

    private HeterogeneousAssetInfo mainAsset() {
        if (mainAsset == null) {
            mainAsset = new HeterogeneousAssetInfo();
            mainAsset.setChainId(htgContext.getConfig().getChainId());
            mainAsset.setAssetId(htgContext.HTG_ASSET_ID());
            mainAsset.setDecimals((byte) htgContext.getConfig().getDecimals());
            mainAsset.setSymbol(htgContext.getConfig().getSymbol());
            mainAsset.setContractAddress(EMPTY_STRING);
        }
        return mainAsset;
    }

    @Override
    public boolean validateHeterogeneousAssetInfoFromNet(String contractAddress, String symbol, int decimals) throws Exception {
        //if (!htgContext.getConverterCoreApi().isProtocol35()) {
            List<Type> symbolResult = htgWalletApi.callViewFunction(contractAddress, HtgUtil.getSymbolERC20Function());
            if (symbolResult.isEmpty()) {
                return false;
            }
            String _symbol = symbolResult.get(0).getValue().toString();
            if (!_symbol.equals(symbol)) {
                return false;
            }
        //}
        List<Type> decimalsResult = htgWalletApi.callViewFunction(contractAddress, HtgUtil.getDecimalsERC20Function());
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
        htgERC20Helper.saveHeterogeneousAssetInfos(assetInfos);
    }

    @Override
    public void rollbackHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception {
        if (assetInfos == null || assetInfos.isEmpty()) {
            return;
        }
        htgERC20Helper.rollbackHeterogeneousAssetInfos(assetInfos);
    }

    @Override
    public void txConfirmedCompleted(String htTxHash, Long blockHeight, String nerveTxHash, byte[] confirmTxRemark) throws Exception {
        logger().info("NerveNetwork confirmation {} transaction Nerver hash: {}", htgContext.getConfig().getSymbol(), nerveTxHash);
        if (StringUtils.isBlank(htTxHash)) {
            logger().warn("Empty htgTxHash warning");
            return;
        }
        // updatedbinpoChange the status of todeleteConfirm in the queue task`ROLLBACK_NUMER`Remove after each block to facilitate state rollback
        HtgUnconfirmedTxPo txPo = htgUnconfirmedTxStorageService.findByTxHash(htTxHash);
        if (txPo == null) {
            txPo = new HtgUnconfirmedTxPo();
            txPo.setTxHash(htTxHash);
        }
        txPo.setDelete(true);
        txPo.setDeletedHeight(blockHeight + HtgConstant.ROLLBACK_NUMER);
        logger().info("NerveNetwork impact [{}] {} transaction [{}] Confirm completion, nerve height: {}, nerver hash: {}", txPo.getTxType(), htgContext.getConfig().getSymbol(), txPo.getTxHash(), blockHeight, txPo.getNerveTxHash());
        boolean delete = txPo.isDelete();
        Long deletedHeight = txPo.getDeletedHeight();
        htgUnconfirmedTxStorageService.update(txPo, update -> {
            update.setDelete(delete);
            update.setDeletedHeight(deletedHeight);
        });
        // Persisted state successfullynerveTx
        if (StringUtils.isNotBlank(nerveTxHash)) {
            logger().debug("Persisted state successfully nerveTxHash: {}", nerveTxHash);
            htgInvokeTxHelper.saveSuccessfulNerve(nerveTxHash);
        }
    }

    @Override
    public HeterogeneousTransactionInfo getDepositTransaction(String txHash) throws Exception {
        // v15After upgrading the wavefield protocol, transactions can be directly obtained from heterogeneous chains to prevent inconsistency between transaction information caused by network block rollback in heterogeneous chains and locally saved transaction information
        if (htgContext.getConverterCoreApi().isSupportProtocol15TrxCrossChain()) {
            HeterogeneousTransactionInfo txInfo = htgParseTxHelper.parseDepositTransaction(txHash);
            if (txInfo == null) {
                return null;
            }
            if (txInfo.getTxTime() == null) {
                EthBlock.Block block = htgWalletApi.getBlockHeaderByHeight(txInfo.getBlockHeight());
                txInfo.setTxTime(block.getTimestamp().longValue());
            }
            return txInfo;
        }
        // fromDBObtain data from, if unable to obtain, then go toHTGObtained from the network
        HeterogeneousTransactionInfo txInfo = htgTxStorageService.findByTxHash(txHash);
        if (txInfo != null) {
            txInfo.setTxType(HeterogeneousChainTxType.DEPOSIT);
        } else {
            txInfo = htgParseTxHelper.parseDepositTransaction(txHash);
            if (txInfo == null) {
                return null;
            }
        }
        if (txInfo.getTxTime() == null) {
            EthBlock.Block block = htgWalletApi.getBlockHeaderByHeight(txInfo.getBlockHeight());
            txInfo.setTxTime(block.getTimestamp().longValue());
        }
        return txInfo;
    }

    @Override
    public HeterogeneousTransactionInfo getWithdrawTransaction(String txHash) throws Exception {
        // v15After upgrading the wavefield protocol, transactions can be directly obtained from heterogeneous chains to prevent inconsistency between transaction information caused by network block rollback in heterogeneous chains and locally saved transaction information
        if (htgContext.getConverterCoreApi().isSupportProtocol15TrxCrossChain()) {
            HeterogeneousTransactionInfo txInfo = htgParseTxHelper.parseWithdrawTransaction(txHash);
            if (txInfo == null) {
                return null;
            }
            if (txInfo.getTxTime() == null) {
                EthBlock.Block block = htgWalletApi.getBlockHeaderByHeight(txInfo.getBlockHeight());
                txInfo.setTxTime(block.getTimestamp().longValue());
            }
            return txInfo;
        }
        // fromDBObtain data from, if unable to obtain, then go toHTGObtained from the network
        HeterogeneousTransactionInfo txInfo = htgTxStorageService.findByTxHash(txHash);
        // Historical legacy issues, directly fromhecoSearching transactions online during assemblytxInfo
        String problemHash = "0xb9d192fd11d822d7bd8ce7cedb91c2ee1dc6b64b3db6efe21cbe090ce37f704e";
        // v14Handling legacy issues after protocol upgrade
        if (htgContext.getConverterCoreApi().isProtocol14() && txHash.equals(problemHash)) {
            txInfo = null;
        }
        if (txInfo != null) {
            txInfo.setTxType(HeterogeneousChainTxType.WITHDRAW);
            Long txTime = txInfo.getTxTime();
            if (StringUtils.isBlank(txInfo.getFrom())) {
                txInfo = htgParseTxHelper.parseWithdrawTransaction(txHash);
                if (txInfo == null) {
                    return null;
                }
                txInfo.setTxTime(txTime);
            }
        } else {
            txInfo = htgParseTxHelper.parseWithdrawTransaction(txHash);
            if (txInfo == null) {
                return null;
            }
        }
        if (txInfo.getTxTime() == null) {
            EthBlock.Block block = htgWalletApi.getBlockHeaderByHeight(txInfo.getBlockHeight());
            txInfo.setTxTime(block.getTimestamp().longValue());
        }
        return txInfo;
    }

    @Override
    public HeterogeneousConfirmedInfo getConfirmedTxInfo(String txHash) throws Exception {
        HeterogeneousConfirmedInfo info = new HeterogeneousConfirmedInfo();
        String from;
        Long txTime = null;
        Long blockHeight;
        List<HeterogeneousAddress> signers = null;
        // v15After upgrading the wavefield protocol, transactions can be directly obtained from heterogeneous chains to prevent inconsistency between transaction information caused by network block rollback in heterogeneous chains and locally saved transaction information
        if (htgContext.getConverterCoreApi().isSupportProtocol15TrxCrossChain()) {
            org.web3j.protocol.core.methods.response.Transaction tx = htgWalletApi.getTransactionByHash(txHash);
            BigInteger txHeight = htgParseTxHelper.getTxHeight(logger(), tx);
            if (tx == null || txHeight == null) {
                return null;
            }
            from = tx.getFrom();
            blockHeight = txHeight.longValue();
            if(signers == null || signers.isEmpty()) {
                TransactionReceipt txReceipt = htgWalletApi.getTxReceipt(txHash);
                signers = htgParseTxHelper.parseSigners(txReceipt, from);
            }
            if (txTime == null) {
                EthBlock.Block block = htgWalletApi.getBlockHeaderByHeight(blockHeight);
                if (block == null) {
                    return null;
                }
                txTime = block.getTimestamp().longValue();
            }
            info.setMultySignAddress(htgContext.MULTY_SIGN_ADDRESS());
            info.setTxTime(txTime);
            info.setSigners(signers);
            return info;
        }
        // fromDBObtain data from, if unable to obtain, then go toHTGObtained from the network
        HeterogeneousTransactionInfo txInfo = htgTxStorageService.findByTxHash(txHash);
        if (txInfo != null && StringUtils.isNotBlank(txInfo.getFrom())) {
            from = txInfo.getFrom();
            blockHeight = txInfo.getBlockHeight();
            signers = txInfo.getSigners();
            txTime = txInfo.getTxTime();
        } else {
            org.web3j.protocol.core.methods.response.Transaction tx = htgWalletApi.getTransactionByHash(txHash);
            if (tx == null || tx.getBlockNumber() == null) {
                return null;
            }
            from = tx.getFrom();
            blockHeight = tx.getBlockNumber().longValue();
        }
        if(signers == null || signers.isEmpty()) {
            TransactionReceipt txReceipt = htgWalletApi.getTxReceipt(txHash);
            signers = htgParseTxHelper.parseSigners(txReceipt, from);
        }
        if (txTime == null) {
            EthBlock.Block block = htgWalletApi.getBlockHeaderByHeight(blockHeight);
            if (block == null) {
                return null;
            }
            txTime = block.getTimestamp().longValue();
        }
        info.setMultySignAddress(htgContext.MULTY_SIGN_ADDRESS());
        info.setTxTime(txTime);
        info.setSigners(signers);
        return info;
    }


    @Override
    public HeterogeneousChangePendingInfo getChangeVirtualBankPendingInfo(String nerveTxHash) throws Exception {
        throw new NulsException(ConverterErrorCode.NO_LONGER_SUPPORTED, "Functions that are no longer supported after contract upgrade[3]");
    }

    @Override
    public String createOrSignWithdrawTx(String nerveTxHash, String toAddress, BigInteger value, Integer assetId) throws NulsException {
        throw new NulsException(ConverterErrorCode.NO_LONGER_SUPPORTED, "Functions that are no longer supported after contract upgrade[0]");
    }

    @Override
    public String createOrSignManagerChangesTx(String nerveTxHash, String[] addAddresses, String[] removeAddresses, int orginTxCount) throws NulsException {
        throw new NulsException(ConverterErrorCode.NO_LONGER_SUPPORTED, "Functions that are no longer supported after contract upgrade[1]");
    }

    @Override
    public String createOrSignUpgradeTx(String nerveTxHash) throws NulsException {
        throw new NulsException(ConverterErrorCode.NO_LONGER_SUPPORTED, "Functions that are no longer supported after contract upgrade[2]");
    }

    @Override
    public String createOrSignWithdrawTxII(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData) throws NulsException {
        return this.createOrSignWithdrawTxII(nerveTxHash, toAddress, value, assetId, signatureData, true);
    }

    @Override
    public boolean validateManagerChangesTxII(String nerveTxHash, String[] addAddresses, String[] removeAddresses, int orginTxCount, String signatureData) throws NulsException {
        logger().info("validate {} Online virtual banking change transactions,nerveTxHash: {}, signatureData: {}", htgContext.getConfig().getSymbol(), nerveTxHash, signatureData);
        try {
            // towardsHTGNetwork request verification
            boolean isCompleted = htgParseTxHelper.isCompletedTransactionByLatest(nerveTxHash);
            if (isCompleted) {
                logger().info("[{}] [{}] transaction [{}] Completed", htgContext.getConfig().getSymbol(), HeterogeneousChainTxType.CHANGE, nerveTxHash);
                return true;
            }
            // Business validation
            if (addAddresses == null) {
                addAddresses = new String[0];
            }
            if (removeAddresses == null) {
                removeAddresses = new String[0];
            }
            Set<String> addSet = new HashSet<>();
            List<Address> addList = new ArrayList<>();
            for (int a = 0, addSize = addAddresses.length; a < addSize; a++) {
                String add = addAddresses[a];
                add = add.toLowerCase();
                addAddresses[a] = add;
                if (!addSet.add(add)) {
                    logger().error("Duplicate list of addresses to be added");
                    return false;
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
                    return false;
                }
                removeList.add(new Address(remove));
            }
            // If you have not joined or exited, send a confirmation transaction directly
            if (addAddresses.length == 0 && removeAddresses.length == 0) {
                return true;
            }
            // Obtain administrator account
            String fromAddress = htgContext.ADMIN_ADDRESS();
            if (removeSet.contains(fromAddress)) {
                logger().error("Logged out administrators cannot participate in administrator change transactions");
                return false;
            }
            Function txFunction = HtgUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, orginTxCount, signatureData);
            // Verify the legality of contract transactions
            EthCall ethCall = htgWalletApi.validateContractCall(fromAddress, htgContext.MULTY_SIGN_ADDRESS(), txFunction);
            if (ethCall.isReverted()) {
                logger().error("[{}]Transaction verification failed, reason: {}", HeterogeneousChainTxType.CHANGE, ethCall.getRevertReason());
                return false;
            }
            return true;
        } catch (Exception e) {
            logger().error(String.format("adds: %s, removes: %s", addAddresses != null ? Arrays.toString(addAddresses) : "[]", removeAddresses != null ? Arrays.toString(removeAddresses) : "[]"), e);
            return false;
        }
    }

    @Override
    public String createOrSignManagerChangesTxII(String nerveTxHash, String[] addAddresses, String[] removeAddresses, int orginTxCount, String signatureData) throws NulsException {
        // increaseresendmechanism
        try {
            if (!htgContext.isAvailableRPC()) {
                logger().error("[{}]networkRPCUnavailable, pause this task", htgContext.getConfig().getSymbol());
                throw new NulsException(ConverterErrorCode.HTG_RPC_UNAVAILABLE);
            }
            return this.createOrSignManagerChangesTxII(nerveTxHash, addAddresses, removeAddresses, orginTxCount, signatureData, true);
        } catch (NulsException e) {
            // When execution fails due to insufficient transaction signatures, report toCORERequesting Byzantine signatures for the transaction again
            if (ConverterUtil.isInsufficientSignature(e)) {
                HtgWaitingTxPo waitingTxPo = htgInvokeTxHelper.findEthWaitingTxPo(nerveTxHash);
                if (waitingTxPo != null) {
                    try {
                        String reSendHtTxHash = htgResendHelper.reSend(waitingTxPo);
                        logger().info("Nervetransaction[{}]Resend completed, reSendHtTxHash: {}", nerveTxHash, reSendHtTxHash);
                        return reSendHtTxHash;
                    } catch (Exception ex) {
                        throw new NulsException(ex);
                    }
                }
            }
            throw e;
        }
    }

    @Override
    public String createOrSignUpgradeTxII(String nerveTxHash, String upgradeContract, String signatureData) throws NulsException {
        return this.createOrSignUpgradeTxII(nerveTxHash, upgradeContract, signatureData, true);
    }

    @Override
    public String forceRecovery(String nerveTxHash, String[] seedManagers, String[] allManagers) throws NulsException {
        /*try {
            HtgUnconfirmedTxPo po = new HtgUnconfirmedTxPo();
            po.setTxType(HeterogeneousChainTxType.RECOVERY);
            po.setNerveTxHash(nerveTxHash);
            htgContext.UNCONFIRMED_TX_QUEUE().offer(po);
            htgCallBackManager.getTxConfirmedProcessor().txConfirmed(
                    HeterogeneousChainTxType.RECOVERY,
                    nerveTxHash,
                    null, //htTxHash,
                    null, //htTx blockHeight,
                    null, //htTx tx time,
                    htgContext.MULTY_SIGN_ADDRESS(),
                    null  //ethTx signers
            );
        } catch (Exception e) {
            if (e instanceof NulsException) {
                throw (NulsException) e;
            }
            throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
        }*/
        return EMPTY_STRING;
    }

    @Override
    public Boolean reAnalysisDepositTx(String htTxHash) throws Exception {
        if (htgCommonHelper.constainHash(htTxHash)) {
            logger().info("Repeated collection of recharge transactionshash: {}No more repeated parsing[0]", htTxHash);
            return true;
        }
        reAnalysisLock.lock();
        try {
            if (htgCommonHelper.constainHash(htTxHash)) {
                logger().info("Repeated collection of recharge transactionshash: {}No more repeated parsing[1]", htTxHash);
                return true;
            }
            logger().info("Re analyze recharge transactions: {}", htTxHash);
            org.web3j.protocol.core.methods.response.Transaction tx = htgWalletApi.getTransactionByHash(htTxHash);
            BigInteger txHeight = htgParseTxHelper.getTxHeight(logger(), tx);
            if (tx == null || txHeight == null) {
                return false;
            }
            HeterogeneousTransactionInfo txInfo = htgParseTxHelper.parseDepositTransaction(tx);
            if (txInfo == null) {
                return false;
            }
            Long blockHeight = txHeight.longValue();
            EthBlock.Block block = htgWalletApi.getBlockHeaderByHeight(blockHeight);
            if (block == null) {
                return false;
            }
            Long txTime = block.getTimestamp().longValue();
            htgAnalysisTxHelper.analysisTx(tx, txTime, blockHeight);
            htgCommonHelper.addHash(htTxHash);
            return true;
        } catch (Exception e) {
            throw e;
        } finally {
            reAnalysisLock.unlock();
        }
    }

    @Override
    public Boolean reAnalysisTx(String htTxHash) throws Exception {
        if (htgCommonHelper.constainHash(htTxHash)) {
            logger().info("Repeated collection of transactionshash: {}No more repeated parsing[0]", htTxHash);
            return true;
        }
        reAnalysisLock.lock();
        try {
            if (htgCommonHelper.constainHash(htTxHash)) {
                logger().info("Repeated collection of transactionshash: {}No more repeated parsing[1]", htTxHash);
                return true;
            }
            logger().info("Re analyze transactions: {}", htTxHash);
            org.web3j.protocol.core.methods.response.Transaction tx = htgWalletApi.getTransactionByHash(htTxHash);
            BigInteger txHeight = htgParseTxHelper.getTxHeight(logger(), tx);
            if (tx == null || txHeight == null) {
                return false;
            }
            Long blockHeight = txHeight.longValue();
            EthBlock.Block block = htgWalletApi.getBlockHeaderByHeight(blockHeight);
            if (block == null) {
                return false;
            }
            Long txTime = block.getTimestamp().longValue();
            htgUnconfirmedTxStorageService.deleteByTxHash(htTxHash);
            htgAnalysisTxHelper.analysisTx(tx, txTime, blockHeight);
            htgCommonHelper.addHash(htTxHash);
            return true;
        } catch (Exception e) {
            throw e;
        } finally {
            reAnalysisLock.unlock();
        }
    }

    @Override
    public long getHeterogeneousNetworkChainId() {
        return htgContext.getConfig().getChainIdOnHtgNetwork();
    }

    @Override
    public String signWithdrawII(String txHash, String toAddress, BigInteger value, Integer assetId) throws NulsException {
        boolean isContractAsset = assetId > 1;
        String contractAddressERC20;
        if (isContractAsset) {
            contractAddressERC20 = htgERC20Helper.getContractAddressByAssetId(assetId);
        } else {
            contractAddressERC20 = HtgConstant.ZERO_ADDRESS;
        }
        // If the accuracy of cross chain assets is different, then the conversion accuracy
        value = htgContext.getConverterCoreApi().checkDecimalsSubtractedToNerveForWithdrawal(htgContext.HTG_CHAIN_ID(), assetId, value);
        // Convert the address to lowercase
        toAddress = toAddress.toLowerCase();
        if (htgContext.getConverterCoreApi().isLocalSign()) {
            // Obtain administrator account
            HtgAccount account = (HtgAccount) this.getAccount(htgContext.ADMIN_ADDRESS());
            account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            String vHash = HtgUtil.encoderWithdraw(htgContext, txHash, toAddress, value, isContractAsset, contractAddressERC20, htgContext.VERSION());
            logger().debug("Withdrawal signature data: {}, {}, {}, {}, {}, {}", txHash, toAddress, value, isContractAsset, contractAddressERC20, htgContext.VERSION());
            logger().debug("Withdrawal signaturevHash: {}, nerveTxHash: {}", vHash, txHash);
            return HtgUtil.dataSign(vHash, priKey);
        } else {
            return htgContext.getConverterCoreApi().signWithdrawByMachine(htgContext.getConfig().getChainIdOnHtgNetwork(), htgContext.ADMIN_ADDRESS_PUBLIC_KEY(),
                    txHash, toAddress, value, isContractAsset, contractAddressERC20, htgContext.VERSION());
        }
    }

    @Override
    public String signManagerChangesII(String nerveTxHash, String[] addAddresses, String[] removeAddresses, int orginTxCount) throws NulsException {
        // Business validation
        if (addAddresses == null) {
            addAddresses = new String[0];
        }
        if (removeAddresses == null) {
            removeAddresses = new String[0];
        }
        // Transaction preparation
        Set<String> addSet = new HashSet<>();
        for (int a = 0, addSize = addAddresses.length; a < addSize; a++) {
            String add = addAddresses[a];
            add = add.toLowerCase();
            addAddresses[a] = add;
            if (!addSet.add(add)) {
                logger().error("Duplicate list of addresses to be added");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_2);
            }
        }
        Set<String> removeSet = new HashSet<>();
        for (int r = 0, removeSize = removeAddresses.length; r < removeSize; r++) {
            String remove = removeAddresses[r];
            remove = remove.toLowerCase();
            removeAddresses[r] = remove;
            if (!removeSet.add(remove)) {
                logger().error("Duplicate list of pending exits");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_4);
            }
        }
        if (htgContext.getConverterCoreApi().isLocalSign()) {
            // Obtain administrator account
            HtgAccount account = (HtgAccount) this.getAccount(htgContext.ADMIN_ADDRESS());
            account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            String vHash = HtgUtil.encoderChange(htgContext, nerveTxHash, addAddresses, orginTxCount, removeAddresses, htgContext.VERSION());
            logger().debug("Change the signature of the transactionvHash: {}, nerveTxHash: {}", vHash, nerveTxHash);
            return HtgUtil.dataSign(vHash, priKey);
        } else {
            return htgContext.getConverterCoreApi().signChangeByMachine(htgContext.getConfig().getChainIdOnHtgNetwork(), htgContext.ADMIN_ADDRESS_PUBLIC_KEY(),
                    nerveTxHash, addAddresses, orginTxCount, removeAddresses, htgContext.VERSION());
        }

    }

    @Override
    public String signUpgradeII(String nerveTxHash, String upgradeContract) throws NulsException {
        // Convert the address to lowercase
        upgradeContract = upgradeContract.toLowerCase();
        if (htgContext.getConverterCoreApi().isLocalSign()) {
            // Obtain administrator account
            HtgAccount account = (HtgAccount) this.getAccount(htgContext.ADMIN_ADDRESS());
            account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            String vHash = HtgUtil.encoderUpgrade(htgContext, nerveTxHash, upgradeContract, htgContext.VERSION());
            logger().debug("Upgrade the signature of the transactionvHash: {}, nerveTxHash: {}", vHash, nerveTxHash);
            return HtgUtil.dataSign(vHash, priKey);
        } else {
            return htgContext.getConverterCoreApi().signUpgradeByMachine(htgContext.getConfig().getChainIdOnHtgNetwork(), htgContext.ADMIN_ADDRESS_PUBLIC_KEY(),
                    nerveTxHash, upgradeContract, htgContext.VERSION());
        }
    }

    @Override
    public Boolean verifySignWithdrawII(String signAddress, String txHash, String toAddress, BigInteger value, Integer assetId, String signed) throws NulsException {
        boolean isContractAsset = assetId > 1;
        String contractAddressERC20;
        if (isContractAsset) {
            contractAddressERC20 = htgERC20Helper.getContractAddressByAssetId(assetId);
        } else {
            contractAddressERC20 = HtgConstant.ZERO_ADDRESS;
        }
        // If the accuracy of cross chain assets is different, then the conversion accuracy
        value = htgContext.getConverterCoreApi().checkDecimalsSubtractedToNerveForWithdrawal(htgContext.HTG_CHAIN_ID(), assetId, value);
        // Convert the address to lowercase
        toAddress = toAddress.toLowerCase();
        String vHash = HtgUtil.encoderWithdraw(htgContext, txHash, toAddress, value, isContractAsset, contractAddressERC20, htgContext.VERSION());
        logger().debug("[Verify signature] Withdrawal data: {}, {}, {}, {}, {}, {}", txHash, toAddress, value, isContractAsset, contractAddressERC20, htgContext.VERSION());
        logger().debug("[Verify signature] WithdrawalvHash: {}", vHash);
        return HtgUtil.verifySign(signAddress, vHash, signed);
    }

    @Override
    public Boolean verifySignManagerChangesII(String signAddress, String nerveTxHash, String[] addAddresses, String[] removeAddresses, int orginTxCount, String signed) throws NulsException {
        if (addAddresses == null) {
            addAddresses = new String[0];
        }
        if (removeAddresses == null) {
            removeAddresses = new String[0];
        }
        // Transaction preparation
        for (int a = 0, addSize = addAddresses.length; a < addSize; a++) {
            String add = addAddresses[a];
            add = add.toLowerCase();
            addAddresses[a] = add;
        }
        for (int r = 0, removeSize = removeAddresses.length; r < removeSize; r++) {
            String remove = removeAddresses[r];
            remove = remove.toLowerCase();
            removeAddresses[r] = remove;
        }
        String vHash = HtgUtil.encoderChange(htgContext, nerveTxHash, addAddresses, orginTxCount, removeAddresses, htgContext.VERSION());
        return HtgUtil.verifySign(signAddress, vHash, signed);
    }

    @Override
    public Boolean verifySignUpgradeII(String signAddress, String txHash, String upgradeContract, String signed) throws NulsException {
        // Convert the address to lowercase
        upgradeContract = upgradeContract.toLowerCase();
        String vHash = HtgUtil.encoderUpgrade(htgContext, txHash, upgradeContract, htgContext.VERSION());
        return HtgUtil.verifySign(signAddress, vHash, signed);
    }


    @Override
    public boolean isEnoughNvtFeeOfWithdraw(BigDecimal nvtAmount, int hAssetId) {
        BigInteger l1Fee = htgContext.getConverterCoreApi().getL1Fee(htgContext.HTG_CHAIN_ID());
        if (l1Fee.compareTo(BigInteger.ZERO) == 0) {
            return this.isEnoughFeeOfWithdrawByOtherMainAsset(AssetName.NVT, nvtAmount, hAssetId);
        } else {
            IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
            BigDecimal nvtUsdPrice = coreApi.getUsdtPriceByAsset(AssetName.NVT);
            BigDecimal ethUsdPrice = coreApi.getUsdtPriceByAsset(AssetName.ETH);
            BigDecimal l2UsdPrice = coreApi.getUsdtPriceByAsset(htgContext.ASSET_NAME());
            int decimalsNVT = AssetName.NVT.decimals();
            // L2 fee
            BigDecimal l2FeeOfNVT = l2UsdPrice.multiply(new BigDecimal(htgContext.getEthGasPrice())).multiply(new BigDecimal(htgContext.GAS_LIMIT_OF_WITHDRAW())).movePointRight(decimalsNVT).movePointLeft(AssetName.ETH.decimals()).divide(nvtUsdPrice, 0, RoundingMode.UP);
            // L1 fee
            BigDecimal l1FeeOfNVT = ethUsdPrice.multiply(new BigDecimal(l1Fee)).movePointRight(decimalsNVT).movePointLeft(AssetName.ETH.decimals()).divide(nvtUsdPrice, 0, RoundingMode.UP);
            BigDecimal totalFeeOfNVT = l1FeeOfNVT.add(l2FeeOfNVT);
            totalFeeOfNVT = totalFeeOfNVT.divide(BigDecimal.TEN.pow(decimalsNVT), 0, RoundingMode.UP).movePointRight(decimalsNVT);
            if (nvtAmount.compareTo(totalFeeOfNVT) < 0) {
                logger().info("[{}]Insufficient withdrawal fees, currently required by the networkNVT: {}, User providedNVT: {}, Additional RequiredNVT: {}",
                        htgContext.getConfig().getSymbol(),
                        totalFeeOfNVT.movePointLeft(decimalsNVT).toPlainString(),
                        nvtAmount.movePointLeft(decimalsNVT).toPlainString(),
                        totalFeeOfNVT.subtract(nvtAmount).movePointLeft(decimalsNVT).toPlainString());
                return false;
            }
            logger().info("[{}]The withdrawal fee is sufficient for the current network needsNVT: {}, User providedNVT: {}",
                    htgContext.getConfig().getSymbol(),
                    totalFeeOfNVT.movePointLeft(decimalsNVT).toPlainString(),
                    nvtAmount.movePointLeft(decimalsNVT).toPlainString());
            return true;
        }
    }

    @Override
    public boolean isEnoughFeeOfWithdrawByMainAssetProtocol15(AssetName assetName, BigDecimal amount, int hAssetId) {
        BigInteger _l1Fee = htgContext.getConverterCoreApi().getL1Fee(htgContext.HTG_CHAIN_ID());
        if (_l1Fee.compareTo(BigInteger.ZERO) == 0) {
            // Can use the main assets of other heterogeneous networks as transaction fees, For example, withdrawal toETH, PaymentBNBAs a handling fee
            if (assetName == htgContext.ASSET_NAME()) {
                return this.calcGasPriceOfWithdrawByMainAssetProtocol15(amount, hAssetId) != null;
            } else {
                return this.isEnoughFeeOfWithdrawByOtherMainAsset(assetName, amount, hAssetId);
            }
        } else {
            IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
            BigDecimal ethUsdPrice = coreApi.getUsdtPriceByAsset(AssetName.ETH);
            BigDecimal l2UsdPrice = coreApi.getUsdtPriceByAsset(htgContext.ASSET_NAME());
            // Can use the main assets of other heterogeneous networks as transaction fees, For example, withdrawal toETH, PaymentBNBAs a handling fee
            if (assetName == htgContext.ASSET_NAME()) {
                int decimals = assetName.decimals();
                // L2 fee
                BigDecimal l2Fee = new BigDecimal(htgContext.getEthGasPrice()).multiply(new BigDecimal(htgContext.GAS_LIMIT_OF_WITHDRAW()));
                // L1 fee
                BigDecimal l1Fee = new BigDecimal(_l1Fee);
                BigDecimal totalFee = l1Fee.add(l2Fee);
                String symbolNative = htgContext.getConfig().getSymbol();
                if (amount.compareTo(totalFee) < 0) {
                    logger().info("[{}]Insufficient withdrawal fees, currently required by the network{}: {}, User provided{}: {}, Additional Required{}: {}",
                            symbolNative,
                            symbolNative,
                            totalFee.movePointLeft(decimals).toPlainString(),
                            symbolNative,
                            amount.movePointLeft(decimals).toPlainString(),
                            symbolNative,
                            totalFee.subtract(amount).movePointLeft(decimals).toPlainString());
                    return false;
                }
                logger().info("[{}]The withdrawal fee is sufficient for the current network needs{}: {}, User provided{}: {}",
                        symbolNative,
                        symbolNative,
                        totalFee.movePointLeft(decimals).toPlainString(),
                        symbolNative,
                        amount.movePointLeft(decimals).toPlainString());
                return true;
            } else {
                BigDecimal payAssetUsdPrice = coreApi.getUsdtPriceByAsset(assetName);
                int decimalsPaid = assetName.decimals();
                String symbolPaid = assetName.toString();
                int decimalsNative = htgContext.ASSET_NAME().decimals();
                // L2 fee
                BigDecimal l2FeeOfPaid = l2UsdPrice.multiply(new BigDecimal(htgContext.getEthGasPrice())).multiply(new BigDecimal(htgContext.GAS_LIMIT_OF_WITHDRAW())).movePointRight(decimalsPaid).movePointLeft(decimalsNative).divide(payAssetUsdPrice, 0, RoundingMode.UP);
                // L1 fee
                BigDecimal l1FeeOfPaid = ethUsdPrice.multiply(new BigDecimal(_l1Fee)).movePointRight(decimalsPaid).movePointLeft(decimalsNative).divide(payAssetUsdPrice, 0, RoundingMode.UP);
                BigDecimal totalFeeOfPaid = l1FeeOfPaid.add(l2FeeOfPaid);
                if (amount.compareTo(totalFeeOfPaid) < 0) {
                    logger().info("[{}]Insufficient withdrawal fees, currently required by the network{}: {}, User provided{}: {}, Additional Required{}: {}",
                            htgContext.getConfig().getSymbol(),
                            symbolPaid,
                            totalFeeOfPaid.movePointLeft(decimalsPaid).toPlainString(),
                            symbolPaid,
                            amount.movePointLeft(decimalsPaid).toPlainString(),
                            symbolPaid,
                            totalFeeOfPaid.subtract(amount).movePointLeft(decimalsPaid).toPlainString());
                    return false;
                }
                logger().info("[{}]The withdrawal fee is sufficient for the current network needs{}: {}, User provided{}: {}",
                        htgContext.getConfig().getSymbol(),
                        symbolPaid,
                        totalFeeOfPaid.movePointLeft(decimalsPaid).toPlainString(),
                        symbolPaid,
                        amount.movePointLeft(decimalsPaid).toPlainString());
                return true;
            }
        }
    }

    @Override
    public boolean isMinterERC20(String erc20) throws Exception {
        return htgParseTxHelper.isMinterERC20(erc20);
    }

    @Override
    public String cancelHtgTx(String nonce, String priceGWei) throws Exception {
        // Obtain administrator account
        HtgAccount account = (HtgAccount) this.getAccount(htgContext.ADMIN_ADDRESS());
        account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
        String fromAddress = account.getAddress();
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        BigInteger gasPrice = new BigInteger(priceGWei).multiply(BigInteger.TEN.pow(9));
        // Calculate a suitablegasPrice
        gasPrice = HtgUtil.calcNiceGasPriceOfWithdraw(htgContext.ASSET_NAME(), new BigDecimal(htgContext.getEthGasPrice()), new BigDecimal(gasPrice)).toBigInteger();
        return htgWalletApi.sendMainAssetWithNonce(
                fromAddress,
                priKey,
                fromAddress,
                BigDecimal.ZERO,
                htgContext.GAS_LIMIT_OF_MAIN_ASSET(),
                gasPrice,
                new BigInteger(nonce)
        );
    }

    @Override
    public void initialSignatureVersion() {
        byte version = htgMultiSignAddressHistoryStorageService.getVersion();
        if (converterConfig.isNewProcessorMode()) {
            htgContext.SET_VERSION(HtgConstant.VERSION_MULTY_SIGN_LATEST);
        }
        if (version > 0) {
            htgContext.SET_VERSION(version);
        }
        logger().info("[{}]Current signature version number on the network: {}", htgContext.getConfig().getSymbol(), htgContext.VERSION());
    }

    @Override
    public HeterogeneousOneClickCrossChainData parseOneClickCrossChainData(String extend) {
        return htgParseTxHelper.parseOneClickCrossChainData(extend, logger());
    }

    @Override
    public HeterogeneousAddFeeCrossChainData parseAddFeeCrossChainData(String extend) {
        return htgParseTxHelper.parseAddFeeCrossChainData(extend, logger());
    }

    @Override
    public HeterogeneousChainGasInfo getHeterogeneousChainGasInfo() {
        if (gasInfo == null) {
            gasInfo = new HeterogeneousChainGasInfo();
            gasInfo.setGasLimitOfWithdraw(htgContext.GAS_LIMIT_OF_WITHDRAW().toString());
        }
        return gasInfo;
    }

    @Override
    public boolean isAvailableRPC() {
        return htgContext.isAvailableRPC();
    }

    @Override
    public BigInteger currentGasPrice() {
        return htgContext.getEthGasPrice();
    }

    @Override
    public void setRegister(IHeterogeneousChainRegister register) {
        this.register = register;
    }

    @Override
    public void closeChainPending() {
        this.closePending = true;
    }

    @Override
    public void closeChainConfirm() {
        if (!this.closePending) {
            throw new RuntimeException("Error steps to close the chain.");
        }
        // close thread pool of task
        this.register.shutdownPending();
        this.register.shutdownConfirm();
    }

    public String createOrSignWithdrawTxII(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData, boolean checkOrder) throws NulsException {
        if (htgContext.getConverterCoreApi().isProtocol22()) {
            // protocol22: Support cross chain assets with different accuracies
            return createOrSignWithdrawTxIIProtocol22(nerveTxHash, toAddress, value, assetId, signatureData, checkOrder);
        } else if (htgContext.getConverterCoreApi().isSupportProtocol15TrxCrossChain()) {
            // protocol15: Modify the handling fee mechanism to support heterogeneous chain main assets as handling fees
            return createOrSignWithdrawTxIIProtocol15(nerveTxHash, toAddress, value, assetId, signatureData, checkOrder);
        } else {
            return _createOrSignWithdrawTxII(nerveTxHash, toAddress, value, assetId, signatureData, checkOrder);
        }
    }

    public String createOrSignManagerChangesTxII(String nerveTxHash, String[] addAddresses, String[] removeAddresses, int orginTxCount, String signatureData, boolean checkOrder) throws NulsException {
        logger().info("Preparing to send virtual bank changes{}Transactions,nerveTxHash: {}, signatureData: {}", htgContext.getConfig().getSymbol(), nerveTxHash, signatureData);
        try {
            // Business validation
            if (addAddresses == null) {
                addAddresses = new String[0];
            }
            if (removeAddresses == null) {
                removeAddresses = new String[0];
            }
            // Prepare data
            HtgUnconfirmedTxPo po = new HtgUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            po.setAddAddresses(addAddresses);
            po.setRemoveAddresses(removeAddresses);
            po.setOrginTxCount(orginTxCount);
            // Transaction preparation
            HtgWaitingTxPo waitingPo = new HtgWaitingTxPo();
            HtgAccount account = this.createTxStartForChange(nerveTxHash, addAddresses, removeAddresses, waitingPo);
            // Save transaction call parameters and set waiting time to end
            htgInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, addAddresses, removeAddresses, orginTxCount, signatureData, account.getOrder(), waitingPo);
            if (account.isEmpty()) {
                return EMPTY_STRING;
            }
            // When checking the order, do not send transactions that are not in the first place
            if (checkOrder && !checkFirstOrder(account.getOrder())) {
                logger().info("Non primary non transaction, order: {}", account.getOrder());
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
            // If you have not joined or exited, send a confirmation transaction directly
            if (addAddresses.length == 0 && removeAddresses.length == 0) {
                logger().info("Virtual banking changes have not been added or exited, and confirmation transactions have been sent directly, nerveTxHash: {}", nerveTxHash);
                try {
                    htgCallBackManager.getTxConfirmedProcessor().txConfirmed(
                            HeterogeneousChainTxType.CHANGE,
                            nerveTxHash,
                            null, //htTxHash,
                            null, //ethTx blockHeight,
                            null, //ethTx tx time,
                            htgContext.MULTY_SIGN_ADDRESS(),
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
            Function createOrSignManagerChangeFunction = HtgUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, orginTxCount, signatureData);
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

    public String createOrSignUpgradeTxII(String nerveTxHash, String upgradeContract, String signatureData, boolean checkOrder) throws NulsException {
        logger().info("Preparing to send virtual banking contract upgrade authorization transactions,nerveTxHash: {}, upgradeContract: {}, signatureData: {}", nerveTxHash, upgradeContract, signatureData);
        try {
            // Transaction preparation
            HtgWaitingTxPo waitingPo = new HtgWaitingTxPo();
            HtgAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.UPGRADE, waitingPo);
            // Save transaction call parameters and set waiting time to end
            htgInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, upgradeContract, signatureData, account.getOrder(), waitingPo);
            if (account.isEmpty()) {
                return EMPTY_STRING;
            }
            // When checking the order, do not send transactions that are not in the first place
            if (checkOrder && !checkFirstOrder(account.getOrder())) {
                logger().info("Non primary non transaction, order: {}", account.getOrder());
                return EMPTY_STRING;
            }
            // Obtain administrator account
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            HtgUnconfirmedTxPo po = new HtgUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);

            Function function = HtgUtil.getCreateOrSignUpgradeFunction(nerveTxHash, upgradeContract, signatureData);
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

    private boolean checkFirstOrder(int bankOrder) {
        if (bankOrder != 1) {
            return false;
        }
        return true;
    }

    private HtgAccount createTxStartForChange(String nerveTxHash, String[] addAddresses, String[] removeAddresses, HtgWaitingTxPo po) throws Exception {
        HtgAccount txStart = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.CHANGE, po);
        Map<String, Integer> currentVirtualBanks = po.getCurrentVirtualBanks();
        if (addAddresses.length == 0 && removeAddresses.length == 0) {
            return txStart;
        }
        for (String add : addAddresses) {
            currentVirtualBanks.remove(add);
        }
        for (String remove : removeAddresses) {
            currentVirtualBanks.remove(remove);
        }
        List<Map.Entry<String, Integer>> list = new ArrayList<>(currentVirtualBanks.entrySet());
        list.sort(ConverterUtil.CHANGE_SORT);
        int i = 1;
        for (Map.Entry<String, Integer> entry : list) {
            currentVirtualBanks.put(entry.getKey(), i++);
        }
        Integer order = currentVirtualBanks.get(htgContext.ADMIN_ADDRESS());
        if (order == null) {
            order = 0x0f;
        }
        txStart.setOrder(order);
        logger().info("Change the current node execution order of the transaction: {}, addAddresses: {}, removeAddresses: {}, currentVirtualBanks: {}", order, Arrays.toString(addAddresses), Arrays.toString(removeAddresses), currentVirtualBanks);
        return txStart;
    }

    private HtgAccount createTxStart(String nerveTxHash, HeterogeneousChainTxType txType, HtgWaitingTxPo po) throws Exception {
        Map<String, Integer> currentVirtualBanks = htgContext.getConverterCoreApi().currentVirtualBanksBalanceOrder(htgContext.getConfig().getChainId());
        po.setCurrentVirtualBanks(currentVirtualBanks);
        int bankSize = htgContext.getConverterCoreApi().getVirtualBankSize();
        /*String realNerveTxHash = nerveTxHash;
        // according tonervetransactionhashCalculate the sequential seed for the first two digits
        int seed = new BigInteger(realNerveTxHash.substring(0, 1), 16).intValue() + 1;

        if (bankSize > 16) {
            seed += new BigInteger(realNerveTxHash.substring(1, 2), 16).intValue() + 1;
        }
        int mod = seed % bankSize + 1;
        currentVirtualBanks.entrySet().stream().forEach(e -> {
            int bankOrder = e.getValue();
            if (bankOrder < mod) {
                bankOrder += bankSize - (mod - 1);
            } else {
                bankOrder -= mod - 1;
            }
            e.setValue(bankOrder);
        });
        logger().debug("Processing, Current Bank Order: {}", currentVirtualBanks);
        List<Map.Entry<String, Integer>> list = new ArrayList(currentVirtualBanks.entrySet());
        list.sort(ConverterUtil.CHANGE_SORT);
        int i = 1;
        for (Map.Entry<String, Integer> entry : list) {
            currentVirtualBanks.put(entry.getKey(), i++);
        }
        logger().debug("After processing, Current Bank Order: {}", currentVirtualBanks);*/

        // Wait for a fixed time in sequence before sending outHTGtransaction
        int bankOrder = currentVirtualBanks.get(htgContext.ADMIN_ADDRESS());
        if (logger().isDebugEnabled()) {
            logger().debug("Sequential calculation parameters bankSize: {}, orginBankOrder: {}, bankOrder: {}", bankSize, htgContext.getConverterCoreApi().getVirtualBankOrder(), bankOrder);
        }
        // towardsHTGNetwork request verification
        boolean isCompleted = htgParseTxHelper.isCompletedTransactionByLatest(nerveTxHash);
        if (isCompleted) {
            logger().info("[{}]transaction[{}]Completed", txType, nerveTxHash);
            return HtgAccount.newEmptyAccount(bankOrder);
        }
        // Obtain administrator account
        HtgAccount account = (HtgAccount) this.getAccount(htgContext.ADMIN_ADDRESS());
        account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
        account.setOrder(bankOrder);
        account.setBankSize(bankSize);
        return account;
    }

    private HtgAccount createTxStartForWithdraw(String nerveTxHash, HeterogeneousChainTxType txType, HtgWaitingTxPo po) throws Exception {
        Map<String, Integer> currentVirtualBanks = htgContext.getConverterCoreApi().currentVirtualBanksBalanceOrder(htgContext.getConfig().getChainId());
        po.setCurrentVirtualBanks(currentVirtualBanks);
        int bankSize = htgContext.getConverterCoreApi().getVirtualBankSize();
        // Wait for a fixed time in sequence before sending outHTGtransaction
        int bankOrder = currentVirtualBanks.get(htgContext.ADMIN_ADDRESS());
        if (logger().isDebugEnabled()) {
            logger().debug("Sequential calculation parameters bankSize: {}, orginBankOrder: {}, bankOrder: {}", bankSize, htgContext.getConverterCoreApi().getVirtualBankOrder(), bankOrder);
        }
        // towardsHTGNetwork request verification
        boolean isCompleted = htgParseTxHelper.isCompletedTransactionByLatest(nerveTxHash);
        if (isCompleted) {
            logger().info("[{}] transaction [{}] Completed", txType, nerveTxHash);
            return HtgAccount.newEmptyAccount(bankOrder);
        }
        // Obtain administrator account
        HtgAccount account = (HtgAccount) this.getAccount(htgContext.ADMIN_ADDRESS());
        account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
        account.setOrder(bankOrder);
        account.setBankSize(bankSize);
        return account;
    }


    private String createTxComplete(String nerveTxHash, HtgUnconfirmedTxPo po, String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType) throws Exception {
        return this.createTxComplete(nerveTxHash, po, fromAddress, priKey, txFunction, txType, null, false);
    }
    private String createTxComplete(String nerveTxHash, HtgUnconfirmedTxPo po, String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType, BigInteger gasPrice, boolean resend) throws Exception {
        // estimateGasLimit
        BigInteger estimateGas;
        try {
            EthEstimateGas estimateGasObj = htgWalletApi.ethEstimateGas(fromAddress, htgContext.MULTY_SIGN_ADDRESS(), txFunction);
            if (estimateGasObj.getError() != null) {
                String error = estimateGasObj.getError().getMessage();
                logger().error("[{}]Transaction estimation GasLimit Failure, reason for failure: {}", txType, error);
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, "estimate GasLimit Failure, " + error);
            } else {
                estimateGas = estimateGasObj.getAmountUsed();
            }
        } catch (Exception e) {
            if (e instanceof NulsException && ConverterUtil.isInsufficientSignature((NulsException) e)) {
                List<HeterogeneousSign> regainSignatures = htgContext.getConverterCoreApi().regainSignatures(htgContext.NERVE_CHAINID(), nerveTxHash, htgContext.getConfig().getChainId());
                if (regainSignatures.size() > HtgConstant.MAX_MANAGERS) {
                    logger().warn("Obtaining Byzantine signature exceeds the maximum value, Obtain numerical values: {}, Maximum value: {}", regainSignatures.size(), HtgConstant.MAX_MANAGERS);
                    regainSignatures = regainSignatures.subList(0, HtgConstant.MAX_MANAGERS);
                }
                StringBuilder builder = new StringBuilder(HtgConstant.HEX_PREFIX);
                regainSignatures.stream().forEach(signature -> builder.append(HexUtil.encode(signature.getSignature())));
                List<Type> typeList = txFunction.getInputParameters();
                List<Type> newTypeList = new ArrayList<>(typeList);
                newTypeList.remove(newTypeList.size() - 1);
                newTypeList.add(new DynamicBytes(Numeric.hexStringToByteArray(builder.toString())));
                txFunction = new Function(txFunction.getName(), newTypeList, List.of(new TypeReference<Type>(){}));
                EthEstimateGas estimateGasObj = htgWalletApi.ethEstimateGas(fromAddress, htgContext.MULTY_SIGN_ADDRESS(), txFunction);
                if (estimateGasObj.getError() != null) {
                    String error = estimateGasObj.getError().getMessage();
                    logger().error("[{}]Transaction estimation GasLimit Failure, reason for failure: {}", txType, error);
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, "estimate GasLimit Failure, " + error);
                } else {
                    estimateGas = estimateGasObj.getAmountUsed();
                }
            } else {
                throw e;
            }
        }

        if (logger().isDebugEnabled()) {
            logger().debug("Transaction type: {}, EstimatedGasLimit: {}", txType, estimateGas);
        }
        BigInteger gasLimit = estimateGas.add(htgContext.BASE_GAS_LIMIT());
        BigInteger nonce = null;
        // When attempting to retry the transaction, use the latest versionnonceValue, notpending nonce
        if (resend) {
            nonce = htgWalletApi.getLatestNonce(fromAddress);
        }
        HtgSendTransactionPo htSendTransactionPo = htgWalletApi.callContract(fromAddress, priKey, htgContext.MULTY_SIGN_ADDRESS(), gasLimit, txFunction, BigInteger.ZERO, gasPrice, nonce);
        String htTxHash = htSendTransactionPo.getTxHash();
        // dockinglaunchhtgRecord transaction relationships during transactionsdbIn, and save the currently usednonceIn the relationship table, if there is any reasonpriceIf there is a need to resend the transaction without packaging it too low, the current one used will be taken outnonceResend transaction
        htgTxRelationStorageService.save(htTxHash, nerveTxHash, htSendTransactionPo);
        // The current node has been sent outhtgtransaction
        htgInvokeTxHelper.saveSentEthTx(nerveTxHash);

        // Save unconfirmed transactions
        po.setTxHash(htTxHash);
        po.setFrom(fromAddress);
        po.setTxType(txType);
        htgUnconfirmedTxStorageService.save(po);
        htgContext.UNCONFIRMED_TX_QUEUE().offer(po);
        // Monitor the packaging status of this transaction
        htgListener.addListeningTx(htTxHash);
        logger().info("NerveNetwork oriented {} Network transmission [{}] transaction, nerveTxHash: {}, details: {}", htgContext.getConfig().getSymbol(), txType, nerveTxHash, po.toString());
        return htTxHash;
    }

    private String _createOrSignWithdrawTxII(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData, boolean checkOrder) throws NulsException {
        try {
            if (!htgContext.isAvailableRPC()) {
                logger().error("[{}] network RPC Unavailable, pause this task", htgContext.getConfig().getSymbol());
                throw new NulsException(ConverterErrorCode.HTG_RPC_UNAVAILABLE);
            }
            logger().info("Preparing to send withdrawal {} Transactions, nerveTxHash: {}, signatureData: {}", htgContext.getConfig().getSymbol(), nerveTxHash, signatureData);
            // Transaction preparation
            HtgWaitingTxPo waitingPo = new HtgWaitingTxPo();
            HtgAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.WITHDRAW, waitingPo);
            // Save transaction call parameters and set waiting time to end
            htgInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, toAddress, value, assetId, signatureData, account.getOrder(), waitingPo);
            if (account.isEmpty()) {
                return EMPTY_STRING;
            }
            // When checking the order, do not send transactions that are not in the first place
            if (checkOrder && !checkFirstOrder(account.getOrder())) {
                logger().info("Non primary non transaction, order: {}", account.getOrder());
                return EMPTY_STRING;
            }
            // Obtain administrator account
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            // Business validation
            HtgUnconfirmedTxPo po = new HtgUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            boolean isContractAsset = assetId > 1;
            String contractAddressERC20;
            if (isContractAsset) {
                contractAddressERC20 = htgERC20Helper.getContractAddressByAssetId(assetId);
                htgERC20Helper.loadERC20(contractAddressERC20, po);
            } else {
                contractAddressERC20 = HtgConstant.ZERO_ADDRESS;
                po.setDecimals(htgContext.getConfig().getDecimals());
                po.setAssetId(htgContext.HTG_ASSET_ID());
            }
            // Check if it isNERVEAsset boundERC20If yes, check if the customized item has already been registered in the multi signed contractERC20Otherwise, the withdrawal will be abnormal
            if (htgContext.getConverterCoreApi().isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                    && !htgParseTxHelper.isMinterERC20(po.getContractAddress())) {
                logger().warn("[{}] Illegal {} Online withdrawal transactions, ERC20 [{}] Bound NERVE Assets, but not registered in the contract", nerveTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
                throw new NulsException(ConverterErrorCode.NOT_BIND_ASSET);
            }
            // Convert the address to lowercase
            toAddress = toAddress.toLowerCase();
            po.setTo(toAddress);
            po.setValue(value);
            po.setIfContractAsset(isContractAsset);
            if (isContractAsset) {
                po.setContractAddress(contractAddressERC20);
            }
            Function createOrSignWithdrawFunction = HtgUtil.getCreateOrSignWithdrawFunction(nerveTxHash, toAddress, value, isContractAsset, contractAddressERC20, signatureData);
            // calculateGasPrice
            IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
            BigDecimal gasPrice = new BigDecimal(htgContext.getEthGasPrice());
            // Check the withdrawal fee for the new mechanism only after reaching the specified height
            if (coreApi.isSupportNewMechanismOfWithdrawalFee()) {
                BigDecimal nvtAmount = new BigDecimal(coreApi.getFeeOfWithdrawTransaction(nerveTxHash).getFee());
                gasPrice = this.calcGasPriceOfWithdrawByOtherMainAsset(AssetName.NVT, nvtAmount, po.getAssetId());
                if (gasPrice == null) {
                    throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
                }
                gasPrice = HtgUtil.calcNiceGasPriceOfWithdraw(htgContext.ASSET_NAME(), new BigDecimal(htgContext.getEthGasPrice()), gasPrice);
            }
            // Send out transactions after verifying the contract
            String htTxHash = this.createTxComplete(nerveTxHash, po, fromAddress, priKey, createOrSignWithdrawFunction, HeterogeneousChainTxType.WITHDRAW, gasPrice.toBigInteger(), !checkOrder);
            if (StringUtils.isNotBlank(htTxHash)) {
                // Record withdrawal transactions that have been transferred toHTGNetwork transmission
                htgPendingTxHelper.commitNervePendingWithdrawTx(nerveTxHash, htTxHash);
            }
            return htTxHash;
        } catch (Exception e) {
            if (e instanceof NulsException) {
                throw (NulsException) e;
            }
            logger().error(e);
            throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
        }
    }

    private String createOrSignWithdrawTxIIProtocol15(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData, boolean checkOrder) throws NulsException {
        try {
            if (!htgContext.isAvailableRPC()) {
                logger().error("[{}]network RPC Unavailable, pause this task", htgContext.getConfig().getSymbol());
                throw new NulsException(ConverterErrorCode.HTG_RPC_UNAVAILABLE);
            }
            logger().info("Preparing to send withdrawal {} Transactions, nerveTxHash: {}, signatureData: {}", htgContext.getConfig().getSymbol(), nerveTxHash, signatureData);
            // Transaction preparation
            HtgWaitingTxPo waitingPo = new HtgWaitingTxPo();
            HtgAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.WITHDRAW, waitingPo);
            // Save transaction call parameters and set waiting time to end
            htgInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, toAddress, value, assetId, signatureData, account.getOrder(), waitingPo);
            if (account.isEmpty()) {
                return EMPTY_STRING;
            }
            // When checking the order, do not send transactions that are not in the first place
            if (checkOrder && !checkFirstOrder(account.getOrder())) {
                logger().info("Non primary non transaction, order: {}", account.getOrder());
                return EMPTY_STRING;
            }
            // Obtain administrator account
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            // Business validation
            HtgUnconfirmedTxPo po = new HtgUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            boolean isContractAsset = assetId > 1;
            String contractAddressERC20;
            if (isContractAsset) {
                contractAddressERC20 = htgERC20Helper.getContractAddressByAssetId(assetId);
                htgERC20Helper.loadERC20(contractAddressERC20, po);
            } else {
                contractAddressERC20 = HtgConstant.ZERO_ADDRESS;
                po.setDecimals(htgContext.getConfig().getDecimals());
                po.setAssetId(htgContext.HTG_ASSET_ID());
            }
            // Check if it isNERVEAsset boundERC20If yes, check if the customized item has already been registered in the multi signed contractERC20Otherwise, the withdrawal will be abnormal
            if (htgContext.getConverterCoreApi().isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                    && !htgParseTxHelper.isMinterERC20(po.getContractAddress())) {
                logger().warn("[{}] Illegal {} Online withdrawal transactions, ERC20 [{}] Bound NERVE Assets, but not registered in the contract", nerveTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
                throw new NulsException(ConverterErrorCode.NOT_BIND_ASSET);
            }
            // Convert the address to lowercase
            toAddress = toAddress.toLowerCase();
            po.setTo(toAddress);
            po.setValue(value);
            po.setIfContractAsset(isContractAsset);
            if (isContractAsset) {
                po.setContractAddress(contractAddressERC20);
            }
            Function createOrSignWithdrawFunction = HtgUtil.getCreateOrSignWithdrawFunction(nerveTxHash, toAddress, value, isContractAsset, contractAddressERC20, signatureData);
            // calculateGasPrice
            // Check withdrawal fees
            IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
            BigDecimal gasPrice;
            WithdrawalTotalFeeInfo feeInfo = coreApi.getFeeOfWithdrawTransaction(nerveTxHash);
            if (feeInfo.isNvtAsset()) feeInfo.setHtgMainAssetName(AssetName.NVT);
            // When using other main assets of non withdrawal networks as transaction fees
            if (feeInfo.getHtgMainAssetName() != htgContext.ASSET_NAME()) {
                gasPrice = this.calcGasPriceOfWithdrawByOtherMainAsset(feeInfo.getHtgMainAssetName(), new BigDecimal(feeInfo.getFee()), po.getAssetId());
            } else {
                gasPrice = this.calcGasPriceOfWithdrawByMainAssetProtocol15(new BigDecimal(feeInfo.getFee()), po.getAssetId());
            }
            if (gasPrice == null) {
                throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
            }
            gasPrice = HtgUtil.calcNiceGasPriceOfWithdraw(htgContext.ASSET_NAME(), new BigDecimal(htgContext.getEthGasPrice()), gasPrice);

            // Send out transactions after verifying the contract
            String htTxHash = this.createTxComplete(nerveTxHash, po, fromAddress, priKey, createOrSignWithdrawFunction, HeterogeneousChainTxType.WITHDRAW, gasPrice.toBigInteger(), !checkOrder);
            if (StringUtils.isNotBlank(htTxHash)) {
                // Record withdrawal transactions that have been transferred toHTGNetwork transmission
                htgPendingTxHelper.commitNervePendingWithdrawTx(nerveTxHash, htTxHash);
            }
            return htTxHash;
        } catch (Exception e) {
            if (e instanceof NulsException) {
                throw (NulsException) e;
            }
            logger().error(e);
            throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
        }
    }

    private String createOrSignWithdrawTxIIProtocol22(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData, boolean checkOrder) throws NulsException {
        try {
            if (!htgContext.isAvailableRPC()) {
                logger().error("[{}]network RPC Unavailable, pause this task", htgContext.getConfig().getSymbol());
                throw new NulsException(ConverterErrorCode.HTG_RPC_UNAVAILABLE);
            }
            logger().info("Preparing to send withdrawal {} Transactions, nerveTxHash: {}, toAddress: {}, value: {}, assetId: {}, signatureData: {}", htgContext.getConfig().getSymbol(), nerveTxHash, toAddress, value, assetId, signatureData);
            // Transaction preparation
            HtgWaitingTxPo waitingPo = new HtgWaitingTxPo();
            HtgAccount account = this.createTxStartForWithdraw(nerveTxHash, HeterogeneousChainTxType.WITHDRAW, waitingPo);
            // Save transaction call parameters and set waiting time to end
            htgInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, toAddress, value, assetId, signatureData, account.getOrder(), waitingPo);
            if (account.isEmpty()) {
                return EMPTY_STRING;
            }
            // When checking the order, do not send transactions that are not in the first place
            if (checkOrder && !checkFirstOrder(account.getOrder())) {
                logger().info("Non primary non transaction, order: {}", account.getOrder());
                return EMPTY_STRING;
            }
            // Obtain administrator account
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            // Business validation
            HtgUnconfirmedTxPo po = new HtgUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            boolean isContractAsset = assetId > 1;
            String contractAddressERC20;
            if (isContractAsset) {
                contractAddressERC20 = htgERC20Helper.getContractAddressByAssetId(assetId);
                htgERC20Helper.loadERC20(contractAddressERC20, po);
            } else {
                contractAddressERC20 = HtgConstant.ZERO_ADDRESS;
                po.setDecimals(htgContext.getConfig().getDecimals());
                po.setAssetId(htgContext.HTG_ASSET_ID());
            }
            IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
            // Check if it isNERVEAsset boundERC20If yes, check if the customized item has already been registered in the multi signed contractERC20Otherwise, the withdrawal will be abnormal
            if (coreApi.isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                    && !htgParseTxHelper.isMinterERC20(po.getContractAddress())) {
                logger().warn("[{}] Illegal {} Online withdrawal transactions, ERC20 [{}] Bound NERVE Assets, but not registered in the contract", nerveTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
                throw new NulsException(ConverterErrorCode.NOT_BIND_ASSET);
            }
            // Convert the address to lowercase
            toAddress = toAddress.toLowerCase();
            po.setTo(toAddress);
            po.setValue(value);
            po.setIfContractAsset(isContractAsset);
            if (isContractAsset) {
                po.setContractAddress(contractAddressERC20);
            }
            // If the accuracy of cross chain assets is different, then the conversion accuracy
            value = coreApi.checkDecimalsSubtractedToNerveForWithdrawal(htgContext.HTG_CHAIN_ID(), assetId, value);
            Function createOrSignWithdrawFunction = HtgUtil.getCreateOrSignWithdrawFunction(nerveTxHash, toAddress, value, isContractAsset, contractAddressERC20, signatureData);
            // calculateGasPrice
            // Check withdrawal fees
            BigDecimal gasPrice;
            WithdrawalTotalFeeInfo feeInfo = coreApi.getFeeOfWithdrawTransaction(nerveTxHash);
            if (feeInfo.isNvtAsset()) feeInfo.setHtgMainAssetName(AssetName.NVT);
            // When using other main assets of non withdrawal networks as transaction fees
            BigDecimal feeAmount = new BigDecimal(coreApi.checkDecimalsSubtractedToNerveForWithdrawal(feeInfo.getHtgMainAssetName().chainId(), 1, feeInfo.getFee()));
            if (feeInfo.getHtgMainAssetName() != htgContext.ASSET_NAME()) {
                gasPrice = this.calcGasPriceOfWithdrawByOtherMainAsset(feeInfo.getHtgMainAssetName(), feeAmount, po.getAssetId());
            } else {
                gasPrice = this.calcGasPriceOfWithdrawByMainAssetProtocol15(feeAmount, po.getAssetId());
            }
            if (gasPrice == null) {
                throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
            }
            gasPrice = HtgUtil.calcNiceGasPriceOfWithdraw(htgContext.ASSET_NAME(), new BigDecimal(htgContext.getEthGasPrice()), gasPrice);

            // Send out transactions after verifying the contract
            String htTxHash = this.createTxComplete(nerveTxHash, po, fromAddress, priKey, createOrSignWithdrawFunction, HeterogeneousChainTxType.WITHDRAW, gasPrice.toBigInteger(), !checkOrder);
            if (StringUtils.isNotBlank(htTxHash)) {
                // Record withdrawal transactions that have been transferred toHTGNetwork transmission
                htgPendingTxHelper.commitNervePendingWithdrawTx(nerveTxHash, htTxHash);
            }
            return htTxHash;
        } catch (Exception e) {
            /*StackTraceElement[] stackTrace0 = e.getStackTrace();
            if (stackTrace0 != null) {
                for (StackTraceElement s : stackTrace0) {
                    logger().error("---pierre test0----, {}", s);
                }
            }
            if (e.getCause() != null) {
                StackTraceElement[] stackTrace = e.getCause().getStackTrace();
                if (stackTrace != null) {
                    for (StackTraceElement s : stackTrace) {
                        logger().error("---pierre test----, {}", s);
                    }
                }
                if (e.getCause().getCause() != null) {
                    StackTraceElement[] stackTrace1 = e.getCause().getCause().getStackTrace();
                    if (stackTrace1 != null) {
                        for (StackTraceElement s : stackTrace1) {
                            logger().error("---pierre test1----, {}", s);
                        }
                    }
                }
            }*/
            if (e instanceof NulsException) {
                throw (NulsException) e;
            }
            logger().error(e);
            if (e.getCause() != null) logger().error(e.getCause());
            throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
        }
    }

    private boolean isEnoughFeeOfWithdrawByOtherMainAsset(AssetName otherMainAssetName, BigDecimal otherMainAssetAmount, int hAssetId) {
        return this.calcGasPriceOfWithdrawByOtherMainAsset(otherMainAssetName, otherMainAssetAmount, hAssetId) != null;
    }

    private BigDecimal calcGasPriceOfWithdrawByOtherMainAsset(AssetName otherMainAssetName, BigDecimal otherMainAssetAmount, int hAssetId) {
        IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
        BigDecimal otherMainAssetUSD = coreApi.getUsdtPriceByAsset(otherMainAssetName);
        BigDecimal htgUSD = coreApi.getUsdtPriceByAsset(htgContext.ASSET_NAME());
        String otherSymbol = otherMainAssetName.toString();
        if(null == otherMainAssetUSD || null == htgUSD){
            logger().error("[{}][withdraw] Withdrawal fee calculation,Unable to obtain complete quotation. {}_USD:{}, {}_USD:{}", htgContext.getConfig().getSymbol(), otherSymbol, otherMainAssetUSD, htgContext.getConfig().getSymbol(), htgUSD);
            throw new NulsRuntimeException(ConverterErrorCode.DATA_NOT_FOUND);
        }
        BigDecimal gasPrice = HtgUtil.calcGasPriceOfWithdraw(otherMainAssetName, otherMainAssetUSD, otherMainAssetAmount, htgUSD, hAssetId, htgContext.GAS_LIMIT_OF_WITHDRAW());
        String gasPriceStr = gasPrice.divide(BigDecimal.TEN.pow(9)).toPlainString();
        if (gasPrice != null && gasPrice.toBigInteger().compareTo(htgContext.getEthGasPrice()) >= 0) {
            logger().info("[{}]The withdrawal fee is sufficient for the current network needsGasPrice: {} Gwei, Actual calculatedGasPrice: {} Gwei",
                    htgContext.getConfig().getSymbol(),
                    new BigDecimal(htgContext.getEthGasPrice()).divide(BigDecimal.TEN.pow(9)).toPlainString(),
                    gasPriceStr);
            return gasPrice;
        }
        BigDecimal otherMainAssetAmountCalc = HtgUtil.calcOtherMainAssetOfWithdraw(otherMainAssetName, otherMainAssetUSD, new BigDecimal(htgContext.getEthGasPrice()), htgUSD, hAssetId, htgContext.GAS_LIMIT_OF_WITHDRAW());
        logger().warn("[{}]Insufficient withdrawal fees, currently required by the networkGasPrice: {} Gwei, Actual calculatedGasPrice: {} Gwei, Total required{}: {}, User provided{}: {}, Additional Required{}: {}",
                htgContext.getConfig().getSymbol(),
                new BigDecimal(htgContext.getEthGasPrice()).divide(BigDecimal.TEN.pow(9)).toPlainString(),
                gasPriceStr,
                otherSymbol,
                otherMainAssetAmountCalc.movePointLeft(otherMainAssetName.decimals()).toPlainString(),
                otherSymbol,
                otherMainAssetAmount.movePointLeft(otherMainAssetName.decimals()).toPlainString(),
                otherSymbol,
                otherMainAssetAmountCalc.subtract(otherMainAssetAmount).movePointLeft(otherMainAssetName.decimals()).toPlainString());
        return null;
    }

    private BigDecimal calcGasPriceOfWithdrawByMainAssetProtocol15(BigDecimal amount, int hAssetId) {
        BigDecimal gasPrice = HtgUtil.calcGasPriceOfWithdrawByMainAssetProtocol15(amount, hAssetId, htgContext.GAS_LIMIT_OF_WITHDRAW());
        String gasPriceStr = gasPrice.divide(BigDecimal.TEN.pow(9)).toPlainString();
        if (gasPrice != null && gasPrice.toBigInteger().compareTo(htgContext.getEthGasPrice()) >= 0) {
            logger().info("[{}]The withdrawal fee is sufficient for the current network needsGasPrice: {} Gwei, Actual calculatedGasPrice: {} Gwei",
                    htgContext.getConfig().getSymbol(),
                    new BigDecimal(htgContext.getEthGasPrice()).divide(BigDecimal.TEN.pow(9)).toPlainString(),
                    gasPriceStr);
            return gasPrice;
        }
        BigDecimal amountCalc = HtgUtil.calcMainAssetOfWithdrawProtocol15(new BigDecimal(htgContext.getEthGasPrice()), hAssetId, htgContext.GAS_LIMIT_OF_WITHDRAW());
        int decimals = htgContext.getConfig().getDecimals();
        String symbol = htgContext.getConfig().getSymbol();
        logger().warn("[{}]Insufficient withdrawal fees, currently required by the networkGasPrice: {} Gwei, Actual calculatedGasPrice: {} Gwei, Total required{}: {}, User provided{}: {}, Additional Required{}: {}",
                htgContext.getConfig().getSymbol(),
                new BigDecimal(htgContext.getEthGasPrice()).divide(BigDecimal.TEN.pow(9)).toPlainString(),
                gasPriceStr,
                symbol,
                amountCalc.movePointLeft(decimals).toPlainString(),
                symbol,
                amount.movePointLeft(decimals).toPlainString(),
                symbol,
                amountCalc.subtract(amount).movePointLeft(decimals).toPlainString()
        );
        return null;
    }

    private String getKeystorePath() {
        if (StringUtils.isBlank(keystorePath)) {
            keystorePath = converterConfig.getDataPath() + File.separator + "keystore" + File.separator + "backup" + File.separator + "eth";
        }
        return keystorePath;
    }

}

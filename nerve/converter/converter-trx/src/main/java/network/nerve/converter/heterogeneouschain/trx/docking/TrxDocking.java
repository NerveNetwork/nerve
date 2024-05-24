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
package network.nerve.converter.heterogeneouschain.trx.docking;

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
import network.nerve.converter.heterogeneouschain.lib.docking.HtgDocking;
import network.nerve.converter.heterogeneouschain.lib.helper.*;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgAccount;
import network.nerve.converter.heterogeneouschain.lib.model.HtgERC20Po;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.model.HtgWaitingTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.*;
import network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant;
import network.nerve.converter.heterogeneouschain.trx.context.TrxContext;
import network.nerve.converter.heterogeneouschain.trx.core.TrxWalletApi;
import network.nerve.converter.heterogeneouschain.trx.helper.TrxAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.trx.helper.TrxERC20Helper;
import network.nerve.converter.heterogeneouschain.trx.helper.TrxParseTxHelper;
import network.nerve.converter.heterogeneouschain.trx.model.TrxAccount;
import network.nerve.converter.heterogeneouschain.trx.model.TrxEstimateSun;
import network.nerve.converter.heterogeneouschain.trx.model.TrxSendTransactionPo;
import network.nerve.converter.heterogeneouschain.trx.utils.TrxUtil;
import network.nerve.converter.model.HeterogeneousSign;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.utils.ConverterUtil;
import org.tron.trident.abi.TypeReference;
import org.tron.trident.abi.datatypes.Address;
import org.tron.trident.abi.datatypes.DynamicBytes;
import org.tron.trident.abi.datatypes.Function;
import org.tron.trident.abi.datatypes.Type;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Contract;
import org.tron.trident.proto.Response;
import org.tron.trident.utils.Base58Check;
import org.tron.trident.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static io.protostuff.ByteString.EMPTY_STRING;
import static network.nerve.converter.heterogeneouschain.lib.context.HtgConstant.ZERO_BYTES;


/**
 * @author: Mimi
 * @date: 2020-08-28
 */
public class TrxDocking extends HtgDocking implements IHeterogeneousChainDocking, BeanInitial {

    private HeterogeneousAssetInfo mainAsset;

    protected TrxWalletApi trxWalletApi;
    protected TrxAnalysisTxHelper trxAnalysisTxHelper;
    protected TrxParseTxHelper trxParseTxHelper;
    protected TrxERC20Helper trxERC20Helper;

    protected HtgListener htgListener;
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
    protected HtgResendHelper htgResendHelper;
    protected HtgPendingTxHelper htgPendingTxHelper;
    private HtgContext htgContext;

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
        return TrxUtil.genTrxAddressByCompressedPublickey(compressedPublicKey);
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
            if (Arrays.equals(account.getPriKey(), Numeric.hexStringToByteArray(priKey))) {
                account.setPriKey(new byte[0]);
                return account;
            }
        }
        if (!FormatValidUtils.validPassword(password)) {
            logger().error("password format wrong");
            throw new NulsException(ConverterErrorCode.PASSWORD_FORMAT_WRONG);
        }
        TrxAccount account = TrxUtil.createAccount(priKey);
        account.encrypt(password);
        try {
            htgAccountStorageService.save(account);
            // Overwrite this address as the virtual bank administrator address
            htgContext.SET_ADMIN_ADDRESS(account.getAddress());
            htgContext.SET_ADMIN_ADDRESS_PUBLIC_KEY(account.getCompressedPublicKey());
            htgContext.SET_ADMIN_ADDRESS_PASSWORD(password);
            logger().info("towards{}Heterogeneous component import node block address information, address: [{}]", htgContext.getConfig().getSymbol(), account.getAddress());
            return account;
        } catch (Exception e) {
            throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR, e);
        }
    }

    private HeterogeneousAccount _importAccountByPubKey(String pubKey, String password) throws NulsException {
        if (StringUtils.isNotBlank(htgContext.ADMIN_ADDRESS())) {
            HtgAccount account = htgAccountStorageService.findByAddress(htgContext.ADMIN_ADDRESS());
            if (Arrays.equals(account.getPubKey(), Numeric.hexStringToByteArray(pubKey))) {
                account.setPriKey(ZERO_BYTES);
                account.setEncryptedPriKey(ZERO_BYTES);
                return account;
            }
        }
        if (!FormatValidUtils.validPassword(password)) {
            logger().error("password format wrong");
            throw new NulsException(ConverterErrorCode.PASSWORD_FORMAT_WRONG);
        }
        HtgAccount account = TrxUtil.createAccountByPubkey(pubKey);
        try {
            htgAccountStorageService.save(account);
            // Overwrite this address as the virtual bank administrator address
            htgContext.SET_ADMIN_ADDRESS(account.getAddress());
            htgContext.SET_ADMIN_ADDRESS_PUBLIC_KEY(account.getCompressedPublicKey());
            htgContext.SET_ADMIN_ADDRESS_PASSWORD(password);
            logger().info("towards{}Heterogeneous component import node block address information, address: [{}]", htgContext.getConfig().getSymbol(), account.getAddress());
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
        try {
            Base58Check.base58ToBytes(address);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public BigDecimal getBalance(String address) {
        BigDecimal ethBalance = null;
        try {
            ethBalance = trxWalletApi.getBalance(address);
        } catch (Exception e) {
            logger().error(e);
        }
        if (ethBalance == null) {
            return BigDecimal.ZERO;
        }
        return TrxUtil.convertSunToTrx(ethBalance.toBigInteger());
    }

    @Override
    public void updateMultySignAddress(String multySignAddress) throws Exception {
        logger().info("{}Update multiple contract addresses, old: {}, new: {}", htgContext.getConfig().getSymbol(), htgContext.MULTY_SIGN_ADDRESS(), multySignAddress);
        //multySignAddress = multySignAddress.toLowerCase();
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
        HtgERC20Po erc20Po = trxERC20Helper.getERC20ByContractAddress(contractAddress);
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
        HtgERC20Po erc20Po = trxERC20Helper.getERC20ByAssetId(assetId);
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
            List<Type> symbolResult = trxWalletApi.callViewFunction(contractAddress, TrxUtil.getSymbolERC20Function());
            if (symbolResult.isEmpty()) {
                return false;
            }
            String _symbol = symbolResult.get(0).getValue().toString();
            if (!_symbol.equals(symbol)) {
                return false;
            }
        //}
        List<Type> decimalsResult = trxWalletApi.callViewFunction(contractAddress, TrxUtil.getDecimalsERC20Function());
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
        trxERC20Helper.saveHeterogeneousAssetInfos(assetInfos);
    }

    @Override
    public void rollbackHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception {
        if (assetInfos == null || assetInfos.isEmpty()) {
            return;
        }
        trxERC20Helper.rollbackHeterogeneousAssetInfos(assetInfos);
    }

    @Override
    public void txConfirmedCompleted(String htTxHash, Long blockHeight, String nerveTxHash, byte[] confirmTxRemark) throws Exception {
        logger().info("NerveNetwork confirmation{}transaction Nerver hash: {}", htgContext.getConfig().getSymbol(), nerveTxHash);
        if (StringUtils.isBlank(htTxHash)) {
            logger().warn("Empty htTxHash warning");
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
        logger().info("NerveNetwork impact[{}]{}transaction[{}]Confirm completion, nerveheight: {}, nerver hash: {}", txPo.getTxType(), htgContext.getConfig().getSymbol(), txPo.getTxHash(), blockHeight, txPo.getNerveTxHash());
        boolean delete = txPo.isDelete();
        Long deletedHeight = txPo.getDeletedHeight();
        htgUnconfirmedTxStorageService.update(txPo, update -> {
            update.setDelete(delete);
            update.setDeletedHeight(deletedHeight);
        });
        // Persisted state successfullynerveTx
        if (StringUtils.isNotBlank(nerveTxHash)) {
            logger().debug("Persisted state successfullynerveTxHash: {}", nerveTxHash);
            htgInvokeTxHelper.saveSuccessfulNerve(nerveTxHash);
        }
    }

    @Override
    public HeterogeneousTransactionInfo getDepositTransaction(String txHash) throws Exception {
        // fromDBObtain data from, if unable to obtain, then go toHTGObtained from the network
        HeterogeneousTransactionInfo txInfo = htgTxStorageService.findByTxHash(txHash);
        if (txInfo != null) {
            txInfo.setTxType(HeterogeneousChainTxType.DEPOSIT);
        } else {
            txInfo = trxParseTxHelper.parseDepositTransaction(txHash);
            if (txInfo == null) {
                return null;
            }
        }
        if (txInfo.getTxTime() == null) {
            Response.TransactionInfo txReceipt = trxWalletApi.getTransactionReceipt(txHash);
            txInfo.setTxTime(txReceipt.getBlockTimeStamp());
        }
        return txInfo;
    }

    @Override
    public HeterogeneousTransactionInfo getWithdrawTransaction(String txHash) throws Exception {
        // fromDBObtain data from, if unable to obtain, then go toHTGObtained from the network
        HeterogeneousTransactionInfo txInfo = htgTxStorageService.findByTxHash(txHash);
        if (txInfo != null) {
            txInfo.setTxType(HeterogeneousChainTxType.WITHDRAW);
            Long txTime = txInfo.getTxTime();
            if (StringUtils.isBlank(txInfo.getFrom())) {
                txInfo = trxParseTxHelper.parseWithdrawTransaction(txHash);
                if (txInfo == null) {
                    return null;
                }
                txInfo.setTxTime(txTime);
            }
        } else {
            txInfo = trxParseTxHelper.parseWithdrawTransaction(txHash);
            if (txInfo == null) {
                return null;
            }
        }
        if (txInfo.getTxTime() == null) {
            Response.TransactionInfo txReceipt = trxWalletApi.getTransactionReceipt(txHash);
            txInfo.setTxTime(txReceipt.getBlockTimeStamp());
        }
        if (txInfo.getTxTime() != null && txInfo.getTxTime().longValue() > 1000000000000l && htgContext.getConverterCoreApi().isProtocol21()) {
            txInfo.setTxTime(txInfo.getTxTime() / 1000);
        }
        return txInfo;
    }

    @Override
    public HeterogeneousConfirmedInfo getConfirmedTxInfo(String txHash) throws Exception {
        HeterogeneousConfirmedInfo info = new HeterogeneousConfirmedInfo();
        // fromDBObtain data from, if unable to obtain, then go toHTGObtained from the network
        HeterogeneousTransactionInfo txInfo = htgTxStorageService.findByTxHash(txHash);
        String from;
        Long txTime = null;
        List<HeterogeneousAddress> signers = null;
        Response.TransactionInfo txReceipt = null;
        if (txInfo != null && StringUtils.isNotBlank(txInfo.getFrom())) {
            from = txInfo.getFrom();
            signers = txInfo.getSigners();
            txTime = txInfo.getTxTime();
        } else {
            Chain.Transaction tx = trxWalletApi.getTransactionByHash(txHash);
            Chain.Transaction.Contract contract = tx.getRawData().getContract(0);
            Contract.TriggerSmartContract tg = Contract.TriggerSmartContract.parseFrom(contract.getParameter().getValue());
            from = TrxUtil.ethAddress2trx(tg.getOwnerAddress().toByteArray());
        }
        if(signers == null || signers.isEmpty()) {
            txReceipt = txReceipt == null ? trxWalletApi.getTransactionReceipt(txHash) : txReceipt;
            signers = trxParseTxHelper.parseSigners(txReceipt, from);
        }
        if (txTime == null) {
            txReceipt = txReceipt == null ? trxWalletApi.getTransactionReceipt(txHash) : txReceipt;
            txTime = txReceipt.getBlockTimeStamp();
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
        logger().info("validate{}Online virtual banking change transactions,nerveTxHash: {}, signatureData: {}", htgContext.getConfig().getSymbol(), nerveTxHash, signatureData);
        try {
            // towardsHTGNetwork request verification
            boolean isCompleted = trxParseTxHelper.isCompletedTransaction(nerveTxHash);
            if (isCompleted) {
                logger().info("[{}]transaction[{}]Completed", HeterogeneousChainTxType.CHANGE, nerveTxHash);
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
                //add = add.toLowerCase();
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
                //remove = remove.toLowerCase();
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
            Function txFunction = TrxUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, orginTxCount, signatureData);
            // Verify the legality of contract transactions
            TrxEstimateSun estimateSun = trxWalletApi.estimateSunUsed(fromAddress, htgContext.MULTY_SIGN_ADDRESS(), txFunction);
            if (estimateSun.isReverted()) {
                logger().error("[{}]Transaction verification failed, reason: {}", HeterogeneousChainTxType.CHANGE, estimateSun.getRevertReason());
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
            Chain.Transaction tx = trxWalletApi.getTransactionByHash(htTxHash);
            if (tx == null) {
                return false;
            }
            Chain.Transaction.Contract contract = tx.getRawData().getContract(0);
            Chain.Transaction.Contract.ContractType type = contract.getType();
            if (Chain.Transaction.Contract.ContractType.TriggerSmartContract == type &&
                    tx.getRet(0).getContractRet() != Chain.Transaction.Result.contractResult.SUCCESS) {
                logger().warn("Abnormal transaction[0]: {}", htTxHash);
                return false;
            }

            Response.TransactionInfo txReceipt = trxWalletApi.getTransactionReceipt(htTxHash);
            if (!TrxUtil.checkTransactionSuccess(txReceipt)) {
                logger().warn("Abnormal transaction[1]: {}", htTxHash);
                return false;
            }
            HeterogeneousTransactionInfo txInfo = trxParseTxHelper.parseDepositTransaction(tx, txReceipt);
            if (txInfo == null) {
                logger().warn("Abnormal transaction[2]: {}", htTxHash);
                return false;
            }
            Long blockHeight = txReceipt.getBlockNumber();
            Long txTime = txReceipt.getBlockTimeStamp();
            trxAnalysisTxHelper.analysisTx(tx, txTime, blockHeight);
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
            Chain.Transaction tx = trxWalletApi.getTransactionByHash(htTxHash);
            if (tx == null) {
                return false;
            }
            Chain.Transaction.Contract contract = tx.getRawData().getContract(0);
            Chain.Transaction.Contract.ContractType type = contract.getType();
            if (Chain.Transaction.Contract.ContractType.TriggerSmartContract == type &&
                    tx.getRet(0).getContractRet() != Chain.Transaction.Result.contractResult.SUCCESS) {
                logger().warn("Abnormal transaction[0]: {}", htTxHash);
                return false;
            }

            Response.TransactionInfo txReceipt = trxWalletApi.getTransactionReceipt(htTxHash);
            if (!TrxUtil.checkTransactionSuccess(txReceipt)) {
                logger().warn("Abnormal transaction[1]: {}", htTxHash);
                return false;
            }
            Long blockHeight = txReceipt.getBlockNumber();
            Long txTime = txReceipt.getBlockTimeStamp();
            htgUnconfirmedTxStorageService.deleteByTxHash(htTxHash);
            trxAnalysisTxHelper.analysisTx(tx, txTime, blockHeight);
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
        // Obtain administrator account
        boolean isContractAsset = assetId > 1;
        String contractAddressERC20;
        if (isContractAsset) {
            contractAddressERC20 = trxERC20Helper.getContractAddressByAssetId(assetId);
        } else {
            contractAddressERC20 = HtgConstant.ZERO_ADDRESS;
        }
        // If the accuracy of cross chain assets is different, then the conversion accuracy
        value = htgContext.getConverterCoreApi().checkDecimalsSubtractedToNerveForWithdrawal(htgContext.HTG_CHAIN_ID(), assetId, value);
        if (htgContext.getConverterCoreApi().isLocalSign()) {
            TrxAccount account = (TrxAccount) this.getAccount(htgContext.ADMIN_ADDRESS());
            account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            String vHash = TrxUtil.encoderWithdraw(htgContext, txHash, toAddress, value, isContractAsset, contractAddressERC20, htgContext.VERSION());
            logger().debug("Withdrawal signature data: {}, {}, {}, {}, {}, {}", txHash, toAddress, value, isContractAsset, contractAddressERC20, htgContext.VERSION());
            logger().debug("Withdrawal signaturevHash: {}, nerveTxHash: {}", vHash, txHash);
            return TrxUtil.dataSign(vHash, priKey);
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
            //add = add.toLowerCase();
            addAddresses[a] = add;
            if (!addSet.add(add)) {
                logger().error("Duplicate list of addresses to be added");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_2);
            }
        }
        Set<String> removeSet = new HashSet<>();
        for (int r = 0, removeSize = removeAddresses.length; r < removeSize; r++) {
            String remove = removeAddresses[r];
            //remove = remove.toLowerCase();
            removeAddresses[r] = remove;
            if (!removeSet.add(remove)) {
                logger().error("Duplicate list of pending exits");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_4);
            }
        }
        if (htgContext.getConverterCoreApi().isLocalSign()) {
            // Obtain administrator account
            TrxAccount account = (TrxAccount) this.getAccount(htgContext.ADMIN_ADDRESS());
            account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            String vHash = TrxUtil.encoderChange(htgContext, nerveTxHash, addAddresses, orginTxCount, removeAddresses, htgContext.VERSION());
            logger().debug("Change the signature of the transactionvHash: {}, nerveTxHash: {}", vHash, nerveTxHash);
            return TrxUtil.dataSign(vHash, priKey);
        } else {
            return htgContext.getConverterCoreApi().signChangeByMachine(htgContext.getConfig().getChainIdOnHtgNetwork(), htgContext.ADMIN_ADDRESS_PUBLIC_KEY(),
                    nerveTxHash, addAddresses, orginTxCount, removeAddresses, htgContext.VERSION());
        }
    }

    @Override
    public String signUpgradeII(String nerveTxHash, String upgradeContract) throws NulsException {
        if (htgContext.getConverterCoreApi().isLocalSign()) {
            // Obtain administrator account
            TrxAccount account = (TrxAccount) this.getAccount(htgContext.ADMIN_ADDRESS());
            account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            String vHash = TrxUtil.encoderUpgrade(htgContext, nerveTxHash, upgradeContract, htgContext.VERSION());
            logger().debug("Upgrade the signature of the transactionvHash: {}, nerveTxHash: {}", vHash, nerveTxHash);
            return TrxUtil.dataSign(vHash, priKey);
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
            contractAddressERC20 = trxERC20Helper.getContractAddressByAssetId(assetId);
        } else {
            contractAddressERC20 = HtgConstant.ZERO_ADDRESS;
        }
        // If the accuracy of cross chain assets is different, then the conversion accuracy
        value = htgContext.getConverterCoreApi().checkDecimalsSubtractedToNerveForWithdrawal(htgContext.HTG_CHAIN_ID(), assetId, value);
        String vHash = TrxUtil.encoderWithdraw(htgContext, txHash, toAddress, value, isContractAsset, contractAddressERC20, htgContext.VERSION());
        logger().debug("[Verify signature] Withdrawal data: {}, {}, {}, {}, {}, {}", txHash, toAddress, value, isContractAsset, contractAddressERC20, htgContext.VERSION());
        logger().debug("[Verify signature] WithdrawalvHash: {}", vHash);
        return TrxUtil.verifySign(signAddress, vHash, signed);
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
            //add = add.toLowerCase();
            addAddresses[a] = add;
        }
        for (int r = 0, removeSize = removeAddresses.length; r < removeSize; r++) {
            String remove = removeAddresses[r];
            //remove = remove.toLowerCase();
            removeAddresses[r] = remove;
        }
        String vHash = TrxUtil.encoderChange(htgContext, nerveTxHash, addAddresses, orginTxCount, removeAddresses, htgContext.VERSION());
        return TrxUtil.verifySign(signAddress, vHash, signed);
    }

    @Override
    public Boolean verifySignUpgradeII(String signAddress, String txHash, String upgradeContract, String signed) throws NulsException {
        // Convert the address to lowercase
        //upgradeContract = upgradeContract.toLowerCase();
        String vHash = TrxUtil.encoderUpgrade(htgContext, txHash, upgradeContract, htgContext.VERSION());
        return TrxUtil.verifySign(signAddress, vHash, signed);
    }

    @Override
    public boolean isEnoughNvtFeeOfWithdraw(BigDecimal nvtAmount, int hAssetId) {
        return this.isEnoughFeeOfWithdrawByOtherMainAsset(AssetName.NVT, nvtAmount, hAssetId);
    }

    @Override
    public boolean isEnoughFeeOfWithdrawByMainAssetProtocol15(AssetName assetName, BigDecimal amount, int hAssetId) {
        // Can use the main assets of other heterogeneous networks as transaction fees, For example, withdrawal toTRX, PaymentBNBAs a handling fee
        if (assetName == htgContext.ASSET_NAME()) {
            return this.calcOtherMainAssetOfWithdrawByMainAssetProtocol15(amount, hAssetId) != null;
        } else {
            return this.isEnoughFeeOfWithdrawByOtherMainAsset(assetName, amount, hAssetId);
        }
    }

    @Override
    public boolean isMinterERC20(String erc20) throws Exception {
        return trxParseTxHelper.isMinterERC20(erc20);
    }

    @Override
    public String cancelHtgTx(String nonce, String priceGWei) throws Exception {
        return EMPTY_STRING;
    }

    @Override
    public String getAddressString(byte[] addressBytes) {
        return TrxUtil.ethAddress2trx(addressBytes);
    }

    @Override
    public byte[] getAddressBytes(String addressString) {
        return TrxUtil.address2Bytes(addressString);
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
        return trxParseTxHelper.parseOneClickCrossChainData(extend, logger());
    }

    @Override
    public HeterogeneousAddFeeCrossChainData parseAddFeeCrossChainData(String extend) {
        return trxParseTxHelper.parseAddFeeCrossChainData(extend, logger());
    }

    @Override
    public HeterogeneousChainGasInfo getHeterogeneousChainGasInfo() {
        TrxContext trxContext = (TrxContext) htgContext;
        if (trxContext.gasInfo == null) {
            trxContext.gasInfo = new HeterogeneousChainGasInfo();
            trxContext.gasInfo.setGasLimitOfWithdraw(htgContext.GAS_LIMIT_OF_WITHDRAW().toString());
            trxContext.gasInfo.setExtend(trxContext.SUN_PER_ENERGY.toString());
        }
        return trxContext.gasInfo;
    }

    @Override
    public boolean isAvailableRPC() {
        return htgContext.isAvailableRPC();
    }

    @Override
    public BigInteger currentGasPrice() {
        return htgContext.getEthGasPrice();
    }

    public String createOrSignWithdrawTxII(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData, boolean checkOrder) throws NulsException {
        if (htgContext.getConverterCoreApi().isProtocol22()) {
            // protocol22: Support cross chain assets with different accuracies
            return createOrSignWithdrawTxIIProtocol22(nerveTxHash, toAddress, value, assetId, signatureData, checkOrder);
        } else {
            return _createOrSignWithdrawTxII(nerveTxHash, toAddress, value, assetId, signatureData, checkOrder);
        }
    }

    private String _createOrSignWithdrawTxII(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData, boolean checkOrder) throws NulsException {
        try {
            if (!htgContext.isAvailableRPC()) {
                logger().error("[{}]networkRPCUnavailable, pause this task", htgContext.getConfig().getSymbol());
                throw new NulsException(ConverterErrorCode.HTG_RPC_UNAVAILABLE);
            }
            logger().info("Preparing to send withdrawal{}Transactions,nerveTxHash: {}, signatureData: {}", htgContext.getConfig().getSymbol(), nerveTxHash, signatureData);
            // Transaction preparation
            HtgWaitingTxPo waitingPo = new HtgWaitingTxPo();
            TrxAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.WITHDRAW, waitingPo);
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
                contractAddressERC20 = trxERC20Helper.getContractAddressByAssetId(assetId);
                trxERC20Helper.loadERC20(contractAddressERC20, po);
            } else {
                contractAddressERC20 = TrxConstant.ZERO_ADDRESS_TRX;
                po.setDecimals(htgContext.getConfig().getDecimals());
                po.setAssetId(htgContext.HTG_ASSET_ID());
            }
            // Check if it isNERVEAsset boundERC20If yes, check if the customized item has already been registered in the multi signed contractERC20Otherwise, the withdrawal will be abnormal
            if (htgContext.getConverterCoreApi().isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                    && !trxParseTxHelper.isMinterERC20(po.getContractAddress())) {
                logger().warn("[{}]Illegal{}Online withdrawal transactions, ERC20[{}]BoundNERVEAssets, but not registered in the contract", nerveTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
                throw new NulsException(ConverterErrorCode.NOT_BIND_ASSET);
            }
            po.setTo(toAddress);
            po.setValue(value);
            po.setIfContractAsset(isContractAsset);
            if (isContractAsset) {
                po.setContractAddress(contractAddressERC20);
            }
            Function createOrSignWithdrawFunction = TrxUtil.getCreateOrSignWithdrawFunction(nerveTxHash, toAddress, value, isContractAsset, contractAddressERC20, signatureData);

            // Inspection fees
            IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
            WithdrawalTotalFeeInfo feeInfo = coreApi.getFeeOfWithdrawTransaction(nerveTxHash);
            BigDecimal feeAmount = new BigDecimal(feeInfo.getFee());
            if (feeInfo.isNvtAsset()) feeInfo.setHtgMainAssetName(AssetName.NVT);
            BigDecimal need;
            // // When using other main assets of non withdrawal networks as transaction fees
            if (feeInfo.getHtgMainAssetName() != htgContext.ASSET_NAME()) {
                need = this.calcOtherMainAssetOfWithdrawByOtherMainAsset(feeInfo.getHtgMainAssetName(), feeAmount, po.getAssetId());
            } else {
                need = this.calcOtherMainAssetOfWithdrawByMainAssetProtocol15(feeAmount, po.getAssetId());
            }
            if (need == null) {
                throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
            }

            // Send out transactions after verifying the contract
            String htTxHash = this.createTxComplete(nerveTxHash, po, fromAddress, priKey, createOrSignWithdrawFunction, HeterogeneousChainTxType.WITHDRAW);
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
                logger().error("[{}]networkRPCUnavailable, pause this task", htgContext.getConfig().getSymbol());
                throw new NulsException(ConverterErrorCode.HTG_RPC_UNAVAILABLE);
            }
            logger().info("Preparing to send withdrawal{}Transactions,nerveTxHash: {}, signatureData: {}", htgContext.getConfig().getSymbol(), nerveTxHash, signatureData);
            // Transaction preparation
            HtgWaitingTxPo waitingPo = new HtgWaitingTxPo();
            TrxAccount account = this.createTxStartForWithdraw(nerveTxHash, HeterogeneousChainTxType.WITHDRAW, waitingPo);
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
                contractAddressERC20 = trxERC20Helper.getContractAddressByAssetId(assetId);
                trxERC20Helper.loadERC20(contractAddressERC20, po);
            } else {
                contractAddressERC20 = TrxConstant.ZERO_ADDRESS_TRX;
                po.setDecimals(htgContext.getConfig().getDecimals());
                po.setAssetId(htgContext.HTG_ASSET_ID());
            }
            IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
            // Check if it isNERVEAsset boundERC20If yes, check if the customized item has already been registered in the multi signed contractERC20Otherwise, the withdrawal will be abnormal
            if (coreApi.isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                    && !trxParseTxHelper.isMinterERC20(po.getContractAddress())) {
                logger().warn("[{}]Illegal{}Online withdrawal transactions, ERC20[{}]BoundNERVEAssets, but not registered in the contract", nerveTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
                throw new NulsException(ConverterErrorCode.NOT_BIND_ASSET);
            }
            po.setTo(toAddress);
            po.setValue(value);
            po.setIfContractAsset(isContractAsset);
            if (isContractAsset) {
                po.setContractAddress(contractAddressERC20);
            }
            // Cross chain asset accuracy varies, conversion accuracy
            value = htgContext.getConverterCoreApi().checkDecimalsSubtractedToNerveForWithdrawal(htgContext.HTG_CHAIN_ID(), assetId, value);
            Function createOrSignWithdrawFunction = TrxUtil.getCreateOrSignWithdrawFunction(nerveTxHash, toAddress, value, isContractAsset, contractAddressERC20, signatureData);

            // Inspection fees
            WithdrawalTotalFeeInfo feeInfo = coreApi.getFeeOfWithdrawTransaction(nerveTxHash);
            if (feeInfo.isNvtAsset()) feeInfo.setHtgMainAssetName(AssetName.NVT);
            BigDecimal feeAmount = new BigDecimal(coreApi.checkDecimalsSubtractedToNerveForWithdrawal(feeInfo.getHtgMainAssetName().chainId(), 1, feeInfo.getFee()));
            BigDecimal need;
            // // When using other main assets of non withdrawal networks as transaction fees
            if (feeInfo.getHtgMainAssetName() != htgContext.ASSET_NAME()) {
                need = this.calcOtherMainAssetOfWithdrawByOtherMainAsset(feeInfo.getHtgMainAssetName(), feeAmount, po.getAssetId());
            } else {
                need = this.calcOtherMainAssetOfWithdrawByMainAssetProtocol15(feeAmount, po.getAssetId());
            }
            if (need == null) {
                throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
            }

            // Send out transactions after verifying the contract
            String htTxHash = this.createTxComplete(nerveTxHash, po, fromAddress, priKey, createOrSignWithdrawFunction, HeterogeneousChainTxType.WITHDRAW);
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
            TrxAccount account = this.createTxStartForChange(nerveTxHash, addAddresses, removeAddresses, waitingPo);
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
                //add = add.toLowerCase();
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
                //remove = remove.toLowerCase();
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
            Function createOrSignManagerChangeFunction = TrxUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, orginTxCount, signatureData);
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
            TrxAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.UPGRADE, waitingPo);
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

            Function function = TrxUtil.getCreateOrSignUpgradeFunction(nerveTxHash, upgradeContract, signatureData);
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

    private TrxAccount createTxStartForChange(String nerveTxHash, String[] addAddresses, String[] removeAddresses, HtgWaitingTxPo po) throws Exception {
        TrxAccount txStart = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.CHANGE, po);
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

    private TrxAccount createTxStart(String nerveTxHash, HeterogeneousChainTxType txType, HtgWaitingTxPo po) throws Exception {
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
        boolean isCompleted = trxParseTxHelper.isCompletedTransaction(nerveTxHash);
        if (isCompleted) {
            logger().info("[{}]transaction[{}]Completed", txType, nerveTxHash);
            return TrxAccount.newEmptyAccount(bankOrder);
        }
        // Obtain administrator account
        TrxAccount account = (TrxAccount) this.getAccount(htgContext.ADMIN_ADDRESS());
        account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
        account.setOrder(bankOrder);
        account.setBankSize(bankSize);
        return account;
    }

    private TrxAccount createTxStartForWithdraw(String nerveTxHash, HeterogeneousChainTxType txType, HtgWaitingTxPo po) throws Exception {
        Map<String, Integer> currentVirtualBanks = htgContext.getConverterCoreApi().currentVirtualBanksBalanceOrder(htgContext.getConfig().getChainId());
        po.setCurrentVirtualBanks(currentVirtualBanks);
        int bankSize = htgContext.getConverterCoreApi().getVirtualBankSize();
        // Wait for a fixed time in sequence before sending outHTGtransaction
        int bankOrder = currentVirtualBanks.get(htgContext.ADMIN_ADDRESS());
        if (logger().isDebugEnabled()) {
            logger().debug("Sequential calculation parameters bankSize: {}, orginBankOrder: {}, bankOrder: {}", bankSize, htgContext.getConverterCoreApi().getVirtualBankOrder(), bankOrder);
        }
        // towardsHTGNetwork request verification
        boolean isCompleted = trxParseTxHelper.isCompletedTransaction(nerveTxHash);
        if (isCompleted) {
            logger().info("[{}]transaction[{}]Completed", txType, nerveTxHash);
            return TrxAccount.newEmptyAccount(bankOrder);
        }
        // Obtain administrator account
        TrxAccount account = (TrxAccount) this.getAccount(htgContext.ADMIN_ADDRESS());
        account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
        account.setOrder(bankOrder);
        account.setBankSize(bankSize);
        return account;
    }


    private String createTxComplete(String nerveTxHash, HtgUnconfirmedTxPo po, String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType) throws Exception {
        // estimatefeeLimit
        TrxEstimateSun estimateSun;
        try {
            estimateSun = trxWalletApi.estimateSunUsed(fromAddress, htgContext.MULTY_SIGN_ADDRESS(), txFunction);
            if (estimateSun.isReverted()) {
                logger().error("[{}]Transaction verification failed, reason: {}", txType, estimateSun.getRevertReason());
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, estimateSun.getRevertReason());
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
                estimateSun = trxWalletApi.estimateSunUsed(fromAddress, htgContext.MULTY_SIGN_ADDRESS(), txFunction);
                if (estimateSun.isReverted()) {
                    logger().error("[{}]Transaction verification failed, reason: {}", txType, estimateSun.getRevertReason());
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, estimateSun.getRevertReason());
                }
            } else {
                throw e;
            }
        }
        // feeLimitchoice
        BigInteger feeLimit;
        if (txType == HeterogeneousChainTxType.WITHDRAW) {
            feeLimit = htgContext.GAS_LIMIT_OF_WITHDRAW();
        } else {
            feeLimit = htgContext.GAS_LIMIT_OF_CHANGE();
        }
        if (estimateSun.getSunUsed() > 0) {
            // Zoom in to3times
            feeLimit = BigDecimal.valueOf(estimateSun.getSunUsed()).multiply(TrxConstant.NUMBER_3).toBigInteger();
        }
        TrxSendTransactionPo trxSendTransactionPo = trxWalletApi.callContract(fromAddress, priKey, htgContext.MULTY_SIGN_ADDRESS(), feeLimit, txFunction, BigInteger.ZERO);
        String htTxHash = trxSendTransactionPo.getTxHash();
        // dockinglaunchhtgRecord transaction relationships during transactionsdbin
        htgTxRelationStorageService.save(htTxHash, nerveTxHash, trxSendTransactionPo.toHtgPo());
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
        logger().info("NerveNetwork oriented{}Network transmission[{}]transaction, nerveTxHash: {}, details: {}", htgContext.getConfig().getSymbol(), txType, nerveTxHash, po.toString());
        return htTxHash;
    }

    private boolean isEnoughFeeOfWithdrawByOtherMainAsset(AssetName otherMainAssetName, BigDecimal otherMainAssetAmount, int hAssetId) {
        return this.calcOtherMainAssetOfWithdrawByOtherMainAsset(otherMainAssetName, otherMainAssetAmount, hAssetId) != null;
    }

    private BigDecimal calcOtherMainAssetOfWithdrawByOtherMainAsset(AssetName otherMainAssetName, BigDecimal otherMainAssetAmount, int hAssetId) {
        String otherSymbol = otherMainAssetName.toString();
        int otherDecimals = otherMainAssetName.decimals();
        IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
        BigDecimal otherMainAssetUSD = coreApi.getUsdtPriceByAsset(otherMainAssetName);
        BigDecimal htgUSD = coreApi.getUsdtPriceByAsset(htgContext.ASSET_NAME());
        if(null == otherMainAssetUSD || null == htgUSD){
            logger().error("[{}][withdraw] Withdrawal fee calculation,Unable to obtain complete quotation. {}_USD:{}, {}_USD:{}", htgContext.getConfig().getSymbol(), otherSymbol, otherMainAssetUSD, htgContext.getConfig().getSymbol(), htgUSD);
            throw new NulsRuntimeException(ConverterErrorCode.DATA_NOT_FOUND);
        }
        BigDecimal needOtherMainAssetAmount = TrxUtil.calcOtherMainAssetOfWithdraw(htgContext, otherMainAssetName, otherMainAssetUSD, htgUSD);
        if (otherMainAssetAmount.compareTo(needOtherMainAssetAmount) >= 0) {
            logger().info("[{}]The handling fee is sufficient for the current network needs{}: {}, Actual expenditure{}: {}",
                    htgContext.getConfig().getSymbol(),
                    otherSymbol,
                    needOtherMainAssetAmount.movePointLeft(otherDecimals).toPlainString(),
                    otherSymbol,
                    otherMainAssetAmount.movePointLeft(otherDecimals).toPlainString());
            return needOtherMainAssetAmount;
        }
        logger().warn("[{}]Insufficient transaction fees, currently required by the network{}: {}, Actual expenditure{}: {}, Additional Required{}: {}",
                htgContext.getConfig().getSymbol(),
                otherSymbol,
                needOtherMainAssetAmount.movePointLeft(otherDecimals).toPlainString(),
                otherSymbol,
                otherMainAssetAmount.movePointLeft(otherDecimals).toPlainString(),
                otherSymbol,
                needOtherMainAssetAmount.subtract(otherMainAssetAmount).movePointLeft(otherDecimals).toPlainString());
        return null;
    }

    private BigDecimal calcOtherMainAssetOfWithdrawByMainAssetProtocol15(BigDecimal amount, int hAssetId) {
        BigDecimal amountCalc = TrxUtil.calcTrxOfWithdrawProtocol15(htgContext);
        if (amount.compareTo(amountCalc) >= 0) {
            return amountCalc;
        }
        logger().warn("[{}]Insufficient transaction fees, currently required by the networkTRX: {}, Actual expenditureTRX: {}, Additional RequiredTRX: {}",
                htgContext.getConfig().getSymbol(),
                amountCalc.movePointLeft(6).toPlainString(),
                amount.movePointLeft(6).toPlainString(),
                amountCalc.subtract(amount).movePointLeft(6).toPlainString());
        return null;
    }

}

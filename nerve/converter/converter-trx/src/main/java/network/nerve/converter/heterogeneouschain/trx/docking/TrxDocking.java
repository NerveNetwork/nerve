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

import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.FormatValidUtils;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
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
import network.nerve.converter.model.bo.*;
import network.nerve.converter.utils.ConverterUtil;
import org.tron.trident.abi.datatypes.Address;
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
            // 覆写这个地址作为虚拟银行管理员地址
            htgContext.SET_ADMIN_ADDRESS(account.getAddress());
            htgContext.SET_ADMIN_ADDRESS_PUBLIC_KEY(account.getCompressedPublicKey());
            htgContext.SET_ADMIN_ADDRESS_PASSWORD(password);
            logger().info("向{}异构组件导入节点出块地址信息, address: [{}]", htgContext.getConfig().getSymbol(), account.getAddress());
            return account;
        } catch (Exception e) {
            throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR, e);
        }
    }

    @Override
    public HeterogeneousAccount importAccountByKeystore(String keystorePath, String password) throws NulsException {
        throw new NulsException(ConverterErrorCode.NO_LONGER_SUPPORTED, "TRON不再支持的函数[3]");
    }

    @Override
    public String exportAccountKeystore(String address, String password) throws NulsException {
        throw new NulsException(ConverterErrorCode.NO_LONGER_SUPPORTED, "TRON不再支持的函数[3]");
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
    public List<HeterogeneousAccount> getAccountList() {
        return htgAccountStorageService.findAll();
    }

    @Override
    public void removeAccount(String address) throws Exception {
        htgAccountStorageService.deleteByAddress(address);
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
    public String createMultySignAddress(String[] pubKeys, int minSigns) {
        // do nothing
        return null;
    }

    @Override
    public void updateMultySignAddress(String multySignAddress) throws Exception {
        logger().info("{}更新多签合约地址, old: {}, new: {}", htgContext.getConfig().getSymbol(), htgContext.MULTY_SIGN_ADDRESS(), multySignAddress);
        //multySignAddress = multySignAddress.toLowerCase();
        // 监听多签地址交易
        htgListener.removeListeningAddress(htgContext.MULTY_SIGN_ADDRESS());
        htgListener.addListeningAddress(multySignAddress);
        // 更新多签地址
        htgContext.SET_MULTY_SIGN_ADDRESS(multySignAddress);
        // 保存当前多签地址到多签地址历史列表中
        htgMultiSignAddressHistoryStorageService.save(multySignAddress);
        // 合约升级后的流程切换操作
        htgUpgradeContractSwitchHelper.switchProcessor(multySignAddress);
    }

    @Override
    public void updateMultySignAddressProtocol16(String multySignAddress, byte version) throws Exception {
        updateMultySignAddress(multySignAddress);
        htgContext.SET_VERSION(version);
        htgMultiSignAddressHistoryStorageService.saveVersion(version);
        logger().info("更新签名版本号: {}", version);
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
        List<Type> symbolResult = trxWalletApi.callViewFunction(contractAddress, TrxUtil.getSymbolERC20Function());
        if (symbolResult.isEmpty()) {
            return false;
        }
        String _symbol = symbolResult.get(0).getValue().toString();
        if (!_symbol.equals(symbol)) {
            return false;
        }
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
    public void txConfirmedCompleted(String htTxHash, Long blockHeight, String nerveTxHash) throws Exception {
        logger().info("Nerve网络确认{}交易 Nerver hash: {}", htgContext.getConfig().getSymbol(), nerveTxHash);
        if (StringUtils.isBlank(htTxHash)) {
            logger().warn("Empty htTxHash warning");
            return;
        }
        // 更新db中po的状态，改为delete，在队列任务中确认`ROLLBACK_NUMER`个块之后再移除，便于状态回滚
        HtgUnconfirmedTxPo txPo = htgUnconfirmedTxStorageService.findByTxHash(htTxHash);
        if (txPo == null) {
            txPo = new HtgUnconfirmedTxPo();
            txPo.setTxHash(htTxHash);
        }
        txPo.setDelete(true);
        txPo.setDeletedHeight(blockHeight + HtgConstant.ROLLBACK_NUMER);
        logger().info("Nerve网络对[{}]{}交易[{}]确认完成, nerve高度: {}, nerver hash: {}", txPo.getTxType(), htgContext.getConfig().getSymbol(), txPo.getTxHash(), blockHeight, txPo.getNerveTxHash());
        boolean delete = txPo.isDelete();
        Long deletedHeight = txPo.getDeletedHeight();
        htgUnconfirmedTxStorageService.update(txPo, update -> {
            update.setDelete(delete);
            update.setDeletedHeight(deletedHeight);
        });
        // 持久化状态已成功的nerveTx
        if (StringUtils.isNotBlank(nerveTxHash)) {
            logger().debug("持久化状态已成功的nerveTxHash: {}", nerveTxHash);
            htgInvokeTxHelper.saveSuccessfulNerve(nerveTxHash);
        }
    }

    @Override
    public HeterogeneousTransactionInfo getDepositTransaction(String txHash) throws Exception {
        // 从DB中获取数据，若获取不到，再到HTG网络中获取
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
        // 从DB中获取数据，若获取不到，再到HTG网络中获取
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
        return txInfo;
    }

    @Override
    public HeterogeneousConfirmedInfo getConfirmedTxInfo(String txHash) throws Exception {
        HeterogeneousConfirmedInfo info = new HeterogeneousConfirmedInfo();
        // 从DB中获取数据，若获取不到，再到HTG网络中获取
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
        throw new NulsException(ConverterErrorCode.NO_LONGER_SUPPORTED, "合约升级后不再支持的函数[3]");
    }

    @Override
    public String createOrSignWithdrawTx(String nerveTxHash, String toAddress, BigInteger value, Integer assetId) throws NulsException {
        throw new NulsException(ConverterErrorCode.NO_LONGER_SUPPORTED, "合约升级后不再支持的函数[0]");
    }

    @Override
    public String createOrSignManagerChangesTx(String nerveTxHash, String[] addAddresses, String[] removeAddresses, int orginTxCount) throws NulsException {
        throw new NulsException(ConverterErrorCode.NO_LONGER_SUPPORTED, "合约升级后不再支持的函数[1]");
    }

    @Override
    public String createOrSignUpgradeTx(String nerveTxHash) throws NulsException {
        throw new NulsException(ConverterErrorCode.NO_LONGER_SUPPORTED, "合约升级后不再支持的函数[2]");
    }

    @Override
    public String createOrSignWithdrawTxII(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData) throws NulsException {
        return this.createOrSignWithdrawTxII(nerveTxHash, toAddress, value, assetId, signatureData, true);
    }

    @Override
    public boolean validateManagerChangesTxII(String nerveTxHash, String[] addAddresses, String[] removeAddresses, int orginTxCount, String signatureData) throws NulsException {
        logger().info("验证{}网络虚拟银行变更交易，nerveTxHash: {}, signatureData: {}", htgContext.getConfig().getSymbol(), nerveTxHash, signatureData);
        try {
            // 向HTG网络请求验证
            boolean isCompleted = trxParseTxHelper.isCompletedTransaction(nerveTxHash);
            if (isCompleted) {
                logger().info("[{}]交易[{}]已完成", HeterogeneousChainTxType.CHANGE, nerveTxHash);
                return true;
            }
            // 业务验证
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
                    logger().error("重复的待加入地址列表");
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
                    logger().error("重复的待退出地址列表");
                    return false;
                }
                removeList.add(new Address(remove));
            }
            // 若没有加入和退出，则直接发确认交易
            if (addAddresses.length == 0 && removeAddresses.length == 0) {
                return true;
            }
            // 获取管理员账户
            String fromAddress = htgContext.ADMIN_ADDRESS();
            if (removeSet.contains(fromAddress)) {
                logger().error("退出的管理员不能参与管理员变更交易");
                return false;
            }
            Function txFunction = TrxUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, orginTxCount, signatureData);
            // 验证合约交易合法性
            TrxEstimateSun estimateSun = trxWalletApi.estimateSunUsed(fromAddress, htgContext.MULTY_SIGN_ADDRESS(), txFunction);
            if (estimateSun.isReverted()) {
                logger().error("[{}]交易验证失败，原因: {}", HeterogeneousChainTxType.CHANGE, estimateSun.getRevertReason());
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
        // 增加resend机制
        try {
            if (!htgContext.isAvailableRPC()) {
                logger().error("[{}]网络RPC不可用，暂停此任务", htgContext.getConfig().getSymbol());
                throw new NulsException(ConverterErrorCode.HTG_RPC_UNAVAILABLE);
            }
            return this.createOrSignManagerChangesTxII(nerveTxHash, addAddresses, removeAddresses, orginTxCount, signatureData, true);
        } catch (NulsException e) {
            // 当出现交易签名不足导致的执行失败时，向CORE重新索要交易的拜占庭签名
            if (ConverterUtil.isInsufficientSignature(e)) {
                HtgWaitingTxPo waitingTxPo = htgInvokeTxHelper.findEthWaitingTxPo(nerveTxHash);
                if (waitingTxPo != null) {
                    try {
                        String reSendHtTxHash = htgResendHelper.reSend(waitingTxPo);
                        logger().info("Nerve交易[{}]重发完成, reSendHtTxHash: {}", nerveTxHash, reSendHtTxHash);
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
        try {
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
        }
        return EMPTY_STRING;
    }

    @Override
    public Boolean reAnalysisDepositTx(String htTxHash) throws Exception {
        if (htgCommonHelper.constainHash(htTxHash)) {
            logger().info("重复收集充值交易hash: {}，不再重复解析[0]", htTxHash);
            return true;
        }
        reAnalysisLock.lock();
        try {
            if (htgCommonHelper.constainHash(htTxHash)) {
                logger().info("重复收集充值交易hash: {}，不再重复解析[1]", htTxHash);
                return true;
            }
            logger().info("重新解析充值交易: {}", htTxHash);
            Chain.Transaction tx = trxWalletApi.getTransactionByHash(htTxHash);
            if (tx == null) {
                return false;
            }
            Chain.Transaction.Contract contract = tx.getRawData().getContract(0);
            Chain.Transaction.Contract.ContractType type = contract.getType();
            if (Chain.Transaction.Contract.ContractType.TriggerSmartContract == type &&
                    tx.getRet(0).getContractRet() != Chain.Transaction.Result.contractResult.SUCCESS) {
                logger().warn("交易不正常[0]: {}", htTxHash);
                return false;
            }

            Response.TransactionInfo txReceipt = trxWalletApi.getTransactionReceipt(htTxHash);
            if (!TrxUtil.checkTransactionSuccess(txReceipt)) {
                logger().warn("交易不正常[1]: {}", htTxHash);
                return false;
            }
            HeterogeneousTransactionInfo txInfo = trxParseTxHelper.parseDepositTransaction(tx, txReceipt);
            if (txInfo == null) {
                logger().warn("交易不正常[2]: {}", htTxHash);
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
            logger().info("重复收集交易hash: {}，不再重复解析[0]", htTxHash);
            return true;
        }
        reAnalysisLock.lock();
        try {
            if (htgCommonHelper.constainHash(htTxHash)) {
                logger().info("重复收集交易hash: {}，不再重复解析[1]", htTxHash);
                return true;
            }
            logger().info("重新解析交易: {}", htTxHash);
            Chain.Transaction tx = trxWalletApi.getTransactionByHash(htTxHash);
            if (tx == null) {
                return false;
            }
            Chain.Transaction.Contract contract = tx.getRawData().getContract(0);
            Chain.Transaction.Contract.ContractType type = contract.getType();
            if (Chain.Transaction.Contract.ContractType.TriggerSmartContract == type &&
                    tx.getRet(0).getContractRet() != Chain.Transaction.Result.contractResult.SUCCESS) {
                logger().warn("交易不正常[0]: {}", htTxHash);
                return false;
            }

            Response.TransactionInfo txReceipt = trxWalletApi.getTransactionReceipt(htTxHash);
            if (!TrxUtil.checkTransactionSuccess(txReceipt)) {
                logger().warn("交易不正常[1]: {}", htTxHash);
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
        // 获取管理员账户
        TrxAccount account = (TrxAccount) this.getAccount(htgContext.ADMIN_ADDRESS());
        account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        boolean isContractAsset = assetId > 1;
        String contractAddressERC20;
        if (isContractAsset) {
            contractAddressERC20 = trxERC20Helper.getContractAddressByAssetId(assetId);
        } else {
            contractAddressERC20 = HtgConstant.ZERO_ADDRESS;
        }
        // 若跨链资产精度不同，则换算精度
        value = htgContext.getConverterCoreApi().checkDecimalsSubtractedToNerveForWithdrawal(htgContext.HTG_CHAIN_ID(), assetId, value);
        String vHash = TrxUtil.encoderWithdraw(htgContext, txHash, toAddress, value, isContractAsset, contractAddressERC20, htgContext.VERSION());
        logger().debug("提现签名数据: {}, {}, {}, {}, {}, {}", txHash, toAddress, value, isContractAsset, contractAddressERC20, htgContext.VERSION());
        logger().debug("提现签名vHash: {}, nerveTxHash: {}", vHash, txHash);
        return TrxUtil.dataSign(vHash, priKey);
    }

    @Override
    public String signManagerChangesII(String nerveTxHash, String[] addAddresses, String[] removeAddresses, int orginTxCount) throws NulsException {
        // 业务验证
        if (addAddresses == null) {
            addAddresses = new String[0];
        }
        if (removeAddresses == null) {
            removeAddresses = new String[0];
        }
        // 交易准备
        Set<String> addSet = new HashSet<>();
        for (int a = 0, addSize = addAddresses.length; a < addSize; a++) {
            String add = addAddresses[a];
            //add = add.toLowerCase();
            addAddresses[a] = add;
            if (!addSet.add(add)) {
                logger().error("重复的待加入地址列表");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_2);
            }
        }
        Set<String> removeSet = new HashSet<>();
        for (int r = 0, removeSize = removeAddresses.length; r < removeSize; r++) {
            String remove = removeAddresses[r];
            //remove = remove.toLowerCase();
            removeAddresses[r] = remove;
            if (!removeSet.add(remove)) {
                logger().error("重复的待退出地址列表");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_4);
            }
        }
        // 获取管理员账户
        TrxAccount account = (TrxAccount) this.getAccount(htgContext.ADMIN_ADDRESS());
        account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        String vHash = TrxUtil.encoderChange(htgContext, nerveTxHash, addAddresses, orginTxCount, removeAddresses, htgContext.VERSION());
        logger().debug("变更交易的签名vHash: {}, nerveTxHash: {}", vHash, nerveTxHash);
        return TrxUtil.dataSign(vHash, priKey);
    }

    @Override
    public String signUpgradeII(String nerveTxHash, String upgradeContract) throws NulsException {
        // 获取管理员账户
        TrxAccount account = (TrxAccount) this.getAccount(htgContext.ADMIN_ADDRESS());
        account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        // 把地址转换成小写
        //upgradeContract = upgradeContract.toLowerCase();
        String vHash = TrxUtil.encoderUpgrade(htgContext, nerveTxHash, upgradeContract, htgContext.VERSION());
        logger().debug("升级交易的签名vHash: {}, nerveTxHash: {}", vHash, nerveTxHash);
        return TrxUtil.dataSign(vHash, priKey);
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
        // 若跨链资产精度不同，则换算精度
        value = htgContext.getConverterCoreApi().checkDecimalsSubtractedToNerveForWithdrawal(htgContext.HTG_CHAIN_ID(), assetId, value);
        String vHash = TrxUtil.encoderWithdraw(htgContext, txHash, toAddress, value, isContractAsset, contractAddressERC20, htgContext.VERSION());
        logger().debug("[验证签名] 提现数据: {}, {}, {}, {}, {}, {}", txHash, toAddress, value, isContractAsset, contractAddressERC20, htgContext.VERSION());
        logger().debug("[验证签名] 提现vHash: {}", vHash);
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
        // 交易准备
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
        // 把地址转换成小写
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
        // 可使用其他异构网络的主资产作为手续费, 比如提现到TRX，支付BNB作为手续费
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
        logger().info("[{}]网络当前签名版本号: {}", htgContext.getConfig().getSymbol(), htgContext.VERSION());
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

    public String createOrSignWithdrawTxII(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData, boolean checkOrder) throws NulsException {
        if (htgContext.getConverterCoreApi().isProtocol22()) {
            // 协议22: 支持不同精度的跨链资产
            return createOrSignWithdrawTxIIProtocol22(nerveTxHash, toAddress, value, assetId, signatureData, checkOrder);
        } else {
            return _createOrSignWithdrawTxII(nerveTxHash, toAddress, value, assetId, signatureData, checkOrder);
        }
    }

    private String _createOrSignWithdrawTxII(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData, boolean checkOrder) throws NulsException {
        try {
            if (!htgContext.isAvailableRPC()) {
                logger().error("[{}]网络RPC不可用，暂停此任务", htgContext.getConfig().getSymbol());
                throw new NulsException(ConverterErrorCode.HTG_RPC_UNAVAILABLE);
            }
            logger().info("准备发送提现的{}交易，nerveTxHash: {}, signatureData: {}", htgContext.getConfig().getSymbol(), nerveTxHash, signatureData);
            // 交易准备
            HtgWaitingTxPo waitingPo = new HtgWaitingTxPo();
            TrxAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.WITHDRAW, waitingPo);
            // 保存交易调用参数，设置等待结束时间
            htgInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, toAddress, value, assetId, signatureData, account.getOrder(), waitingPo);
            if (account.isEmpty()) {
                return EMPTY_STRING;
            }
            // 当要检查顺序时，非首位不发交易
            if (checkOrder && !checkFirstOrder(account.getOrder())) {
                logger().info("非首位不发交易, order: {}", account.getOrder());
                return EMPTY_STRING;
            }
            // 获取管理员账户
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            // 业务验证
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
            // 检查是否是NERVE资产绑定的ERC20，是则检查多签合约内是否已经注册此定制的ERC20，否则提现异常
            if (htgContext.getConverterCoreApi().isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                    && !trxParseTxHelper.isMinterERC20(po.getContractAddress())) {
                logger().warn("[{}]不合法的{}网络的提现交易, ERC20[{}]已绑定NERVE资产，但合约内未注册", nerveTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
                throw new NulsException(ConverterErrorCode.NOT_BIND_ASSET);
            }
            po.setTo(toAddress);
            po.setValue(value);
            po.setIfContractAsset(isContractAsset);
            if (isContractAsset) {
                po.setContractAddress(contractAddressERC20);
            }
            Function createOrSignWithdrawFunction = TrxUtil.getCreateOrSignWithdrawFunction(nerveTxHash, toAddress, value, isContractAsset, contractAddressERC20, signatureData);

            // 检查手续费
            IConverterCoreApi coreApi = htgContext.getConverterCoreApi();
            WithdrawalTotalFeeInfo feeInfo = coreApi.getFeeOfWithdrawTransaction(nerveTxHash);
            BigDecimal feeAmount = new BigDecimal(feeInfo.getFee());
            if (feeInfo.isNvtAsset()) feeInfo.setHtgMainAssetName(AssetName.NVT);
            BigDecimal need;
            // // 使用非提现网络的其他主资产作为手续费时
            if (feeInfo.getHtgMainAssetName() != htgContext.ASSET_NAME()) {
                need = this.calcOtherMainAssetOfWithdrawByOtherMainAsset(feeInfo.getHtgMainAssetName(), feeAmount, po.getAssetId());
            } else {
                need = this.calcOtherMainAssetOfWithdrawByMainAssetProtocol15(feeAmount, po.getAssetId());
            }
            if (need == null) {
                throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
            }

            // 验证合约后发出交易
            String htTxHash = this.createTxComplete(nerveTxHash, po, fromAddress, priKey, createOrSignWithdrawFunction, HeterogeneousChainTxType.WITHDRAW);
            if (StringUtils.isNotBlank(htTxHash)) {
                // 记录提现交易已向HTG网络发出
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
                logger().error("[{}]网络RPC不可用，暂停此任务", htgContext.getConfig().getSymbol());
                throw new NulsException(ConverterErrorCode.HTG_RPC_UNAVAILABLE);
            }
            logger().info("准备发送提现的{}交易，nerveTxHash: {}, signatureData: {}", htgContext.getConfig().getSymbol(), nerveTxHash, signatureData);
            // 交易准备
            HtgWaitingTxPo waitingPo = new HtgWaitingTxPo();
            TrxAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.WITHDRAW, waitingPo);
            // 保存交易调用参数，设置等待结束时间
            htgInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, toAddress, value, assetId, signatureData, account.getOrder(), waitingPo);
            if (account.isEmpty()) {
                return EMPTY_STRING;
            }
            // 当要检查顺序时，非首位不发交易
            if (checkOrder && !checkFirstOrder(account.getOrder())) {
                logger().info("非首位不发交易, order: {}", account.getOrder());
                return EMPTY_STRING;
            }
            // 获取管理员账户
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            // 业务验证
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
            // 检查是否是NERVE资产绑定的ERC20，是则检查多签合约内是否已经注册此定制的ERC20，否则提现异常
            if (coreApi.isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                    && !trxParseTxHelper.isMinterERC20(po.getContractAddress())) {
                logger().warn("[{}]不合法的{}网络的提现交易, ERC20[{}]已绑定NERVE资产，但合约内未注册", nerveTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
                throw new NulsException(ConverterErrorCode.NOT_BIND_ASSET);
            }
            po.setTo(toAddress);
            po.setValue(value);
            po.setIfContractAsset(isContractAsset);
            if (isContractAsset) {
                po.setContractAddress(contractAddressERC20);
            }
            // 跨链资产精度不同，换算精度
            value = htgContext.getConverterCoreApi().checkDecimalsSubtractedToNerveForWithdrawal(htgContext.HTG_CHAIN_ID(), assetId, value);
            Function createOrSignWithdrawFunction = TrxUtil.getCreateOrSignWithdrawFunction(nerveTxHash, toAddress, value, isContractAsset, contractAddressERC20, signatureData);

            // 检查手续费
            WithdrawalTotalFeeInfo feeInfo = coreApi.getFeeOfWithdrawTransaction(nerveTxHash);
            if (feeInfo.isNvtAsset()) feeInfo.setHtgMainAssetName(AssetName.NVT);
            BigDecimal feeAmount = new BigDecimal(coreApi.checkDecimalsSubtractedToNerveForWithdrawal(feeInfo.getHtgMainAssetName().chainId(), 1, feeInfo.getFee()));
            BigDecimal need;
            // // 使用非提现网络的其他主资产作为手续费时
            if (feeInfo.getHtgMainAssetName() != htgContext.ASSET_NAME()) {
                need = this.calcOtherMainAssetOfWithdrawByOtherMainAsset(feeInfo.getHtgMainAssetName(), feeAmount, po.getAssetId());
            } else {
                need = this.calcOtherMainAssetOfWithdrawByMainAssetProtocol15(feeAmount, po.getAssetId());
            }
            if (need == null) {
                throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
            }

            // 验证合约后发出交易
            String htTxHash = this.createTxComplete(nerveTxHash, po, fromAddress, priKey, createOrSignWithdrawFunction, HeterogeneousChainTxType.WITHDRAW);
            if (StringUtils.isNotBlank(htTxHash)) {
                // 记录提现交易已向HTG网络发出
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
        logger().info("准备发送虚拟银行变更{}交易，nerveTxHash: {}, signatureData: {}", htgContext.getConfig().getSymbol(), nerveTxHash, signatureData);
        try {
            // 业务验证
            if (addAddresses == null) {
                addAddresses = new String[0];
            }
            if (removeAddresses == null) {
                removeAddresses = new String[0];
            }
            // 准备数据
            HtgUnconfirmedTxPo po = new HtgUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            po.setAddAddresses(addAddresses);
            po.setRemoveAddresses(removeAddresses);
            po.setOrginTxCount(orginTxCount);
            // 交易准备
            HtgWaitingTxPo waitingPo = new HtgWaitingTxPo();
            TrxAccount account = this.createTxStartForChange(nerveTxHash, addAddresses, removeAddresses, waitingPo);
            // 保存交易调用参数，设置等待结束时间
            htgInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, addAddresses, removeAddresses, orginTxCount, signatureData, account.getOrder(), waitingPo);
            if (account.isEmpty()) {
                return EMPTY_STRING;
            }
            // 当要检查顺序时，非首位不发交易
            if (checkOrder && !checkFirstOrder(account.getOrder())) {
                logger().info("非首位不发交易, order: {}", account.getOrder());
                return EMPTY_STRING;
            }
            Set<String> addSet = new HashSet<>();
            List<Address> addList = new ArrayList<>();
            for (int a = 0, addSize = addAddresses.length; a < addSize; a++) {
                String add = addAddresses[a];
                //add = add.toLowerCase();
                addAddresses[a] = add;
                if (!addSet.add(add)) {
                    logger().error("重复的待加入地址列表");
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
                    logger().error("重复的待退出地址列表");
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_4);
                }
                removeList.add(new Address(remove));
            }
            // 若没有加入和退出，则直接发确认交易
            if (addAddresses.length == 0 && removeAddresses.length == 0) {
                logger().info("虚拟银行变更没有加入和退出，直接发确认交易, nerveTxHash: {}", nerveTxHash);
                try {
                    htgCallBackManager.getTxConfirmedProcessor().txConfirmed(
                            HeterogeneousChainTxType.CHANGE,
                            nerveTxHash,
                            null, //htTxHash,
                            null, //ethTx blockHeight,
                            null, //ethTx tx time,
                            htgContext.MULTY_SIGN_ADDRESS(),
                            null  //ethTx signers
                    );
                } catch (Exception e) {
                    if (e instanceof NulsException) {
                        throw (NulsException) e;
                    }
                    throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
                }
                return EMPTY_STRING;
            }
            // 获取管理员账户
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            if (removeSet.contains(fromAddress)) {
                logger().error("退出的管理员不能参与管理员变更交易");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_6);
            }
            Function createOrSignManagerChangeFunction = TrxUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, orginTxCount, signatureData);
            // 验证合约后发出交易
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
        logger().info("准备发送虚拟银行合约升级授权交易，nerveTxHash: {}, upgradeContract: {}, signatureData: {}", nerveTxHash, upgradeContract, signatureData);
        try {
            // 交易准备
            HtgWaitingTxPo waitingPo = new HtgWaitingTxPo();
            TrxAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.UPGRADE, waitingPo);
            // 保存交易调用参数，设置等待结束时间
            htgInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, upgradeContract, signatureData, account.getOrder(), waitingPo);
            if (account.isEmpty()) {
                return EMPTY_STRING;
            }
            // 当要检查顺序时，非首位不发交易
            if (checkOrder && !checkFirstOrder(account.getOrder())) {
                logger().info("非首位不发交易, order: {}", account.getOrder());
                return EMPTY_STRING;
            }
            // 获取管理员账户
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            HtgUnconfirmedTxPo po = new HtgUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);

            Function function = TrxUtil.getCreateOrSignUpgradeFunction(nerveTxHash, upgradeContract, signatureData);
            // 验证合约后发出交易
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
        List<Map.Entry<String, Integer>> list = new ArrayList(currentVirtualBanks.entrySet());
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
        logger().info("变更交易的当前节点执行顺序: {}, addAddresses: {}, removeAddresses: {}, currentVirtualBanks: {}", order, Arrays.toString(addAddresses), Arrays.toString(removeAddresses), currentVirtualBanks);
        return txStart;
    }

    private TrxAccount createTxStart(String nerveTxHash, HeterogeneousChainTxType txType, HtgWaitingTxPo po) throws Exception {
        Map<String, Integer> currentVirtualBanks = htgContext.getConverterCoreApi().currentVirtualBanks(htgContext.getConfig().getChainId());
        po.setCurrentVirtualBanks(currentVirtualBanks);
        String realNerveTxHash = nerveTxHash;
        // 根据nerve交易hash前两位算出顺序种子
        int seed = new BigInteger(realNerveTxHash.substring(0, 1), 16).intValue() + 1;
        int bankSize = htgContext.getConverterCoreApi().getVirtualBankSize();
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
        logger().debug("加工中, 当前银行顺序: {}", currentVirtualBanks);
        List<Map.Entry<String, Integer>> list = new ArrayList(currentVirtualBanks.entrySet());
        list.sort(ConverterUtil.CHANGE_SORT);
        int i = 1;
        for (Map.Entry<String, Integer> entry : list) {
            currentVirtualBanks.put(entry.getKey(), i++);
        }
        logger().debug("加工后, 当前银行顺序: {}", currentVirtualBanks);
        // 按顺序等待固定时间后再发出HTG交易
        int bankOrder = currentVirtualBanks.get(htgContext.ADMIN_ADDRESS());
        if (logger().isDebugEnabled()) {
            logger().debug("顺序计算参数 bankSize: {}, seed: {}, mod: {}, orginBankOrder: {}, bankOrder: {}", bankSize, seed, mod, htgContext.getConverterCoreApi().getVirtualBankOrder(), bankOrder);
        }
        // 向HTG网络请求验证
        boolean isCompleted = trxParseTxHelper.isCompletedTransaction(nerveTxHash);
        if (isCompleted) {
            logger().info("[{}]交易[{}]已完成", txType, nerveTxHash);
            return TrxAccount.newEmptyAccount(bankOrder);
        }
        // 获取管理员账户
        TrxAccount account = (TrxAccount) this.getAccount(htgContext.ADMIN_ADDRESS());
        account.decrypt(htgContext.ADMIN_ADDRESS_PASSWORD());
        account.setOrder(bankOrder);
        account.setMod(mod);
        account.setBankSize(bankSize);
        return account;
    }


    private String createTxComplete(String nerveTxHash, HtgUnconfirmedTxPo po, String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType) throws Exception {
        // 估算feeLimit
        TrxEstimateSun estimateSun = trxWalletApi.estimateSunUsed(fromAddress, htgContext.MULTY_SIGN_ADDRESS(), txFunction);
        if (estimateSun.isReverted()) {
            logger().error("[{}]交易验证失败，原因: {}", txType, estimateSun.getRevertReason());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, estimateSun.getRevertReason());
        }
        // feeLimit选择
        BigInteger feeLimit;
        if (txType == HeterogeneousChainTxType.WITHDRAW) {
            feeLimit = htgContext.GAS_LIMIT_OF_WITHDRAW();
        } else {
            feeLimit = htgContext.GAS_LIMIT_OF_CHANGE();
        }
        if (estimateSun.getSunUsed() > 0) {
            // 放大到1.3倍
            feeLimit = BigDecimal.valueOf(estimateSun.getSunUsed()).multiply(TrxConstant.NUMBER_1_DOT_3).toBigInteger();
        }
        TrxSendTransactionPo trxSendTransactionPo = trxWalletApi.callContract(fromAddress, priKey, htgContext.MULTY_SIGN_ADDRESS(), feeLimit, txFunction, BigInteger.ZERO);
        String htTxHash = trxSendTransactionPo.getTxHash();
        // docking发起htg交易时，把交易关系记录到db中
        htgTxRelationStorageService.save(htTxHash, nerveTxHash, trxSendTransactionPo.toHtgPo());
        // 当前节点已发出htg交易
        htgInvokeTxHelper.saveSentEthTx(nerveTxHash);

        // 保存未确认交易
        po.setTxHash(htTxHash);
        po.setFrom(fromAddress);
        po.setTxType(txType);
        htgUnconfirmedTxStorageService.save(po);
        htgContext.UNCONFIRMED_TX_QUEUE().offer(po);
        // 监听此交易的打包状态
        htgListener.addListeningTx(htTxHash);
        logger().info("Nerve网络向{}网络发出[{}]交易, nerveTxHash: {}, 详情: {}", htgContext.getConfig().getSymbol(), txType, nerveTxHash, po.toString());
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
            logger().error("[{}][withdraw] 提现手续费计算,没有获取到完整的报价. {}_USD:{}, {}_USD:{}", htgContext.getConfig().getSymbol(), otherSymbol, otherMainAssetUSD, htgContext.getConfig().getSymbol(), htgUSD);
            throw new NulsRuntimeException(ConverterErrorCode.DATA_NOT_FOUND);
        }
        BigDecimal needOtherMainAssetAmount = TrxUtil.calcOtherMainAssetOfWithdraw(htgContext, otherMainAssetName, otherMainAssetUSD, htgUSD);
        if (otherMainAssetAmount.compareTo(needOtherMainAssetAmount) >= 0) {
            logger().info("[{}]手续费足够，当前网络需要的{}: {}, 实际支出的{}: {}",
                    htgContext.getConfig().getSymbol(),
                    otherSymbol,
                    needOtherMainAssetAmount.movePointLeft(otherDecimals).toPlainString(),
                    otherSymbol,
                    otherMainAssetAmount.movePointLeft(otherDecimals).toPlainString());
            return needOtherMainAssetAmount;
        }
        logger().warn("[{}]手续费不足，当前网络需要的{}: {}, 实际支出的{}: {}, 需要追加的{}: {}",
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
        logger().warn("[{}]手续费不足，当前网络需要的TRX: {}, 实际支出的TRX: {}, 需要追加的TRX: {}",
                htgContext.getConfig().getSymbol(),
                amountCalc.movePointLeft(6).toPlainString(),
                amount.movePointLeft(6).toPlainString(),
                amountCalc.subtract(amount).movePointLeft(6).toPlainString());
        return null;
    }

}

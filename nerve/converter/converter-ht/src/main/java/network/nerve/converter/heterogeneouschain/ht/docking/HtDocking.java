/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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
package network.nerve.converter.heterogeneouschain.ht.docking;

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
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.ht.callback.HtCallBackManager;
import network.nerve.converter.heterogeneouschain.ht.constant.HtConstant;
import network.nerve.converter.heterogeneouschain.ht.context.HtContext;
import network.nerve.converter.heterogeneouschain.ht.core.HtWalletApi;
import network.nerve.converter.heterogeneouschain.ht.helper.*;
import network.nerve.converter.heterogeneouschain.ht.listener.HtListener;
import network.nerve.converter.heterogeneouschain.ht.model.*;
import network.nerve.converter.heterogeneouschain.ht.storage.*;
import network.nerve.converter.heterogeneouschain.ht.utils.HtUtil;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.utils.ConverterUtil;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static io.protostuff.ByteString.EMPTY_STRING;


/**
 * @author: Mimi
 * @date: 2020-08-28
 */
public class HtDocking implements IHeterogeneousChainDocking {

    private static HeterogeneousAssetInfo mainAsset;
    private static final HtDocking DOCKING = new HtDocking();

    protected HtWalletApi htWalletApi;
    protected HtListener htListener;
    protected HtERC20Helper htERC20Helper;
    protected ConverterConfig converterConfig;
    protected HtTxRelationStorageService htTxRelationStorageService;
    protected HtUnconfirmedTxStorageService htUnconfirmedTxStorageService;
    protected HtMultiSignAddressHistoryStorageService htMultiSignAddressHistoryStorageService;
    protected HtTxStorageService htTxStorageService;
    protected HtAccountStorageService htAccountStorageService;
    protected HtCommonHelper htCommonHelper;
    protected HtUpgradeContractSwitchHelper htUpgradeContractSwitchHelper;
    protected ReentrantLock reAnalysisLock = new ReentrantLock();
    private String keystorePath;

    protected HtCallBackManager htCallBackManager;
    protected HtInvokeTxHelper htInvokeTxHelper;
    protected HtParseTxHelper htParseTxHelper;
    protected HtAnalysisTxHelper htAnalysisTxHelper;
    protected HtResendHelper htResendHelper;
    protected HtPendingTxHelper htPendingTxHelper;

    private HtDocking() {
    }

    private NulsLogger logger() {
        return HtContext.logger();
    }

    public static HtDocking getInstance() {
        return DOCKING;
    }

    @Override
    public int version() {
        return HtConstant.VERSION;
    }

    @Override
    public boolean isSupportContractAssetByCurrentChain() {
        return true;
    }

    @Override
    public Integer getChainId() {
        return HtContext.getConfig().getChainId();
    }

    @Override
    public String getChainSymbol() {
        return HtContext.getConfig().getSymbol();
    }

    @Override
    public String getCurrentSignAddress() {
        return HtContext.ADMIN_ADDRESS;
    }

    @Override
    public String getCurrentMultySignAddress() {
        return HtContext.MULTY_SIGN_ADDRESS;
    }

    @Override
    public String generateAddressByCompressedPublicKey(String compressedPublicKey) {
        return HtUtil.genEthAddressByCompressedPublickey(compressedPublicKey);
    }

    @Override
    public HeterogeneousAccount importAccountByPriKey(String priKey, String password) throws NulsException {
        if (StringUtils.isNotBlank(HtContext.ADMIN_ADDRESS)) {
            HtAccount account = htAccountStorageService.findByAddress(HtContext.ADMIN_ADDRESS);
            account.decrypt(HtContext.ADMIN_ADDRESS_PASSWORD);
            if (Arrays.equals(account.getPriKey(), HexUtil.decode(priKey))) {
                account.setPriKey(new byte[0]);
                return account;
            }
        }
        if (!FormatValidUtils.validPassword(password)) {
            logger().error("password format wrong");
            throw new NulsException(ConverterErrorCode.PASSWORD_FORMAT_WRONG);
        }
        HtAccount account = HtUtil.createAccount(priKey);
        account.encrypt(password);
        try {
            htAccountStorageService.save(account);
            // 覆写这个地址作为虚拟银行管理员地址
            HtContext.ADMIN_ADDRESS = account.getAddress();
            HtContext.ADMIN_ADDRESS_PUBLIC_KEY = account.getCompressedPublicKey();
            HtContext.ADMIN_ADDRESS_PASSWORD = password;
            logger().info("向HT异构组件导入节点出块地址信息, address: [{}]", account.getAddress());
            return account;
        } catch (Exception e) {
            throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR, e);
        }
    }

    @Override
    public HeterogeneousAccount importAccountByKeystore(String keystorePath, String password) throws NulsException {
        Credentials credentials;
        try {
            credentials = WalletUtils.loadCredentials(password, keystorePath);
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.PARSE_JSON_FAILD, e);
        } catch (CipherException e) {
            throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
        }
        try {
            ECKeyPair keyPair = credentials.getEcKeyPair();
            HeterogeneousAccount account = this.importAccountByPriKey(Numeric.encodeQuantity(keyPair.getPrivateKey()), password);
            return account;
        } catch (NulsException e) {
            throw e;
        }
    }

    @Override
    public String exportAccountKeystore(String address, String password) throws NulsException {
        if (!validateAccountPassword(address, password)) {
            logger().error("password is wrong");
            throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
        }
        String destinationDirectoryPath = getKeystorePath();
        HeterogeneousAccount account = this.getAccount(address);
        account.decrypt(password);
        try {
            WalletUtils.generateNewWalletFile(password, new File(destinationDirectoryPath));
            return destinationDirectoryPath;
        } catch (Exception e) {
            logger().error(e);
            throw new NulsException(ConverterErrorCode.DATA_ERROR, "failed to generate the keystore");
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
        return htAccountStorageService.findByAddress(address);
    }

    @Override
    public List<HeterogeneousAccount> getAccountList() {
        return htAccountStorageService.findAll();
    }

    @Override
    public void removeAccount(String address) throws Exception {
        htAccountStorageService.deleteByAddress(address);
    }

    @Override
    public boolean validateAddress(String address) {
        return WalletUtils.isValidAddress(address);
    }

    @Override
    public BigDecimal getBalance(String address) {
        BigDecimal ethBalance = null;
        try {
            ethBalance = htWalletApi.getBalance(address);
        } catch (Exception e) {
            logger().error(e);
        }
        if (ethBalance == null) {
            return BigDecimal.ZERO;
        }
        return HtWalletApi.convertWeiToHt(ethBalance.toBigInteger());
    }

    @Override
    public String createMultySignAddress(String[] pubKeys, int minSigns) {
        // do nothing
        return null;
    }

    @Override
    public void updateMultySignAddress(String multySignAddress) throws Exception {
        logger().info("HT更新多签合约地址, old: {}, new: {}", HtContext.MULTY_SIGN_ADDRESS, multySignAddress);
        multySignAddress = multySignAddress.toLowerCase();
        // 监听多签地址交易
        htListener.removeListeningAddress(HtContext.MULTY_SIGN_ADDRESS);
        htListener.addListeningAddress(multySignAddress);
        // 更新多签地址
        HtContext.MULTY_SIGN_ADDRESS = multySignAddress;
        // 保存当前多签地址到多签地址历史列表中
        htMultiSignAddressHistoryStorageService.save(multySignAddress);
        // 合约升级后的流程切换操作
        htUpgradeContractSwitchHelper.switchProcessor(multySignAddress);
    }

    @Override
    public void txConfirmedRollback(String txHash) throws Exception {
        HtUnconfirmedTxPo txPo = htUnconfirmedTxStorageService.findByTxHash(txHash);
        if (txPo != null) {
            txPo.setDelete(false);
            txPo.setDeletedHeight(null);
            htUnconfirmedTxStorageService.update(txPo, update -> {
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
        HtERC20Po erc20Po = htERC20Helper.getERC20ByContractAddress(contractAddress);
        if (erc20Po == null) {
            return null;
        }
        HeterogeneousAssetInfo assetInfo = new HeterogeneousAssetInfo();
        assetInfo.setChainId(HtConstant.HT_CHAIN_ID);
        assetInfo.setAssetId(erc20Po.getAssetId());
        assetInfo.setDecimals((byte) erc20Po.getDecimals());
        assetInfo.setSymbol(erc20Po.getSymbol());
        assetInfo.setContractAddress(erc20Po.getAddress());
        return assetInfo;
    }

    @Override
    public HeterogeneousAssetInfo getAssetByAssetId(int assetId) {
        if (HtConstant.HT_ASSET_ID == assetId) {
            return this.getMainAsset();
        }
        HtERC20Po erc20Po = htERC20Helper.getERC20ByAssetId(assetId);
        if (erc20Po == null) {
            return null;
        }
        HeterogeneousAssetInfo assetInfo = new HeterogeneousAssetInfo();
        assetInfo.setChainId(HtConstant.HT_CHAIN_ID);
        assetInfo.setAssetId(erc20Po.getAssetId());
        assetInfo.setDecimals((byte) erc20Po.getDecimals());
        assetInfo.setSymbol(erc20Po.getSymbol());
        assetInfo.setContractAddress(erc20Po.getAddress());
        return assetInfo;
    }

    @Override
    public List<HeterogeneousAssetInfo> getAllInitializedAssets() throws Exception {
        List<HeterogeneousAssetInfo> result;
        List<HtERC20Po> erc20PoList = htERC20Helper.getAllInitializedERC20();
        if (erc20PoList.isEmpty()) {
            result = new ArrayList<>(1);
            result.add(this.getMainAsset());
            return result;
        }
        result = new ArrayList<>(1 + erc20PoList.size());
        result.add(this.getMainAsset());
        erc20PoList.stream().forEach(erc20 -> {
            HeterogeneousAssetInfo assetInfo = new HeterogeneousAssetInfo();
            assetInfo.setChainId(HtConstant.HT_CHAIN_ID);
            assetInfo.setSymbol(erc20.getSymbol());
            assetInfo.setDecimals(erc20.getDecimals());
            assetInfo.setAssetId(erc20.getAssetId());
            assetInfo.setContractAddress(erc20.getAddress());
            result.add(assetInfo);
        });
        return result;
    }

    private static HeterogeneousAssetInfo mainAsset() {
        if (mainAsset == null) {
            mainAsset = new HeterogeneousAssetInfo();
            mainAsset.setChainId(HtConstant.HT_CHAIN_ID);
            mainAsset.setAssetId(HtConstant.HT_ASSET_ID);
            mainAsset.setDecimals((byte) HtConstant.HT_DECIMALS);
            mainAsset.setSymbol(HtConstant.HT_SYMBOL);
            mainAsset.setContractAddress(EMPTY_STRING);
        }
        return mainAsset;
    }

    @Override
    public boolean validateHeterogeneousAssetInfoFromNet(String contractAddress, String symbol, int decimals) throws Exception {
        List<Type> symbolResult = htWalletApi.callViewFunction(contractAddress, HtUtil.getSymbolERC20Function());
        if (symbolResult.isEmpty()) {
            return false;
        }
        String _symbol = symbolResult.get(0).getValue().toString();
        if (!_symbol.equals(symbol)) {
            return false;
        }
        List<Type> decimalsResult = htWalletApi.callViewFunction(contractAddress, HtUtil.getDecimalsERC20Function());
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
        htERC20Helper.saveHeterogeneousAssetInfos(assetInfos);
    }

    @Override
    public void rollbackHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception {
        if (assetInfos == null || assetInfos.isEmpty()) {
            return;
        }
        htERC20Helper.rollbackHeterogeneousAssetInfos(assetInfos);
    }

    @Override
    public void txConfirmedCompleted(String htTxHash, Long blockHeight, String nerveTxHash) throws Exception {
        logger().info("Nerve网络确认HT交易 Nerver hash: {}", nerveTxHash);
        if (StringUtils.isBlank(htTxHash)) {
            logger().warn("Empty htTxHash warning");
            return;
        }
        // 更新db中po的状态，改为delete，在队列任务中确认`ROLLBACK_NUMER`个块之后再移除，便于状态回滚
        HtUnconfirmedTxPo txPo = htUnconfirmedTxStorageService.findByTxHash(htTxHash);
        if (txPo == null) {
            txPo = new HtUnconfirmedTxPo();
            txPo.setTxHash(htTxHash);
        }
        txPo.setDelete(true);
        txPo.setDeletedHeight(blockHeight + HtConstant.ROLLBACK_NUMER);
        logger().info("Nerve网络对[{}]HT交易[{}]确认完成, nerve高度: {}, nerver hash: {}", txPo.getTxType(), txPo.getTxHash(), blockHeight, txPo.getNerveTxHash());
        boolean delete = txPo.isDelete();
        Long deletedHeight = txPo.getDeletedHeight();
        htUnconfirmedTxStorageService.update(txPo, update -> {
            update.setDelete(delete);
            update.setDeletedHeight(deletedHeight);
        });
        // 持久化状态已成功的nerveTx
        if (StringUtils.isNotBlank(nerveTxHash)) {
            logger().debug("持久化状态已成功的nerveTxHash: {}", nerveTxHash);
            htInvokeTxHelper.saveSuccessfulNerve(nerveTxHash);
        }
    }

    @Override
    public HeterogeneousTransactionInfo getDepositTransaction(String txHash) throws Exception {
        // 从DB中获取数据，若获取不到，再到HT网络中获取
        HeterogeneousTransactionInfo txInfo = htTxStorageService.findByTxHash(txHash);
        if (txInfo != null) {
            txInfo.setTxType(HeterogeneousChainTxType.DEPOSIT);
        } else {
            txInfo = htParseTxHelper.parseDepositTransaction(txHash);
            if (txInfo == null) {
                return null;
            }
        }
        if (txInfo.getTxTime() == null) {
            EthBlock.Block block = htWalletApi.getBlockHeaderByHeight(txInfo.getBlockHeight());
            txInfo.setTxTime(block.getTimestamp().longValue());
        }
        return txInfo;
    }

    @Override
    public HeterogeneousTransactionInfo getWithdrawTransaction(String txHash) throws Exception {
        // 从DB中获取数据，若获取不到，再到HT网络中获取
        HeterogeneousTransactionInfo txInfo = htTxStorageService.findByTxHash(txHash);
        if (txInfo != null) {
            txInfo.setTxType(HeterogeneousChainTxType.WITHDRAW);
            Long txTime = txInfo.getTxTime();
            if (StringUtils.isBlank(txInfo.getFrom())) {
                txInfo = htParseTxHelper.parseWithdrawTransaction(txHash);
                if (txInfo == null) {
                    return null;
                }
                txInfo.setTxTime(txTime);
            }
        } else {
            txInfo = htParseTxHelper.parseWithdrawTransaction(txHash);
            if (txInfo == null) {
                return null;
            }
        }
        if (txInfo.getTxTime() == null) {
            EthBlock.Block block = htWalletApi.getBlockHeaderByHeight(txInfo.getBlockHeight());
            txInfo.setTxTime(block.getTimestamp().longValue());
        }
        return txInfo;
    }

    @Override
    public HeterogeneousConfirmedInfo getConfirmedTxInfo(String txHash) throws Exception {
        HeterogeneousConfirmedInfo info = new HeterogeneousConfirmedInfo();
        // 从DB中获取数据，若获取不到，再到HT网络中获取
        HeterogeneousTransactionInfo txInfo = htTxStorageService.findByTxHash(txHash);
        String from;
        Long txTime = null;
        Long blockHeight;
        List<HeterogeneousAddress> signers = null;
        if (txInfo != null && StringUtils.isNotBlank(txInfo.getFrom())) {
            from = txInfo.getFrom();
            blockHeight = txInfo.getBlockHeight();
            signers = txInfo.getSigners();
            txTime = txInfo.getTxTime();
        } else {
            org.web3j.protocol.core.methods.response.Transaction tx = htWalletApi.getTransactionByHash(txHash);
            if (tx == null || tx.getBlockNumber() == null) {
                return null;
            }
            from = tx.getFrom();
            blockHeight = tx.getBlockNumber().longValue();
        }
        if(signers == null || signers.isEmpty()) {
            TransactionReceipt txReceipt = htWalletApi.getTxReceipt(txHash);
            signers = htParseTxHelper.parseSigners(txReceipt, from);
        }
        if (txTime == null) {
            EthBlock.Block block = htWalletApi.getBlockHeaderByHeight(blockHeight);
            if (block == null) {
                return null;
            }
            txTime = block.getTimestamp().longValue();
        }
        info.setMultySignAddress(HtContext.MULTY_SIGN_ADDRESS);
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
        logger().info("验证Huobi网络虚拟银行变更交易，nerveTxHash: {}, signatureData: {}", nerveTxHash, signatureData);
        try {
            // 向Huobi网络请求验证
            boolean isCompleted = htParseTxHelper.isCompletedTransaction(nerveTxHash);
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
                add = add.toLowerCase();
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
                remove = remove.toLowerCase();
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
            String fromAddress = HtContext.ADMIN_ADDRESS;
            if (removeSet.contains(fromAddress)) {
                logger().error("退出的管理员不能参与管理员变更交易");
                return false;
            }
            Function txFunction = HtUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, orginTxCount, signatureData);
            // 验证合约交易合法性
            EthCall ethCall = htWalletApi.validateContractCall(fromAddress, HtContext.MULTY_SIGN_ADDRESS, txFunction);
            if (ethCall.isReverted()) {
                logger().error("[{}]交易验证失败，原因: {}", HeterogeneousChainTxType.CHANGE, ethCall.getRevertReason());
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
            return this.createOrSignManagerChangesTxII(nerveTxHash, addAddresses, removeAddresses, orginTxCount, signatureData, true);
        } catch (NulsException e) {
            // 当出现交易签名不足导致的执行失败时，向CORE重新索要交易的拜占庭签名
            if (ConverterUtil.isInsufficientSignature(e)) {
                HtWaitingTxPo waitingTxPo = htInvokeTxHelper.findEthWaitingTxPo(nerveTxHash);
                if (waitingTxPo != null) {
                    try {
                        String reSendHtTxHash = htResendHelper.reSend(waitingTxPo);
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
            HtUnconfirmedTxPo po = new HtUnconfirmedTxPo();
            po.setTxType(HeterogeneousChainTxType.RECOVERY);
            po.setNerveTxHash(nerveTxHash);
            HtContext.UNCONFIRMED_TX_QUEUE.offer(po);
            htCallBackManager.getTxConfirmedProcessor().txConfirmed(
                    HeterogeneousChainTxType.RECOVERY,
                    nerveTxHash,
                    null, //htTxHash,
                    null, //htTx blockHeight,
                    null, //htTx tx time,
                    HtContext.MULTY_SIGN_ADDRESS,
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
        if (htCommonHelper.constainHash(htTxHash)) {
            logger().info("重复收集充值交易hash: {}，不再重复解析[0]", htTxHash);
            return true;
        }
        reAnalysisLock.lock();
        try {
            if (htCommonHelper.constainHash(htTxHash)) {
                logger().info("重复收集充值交易hash: {}，不再重复解析[1]", htTxHash);
                return true;
            }
            logger().info("重新解析充值交易: {}", htTxHash);
            org.web3j.protocol.core.methods.response.Transaction tx = htWalletApi.getTransactionByHash(htTxHash);
            if (tx == null || tx.getBlockNumber() == null) {
                return false;
            }
            HeterogeneousTransactionInfo txInfo = htParseTxHelper.parseDepositTransaction(tx);
            if (txInfo == null) {
                return false;
            }
            Long blockHeight = tx.getBlockNumber().longValue();
            EthBlock.Block block = htWalletApi.getBlockHeaderByHeight(blockHeight);
            if (block == null) {
                return false;
            }
            Long txTime = block.getTimestamp().longValue();
            htAnalysisTxHelper.analysisTx(tx, txTime, blockHeight);
            htCommonHelper.addHash(htTxHash);
            return true;
        } catch (Exception e) {
            throw e;
        } finally {
            reAnalysisLock.unlock();
        }
    }

    @Override
    public String signWithdrawII(String txHash, String toAddress, BigInteger value, Integer assetId) throws NulsException {
        // 获取管理员账户
        HtAccount account = (HtAccount) this.getAccount(HtContext.ADMIN_ADDRESS);
        account.decrypt(HtContext.ADMIN_ADDRESS_PASSWORD);
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        boolean isContractAsset = assetId > 1;
        String contractAddressERC20;
        if (isContractAsset) {
            contractAddressERC20 = htERC20Helper.getContractAddressByAssetId(assetId);
        } else {
            contractAddressERC20 = HtConstant.ZERO_ADDRESS;
        }
        // 把地址转换成小写
        toAddress = toAddress.toLowerCase();
        String vHash = HtUtil.encoderWithdraw(txHash, toAddress, value, isContractAsset, contractAddressERC20, HtConstant.VERSION);
        logger().debug("提现签名数据: {}, {}, {}, {}, {}, {}", txHash, toAddress, value, isContractAsset, contractAddressERC20, HtConstant.VERSION);
        logger().debug("提现签名vHash: {}, nerveTxHash: {}", vHash, txHash);
        return HtUtil.dataSign(vHash, priKey);
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
            add = add.toLowerCase();
            addAddresses[a] = add;
            if (!addSet.add(add)) {
                logger().error("重复的待加入地址列表");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_2);
            }
        }
        Set<String> removeSet = new HashSet<>();
        for (int r = 0, removeSize = removeAddresses.length; r < removeSize; r++) {
            String remove = removeAddresses[r];
            remove = remove.toLowerCase();
            removeAddresses[r] = remove;
            if (!removeSet.add(remove)) {
                logger().error("重复的待退出地址列表");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_4);
            }
        }
        // 获取管理员账户
        HtAccount account = (HtAccount) this.getAccount(HtContext.ADMIN_ADDRESS);
        account.decrypt(HtContext.ADMIN_ADDRESS_PASSWORD);
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        String vHash = HtUtil.encoderChange(nerveTxHash, addAddresses, orginTxCount, removeAddresses, HtConstant.VERSION);
        logger().debug("变更交易的签名vHash: {}, nerveTxHash: {}", vHash, nerveTxHash);
        return HtUtil.dataSign(vHash, priKey);
    }

    @Override
    public String signUpgradeII(String nerveTxHash, String upgradeContract) throws NulsException {
        // 获取管理员账户
        HtAccount account = (HtAccount) this.getAccount(HtContext.ADMIN_ADDRESS);
        account.decrypt(HtContext.ADMIN_ADDRESS_PASSWORD);
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        // 把地址转换成小写
        upgradeContract = upgradeContract.toLowerCase();
        String vHash = HtUtil.encoderUpgrade(nerveTxHash, upgradeContract, HtConstant.VERSION);
        logger().debug("升级交易的签名vHash: {}, nerveTxHash: {}", vHash, nerveTxHash);
        return HtUtil.dataSign(vHash, priKey);
    }

    @Override
    public Boolean verifySignWithdrawII(String signAddress, String txHash, String toAddress, BigInteger value, Integer assetId, String signed) throws NulsException {
        boolean isContractAsset = assetId > 1;
        String contractAddressERC20;
        if (isContractAsset) {
            contractAddressERC20 = htERC20Helper.getContractAddressByAssetId(assetId);
        } else {
            contractAddressERC20 = HtConstant.ZERO_ADDRESS;
        }
        // 把地址转换成小写
        toAddress = toAddress.toLowerCase();
        String vHash = HtUtil.encoderWithdraw(txHash, toAddress, value, isContractAsset, contractAddressERC20, HtConstant.VERSION);
        logger().debug("[验证签名] 提现数据: {}, {}, {}, {}, {}, {}", txHash, toAddress, value, isContractAsset, contractAddressERC20, HtConstant.VERSION);
        logger().debug("[验证签名] 提现vHash: {}", vHash);
        return HtUtil.verifySign(signAddress, vHash, signed);
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
            add = add.toLowerCase();
            addAddresses[a] = add;
        }
        for (int r = 0, removeSize = removeAddresses.length; r < removeSize; r++) {
            String remove = removeAddresses[r];
            remove = remove.toLowerCase();
            removeAddresses[r] = remove;
        }
        String vHash = HtUtil.encoderChange(nerveTxHash, addAddresses, orginTxCount, removeAddresses, HtConstant.VERSION);
        return HtUtil.verifySign(signAddress, vHash, signed);
    }

    @Override
    public Boolean verifySignUpgradeII(String signAddress, String txHash, String upgradeContract, String signed) throws NulsException {
        // 把地址转换成小写
        upgradeContract = upgradeContract.toLowerCase();
        String vHash = HtUtil.encoderUpgrade(txHash, upgradeContract, HtConstant.VERSION);
        return HtUtil.verifySign(signAddress, vHash, signed);
    }

    @Override
    public boolean isEnoughFeeOfWithdraw(BigDecimal nvtAmount, int hAssetId) {
        IConverterCoreApi coreApi = HtContext.getConverterCoreApi();
        BigDecimal nvtUSD = coreApi.getUsdtPriceByAsset(AssetName.NVT);
        BigDecimal htUSD = coreApi.getUsdtPriceByAsset(AssetName.HT);
        if(null == nvtUSD || null == htUSD){
            logger().error("[withdraw] 提现手续费计算,没有获取到完整的报价. nvtUSD:{}, htUSD:{}", nvtUSD, htUSD);
            throw new NulsRuntimeException(ConverterErrorCode.DATA_NOT_FOUND);
        }
        BigDecimal gasPrice = HtUtil.calGasPriceOfWithdraw(nvtUSD, nvtAmount, htUSD, hAssetId);
        if (gasPrice.toBigInteger().compareTo(HtContext.getEthGasPrice()) >= 0) {
            logger().info("手续费足够，当前网络需要的GasPrice: {} Gwei, 实际计算出的GasPrice: {} Gwei",
                    new BigDecimal(HtContext.getEthGasPrice()).divide(BigDecimal.TEN.pow(9)).toPlainString(),
                    gasPrice.divide(BigDecimal.TEN.pow(9)).toPlainString());
            return true;
        }
        BigDecimal nvtAmountCalc = HtUtil.calNVTOfWithdraw(nvtUSD, new BigDecimal(HtContext.getEthGasPrice()), htUSD, hAssetId);
        logger().warn("手续费不足，当前网络需要的GasPrice: {} Gwei, 实际计算出的GasPrice: {} Gwei, 总共需要的NVT: {}, 用户提供的NVT: {}, 需要追加的NVT: {}",
                new BigDecimal(HtContext.getEthGasPrice()).divide(BigDecimal.TEN.pow(9)).toPlainString(),
                gasPrice.divide(BigDecimal.TEN.pow(9)).toPlainString(),
                nvtAmountCalc.movePointLeft(8).toPlainString(),
                nvtAmount.movePointLeft(8).toPlainString(),
                nvtAmountCalc.subtract(nvtAmount).movePointLeft(8).toPlainString()
        );
        return false;
    }

    public String createOrSignWithdrawTxII(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData, boolean checkOrder) throws NulsException {
        try {
            logger().info("准备发送提现的HT交易，nerveTxHash: {}, signatureData: {}", nerveTxHash, signatureData);
            // 交易准备
            HtWaitingTxPo waitingPo = new HtWaitingTxPo();
            HtAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.WITHDRAW, waitingPo);
            // 保存交易调用参数，设置等待结束时间
            htInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, toAddress, value, assetId, signatureData, account.getOrder(), waitingPo);
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
            HtUnconfirmedTxPo po = new HtUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            boolean isContractAsset = assetId > 1;
            String contractAddressERC20;
            if (isContractAsset) {
                contractAddressERC20 = htERC20Helper.getContractAddressByAssetId(assetId);
                htERC20Helper.loadERC20(contractAddressERC20, po);
            } else {
                contractAddressERC20 = HtConstant.ZERO_ADDRESS;
                po.setDecimals(HtConstant.HT_DECIMALS);
                po.setAssetId(HtConstant.HT_ASSET_ID);
            }
            // 检查是否是NERVE资产绑定的ERC20，是则检查多签合约内是否已经注册此定制的ERC20，否则提现异常
            if (HtContext.getConverterCoreApi().isBoundHeterogeneousAsset(HtConstant.HT_CHAIN_ID, po.getAssetId())
                    && !htParseTxHelper.isMinterERC20(po.getContractAddress())) {
                logger().warn("[{}]不合法的Huobi网络的提现交易, ERC20[{}]已绑定NERVE资产，但合约内未注册", nerveTxHash, po.getContractAddress());
                throw new NulsException(ConverterErrorCode.NOT_BIND_ASSET);
            }
            // 把地址转换成小写
            toAddress = toAddress.toLowerCase();
            po.setTo(toAddress);
            po.setValue(value);
            po.setIfContractAsset(isContractAsset);
            if (isContractAsset) {
                po.setContractAddress(contractAddressERC20);
            }
            Function createOrSignWithdrawFunction = HtUtil.getCreateOrSignWithdrawFunction(nerveTxHash, toAddress, value, isContractAsset, contractAddressERC20, signatureData);
            // 计算GasPrice
            IConverterCoreApi coreApi = HtContext.getConverterCoreApi();
            BigDecimal gasPrice = new BigDecimal(HtContext.getEthGasPrice());
            // 达到指定高度才检查新机制的提现手续费
            if (coreApi.isSupportNewMechanismOfWithdrawalFee()) {
                BigDecimal nvtUSD = coreApi.getUsdtPriceByAsset(AssetName.NVT);
                BigDecimal nvtAmount = coreApi.getFeeOfWithdrawTransaction(nerveTxHash);
                BigDecimal htUSD = coreApi.getUsdtPriceByAsset(AssetName.HT);
                gasPrice = HtUtil.calGasPriceOfWithdraw(nvtUSD, nvtAmount, htUSD, po.getAssetId());
                if (gasPrice == null || gasPrice.toBigInteger().compareTo(HtContext.getEthGasPrice()) < 0) {
                    BigDecimal nvtAmountCalc = HtUtil.calNVTOfWithdraw(nvtUSD, new BigDecimal(HtContext.getEthGasPrice()), htUSD, po.getAssetId());
                    gasPrice = gasPrice == null ? BigDecimal.ZERO : gasPrice;
                    logger().error("[提现]交易[{}]手续费不足，当前Huobi网络的GasPrice: {} Gwei, 实际提供的GasPrice: {} Gwei, 总共需要的NVT: {}, 用户提供的NVT: {}, 需要追加的NVT: {}",
                            nerveTxHash,
                            new BigDecimal(HtContext.getEthGasPrice()).divide(BigDecimal.TEN.pow(9)).toPlainString(),
                            gasPrice.divide(BigDecimal.TEN.pow(9)).toPlainString(),
                            nvtAmountCalc.movePointLeft(8).toPlainString(),
                            nvtAmount.movePointLeft(8).toPlainString(),
                            nvtAmountCalc.subtract(nvtAmount).movePointLeft(8).toPlainString()
                    );
                    throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
                }
                gasPrice = HtUtil.calNiceGasPriceOfWithdraw(new BigDecimal(HtContext.getEthGasPrice()), gasPrice);
            }
            // 验证合约后发出交易
            String htTxHash = this.createTxComplete(nerveTxHash, po, fromAddress, priKey, createOrSignWithdrawFunction, HeterogeneousChainTxType.WITHDRAW, gasPrice.toBigInteger());
            if (StringUtils.isNotBlank(htTxHash)) {
                // 记录提现交易已向HT网络发出
                htPendingTxHelper.commitNervePendingWithdrawTx(nerveTxHash, htTxHash);
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
        logger().info("准备发送虚拟银行变更HT交易，nerveTxHash: {}, signatureData: {}", nerveTxHash, signatureData);
        try {
            // 业务验证
            if (addAddresses == null) {
                addAddresses = new String[0];
            }
            if (removeAddresses == null) {
                removeAddresses = new String[0];
            }
            // 准备数据
            HtUnconfirmedTxPo po = new HtUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            po.setAddAddresses(addAddresses);
            po.setRemoveAddresses(removeAddresses);
            po.setOrginTxCount(orginTxCount);
            // 交易准备
            HtWaitingTxPo waitingPo = new HtWaitingTxPo();
            HtAccount account = this.createTxStartForChange(nerveTxHash, addAddresses, removeAddresses, waitingPo);
            // 保存交易调用参数，设置等待结束时间
            htInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, addAddresses, removeAddresses, orginTxCount, signatureData, account.getOrder(), waitingPo);
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
                add = add.toLowerCase();
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
                remove = remove.toLowerCase();
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
                    htCallBackManager.getTxConfirmedProcessor().txConfirmed(
                            HeterogeneousChainTxType.CHANGE,
                            nerveTxHash,
                            null, //htTxHash,
                            null, //ethTx blockHeight,
                            null, //ethTx tx time,
                            HtContext.MULTY_SIGN_ADDRESS,
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
            Function createOrSignManagerChangeFunction = HtUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, orginTxCount, signatureData);
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
            HtWaitingTxPo waitingPo = new HtWaitingTxPo();
            HtAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.UPGRADE, waitingPo);
            // 保存交易调用参数，设置等待结束时间
            htInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, upgradeContract, signatureData, account.getOrder(), waitingPo);
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
            HtUnconfirmedTxPo po = new HtUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);

            Function function = HtUtil.getCreateOrSignUpgradeFunction(nerveTxHash, upgradeContract, signatureData);
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

    private HtAccount createTxStartForChange(String nerveTxHash, String[] addAddresses, String[] removeAddresses, HtWaitingTxPo po) throws Exception {
        HtAccount txStart = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.CHANGE, po);
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
        Integer order = currentVirtualBanks.get(HtContext.ADMIN_ADDRESS);
        if (order == null) {
            order = 0x0f;
        }
        txStart.setOrder(order);
        logger().info("变更交易的当前节点执行顺序: {}, addAddresses: {}, removeAddresses: {}, currentVirtualBanks: {}", order, Arrays.toString(addAddresses), Arrays.toString(removeAddresses), currentVirtualBanks);
        return txStart;
    }

    private HtAccount createTxStart(String nerveTxHash, HeterogeneousChainTxType txType, HtWaitingTxPo po) throws Exception {
        Map<String, Integer> currentVirtualBanks = HtContext.getConverterCoreApi().currentVirtualBanks(HtConstant.HT_CHAIN_ID);
        po.setCurrentVirtualBanks(currentVirtualBanks);
        String realNerveTxHash = nerveTxHash;
        // 根据nerve交易hash前两位算出顺序种子
        int seed = new BigInteger(realNerveTxHash.substring(0, 1), 16).intValue() + 1;
        int bankSize = HtContext.getConverterCoreApi().getVirtualBankSize();
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
        // 按顺序等待固定时间后再发出HT交易
        int bankOrder = currentVirtualBanks.get(HtContext.ADMIN_ADDRESS);
        if (logger().isDebugEnabled()) {
            logger().debug("顺序计算参数 bankSize: {}, seed: {}, mod: {}, orginBankOrder: {}, bankOrder: {}", bankSize, seed, mod, HtContext.getConverterCoreApi().getVirtualBankOrder(), bankOrder);
        }
        // 向HT网络请求验证
        boolean isCompleted = htParseTxHelper.isCompletedTransaction(nerveTxHash);
        if (isCompleted) {
            logger().info("[{}]交易[{}]已完成", txType, nerveTxHash);
            return HtAccount.newEmptyAccount(bankOrder);
        }
        // 获取管理员账户
        HtAccount account = (HtAccount) this.getAccount(HtContext.ADMIN_ADDRESS);
        account.decrypt(HtContext.ADMIN_ADDRESS_PASSWORD);
        account.setOrder(bankOrder);
        account.setMod(mod);
        account.setBankSize(bankSize);
        return account;
    }


    private String createTxComplete(String nerveTxHash, HtUnconfirmedTxPo po, String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType) throws Exception {
        return this.createTxComplete(nerveTxHash, po, fromAddress, priKey, txFunction, txType, null);
    }
    private String createTxComplete(String nerveTxHash, HtUnconfirmedTxPo po, String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType, BigInteger gasPrice) throws Exception {
        // 验证合约交易合法性
        EthCall ethCall = htWalletApi.validateContractCall(fromAddress, HtContext.MULTY_SIGN_ADDRESS, txFunction);
        if (ethCall.isReverted()) {
            logger().error("[{}]交易验证失败，原因: {}", txType, ethCall.getRevertReason());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, ethCall.getRevertReason());
        }
        // 估算GasLimit
        BigInteger estimateGas = htWalletApi.ethEstimateGas(fromAddress, HtContext.MULTY_SIGN_ADDRESS, txFunction);
        if (logger().isDebugEnabled()) {
            logger().debug("交易类型: {}, 估算的GasLimit: {}", txType, estimateGas);
        }
        if (estimateGas.compareTo(BigInteger.ZERO) == 0) {
            logger().error("[{}]交易验证失败，原因: 估算GasLimit失败", txType);
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, "估算GasLimit失败");
        }
        BigInteger gasLimit = estimateGas.add(HtConstant.BASE_GAS_LIMIT);
        HtSendTransactionPo htSendTransactionPo = htWalletApi.callContract(fromAddress, priKey, HtContext.MULTY_SIGN_ADDRESS, gasLimit, txFunction, BigInteger.ZERO, gasPrice);
        String htTxHash = htSendTransactionPo.getTxHash();
        // docking发起eth交易时，把交易关系记录到db中，并保存当前使用的nonce到关系表中，若有因为price过低不打包交易而重发的需要，则取出当前使用的nonce重发交易
        htTxRelationStorageService.save(htTxHash, nerveTxHash, htSendTransactionPo);
        // 当前节点已发出eth交易
        htInvokeTxHelper.saveSentEthTx(nerveTxHash);

        // 保存未确认交易
        po.setTxHash(htTxHash);
        po.setFrom(fromAddress);
        po.setTxType(txType);
        htUnconfirmedTxStorageService.save(po);
        HtContext.UNCONFIRMED_TX_QUEUE.offer(po);
        // 监听此交易的打包状态
        htListener.addListeningTx(htTxHash);
        logger().info("Nerve网络向HT网络发出[{}]交易, nerveTxHash: {}, 详情: {}", txType, nerveTxHash, po.toString());
        return htTxHash;
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

    public void setHtTxRelationStorageService(HtTxRelationStorageService htTxRelationStorageService) {
        this.htTxRelationStorageService = htTxRelationStorageService;
    }

    public void setHtWalletApi(HtWalletApi htWalletApi) {
        this.htWalletApi = htWalletApi;
    }

    public void setHtUnconfirmedTxStorageService(HtUnconfirmedTxStorageService htUnconfirmedTxStorageService) {
        this.htUnconfirmedTxStorageService = htUnconfirmedTxStorageService;
    }

    public void setHtListener(HtListener htListener) {
        this.htListener = htListener;
    }

    public void setHtAccountStorageService(HtAccountStorageService htAccountStorageService) {
        this.htAccountStorageService = htAccountStorageService;
    }

    public void setHtERC20Helper(HtERC20Helper htERC20Helper) {
        this.htERC20Helper = htERC20Helper;
    }

    public void setHtTxStorageService(HtTxStorageService htTxStorageService) {
        this.htTxStorageService = htTxStorageService;
    }

    public HtCallBackManager getHtCallBackManager() {
        return htCallBackManager;
    }

    public void setHtCallBackManager(HtCallBackManager htCallBackManager) {
        this.htCallBackManager = htCallBackManager;
    }

    public void setHtMultiSignAddressHistoryStorageService(HtMultiSignAddressHistoryStorageService htMultiSignAddressHistoryStorageService) {
        this.htMultiSignAddressHistoryStorageService = htMultiSignAddressHistoryStorageService;
    }

    public void setHtUpgradeContractSwitchHelper(HtUpgradeContractSwitchHelper htUpgradeContractSwitchHelper) {
        this.htUpgradeContractSwitchHelper = htUpgradeContractSwitchHelper;
    }

    public void setHtCommonHelper(HtCommonHelper htCommonHelper) {
        this.htCommonHelper = htCommonHelper;
    }
    
    public void setHtInvokeTxHelper(HtInvokeTxHelper htInvokeTxHelper) {
        this.htInvokeTxHelper = htInvokeTxHelper;
    }

    public void setHtParseTxHelper(HtParseTxHelper htParseTxHelper) {
        this.htParseTxHelper = htParseTxHelper;
    }

    public void setHtAnalysisTxHelper(HtAnalysisTxHelper htAnalysisTxHelper) {
        this.htAnalysisTxHelper = htAnalysisTxHelper;
    }

    public void setHtResendHelper(HtResendHelper htResendHelper) {
        this.htResendHelper = htResendHelper;
    }

    public void setHtPendingTxHelper(HtPendingTxHelper htPendingTxHelper) {
        this.htPendingTxHelper = htPendingTxHelper;
    }
}

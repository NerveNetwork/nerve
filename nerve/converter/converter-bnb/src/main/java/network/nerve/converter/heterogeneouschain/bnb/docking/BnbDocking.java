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
package network.nerve.converter.heterogeneouschain.bnb.docking;

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
import network.nerve.converter.heterogeneouschain.bnb.callback.BnbCallBackManager;
import network.nerve.converter.heterogeneouschain.bnb.constant.BnbConstant;
import network.nerve.converter.heterogeneouschain.bnb.context.BnbContext;
import network.nerve.converter.heterogeneouschain.bnb.core.BNBWalletApi;
import network.nerve.converter.heterogeneouschain.bnb.helper.*;
import network.nerve.converter.heterogeneouschain.bnb.listener.BnbListener;
import network.nerve.converter.heterogeneouschain.bnb.model.*;
import network.nerve.converter.heterogeneouschain.bnb.storage.*;
import network.nerve.converter.heterogeneouschain.bnb.utils.BnbUtil;
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
import static network.nerve.converter.heterogeneouschain.bnb.constant.BnbConstant.VERSION;
import static network.nerve.converter.heterogeneouschain.bnb.constant.BnbConstant.ZERO_ADDRESS;


/**
 * @author: Mimi
 * @date: 2020-08-28
 */
public class BnbDocking implements IHeterogeneousChainDocking {

    private static HeterogeneousAssetInfo mainAsset;
    private static final BnbDocking DOCKING = new BnbDocking();

    protected BNBWalletApi bnbWalletApi;
    protected BnbListener bnbListener;
    protected BnbERC20Helper bnbERC20Helper;
    protected ConverterConfig converterConfig;
    protected BnbTxRelationStorageService bnbTxRelationStorageService;
    protected BnbUnconfirmedTxStorageService bnbUnconfirmedTxStorageService;
    protected BnbMultiSignAddressHistoryStorageService bnbMultiSignAddressHistoryStorageService;
    protected BnbTxStorageService bnbTxStorageService;
    protected BnbAccountStorageService bnbAccountStorageService;
    protected BnbCommonHelper bnbCommonHelper;
    protected BnbUpgradeContractSwitchHelper bnbUpgradeContractSwitchHelper;
    protected ReentrantLock reAnalysisLock = new ReentrantLock();
    private String keystorePath;

    protected BnbCallBackManager bnbCallBackManager;
    protected BnbInvokeTxHelper bnbInvokeTxHelper;
    protected BnbParseTxHelper bnbParseTxHelper;
    protected BnbAnalysisTxHelper bnbAnalysisTxHelper;
    protected BnbResendHelper bnbResendHelper;
    protected BnbPendingTxHelper bnbPendingTxHelper;

    private BnbDocking() {
    }

    private NulsLogger logger() {
        return BnbContext.logger();
    }

    public static BnbDocking getInstance() {
        return DOCKING;
    }

    @Override
    public int version() {
        return VERSION;
    }

    @Override
    public boolean isSupportContractAssetByCurrentChain() {
        return true;
    }

    @Override
    public Integer getChainId() {
        return BnbContext.getConfig().getChainId();
    }

    @Override
    public String getChainSymbol() {
        return BnbContext.getConfig().getSymbol();
    }

    @Override
    public String getCurrentSignAddress() {
        return BnbContext.ADMIN_ADDRESS;
    }

    @Override
    public String getCurrentMultySignAddress() {
        return BnbContext.MULTY_SIGN_ADDRESS;
    }

    @Override
    public String generateAddressByCompressedPublicKey(String compressedPublicKey) {
        return BnbUtil.genEthAddressByCompressedPublickey(compressedPublicKey);
    }

    @Override
    public HeterogeneousAccount importAccountByPriKey(String priKey, String password) throws NulsException {
        if (StringUtils.isNotBlank(BnbContext.ADMIN_ADDRESS)) {
            BnbAccount account = bnbAccountStorageService.findByAddress(BnbContext.ADMIN_ADDRESS);
            account.decrypt(BnbContext.ADMIN_ADDRESS_PASSWORD);
            if (Arrays.equals(account.getPriKey(), HexUtil.decode(priKey))) {
                account.setPriKey(new byte[0]);
                return account;
            }
        }
        if (!FormatValidUtils.validPassword(password)) {
            logger().error("password format wrong");
            throw new NulsException(ConverterErrorCode.PASSWORD_FORMAT_WRONG);
        }
        BnbAccount account = BnbUtil.createAccount(priKey);
        account.encrypt(password);
        try {
            bnbAccountStorageService.save(account);
            // 覆写这个地址作为虚拟银行管理员地址
            BnbContext.ADMIN_ADDRESS = account.getAddress();
            BnbContext.ADMIN_ADDRESS_PUBLIC_KEY = account.getCompressedPublicKey();
            BnbContext.ADMIN_ADDRESS_PASSWORD = password;
            logger().info("向BNB异构组件导入节点出块地址信息, address: [{}]", account.getAddress());
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
        return bnbAccountStorageService.findByAddress(address);
    }

    @Override
    public List<HeterogeneousAccount> getAccountList() {
        return bnbAccountStorageService.findAll();
    }

    @Override
    public void removeAccount(String address) throws Exception {
        bnbAccountStorageService.deleteByAddress(address);
    }

    @Override
    public boolean validateAddress(String address) {
        return WalletUtils.isValidAddress(address);
    }

    @Override
    public BigDecimal getBalance(String address) {
        BigDecimal ethBalance = null;
        try {
            ethBalance = bnbWalletApi.getBalance(address);
        } catch (Exception e) {
            logger().error(e);
        }
        if (ethBalance == null) {
            return BigDecimal.ZERO;
        }
        return BNBWalletApi.convertWeiToBnb(ethBalance.toBigInteger());
    }

    @Override
    public String createMultySignAddress(String[] pubKeys, int minSigns) {
        // do nothing
        return null;
    }

    @Override
    public void updateMultySignAddress(String multySignAddress) throws Exception {
        logger().info("BNB更新多签合约地址, old: {}, new: {}", BnbContext.MULTY_SIGN_ADDRESS, multySignAddress);
        multySignAddress = multySignAddress.toLowerCase();
        // 监听多签地址交易
        bnbListener.removeListeningAddress(BnbContext.MULTY_SIGN_ADDRESS);
        bnbListener.addListeningAddress(multySignAddress);
        // 更新多签地址
        BnbContext.MULTY_SIGN_ADDRESS = multySignAddress;
        // 保存当前多签地址到多签地址历史列表中
        bnbMultiSignAddressHistoryStorageService.save(multySignAddress);
        // 合约升级后的流程切换操作
        bnbUpgradeContractSwitchHelper.switchProcessor(multySignAddress);
    }

    @Override
    public void txConfirmedRollback(String txHash) throws Exception {
        BnbUnconfirmedTxPo txPo = bnbUnconfirmedTxStorageService.findByTxHash(txHash);
        if (txPo != null) {
            txPo.setDelete(false);
            txPo.setDeletedHeight(null);
            bnbUnconfirmedTxStorageService.update(txPo, update -> {
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
        BnbERC20Po erc20Po = bnbERC20Helper.getERC20ByContractAddress(contractAddress);
        if (erc20Po == null) {
            return null;
        }
        HeterogeneousAssetInfo assetInfo = new HeterogeneousAssetInfo();
        assetInfo.setChainId(BnbConstant.BNB_CHAIN_ID);
        assetInfo.setAssetId(erc20Po.getAssetId());
        assetInfo.setDecimals((byte) erc20Po.getDecimals());
        assetInfo.setSymbol(erc20Po.getSymbol());
        assetInfo.setContractAddress(erc20Po.getAddress());
        return assetInfo;
    }

    @Override
    public HeterogeneousAssetInfo getAssetByAssetId(int assetId) {
        if (BnbConstant.BNB_ASSET_ID == assetId) {
            return this.getMainAsset();
        }
        BnbERC20Po erc20Po = bnbERC20Helper.getERC20ByAssetId(assetId);
        if (erc20Po == null) {
            return null;
        }
        HeterogeneousAssetInfo assetInfo = new HeterogeneousAssetInfo();
        assetInfo.setChainId(BnbConstant.BNB_CHAIN_ID);
        assetInfo.setAssetId(erc20Po.getAssetId());
        assetInfo.setDecimals((byte) erc20Po.getDecimals());
        assetInfo.setSymbol(erc20Po.getSymbol());
        assetInfo.setContractAddress(erc20Po.getAddress());
        return assetInfo;
    }

    @Override
    public List<HeterogeneousAssetInfo> getAllInitializedAssets() throws Exception {
        List<HeterogeneousAssetInfo> result;
        List<BnbERC20Po> erc20PoList = bnbERC20Helper.getAllInitializedERC20();
        if (erc20PoList.isEmpty()) {
            result = new ArrayList<>(1);
            result.add(this.getMainAsset());
            return result;
        }
        result = new ArrayList<>(1 + erc20PoList.size());
        result.add(this.getMainAsset());
        erc20PoList.stream().forEach(erc20 -> {
            HeterogeneousAssetInfo assetInfo = new HeterogeneousAssetInfo();
            assetInfo.setChainId(BnbConstant.BNB_CHAIN_ID);
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
            mainAsset.setChainId(BnbConstant.BNB_CHAIN_ID);
            mainAsset.setAssetId(BnbConstant.BNB_ASSET_ID);
            mainAsset.setDecimals((byte) BnbConstant.BNB_DECIMALS);
            mainAsset.setSymbol(BnbConstant.BNB_SYMBOL);
            mainAsset.setContractAddress(EMPTY_STRING);
        }
        return mainAsset;
    }

    @Override
    public boolean validateHeterogeneousAssetInfoFromNet(String contractAddress, String symbol, int decimals) throws Exception {
        List<Type> symbolResult = bnbWalletApi.callViewFunction(contractAddress, BnbUtil.getSymbolERC20Function());
        if (symbolResult.isEmpty()) {
            return false;
        }
        String _symbol = symbolResult.get(0).getValue().toString();
        if (!_symbol.equals(symbol)) {
            return false;
        }
        List<Type> decimalsResult = bnbWalletApi.callViewFunction(contractAddress, BnbUtil.getDecimalsERC20Function());
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
        bnbERC20Helper.saveHeterogeneousAssetInfos(assetInfos);
    }

    @Override
    public void rollbackHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception {
        if (assetInfos == null || assetInfos.isEmpty()) {
            return;
        }
        bnbERC20Helper.rollbackHeterogeneousAssetInfos(assetInfos);
    }

    @Override
    public void txConfirmedCompleted(String bnbTxHash, Long blockHeight, String nerveTxHash) throws Exception {
        logger().info("Nerve网络确认BNB交易 Nerver hash: {}", nerveTxHash);
        if (StringUtils.isBlank(bnbTxHash)) {
            logger().warn("Empty bnbTxHash warning");
            return;
        }
        // 更新db中po的状态，改为delete，在队列任务中确认`ROLLBACK_NUMER`个块之后再移除，便于状态回滚
        BnbUnconfirmedTxPo txPo = bnbUnconfirmedTxStorageService.findByTxHash(bnbTxHash);
        if (txPo == null) {
            txPo = new BnbUnconfirmedTxPo();
            txPo.setTxHash(bnbTxHash);
        }
        txPo.setDelete(true);
        txPo.setDeletedHeight(blockHeight + BnbConstant.ROLLBACK_NUMER);
        logger().info("Nerve网络对[{}]BNB交易[{}]确认完成, nerve高度: {}, nerver hash: {}", txPo.getTxType(), txPo.getTxHash(), blockHeight, txPo.getNerveTxHash());
        boolean delete = txPo.isDelete();
        Long deletedHeight = txPo.getDeletedHeight();
        bnbUnconfirmedTxStorageService.update(txPo, update -> {
            update.setDelete(delete);
            update.setDeletedHeight(deletedHeight);
        });
        // 持久化状态已成功的nerveTx
        if (StringUtils.isNotBlank(nerveTxHash)) {
            logger().debug("持久化状态已成功的nerveTxHash: {}", nerveTxHash);
            bnbInvokeTxHelper.saveSuccessfulNerve(nerveTxHash);
        }
    }

    @Override
    public HeterogeneousTransactionInfo getDepositTransaction(String txHash) throws Exception {
        // 从DB中获取数据，若获取不到，再到BNB网络中获取
        HeterogeneousTransactionInfo txInfo = bnbTxStorageService.findByTxHash(txHash);
        if (txInfo != null) {
            txInfo.setTxType(HeterogeneousChainTxType.DEPOSIT);
        } else {
            txInfo = bnbParseTxHelper.parseDepositTransaction(txHash);
            if (txInfo == null) {
                return null;
            }
        }
        if (txInfo.getTxTime() == null) {
            EthBlock.Block block = bnbWalletApi.getBlockHeaderByHeight(txInfo.getBlockHeight());
            txInfo.setTxTime(block.getTimestamp().longValue());
        }
        return txInfo;
    }

    @Override
    public HeterogeneousTransactionInfo getWithdrawTransaction(String txHash) throws Exception {
        // 从DB中获取数据，若获取不到，再到BNB网络中获取
        HeterogeneousTransactionInfo txInfo = bnbTxStorageService.findByTxHash(txHash);
        if (txInfo != null) {
            txInfo.setTxType(HeterogeneousChainTxType.WITHDRAW);
            Long txTime = txInfo.getTxTime();
            if (StringUtils.isBlank(txInfo.getFrom())) {
                txInfo = bnbParseTxHelper.parseWithdrawTransaction(txHash);
                if (txInfo == null) {
                    return null;
                }
                txInfo.setTxTime(txTime);
            }
        } else {
            txInfo = bnbParseTxHelper.parseWithdrawTransaction(txHash);
            if (txInfo == null) {
                return null;
            }
        }
        if (txInfo.getTxTime() == null) {
            EthBlock.Block block = bnbWalletApi.getBlockHeaderByHeight(txInfo.getBlockHeight());
            txInfo.setTxTime(block.getTimestamp().longValue());
        }
        return txInfo;
    }

    @Override
    public HeterogeneousConfirmedInfo getConfirmedTxInfo(String txHash) throws Exception {
        HeterogeneousConfirmedInfo info = new HeterogeneousConfirmedInfo();
        // 从DB中获取数据，若获取不到，再到BNB网络中获取
        HeterogeneousTransactionInfo txInfo = bnbTxStorageService.findByTxHash(txHash);
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
            org.web3j.protocol.core.methods.response.Transaction tx = bnbWalletApi.getTransactionByHash(txHash);
            if (tx == null || tx.getBlockNumber() == null) {
                return null;
            }
            from = tx.getFrom();
            blockHeight = tx.getBlockNumber().longValue();
        }
        if(signers == null || signers.isEmpty()) {
            TransactionReceipt txReceipt = bnbWalletApi.getTxReceipt(txHash);
            signers = bnbParseTxHelper.parseSigners(txReceipt, from);
        }
        if (txTime == null) {
            EthBlock.Block block = bnbWalletApi.getBlockHeaderByHeight(blockHeight);
            if (block == null) {
                return null;
            }
            txTime = block.getTimestamp().longValue();
        }
        info.setMultySignAddress(BnbContext.MULTY_SIGN_ADDRESS);
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
        logger().info("验证Binance网络虚拟银行变更交易，nerveTxHash: {}, signatureData: {}", nerveTxHash, signatureData);
        try {
            // 向Binance网络请求验证
            boolean isCompleted = bnbParseTxHelper.isCompletedTransaction(nerveTxHash);
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
            String fromAddress = BnbContext.ADMIN_ADDRESS;
            if (removeSet.contains(fromAddress)) {
                logger().error("退出的管理员不能参与管理员变更交易");
                return false;
            }
            Function txFunction = BnbUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, orginTxCount, signatureData);
            // 验证合约交易合法性
            EthCall ethCall = bnbWalletApi.validateContractCall(fromAddress, BnbContext.MULTY_SIGN_ADDRESS, txFunction);
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
                BnbWaitingTxPo waitingTxPo = bnbInvokeTxHelper.findEthWaitingTxPo(nerveTxHash);
                if (waitingTxPo != null) {
                    try {
                        String reSendbnbTxHash = bnbResendHelper.reSend(waitingTxPo);
                        logger().info("Nerve交易[{}]重发完成, reSendbnbTxHash: {}", nerveTxHash, reSendbnbTxHash);
                        return reSendbnbTxHash;
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
            BnbUnconfirmedTxPo po = new BnbUnconfirmedTxPo();
            po.setTxType(HeterogeneousChainTxType.RECOVERY);
            po.setNerveTxHash(nerveTxHash);
            BnbContext.UNCONFIRMED_TX_QUEUE.offer(po);
            bnbCallBackManager.getTxConfirmedProcessor().txConfirmed(
                    HeterogeneousChainTxType.RECOVERY,
                    nerveTxHash,
                    null, //ethTxHash,
                    null, //ethTx blockHeight,
                    null, //ethTx tx time,
                    BnbContext.MULTY_SIGN_ADDRESS,
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
    public Boolean reAnalysisDepositTx(String bnbTxHash) throws Exception {
        if (bnbCommonHelper.constainHash(bnbTxHash)) {
            logger().info("重复收集充值交易hash: {}，不再重复解析[0]", bnbTxHash);
            return true;
        }
        reAnalysisLock.lock();
        try {
            if (bnbCommonHelper.constainHash(bnbTxHash)) {
                logger().info("重复收集充值交易hash: {}，不再重复解析[1]", bnbTxHash);
                return true;
            }
            logger().info("重新解析充值交易: {}", bnbTxHash);
            org.web3j.protocol.core.methods.response.Transaction tx = bnbWalletApi.getTransactionByHash(bnbTxHash);
            if (tx == null || tx.getBlockNumber() == null) {
                return false;
            }
            HeterogeneousTransactionInfo txInfo = bnbParseTxHelper.parseDepositTransaction(tx);
            if (txInfo == null) {
                return false;
            }
            Long blockHeight = tx.getBlockNumber().longValue();
            EthBlock.Block block = bnbWalletApi.getBlockHeaderByHeight(blockHeight);
            if (block == null) {
                return false;
            }
            Long txTime = block.getTimestamp().longValue();
            bnbAnalysisTxHelper.analysisTx(tx, txTime, blockHeight);
            bnbCommonHelper.addHash(bnbTxHash);
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
        BnbAccount account = (BnbAccount) this.getAccount(BnbContext.ADMIN_ADDRESS);
        account.decrypt(BnbContext.ADMIN_ADDRESS_PASSWORD);
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        boolean isContractAsset = assetId > 1;
        String contractAddressERC20;
        if (isContractAsset) {
            contractAddressERC20 = bnbERC20Helper.getContractAddressByAssetId(assetId);
        } else {
            contractAddressERC20 = ZERO_ADDRESS;
        }
        // 把地址转换成小写
        toAddress = toAddress.toLowerCase();
        String vHash = BnbUtil.encoderWithdraw(txHash, toAddress, value, isContractAsset, contractAddressERC20, VERSION);
        logger().debug("提现签名数据: {}, {}, {}, {}, {}, {}", txHash, toAddress, value, isContractAsset, contractAddressERC20, VERSION);
        logger().debug("提现签名vHash: {}, nerveTxHash: {}", vHash, txHash);
        return BnbUtil.dataSign(vHash, priKey);
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
        BnbAccount account = (BnbAccount) this.getAccount(BnbContext.ADMIN_ADDRESS);
        account.decrypt(BnbContext.ADMIN_ADDRESS_PASSWORD);
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        String vHash = BnbUtil.encoderChange(nerveTxHash, addAddresses, orginTxCount, removeAddresses, VERSION);
        logger().debug("变更交易的签名vHash: {}, nerveTxHash: {}", vHash, nerveTxHash);
        return BnbUtil.dataSign(vHash, priKey);
    }

    @Override
    public String signUpgradeII(String nerveTxHash, String upgradeContract) throws NulsException {
        // 获取管理员账户
        BnbAccount account = (BnbAccount) this.getAccount(BnbContext.ADMIN_ADDRESS);
        account.decrypt(BnbContext.ADMIN_ADDRESS_PASSWORD);
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        // 把地址转换成小写
        upgradeContract = upgradeContract.toLowerCase();
        String vHash = BnbUtil.encoderUpgrade(nerveTxHash, upgradeContract, VERSION);
        logger().debug("升级交易的签名vHash: {}, nerveTxHash: {}", vHash, nerveTxHash);
        return BnbUtil.dataSign(vHash, priKey);
    }

    @Override
    public Boolean verifySignWithdrawII(String signAddress, String txHash, String toAddress, BigInteger value, Integer assetId, String signed) throws NulsException {
        boolean isContractAsset = assetId > 1;
        String contractAddressERC20;
        if (isContractAsset) {
            contractAddressERC20 = bnbERC20Helper.getContractAddressByAssetId(assetId);
        } else {
            contractAddressERC20 = ZERO_ADDRESS;
        }
        // 把地址转换成小写
        toAddress = toAddress.toLowerCase();
        String vHash = BnbUtil.encoderWithdraw(txHash, toAddress, value, isContractAsset, contractAddressERC20, VERSION);
        logger().debug("[验证签名] 提现数据: {}, {}, {}, {}, {}, {}", txHash, toAddress, value, isContractAsset, contractAddressERC20, VERSION);
        logger().debug("[验证签名] 提现vHash: {}", vHash);
        return BnbUtil.verifySign(signAddress, vHash, signed);
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
        String vHash = BnbUtil.encoderChange(nerveTxHash, addAddresses, orginTxCount, removeAddresses, VERSION);
        return BnbUtil.verifySign(signAddress, vHash, signed);
    }

    @Override
    public Boolean verifySignUpgradeII(String signAddress, String txHash, String upgradeContract, String signed) throws NulsException {
        // 把地址转换成小写
        upgradeContract = upgradeContract.toLowerCase();
        String vHash = BnbUtil.encoderUpgrade(txHash, upgradeContract, VERSION);
        return BnbUtil.verifySign(signAddress, vHash, signed);
    }

    @Override
    public boolean isEnoughFeeOfWithdraw(BigDecimal nvtAmount, int hAssetId) {
        IConverterCoreApi coreApi = BnbContext.getConverterCoreApi();
        BigDecimal nvtUSD = coreApi.getUsdtPriceByAsset(AssetName.NVT);
        BigDecimal bnbUSD = coreApi.getUsdtPriceByAsset(AssetName.BNB);
        if(null == nvtUSD || null == bnbUSD){
            logger().error("[withdraw] 提现手续费计算,没有获取到完整的报价. nvtUSD:{}, bnbUSD:{}", nvtUSD, bnbUSD);
            throw new NulsRuntimeException(ConverterErrorCode.DATA_NOT_FOUND);
        }
        BigDecimal gasPrice = BnbUtil.calGasPriceOfWithdraw(nvtUSD, nvtAmount, bnbUSD, hAssetId);
        if (gasPrice.toBigInteger().compareTo(BnbContext.getEthGasPrice()) >= 0) {
            logger().info("手续费足够，当前网络需要的GasPrice: {} Gwei, 实际计算出的GasPrice: {} Gwei",
                    new BigDecimal(BnbContext.getEthGasPrice()).divide(BigDecimal.TEN.pow(9)).toPlainString(),
                    gasPrice.divide(BigDecimal.TEN.pow(9)).toPlainString());
            return true;
        }
        BigDecimal nvtAmountCalc = BnbUtil.calNVTOfWithdraw(nvtUSD, new BigDecimal(BnbContext.getEthGasPrice()), bnbUSD, hAssetId);
        logger().warn("手续费不足，当前网络需要的GasPrice: {} Gwei, 实际计算出的GasPrice: {} Gwei, 总共需要的NVT: {}, 用户提供的NVT: {}, 需要追加的NVT: {}",
                new BigDecimal(BnbContext.getEthGasPrice()).divide(BigDecimal.TEN.pow(9)).toPlainString(),
                gasPrice.divide(BigDecimal.TEN.pow(9)).toPlainString(),
                nvtAmountCalc.movePointLeft(8).toPlainString(),
                nvtAmount.movePointLeft(8).toPlainString(),
                nvtAmountCalc.subtract(nvtAmount).movePointLeft(8).toPlainString()
        );
        return false;
    }

    public String createOrSignWithdrawTxII(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData, boolean checkOrder) throws NulsException {
        try {
            logger().info("准备发送提现的BNB交易，nerveTxHash: {}, signatureData: {}", nerveTxHash, signatureData);
            // 交易准备
            BnbWaitingTxPo waitingPo = new BnbWaitingTxPo();
            BnbAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.WITHDRAW, waitingPo);
            // 保存交易调用参数，设置等待结束时间
            bnbInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, toAddress, value, assetId, signatureData, account.getOrder(), waitingPo);
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
            BnbUnconfirmedTxPo po = new BnbUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            boolean isContractAsset = assetId > 1;
            String contractAddressERC20;
            if (isContractAsset) {
                contractAddressERC20 = bnbERC20Helper.getContractAddressByAssetId(assetId);
                bnbERC20Helper.loadERC20(contractAddressERC20, po);
            } else {
                contractAddressERC20 = ZERO_ADDRESS;
                po.setDecimals(BnbConstant.BNB_DECIMALS);
                po.setAssetId(BnbConstant.BNB_ASSET_ID);
            }
            // 检查是否是NERVE资产绑定的ERC20，是则检查多签合约内是否已经注册此定制的ERC20，否则提现异常
            if (BnbContext.getConverterCoreApi().isBoundHeterogeneousAsset(BnbConstant.BNB_CHAIN_ID, po.getAssetId())
                    && !bnbParseTxHelper.isMinterERC20(po.getContractAddress())) {
                logger().warn("[{}]不合法的Binance网络的提现交易, ERC20[{}]已绑定NERVE资产，但合约内未注册", nerveTxHash, po.getContractAddress());
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
            Function createOrSignWithdrawFunction = BnbUtil.getCreateOrSignWithdrawFunction(nerveTxHash, toAddress, value, isContractAsset, contractAddressERC20, signatureData);
            // 计算GasPrice
            IConverterCoreApi coreApi = BnbContext.getConverterCoreApi();
            BigDecimal gasPrice = new BigDecimal(BnbContext.getEthGasPrice());
            // 达到指定高度才检查新机制的提现手续费
            if (coreApi.isSupportNewMechanismOfWithdrawalFee()) {
                BigDecimal nvtUSD = coreApi.getUsdtPriceByAsset(AssetName.NVT);
                BigDecimal nvtAmount = coreApi.getFeeOfWithdrawTransaction(nerveTxHash);
                BigDecimal bnbUSD = coreApi.getUsdtPriceByAsset(AssetName.BNB);
                gasPrice = BnbUtil.calGasPriceOfWithdraw(nvtUSD, nvtAmount, bnbUSD, po.getAssetId());
                if (gasPrice == null || gasPrice.toBigInteger().compareTo(BnbContext.getEthGasPrice()) < 0) {
                    BigDecimal nvtAmountCalc = BnbUtil.calNVTOfWithdraw(nvtUSD, new BigDecimal(BnbContext.getEthGasPrice()), bnbUSD, po.getAssetId());
                    gasPrice = gasPrice == null ? BigDecimal.ZERO : gasPrice;
                    logger().error("[提现]交易[{}]手续费不足，当前Binance网络的GasPrice: {} Gwei, 实际提供的GasPrice: {} Gwei, 总共需要的NVT: {}, 用户提供的NVT: {}, 需要追加的NVT: {}",
                            nerveTxHash,
                            new BigDecimal(BnbContext.getEthGasPrice()).divide(BigDecimal.TEN.pow(9)).toPlainString(),
                            gasPrice.divide(BigDecimal.TEN.pow(9)).toPlainString(),
                            nvtAmountCalc.movePointLeft(8).toPlainString(),
                            nvtAmount.movePointLeft(8).toPlainString(),
                            nvtAmountCalc.subtract(nvtAmount).movePointLeft(8).toPlainString()
                    );
                    throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
                }
                gasPrice = BnbUtil.calNiceGasPriceOfWithdraw(new BigDecimal(BnbContext.getEthGasPrice()), gasPrice);
            }
            // 验证合约后发出交易
            String bnbTxHash = this.createTxComplete(nerveTxHash, po, fromAddress, priKey, createOrSignWithdrawFunction, HeterogeneousChainTxType.WITHDRAW, gasPrice.toBigInteger());
            if (StringUtils.isNotBlank(bnbTxHash)) {
                // 记录提现交易已向BNB网络发出
                bnbPendingTxHelper.commitNervePendingWithdrawTx(nerveTxHash, bnbTxHash);
            }
            return bnbTxHash;
        } catch (Exception e) {
            if (e instanceof NulsException) {
                throw (NulsException) e;
            }
            logger().error(e);
            throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
        }
    }

    public String createOrSignManagerChangesTxII(String nerveTxHash, String[] addAddresses, String[] removeAddresses, int orginTxCount, String signatureData, boolean checkOrder) throws NulsException {
        logger().info("准备发送虚拟银行变更BNB交易，nerveTxHash: {}, signatureData: {}", nerveTxHash, signatureData);
        try {
            // 业务验证
            if (addAddresses == null) {
                addAddresses = new String[0];
            }
            if (removeAddresses == null) {
                removeAddresses = new String[0];
            }
            // 准备数据
            BnbUnconfirmedTxPo po = new BnbUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            po.setAddAddresses(addAddresses);
            po.setRemoveAddresses(removeAddresses);
            po.setOrginTxCount(orginTxCount);
            // 交易准备
            BnbWaitingTxPo waitingPo = new BnbWaitingTxPo();
            BnbAccount account = this.createTxStartForChange(nerveTxHash, addAddresses, removeAddresses, waitingPo);
            // 保存交易调用参数，设置等待结束时间
            bnbInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, addAddresses, removeAddresses, orginTxCount, signatureData, account.getOrder(), waitingPo);
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
                    bnbCallBackManager.getTxConfirmedProcessor().txConfirmed(
                            HeterogeneousChainTxType.CHANGE,
                            nerveTxHash,
                            null, //bnbTxHash,
                            null, //ethTx blockHeight,
                            null, //ethTx tx time,
                            BnbContext.MULTY_SIGN_ADDRESS,
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
            Function createOrSignManagerChangeFunction = BnbUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, orginTxCount, signatureData);
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
            BnbWaitingTxPo waitingPo = new BnbWaitingTxPo();
            BnbAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.UPGRADE, waitingPo);
            // 保存交易调用参数，设置等待结束时间
            bnbInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, upgradeContract, signatureData, account.getOrder(), waitingPo);
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
            BnbUnconfirmedTxPo po = new BnbUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);

            Function function = BnbUtil.getCreateOrSignUpgradeFunction(nerveTxHash, upgradeContract, signatureData);
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

    private BnbAccount createTxStartForChange(String nerveTxHash, String[] addAddresses, String[] removeAddresses, BnbWaitingTxPo po) throws Exception {
        BnbAccount txStart = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.CHANGE, po);
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
        Integer order = currentVirtualBanks.get(BnbContext.ADMIN_ADDRESS);
        if (order == null) {
            order = 0x0f;
        }
        txStart.setOrder(order);
        logger().info("变更交易的当前节点执行顺序: {}, addAddresses: {}, removeAddresses: {}, currentVirtualBanks: {}", order, Arrays.toString(addAddresses), Arrays.toString(removeAddresses), currentVirtualBanks);
        return txStart;
    }

    private BnbAccount createTxStart(String nerveTxHash, HeterogeneousChainTxType txType, BnbWaitingTxPo po) throws Exception {
        Map<String, Integer> currentVirtualBanks = BnbContext.getConverterCoreApi().currentVirtualBanks(BnbConstant.BNB_CHAIN_ID);
        po.setCurrentVirtualBanks(currentVirtualBanks);
        String realNerveTxHash = nerveTxHash;
        // 根据nerve交易hash前两位算出顺序种子
        int seed = new BigInteger(realNerveTxHash.substring(0, 1), 16).intValue() + 1;
        int bankSize = BnbContext.getConverterCoreApi().getVirtualBankSize();
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
        // 按顺序等待固定时间后再发出BNB交易
        int bankOrder = currentVirtualBanks.get(BnbContext.ADMIN_ADDRESS);
        if (logger().isDebugEnabled()) {
            logger().debug("顺序计算参数 bankSize: {}, seed: {}, mod: {}, orginBankOrder: {}, bankOrder: {}", bankSize, seed, mod, BnbContext.getConverterCoreApi().getVirtualBankOrder(), bankOrder);
        }
        // 向BNB网络请求验证
        boolean isCompleted = bnbParseTxHelper.isCompletedTransaction(nerveTxHash);
        if (isCompleted) {
            logger().info("[{}]交易[{}]已完成", txType, nerveTxHash);
            return BnbAccount.newEmptyAccount(bankOrder);
        }
        // 获取管理员账户
        BnbAccount account = (BnbAccount) this.getAccount(BnbContext.ADMIN_ADDRESS);
        account.decrypt(BnbContext.ADMIN_ADDRESS_PASSWORD);
        account.setOrder(bankOrder);
        account.setMod(mod);
        account.setBankSize(bankSize);
        return account;
    }


    private String createTxComplete(String nerveTxHash, BnbUnconfirmedTxPo po, String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType) throws Exception {
        return this.createTxComplete(nerveTxHash, po, fromAddress, priKey, txFunction, txType, null);
    }
    private String createTxComplete(String nerveTxHash, BnbUnconfirmedTxPo po, String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType, BigInteger gasPrice) throws Exception {
        // 验证合约交易合法性
        EthCall ethCall = bnbWalletApi.validateContractCall(fromAddress, BnbContext.MULTY_SIGN_ADDRESS, txFunction);
        if (ethCall.isReverted()) {
            logger().error("[{}]交易验证失败，原因: {}", txType, ethCall.getRevertReason());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, ethCall.getRevertReason());
        }
        // 估算GasLimit
        BigInteger estimateGas = bnbWalletApi.ethEstimateGas(fromAddress, BnbContext.MULTY_SIGN_ADDRESS, txFunction);
        if (logger().isDebugEnabled()) {
            logger().debug("交易类型: {}, 估算的GasLimit: {}", txType, estimateGas);
        }
        if (estimateGas.compareTo(BigInteger.ZERO) == 0) {
            logger().error("[{}]交易验证失败，原因: 估算GasLimit失败", txType);
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, "估算GasLimit失败");
        }
        BigInteger gasLimit = estimateGas.add(BnbConstant.BASE_GAS_LIMIT);
        BnbSendTransactionPo bnbSendTransactionPo = bnbWalletApi.callContract(fromAddress, priKey, BnbContext.MULTY_SIGN_ADDRESS, gasLimit, txFunction, BigInteger.ZERO, gasPrice);
        String bnbTxHash = bnbSendTransactionPo.getTxHash();
        // docking发起eth交易时，把交易关系记录到db中，并保存当前使用的nonce到关系表中，若有因为price过低不打包交易而重发的需要，则取出当前使用的nonce重发交易
        bnbTxRelationStorageService.save(bnbTxHash, nerveTxHash, bnbSendTransactionPo);
        // 当前节点已发出eth交易
        bnbInvokeTxHelper.saveSentEthTx(nerveTxHash);

        // 保存未确认交易
        po.setTxHash(bnbTxHash);
        po.setFrom(fromAddress);
        po.setTxType(txType);
        bnbUnconfirmedTxStorageService.save(po);
        BnbContext.UNCONFIRMED_TX_QUEUE.offer(po);
        // 监听此交易的打包状态
        bnbListener.addListeningTx(bnbTxHash);
        logger().info("Nerve网络向BNB网络发出[{}]交易, nerveTxHash: {}, 详情: {}", txType, nerveTxHash, po.toString());
        return bnbTxHash;
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

    public void setBnbTxRelationStorageService(BnbTxRelationStorageService bnbTxRelationStorageService) {
        this.bnbTxRelationStorageService = bnbTxRelationStorageService;
    }

    public void setBnbWalletApi(BNBWalletApi bnbWalletApi) {
        this.bnbWalletApi = bnbWalletApi;
    }

    public void setBnbUnconfirmedTxStorageService(BnbUnconfirmedTxStorageService bnbUnconfirmedTxStorageService) {
        this.bnbUnconfirmedTxStorageService = bnbUnconfirmedTxStorageService;
    }

    public void setBnbListener(BnbListener bnbListener) {
        this.bnbListener = bnbListener;
    }

    public void setBnbAccountStorageService(BnbAccountStorageService bnbAccountStorageService) {
        this.bnbAccountStorageService = bnbAccountStorageService;
    }

    public void setBnbERC20Helper(BnbERC20Helper bnbERC20Helper) {
        this.bnbERC20Helper = bnbERC20Helper;
    }

    public void setBnbTxStorageService(BnbTxStorageService bnbTxStorageService) {
        this.bnbTxStorageService = bnbTxStorageService;
    }

    public BnbCallBackManager getBnbCallBackManager() {
        return bnbCallBackManager;
    }

    public void setBnbCallBackManager(BnbCallBackManager bnbCallBackManager) {
        this.bnbCallBackManager = bnbCallBackManager;
    }

    public void setBnbMultiSignAddressHistoryStorageService(BnbMultiSignAddressHistoryStorageService bnbMultiSignAddressHistoryStorageService) {
        this.bnbMultiSignAddressHistoryStorageService = bnbMultiSignAddressHistoryStorageService;
    }

    public void setBnbUpgradeContractSwitchHelper(BnbUpgradeContractSwitchHelper bnbUpgradeContractSwitchHelper) {
        this.bnbUpgradeContractSwitchHelper = bnbUpgradeContractSwitchHelper;
    }

    public void setBnbCommonHelper(BnbCommonHelper bnbCommonHelper) {
        this.bnbCommonHelper = bnbCommonHelper;
    }
    
    public void setBnbInvokeTxHelper(BnbInvokeTxHelper bnbInvokeTxHelper) {
        this.bnbInvokeTxHelper = bnbInvokeTxHelper;
    }

    public void setBnbParseTxHelper(BnbParseTxHelper bnbParseTxHelper) {
        this.bnbParseTxHelper = bnbParseTxHelper;
    }

    public void setBnbAnalysisTxHelper(BnbAnalysisTxHelper bnbAnalysisTxHelper) {
        this.bnbAnalysisTxHelper = bnbAnalysisTxHelper;
    }

    public void setBnbResendHelper(BnbResendHelper bnbResendHelper) {
        this.bnbResendHelper = bnbResendHelper;
    }

    public void setBnbPendingTxHelper(BnbPendingTxHelper bnbPendingTxHelper) {
        this.bnbPendingTxHelper = bnbPendingTxHelper;
    }
}

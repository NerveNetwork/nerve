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
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.eth.callback.EthCallBackManager;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import network.nerve.converter.heterogeneouschain.eth.helper.*;
import network.nerve.converter.heterogeneouschain.eth.listener.EthListener;
import network.nerve.converter.heterogeneouschain.eth.model.*;
import network.nerve.converter.heterogeneouschain.eth.storage.*;
import network.nerve.converter.heterogeneouschain.eth.utils.EthUtil;
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
    protected EthUpgradeContractSwitchHelper ethUpgradeContractSwitchHelper;
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
            // 覆写这个地址作为虚拟银行管理员地址
            EthContext.ADMIN_ADDRESS = account.getAddress();
            EthContext.ADMIN_ADDRESS_PUBLIC_KEY = account.getCompressedPublicKey();
            EthContext.ADMIN_ADDRESS_PASSWORD = password;
            logger().info("向ETH异构组件导入节点出块地址信息, address: [{}]", account.getAddress());
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
        return ethAccountStorageService.findByAddress(address);
    }

    @Override
    public List<HeterogeneousAccount> getAccountList() {
        return ethAccountStorageService.findAll();
    }

    @Override
    public void removeAccount(String address) throws Exception {
        ethAccountStorageService.deleteByAddress(address);
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
    public String createMultySignAddress(String[] pubKeys, int minSigns) {
        // do nothing
        return null;
    }

    @Override
    public void updateMultySignAddress(String multySignAddress) throws Exception {
        logger().info("更新多签合约地址, old: {}, new: {}", EthContext.MULTY_SIGN_ADDRESS, multySignAddress);
        multySignAddress = multySignAddress.toLowerCase();
        // 监听多签地址交易
        ethListener.removeListeningAddress(EthContext.MULTY_SIGN_ADDRESS);
        ethListener.addListeningAddress(multySignAddress);
        // 更新多签地址
        EthContext.MULTY_SIGN_ADDRESS = multySignAddress;
        // 保存当前多签地址到多签地址历史列表中
        ethMultiSignAddressHistoryStorageService.save(multySignAddress);
        // 合约升级后的流程切换操作
        ethUpgradeContractSwitchHelper.switchProcessor(multySignAddress);
    }

    protected void txConfirmedCompleted(String ethTxHash, Long blockHeight) throws Exception {
        if (StringUtils.isBlank(ethTxHash)) {
            logger().warn("Empty ethTxHash warning");
            return;
        }
        // 更新db中po的状态，改为delete，在队列任务中确认`ROLLBACK_NUMER`个块之后再移除，便于状态回滚
        EthUnconfirmedTxPo txPo = ethUnconfirmedTxStorageService.findByTxHash(ethTxHash);
        if (txPo == null) {
            txPo = new EthUnconfirmedTxPo();
            txPo.setTxHash(ethTxHash);
        }
        txPo.setDelete(true);
        txPo.setDeletedHeight(blockHeight + EthConstant.ROLLBACK_NUMER);
        logger().info("Nerve网络对[{}]ETH交易[{}]确认完成, nerve高度: {}, nerver hash: {}", txPo.getTxType(), txPo.getTxHash(), blockHeight, txPo.getNerveTxHash());
        boolean delete = txPo.isDelete();
        Long deletedHeight = txPo.getDeletedHeight();
        ethUnconfirmedTxStorageService.update(txPo, update -> {
            update.setDelete(delete);
            update.setDeletedHeight(deletedHeight);
        });
    }

    @Override
    public void txConfirmedCompleted(String ethTxHash, Long blockHeight, String nerveTxHash) throws Exception {
        logger().info("Nerve网络确认ETH交易 Nerver hash: {}", nerveTxHash);
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

    @Override
    public List<HeterogeneousAssetInfo> getAllInitializedAssets() throws Exception {
        List<HeterogeneousAssetInfo> result;
        List<EthERC20Po> erc20PoList = ethERC20Helper.getAllInitializedERC20();
        if (erc20PoList.isEmpty()) {
            result = new ArrayList<>(1);
            result.add(this.getMainAsset());
            return result;
        }
        result = new ArrayList<>(1 + erc20PoList.size());
        result.add(this.getMainAsset());
        erc20PoList.stream().forEach(erc20 -> {
            HeterogeneousAssetInfo assetInfo = new HeterogeneousAssetInfo();
            assetInfo.setChainId(EthConstant.ETH_CHAIN_ID);
            assetInfo.setSymbol(erc20.getSymbol());
            assetInfo.setDecimals(erc20.getDecimals());
            assetInfo.setAssetId(erc20.getAssetId());
            assetInfo.setContractAddress(erc20.getAddress());
            result.add(assetInfo);
        });
        return result;
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
        // 从DB中获取数据，若获取不到，再到ETH网络中获取
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
        // 从DB中获取数据，若获取不到，再到ETH网络中获取
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
        // 从DB中获取数据，若获取不到，再到ETH网络中获取
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
            logger().info("准备发送提现的ETH交易，nerveTxHash: {}", nerveTxHash);
            if (checkGas) {
                if (EthContext.getEthGasPrice().compareTo(EthConstant.HIGH_GAS_PRICE) > 0) {
                    if (EthContext.getConverterCoreApi().isWithdrawalComfired(nerveTxHash)) {
                        logger().info("[提现]交易[{}]已完成", nerveTxHash);
                        return EMPTY_STRING;
                    }
                    logger().warn("Ethereum 网络 Gas Price 高于 {} wei，暂停提现", EthConstant.HIGH_GAS_PRICE);
                    throw new NulsException(ConverterErrorCode.HIGH_GAS_PRICE_OF_ETH);
                }
            }
            // 交易准备
            EthAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.WITHDRAW);
            if (account.isEmpty()) {
                return EMPTY_STRING;
            }
            // 获取管理员账户
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            // 业务验证
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
            // 把地址转换成小写
            toAddress = toAddress.toLowerCase();
            po.setTo(toAddress);
            po.setValue(value);
            po.setIfContractAsset(isContractAsset);
            if (isContractAsset) {
                po.setContractAddress(contractAddressERC20);
            }
            // 校验交易数据是否一致，从合约view函数中提取
            List pendingWithdrawInfo = getPendingWithdrawInfo(nerveTxHash);
            if (!pendingWithdrawInfo.isEmpty() && !validatePendingWithdrawInfo(pendingWithdrawInfo, po)) {
                logger().error("提现交易数据不一致, pendingWithdrawInfo: {}, po: {}", Arrays.toString(pendingWithdrawInfo.toArray()), po.toString());
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_DATA_INCONSISTENCY);
            }
            Function createOrSignWithdrawFunction = EthUtil.getCreateOrSignWithdrawFunction(nerveTxHash, toAddress, value, isContractAsset, contractAddressERC20);
            // 验证合约后发出交易
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
        logger().info("准备发送虚拟银行变更ETH交易，nerveTxHash: {}", nerveTxHash);
        try {
            // 业务验证
            if (addAddresses == null) {
                addAddresses = new String[0];
            }
            if (removeAddresses == null) {
                removeAddresses = new String[0];
            }
            // 准备数据，以此校验交易数据是否一致，从合约view函数中提取
            EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            po.setAddAddresses(addAddresses);
            po.setRemoveAddresses(removeAddresses);
            po.setOrginTxCount(orginTxCount);
            // 交易准备
            EthAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.CHANGE);
            if (account.isEmpty()) {
                // 数据一致性检查
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
            // 数据一致性检查
            this.checkChangeDataInconsistency(nerveTxHash, po, account.getOrder());
            // 若没有加入和退出，则直接发确认交易
            if (addAddresses.length == 0 && removeAddresses.length == 0) {
                logger().info("虚拟银行变更没有加入和退出，直接发确认交易, nerveTxHash: {}", nerveTxHash);
                try {
                    ethCallBackManager.getTxConfirmedProcessor().txConfirmed(
                            HeterogeneousChainTxType.CHANGE,
                            nerveTxHash,
                            null, //ethTxHash,
                            null, //ethTx blockHeight,
                            null, //ethTx tx time,
                            EthContext.MULTY_SIGN_ADDRESS,
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
            Function createOrSignManagerChangeFunction = EthUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, orginTxCount);
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

    @Override
    public String createOrSignUpgradeTx(String nerveTxHash) throws NulsException {
        logger().info("准备发送虚拟银行合约升级授权交易，nerveTxHash: {}", nerveTxHash);
        try {
            // 交易准备
            EthAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.UPGRADE);
            if (account.isEmpty()) {
                return EMPTY_STRING;
            }
            // 获取管理员账户
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);

            Function function = EthUtil.getCreateOrSignUpgradeFunction(nerveTxHash);
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
                logger().debug("恢复机制数据: allManagers is {}, allManagersInContract is {}", Arrays.toString(allManagers), Arrays.toString(allManagersInContract.toArray()));
            }
            if (sendRecoveryII) {
                return this.recoveryII(recoveryDto, allManagersInContract);
            }
            Set<String> removeSet = new HashSet<>(allManagersInContract);
            for(String seed : seedManagers) {
                removeSet.remove(seed);
            }
            // 剔除现有合约管理员
            String[] removes = new String[removeSet.size()];
            if (removeSet.size() != 0) {
                // 执行恢复第一步
                removeSet.toArray(removes);
                String txHash = this.recoveryI(nerveTxHash, removes, recoveryDto);
                // 第一步已执行完成
                if (StringUtils.isBlank(txHash)) {
                    // 执行恢复第二步
                    return this.recoveryII(recoveryDto, allManagersInContract);
                }
                return txHash;
            } else {
                // 执行恢复第二步
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
            logger().info("重复收集充值交易hash: {}，不再重复解析[0]", ethTxHash);
            return true;
        }
        reAnalysisLock.lock();
        try {
            if (ethCommonHelper.constainHash(ethTxHash)) {
                logger().info("重复收集充值交易hash: {}，不再重复解析[1]", ethTxHash);
                return true;
            }
            logger().info("重新解析充值交易: {}", ethTxHash);
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

    private String recovery(String nerveTxKey, String[] adds, String[] removes, EthRecoveryDto recoveryDto) throws NulsException {
        try {
            // 当前虚拟银行触发恢复机制时，持久化保存调用参数，以提供给第二步恢复时使用
            ethTxStorageService.saveRecovery(nerveTxKey, recoveryDto);
            // 准备数据，以此校验交易数据是否一致，从合约view函数中提取
            EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
            po.setRecoveryDto(recoveryDto);
            po.setNerveTxHash(nerveTxKey);
            po.setAddAddresses(adds);
            po.setRemoveAddresses(removes);
            po.setOrginTxCount(1);
            // 交易准备
            EthAccount account = this.createTxStart(nerveTxKey, HeterogeneousChainTxType.RECOVERY);
            if (account.isEmpty()) {
                // 数据一致性检查
                this.checkChangeDataInconsistency(nerveTxKey, po, account.getOrder());
                return EMPTY_STRING;
            }
            // 业务验证
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
            // 数据一致性检查
            this.checkChangeDataInconsistency(nerveTxKey, po, account.getOrder());
            // 获取管理员账户
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            Function createOrSignManagerChangeFunction = EthUtil.getCreateOrSignManagerChangeFunction(nerveTxKey, addList, removeList, 1);
            // 验证合约后发出交易
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
        // 添加当前nerve管理员
        Set<String> allManagerSet = new HashSet<>(Arrays.asList(recoveryDto.getAllManagers()));
        // 检查合约内管理员是否与nerve管理员一致
        if (checkManagerInconsistency(allManagerSet, allManagersInContract)) {
            if (checkManagerInconsistency(new HashSet<>(Arrays.asList(recoveryDto.getSeedManagers())), allManagersInContract)) {
                logger().info("[确认]网络中仅有种子管理员，不再发送第二步恢复，将发起确认重置虚拟银行异构链(合约)");
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
                            null  //txPo.getSigners()
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
            logger().info("[结束]合约已经恢复管理员");
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
        // 从ETH网络上获取当前交易的变更数据
        List pendingManagerChangeInfo = getPendingManagerChangeInfo(nerveTxHash);
        if (pendingManagerChangeInfo.isEmpty() && order > 1) {
            // 当发交易的顺序不在首位，并且未查询到ETH网络上当前交易的变更数据，说明首位发交易的节点比后顺序位的节点慢了一点，那么后顺序位的节点则等待一个间隔周期再获取一次
            if (logger().isDebugEnabled()) {
                logger().debug("非首位节点未获取到变更数据，等待{}秒后重新获取", EthContext.getIntervalWaitting());
            }
            TimeUnit.SECONDS.sleep(EthContext.getIntervalWaitting());
            pendingManagerChangeInfo = getPendingManagerChangeInfo(nerveTxHash);
        }
        if (logger().isDebugEnabled()) {
            logger().debug("一致性检查准备数据, change from contract: {}, sending tx po: [count:{},hash:{},adds:{},removes:{}]",
                    Arrays.toString(pendingManagerChangeInfo.toArray()),
                    po.getOrginTxCount(),
                    nerveTxHash,
                    Arrays.toString(po.getAddAddresses()),
                    Arrays.toString(po.getRemoveAddresses())
            );
        }
        // 检查一致性
        if (!pendingManagerChangeInfo.isEmpty() && !validatePendingManagerChangeInfo(pendingManagerChangeInfo, po)) {
            logger().error("管理员变更交易数据不一致, change from contract: {}, sending tx po: [count:{},hash:{},adds:{},removes:{}]",
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
        // 本地发起的交易，保存nerve交易hash，表示此交易在本节点发出过，不一定成功发出eth交易，仅代表被触发过
        ethTxRelationStorageService.saveNerveTxHash(nerveTxHash);
        String realNerveTxHash = nerveTxHash;
        if (nerveTxHash.startsWith(EthConstant.ETH_RECOVERY_I) || nerveTxHash.startsWith(EthConstant.ETH_RECOVERY_II)) {
            realNerveTxHash = nerveTxHash.substring(EthConstant.ETH_RECOVERY_I.length());
        }
        // 根据nerve交易hash前两位算出顺序种子
        int seed = new BigInteger(realNerveTxHash.substring(0, 1), 16).intValue() + 1;
        int bankSize = EthContext.getConverterCoreApi().getVirtualBankSize();
        if (bankSize > 16) {
            seed += new BigInteger(realNerveTxHash.substring(1, 2), 16).intValue() + 1;
        }
        int mod = seed % bankSize + 1;
        // 按顺序等待固定时间后再发出ETH交易
        int bankOrder = EthContext.getConverterCoreApi().getVirtualBankOrder();
        if (bankOrder < mod) {
            bankOrder += bankSize - (mod - 1);
        } else {
            bankOrder -= mod - 1;
        }
        int waitting = (bankOrder - 1) * EthContext.getIntervalWaitting();
        if (logger().isDebugEnabled()) {
            logger().debug("顺序计算参数 bankSize: {}, seed: {}, mod: {}, orginBankOrder: {}, bankOrder: {}, waitting: {}",
                    bankSize,
                    seed,
                    mod,
                    EthContext.getConverterCoreApi().getVirtualBankOrder(),
                    bankOrder,
                    waitting);
        }
        logger().info("等待{}秒后发出[{}]的ETH交易, nerveTxHash: {}", waitting, txType, nerveTxHash);
        TimeUnit.SECONDS.sleep(waitting);
        // 向ETH网络请求验证
        boolean isCompleted = ethParseTxHelper.isCompletedTransaction(nerveTxHash);
        if (isCompleted) {
            logger().info("[{}]交易[{}]已完成", txType, nerveTxHash);
            return EthAccount.newEmptyAccount(bankOrder);
        }
        // 获取管理员账户
        EthAccount account = (EthAccount) this.getAccount(EthContext.ADMIN_ADDRESS);
        account.decrypt(EthContext.ADMIN_ADDRESS_PASSWORD);
        account.setOrder(bankOrder);
        return account;
    }

    private String createTxComplete(String nerveTxHash, EthUnconfirmedTxPo po, String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType) throws Exception {
        // 验证合约交易合法性
        EthCall ethCall = ethWalletApi.validateContractCall(fromAddress, EthContext.MULTY_SIGN_ADDRESS, txFunction);
        if (ethCall.isReverted()) {
            // 当验证为重复签名时，则说明当前节点已签名此交易，返回 EMPTY_STRING 表明不再执行
            if (ConverterUtil.isDuplicateSignature(ethCall.getRevertReason())) {
                logger().info("[{}]当前节点已签名此交易，不再执行, nerveTxHash: {}", txType, nerveTxHash);
                return EMPTY_STRING;
            }
            logger().error("[{}]交易验证失败，原因: {}", txType, ethCall.getRevertReason());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, ethCall.getRevertReason());
        }
        // 估算GasLimit
        BigInteger estimateGas = ethWalletApi.ethEstimateGas(fromAddress, EthContext.MULTY_SIGN_ADDRESS, txFunction);
        if (logger().isDebugEnabled()) {
            logger().debug("交易类型: {}, 估算的GasLimit: {}", txType, estimateGas);
        }
        if (estimateGas.compareTo(BigInteger.ZERO) == 0) {
            logger().error("[{}]交易验证失败，原因: 估算GasLimit失败", txType);
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, "估算GasLimit失败");
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
        // docking发起eth交易时，把交易关系记录到db中，并保存当前使用的nonce到关系表中，若有因为price过低不打包交易而重发的需要，则取出当前使用的nonce重发交易
        ethTxRelationStorageService.save(ethTxHash, nerveTxHash, ethSendTransactionPo);

        // 保存未确认交易
        po.setTxHash(ethTxHash);
        po.setFrom(fromAddress);
        po.setTxType(txType);
        ethUnconfirmedTxStorageService.save(po);
        EthContext.UNCONFIRMED_TX_QUEUE.offer(po);
        // 监听此交易的打包状态
        ethListener.addListeningTx(ethTxHash);
        logger().info("Nerve网络向ETH网络发出[{}]交易, nerveTxHash: {}, 详情: {}", txType, nerveTxHash, po.toString());
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
        // 检查加入列表
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
        // 检查退出列表
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
        // 当从合约里查询的变更交易中，合并的原始nerve交易数量txCount、合约内多签数量signedCount，都是0时，则是不存在的交易，即返回空集合
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

    public void setEthUpgradeContractSwitchHelper(EthUpgradeContractSwitchHelper ethUpgradeContractSwitchHelper) {
        this.ethUpgradeContractSwitchHelper = ethUpgradeContractSwitchHelper;
    }

    public void setEthCommonHelper(EthCommonHelper ethCommonHelper) {
        this.ethCommonHelper = ethCommonHelper;
    }
}

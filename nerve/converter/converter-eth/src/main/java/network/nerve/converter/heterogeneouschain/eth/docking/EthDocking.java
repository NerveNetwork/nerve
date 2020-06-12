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
import network.nerve.converter.heterogeneouschain.eth.helper.EthERC20Helper;
import network.nerve.converter.heterogeneouschain.eth.helper.EthParseTxHelper;
import network.nerve.converter.heterogeneouschain.eth.listener.EthListener;
import network.nerve.converter.heterogeneouschain.eth.model.EthAccount;
import network.nerve.converter.heterogeneouschain.eth.model.EthERC20Po;
import network.nerve.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.eth.storage.*;
import network.nerve.converter.heterogeneouschain.eth.utils.EthUtil;
import network.nerve.converter.model.bo.*;
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
    private ETHWalletApi ethWalletApi;
    private EthListener ethListener;
    private EthERC20Helper ethERC20Helper;
    private ConverterConfig converterConfig;
    private EthTxRelationStorageService ethTxRelationStorageService;
    private EthUnconfirmedTxStorageService ethUnconfirmedTxStorageService;
    private EthMultiSignAddressHistoryStorageService ethMultiSignAddressHistoryStorageService;
    private EthTxStorageService ethTxStorageService;
    private EthAccountStorageService ethAccountStorageService;
    private EthParseTxHelper ethParseTxHelper;
    private EthCallBackManager ethCallBackManager;
    private String keystorePath;


    private EthDocking() {
    }

    private NulsLogger logger() {
        return EthContext.logger();
    }

    public static EthDocking getInstance() {
        return DOCKING;
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
        //EthContext.MULTY_SIGN_ADDRESS_HISTORY_SET.add(multySignAddress);
    }

    @Override
    public void txConfirmedCompleted(String txHash, Long blockHeight) throws Exception {
        // 更新db中po的状态，改为delete，在队列任务中确认`ROLLBACK_NUMER`个块之后再移除，便于状态回滚
        EthUnconfirmedTxPo txPo = ethUnconfirmedTxStorageService.findByTxHash(txHash);
        if (txPo == null) {
            txPo = new EthUnconfirmedTxPo();
            txPo.setTxHash(txHash);
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
        if (txInfo != null) {
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
    public HeterogeneousConfirmedInfo getChangeVirtualBankConfirmedTxInfo(String txHash) throws Exception {
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
        if(signers == null) {
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
    public String createOrSignWithdrawTx(String nerveTxHash, String toAddress, BigInteger value, Integer assetId) throws NulsException {
        logger().info("准备发送提现的ETH交易，nerveTxHash: {}", nerveTxHash);
        try {
            // 交易准备
            HeterogeneousAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.WITHDRAW);
            if (account == null) {
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
    public String createOrSignManagerChangesTx(String nerveTxHash, String[] addAddresses, String[] removeAddresses, String[] currentAddresses) throws NulsException {
        logger().info("准备发送虚拟银行变更ETH交易，nerveTxHash: {}", nerveTxHash);
        try {
            // 交易准备
            HeterogeneousAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.CHANGE);
            if (account == null) {
                return EMPTY_STRING;
            }
            // 业务验证
            Set<String> allSet = null;
            // add by Mimi at 2020-04-10 赋值为空，变量无意义
            currentAddresses = null;
            // end code by Mimi
            if (currentAddresses != null) {
                allSet = new HashSet<>(Arrays.asList(currentAddresses));
            }
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
                if (allSet != null && allSet.contains(add)) {
                    logger().error("待加入中存在地址-已经是管理员");
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_1);
                }
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
                if (allSet != null && !allSet.contains(remove)) {
                    logger().error("待退出中存在地址-不是管理员");
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_3);
                }
                if (!removeSet.add(remove)) {
                    logger().error("重复的待退出地址列表");
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_4);
                }
                removeList.add(new Address(remove));
            }
            if (currentAddresses != null && currentAddresses.length + addAddresses.length - removeAddresses.length > 15) {
                logger().error("Maximum 15 managers");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_5);
            }
            // 获取管理员账户
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            if (removeSet.contains(fromAddress)) {
                logger().error("退出的管理员不能参与管理员变更交易");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_6);
            }
            EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            po.setAddAddresses(addAddresses);
            po.setRemoveAddresses(removeAddresses);
            po.setCurrentAddresses(currentAddresses);
            // 校验交易数据是否一致，从合约view函数中提取
            List pendingManagerChangeInfo = getPendingManagerChangeInfo(nerveTxHash);
            if (!pendingManagerChangeInfo.isEmpty() && !validatePendingManagerChangeInfo(pendingManagerChangeInfo, po)) {
                logger().error("管理员变更交易数据不一致, pendingManagerChangeInfo: {}, po: {}", Arrays.toString(pendingManagerChangeInfo.toArray()), po.toString());
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_DATA_INCONSISTENCY);
            }
            Function createOrSignManagerChangeFunction = EthUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList);
            // 验证合约后发出交易
            return this.createTxComplete(nerveTxHash, po, fromAddress, priKey, createOrSignManagerChangeFunction, HeterogeneousChainTxType.CHANGE);
        } catch (Exception e) {
            logger().error(e);
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
            HeterogeneousAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.WITHDRAW);
            if (account == null) {
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

    private HeterogeneousAccount createTxStart(String nerveTxHash, HeterogeneousChainTxType txType) throws Exception {
        // 根据nerve交易hash前两位算出顺序种子
        int seed = new BigInteger(nerveTxHash.substring(0, 1), 16).intValue() + 1;
        int bankSize = EthContext.getConverterCoreApi().getVirtualBankSize();
        if (bankSize > 16) {
            seed += new BigInteger(nerveTxHash.substring(1, 2), 16).intValue() + 1;
        }
        int mod = seed % bankSize + 1;
        // 按顺序等待固定时间后再发出ETH交易
        int bankOrder = EthContext.getConverterCoreApi().getVirtualBankOrder();
        if (bankOrder < mod) {
            bankOrder += bankSize - (mod - 1);
        } else {
            bankOrder -= mod - 1;
        }
        int waitting = (bankOrder - 1) * EthConstant.INTERVAL_WAITTING;
        if (logger().isDebugEnabled()) {
            logger().debug("顺序计算参数 seed: {}, mod: {}, bankOrder: {}, waitting: {}", seed, mod, bankOrder, waitting);
        }
        logger().info("等待{}秒后发出[{}]的ETH交易", waitting, txType);
        TimeUnit.SECONDS.sleep(waitting);
        // 向ETH网络请求验证
        boolean isCompleted = isCompletedTransaction(nerveTxHash);
        if (isCompleted) {
            logger().info("[{}]交易[{}]已完成", txType, nerveTxHash);
            return null;
        }
        // 获取管理员账户
        HeterogeneousAccount account = this.getAccount(EthContext.ADMIN_ADDRESS);
        account.decrypt(EthContext.ADMIN_ADDRESS_PASSWORD);
        return account;
    }

    private String createTxComplete(String nerveTxHash, EthUnconfirmedTxPo po, String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType) throws Exception {
        // 验证合约交易合法性
        EthCall ethCall = ethWalletApi.validateContractCall(fromAddress, EthContext.MULTY_SIGN_ADDRESS, txFunction);
        if (ethCall.isReverted()) {
            logger().error("[{}]交易验证失败，原因: {}", txType, ethCall.getRevertReason());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, ethCall.getRevertReason());
        }
        // 估算GasLimit
        BigInteger estimateGas = ethWalletApi.ethEstimateGas(fromAddress, EthContext.MULTY_SIGN_ADDRESS, txFunction);
        if (estimateGas.compareTo(BigInteger.ZERO) == 0) {
            logger().error("[{}]交易验证失败，原因: 估算GasLimit失败", txType);
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, "估算GasLimit失败");
        }
        BigInteger hardGasLimit = txType == HeterogeneousChainTxType.CHANGE ? EthConstant.ETH_GAS_LIMIT_OF_MULTY_SIGN_CHANGE : EthConstant.ETH_GAS_LIMIT_OF_MULTY_SIGN;
        BigInteger gasLimit = hardGasLimit.compareTo(estimateGas) > 0 ? hardGasLimit : estimateGas;
        String ethTxHash = ethWalletApi.callContract(fromAddress, priKey, EthContext.MULTY_SIGN_ADDRESS, gasLimit, txFunction);
        // docking发起交易时，把交易关系记录到db中
        ethTxRelationStorageService.save(ethTxHash, nerveTxHash);

        // 保存未确认交易
        po.setTxHash(ethTxHash);
        po.setFrom(fromAddress);
        po.setTxType(txType);
        ethUnconfirmedTxStorageService.save(po);
        EthContext.UNCONFIRMED_TX_QUEUE.offer(po);
        // 监听此交易的打包状态
        ethListener.addListeningTx(ethTxHash);
        logger().info("Nerve网络向ETH网络发出[{}]请求, 详情: {}", txType, po.superString());
        return ethTxHash;
    }

    private boolean validatePendingWithdrawInfo(List pendingWithdrawInfo, EthUnconfirmedTxPo po) {
        String to = pendingWithdrawInfo.get(1).toString();
        BigInteger value = new BigInteger(pendingWithdrawInfo.get(2).toString());
        Boolean isERC20 = Boolean.parseBoolean(pendingWithdrawInfo.get(3).toString());
        String erc20 = pendingWithdrawInfo.get(4).toString();
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
        if (!list.isEmpty() && Integer.parseInt(list.get(5).toString()) == 0) {
            return Collections.emptyList();
        }
        return list;
    }

    private boolean validatePendingManagerChangeInfo(List pendingManagerChangeInfo, EthUnconfirmedTxPo po) {
        String nerveTxHash = po.getNerveTxHash();
        int i = 1;
        String key = (String) pendingManagerChangeInfo.get(i++);
        if (!nerveTxHash.equals(key)) {
            // 新的成员变更交易，覆盖之前合约内的变更交易
            return true;
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
        if (!list.isEmpty() && Integer.parseInt(list.get(list.size() - 1).toString()) == 0) {
            return Collections.emptyList();
        }
        return list;
    }

    private boolean isCompletedTransaction(String nerveTxHash) throws Exception {
        Function isCompletedFunction = EthUtil.getIsCompletedFunction(nerveTxHash);
        List<Type> valueTypes = ethWalletApi.callViewFunction(EthContext.MULTY_SIGN_ADDRESS, isCompletedFunction);
        boolean isCompleted = Boolean.parseBoolean(valueTypes.get(0).getValue().toString());
        return isCompleted;
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
}

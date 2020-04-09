/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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
package nerve.network.converter.heterogeneouschain.eth.docking;

import nerve.network.converter.config.ConverterConfig;
import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import nerve.network.converter.enums.HeterogeneousChainTxType;
import nerve.network.converter.heterogeneouschain.eth.constant.EthConstant;
import nerve.network.converter.heterogeneouschain.eth.context.EthContext;
import nerve.network.converter.heterogeneouschain.eth.core.ETHWalletApi;
import nerve.network.converter.heterogeneouschain.eth.helper.EthERC20Helper;
import nerve.network.converter.heterogeneouschain.eth.helper.EthParseTxHelper;
import nerve.network.converter.heterogeneouschain.eth.listener.EthListener;
import nerve.network.converter.heterogeneouschain.eth.model.EthAccount;
import nerve.network.converter.heterogeneouschain.eth.model.EthERC20Po;
import nerve.network.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import nerve.network.converter.heterogeneouschain.eth.storage.EthAccountStorageService;
import nerve.network.converter.heterogeneouschain.eth.storage.EthTxRelationStorageService;
import nerve.network.converter.heterogeneouschain.eth.storage.EthTxStorageService;
import nerve.network.converter.heterogeneouschain.eth.storage.EthUnconfirmedTxStorageService;
import nerve.network.converter.heterogeneouschain.eth.utils.EthUtil;
import nerve.network.converter.model.bo.HeterogeneousAccount;
import nerve.network.converter.model.bo.HeterogeneousAssetInfo;
import nerve.network.converter.model.bo.HeterogeneousConfirmedInfo;
import nerve.network.converter.model.bo.HeterogeneousTransactionInfo;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.FormatValidUtils;
import io.nuls.core.model.StringUtils;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static io.protostuff.ByteString.EMPTY_STRING;


/**
 * @author: Chino
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
    private EthTxStorageService ethTxStorageService;
    private EthAccountStorageService ethAccountStorageService;
    private EthParseTxHelper ethParseTxHelper;
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
        return EthConstant.ETH_CHAIN_ID;
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
            logger().info("向ETH异构组件导入节点出块地址信息, address: {}", account.getAddress());
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
    public void txConfirmedCompleted(String txHash, Long blockHeight) throws Exception {
        // 更新db中po的状态，改为delete，在队列任务中确认`ROLLBACK_NUMER`个块之后再移除，便于状态回滚
        EthUnconfirmedTxPo txPo = ethUnconfirmedTxStorageService.findByTxHash(txHash);
        if (txPo == null) {
            txPo = new EthUnconfirmedTxPo();
            txPo.setTxHash(txHash);
            txPo.setDelete(true);
            txPo.setDeletedHeight(blockHeight + EthConstant.ROLLBACK_NUMER);
        } else {
            txPo.setDelete(true);
            txPo.setDeletedHeight(blockHeight + EthConstant.ROLLBACK_NUMER);
        }
        ethUnconfirmedTxStorageService.save(txPo);
    }

    @Override
    public void txConfirmedRollback(String txHash) throws Exception {
        EthUnconfirmedTxPo txPo = ethUnconfirmedTxStorageService.findByTxHash(txHash);
        if (txPo != null) {
            txPo.setDelete(false);
            txPo.setDeletedHeight(null);
            ethUnconfirmedTxStorageService.save(txPo);
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
        if(EthConstant.ETH_ASSET_ID == assetId) {
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
    public List<HeterogeneousAssetInfo> getAllInitializedAssets() {
        List<HeterogeneousAssetInfo> result;
        List<EthERC20Po> erc20PoList = ethERC20Helper.getAllInitializedERC20();
        if(erc20PoList.isEmpty()) {
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
            return txInfo;
        }
        txInfo = ethParseTxHelper.parseDepositTransaction(txHash);
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
            return txInfo;
        }
        txInfo = ethParseTxHelper.parseWithdrawTransaction(txHash);
        if (txInfo.getTxTime() == null) {
            EthBlock.Block block = ethWalletApi.getBlockHeaderByHeight(txInfo.getBlockHeight());
            txInfo.setTxTime(block.getTimestamp().longValue());
        }
        return txInfo;
    }

    @Override
    public HeterogeneousConfirmedInfo getChangeVirtualBankConfirmedTxInfo(String txHash) throws Exception {
        HeterogeneousConfirmedInfo info = new HeterogeneousConfirmedInfo();
        org.web3j.protocol.core.methods.response.Transaction tx = ethWalletApi.getTransactionByHash(txHash);
        if (tx == null || tx.getBlockNumber() == null) {
            return null;
        }
        EthBlock.Block block = ethWalletApi.getBlockHeaderByHeight(tx.getBlockNumber().longValue());
        if (block == null) {
            return null;
        }
        info.setMultySignAddress(EthContext.MULTY_SIGN_ADDRESS);
        info.setTxTime(block.getTimestamp().longValue());
        return info;
    }

    @Override
    public String createOrSignWithdrawTx(String nerveTxHash, String toAddress, BigInteger value, Integer assetId) throws NulsException {
        try {
            // 向ETH网络请求验证
            boolean isCompleted = isCompletedTransaction(nerveTxHash);
            if (isCompleted) {
                logger().info("提现交易[{}]已完成", nerveTxHash);
                //throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_COMPLETED);
                return EMPTY_STRING;
            }
            // 获取管理员账户
            HeterogeneousAccount account = this.getAccount(EthContext.ADMIN_ADDRESS);
            account.decrypt(EthContext.ADMIN_ADDRESS_PASSWORD);
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            boolean isContractAsset = assetId > 1;
            String contractAddressERC20;
            EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            if (isContractAsset) {
                contractAddressERC20 = ethERC20Helper.getContractAddressByAssetId(assetId);
                ethERC20Helper.loadERC20(contractAddressERC20, po);
            } else {
                contractAddressERC20 = EthConstant.ZERO_ADDRESS;
                po.setDecimals(EthConstant.ETH_DECIMALS);
                po.setAssetId(EthConstant.ETH_ASSET_ID);
            }
            po.setFrom(fromAddress);
            po.setTo(toAddress);
            po.setValue(value);
            po.setIfContractAsset(isContractAsset);
            if (isContractAsset) {
                po.setContractAddress(contractAddressERC20);
            }
            // 校验交易数据是否一致，从合约view函数中提取
            List pendingWithdrawInfo = getPendingWithdrawInfo(nerveTxHash);
            if (!pendingWithdrawInfo.isEmpty() && !validatePendingWithdrawInfo(pendingWithdrawInfo, po)) {
                logger().error("提现交易数据不一致");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_DATA_INCONSISTENCY);
            }

            Function createOrSignWithdrawFunction = EthUtil.getCreateOrSignWithdrawFunction(nerveTxHash, toAddress, value, isContractAsset, contractAddressERC20);
            // 验证合约交易合法性
            EthCall ethCall = ethWalletApi.validateContractCall(fromAddress, EthContext.MULTY_SIGN_ADDRESS, createOrSignWithdrawFunction);
            if (ethCall.isReverted()) {
                logger().error("提现交易验证失败，原因: {}", ethCall.getRevertReason());
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, ethCall.getRevertReason());
            }

            String ethTxHash = ethWalletApi.callContract(fromAddress, priKey, EthContext.MULTY_SIGN_ADDRESS, createOrSignWithdrawFunction);
            // docking发起交易时，把交易关系记录到db中
            ethTxRelationStorageService.save(ethTxHash, nerveTxHash);

            // 保存未确认交易
            po.setTxHash(ethTxHash);
            po.setTxType(HeterogeneousChainTxType.WITHDRAW);
            ethUnconfirmedTxStorageService.save(po);
            EthContext.UNCONFIRMED_TX_QUEUE.offer(po);
            // 监听此交易的打包状态
            ethListener.addListeningTx(ethTxHash);
            logger().info("Nerve网络向ETH网络发出提现请求, 详情: {}", po.superString());
            return ethTxHash;
        } catch (Exception e) {
            logger().error(e);
            throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
        }
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
        if (!erc20.equals(po.getContractAddress())) {
            return false;
        }
        return true;
    }

    private List getPendingWithdrawInfo(String nerveTxHash) throws Exception {
        Function pendingWithdrawTransactionFunction = EthUtil.getPendingWithdrawTransactionFunction(nerveTxHash);
        List<Type> valueTypes = ethWalletApi.callViewFunction(EthContext.MULTY_SIGN_ADDRESS, pendingWithdrawTransactionFunction);
        List<Object> list = valueTypes.stream().map(type -> type.getValue()).collect(Collectors.toList());
        if(!list.isEmpty() && Integer.parseInt(list.get(5).toString()) == 0) {
            return Collections.emptyList();
        }
        return list;
    }

    private boolean validatePendingManagerChangeInfo(List pendingManagerChangeInfo, EthUnconfirmedTxPo po) {
        // 检查加入列表
        int poAddSize;
        String[] addAddresses = po.getAddAddresses();
        if (addAddresses != null) {
            poAddSize = addAddresses.length;
        } else {
            poAddSize = 0;
        }
        List<Address> adds = (List<Address>) pendingManagerChangeInfo.get(1);
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
        List<Address> removes = (List<Address>) pendingManagerChangeInfo.get(2);
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
        if(!list.isEmpty() && Integer.parseInt(list.get(4).toString()) == 0) {
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

    @Override
    public String createOrSignManagerChangesTx(String nerveTxHash, String[] addAddresses, String[] removeAddresses, String[] currentAddresses) throws NulsException {
        try {
            Set<String> allSet = null;
            if (currentAddresses != null) {
                allSet = new HashSet<>(Arrays.asList(currentAddresses));
            }
            Set<String> addSet = new HashSet<>();
            List<Address> addList = new ArrayList<>();
            for (String add : addAddresses) {
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
            for (String remove : removeAddresses) {
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
            HeterogeneousAccount account = this.getAccount(EthContext.ADMIN_ADDRESS);
            account.decrypt(EthContext.ADMIN_ADDRESS_PASSWORD);
            String fromAddress = account.getAddress();
            String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
            if (removeSet.contains(fromAddress)) {
                logger().error("退出的管理员不能参与管理员变更交易");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_MANAGER_CHANGE_ERROR_6);
            }
            // 向ETH网络请求验证
            boolean isCompleted = isCompletedTransaction(nerveTxHash);
            if (isCompleted) {
                logger().info("管理员变更交易[{}]已完成", nerveTxHash);
                //throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_COMPLETED);
                return EMPTY_STRING;
            }

            EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            po.setAddAddresses(addAddresses);
            po.setRemoveAddresses(removeAddresses);
            po.setCurrentAddresses(currentAddresses);
            // 校验交易数据是否一致，从合约view函数中提取
            List pendingManagerChangeInfo = getPendingManagerChangeInfo(nerveTxHash);
            if (!pendingManagerChangeInfo.isEmpty() && !validatePendingManagerChangeInfo(pendingManagerChangeInfo, po)) {
                logger().error("管理员变更交易数据不一致");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_DATA_INCONSISTENCY);
            }

            Function createOrSignManagerChangeFunction = EthUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList);
            // 验证合约交易合法性
            EthCall ethCall = ethWalletApi.validateContractCall(fromAddress, EthContext.MULTY_SIGN_ADDRESS, createOrSignManagerChangeFunction);
            if (ethCall.isReverted()) {
                logger().error("管理员变更交易验证失败，原因: {}", ethCall.getRevertReason());
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TRANSACTION_CONTRACT_VALIDATION_FAILED, ethCall.getRevertReason());
            }

            String ethTxHash = ethWalletApi.callContract(fromAddress, priKey, EthContext.MULTY_SIGN_ADDRESS, createOrSignManagerChangeFunction);
            // docking发起交易时，把交易关系记录到db中
            ethTxRelationStorageService.save(ethTxHash, nerveTxHash);
            // 保存未确认交易
            po.setTxHash(ethTxHash);
            po.setFrom(fromAddress);
            po.setTxType(HeterogeneousChainTxType.CHANGE);
            ethUnconfirmedTxStorageService.save(po);
            EthContext.UNCONFIRMED_TX_QUEUE.offer(po);
            // 监听此交易的打包状态
            ethListener.addListeningTx(ethTxHash);
            return ethTxHash;
        } catch (Exception e) {
            logger().error(e);
            throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
        }
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
}

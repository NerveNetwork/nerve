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
package network.nerve.converter.heterogeneouschain.ethII.docking;

import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.docking.EthDocking;
import network.nerve.converter.heterogeneouschain.eth.model.EthAccount;
import network.nerve.converter.heterogeneouschain.eth.model.EthSendTransactionPo;
import network.nerve.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.ethII.constant.EthIIConstant;
import network.nerve.converter.heterogeneouschain.ethII.helper.*;
import network.nerve.converter.heterogeneouschain.ethII.model.EthWaitingTxPo;
import network.nerve.converter.heterogeneouschain.ethII.utils.EthIIUtil;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousChangePendingInfo;
import network.nerve.converter.model.bo.HeterogeneousConfirmedInfo;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.utils.ConverterUtil;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static io.protostuff.ByteString.EMPTY_STRING;
import static network.nerve.converter.heterogeneouschain.eth.constant.EthConstant.ZERO_ADDRESS;
import static network.nerve.converter.heterogeneouschain.ethII.constant.EthIIConstant.VERSION;


/**
 * @author: Mimi
 * @date: 2020-08-28
 */
public class EthIIDocking extends EthDocking {

    private static final EthIIDocking DOCKING = new EthIIDocking();
    protected EthIIInvokeTxHelper ethIIInvokeTxHelper;
    protected EthIIParseTxHelper ethIIParseTxHelper;
    protected EthIIAnalysisTxHelper ethIIAnalysisTxHelper;
    protected EthIIResendHelper ethIIResendHelper;
    protected EthIIPendingTxHelper ethIIPendingTxHelper;

    private EthIIDocking() {
    }

    private NulsLogger logger() {
        return EthContext.logger();
    }

    public static EthIIDocking getInstance() {
        return DOCKING;
    }

    @Override
    public int version() {
        return EthIIConstant.VERSION;
    }

    @Override
    public void txConfirmedCompleted(String ethTxHash, Long blockHeight, String nerveTxHash) throws Exception {
        super.txConfirmedCompleted(ethTxHash, blockHeight);
        // 持久化状态已成功的nerveTx
        if (StringUtils.isNotBlank(nerveTxHash)) {
            logger().debug("持久化状态已成功的nerveTxHash: {}", nerveTxHash);
            ethIIInvokeTxHelper.saveSuccessfulNerve(nerveTxHash);
        }
    }

    @Override
    public HeterogeneousTransactionInfo getDepositTransaction(String txHash) throws Exception {
        // 从DB中获取数据，若获取不到，再到ETH网络中获取
        HeterogeneousTransactionInfo txInfo = ethTxStorageService.findByTxHash(txHash);
        if (txInfo != null) {
            txInfo.setTxType(HeterogeneousChainTxType.DEPOSIT);
        } else {
            txInfo = ethIIParseTxHelper.parseDepositTransaction(txHash);
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
                txInfo = ethIIParseTxHelper.parseWithdrawTransaction(txHash);
                if (txInfo == null) {
                    return null;
                }
                txInfo.setTxTime(txTime);
            }
        } else {
            txInfo = ethIIParseTxHelper.parseWithdrawTransaction(txHash);
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
            org.web3j.protocol.core.methods.response.Transaction tx = ethWalletApi.getTransactionByHash(txHash);
            if (tx == null || tx.getBlockNumber() == null) {
                return null;
            }
            from = tx.getFrom();
            blockHeight = tx.getBlockNumber().longValue();
        }
        if(signers == null || signers.isEmpty()) {
            TransactionReceipt txReceipt = ethWalletApi.getTxReceipt(txHash);
            signers = ethIIParseTxHelper.parseSigners(txReceipt, from);
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
        logger().info("验证Ethereum网络虚拟银行变更交易，nerveTxHash: {}, signatureData: {}", nerveTxHash, signatureData);
        try {
            // 业务验证
            if (addAddresses == null) {
                addAddresses = new String[0];
            }
            if (removeAddresses == null) {
                removeAddresses = new String[0];
            }
            // 向ETH网络请求验证
            boolean isCompleted = ethParseTxHelper.isCompletedTransaction(nerveTxHash);
            if (isCompleted) {
                logger().info("[{}]交易[{}]已完成", HeterogeneousChainTxType.CHANGE, nerveTxHash);
                return true;
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
            String fromAddress = EthContext.ADMIN_ADDRESS;
            if (removeSet.contains(fromAddress)) {
                logger().error("退出的管理员不能参与管理员变更交易");
                return false;
            }
            Function txFunction = EthIIUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, orginTxCount, signatureData);
            // 验证合约交易合法性
            EthCall ethCall = ethWalletApi.validateContractCall(fromAddress, EthContext.MULTY_SIGN_ADDRESS, txFunction);
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
                EthWaitingTxPo waitingTxPo = ethIIInvokeTxHelper.findEthWaitingTxPo(nerveTxHash);
                if (waitingTxPo != null) {
                    try {
                        String reSendEthTxHash = ethIIResendHelper.reSend(waitingTxPo);
                        logger().info("Nerve交易[{}]重发完成, reSendEthTxHash: {}", nerveTxHash, reSendEthTxHash);
                        return reSendEthTxHash;
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
            EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
            po.setTxType(HeterogeneousChainTxType.RECOVERY);
            po.setNerveTxHash(nerveTxHash);
            EthContext.UNCONFIRMED_TX_QUEUE.offer(po);
            ethCallBackManager.getTxConfirmedProcessor().txConfirmed(
                    HeterogeneousChainTxType.RECOVERY,
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
            HeterogeneousTransactionInfo txInfo = ethIIParseTxHelper.parseDepositTransaction(tx);
            if (txInfo == null) {
                return false;
            }
            Long blockHeight = tx.getBlockNumber().longValue();
            EthBlock.Block block = ethWalletApi.getBlockHeaderByHeight(blockHeight);
            if (block == null) {
                return false;
            }
            Long txTime = block.getTimestamp().longValue();
            ethIIAnalysisTxHelper.analysisTx(tx, txTime, blockHeight);
            ethCommonHelper.addHash(ethTxHash);
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
        EthAccount account = (EthAccount) this.getAccount(EthContext.ADMIN_ADDRESS);
        account.decrypt(EthContext.ADMIN_ADDRESS_PASSWORD);
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        boolean isContractAsset = assetId > 1;
        String contractAddressERC20;
        if (isContractAsset) {
            contractAddressERC20 = ethERC20Helper.getContractAddressByAssetId(assetId);
        } else {
            contractAddressERC20 = ZERO_ADDRESS;
        }
        // 把地址转换成小写
        toAddress = toAddress.toLowerCase();
        String vHash = EthIIUtil.encoderWithdraw(txHash, toAddress, value, isContractAsset, contractAddressERC20, VERSION);
        logger().debug("提现签名数据: {}, {}, {}, {}, {}, {}", txHash, toAddress, value, isContractAsset, contractAddressERC20, VERSION);
        logger().debug("提现签名vHash: {}, nerveTxHash: {}", vHash, txHash);
        return EthIIUtil.dataSign(vHash, priKey);
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
        EthAccount account = (EthAccount) this.getAccount(EthContext.ADMIN_ADDRESS);
        account.decrypt(EthContext.ADMIN_ADDRESS_PASSWORD);
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        String vHash = EthIIUtil.encoderChange(nerveTxHash, addAddresses, orginTxCount, removeAddresses, VERSION);
        logger().debug("变更交易的签名vHash: {}, nerveTxHash: {}", vHash, nerveTxHash);
        return EthIIUtil.dataSign(vHash, priKey);
    }

    @Override
    public String signUpgradeII(String nerveTxHash, String upgradeContract) throws NulsException {
        // 获取管理员账户
        EthAccount account = (EthAccount) this.getAccount(EthContext.ADMIN_ADDRESS);
        account.decrypt(EthContext.ADMIN_ADDRESS_PASSWORD);
        String priKey = Numeric.toHexStringNoPrefix(account.getPriKey());
        // 把地址转换成小写
        upgradeContract = upgradeContract.toLowerCase();
        String vHash = EthIIUtil.encoderUpgrade(nerveTxHash, upgradeContract, VERSION);
        logger().debug("升级交易的签名vHash: {}, nerveTxHash: {}", vHash, nerveTxHash);
        return EthIIUtil.dataSign(vHash, priKey);
    }

    @Override
    public Boolean verifySignWithdrawII(String signAddress, String txHash, String toAddress, BigInteger value, Integer assetId, String signed) throws NulsException {
        boolean isContractAsset = assetId > 1;
        String contractAddressERC20;
        if (isContractAsset) {
            contractAddressERC20 = ethERC20Helper.getContractAddressByAssetId(assetId);
        } else {
            contractAddressERC20 = ZERO_ADDRESS;
        }
        // 把地址转换成小写
        toAddress = toAddress.toLowerCase();
        String vHash = EthIIUtil.encoderWithdraw(txHash, toAddress, value, isContractAsset, contractAddressERC20, VERSION);
        logger().debug("[验证签名] 提现数据: {}, {}, {}, {}, {}, {}", txHash, toAddress, value, isContractAsset, contractAddressERC20, VERSION);
        logger().debug("[验证签名] 提现vHash: {}", vHash);
        return EthIIUtil.verifySign(signAddress, vHash, signed);
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
        String vHash = EthIIUtil.encoderChange(nerveTxHash, addAddresses, orginTxCount, removeAddresses, VERSION);
        return EthIIUtil.verifySign(signAddress, vHash, signed);
    }

    @Override
    public Boolean verifySignUpgradeII(String signAddress, String txHash, String upgradeContract, String signed) throws NulsException {
        // 把地址转换成小写
        upgradeContract = upgradeContract.toLowerCase();
        String vHash = EthIIUtil.encoderUpgrade(txHash, upgradeContract, VERSION);
        return EthIIUtil.verifySign(signAddress, vHash, signed);
    }

    @Override
    public boolean isEnoughFeeOfWithdraw(BigDecimal nvtAmount, int hAssetId) {
        IConverterCoreApi coreApi = EthContext.getConverterCoreApi();
        BigDecimal nvtUSD = coreApi.getUsdtPriceByAsset(AssetName.NVT);
        BigDecimal ethUSD = coreApi.getUsdtPriceByAsset(AssetName.ETH);
        if(null == nvtUSD || null == ethUSD){
            logger().error("[withdraw] 提现手续费计算,没有获取到完整的报价. nvtUSD:{}, ethUSD:{}", nvtUSD, ethUSD);
            throw new NulsRuntimeException(ConverterErrorCode.DATA_NOT_FOUND);
        }
        BigDecimal gasPrice = EthIIUtil.calGasPriceOfWithdraw(nvtUSD, nvtAmount, ethUSD, hAssetId);
        if (gasPrice.toBigInteger().compareTo(EthContext.getEthGasPrice()) >= 0) {
            logger().info("手续费足够，当前网络需要的GasPrice: {} Gwei, 实际计算出的GasPrice: {} Gwei",
                    new BigDecimal(EthContext.getEthGasPrice()).divide(BigDecimal.TEN.pow(9)).toPlainString(),
                    gasPrice.divide(BigDecimal.TEN.pow(9)).toPlainString());
            return true;
        }
        BigDecimal nvtAmountCalc = EthIIUtil.calNVTOfWithdraw(nvtUSD, new BigDecimal(EthContext.getEthGasPrice()), ethUSD, hAssetId);
        logger().warn("手续费不足，当前网络需要的GasPrice: {} Gwei, 实际计算出的GasPrice: {} Gwei, 总共需要的NVT: {}, 用户提供的NVT: {}, 需要追加的NVT: {}",
                new BigDecimal(EthContext.getEthGasPrice()).divide(BigDecimal.TEN.pow(9)).toPlainString(),
                gasPrice.divide(BigDecimal.TEN.pow(9)).toPlainString(),
                nvtAmountCalc.movePointLeft(8).toPlainString(),
                nvtAmount.movePointLeft(8).toPlainString(),
                nvtAmountCalc.subtract(nvtAmount).movePointLeft(8).toPlainString()
                );
        return false;
    }

    public String createOrSignWithdrawTxII(String nerveTxHash, String toAddress, BigInteger value, Integer assetId, String signatureData, boolean checkOrder) throws NulsException {
        try {
            logger().info("准备发送提现的ETH交易，nerveTxHash: {}, signatureData: {}", nerveTxHash, signatureData);
            // 交易准备
            EthWaitingTxPo waitingPo = new EthWaitingTxPo();
            EthAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.WITHDRAW, waitingPo);
            // 保存交易调用参数，设置等待结束时间
            ethIIInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, toAddress, value, assetId, signatureData, account.getOrder(), waitingPo);
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
            // 检查是否是NERVE资产绑定的ERC20，是则检查多签合约内是否已经注册此定制的ERC20，否则提现异常
            if (EthContext.getConverterCoreApi().isBoundHeterogeneousAsset(EthConstant.ETH_CHAIN_ID, po.getAssetId())
                    && !ethIIParseTxHelper.isMinterERC20(po.getContractAddress())) {
                logger().warn("[{}]不合法的Ethereum网络的提现交易, ERC20[{}]已绑定NERVE资产，但合约内未注册", nerveTxHash, po.getContractAddress());
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
            Function createOrSignWithdrawFunction = EthIIUtil.getCreateOrSignWithdrawFunction(nerveTxHash, toAddress, value, isContractAsset, contractAddressERC20, signatureData);
            // 计算GasPrice
            IConverterCoreApi coreApi = EthContext.getConverterCoreApi();
            BigDecimal gasPrice = new BigDecimal(EthContext.getEthGasPrice());
            // 达到指定高度才检查新机制的提现手续费
            if (coreApi.isSupportNewMechanismOfWithdrawalFee()) {
                BigDecimal nvtUSD = coreApi.getUsdtPriceByAsset(AssetName.NVT);
                BigDecimal nvtAmount = coreApi.getFeeOfWithdrawTransaction(nerveTxHash);
                BigDecimal ethUSD = coreApi.getUsdtPriceByAsset(AssetName.ETH);
                gasPrice = EthIIUtil.calGasPriceOfWithdraw(nvtUSD, nvtAmount, ethUSD, po.getAssetId());
                if (gasPrice == null || gasPrice.toBigInteger().compareTo(EthContext.getEthGasPrice()) < 0) {
                    BigDecimal nvtAmountCalc = EthIIUtil.calNVTOfWithdraw(nvtUSD, new BigDecimal(EthContext.getEthGasPrice()), ethUSD, po.getAssetId());
                    gasPrice = gasPrice == null ? BigDecimal.ZERO : gasPrice;
                    logger().error("[提现]交易[{}]手续费不足，当前Ethereum网络的GasPrice: {} Gwei, 实际提供的GasPrice: {} Gwei, 总共需要的NVT: {}, 用户提供的NVT: {}, 需要追加的NVT: {}",
                            nerveTxHash,
                            new BigDecimal(EthContext.getEthGasPrice()).divide(BigDecimal.TEN.pow(9)).toPlainString(),
                            gasPrice.divide(BigDecimal.TEN.pow(9)).toPlainString(),
                            nvtAmountCalc.movePointLeft(8).toPlainString(),
                            nvtAmount.movePointLeft(8).toPlainString(),
                            nvtAmountCalc.subtract(nvtAmount).movePointLeft(8).toPlainString()
                    );
                    throw new NulsException(ConverterErrorCode.INSUFFICIENT_FEE_OF_WITHDRAW);
                }
                gasPrice = EthIIUtil.calNiceGasPriceOfWithdraw(new BigDecimal(EthContext.getEthGasPrice()), gasPrice);
            }
            // 验证合约后发出交易
            String ethTxHash = this.createTxComplete(nerveTxHash, po, fromAddress, priKey, createOrSignWithdrawFunction, HeterogeneousChainTxType.WITHDRAW, gasPrice.toBigInteger());
            if (StringUtils.isNotBlank(ethTxHash)) {
                // 记录提现交易已向ETH网络发出
                ethIIPendingTxHelper.commitNervePendingWithdrawTx(nerveTxHash, ethTxHash);
            }
            return ethTxHash;
        } catch (Exception e) {
            if (e instanceof NulsException) {
                throw (NulsException) e;
            }
            logger().error(e);
            throw new NulsException(ConverterErrorCode.DATA_ERROR, e);
        }
    }

    public String createOrSignManagerChangesTxII(String nerveTxHash, String[] addAddresses, String[] removeAddresses, int orginTxCount, String signatureData, boolean checkOrder) throws NulsException {
        logger().info("准备发送虚拟银行变更ETH交易，nerveTxHash: {}, signatureData: {}", nerveTxHash, signatureData);
        try {
            // 业务验证
            if (addAddresses == null) {
                addAddresses = new String[0];
            }
            if (removeAddresses == null) {
                removeAddresses = new String[0];
            }
            // 准备数据
            EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);
            po.setAddAddresses(addAddresses);
            po.setRemoveAddresses(removeAddresses);
            po.setOrginTxCount(orginTxCount);
            // 交易准备
            EthWaitingTxPo waitingPo = new EthWaitingTxPo();
            EthAccount account = this.createTxStartForChange(nerveTxHash, addAddresses, removeAddresses, waitingPo);
            // 保存交易调用参数，设置等待结束时间
            ethIIInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, addAddresses, removeAddresses, orginTxCount, signatureData, account.getOrder(), waitingPo);
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
            Function createOrSignManagerChangeFunction = EthIIUtil.getCreateOrSignManagerChangeFunction(nerveTxHash, addList, removeList, orginTxCount, signatureData);
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
            EthWaitingTxPo waitingPo = new EthWaitingTxPo();
            EthAccount account = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.UPGRADE, waitingPo);
            // 保存交易调用参数，设置等待结束时间
            ethIIInvokeTxHelper.saveWaittingInvokeQueue(nerveTxHash, upgradeContract, signatureData, account.getOrder(), waitingPo);
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
            EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
            po.setNerveTxHash(nerveTxHash);

            Function function = EthIIUtil.getCreateOrSignUpgradeFunction(nerveTxHash, upgradeContract, signatureData);
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

    private EthAccount createTxStartForChange(String nerveTxHash, String[] addAddresses, String[] removeAddresses, EthWaitingTxPo po) throws Exception {
        EthAccount txStart = this.createTxStart(nerveTxHash, HeterogeneousChainTxType.CHANGE, po);
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
        list.sort(changeSort);
        int i = 1;
        for (Map.Entry<String, Integer> entry : list) {
            currentVirtualBanks.put(entry.getKey(), i++);
        }
        Integer order = currentVirtualBanks.get(EthContext.ADMIN_ADDRESS);
        if (order == null) {
            order = 0x0f;
        }
        txStart.setOrder(order);
        logger().info("变更交易的当前节点执行顺序: {}, addAddresses: {}, removeAddresses: {}, currentVirtualBanks: {}", order, Arrays.toString(addAddresses), Arrays.toString(removeAddresses), currentVirtualBanks);
        return txStart;
    }

    private EthAccount createTxStart(String nerveTxHash, HeterogeneousChainTxType txType, EthWaitingTxPo po) throws Exception {
        Map<String, Integer> currentVirtualBanks = EthContext.getConverterCoreApi().currentVirtualBanks(EthConstant.ETH_CHAIN_ID);
        po.setCurrentVirtualBanks(currentVirtualBanks);
        String realNerveTxHash = nerveTxHash;
        // 根据nerve交易hash前两位算出顺序种子
        int seed = new BigInteger(realNerveTxHash.substring(0, 1), 16).intValue() + 1;
        int bankSize = EthContext.getConverterCoreApi().getVirtualBankSize();
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
        // 按顺序等待固定时间后再发出ETH交易
        int bankOrder = currentVirtualBanks.get(EthContext.ADMIN_ADDRESS);
        if (logger().isDebugEnabled()) {
            logger().debug("顺序计算参数 bankSize: {}, seed: {}, mod: {}, orginBankOrder: {}, bankOrder: {}", bankSize, seed, mod, EthContext.getConverterCoreApi().getVirtualBankOrder(), bankOrder);
        }
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
        account.setMod(mod);
        account.setBankSize(bankSize);
        return account;
    }


    private String createTxComplete(String nerveTxHash, EthUnconfirmedTxPo po, String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType) throws Exception {
        return this.createTxComplete(nerveTxHash, po, fromAddress, priKey, txFunction, txType, null);
    }
    private String createTxComplete(String nerveTxHash, EthUnconfirmedTxPo po, String fromAddress, String priKey, Function txFunction, HeterogeneousChainTxType txType, BigInteger gasPrice) throws Exception {
        // 验证合约交易合法性
        EthCall ethCall = ethWalletApi.validateContractCall(fromAddress, EthContext.MULTY_SIGN_ADDRESS, txFunction);
        if (ethCall.isReverted()) {
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
        BigInteger gasLimit = estimateGas.add(EthConstant.BASE_GAS_LIMIT);
        EthSendTransactionPo ethSendTransactionPo = ethWalletApi.callContract(fromAddress, priKey, EthContext.MULTY_SIGN_ADDRESS, gasLimit, txFunction, BigInteger.ZERO, gasPrice);
        String ethTxHash = ethSendTransactionPo.getTxHash();
        // docking发起eth交易时，把交易关系记录到db中，并保存当前使用的nonce到关系表中，若有因为price过低不打包交易而重发的需要，则取出当前使用的nonce重发交易
        ethTxRelationStorageService.save(ethTxHash, nerveTxHash, ethSendTransactionPo);
        // 当前节点已发出eth交易
        ethIIInvokeTxHelper.saveSentEthTx(nerveTxHash);

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

    private Comparator changeSort = new Comparator<Map.Entry<String, Integer>>() {
        @Override
        public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
            if(o1.getValue() > o2.getValue()) {
                return 1;
            } else if(o1.getValue() < o2.getValue()) {
                return -1;
            }
            return 0;
        }
    };

    public void setEthIIInvokeTxHelper(EthIIInvokeTxHelper ethIIInvokeTxHelper) {
        this.ethIIInvokeTxHelper = ethIIInvokeTxHelper;
    }

    public void setEthIIParseTxHelper(EthIIParseTxHelper ethIIParseTxHelper) {
        this.ethIIParseTxHelper = ethIIParseTxHelper;
    }

    public void setEthIIAnalysisTxHelper(EthIIAnalysisTxHelper ethIIAnalysisTxHelper) {
        this.ethIIAnalysisTxHelper = ethIIAnalysisTxHelper;
    }

    public void setEthIIResendHelper(EthIIResendHelper ethIIResendHelper) {
        this.ethIIResendHelper = ethIIResendHelper;
    }

    public void setEthIIPendingTxHelper(EthIIPendingTxHelper ethIIPendingTxHelper) {
        this.ethIIPendingTxHelper = ethIIPendingTxHelper;
    }
}

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
package network.nerve.converter.heterogeneouschain.lib.helper;

import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgInput;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxStorageService;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.*;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Mimi
 * @date: 2020-03-20
 */
public class HtgParseTxHelper implements BeanInitial {

    private HtgERC20Helper htgERC20Helper;
    private HtgTxStorageService htgTxStorageService;
    private HtgWalletApi htgWalletApi;
    private HtgListener htgListener;
    private HtgContext htgContext;

    //public HtgParseTxHelper(BeanMap beanMap) {
    //    this.htgERC20Helper = (HtgERC20Helper) beanMap.get("htgERC20Helper");
    //    this.htgTxStorageService = (HtgTxStorageService) beanMap.get("htgTxStorageService");
    //    this.htgWalletApi = (HtgWalletApi) beanMap.get("htgWalletApi");
    //    this.htgListener = (HtgListener) beanMap.get("htgListener");
    //    this.htgContext = (HtgContext) beanMap.get("htgContext");
    //}

    private NulsLogger logger() {
        return htgContext.logger();
    }

    public boolean isCompletedTransaction(String nerveTxHash) throws Exception {
        return isCompletedTransactionByStatus(nerveTxHash, false);
    }

    public boolean isCompletedTransactionByLatest(String nerveTxHash) throws Exception {
        return isCompletedTransactionByStatus(nerveTxHash, true);
    }

    private boolean isCompletedTransactionByStatus(String nerveTxHash, boolean latest) throws Exception {
        Function isCompletedFunction = HtgUtil.getIsCompletedFunction(nerveTxHash);
        List<Type> valueTypes = htgWalletApi.callViewFunction(htgContext.MULTY_SIGN_ADDRESS(), isCompletedFunction, latest);
        if (valueTypes == null || valueTypes.size() == 0) {
            return false;
        }
        boolean isCompleted = Boolean.parseBoolean(valueTypes.get(0).getValue().toString());
        return isCompleted;
    }

    public boolean isMinterERC20(String erc20) throws Exception {
        return isMinterERC20ByStatus(erc20, false);
    }

    public boolean isMinterERC20ByLatest(String erc20) throws Exception {
        return isMinterERC20ByStatus(erc20, true);
    }

    private boolean isMinterERC20ByStatus(String erc20, boolean latest) throws Exception {
        if (StringUtils.isBlank(erc20)) {
            return true;
        }
        Function isMinterERC20Function = HtgUtil.getIsMinterERC20Function(erc20);
        List<Type> valueTypes = htgWalletApi.callViewFunction(htgContext.MULTY_SIGN_ADDRESS(), isMinterERC20Function, latest);
        boolean isMinterERC20 = Boolean.parseBoolean(valueTypes.get(0).getValue().toString());
        return isMinterERC20;
    }

    /**
     * 解析提现交易数据
     */
    public HeterogeneousTransactionInfo parseWithdrawTransaction(Transaction tx, TransactionReceipt txReceipt) throws Exception {
        if (tx == null) {
            logger().warn("解析交易的数据不存在或不完整");
            return null;
        }
        String txHash = tx.getHash();
        HeterogeneousTransactionInfo txInfo = HtgUtil.newTransactionInfo(tx, htgContext.NERVE_CHAINID(), this, logger());
        boolean isWithdraw;
        if (tx.getInput().length() < 10) {
            logger().warn("不是提现交易[0]");
            return null;
        }
        String methodNameHash = tx.getInput().substring(0, 10);
        // 提现交易的固定地址
        if (htgListener.isListeningAddress(tx.getTo()) &&
                HtgConstant.METHOD_HASH_CREATEORSIGNWITHDRAW.equals(methodNameHash)) {
            if (txReceipt == null) {
                txReceipt = htgWalletApi.getTxReceipt(txHash);
            }
            // 解析交易收据
            if (htgContext.getConverterCoreApi().isProtocol21()) {
                // 协议v1.21
                isWithdraw = this.newParseWithdrawTxReceiptSinceProtocol21(tx, txReceipt, txInfo);
            } else if (htgContext.getConverterCoreApi().isSupportProtocol13NewValidationOfERC20()) {
                // 协议v1.13
                isWithdraw = this.newParseWithdrawTxReceipt(tx, txReceipt, txInfo);
            } else {
                isWithdraw = this.parseWithdrawTxReceipt(txReceipt, txInfo);
            }
            if (!isWithdraw) {
                logger().warn("不是提现交易[1], hash: {}", txHash);
                return null;
            }
            if (txInfo.isIfContractAsset()) {
                htgERC20Helper.loadERC20(txInfo.getContractAddress(), txInfo);
            }
        } else {
            logger().warn("不是提现交易[2], hash: {}, txTo: {}, methodNameHash: {}", txHash, tx.getTo(), methodNameHash);
            return null;
        }
        txInfo.setTxType(HeterogeneousChainTxType.WITHDRAW);
        // 解析多签列表
        this.loadSigners(txReceipt, txInfo);
        return txInfo;
    }

    public HeterogeneousTransactionInfo parseWithdrawTransaction(Transaction tx) throws Exception {
        return this.parseWithdrawTransaction(tx, null);
    }

    public HeterogeneousTransactionInfo parseWithdrawTransaction(String txHash) throws Exception {
        Transaction tx = htgWalletApi.getTransactionByHash(txHash);
        if (tx == null) {
            logger().warn("交易不存在");
            return null;
        }
        if (tx.getTo() == null) {
            logger().warn("不是提现交易");
            return null;
        }
        tx.setFrom(tx.getFrom().toLowerCase());
        tx.setTo(tx.getTo().toLowerCase());
        return this.parseWithdrawTransaction(tx, null);
    }

    /**
     * 解析直接转账的方式的充值交易数据
     */
    private HeterogeneousTransactionInfo parseDepositTransactionByTransferDirectly(Transaction tx, TransactionReceipt txReceipt) throws Exception {
        if (tx == null) {
            logger().warn("交易不存在");
            return null;
        }
        String txHash = tx.getHash();
        HeterogeneousTransactionInfo txInfo = HtgUtil.newTransactionInfo(tx, htgContext.NERVE_CHAINID(), this, logger());
        boolean isDeposit = false;
        if (txReceipt == null) {
            txReceipt = htgWalletApi.getTxReceipt(txHash);
        }
        do {
            // HT充值交易的固定接收地址,金额大于0, 没有input
            if (htgListener.isListeningAddress(tx.getTo()) &&
                    tx.getValue().compareTo(BigInteger.ZERO) > 0 &&
                    tx.getInput().equals(HtgConstant.HEX_PREFIX)) {
                if(!this.validationEthDeposit(tx, txReceipt)) {
                    logger().error("[{}]不是充值交易[0]", txHash);
                    return null;
                }
                isDeposit = true;
                txInfo.setDecimals(htgContext.getConfig().getDecimals());
                txInfo.setAssetId(htgContext.HTG_ASSET_ID());
                txInfo.setValue(tx.getValue());
                txInfo.setIfContractAsset(false);
                break;
            }
            // ERC20充值交易
            if (htgERC20Helper.isERC20(tx.getTo(), txInfo)) {
                if (htgERC20Helper.hasERC20WithListeningAddress(txReceipt, txInfo, address -> htgListener.isListeningAddress(address))) {
                    // 检查是否是NERVE资产绑定的ERC20，是则检查多签合约内是否已经注册此定制的ERC20，否则充值异常
                    if (htgContext.getConverterCoreApi().isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), txInfo.getAssetId())
                            && !isMinterERC20(txInfo.getContractAddress())) {
                        logger().warn("[{}]不合法的{}网络的充值交易[6], ERC20[{}]已绑定NERVE资产，但合约内未注册", txHash, htgContext.getConfig().getSymbol(), txInfo.getContractAddress());
                        break;
                    }
                    isDeposit = true;
                    break;
                }
            }
        } while (false);
        if (!isDeposit) {
            logger().error("[{}]不是充值交易[1]", txHash);
            return null;
        }
        txInfo.setTxType(HeterogeneousChainTxType.DEPOSIT);
        return txInfo;
    }

    public BigInteger getTxHeight(NulsLogger logger, Transaction tx) throws Exception {
        try {
            if (tx == null) {
                return null;
            }
            return tx.getBlockNumber();
        } catch (Exception e) {
            String txHash = tx.getHash();
            logger.error("解析充值交易错误, 区块高度解析失败, 交易hash: {}, BlockNumberRaw: {}, BlockHash: {}", txHash, tx.getBlockNumberRaw(), tx.getBlockHash());
            TransactionReceipt txReceipt = null;
            try {
                txReceipt = htgWalletApi.getTxReceipt(txHash);
                return txReceipt.getBlockNumber();
            } catch (Exception ex) {
                if (txReceipt != null) {
                    logger.error("再次解析充值交易错误, 区块高度解析失败, 交易hash: {}, BlockNumberRaw: {}, BlockHash: {}", txHash, txReceipt.getBlockNumberRaw(), txReceipt.getBlockHash());
                } else {
                    logger.error("再次解析充值交易错误, 区块高度解析失败, 交易hash: {}, empty txReceipt.", txHash);
                }
                throw ex;
            }
        }
    }

    public HeterogeneousTransactionInfo parseDepositTransaction(Transaction tx) throws Exception {
        HtgInput htInput = this.parseInput(tx.getInput());
        // 新的充值交易方式，调用多签合约的crossOut函数
        if (htInput.isDepositTx()) {
            HeterogeneousTransactionInfo po = new HeterogeneousTransactionInfo();
            po.setTxHash(tx.getHash());
            po.setBlockHeight(getTxHeight(logger(), tx).longValue());
            boolean isDepositTx = this.validationEthDepositByCrossOut(tx, po);
            if (!isDepositTx) {
                return null;
            }
            po.setTxType(HeterogeneousChainTxType.DEPOSIT);
            return po;
        }
        // 新的充值交易方式II，调用多签合约的crossOutII函数
        if (htInput.isDepositIITx()) {
            HeterogeneousTransactionInfo po = new HeterogeneousTransactionInfo();
            po.setTxHash(tx.getHash());
            po.setBlockHeight(getTxHeight(logger(), tx).longValue());
            boolean isDepositTx = this.validationEthDepositByCrossOutII(tx, null, po);
            if (!isDepositTx) {
                return null;
            }
            po.setTxType(HeterogeneousChainTxType.DEPOSIT);
            return po;
        }
        return this.parseDepositTransactionByTransferDirectly(tx, null);
    }

    public HeterogeneousTransactionInfo parseDepositTransaction(String txHash) throws Exception {
        Transaction tx = htgWalletApi.getTransactionByHash(txHash);
        if (tx == null) {
            logger().warn("交易不存在");
            return null;
        }
        if (tx.getTo() == null) {
            logger().warn("不是充值交易");
            return null;
        }
        tx.setFrom(tx.getFrom().toLowerCase());
        tx.setTo(tx.getTo().toLowerCase());
        return this.parseDepositTransaction(tx);
    }

    public boolean validationEthDeposit(Transaction tx) throws Exception {
        return this.validationEthDeposit(tx, null);
    }

    private boolean validationEthDeposit(Transaction tx, TransactionReceipt txReceipt) throws Exception {
        if (tx == null) {
            logger().warn("交易不存在");
            return false;
        }
        String txHash = tx.getHash();
        if (txReceipt == null) {
            txReceipt = htgWalletApi.getTxReceipt(txHash);
        }
        List<Log> logs = txReceipt.getLogs();
        if (logs == null || logs.isEmpty()) {
            return false;
        }
        for (Log log : logs) {
            List<String> topics = log.getTopics();
            if (log.getTopics().size() == 0) {
                continue;
            }
            String eventHash = topics.get(0);
            if (!HtgConstant.EVENT_HASH_HT_DEPOSIT_FUNDS.equals(eventHash)) {
                continue;
            }
            List<Object> depositEvent = HtgUtil.parseEvent(log.getData(), HtgConstant.EVENT_DEPOSIT_FUNDS);
            if (depositEvent == null && depositEvent.size() != 2) {
                return false;
            }
            String from = depositEvent.get(0).toString();
            BigInteger amount = new BigInteger(depositEvent.get(1).toString());
            if (tx.getFrom().equals(from) && tx.getValue().compareTo(amount) == 0) {
                return true;
            }
        }

        return false;
    }

    public boolean validationEthDepositByCrossOut(Transaction tx, HeterogeneousTransactionInfo po) throws Exception {
        return this.validationEthDepositByCrossOut(tx, null, po);
    }

    private boolean validationEthDepositByCrossOut(Transaction tx, TransactionReceipt txReceipt, HeterogeneousTransactionInfo po) throws Exception {
        if (htgContext.getConverterCoreApi().isProtocol22()) {
            return newValidationEthDepositByCrossOutProtocol22(tx, txReceipt, po);
        } else if (htgContext.getConverterCoreApi().isSupportProtocol13NewValidationOfERC20()) {
            return newValidationEthDepositByCrossOut(tx, txReceipt, po);
        } else {
            return _validationEthDepositByCrossOut(tx, txReceipt, po);
        }
    }

    private boolean _validationEthDepositByCrossOut(Transaction tx, TransactionReceipt txReceipt, HeterogeneousTransactionInfo po) throws Exception {
        if (tx == null) {
            logger().warn("交易不存在");
            return false;
        }
        String txHash = tx.getHash();
        if (txReceipt == null) {
            txReceipt = htgWalletApi.getTxReceipt(txHash);
        }
        List<Log> logs = txReceipt.getLogs();
        if (logs == null || logs.isEmpty()) {
            logger().warn("交易[{}]事件为空", txHash);
            return false;
        }
        int logSize = logs.size();
        if (logSize == 1) {
            // HTG充值交易
            Log log = logs.get(0);
            List<String> topics = log.getTopics();
            if (log.getTopics().size() == 0) {
                logger().warn("交易[{}]log未知", txHash);
                return false;
            }
            String eventHash = topics.get(0);
            if (!HtgConstant.EVENT_HASH_CROSS_OUT_FUNDS.equals(eventHash)) {
                logger().warn("交易[{}]事件未知", txHash);
                return false;
            }
            List<Object> depositEvent = HtgUtil.parseEvent(log.getData(), HtgConstant.EVENT_CROSS_OUT_FUNDS);
            if (depositEvent == null && depositEvent.size() != 4) {
                logger().warn("交易[{}]CrossOut事件数据不合法[0]", txHash);
                return false;
            }
            String from = depositEvent.get(0).toString();
            String to = depositEvent.get(1).toString();
            BigInteger amount = new BigInteger(depositEvent.get(2).toString());
            String erc20 = depositEvent.get(3).toString();
            if (tx.getFrom().equals(from) && tx.getValue().compareTo(amount) == 0 && HtgConstant.ZERO_ADDRESS.equals(erc20)) {
                if (po != null) {
                    po.setIfContractAsset(false);
                    po.setFrom(from);
                    po.setTo(tx.getTo());
                    po.setValue(amount);
                    po.setDecimals(htgContext.getConfig().getDecimals());
                    po.setAssetId(htgContext.HTG_ASSET_ID());
                    po.setNerveAddress(to);
                }
                return true;
            }
        } else {
            // ERC20充值交易
            List<Object> crossOutInput = HtgUtil.parseInput(tx.getInput(), HtgConstant.INPUT_CROSS_OUT);
            String _to = crossOutInput.get(0).toString();
            BigInteger _amount = new BigInteger(crossOutInput.get(1).toString());
            String _erc20 = crossOutInput.get(2).toString().toLowerCase();
            if (!htgERC20Helper.isERC20(_erc20, po)) {
                logger().warn("erc20[{}]未注册", _erc20);
                return false;
            }
            boolean transferEvent = false;
            boolean burnEvent = true;
            boolean crossOutEvent = false;
            BigInteger actualAmount = _amount;
            for (Log log : logs) {
                List<String> topics = log.getTopics();
                String eventHash = topics.get(0);
                String eventContract = log.getAddress().toLowerCase();
                if (HtgConstant.EVENT_HASH_ERC20_TRANSFER.equals(eventHash)) {
                    if (!eventContract.equals(_erc20)) {
                        logger().warn("交易[{}]的ERC20地址不匹配", txHash);
                        return false;
                    }
                    int length = topics.get(1).length();
                    String fromAddress = HtgConstant.HEX_PREFIX + topics.get(1).substring(26, length).toString();
                    String toAddress = HtgConstant.HEX_PREFIX + topics.get(2).substring(26, length).toString();
                    String data;
                    if (topics.size() == 3) {
                        data = log.getData();
                    } else {
                        data = topics.get(3);
                    }
                    String[] v = data.split("x");
                    // 转账金额
                    BigInteger amount = new BigInteger(v[1], 16);
                    // 当toAddress是0x0时，则说明这是一个从当前多签合约销毁erc20的事件
                    if (HtgConstant.ZERO_ADDRESS.equals(toAddress)) {
                        if (!fromAddress.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                            logger().warn("交易[{}]的销毁地址不匹配", txHash);
                            burnEvent = false;
                            break;
                        }
                        if (amount.compareTo(_amount) != 0) {
                            logger().warn("交易[{}]的ERC20销毁金额不匹配", txHash);
                            burnEvent = false;
                            break;
                        }
                    } else {
                        // 用户转移token到多签合约的事件
                        // 必须是用户地址
                        if (!fromAddress.equals(tx.getFrom())) {
                            logger().warn("交易[{}]的ERC20用户地址不匹配", txHash);
                            return false;
                        }
                        // 必须是多签合约地址
                        if (!toAddress.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                            logger().warn("交易[{}]的ERC20充值地址不匹配", txHash);
                            return false;
                        }
                        // 是否支持转账即销毁部分的ERC20
                        if (htgContext.getConverterCoreApi().isSupportProtocol12ERC20OfTransferBurn()) {
                            if (amount.compareTo(_amount) > 0) {
                                logger().warn("交易[{}]的ERC20充值金额不匹配", txHash);
                                return false;
                            }
                            actualAmount = amount;
                        } else {
                            if (amount.compareTo(_amount) != 0) {
                                logger().warn("交易[{}]的ERC20充值金额不匹配", txHash);
                                return false;
                            }
                        }

                        transferEvent = true;
                    }
                }
                if (HtgConstant.EVENT_HASH_CROSS_OUT_FUNDS.equals(eventHash)) {
                    List<Object> depositEvent = HtgUtil.parseEvent(log.getData(), HtgConstant.EVENT_CROSS_OUT_FUNDS);
                    if (depositEvent == null && depositEvent.size() != 4) {
                        logger().warn("交易[{}]CrossOut事件数据不合法[1]", txHash);
                        return false;
                    }
                    String from = depositEvent.get(0).toString();
                    String to = depositEvent.get(1).toString();
                    BigInteger amount = new BigInteger(depositEvent.get(2).toString());
                    String erc20 = depositEvent.get(3).toString();
                    if (!tx.getFrom().equals(from)) {
                        logger().warn("交易[{}]CrossOut事件数据不合法[2]", txHash);
                        return false;
                    }
                    if (!_to.equals(to)) {
                        logger().warn("交易[{}]CrossOut事件数据不合法[3]", txHash);
                        return false;
                    }
                    if (amount.compareTo(_amount) != 0) {
                        logger().warn("交易[{}]CrossOut事件数据不合法[4]", txHash);
                        return false;
                    }
                    if (!_erc20.equals(erc20)) {
                        logger().warn("交易[{}]CrossOut事件数据不合法[5]", txHash);
                        return false;
                    }
                    crossOutEvent = true;
                }
            }
            if (transferEvent && burnEvent && crossOutEvent) {
                if (po != null && actualAmount.compareTo(BigInteger.ZERO) > 0) {
                    po.setIfContractAsset(true);
                    po.setContractAddress(_erc20);
                    po.setFrom(tx.getFrom());
                    po.setTo(tx.getTo());
                    po.setValue(actualAmount);
                    po.setNerveAddress(_to);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 解析管理员变更交易数据
     */
    public HeterogeneousTransactionInfo parseManagerChangeTransaction(Transaction tx, TransactionReceipt txReceipt) throws Exception {
        if (tx == null) {
            logger().warn("交易不存在");
            return null;
        }
        HeterogeneousTransactionInfo txInfo = HtgUtil.newTransactionInfo(tx, htgContext.NERVE_CHAINID(), this, logger());
        boolean isChange = false;
        String input, methodHash;
        if (htgListener.isListeningAddress(tx.getTo()) && (input = tx.getInput()).length() >= 10) {
            methodHash = input.substring(0, 10);
            if (methodHash.equals(HtgConstant.METHOD_HASH_CREATEORSIGNMANAGERCHANGE)) {
                isChange = true;
                List<Object> inputData = HtgUtil.parseInput(input, HtgConstant.INPUT_CHANGE);
                List<Address> adds = (List<Address>) inputData.get(1);
                List<Address> quits = (List<Address>) inputData.get(2);
                if (!adds.isEmpty()) {
                    txInfo.setAddAddresses(HtgUtil.list2array(adds.stream().map(a -> a.getValue()).collect(Collectors.toList())));
                }
                if (!quits.isEmpty()) {
                    txInfo.setRemoveAddresses(HtgUtil.list2array(quits.stream().map(q -> q.getValue()).collect(Collectors.toList())));
                }
            }
        }
        if (!isChange) {
            logger().warn("不是变更交易");
            return null;
        }
        txInfo.setTxType(HeterogeneousChainTxType.CHANGE);
        // 解析多签列表
        this.loadSigners(txReceipt, txInfo);
        return txInfo;
    }

    /**
     * 解析合约升级授权交易数据
     */
    public HeterogeneousTransactionInfo parseUpgradeTransaction(Transaction tx, TransactionReceipt txReceipt) throws Exception {
        if (tx == null) {
            logger().warn("交易不存在");
            return null;
        }
        HeterogeneousTransactionInfo txInfo = HtgUtil.newTransactionInfo(tx, htgContext.NERVE_CHAINID(), this, logger());
        boolean isUpgrade = false;
        String input, methodHash;
        if (htgListener.isListeningAddress(tx.getTo()) && (input = tx.getInput()).length() >= 10) {
            methodHash = input.substring(0, 10);
            if (methodHash.equals(HtgConstant.METHOD_HASH_CREATEORSIGNUPGRADE)) {
                isUpgrade = true;
            }
        }
        if (!isUpgrade) {
            logger().warn("不是合约升级授权交易");
            return null;
        }
        txInfo.setTxType(HeterogeneousChainTxType.UPGRADE);
        // 解析多签列表
        this.loadSigners(txReceipt, txInfo);
        return txInfo;
    }

    public List<HeterogeneousAddress> parseSigners(TransactionReceipt txReceipt, String txFrom) {
        List<Object> eventResult = this.loadDataFromEvent(txReceipt);
        if (eventResult == null || eventResult.isEmpty()) {
            return null;
        }
        List<HeterogeneousAddress> signers = new ArrayList<>();
        signers.add(new HeterogeneousAddress(htgContext.getConfig().getChainId(), txFrom));
        return signers;
    }

    private void loadSigners(TransactionReceipt txReceipt, HeterogeneousTransactionInfo txInfo) {
        List<Object> eventResult = this.loadDataFromEvent(txReceipt);
        if (eventResult != null && !eventResult.isEmpty()) {
            txInfo.setNerveTxHash(eventResult.get(eventResult.size() - 1).toString());
            List<HeterogeneousAddress> signers = new ArrayList<>();
            signers.add(new HeterogeneousAddress(htgContext.getConfig().getChainId(), txInfo.getFrom()));
            txInfo.setSigners(signers);
        }
    }

    private List<Object> loadDataFromEvent(TransactionReceipt txReceipt) {
        List<Log> logs = txReceipt.getLogs();
        if (logs == null || logs.isEmpty()) {
            return null;
        }
        Log log = logs.get(logs.size() - 1);
        List<String> topics = log.getTopics();
        if (log.getTopics().size() == 0) {
            return null;
        }
        String eventHash = topics.get(0);
        // Polygon链，存在erc20转账的合约交易中，会在末尾多出一个未知事件
        if (htgContext.getConfig().getChainId() == 106) {
            if (HtgConstant.EVENT_HASH_UNKNOWN_ON_POLYGON.equals(eventHash)) {
                log = logs.get(logs.size() - 2);
                topics = log.getTopics();
                eventHash = topics.get(0);
            }
        }

        // topics 解析事件名, 签名完成会触发的事件
        // 解析事件数据，获得交易的成功事件数据列表
        List<Object> eventResult = null;
        switch (eventHash) {
            case HtgConstant.EVENT_HASH_TRANSACTION_WITHDRAW_COMPLETED:
                eventResult = HtgUtil.parseEvent(log.getData(), HtgConstant.EVENT_TRANSACTION_WITHDRAW_COMPLETED);
                break;
            case HtgConstant.EVENT_HASH_TRANSACTION_MANAGER_CHANGE_COMPLETED:
                eventResult = HtgUtil.parseEvent(log.getData(), HtgConstant.EVENT_TRANSACTION_MANAGER_CHANGE_COMPLETED);
                break;
            case HtgConstant.EVENT_HASH_TRANSACTION_UPGRADE_COMPLETED:
                eventResult = HtgUtil.parseEvent(log.getData(), HtgConstant.EVENT_TRANSACTION_UPGRADE_COMPLETED);
                break;
        }
        return eventResult;
    }

    private boolean parseWithdrawTxReceipt(TransactionReceipt txReceipt, HeterogeneousTransactionBaseInfo po) {
        if (txReceipt == null || !txReceipt.isStatusOK()) {
            return false;
        }
        List<Log> logs = txReceipt.getLogs();
        if (logs != null && logs.size() > 0) {
            for (Log log : logs) {
                List<String> topics = log.getTopics();
                if (topics.size() == 0) {
                    continue;
                }
                // 为ERC20提现
                if (topics.get(0).equals(HtgConstant.EVENT_HASH_ERC20_TRANSFER)) {
                    String toAddress = HtgConstant.HEX_PREFIX + topics.get(2).substring(26, topics.get(1).length()).toString();
                    String data;
                    if (topics.size() == 3) {
                        data = log.getData();
                    } else {
                        data = topics.get(3);
                    }
                    String[] v = data.split("x");
                    // 转账金额
                    BigInteger amount = new BigInteger(v[1], 16);
                    if (amount.compareTo(BigInteger.ZERO) > 0) {
                        po.setIfContractAsset(true);
                        po.setContractAddress(log.getAddress().toLowerCase());
                        po.setTo(toAddress.toLowerCase());
                        po.setValue(amount);
                        return true;
                    }
                    return false;
                }
                // 为HT提现
                if (topics.get(0).equals(HtgConstant.EVENT_HASH_TRANSFERFUNDS)) {
                    String data = log.getData();
                    String to = HtgConstant.HEX_PREFIX + data.substring(26, 66);
                    String amountStr = data.substring(66, 130);
                    // 转账金额
                    BigInteger amount = new BigInteger(amountStr, 16);
                    if (amount.compareTo(BigInteger.ZERO) > 0) {
                        po.setTo(to.toLowerCase());
                        po.setValue(amount);
                        po.setDecimals(htgContext.getConfig().getDecimals());
                        po.setAssetId(htgContext.HTG_ASSET_ID());
                        return true;
                    }
                    return false;
                }
            }
        }
        return false;
    }

    public HtgInput parseInput(String input) {
        if (input.length() < 10) {
            return HtgInput.empty();
        }
        String methodHash;
        if ((methodHash = input.substring(0, 10)).equals(HtgConstant.METHOD_HASH_CREATEORSIGNWITHDRAW)) {
            return new HtgInput(true, HeterogeneousChainTxType.WITHDRAW, HtgUtil.parseInput(input, HtgConstant.INPUT_WITHDRAW).get(0).toString());
        }
        if (methodHash.equals(HtgConstant.METHOD_HASH_CREATEORSIGNMANAGERCHANGE)) {
            return new HtgInput(true, HeterogeneousChainTxType.CHANGE, HtgUtil.parseInput(input, HtgConstant.INPUT_CHANGE).get(0).toString());
        }
        if (methodHash.equals(HtgConstant.METHOD_HASH_CREATEORSIGNUPGRADE)) {
            return new HtgInput(true, HeterogeneousChainTxType.UPGRADE, HtgUtil.parseInput(input, HtgConstant.INPUT_UPGRADE).get(0).toString());
        }
        if (methodHash.equals(HtgConstant.METHOD_HASH_CROSS_OUT)) {
            return new HtgInput(true, HeterogeneousChainTxType.DEPOSIT);
        }
        if (methodHash.equals(HtgConstant.METHOD_HASH_CROSS_OUT_II)) {
            return new HtgInput(true);
        }
        return HtgInput.empty();
    }

    public HeterogeneousOneClickCrossChainData parseOneClickCrossChainData(String extend, NulsLogger logger) {
        if(StringUtils.isBlank(extend)) {
            return null;
        }
        extend = Numeric.prependHexPrefix(extend);
        if(extend.length() < 10) {
            return null;
        }
        String methodHash = extend.substring(0, 10);
        if (!HtgConstant.METHOD_HASH_ONE_CLICK_CROSS_CHAIN.equals(methodHash)) {
            return null;
        }
        extend = HtgConstant.HEX_PREFIX + extend.substring(10);
        try {
            List<Type> typeList = FunctionReturnDecoder.decode(extend, HtgConstant.INPUT_ONE_CLICK_CROSS_CHAIN);
            if (typeList == null || typeList.isEmpty()) {
                return null;
            }
            if (typeList.size() < 6) {
                return null;
            }
            int index = 0;
            List<Object> list = typeList.stream().map(type -> type.getValue()).collect(Collectors.toList());
            BigInteger feeAmount = (BigInteger) list.get(index++);
            int desChainId = ((BigInteger) list.get(index++)).intValue();
            String desToAddress = (String) list.get(index++);
            BigInteger tipping = (BigInteger) list.get(index++);
            String tippingAddress = (String) list.get(index++);
            String desExtend = Numeric.toHexString((byte[]) list.get(index++));
            return new HeterogeneousOneClickCrossChainData(feeAmount, desChainId, desToAddress, tipping == null ? BigInteger.ZERO : tipping, tippingAddress, desExtend);
        } catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    public HeterogeneousAddFeeCrossChainData parseAddFeeCrossChainData(String extend, NulsLogger logger) {
        if(StringUtils.isBlank(extend)) {
            return null;
        }
        extend = Numeric.prependHexPrefix(extend);
        if(extend.length() < 10) {
            return null;
        }
        String methodHash = extend.substring(0, 10);
        if (!HtgConstant.METHOD_HASH_ADD_FEE_CROSS_CHAIN.equals(methodHash)) {
            return null;
        }
        extend = HtgConstant.HEX_PREFIX + extend.substring(10);
        try {
            List<Type> typeList = FunctionReturnDecoder.decode(extend, HtgConstant.INPUT_ADD_FEE_CROSS_CHAIN);
            if (typeList == null || typeList.isEmpty()) {
                return null;
            }
            if (typeList.size() < 2) {
                return null;
            }
            int index = 0;
            List<Object> list = typeList.stream().map(type -> type.getValue()).collect(Collectors.toList());
            String nerveTxHash = (String) list.get(index++);
            String subExtend = Numeric.toHexString((byte[]) list.get(index++));
            return new HeterogeneousAddFeeCrossChainData(nerveTxHash, subExtend);
        } catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    private boolean newValidationEthDepositByCrossOutProtocol22(Transaction tx, TransactionReceipt txReceipt, HeterogeneousTransactionInfo po) throws Exception {
        if (tx == null) {
            logger().warn("交易不存在");
            return false;
        }
        String txHash = tx.getHash();
        if (txReceipt == null) {
            txReceipt = htgWalletApi.getTxReceipt(txHash);
        }
        List<Log> logs = txReceipt.getLogs();
        if (logs == null || logs.isEmpty()) {
            logger().warn("交易[{}]事件为空", txHash);
            return false;
        }
        List<Object> crossOutInput = HtgUtil.parseInput(tx.getInput(), HtgConstant.INPUT_CROSS_OUT);
        String _to = crossOutInput.get(0).toString();
        BigInteger _amount = new BigInteger(crossOutInput.get(1).toString());
        String _erc20 = crossOutInput.get(2).toString().toLowerCase();
        if (HtgConstant.ZERO_ADDRESS.equals(_erc20)) {
            // 主资产充值交易
            for (Log log : logs) {
                List<String> topics = log.getTopics();
                if (log.getTopics().size() == 0) {
                    continue;
                }
                String eventHash = topics.get(0);
                String eventContract = log.getAddress().toLowerCase();
                if (!HtgConstant.EVENT_HASH_CROSS_OUT_FUNDS.equals(eventHash)) {
                    continue;
                }
                if (!eventContract.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                    continue;
                }
                List<Object> depositEvent = HtgUtil.parseEvent(log.getData(), HtgConstant.EVENT_CROSS_OUT_FUNDS);
                if (depositEvent == null && depositEvent.size() != 4) {
                    logger().warn("交易[{}]CrossOut事件数据不合法[0]", txHash);
                    return false;
                }
                String from = depositEvent.get(0).toString();
                String to = depositEvent.get(1).toString();
                BigInteger amount = new BigInteger(depositEvent.get(2).toString());
                String erc20 = depositEvent.get(3).toString();
                if (tx.getFrom().equals(from) && tx.getValue().compareTo(amount) == 0 && HtgConstant.ZERO_ADDRESS.equals(erc20)) {
                    if (po != null) {
                        po.setIfContractAsset(false);
                        po.setFrom(from);
                        po.setTo(tx.getTo());
                        po.setValue(amount);
                        po.setDecimals(htgContext.getConfig().getDecimals());
                        po.setAssetId(htgContext.HTG_ASSET_ID());
                        po.setNerveAddress(to);
                    }
                    return true;
                }
            }
            logger().warn("交易[{}]的主资产[{}]充值事件不匹配", txHash, htgContext.getConfig().getSymbol());
            return false;
        } else {
            // ERC20充值交易
            if (!htgERC20Helper.isERC20(_erc20, po)) {
                logger().warn("erc20[{}]未注册", _erc20);
                return false;
            }
            boolean transferEvent = false;
            boolean burnEvent = true;
            boolean crossOutEvent = false;
            BigInteger actualAmount;
            BigInteger calcAmount = BigInteger.ZERO;
            for (Log log : logs) {
                List<String> topics = log.getTopics();
                if (topics.size() == 0) {
                    continue;
                }
                String eventHash = topics.get(0);
                String eventContract = log.getAddress().toLowerCase();
                if (HtgConstant.EVENT_HASH_ERC20_TRANSFER.equals(eventHash)) {
                    if (!eventContract.equals(_erc20)) {
                        continue;
                    }
                    int length = topics.get(1).length();
                    String fromAddress = HtgConstant.HEX_PREFIX + topics.get(1).substring(26, length).toString();
                    String toAddress = HtgConstant.HEX_PREFIX + topics.get(2).substring(26, length).toString();
                    String data;
                    if (topics.size() == 3) {
                        data = log.getData();
                    } else {
                        data = topics.get(3);
                    }
                    String[] v = data.split("x");
                    // 转账金额
                    BigInteger amount = new BigInteger(v[1], 16);
                    // 当toAddress是0x0时，则说明这是一个从当前多签合约销毁erc20的transfer事件
                    if (HtgConstant.ZERO_ADDRESS.equals(toAddress)) {
                        if (!fromAddress.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                            continue;
                        }
                        if (amount.compareTo(_amount) != 0) {
                            logger().warn("交易[{}]的ERC20销毁金额不匹配", txHash);
                            burnEvent = false;
                            break;
                        }
                    } else {
                        // 用户转移token到多签合约的事件
                        // 必须是多签合约地址
                        if (!toAddress.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                            continue;
                        }
                        calcAmount = calcAmount.add(amount);
                        transferEvent = true;
                    }
                }
                if (HtgConstant.EVENT_HASH_CROSS_OUT_FUNDS.equals(eventHash)) {
                    if (!eventContract.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                        continue;
                    }
                    List<Object> depositEvent = HtgUtil.parseEvent(log.getData(), HtgConstant.EVENT_CROSS_OUT_FUNDS);
                    if (depositEvent == null && depositEvent.size() != 4) {
                        logger().warn("交易[{}]CrossOut事件数据不合法[1]", txHash);
                        return false;
                    }
                    String from = depositEvent.get(0).toString();
                    String to = depositEvent.get(1).toString();
                    BigInteger amount = new BigInteger(depositEvent.get(2).toString());
                    String erc20 = depositEvent.get(3).toString();
                    if (!tx.getFrom().equals(from)) {
                        logger().warn("交易[{}]CrossOut事件数据不合法[2]", txHash);
                        return false;
                    }
                    if (!_to.equals(to)) {
                        logger().warn("交易[{}]CrossOut事件数据不合法[3]", txHash);
                        return false;
                    }
                    if (amount.compareTo(_amount) != 0) {
                        logger().warn("交易[{}]CrossOut事件数据不合法[4]", txHash);
                        return false;
                    }
                    if (!_erc20.equals(erc20)) {
                        logger().warn("交易[{}]CrossOut事件数据不合法[5]", txHash);
                        return false;
                    }
                    crossOutEvent = true;
                }
            }
            if (transferEvent && burnEvent && crossOutEvent) {
                if (calcAmount.compareTo(_amount) > 0) {
                    logger().warn("交易[{}]的ERC20充值金额不匹配", txHash);
                    return false;
                }
                actualAmount = calcAmount;
                if (actualAmount.equals(BigInteger.ZERO)) {
                    logger().warn("交易[{}]的ERC20充值金额为0", txHash);
                    return false;
                }

                if (po != null && actualAmount.compareTo(BigInteger.ZERO) > 0) {
                    po.setIfContractAsset(true);
                    po.setContractAddress(_erc20);
                    po.setFrom(tx.getFrom());
                    po.setTo(tx.getTo());
                    po.setValue(actualAmount);
                    po.setNerveAddress(_to);
                }
                return true;
            } else {
                logger().warn("交易[{}]的ERC20充值事件不匹配, transferEvent: {}, burnEvent: {}, crossOutEvent: {}",
                        txHash, transferEvent, burnEvent, crossOutEvent);
                return false;
            }
        }
    }

    private boolean newValidationEthDepositByCrossOut(Transaction tx, TransactionReceipt txReceipt, HeterogeneousTransactionInfo po) throws Exception {
        if (tx == null) {
            logger().warn("交易不存在");
            return false;
        }
        String txHash = tx.getHash();
        if (txReceipt == null) {
            txReceipt = htgWalletApi.getTxReceipt(txHash);
        }
        List<Log> logs = txReceipt.getLogs();
        if (logs == null || logs.isEmpty()) {
            logger().warn("交易[{}]事件为空", txHash);
            return false;
        }
        List<Object> crossOutInput = HtgUtil.parseInput(tx.getInput(), HtgConstant.INPUT_CROSS_OUT);
        String _to = crossOutInput.get(0).toString();
        BigInteger _amount = new BigInteger(crossOutInput.get(1).toString());
        String _erc20 = crossOutInput.get(2).toString().toLowerCase();
        if (HtgConstant.ZERO_ADDRESS.equals(_erc20)) {
            // 主资产充值交易
            for (Log log : logs) {
                List<String> topics = log.getTopics();
                if (log.getTopics().size() == 0) {
                    continue;
                }
                String eventHash = topics.get(0);
                String eventContract = log.getAddress().toLowerCase();
                if (!HtgConstant.EVENT_HASH_CROSS_OUT_FUNDS.equals(eventHash)) {
                    continue;
                }
                if (!eventContract.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                    continue;
                }
                List<Object> depositEvent = HtgUtil.parseEvent(log.getData(), HtgConstant.EVENT_CROSS_OUT_FUNDS);
                if (depositEvent == null && depositEvent.size() != 4) {
                    logger().warn("交易[{}]CrossOut事件数据不合法[0]", txHash);
                    return false;
                }
                String from = depositEvent.get(0).toString();
                String to = depositEvent.get(1).toString();
                BigInteger amount = new BigInteger(depositEvent.get(2).toString());
                String erc20 = depositEvent.get(3).toString();
                if (tx.getFrom().equals(from) && tx.getValue().compareTo(amount) == 0 && HtgConstant.ZERO_ADDRESS.equals(erc20)) {
                    if (po != null) {
                        po.setIfContractAsset(false);
                        po.setFrom(from);
                        po.setTo(tx.getTo());
                        po.setValue(amount);
                        po.setDecimals(htgContext.getConfig().getDecimals());
                        po.setAssetId(htgContext.HTG_ASSET_ID());
                        po.setNerveAddress(to);
                    }
                    return true;
                }
            }
            logger().warn("交易[{}]的主资产[{}]充值事件不匹配", txHash, htgContext.getConfig().getSymbol());
            return false;
        } else {
            // ERC20充值交易
            if (!htgERC20Helper.isERC20(_erc20, po)) {
                logger().warn("erc20[{}]未注册", _erc20);
                return false;
            }
            boolean transferEvent = false;
            boolean burnEvent = true;
            boolean crossOutEvent = false;
            BigInteger actualAmount;
            BigInteger calcAmount = BigInteger.ZERO;
            for (Log log : logs) {
                List<String> topics = log.getTopics();
                if (topics.size() == 0) {
                    continue;
                }
                String eventHash = topics.get(0);
                String eventContract = log.getAddress().toLowerCase();
                if (HtgConstant.EVENT_HASH_ERC20_TRANSFER.equals(eventHash)) {
                    if (!eventContract.equals(_erc20)) {
                        continue;
                    }
                    int length = topics.get(1).length();
                    String fromAddress = HtgConstant.HEX_PREFIX + topics.get(1).substring(26, length).toString();
                    String toAddress = HtgConstant.HEX_PREFIX + topics.get(2).substring(26, length).toString();
                    String data;
                    if (topics.size() == 3) {
                        data = log.getData();
                    } else {
                        data = topics.get(3);
                    }
                    String[] v = data.split("x");
                    // 转账金额
                    BigInteger amount = new BigInteger(v[1], 16);
                    // 当toAddress是0x0时，则说明这是一个从当前多签合约销毁erc20的transfer事件
                    if (HtgConstant.ZERO_ADDRESS.equals(toAddress)) {
                        if (!fromAddress.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                            logger().warn("交易[{}]的销毁地址不匹配", txHash);
                            burnEvent = false;
                            break;
                        }
                        if (amount.compareTo(_amount) != 0) {
                            logger().warn("交易[{}]的ERC20销毁金额不匹配", txHash);
                            burnEvent = false;
                            break;
                        }
                    } else {
                        // 用户转移token到多签合约的事件
                        // 必须是多签合约地址
                        if (!toAddress.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                            continue;
                        }
                        calcAmount = calcAmount.add(amount);
                        transferEvent = true;
                    }
                }
                if (HtgConstant.EVENT_HASH_CROSS_OUT_FUNDS.equals(eventHash)) {
                    if (!eventContract.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                        continue;
                    }
                    List<Object> depositEvent = HtgUtil.parseEvent(log.getData(), HtgConstant.EVENT_CROSS_OUT_FUNDS);
                    if (depositEvent == null && depositEvent.size() != 4) {
                        logger().warn("交易[{}]CrossOut事件数据不合法[1]", txHash);
                        return false;
                    }
                    String from = depositEvent.get(0).toString();
                    String to = depositEvent.get(1).toString();
                    BigInteger amount = new BigInteger(depositEvent.get(2).toString());
                    String erc20 = depositEvent.get(3).toString();
                    if (!tx.getFrom().equals(from)) {
                        logger().warn("交易[{}]CrossOut事件数据不合法[2]", txHash);
                        return false;
                    }
                    if (!_to.equals(to)) {
                        logger().warn("交易[{}]CrossOut事件数据不合法[3]", txHash);
                        return false;
                    }
                    if (amount.compareTo(_amount) != 0) {
                        logger().warn("交易[{}]CrossOut事件数据不合法[4]", txHash);
                        return false;
                    }
                    if (!_erc20.equals(erc20)) {
                        logger().warn("交易[{}]CrossOut事件数据不合法[5]", txHash);
                        return false;
                    }
                    crossOutEvent = true;
                }
            }
            if (transferEvent && burnEvent && crossOutEvent) {
                if (calcAmount.compareTo(_amount) > 0) {
                    logger().warn("交易[{}]的ERC20充值金额不匹配", txHash);
                    return false;
                }
                actualAmount = calcAmount;
                if (actualAmount.equals(BigInteger.ZERO)) {
                    logger().warn("交易[{}]的ERC20充值金额为0", txHash);
                    return false;
                }

                if (po != null && actualAmount.compareTo(BigInteger.ZERO) > 0) {
                    po.setIfContractAsset(true);
                    po.setContractAddress(_erc20);
                    po.setFrom(tx.getFrom());
                    po.setTo(tx.getTo());
                    po.setValue(actualAmount);
                    po.setNerveAddress(_to);
                }
                return true;
            } else {
                logger().warn("交易[{}]的ERC20充值事件不匹配, transferEvent: {}, burnEvent: {}, crossOutEvent: {}",
                        txHash, transferEvent, burnEvent, crossOutEvent);
                return false;
            }
        }
    }

    private boolean newParseWithdrawTxReceipt(Transaction tx, TransactionReceipt txReceipt, HeterogeneousTransactionBaseInfo po) {
        if (txReceipt == null || !txReceipt.isStatusOK()) {
            return false;
        }
        String txHash = tx.getHash();
        List<Object> withdrawInput = HtgUtil.parseInput(tx.getInput(), HtgConstant.INPUT_WITHDRAW);
        String receive = withdrawInput.get(1).toString();
        Boolean isERC20 = Boolean.parseBoolean(withdrawInput.get(3).toString());
        String erc20 = withdrawInput.get(4).toString().toLowerCase();
        boolean correctErc20 = false;
        boolean correctMainAsset = false;
        List<Log> logs = txReceipt.getLogs();
        if (logs != null && logs.size() > 0) {
            List<String> topics;
            String eventHash;
            String contract;
            // 转账金额
            BigInteger amount = BigInteger.ZERO;
            for (Log log : logs) {
                topics = log.getTopics();
                if (topics.size() == 0) {
                    continue;
                }
                eventHash = topics.get(0);
                contract = log.getAddress().toLowerCase();
                // 为ERC20提现
                if (eventHash.equals(HtgConstant.EVENT_HASH_ERC20_TRANSFER)) {
                    int length = topics.get(1).length();
                    String fromAddress = HtgConstant.HEX_PREFIX + topics.get(1).substring(26, length).toString();
                    if (isERC20 &&
                            contract.equalsIgnoreCase(erc20) &&
                            (fromAddress.equalsIgnoreCase(htgContext.MULTY_SIGN_ADDRESS()) || fromAddress.equalsIgnoreCase(HtgConstant.ZERO_ADDRESS))) {
                        String toAddress = HtgConstant.HEX_PREFIX + topics.get(2).substring(26, length).toString();
                        if (!receive.equalsIgnoreCase(toAddress)) {
                            logger().warn("提现交易[{}]的接收地址不匹配", txHash);
                            return false;
                        }
                        correctErc20 = true;
                        String data;
                        if (topics.size() == 3) {
                            data = log.getData();
                        } else {
                            data = topics.get(3);
                        }
                        String[] v = data.split("x");
                        // 转账金额
                        BigInteger _amount = new BigInteger(v[1], 16);
                        if (_amount.compareTo(BigInteger.ZERO) > 0) {
                            amount = amount.add(_amount);
                        }
                    }
                }
                // 为主资产提现
                if (eventHash.equals(HtgConstant.EVENT_HASH_TRANSFERFUNDS)) {
                    if (isERC20 || !contract.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                        if (isERC20) {
                            logger().warn("提现交易[{}]的提现类型冲突[0]", txHash);
                        } else {
                            logger().warn("提现交易[{}]的多签合约地址不匹配", txHash);
                        }
                        return false;
                    }
                    correctMainAsset = true;
                    String data = log.getData();
                    String toAddress = HtgConstant.HEX_PREFIX + data.substring(26, 66);
                    if (!receive.equalsIgnoreCase(toAddress)) {
                        logger().warn("提现交易[{}]的接收地址不匹配[主资产提现]", txHash);
                        return false;
                    }
                    String amountStr = data.substring(66, 130);
                    // 转账金额
                    BigInteger _amount = new BigInteger(amountStr, 16);
                    if (_amount.compareTo(BigInteger.ZERO) > 0) {
                        amount = amount.add(_amount);
                    }
                }
            }
            if (correctErc20 && correctMainAsset) {
                logger().warn("提现交易[{}]的提现类型冲突[1]", txHash);
                return false;
            }
            if (correctMainAsset) {
                po.setTo(receive.toLowerCase());
                po.setValue(amount);
                po.setDecimals(htgContext.getConfig().getDecimals());
                po.setAssetId(htgContext.HTG_ASSET_ID());
                return true;
            } else if (correctErc20) {
                po.setIfContractAsset(true);
                po.setContractAddress(erc20);
                po.setTo(receive.toLowerCase());
                po.setValue(amount);
                return true;
            }
        }
        logger().warn("提现交易[{}]解析数据缺失", txHash);
        return false;
    }

    private boolean newParseWithdrawTxReceiptSinceProtocol21(Transaction tx, TransactionReceipt txReceipt, HeterogeneousTransactionBaseInfo po) {
        if (txReceipt == null || !txReceipt.isStatusOK()) {
            return false;
        }
        String txHash = tx.getHash();
        List<Object> withdrawInput = HtgUtil.parseInput(tx.getInput(), HtgConstant.INPUT_WITHDRAW);
        String receive = withdrawInput.get(1).toString();
        Boolean isERC20 = Boolean.parseBoolean(withdrawInput.get(3).toString());
        String erc20 = withdrawInput.get(4).toString().toLowerCase();
        List<Log> logs = txReceipt.getLogs();
        if (logs != null && logs.size() > 0) {
            boolean correctErc20 = false;
            boolean correctMainAsset = false;
            boolean hasReceiveAddress = false;
            List<String> topics;
            String eventHash;
            String contract;
            // 转账金额
            BigInteger amount = BigInteger.ZERO;
            for (Log log : logs) {
                topics = log.getTopics();
                if (topics.size() == 0) {
                    continue;
                }
                eventHash = topics.get(0);
                contract = log.getAddress().toLowerCase();
                // 为ERC20提现
                if (eventHash.equals(HtgConstant.EVENT_HASH_ERC20_TRANSFER)) {
                    int length = topics.get(1).length();
                    String fromAddress = HtgConstant.HEX_PREFIX + topics.get(1).substring(26, length);
                    if (isERC20 &&
                            contract.equalsIgnoreCase(erc20) &&
                            (fromAddress.equalsIgnoreCase(htgContext.MULTY_SIGN_ADDRESS()) || fromAddress.equalsIgnoreCase(HtgConstant.ZERO_ADDRESS))) {
                        String toAddress = HtgConstant.HEX_PREFIX + topics.get(2).substring(26, length);
                        if (!receive.equalsIgnoreCase(toAddress)) {
                            logger().warn("提现交易[{}]的接收地址不匹配", txHash);
                            continue;
                        }
                        correctErc20 = true;
                        hasReceiveAddress = true;
                        String data;
                        if (topics.size() == 3) {
                            data = log.getData();
                        } else {
                            data = topics.get(3);
                        }
                        String[] v = data.split("x");
                        // 转账金额
                        BigInteger _amount = new BigInteger(v[1], 16);
                        if (_amount.compareTo(BigInteger.ZERO) > 0) {
                            amount = amount.add(_amount);
                        }
                    }
                }
                // 为主资产提现
                if (eventHash.equals(HtgConstant.EVENT_HASH_TRANSFERFUNDS)) {
                    if (isERC20 || !contract.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                        if (isERC20) {
                            logger().warn("提现交易[{}]的提现类型冲突[0]", txHash);
                        } else {
                            logger().warn("提现交易[{}]的多签合约地址不匹配", txHash);
                        }
                        return false;
                    }
                    String data = log.getData();
                    String toAddress = HtgConstant.HEX_PREFIX + data.substring(26, 66);
                    if (!receive.equalsIgnoreCase(toAddress)) {
                        logger().warn("提现交易[{}]的接收地址不匹配[主资产提现]", txHash);
                        return false;
                    }
                    correctMainAsset = true;
                    hasReceiveAddress = true;
                    String amountStr = data.substring(66, 130);
                    // 转账金额
                    BigInteger _amount = new BigInteger(amountStr, 16);
                    if (_amount.compareTo(BigInteger.ZERO) > 0) {
                        amount = amount.add(_amount);
                    }
                }
            }
            if (!hasReceiveAddress) {
                logger().warn("提现交易[{}]的接收地址不匹配", txHash);
                return false;
            }
            if (correctErc20 && correctMainAsset) {
                logger().warn("提现交易[{}]的提现类型冲突[1]", txHash);
                return false;
            }
            if (correctMainAsset) {
                po.setTo(receive.toLowerCase());
                po.setValue(amount);
                po.setDecimals(htgContext.getConfig().getDecimals());
                po.setAssetId(htgContext.HTG_ASSET_ID());
                return true;
            } else if (correctErc20) {
                po.setIfContractAsset(true);
                po.setContractAddress(erc20);
                po.setTo(receive.toLowerCase());
                po.setValue(amount);
                return true;
            }
        }
        logger().warn("提现交易[{}]解析数据缺失", txHash);
        return false;
    }

    public boolean validationEthDepositByCrossOutII(Transaction tx, TransactionReceipt txReceipt, HeterogeneousTransactionInfo po) throws Exception {
        if (tx == null) {
            logger().warn("交易不存在");
            return false;
        }
        String txHash = tx.getHash();
        if (txReceipt == null) {
            txReceipt = htgWalletApi.getTxReceipt(txHash);
        }
        List<Log> logs = txReceipt.getLogs();
        if (logs == null || logs.isEmpty()) {
            logger().warn("交易[{}]事件为空", txHash);
            return false;
        }
        List<Object> crossOutInput = HtgUtil.parseInput(tx.getInput(), HtgConstant.INPUT_CROSS_OUT_II);
        String _to = crossOutInput.get(0).toString();
        BigInteger _amount = new BigInteger(crossOutInput.get(1).toString());
        String _erc20 = crossOutInput.get(2).toString().toLowerCase();
        if (HtgConstant.ZERO_ADDRESS.equals(_erc20)) {
            // 主资产充值交易
            for (Log log : logs) {
                List<String> topics = log.getTopics();
                String eventHash = topics.get(0);
                String eventContract = log.getAddress().toLowerCase();
                if (!HtgConstant.EVENT_HASH_CROSS_OUT_II_FUNDS.equals(eventHash)) {
                    continue;
                }
                if (!eventContract.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                    continue;
                }
                List<Object> depositEvent = HtgUtil.parseEvent(log.getData(), HtgConstant.EVENT_CROSS_OUT_II_FUNDS);
                if (depositEvent == null && depositEvent.size() != 6) {
                    logger().warn("交易[{}]CrossOutII事件数据不合法[0]", txHash);
                    return false;
                }
                String from = depositEvent.get(0).toString();
                String to = depositEvent.get(1).toString();
                String erc20 = depositEvent.get(3).toString();
                BigInteger ethAmount = new BigInteger(depositEvent.get(4).toString());
                String extend = Numeric.toHexString((byte[]) depositEvent.get(5));
                po.setDepositIIExtend(extend);
                if (tx.getFrom().equals(from) && tx.getValue().compareTo(ethAmount) == 0 && HtgConstant.ZERO_ADDRESS.equals(erc20)) {
                    if (po != null) {
                        po.setIfContractAsset(false);
                        po.setFrom(from);
                        po.setTo(tx.getTo());
                        po.setValue(ethAmount);
                        po.setDecimals(htgContext.getConfig().getDecimals());
                        po.setAssetId(htgContext.HTG_ASSET_ID());
                        po.setNerveAddress(to);
                        po.setTxType(HeterogeneousChainTxType.DEPOSIT);
                    }
                    return true;
                }
            }
            logger().warn("交易[{}]的主资产[{}]充值II事件不匹配", txHash, htgContext.getConfig().getSymbol());
            return false;
        } else {
            // ERC20充值和主资产充值
            if (!htgERC20Helper.isERC20(_erc20, po)) {
                logger().warn("CrossOutII: erc20[{}]未注册", _erc20);
                return false;
            }
            boolean transferEvent = false;
            boolean burnEvent = true;
            boolean crossOutEvent = false;
            BigInteger erc20Amount = BigInteger.ZERO;
            for (Log log : logs) {
                List<String> topics = log.getTopics();
                String eventHash = topics.get(0);
                String eventContract = log.getAddress().toLowerCase();
                if (HtgConstant.EVENT_HASH_ERC20_TRANSFER.equals(eventHash)) {
                    if (!eventContract.equals(_erc20)) {
                        continue;
                    }
                    int length = topics.get(1).length();
                    String fromAddress = HtgConstant.HEX_PREFIX + topics.get(1).substring(26, length).toString();
                    String toAddress = HtgConstant.HEX_PREFIX + topics.get(2).substring(26, length).toString();
                    String data;
                    if (topics.size() == 3) {
                        data = log.getData();
                    } else {
                        data = topics.get(3);
                    }
                    String[] v = data.split("x");
                    // 转账金额
                    BigInteger amount = new BigInteger(v[1], 16);
                    // 当toAddress是0x0时，则说明这是一个从当前多签合约销毁erc20的transfer事件
                    if (HtgConstant.ZERO_ADDRESS.equals(toAddress)) {
                        if (!fromAddress.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                            continue;
                        }
                        if (amount.compareTo(_amount) != 0) {
                            logger().warn("CrossOutII: 交易[{}]的ERC20销毁金额不匹配", txHash);
                            burnEvent = false;
                            break;
                        }
                    } else {
                        // 用户转移token到多签合约的事件
                        // 必须是多签合约地址
                        if (!toAddress.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                            continue;
                        }
                        erc20Amount = erc20Amount.add(amount);
                        transferEvent = true;
                    }
                }
                if (HtgConstant.EVENT_HASH_CROSS_OUT_II_FUNDS.equals(eventHash)) {
                    if (!eventContract.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                        continue;
                    }
                    List<Object> depositEvent = HtgUtil.parseEvent(log.getData(), HtgConstant.EVENT_CROSS_OUT_II_FUNDS);
                    if (depositEvent == null && depositEvent.size() != 6) {
                        logger().warn("交易[{}]CrossOutII事件数据不合法[1]", txHash);
                        return false;
                    }
                    String from = depositEvent.get(0).toString();
                    String to = depositEvent.get(1).toString();
                    BigInteger amount = new BigInteger(depositEvent.get(2).toString());
                    String erc20 = depositEvent.get(3).toString();
                    BigInteger ethAmount = new BigInteger(depositEvent.get(4).toString());
                    String extend = Numeric.toHexString((byte[]) depositEvent.get(5));
                    if (!tx.getFrom().equals(from)) {
                        logger().warn("交易[{}]CrossOutII事件数据不合法[2]", txHash);
                        return false;
                    }
                    if (!_to.equals(to)) {
                        logger().warn("交易[{}]CrossOutII事件数据不合法[3]", txHash);
                        return false;
                    }
                    if (amount.compareTo(_amount) != 0) {
                        logger().warn("交易[{}]CrossOutII事件数据不合法[4]", txHash);
                        return false;
                    }
                    if (!_erc20.equals(erc20)) {
                        logger().warn("交易[{}]CrossOutII事件数据不合法[5]", txHash);
                        return false;
                    }
                    if (tx.getFrom().equals(from) && tx.getValue().compareTo(BigInteger.ZERO) > 0 && tx.getValue().compareTo(ethAmount) == 0) {
                        // 记录主资产充值
                        po.setDepositIIMainAsset(ethAmount, htgContext.getConfig().getDecimals(), htgContext.HTG_ASSET_ID());
                    }
                    po.setDepositIIExtend(extend);
                    crossOutEvent = true;
                }
            }
            if (transferEvent && burnEvent && crossOutEvent) {
                if (erc20Amount.compareTo(_amount) > 0) {
                    logger().warn("CrossOutII: 交易[{}]的ERC20充值金额不匹配", txHash);
                    return false;
                }
                if (erc20Amount.equals(BigInteger.ZERO)) {
                    logger().warn("CrossOutII: 交易[{}]的ERC20充值金额为0", txHash);
                    return false;
                }

                if (po != null && erc20Amount.compareTo(BigInteger.ZERO) > 0) {
                    po.setIfContractAsset(true);
                    po.setContractAddress(_erc20);
                    po.setFrom(tx.getFrom());
                    po.setTo(tx.getTo());
                    po.setValue(erc20Amount);
                    po.setNerveAddress(_to);
                    po.setTxType(HeterogeneousChainTxType.DEPOSIT);
                }
                return true;
            } else {
                logger().warn("交易[{}]的ERC20充值II事件不匹配, transferEvent: {}, burnEvent: {}, crossOutEvent: {}",
                        txHash, transferEvent, burnEvent, crossOutEvent);
                return false;
            }
        }
    }
}

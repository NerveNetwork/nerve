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
package network.nerve.converter.heterogeneouschain.lib.helper;

import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgInput;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxStorageService;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousTransactionBaseInfo;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

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
        HeterogeneousTransactionInfo txInfo = HtgUtil.newTransactionInfo(tx, htgContext.NERVE_CHAINID());
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
            isWithdraw = this.parseWithdrawTxReceipt(txReceipt, txInfo);
            if (!isWithdraw) {
                logger().warn("不是提现交易[1], hash: {}", txHash);
                return null;
            }
            if (txInfo.isIfContractAsset()) {
                htgERC20Helper.loadERC20(txInfo.getContractAddress(), txInfo);
            }
        } else {
            logger().warn("不是提现交易[2], hash: {}", txHash);
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
     * 解析充值交易数据
     */
    private HeterogeneousTransactionInfo parseDepositTransaction(Transaction tx, TransactionReceipt txReceipt) throws Exception {
        if (tx == null) {
            logger().warn("交易不存在");
            return null;
        }
        String txHash = tx.getHash();
        HeterogeneousTransactionInfo txInfo = HtgUtil.newTransactionInfo(tx, htgContext.NERVE_CHAINID());
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

    public HeterogeneousTransactionInfo parseDepositTransaction(Transaction tx) throws Exception {
        HtgInput htInput = this.parseInput(tx.getInput());
        // 新的充值交易方式，调用多签合约的crossOut函数
        if (htInput.isDepositTx()) {
            HeterogeneousTransactionInfo po = new HeterogeneousTransactionInfo();
            po.setTxHash(tx.getHash());
            po.setBlockHeight(tx.getBlockNumber().longValue());
            boolean isDepositTx = this.validationEthDepositByCrossOut(tx, po);
            if (!isDepositTx) {
                return null;
            }
            return po;
        }
        return this.parseDepositTransaction(tx, null);
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
            // HT充值交易
            Log log = logs.get(0);
            List<String> topics = log.getTopics();
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
                        if (!fromAddress.equals(tx.getTo())) {
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
                        if (!toAddress.equals(tx.getTo())) {
                            logger().warn("交易[{}]的ERC20充值地址不匹配", txHash);
                            return false;
                        }
                        // 是否支持转账即销毁部分的ERC20
                        if (htgContext.getConverterCoreApi().isSupportERC20OfTransferBurn()) {
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
    public HeterogeneousTransactionInfo parseManagerChangeTransaction(Transaction tx, TransactionReceipt txReceipt) {
        if (tx == null) {
            logger().warn("交易不存在");
            return null;
        }
        HeterogeneousTransactionInfo txInfo = HtgUtil.newTransactionInfo(tx, htgContext.NERVE_CHAINID());
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
    public HeterogeneousTransactionInfo parseUpgradeTransaction(Transaction tx, TransactionReceipt txReceipt) {
        if (tx == null) {
            logger().warn("交易不存在");
            return null;
        }
        HeterogeneousTransactionInfo txInfo = HtgUtil.newTransactionInfo(tx, htgContext.NERVE_CHAINID());
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
        String eventHash = topics.get(0);
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
        return HtgInput.empty();
    }

}

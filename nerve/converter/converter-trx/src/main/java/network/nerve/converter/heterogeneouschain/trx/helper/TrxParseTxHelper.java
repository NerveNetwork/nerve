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
package network.nerve.converter.heterogeneouschain.trx.helper;

import com.google.protobuf.ByteString;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgInput;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgTxStorageService;
import network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant;
import network.nerve.converter.heterogeneouschain.trx.core.TrxWalletApi;
import network.nerve.converter.heterogeneouschain.trx.model.TRC20TransferEvent;
import network.nerve.converter.heterogeneouschain.trx.model.TrxTransaction;
import network.nerve.converter.heterogeneouschain.trx.utils.TrxUtil;
import network.nerve.converter.model.bo.*;
import org.tron.trident.abi.FunctionReturnDecoder;
import org.tron.trident.abi.datatypes.Address;
import org.tron.trident.abi.datatypes.Function;
import org.tron.trident.abi.datatypes.Type;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Contract;
import org.tron.trident.proto.Response;
import org.tron.trident.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant.HEX_PREFIX;

/**
 * @author: Mimi
 * @date: 2020-03-20
 */
public class TrxParseTxHelper implements BeanInitial {

    private TrxERC20Helper trxERC20Helper;
    private TrxWalletApi trxWalletApi;
    private HtgTxStorageService htgTxStorageService;
    private HtgListener htgListener;
    private HtgContext htgContext;

    private NulsLogger logger() {
        return htgContext.logger();
    }

    public boolean isCompletedTransaction(String nerveTxHash) throws Exception {
        Function isCompletedFunction = TrxUtil.getIsCompletedFunction(nerveTxHash);
        List<Type> valueTypes = trxWalletApi.callViewFunction(htgContext.MULTY_SIGN_ADDRESS(), isCompletedFunction);
        boolean isCompleted = Boolean.parseBoolean(valueTypes.get(0).getValue().toString());
        return isCompleted;
    }

    public boolean isMinterERC20(String erc20) throws Exception {
        Function isMinterERC20Function = TrxUtil.getIsMinterERC20Function(erc20);
        List<Type> valueTypes = trxWalletApi.callViewFunction(htgContext.MULTY_SIGN_ADDRESS(), isMinterERC20Function);
        boolean isMinterERC20 = Boolean.parseBoolean(valueTypes.get(0).getValue().toString());
        return isMinterERC20;
    }

    public HeterogeneousTransactionInfo parseWithdrawTransaction(String txHash) throws Exception {
        Chain.Transaction tx = trxWalletApi.getTransactionByHash(txHash);
        if (tx == null) {
            logger().warn("交易不存在");
            return null;
        }
        return this.parseWithdrawTransaction(tx);
    }

    public HeterogeneousTransactionInfo parseWithdrawTransaction(Chain.Transaction tx) throws Exception {
        if (tx == null) {
            logger().warn("交易不存在");
            return null;
        }
        Chain.Transaction.Contract contract = tx.getRawData().getContract(0);
        Chain.Transaction.Contract.ContractType type = contract.getType();
        if (Chain.Transaction.Contract.ContractType.TriggerSmartContract != type) {
            logger().warn("不是提现交易");
            return null;
        }
        Contract.TriggerSmartContract tg = Contract.TriggerSmartContract.parseFrom(contract.getParameter().getValue());
        String from = TrxUtil.ethAddress2trx(tg.getOwnerAddress().toByteArray());
        String to = TrxUtil.ethAddress2trx(tg.getContractAddress().toByteArray());
        BigInteger value = BigInteger.valueOf(tg.getCallValue());
        String input = Numeric.toHexString(tg.getData().toByteArray());
        // 计算txHash
        String txHash = TrxUtil.calcTxHash(tx);
        TrxTransaction txInfo = new TrxTransaction(tx, txHash, from, to, value, input, type);
        return this.parseWithdrawTransaction(txInfo, null);
    }

    /**
     * 解析提现交易数据
     */
    public HeterogeneousTransactionInfo parseWithdrawTransaction(TrxTransaction trxTxInfo, Response.TransactionInfo txReceipt) throws Exception {
        if (trxTxInfo == null) {
            logger().warn("解析交易的数据不存在或不完整");
            return null;
        }
        String txHash = trxTxInfo.getHash();
        HeterogeneousTransactionInfo txInfo = TrxUtil.newTransactionInfo(trxTxInfo, htgContext.NERVE_CHAINID());

        boolean isWithdraw;
        if (trxTxInfo.getInput().length() < 10) {
            logger().warn("不是提现交易[0]");
            return null;
        }
        String methodNameHash = trxTxInfo.getInput().substring(0, 10);
        // 提现交易的固定地址
        if (htgListener.isListeningAddress(trxTxInfo.getTo()) &&
                TrxConstant.METHOD_HASH_CREATEORSIGNWITHDRAW.equals(methodNameHash)) {
            if (txReceipt == null) {
                txReceipt = trxWalletApi.getTransactionReceipt(txHash);
            }
            txInfo.setBlockHeight(txReceipt.getBlockNumber());
            txInfo.setTxTime(txReceipt.getBlockTimeStamp());
            // 解析交易收据
            if (htgContext.getConverterCoreApi().isProtocol21()) {
                // 协议v1.21
                isWithdraw = this.newParseWithdrawTxReceiptSinceProtocol21(trxTxInfo, txReceipt, txInfo);
            } else {
                isWithdraw = this.newParseWithdrawTxReceipt(trxTxInfo, txReceipt, txInfo);
            }

            if (!isWithdraw) {
                logger().warn("不是提现交易[1], hash: {}", txHash);
                return null;
            }
            if (txInfo.isIfContractAsset()) {
                trxERC20Helper.loadERC20(txInfo.getContractAddress(), txInfo);
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

    public HeterogeneousTransactionInfo parseDepositTransaction(String txHash) throws Exception {
        Chain.Transaction tx = trxWalletApi.getTransactionByHash(txHash);
        if (tx == null) {
            logger().warn("交易不存在");
            return null;
        }
        return this.parseDepositTransaction(tx);
    }

    public HeterogeneousTransactionInfo parseDepositTransaction(Chain.Transaction tx) throws Exception {
        return this.parseDepositTransaction(tx, null);
    }

    public HeterogeneousTransactionInfo parseDepositTransaction(Chain.Transaction tx, Response.TransactionInfo txReceipt) throws Exception {
        if (tx == null) {
            logger().warn("交易不存在");
            return null;
        }
        TrxTransaction trxTxInfo = TrxUtil.generateTxInfo(tx);
        // 过滤 非TRX转账和调用合约交易
        if (trxTxInfo == null) {
            logger().warn("不是充值交易");
            return null;
        }

        HtgInput htInput = this.parseInput(trxTxInfo.getInput());
        // 新的充值交易方式，调用多签合约的crossOut函数
        if (htInput.isDepositTx()) {
            HeterogeneousTransactionInfo po = new HeterogeneousTransactionInfo();
            po.setTxHash(trxTxInfo.getHash());
            boolean isDepositTx = this.validationDepositByCrossOut(trxTxInfo, po);
            if (!isDepositTx) {
                return null;
            }
            po.setTxType(HeterogeneousChainTxType.DEPOSIT);
            return po;
        }
        // 新的充值交易方式II，调用多签合约的crossOutII函数
        if (htInput.isDepositIITx()) {
            HeterogeneousTransactionInfo po = new HeterogeneousTransactionInfo();
            po.setTxHash(trxTxInfo.getHash());
            boolean isDepositTx = this.validationDepositByCrossOutII(trxTxInfo, null, po);
            if (!isDepositTx) {
                return null;
            }
            po.setTxType(HeterogeneousChainTxType.DEPOSIT);
            return po;
        }
        return this.parseDepositTransactionByTransferDirectly(trxTxInfo, txReceipt);
    }

    /**
     * 解析充值交易数据
     */
    private HeterogeneousTransactionInfo parseDepositTransactionByTransferDirectly(TrxTransaction trxTxInfo, Response.TransactionInfo txReceipt) throws Exception {
        if (trxTxInfo == null) {
            logger().warn("交易不存在");
            return null;
        }
        String txHash = trxTxInfo.getHash();
        HeterogeneousTransactionInfo txInfo = TrxUtil.newTransactionInfo(trxTxInfo, htgContext.NERVE_CHAINID());
        boolean isDeposit = false;
        if (txReceipt == null) {
            txReceipt = trxWalletApi.getTransactionReceipt(txHash);
        }
        txInfo.setBlockHeight(txReceipt.getBlockNumber());
        txInfo.setTxTime(txReceipt.getBlockTimeStamp());
        do {
            // HT充值交易的固定接收地址,金额大于0, 没有input
            if (htgListener.isListeningAddress(trxTxInfo.getTo()) && trxTxInfo.getValue().compareTo(BigInteger.ZERO) > 0) {
                if(Chain.Transaction.Contract.ContractType.TriggerSmartContract == trxTxInfo.getType() && !this.validationDeposit(trxTxInfo, txReceipt)) {
                    logger().error("[{}]不是充值交易[0]", txHash);
                    return null;
                }
                isDeposit = true;
                txInfo.setDecimals(htgContext.getConfig().getDecimals());
                txInfo.setAssetId(htgContext.HTG_ASSET_ID());
                txInfo.setValue(trxTxInfo.getValue());
                txInfo.setIfContractAsset(false);
                break;
            }
            // ERC20充值交易
            if (trxERC20Helper.isERC20(trxTxInfo.getTo(), txInfo)) {
                if (trxERC20Helper.hasERC20WithListeningAddress(txReceipt, txInfo, address -> htgListener.isListeningAddress(address))) {
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

    public boolean validationDeposit(TrxTransaction tx) throws Exception {
        return this.validationDeposit(tx, null);
    }

    private boolean validationDeposit(TrxTransaction tx, Response.TransactionInfo txReceipt) throws Exception {
        if (tx == null) {
            logger().warn("交易不存在");
            return false;
        }
        String txHash = tx.getHash();
        if (txReceipt == null) {
            txReceipt = trxWalletApi.getTransactionReceipt(txHash);
        }
        List<Response.TransactionInfo.Log> logs = txReceipt.getLogList();
        if (logs == null || logs.isEmpty()) {
            return false;
        }
        for (Response.TransactionInfo.Log log : logs) {
            String eventHash = Numeric.toHexString(log.getTopics(0).toByteArray());
            if (!TrxConstant.EVENT_HASH_HT_DEPOSIT_FUNDS.equals(eventHash)) {
                continue;
            }
            List<Object> depositEvent = TrxUtil.parseEvent(Numeric.toHexString(log.getData().toByteArray()), TrxConstant.EVENT_DEPOSIT_FUNDS);
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

    public boolean validationDepositByCrossOut(TrxTransaction tx, HeterogeneousTransactionInfo po) throws Exception {
        return this.validationDepositByCrossOut(tx, null, po);
    }

    private boolean validationDepositByCrossOut(TrxTransaction tx, Response.TransactionInfo txReceipt, HeterogeneousTransactionInfo po) throws Exception {
        return newValidationDepositByCrossOut(tx, txReceipt, po);
    }

    /**
     * 解析管理员变更交易数据
     */
    public HeterogeneousTransactionInfo parseManagerChangeTransaction(TrxTransaction trxTxInfo, Response.TransactionInfo txReceipt) throws Exception {
        if (trxTxInfo == null) {
            logger().warn("交易不存在");
            return null;
        }
        HeterogeneousTransactionInfo txInfo = TrxUtil.newTransactionInfo(trxTxInfo, htgContext.NERVE_CHAINID());
        txInfo.setBlockHeight(txReceipt.getBlockNumber());
        txInfo.setTxTime(txReceipt.getBlockTimeStamp());
        boolean isChange = false;
        String input, methodHash;
        if (htgListener.isListeningAddress(trxTxInfo.getTo()) && (input = trxTxInfo.getInput()).length() >= 10) {
            methodHash = input.substring(0, 10);
            if (methodHash.equals(TrxConstant.METHOD_HASH_CREATEORSIGNMANAGERCHANGE)) {
                isChange = true;
                List<Object> inputData = TrxUtil.parseInput(input, TrxConstant.INPUT_CHANGE);
                List<Address> adds = (List<Address>) inputData.get(1);
                List<Address> quits = (List<Address>) inputData.get(2);
                if (!adds.isEmpty()) {
                    txInfo.setAddAddresses(TrxUtil.list2array(adds.stream().map(a -> a.getValue()).collect(Collectors.toList())));
                }
                if (!quits.isEmpty()) {
                    txInfo.setRemoveAddresses(TrxUtil.list2array(quits.stream().map(q -> q.getValue()).collect(Collectors.toList())));
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
    public HeterogeneousTransactionInfo parseUpgradeTransaction(TrxTransaction trxTxInfo, Response.TransactionInfo txReceipt) throws Exception {
        if (trxTxInfo == null) {
            logger().warn("交易不存在");
            return null;
        }
        HeterogeneousTransactionInfo txInfo = TrxUtil.newTransactionInfo(trxTxInfo, htgContext.NERVE_CHAINID());
        txInfo.setBlockHeight(txReceipt.getBlockNumber());
        txInfo.setTxTime(txReceipt.getBlockTimeStamp());
        boolean isUpgrade = false;
        String input, methodHash;
        if (htgListener.isListeningAddress(trxTxInfo.getTo()) && (input = trxTxInfo.getInput()).length() >= 10) {
            methodHash = input.substring(0, 10);
            if (methodHash.equals(TrxConstant.METHOD_HASH_CREATEORSIGNUPGRADE)) {
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

    public List<HeterogeneousAddress> parseSigners(Response.TransactionInfo txReceipt, String txFrom) {
        List<Object> eventResult = this.loadDataFromEvent(txReceipt);
        if (eventResult == null || eventResult.isEmpty()) {
            return null;
        }
        List<HeterogeneousAddress> signers = new ArrayList<>();
        signers.add(new HeterogeneousAddress(htgContext.getConfig().getChainId(), txFrom));
        return signers;
    }

    private void loadSigners(Response.TransactionInfo txReceipt, HeterogeneousTransactionInfo txInfo) {
        List<Object> eventResult = this.loadDataFromEvent(txReceipt);
        if (eventResult != null && !eventResult.isEmpty()) {
            txInfo.setNerveTxHash(eventResult.get(eventResult.size() - 1).toString());
            List<HeterogeneousAddress> signers = new ArrayList<>();
            signers.add(new HeterogeneousAddress(htgContext.getConfig().getChainId(), txInfo.getFrom()));
            txInfo.setSigners(signers);
        }
    }

    private List<Object> loadDataFromEvent(Response.TransactionInfo txReceipt) {
        List<Response.TransactionInfo.Log> logs = txReceipt.getLogList();
        if (logs == null || logs.isEmpty()) {
            return null;
        }
        Response.TransactionInfo.Log log = logs.get(logs.size() - 1);
        String eventHash = Numeric.toHexString(log.getTopics(0).toByteArray());
        String logData = Numeric.toHexString(log.getData().toByteArray());
        // topics 解析事件名, 签名完成会触发的事件
        // 解析事件数据，获得交易的成功事件数据列表
        List<Object> eventResult = null;
        switch (eventHash) {
            case TrxConstant.EVENT_HASH_TRANSACTION_WITHDRAW_COMPLETED:
                eventResult = TrxUtil.parseEvent(logData, TrxConstant.EVENT_TRANSACTION_WITHDRAW_COMPLETED);
                break;
            case TrxConstant.EVENT_HASH_TRANSACTION_MANAGER_CHANGE_COMPLETED:
                eventResult = TrxUtil.parseEvent(logData, TrxConstant.EVENT_TRANSACTION_MANAGER_CHANGE_COMPLETED);
                break;
            case TrxConstant.EVENT_HASH_TRANSACTION_UPGRADE_COMPLETED:
                eventResult = TrxUtil.parseEvent(logData, TrxConstant.EVENT_TRANSACTION_UPGRADE_COMPLETED);
                break;
        }
        return eventResult;
    }

    public HtgInput parseInput(String input) {
        if(StringUtils.isBlank(input)) {
            return HtgInput.empty();
        }
        input = Numeric.cleanHexPrefix(input);
        if (input.length() < 8) {
            return HtgInput.empty();
        }
        String methodHash;
        if ((methodHash = HEX_PREFIX + input.substring(0, 8)).equals(TrxConstant.METHOD_HASH_CREATEORSIGNWITHDRAW)) {
            return new HtgInput(true, HeterogeneousChainTxType.WITHDRAW, TrxUtil.parseInput(input, TrxConstant.INPUT_WITHDRAW).get(0).toString());
        }
        if (methodHash.equals(TrxConstant.METHOD_HASH_CREATEORSIGNMANAGERCHANGE)) {
            return new HtgInput(true, HeterogeneousChainTxType.CHANGE, TrxUtil.parseInput(input, TrxConstant.INPUT_CHANGE).get(0).toString());
        }
        if (methodHash.equals(TrxConstant.METHOD_HASH_CREATEORSIGNUPGRADE)) {
            return new HtgInput(true, HeterogeneousChainTxType.UPGRADE, TrxUtil.parseInput(input, TrxConstant.INPUT_UPGRADE).get(0).toString());
        }
        if (methodHash.equals(TrxConstant.METHOD_HASH_CROSS_OUT)) {
            return new HtgInput(true, HeterogeneousChainTxType.DEPOSIT);
        }
        if (methodHash.equals(TrxConstant.METHOD_HASH_CROSS_OUT_II)) {
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
        if (!TrxConstant.METHOD_HASH_ONE_CLICK_CROSS_CHAIN.equals(methodHash)) {
            return null;
        }
        extend = TrxConstant.HEX_PREFIX + extend.substring(10);
        try {
            List<Type> typeList = FunctionReturnDecoder.decode(extend, TrxConstant.INPUT_ONE_CLICK_CROSS_CHAIN);
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
        if (!TrxConstant.METHOD_HASH_ADD_FEE_CROSS_CHAIN.equals(methodHash)) {
            return null;
        }
        extend = TrxConstant.HEX_PREFIX + extend.substring(10);
        try {
            List<Type> typeList = FunctionReturnDecoder.decode(extend, TrxConstant.INPUT_ADD_FEE_CROSS_CHAIN);
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


    private boolean newValidationDepositByCrossOut(TrxTransaction tx, Response.TransactionInfo txReceipt, HeterogeneousTransactionInfo po) throws Exception {
        if (tx == null) {
            logger().warn("交易不存在");
            return false;
        }
        String txHash = tx.getHash();
        if (txReceipt == null) {
            txReceipt = trxWalletApi.getTransactionReceipt(txHash);
        }
        po.setBlockHeight(txReceipt.getBlockNumber());
        po.setTxTime(txReceipt.getBlockTimeStamp());
        List<Response.TransactionInfo.Log> logs = txReceipt.getLogList();
        if (logs == null || logs.isEmpty()) {
            logger().warn("交易[{}]事件为空", txHash);
            return false;
        }
        List<Object> crossOutInput = TrxUtil.parseInput(tx.getInput(), TrxConstant.INPUT_CROSS_OUT);
        String _to = crossOutInput.get(0).toString();
        BigInteger _amount = new BigInteger(crossOutInput.get(1).toString());
        String _erc20 = crossOutInput.get(2).toString();
        if (TrxConstant.ZERO_ADDRESS_TRX.equals(_erc20)) {
            // 主资产充值交易
            for (Response.TransactionInfo.Log log : logs) {
                ByteString topics = log.getTopics(0);
                String eventHash = Numeric.toHexString(topics.toByteArray());
                String eventContract = TrxUtil.ethAddress2trx(log.getAddress().toByteArray());
                if (!TrxConstant.EVENT_HASH_CROSS_OUT_FUNDS.equals(eventHash)) {
                    continue;
                }
                if (!eventContract.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                    continue;
                }
                String data = Numeric.toHexString(log.getData().toByteArray());
                List<Object> depositEvent = TrxUtil.parseEvent(data, TrxConstant.EVENT_CROSS_OUT_FUNDS);
                if (depositEvent == null && depositEvent.size() != 4) {
                    logger().warn("交易[{}]CrossOut事件数据不合法[0]", txHash);
                    return false;
                }
                String from = depositEvent.get(0).toString();
                String to = depositEvent.get(1).toString();
                BigInteger amount = new BigInteger(depositEvent.get(2).toString());
                String erc20 = depositEvent.get(3).toString();
                if (tx.getFrom().equals(from) && tx.getValue().compareTo(amount) == 0 && TrxConstant.ZERO_ADDRESS_TRX.equals(erc20)) {
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
            if (!trxERC20Helper.isERC20(_erc20, po)) {
                logger().warn("erc20[{}]未注册", _erc20);
                return false;
            }
            boolean transferEvent = false;
            boolean burnEvent = true;
            boolean crossOutEvent = false;
            BigInteger actualAmount;
            BigInteger calcAmount = BigInteger.ZERO;
            for (Response.TransactionInfo.Log log : logs) {
                ByteString topics = log.getTopics(0);
                String eventHash = Numeric.toHexString(topics.toByteArray());
                String eventContract = TrxUtil.ethAddress2trx(log.getAddress().toByteArray());
                if (TrxConstant.EVENT_HASH_ERC20_TRANSFER.equals(eventHash)) {
                    if (!eventContract.equals(_erc20)) {
                        continue;
                    }
                    TRC20TransferEvent trc20Event = TrxUtil.parseTRC20Event(log);
                    String fromAddress = trc20Event.getFrom();
                    String toAddress = trc20Event.getTo();
                    // 转账金额
                    BigInteger amount = trc20Event.getValue();
                    // 当toAddress是0x0时，则说明这是一个从当前多签合约销毁erc20的transfer事件
                    if (TrxConstant.ZERO_ADDRESS_TRX.equals(toAddress)) {
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
                if (TrxConstant.EVENT_HASH_CROSS_OUT_FUNDS.equals(eventHash)) {
                    if (!eventContract.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                        continue;
                    }
                    List<Object> depositEvent = TrxUtil.parseEvent(Numeric.toHexString(log.getData().toByteArray()), TrxConstant.EVENT_CROSS_OUT_FUNDS);
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

    public boolean validationDepositByCrossOutII(TrxTransaction tx, Response.TransactionInfo txReceipt, HeterogeneousTransactionInfo po) throws Exception {
        if (tx == null) {
            logger().warn("交易不存在");
            return false;
        }
        String txHash = tx.getHash();
        if (txReceipt == null) {
            txReceipt = trxWalletApi.getTransactionReceipt(txHash);
        }
        po.setBlockHeight(txReceipt.getBlockNumber());
        po.setTxTime(txReceipt.getBlockTimeStamp());
        List<Response.TransactionInfo.Log> logs = txReceipt.getLogList();
        if (logs == null || logs.isEmpty()) {
            logger().warn("交易[{}]事件为空", txHash);
            return false;
        }
        List<Object> crossOutInput = TrxUtil.parseInput(tx.getInput(), TrxConstant.INPUT_CROSS_OUT_II);
        String _to = crossOutInput.get(0).toString();
        BigInteger _amount = new BigInteger(crossOutInput.get(1).toString());
        String _erc20 = crossOutInput.get(2).toString();
        if (TrxConstant.ZERO_ADDRESS_TRX.equals(_erc20)) {
            // 主资产充值交易
            for (Response.TransactionInfo.Log log : logs) {
                ByteString topics = log.getTopics(0);
                String eventHash = Numeric.toHexString(topics.toByteArray());
                String eventContract = TrxUtil.ethAddress2trx(log.getAddress().toByteArray());
                if (!TrxConstant.EVENT_HASH_CROSS_OUT_II_FUNDS.equals(eventHash)) {
                    continue;
                }
                if (!eventContract.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                    continue;
                }
                String data = Numeric.toHexString(log.getData().toByteArray());
                List<Object> depositEvent = TrxUtil.parseEvent(data, TrxConstant.EVENT_CROSS_OUT_II_FUNDS);
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
                if (tx.getFrom().equals(from) && tx.getValue().compareTo(ethAmount) == 0 && TrxConstant.ZERO_ADDRESS_TRX.equals(erc20)) {
                    if (po != null) {
                        po.setIfContractAsset(false);
                        po.setFrom(from);
                        po.setTo(tx.getTo());
                        po.setValue(ethAmount);
                        po.setDecimals(htgContext.getConfig().getDecimals());
                        po.setAssetId(htgContext.HTG_ASSET_ID());
                        po.setNerveAddress(to);
                    }
                    return true;
                }
            }
            logger().warn("交易[{}]的主资产[{}]充值II事件不匹配", txHash, htgContext.getConfig().getSymbol());
            return false;
        } else {
            // ERC20充值交易
            if (!trxERC20Helper.isERC20(_erc20, po)) {
                logger().warn("CrossOutII: erc20[{}]未注册", _erc20);
                return false;
            }
            boolean transferEvent = false;
            boolean burnEvent = true;
            boolean crossOutEvent = false;
            BigInteger erc20Amount = BigInteger.ZERO;
            for (Response.TransactionInfo.Log log : logs) {
                ByteString topics = log.getTopics(0);
                String eventHash = Numeric.toHexString(topics.toByteArray());
                String eventContract = TrxUtil.ethAddress2trx(log.getAddress().toByteArray());
                if (TrxConstant.EVENT_HASH_ERC20_TRANSFER.equals(eventHash)) {
                    if (!eventContract.equals(_erc20)) {
                        continue;
                    }
                    TRC20TransferEvent trc20Event = TrxUtil.parseTRC20Event(log);
                    String fromAddress = trc20Event.getFrom();
                    String toAddress = trc20Event.getTo();
                    // 转账金额
                    BigInteger amount = trc20Event.getValue();
                    // 当toAddress是0x0时，则说明这是一个从当前多签合约销毁erc20的transfer事件
                    if (TrxConstant.ZERO_ADDRESS_TRX.equals(toAddress)) {
                        if (!fromAddress.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                            logger().warn("CrossOutII: 交易[{}]的销毁地址不匹配", txHash);
                            burnEvent = false;
                            break;
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
                if (TrxConstant.EVENT_HASH_CROSS_OUT_II_FUNDS.equals(eventHash)) {
                    if (!eventContract.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                        continue;
                    }
                    List<Object> depositEvent = TrxUtil.parseEvent(Numeric.toHexString(log.getData().toByteArray()), TrxConstant.EVENT_CROSS_OUT_II_FUNDS);
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
                }
                return true;
            } else {
                logger().warn("交易[{}]的ERC20充值II事件不匹配, transferEvent: {}, burnEvent: {}, crossOutEvent: {}",
                        txHash, transferEvent, burnEvent, crossOutEvent);
                return false;
            }
        }
    }

    private boolean newParseWithdrawTxReceipt(TrxTransaction trxTxInfo, Response.TransactionInfo txReceipt, HeterogeneousTransactionBaseInfo po) {
        if (!TrxUtil.checkTransactionSuccess(txReceipt)) {
            return false;
        }
        String txHash = trxTxInfo.getHash();
        List<Object> withdrawInput = TrxUtil.parseInput(trxTxInfo.getInput(), TrxConstant.INPUT_WITHDRAW);
        String receive = withdrawInput.get(1).toString();
        Boolean isERC20 = Boolean.parseBoolean(withdrawInput.get(3).toString());
        String erc20 = withdrawInput.get(4).toString();
        boolean correctErc20 = false;
        boolean correctMainAsset = false;
        List<Response.TransactionInfo.Log> logs = txReceipt.getLogList();
        if (logs != null && logs.size() > 0) {
            String eventHash;
            String contract;
            // 转账金额
            BigInteger amount = BigInteger.ZERO;
            for (Response.TransactionInfo.Log log : logs) {
                eventHash = Numeric.toHexString(log.getTopics(0).toByteArray());
                contract = TrxUtil.ethAddress2trx(log.getAddress().toByteArray());
                // 为ERC20提现
                if (eventHash.equals(TrxConstant.EVENT_HASH_ERC20_TRANSFER)) {
                    TRC20TransferEvent trc20Event = TrxUtil.parseTRC20Event(log);
                    String fromAddress = trc20Event.getFrom();
                    String toAddress = trc20Event.getTo();
                    // 转账金额
                    BigInteger _amount = trc20Event.getValue();
                    if (isERC20 &&
                            contract.equals(erc20) &&
                            (fromAddress.equals(htgContext.MULTY_SIGN_ADDRESS()) || fromAddress.equals(TrxConstant.ZERO_ADDRESS_TRX))) {
                        if (!receive.equals(toAddress)) {
                            logger().warn("提现交易[{}]的接收地址不匹配", txHash);
                            return false;
                        }
                        correctErc20 = true;
                        if (_amount.compareTo(BigInteger.ZERO) > 0) {
                            amount = amount.add(_amount);
                        }
                    }
                }
                // 为主资产提现
                if (eventHash.equals(TrxConstant.EVENT_HASH_TRANSFERFUNDS)) {
                    if (isERC20 || !contract.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                        if (isERC20) {
                            logger().warn("提现交易[{}]的提现类型冲突[0]", txHash);
                        } else {
                            logger().warn("提现交易[{}]的多签合约地址不匹配", txHash);
                        }
                        return false;
                    }
                    correctMainAsset = true;
                    String data = Numeric.toHexString(log.getData().toByteArray());
                    List<Object> objects = TrxUtil.parseEvent(data, TrxConstant.EVENT_TRANSFER_FUNDS);
                    String toAddress = objects.get(0).toString();
                    if (!receive.equals(toAddress)) {
                        logger().warn("提现交易[{}]的接收地址不匹配[主资产提现]", txHash);
                        return false;
                    }
                    // 转账金额
                    BigInteger _amount = (BigInteger) objects.get(1);
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
                po.setTo(receive);
                po.setValue(amount);
                po.setDecimals(htgContext.getConfig().getDecimals());
                po.setAssetId(htgContext.HTG_ASSET_ID());
                return true;
            } else if (correctErc20) {
                po.setIfContractAsset(true);
                po.setContractAddress(erc20);
                po.setTo(receive);
                po.setValue(amount);
                return true;
            }
        }
        logger().warn("提现交易[{}]解析数据缺失", txHash);
        return false;
    }

    private boolean newParseWithdrawTxReceiptSinceProtocol21(TrxTransaction trxTxInfo, Response.TransactionInfo txReceipt, HeterogeneousTransactionBaseInfo po) {
        if (!TrxUtil.checkTransactionSuccess(txReceipt)) {
            return false;
        }
        String txHash = trxTxInfo.getHash();
        List<Object> withdrawInput = TrxUtil.parseInput(trxTxInfo.getInput(), TrxConstant.INPUT_WITHDRAW);
        String receive = withdrawInput.get(1).toString();
        Boolean isERC20 = Boolean.parseBoolean(withdrawInput.get(3).toString());
        String erc20 = withdrawInput.get(4).toString();
        List<Response.TransactionInfo.Log> logs = txReceipt.getLogList();
        if (logs != null && logs.size() > 0) {
            boolean correctErc20 = false;
            boolean correctMainAsset = false;
            boolean hasReceiveAddress = false;
            String eventHash;
            String contract;
            // 转账金额
            BigInteger amount = BigInteger.ZERO;
            for (Response.TransactionInfo.Log log : logs) {
                eventHash = Numeric.toHexString(log.getTopics(0).toByteArray());
                contract = TrxUtil.ethAddress2trx(log.getAddress().toByteArray());
                // 为ERC20提现
                if (eventHash.equals(TrxConstant.EVENT_HASH_ERC20_TRANSFER)) {
                    TRC20TransferEvent trc20Event = TrxUtil.parseTRC20Event(log);
                    String fromAddress = trc20Event.getFrom();
                    String toAddress = trc20Event.getTo();
                    // 转账金额
                    BigInteger _amount = trc20Event.getValue();
                    if (isERC20 &&
                            contract.equals(erc20) &&
                            (fromAddress.equals(htgContext.MULTY_SIGN_ADDRESS()) || fromAddress.equals(TrxConstant.ZERO_ADDRESS_TRX))) {
                        if (!receive.equals(toAddress)) {
                            logger().warn("提现交易[{}]的接收地址不匹配", txHash);
                            continue;
                        }
                        correctErc20 = true;
                        hasReceiveAddress = true;
                        if (_amount.compareTo(BigInteger.ZERO) > 0) {
                            amount = amount.add(_amount);
                        }
                    }
                }
                // 为主资产提现
                if (eventHash.equals(TrxConstant.EVENT_HASH_TRANSFERFUNDS)) {
                    if (isERC20 || !contract.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                        if (isERC20) {
                            logger().warn("提现交易[{}]的提现类型冲突[0]", txHash);
                        } else {
                            logger().warn("提现交易[{}]的多签合约地址不匹配", txHash);
                        }
                        return false;
                    }
                    String data = Numeric.toHexString(log.getData().toByteArray());
                    List<Object> objects = TrxUtil.parseEvent(data, TrxConstant.EVENT_TRANSFER_FUNDS);
                    String toAddress = objects.get(0).toString();
                    if (!receive.equals(toAddress)) {
                        logger().warn("提现交易[{}]的接收地址不匹配[主资产提现]", txHash);
                        return false;
                    }
                    correctMainAsset = true;
                    hasReceiveAddress = true;
                    // 转账金额
                    BigInteger _amount = (BigInteger) objects.get(1);
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
                po.setTo(receive);
                po.setValue(amount);
                po.setDecimals(htgContext.getConfig().getDecimals());
                po.setAssetId(htgContext.HTG_ASSET_ID());
                return true;
            } else if (correctErc20) {
                po.setIfContractAsset(true);
                po.setContractAddress(erc20);
                po.setTo(receive);
                po.setValue(amount);
                return true;
            }
        }
        logger().warn("提现交易[{}]解析数据缺失", txHash);
        return false;
    }

}

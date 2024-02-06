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
     * Analyze withdrawal transaction data
     */
    public HeterogeneousTransactionInfo parseWithdrawTransaction(Transaction tx, TransactionReceipt txReceipt) throws Exception {
        if (tx == null) {
            logger().warn("The data for parsing transactions does not exist or is incomplete");
            return null;
        }
        String txHash = tx.getHash();
        HeterogeneousTransactionInfo txInfo = HtgUtil.newTransactionInfo(tx, htgContext.NERVE_CHAINID(), this, logger());
        boolean isWithdraw;
        if (tx.getInput().length() < 10) {
            logger().warn("Not a withdrawal transaction[0]");
            return null;
        }
        String methodNameHash = tx.getInput().substring(0, 10);
        // Fixed address for withdrawal transactions
        if (htgListener.isListeningAddress(tx.getTo()) &&
                HtgConstant.METHOD_HASH_CREATEORSIGNWITHDRAW.equals(methodNameHash)) {
            if (txReceipt == null) {
                txReceipt = htgWalletApi.getTxReceipt(txHash);
            }
            // Analyze transaction receipts
            if (htgContext.getConverterCoreApi().isProtocol21()) {
                // protocolv1.21
                isWithdraw = this.newParseWithdrawTxReceiptSinceProtocol21(tx, txReceipt, txInfo);
            } else if (htgContext.getConverterCoreApi().isSupportProtocol13NewValidationOfERC20()) {
                // protocolv1.13
                isWithdraw = this.newParseWithdrawTxReceipt(tx, txReceipt, txInfo);
            } else {
                isWithdraw = this.parseWithdrawTxReceipt(txReceipt, txInfo);
            }
            if (!isWithdraw) {
                logger().warn("Not a withdrawal transaction[1], hash: {}", txHash);
                return null;
            }
            if (txInfo.isIfContractAsset()) {
                htgERC20Helper.loadERC20(txInfo.getContractAddress(), txInfo);
            }
        } else {
            logger().warn("Not a withdrawal transaction[2], hash: {}, txTo: {}, methodNameHash: {}", txHash, tx.getTo(), methodNameHash);
            return null;
        }
        txInfo.setTxType(HeterogeneousChainTxType.WITHDRAW);
        // Parsing multi signature lists
        this.loadSigners(txReceipt, txInfo);
        return txInfo;
    }

    public HeterogeneousTransactionInfo parseWithdrawTransaction(Transaction tx) throws Exception {
        return this.parseWithdrawTransaction(tx, null);
    }

    public HeterogeneousTransactionInfo parseWithdrawTransaction(String txHash) throws Exception {
        Transaction tx = htgWalletApi.getTransactionByHash(txHash);
        if (tx == null) {
            logger().warn("Transaction does not exist");
            return null;
        }
        if (tx.getTo() == null) {
            logger().warn("Not a withdrawal transaction");
            return null;
        }
        tx.setFrom(tx.getFrom().toLowerCase());
        tx.setTo(tx.getTo().toLowerCase());
        return this.parseWithdrawTransaction(tx, null);
    }

    /**
     * Analyze the recharge transaction data of direct transfer methods
     */
    private HeterogeneousTransactionInfo parseDepositTransactionByTransferDirectly(Transaction tx, TransactionReceipt txReceipt) throws Exception {
        if (tx == null) {
            logger().warn("Transaction does not exist");
            return null;
        }
        String txHash = tx.getHash();
        HeterogeneousTransactionInfo txInfo = HtgUtil.newTransactionInfo(tx, htgContext.NERVE_CHAINID(), this, logger());
        boolean isDeposit = false;
        if (txReceipt == null) {
            txReceipt = htgWalletApi.getTxReceipt(txHash);
        }
        do {
            // HTFixed receiving address for recharge transactions,Amount greater than0, absenceinput
            if (htgListener.isListeningAddress(tx.getTo()) &&
                    tx.getValue().compareTo(BigInteger.ZERO) > 0 &&
                    tx.getInput().equals(HtgConstant.HEX_PREFIX)) {
                if(!this.validationEthDeposit(tx, txReceipt)) {
                    logger().error("[{}]Not a recharge transaction[0]", txHash);
                    return null;
                }
                isDeposit = true;
                txInfo.setDecimals(htgContext.getConfig().getDecimals());
                txInfo.setAssetId(htgContext.HTG_ASSET_ID());
                txInfo.setValue(tx.getValue());
                txInfo.setIfContractAsset(false);
                break;
            }
            // ERC20Recharge transaction
            if (htgERC20Helper.isERC20(tx.getTo(), txInfo)) {
                if (htgERC20Helper.hasERC20WithListeningAddress(txReceipt, txInfo, address -> htgListener.isListeningAddress(address))) {
                    // Check if it isNERVEAsset boundERC20If yes, check if the customized item has already been registered in the multi signed contractERC20Otherwise, the recharge will be abnormal
                    if (htgContext.getConverterCoreApi().isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), txInfo.getAssetId())
                            && !isMinterERC20(txInfo.getContractAddress())) {
                        logger().warn("[{}]Illegal{}Online recharge transactions[6], ERC20[{}]BoundNERVEAssets, but not registered in the contract", txHash, htgContext.getConfig().getSymbol(), txInfo.getContractAddress());
                        break;
                    }
                    isDeposit = true;
                    break;
                }
            }
        } while (false);
        if (!isDeposit) {
            logger().error("[{}]Not a recharge transaction[1]", txHash);
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
            logger.error("Analyzing recharge transaction errors, Block height parsing failed, transactionhash: {}, BlockNumberRaw: {}, BlockHash: {}", txHash, tx.getBlockNumberRaw(), tx.getBlockHash());
            TransactionReceipt txReceipt = null;
            try {
                htgWalletApi.refreshCache(txHash);
                txReceipt = htgWalletApi.getTxReceipt(txHash);
                return txReceipt.getBlockNumber();
            } catch (Exception ex) {
                if (txReceipt != null) {
                    logger.error("Re analyze recharge transaction errors, Block height parsing failed, transactionhash: {}, BlockNumberRaw: {}, BlockHash: {}", txHash, txReceipt.getBlockNumberRaw(), txReceipt.getBlockHash());
                } else {
                    logger.error("Re analyze recharge transaction errors, Block height parsing failed, transactionhash: {}, empty txReceipt.", txHash);
                }
                throw ex;
            }
        }
    }

    public HeterogeneousTransactionInfo parseDepositTransaction(Transaction tx) throws Exception {
        HtgInput htInput = this.parseInput(tx.getInput());
        // New recharge transaction method, calling for multi contract signingcrossOutfunction
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
        // New recharge transaction methodsII, calling multiple signed contractscrossOutIIfunction
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
            logger().warn("Transaction does not exist");
            return null;
        }
        if (tx.getTo() == null) {
            logger().warn("Not a recharge transaction");
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
            logger().warn("Transaction does not exist");
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
            logger().warn("Transaction does not exist");
            return false;
        }
        String txHash = tx.getHash();
        if (txReceipt == null) {
            txReceipt = htgWalletApi.getTxReceipt(txHash);
        }
        List<Log> logs = txReceipt.getLogs();
        if (logs == null || logs.isEmpty()) {
            logger().warn("transaction[{}]Event is empty", txHash);
            return false;
        }
        int logSize = logs.size();
        if (logSize == 1) {
            // HTGRecharge transaction
            Log log = logs.get(0);
            List<String> topics = log.getTopics();
            if (log.getTopics().size() == 0) {
                logger().warn("transaction[{}]logunknown", txHash);
                return false;
            }
            String eventHash = topics.get(0);
            if (!HtgConstant.EVENT_HASH_CROSS_OUT_FUNDS.equals(eventHash)) {
                logger().warn("transaction[{}]Event unknown", txHash);
                return false;
            }
            List<Object> depositEvent = HtgUtil.parseEvent(log.getData(), HtgConstant.EVENT_CROSS_OUT_FUNDS);
            if (depositEvent == null && depositEvent.size() != 4) {
                logger().warn("transaction[{}]CrossOutIllegal event data[0]", txHash);
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
            // ERC20Recharge transaction
            List<Object> crossOutInput = HtgUtil.parseInput(tx.getInput(), HtgConstant.INPUT_CROSS_OUT);
            String _to = crossOutInput.get(0).toString();
            BigInteger _amount = new BigInteger(crossOutInput.get(1).toString());
            String _erc20 = crossOutInput.get(2).toString().toLowerCase();
            if (!htgERC20Helper.isERC20(_erc20, po)) {
                logger().warn("erc20[{}]unregistered", _erc20);
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
                        logger().warn("transaction[{}]ofERC20Address mismatch", txHash);
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
                    // Transfer amount
                    BigInteger amount = new BigInteger(v[1], 16);
                    // WhentoAddressyes0x0When, it indicates that this is a destruction from the current multi signed contracterc20Event of
                    if (HtgConstant.ZERO_ADDRESS.equals(toAddress)) {
                        if (!fromAddress.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                            logger().warn("transaction[{}]The destruction address of does not match", txHash);
                            burnEvent = false;
                            break;
                        }
                        if (amount.compareTo(_amount) != 0) {
                            logger().warn("transaction[{}]ofERC20Destruction amount mismatch", txHash);
                            burnEvent = false;
                            break;
                        }
                    } else {
                        // User transfertokenThe event of signing multiple contracts
                        // Must be a user address
                        if (!fromAddress.equals(tx.getFrom())) {
                            logger().warn("transaction[{}]ofERC20User address mismatch", txHash);
                            return false;
                        }
                        // Must be a multiple contract address
                        if (!toAddress.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                            logger().warn("transaction[{}]ofERC20Recharge address mismatch", txHash);
                            return false;
                        }
                        // Does it support transfer and partial destructionERC20
                        if (htgContext.getConverterCoreApi().isSupportProtocol12ERC20OfTransferBurn()) {
                            if (amount.compareTo(_amount) > 0) {
                                logger().warn("transaction[{}]ofERC20Recharge amount mismatch", txHash);
                                return false;
                            }
                            actualAmount = amount;
                        } else {
                            if (amount.compareTo(_amount) != 0) {
                                logger().warn("transaction[{}]ofERC20Recharge amount mismatch", txHash);
                                return false;
                            }
                        }

                        transferEvent = true;
                    }
                }
                if (HtgConstant.EVENT_HASH_CROSS_OUT_FUNDS.equals(eventHash)) {
                    List<Object> depositEvent = HtgUtil.parseEvent(log.getData(), HtgConstant.EVENT_CROSS_OUT_FUNDS);
                    if (depositEvent == null && depositEvent.size() != 4) {
                        logger().warn("transaction[{}]CrossOutIllegal event data[1]", txHash);
                        return false;
                    }
                    String from = depositEvent.get(0).toString();
                    String to = depositEvent.get(1).toString();
                    BigInteger amount = new BigInteger(depositEvent.get(2).toString());
                    String erc20 = depositEvent.get(3).toString();
                    if (!tx.getFrom().equals(from)) {
                        logger().warn("transaction[{}]CrossOutIllegal event data[2]", txHash);
                        return false;
                    }
                    if (!_to.equals(to)) {
                        logger().warn("transaction[{}]CrossOutIllegal event data[3]", txHash);
                        return false;
                    }
                    if (amount.compareTo(_amount) != 0) {
                        logger().warn("transaction[{}]CrossOutIllegal event data[4]", txHash);
                        return false;
                    }
                    if (!_erc20.equals(erc20)) {
                        logger().warn("transaction[{}]CrossOutIllegal event data[5]", txHash);
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
     * Analyze administrator's change of transaction data
     */
    public HeterogeneousTransactionInfo parseManagerChangeTransaction(Transaction tx, TransactionReceipt txReceipt) throws Exception {
        if (tx == null) {
            logger().warn("Transaction does not exist");
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
            logger().warn("Not a change transaction");
            return null;
        }
        txInfo.setTxType(HeterogeneousChainTxType.CHANGE);
        // Parsing multi signature lists
        this.loadSigners(txReceipt, txInfo);
        return txInfo;
    }

    /**
     * Analyzing Contract Upgrade Authorization Transaction Data
     */
    public HeterogeneousTransactionInfo parseUpgradeTransaction(Transaction tx, TransactionReceipt txReceipt) throws Exception {
        if (tx == null) {
            logger().warn("Transaction does not exist");
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
            logger().warn("Not a contract upgrade authorization transaction");
            return null;
        }
        txInfo.setTxType(HeterogeneousChainTxType.UPGRADE);
        // Parsing multi signature lists
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
        Log signerLog = null;
        for (Log log : logs) {
            List<String> topics = log.getTopics();
            if (topics != null && topics.size() > 0) {
                String eventHash = topics.get(0);
                if (HtgConstant.TRANSACTION_COMPLETED_TOPICS.contains(eventHash)) {
                    signerLog = log;
                    break;
                }
            }
        }
        if (signerLog == null) {
            return null;
        }
        String eventHash = signerLog.getTopics().get(0);
        //// PolygonChain, existingerc20In the contract transaction of transfer, an unknown event will appear at the end
        //if (htgContext.getConfig().getChainId() == 106) {
        //    if (HtgConstant.EVENT_HASH_UNKNOWN_ON_POLYGON.equals(eventHash)) {
        //        log = logs.get(logs.size() - 2);
        //        topics = log.getTopics();
        //        eventHash = topics.get(0);
        //    }
        //} else if (htgContext.getConfig().getChainId() == 122) {
        //    // REIChain, existingerc20In the contract transaction of transfer, an unknown event will appear at the end
        //    if (HtgConstant.EVENT_HASH_UNKNOWN_ON_REI.equals(eventHash)) {
        //        log = logs.get(logs.size() - 2);
        //        topics = log.getTopics();
        //        eventHash = topics.get(0);
        //    }
        //}

        // topics Parsing event names, Event triggered upon signature completion
        // Parse event data to obtain a list of successful transaction event data
        List<Object> eventResult = null;
        switch (eventHash) {
            case HtgConstant.EVENT_HASH_TRANSACTION_WITHDRAW_COMPLETED:
                eventResult = HtgUtil.parseEvent(signerLog.getData(), HtgConstant.EVENT_TRANSACTION_WITHDRAW_COMPLETED);
                break;
            case HtgConstant.EVENT_HASH_TRANSACTION_MANAGER_CHANGE_COMPLETED:
                eventResult = HtgUtil.parseEvent(signerLog.getData(), HtgConstant.EVENT_TRANSACTION_MANAGER_CHANGE_COMPLETED);
                break;
            case HtgConstant.EVENT_HASH_TRANSACTION_UPGRADE_COMPLETED:
                eventResult = HtgUtil.parseEvent(signerLog.getData(), HtgConstant.EVENT_TRANSACTION_UPGRADE_COMPLETED);
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
                // byERC20Withdrawal
                if (topics.get(0).equals(HtgConstant.EVENT_HASH_ERC20_TRANSFER)) {
                    String toAddress = HtgConstant.HEX_PREFIX + topics.get(2).substring(26, topics.get(1).length()).toString();
                    String data;
                    if (topics.size() == 3) {
                        data = log.getData();
                    } else {
                        data = topics.get(3);
                    }
                    String[] v = data.split("x");
                    // Transfer amount
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
                // byHTWithdrawal
                if (topics.get(0).equals(HtgConstant.EVENT_HASH_TRANSFERFUNDS)) {
                    String data = log.getData();
                    String to = HtgConstant.HEX_PREFIX + data.substring(26, 66);
                    String amountStr = data.substring(66, 130);
                    // Transfer amount
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
            logger().warn("Transaction does not exist");
            return false;
        }
        String txHash = tx.getHash();
        if (txReceipt == null) {
            txReceipt = htgWalletApi.getTxReceipt(txHash);
        }
        List<Log> logs = txReceipt.getLogs();
        if (logs == null || logs.isEmpty()) {
            logger().warn("transaction[{}]Event is empty", txHash);
            return false;
        }
        List<Object> crossOutInput = HtgUtil.parseInput(tx.getInput(), HtgConstant.INPUT_CROSS_OUT);
        String _to = crossOutInput.get(0).toString();
        BigInteger _amount = new BigInteger(crossOutInput.get(1).toString());
        String _erc20 = crossOutInput.get(2).toString().toLowerCase();
        if (HtgConstant.ZERO_ADDRESS.equals(_erc20)) {
            // Main asset recharge transaction
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
                    logger().warn("transaction[{}]CrossOutIllegal event data[0]", txHash);
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
            logger().warn("transaction[{}]The main asset of[{}]Recharge event mismatch", txHash, htgContext.getConfig().getSymbol());
            return false;
        } else {
            // ERC20Recharge transaction
            if (!htgERC20Helper.isERC20(_erc20, po)) {
                logger().warn("erc20[{}]unregistered", _erc20);
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
                    // Transfer amount
                    BigInteger amount = new BigInteger(v[1], 16);
                    // WhentoAddressyes0x0When, it indicates that this is a destruction from the current multi signed contracterc20oftransferevent
                    if (HtgConstant.ZERO_ADDRESS.equals(toAddress)) {
                        if (!fromAddress.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                            continue;
                        }
                        if (amount.compareTo(_amount) != 0) {
                            logger().warn("transaction[{}]ofERC20Destruction amount mismatch", txHash);
                            burnEvent = false;
                            break;
                        }
                    } else {
                        // User transfertokenThe event of signing multiple contracts
                        // Must be a multiple contract address
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
                        logger().warn("transaction[{}]CrossOutIllegal event data[1]", txHash);
                        return false;
                    }
                    String from = depositEvent.get(0).toString();
                    String to = depositEvent.get(1).toString();
                    BigInteger amount = new BigInteger(depositEvent.get(2).toString());
                    String erc20 = depositEvent.get(3).toString();
                    if (!tx.getFrom().equals(from)) {
                        logger().warn("transaction[{}]CrossOutIllegal event data[2]", txHash);
                        return false;
                    }
                    if (!_to.equals(to)) {
                        logger().warn("transaction[{}]CrossOutIllegal event data[3]", txHash);
                        return false;
                    }
                    if (amount.compareTo(_amount) != 0) {
                        logger().warn("transaction[{}]CrossOutIllegal event data[4]", txHash);
                        return false;
                    }
                    if (!_erc20.equals(erc20)) {
                        logger().warn("transaction[{}]CrossOutIllegal event data[5]", txHash);
                        return false;
                    }
                    crossOutEvent = true;
                }
            }
            if (transferEvent && burnEvent && crossOutEvent) {
                if (calcAmount.compareTo(_amount) > 0) {
                    logger().warn("transaction[{}]ofERC20Recharge amount mismatch", txHash);
                    return false;
                }
                actualAmount = calcAmount;
                if (actualAmount.equals(BigInteger.ZERO)) {
                    logger().warn("transaction[{}]ofERC20The recharge amount is0", txHash);
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
                logger().warn("transaction[{}]ofERC20Recharge event mismatch, transferEvent: {}, burnEvent: {}, crossOutEvent: {}",
                        txHash, transferEvent, burnEvent, crossOutEvent);
                return false;
            }
        }
    }

    private boolean newValidationEthDepositByCrossOut(Transaction tx, TransactionReceipt txReceipt, HeterogeneousTransactionInfo po) throws Exception {
        if (tx == null) {
            logger().warn("Transaction does not exist");
            return false;
        }
        String txHash = tx.getHash();
        if (txReceipt == null) {
            txReceipt = htgWalletApi.getTxReceipt(txHash);
        }
        List<Log> logs = txReceipt.getLogs();
        if (logs == null || logs.isEmpty()) {
            logger().warn("transaction[{}]Event is empty", txHash);
            return false;
        }
        List<Object> crossOutInput = HtgUtil.parseInput(tx.getInput(), HtgConstant.INPUT_CROSS_OUT);
        String _to = crossOutInput.get(0).toString();
        BigInteger _amount = new BigInteger(crossOutInput.get(1).toString());
        String _erc20 = crossOutInput.get(2).toString().toLowerCase();
        if (HtgConstant.ZERO_ADDRESS.equals(_erc20)) {
            // Main asset recharge transaction
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
                    logger().warn("transaction[{}]CrossOutIllegal event data[0]", txHash);
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
            logger().warn("transaction[{}]The main asset of[{}]Recharge event mismatch", txHash, htgContext.getConfig().getSymbol());
            return false;
        } else {
            // ERC20Recharge transaction
            if (!htgERC20Helper.isERC20(_erc20, po)) {
                logger().warn("erc20[{}]unregistered", _erc20);
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
                    // Transfer amount
                    BigInteger amount = new BigInteger(v[1], 16);
                    // WhentoAddressyes0x0When, it indicates that this is a destruction from the current multi signed contracterc20oftransferevent
                    if (HtgConstant.ZERO_ADDRESS.equals(toAddress)) {
                        if (!fromAddress.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                            logger().warn("transaction[{}]The destruction address of does not match", txHash);
                            burnEvent = false;
                            break;
                        }
                        if (amount.compareTo(_amount) != 0) {
                            logger().warn("transaction[{}]ofERC20Destruction amount mismatch", txHash);
                            burnEvent = false;
                            break;
                        }
                    } else {
                        // User transfertokenThe event of signing multiple contracts
                        // Must be a multiple contract address
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
                        logger().warn("transaction[{}]CrossOutIllegal event data[1]", txHash);
                        return false;
                    }
                    String from = depositEvent.get(0).toString();
                    String to = depositEvent.get(1).toString();
                    BigInteger amount = new BigInteger(depositEvent.get(2).toString());
                    String erc20 = depositEvent.get(3).toString();
                    if (!tx.getFrom().equals(from)) {
                        logger().warn("transaction[{}]CrossOutIllegal event data[2]", txHash);
                        return false;
                    }
                    if (!_to.equals(to)) {
                        logger().warn("transaction[{}]CrossOutIllegal event data[3]", txHash);
                        return false;
                    }
                    if (amount.compareTo(_amount) != 0) {
                        logger().warn("transaction[{}]CrossOutIllegal event data[4]", txHash);
                        return false;
                    }
                    if (!_erc20.equals(erc20)) {
                        logger().warn("transaction[{}]CrossOutIllegal event data[5]", txHash);
                        return false;
                    }
                    crossOutEvent = true;
                }
            }
            if (transferEvent && burnEvent && crossOutEvent) {
                if (calcAmount.compareTo(_amount) > 0) {
                    logger().warn("transaction[{}]ofERC20Recharge amount mismatch", txHash);
                    return false;
                }
                actualAmount = calcAmount;
                if (actualAmount.equals(BigInteger.ZERO)) {
                    logger().warn("transaction[{}]ofERC20The recharge amount is0", txHash);
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
                logger().warn("transaction[{}]ofERC20Recharge event mismatch, transferEvent: {}, burnEvent: {}, crossOutEvent: {}",
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
            // Transfer amount
            BigInteger amount = BigInteger.ZERO;
            for (Log log : logs) {
                topics = log.getTopics();
                if (topics.size() == 0) {
                    continue;
                }
                eventHash = topics.get(0);
                contract = log.getAddress().toLowerCase();
                // byERC20Withdrawal
                if (eventHash.equals(HtgConstant.EVENT_HASH_ERC20_TRANSFER)) {
                    int length = topics.get(1).length();
                    String fromAddress = HtgConstant.HEX_PREFIX + topics.get(1).substring(26, length).toString();
                    if (isERC20 &&
                            contract.equalsIgnoreCase(erc20) &&
                            (fromAddress.equalsIgnoreCase(htgContext.MULTY_SIGN_ADDRESS()) || fromAddress.equalsIgnoreCase(HtgConstant.ZERO_ADDRESS))) {
                        String toAddress = HtgConstant.HEX_PREFIX + topics.get(2).substring(26, length).toString();
                        if (!receive.equalsIgnoreCase(toAddress)) {
                            logger().warn("Withdrawal transactions[{}]The receiving address of does not match", txHash);
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
                        // Transfer amount
                        BigInteger _amount = new BigInteger(v[1], 16);
                        if (_amount.compareTo(BigInteger.ZERO) > 0) {
                            amount = amount.add(_amount);
                        }
                    }
                }
                // Withdrawal of main assets
                if (eventHash.equals(HtgConstant.EVENT_HASH_TRANSFERFUNDS)) {
                    if (isERC20 || !contract.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                        if (isERC20) {
                            logger().warn("Withdrawal transactions[{}]Conflict in withdrawal type[0]", txHash);
                        } else {
                            logger().warn("Withdrawal transactions[{}]The address of the multiple signed contracts does not match", txHash);
                        }
                        return false;
                    }
                    correctMainAsset = true;
                    String data = log.getData();
                    String toAddress = HtgConstant.HEX_PREFIX + data.substring(26, 66);
                    if (!receive.equalsIgnoreCase(toAddress)) {
                        logger().warn("Withdrawal transactions[{}]The receiving address of does not match[Withdrawal of main assets]", txHash);
                        return false;
                    }
                    String amountStr = data.substring(66, 130);
                    // Transfer amount
                    BigInteger _amount = new BigInteger(amountStr, 16);
                    if (_amount.compareTo(BigInteger.ZERO) > 0) {
                        amount = amount.add(_amount);
                    }
                }
            }
            if (correctErc20 && correctMainAsset) {
                logger().warn("Withdrawal transactions[{}]Conflict in withdrawal type[1]", txHash);
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
        logger().warn("Withdrawal transactions[{}]Missing parsing data", txHash);
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
            // Transfer amount
            BigInteger amount = BigInteger.ZERO;
            for (Log log : logs) {
                topics = log.getTopics();
                if (topics.size() == 0) {
                    continue;
                }
                eventHash = topics.get(0);
                contract = log.getAddress().toLowerCase();
                // byERC20Withdrawal
                if (eventHash.equals(HtgConstant.EVENT_HASH_ERC20_TRANSFER)) {
                    int length = topics.get(1).length();
                    String fromAddress = HtgConstant.HEX_PREFIX + topics.get(1).substring(26, length);
                    if (isERC20 &&
                            contract.equalsIgnoreCase(erc20) &&
                            (fromAddress.equalsIgnoreCase(htgContext.MULTY_SIGN_ADDRESS()) || fromAddress.equalsIgnoreCase(HtgConstant.ZERO_ADDRESS))) {
                        String toAddress = HtgConstant.HEX_PREFIX + topics.get(2).substring(26, length);
                        if (!receive.equalsIgnoreCase(toAddress)) {
                            logger().warn("Withdrawal transactions[{}]The receiving address of does not match", txHash);
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
                        // Transfer amount
                        BigInteger _amount = new BigInteger(v[1], 16);
                        if (_amount.compareTo(BigInteger.ZERO) > 0) {
                            amount = amount.add(_amount);
                        }
                    }
                }
                // Withdrawal of main assets
                if (eventHash.equals(HtgConstant.EVENT_HASH_TRANSFERFUNDS)) {
                    if (isERC20 || !contract.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                        if (isERC20) {
                            logger().warn("Withdrawal transactions[{}]Conflict in withdrawal type[0]", txHash);
                        } else {
                            logger().warn("Withdrawal transactions[{}]The address of the multiple signed contracts does not match", txHash);
                        }
                        return false;
                    }
                    String data = log.getData();
                    String toAddress = HtgConstant.HEX_PREFIX + data.substring(26, 66);
                    if (!receive.equalsIgnoreCase(toAddress)) {
                        logger().warn("Withdrawal transactions[{}]The receiving address of does not match[Withdrawal of main assets]", txHash);
                        return false;
                    }
                    correctMainAsset = true;
                    hasReceiveAddress = true;
                    String amountStr = data.substring(66, 130);
                    // Transfer amount
                    BigInteger _amount = new BigInteger(amountStr, 16);
                    if (_amount.compareTo(BigInteger.ZERO) > 0) {
                        amount = amount.add(_amount);
                    }
                }
            }
            if (!hasReceiveAddress) {
                logger().warn("Withdrawal transactions[{}]The receiving address of does not match", txHash);
                return false;
            }
            if (correctErc20 && correctMainAsset) {
                logger().warn("Withdrawal transactions[{}]Conflict in withdrawal type[1]", txHash);
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
        logger().warn("Withdrawal transactions[{}]Missing parsing data", txHash);
        return false;
    }

    public boolean validationEthDepositByCrossOutII(Transaction tx, TransactionReceipt txReceipt, HeterogeneousTransactionInfo po) throws Exception {
        if (tx == null) {
            logger().warn("Transaction does not exist");
            return false;
        }
        String txHash = tx.getHash();
        if (txReceipt == null) {
            txReceipt = htgWalletApi.getTxReceipt(txHash);
        }
        List<Log> logs = txReceipt.getLogs();
        if (logs == null || logs.isEmpty()) {
            logger().warn("transaction[{}]Event is empty", txHash);
            return false;
        }
        List<Object> crossOutInput = HtgUtil.parseInput(tx.getInput(), HtgConstant.INPUT_CROSS_OUT_II);
        String _to = crossOutInput.get(0).toString();
        BigInteger _amount = new BigInteger(crossOutInput.get(1).toString());
        String _erc20 = crossOutInput.get(2).toString().toLowerCase();
        if (HtgConstant.ZERO_ADDRESS.equals(_erc20)) {
            // Main asset recharge transaction
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
                    logger().warn("transaction[{}]CrossOutIIIllegal event data[0]", txHash);
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
            logger().warn("transaction[{}]The main asset of[{}]RechargeIIEvent mismatch", txHash, htgContext.getConfig().getSymbol());
            return false;
        } else {
            // ERC20Recharge and main asset recharge
            if (!htgERC20Helper.isERC20(_erc20, po)) {
                logger().warn("CrossOutII: erc20[{}]unregistered", _erc20);
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
                    // Transfer amount
                    BigInteger amount = new BigInteger(v[1], 16);
                    // WhentoAddressyes0x0When, it indicates that this is a destruction from the current multi signed contracterc20oftransferevent
                    if (HtgConstant.ZERO_ADDRESS.equals(toAddress)) {
                        if (!fromAddress.equals(htgContext.MULTY_SIGN_ADDRESS())) {
                            continue;
                        }
                        if (amount.compareTo(_amount) != 0) {
                            logger().warn("CrossOutII: transaction[{}]ofERC20Destruction amount mismatch", txHash);
                            burnEvent = false;
                            break;
                        }
                    } else {
                        // User transfertokenThe event of signing multiple contracts
                        // Must be a multiple contract address
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
                        logger().warn("transaction[{}]CrossOutIIIllegal event data[1]", txHash);
                        return false;
                    }
                    String from = depositEvent.get(0).toString();
                    String to = depositEvent.get(1).toString();
                    BigInteger amount = new BigInteger(depositEvent.get(2).toString());
                    String erc20 = depositEvent.get(3).toString();
                    BigInteger ethAmount = new BigInteger(depositEvent.get(4).toString());
                    String extend = Numeric.toHexString((byte[]) depositEvent.get(5));
                    if (!tx.getFrom().equals(from)) {
                        logger().warn("transaction[{}]CrossOutIIIllegal event data[2]", txHash);
                        return false;
                    }
                    if (!_to.equals(to)) {
                        logger().warn("transaction[{}]CrossOutIIIllegal event data[3]", txHash);
                        return false;
                    }
                    if (amount.compareTo(_amount) != 0) {
                        logger().warn("transaction[{}]CrossOutIIIllegal event data[4]", txHash);
                        return false;
                    }
                    if (!_erc20.equals(erc20)) {
                        logger().warn("transaction[{}]CrossOutIIIllegal event data[5]", txHash);
                        return false;
                    }
                    if (tx.getFrom().equals(from) && tx.getValue().compareTo(BigInteger.ZERO) > 0 && tx.getValue().compareTo(ethAmount) == 0) {
                        // Record main asset recharge
                        po.setDepositIIMainAsset(ethAmount, htgContext.getConfig().getDecimals(), htgContext.HTG_ASSET_ID());
                    }
                    po.setDepositIIExtend(extend);
                    crossOutEvent = true;
                }
            }
            if (transferEvent && burnEvent && crossOutEvent) {
                if (erc20Amount.compareTo(_amount) > 0) {
                    logger().warn("CrossOutII: transaction[{}]ofERC20Recharge amount mismatch", txHash);
                    return false;
                }
                if (erc20Amount.equals(BigInteger.ZERO)) {
                    logger().warn("CrossOutII: transaction[{}]ofERC20The recharge amount is0", txHash);
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
                logger().warn("transaction[{}]ofERC20RechargeIIEvent mismatch, transferEvent: {}, burnEvent: {}, crossOutEvent: {}",
                        txHash, transferEvent, burnEvent, crossOutEvent);
                return false;
            }
        }
    }
}

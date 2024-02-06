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
package network.nerve.converter.heterogeneouschain.eth.helper;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import network.nerve.converter.heterogeneouschain.eth.listener.EthListener;
import network.nerve.converter.heterogeneouschain.eth.storage.EthTxStorageService;
import network.nerve.converter.heterogeneouschain.eth.utils.EthUtil;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Mimi
 * @date: 2020-03-20
 */
@Component
public class EthParseTxHelper {

    @Autowired
    private EthERC20Helper ethERC20Helper;
    @Autowired
    private EthTxStorageService ethTxStorageService;
    @Autowired
    private ETHWalletApi ethWalletApi;
    @Autowired
    private EthListener ethListener;

    public boolean isCompletedTransaction(String nerveTxHash) throws Exception {
        return isCompletedTransactionByStatus(nerveTxHash, false);
    }

    public boolean isCompletedTransactionByLatest(String nerveTxHash) throws Exception {
        return isCompletedTransactionByStatus(nerveTxHash, true);
    }

    private boolean isCompletedTransactionByStatus(String nerveTxHash, boolean latest) throws Exception {
        Function isCompletedFunction = EthUtil.getIsCompletedFunction(nerveTxHash);
        List<Type> valueTypes = ethWalletApi.callViewFunction(EthContext.MULTY_SIGN_ADDRESS, isCompletedFunction, latest);
        boolean isCompleted = Boolean.parseBoolean(valueTypes.get(0).getValue().toString());
        return isCompleted;
    }
    /**
     * Analyze withdrawal transaction data
     */
    public HeterogeneousTransactionInfo parseWithdrawTransaction(Transaction tx, TransactionReceipt txReceipt) throws Exception {
        if (tx == null) {
            EthContext.logger().warn("The data for parsing transactions does not exist or is incomplete");
            return null;
        }
        String txHash = tx.getHash();
        HeterogeneousTransactionInfo txInfo = EthUtil.newTransactionInfo(tx);
        boolean isWithdraw;
        if (tx.getInput().length() < 10) {
            EthContext.logger().warn("Not a withdrawal transaction[0]");
            return null;
        }
        String methodNameHash = tx.getInput().substring(0, 10);
        // Fixed address for withdrawal transactions
        if (ethListener.isListeningAddress(tx.getTo()) &&
                EthConstant.METHOD_HASH_CREATEORSIGNWITHDRAW.equals(methodNameHash)) {
            if (txReceipt == null) {
                txReceipt = ethWalletApi.getTxReceipt(txHash);
            }
            isWithdraw = this.parseWithdrawTxReceipt(txReceipt, txInfo);
            if (!isWithdraw) {
                EthContext.logger().warn("Not a final withdrawal transaction[1], hash: {}", txHash);
                return null;
            }
            if (txInfo.isIfContractAsset()) {
                ethERC20Helper.loadERC20(txInfo.getContractAddress(), txInfo);
            }
        } else {
            EthContext.logger().warn("Not a final withdrawal transaction[2], hash: {}", txHash);
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
        Transaction tx = ethWalletApi.getTransactionByHash(txHash);
        if (tx == null) {
            EthContext.logger().warn("Transaction does not exist");
            return null;
        }
        if (tx.getTo() == null) {
            EthContext.logger().warn("Not a withdrawal transaction");
            return null;
        }
        tx.setFrom(tx.getFrom().toLowerCase());
        tx.setTo(tx.getTo().toLowerCase());
        return this.parseWithdrawTransaction(tx, null);
    }

    /**
     * Analyzing recharge transaction data
     */
    public HeterogeneousTransactionInfo parseDepositTransaction(Transaction tx, TransactionReceipt txReceipt) throws Exception {
        if (tx == null) {
            EthContext.logger().warn("Transaction does not exist");
            return null;
        }
        String txHash = tx.getHash();
        HeterogeneousTransactionInfo txInfo = EthUtil.newTransactionInfo(tx);
        boolean isDeposit = false;
        if (txReceipt == null) {
            txReceipt = ethWalletApi.getTxReceipt(txHash);
        }
        do {
            // ETHFixed receiving address for recharge transactions,Amount greater than0, absenceinput
            if (ethListener.isListeningAddress(tx.getTo()) &&
                    tx.getValue().compareTo(BigInteger.ZERO) > 0 &&
                    tx.getInput().equals(EthConstant.HEX_PREFIX)) {
                if(!this.validationEthDeposit(tx, txReceipt)) {
                    EthContext.logger().error("[{}]Not a recharge transaction[0]", txHash);
                    return null;
                }
                isDeposit = true;
                txInfo.setDecimals(EthConstant.ETH_DECIMALS);
                txInfo.setAssetId(EthConstant.ETH_ASSET_ID);
                txInfo.setValue(tx.getValue());
                txInfo.setIfContractAsset(false);
                break;
            }
            // ERC20Recharge transaction
            if (ethERC20Helper.isERC20(tx.getTo(), txInfo)) {
                if (ethERC20Helper.hasERC20WithListeningAddress(txReceipt, txInfo, address -> ethListener.isListeningAddress(address))) {
                    isDeposit = true;
                    break;
                }
            }
        } while (false);
        if (!isDeposit) {
            EthContext.logger().error("[{}]Not a recharge transaction[1]", txHash);
            return null;
        }
        txInfo.setTxType(HeterogeneousChainTxType.DEPOSIT);
        return txInfo;
    }

    public HeterogeneousTransactionInfo parseDepositTransaction(Transaction tx) throws Exception {
        return this.parseDepositTransaction(tx, null);
    }

    public HeterogeneousTransactionInfo parseDepositTransaction(String txHash) throws Exception {
        Transaction tx = ethWalletApi.getTransactionByHash(txHash);
        if (tx == null) {
            EthContext.logger().warn("Transaction does not exist");
            return null;
        }
        if (tx.getTo() == null) {
            EthContext.logger().warn("Not a recharge transaction");
            return null;
        }
        tx.setFrom(tx.getFrom().toLowerCase());
        tx.setTo(tx.getTo().toLowerCase());
        return this.parseDepositTransaction(tx, null);
    }

    public boolean validationEthDeposit(Transaction tx) throws Exception {
        return this.validationEthDeposit(tx, null);
    }

    private boolean validationEthDeposit(Transaction tx, TransactionReceipt txReceipt) throws Exception {
        if (tx == null) {
            EthContext.logger().warn("Transaction does not exist");
            return false;
        }
        String txHash = tx.getHash();
        if (txReceipt == null) {
            txReceipt = ethWalletApi.getTxReceipt(txHash);
        }
        List<Log> logs = txReceipt.getLogs();
        if (logs == null || logs.isEmpty()) {
            return false;
        }
        for (Log log : logs) {
            List<String> topics = log.getTopics();
            if (topics.size() == 0) {
                continue;
            }
            String eventHash = topics.get(0);
            if (!EthConstant.EVENT_HASH_ETH_DEPOSIT_FUNDS.equals(eventHash)) {
                continue;
            }
            List<Object> depositEvent = EthUtil.parseEvent(log.getData(), EthConstant.EVENT_DEPOSIT_FUNDS);
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

    /**
     * Analyze administrator's change of transaction data
     */
    public HeterogeneousTransactionInfo parseManagerChangeTransaction(Transaction tx, TransactionReceipt txReceipt) {
        if (tx == null) {
            EthContext.logger().warn("Transaction does not exist");
            return null;
        }
        HeterogeneousTransactionInfo txInfo = EthUtil.newTransactionInfo(tx);
        boolean isChange = false;
        String input, methodHash;
        if (ethListener.isListeningAddress(tx.getTo()) && (input = tx.getInput()).length() >= 10) {
            methodHash = input.substring(0, 10);
            if (methodHash.equals(EthConstant.METHOD_HASH_CREATEORSIGNMANAGERCHANGE)) {
                isChange = true;
                List<Object> inputData = EthUtil.parseInput(input, EthConstant.INPUT_CHANGE);
                List<Address> adds = (List<Address>) inputData.get(1);
                List<Address> quits = (List<Address>) inputData.get(2);
                BigInteger orginTxCount = (BigInteger) inputData.get(3);
                if (!adds.isEmpty()) {
                    txInfo.setAddAddresses(EthUtil.list2array(adds.stream().map(a -> a.getValue()).collect(Collectors.toList())));
                }
                if (!quits.isEmpty()) {
                    txInfo.setRemoveAddresses(EthUtil.list2array(quits.stream().map(q -> q.getValue()).collect(Collectors.toList())));
                }
                if (orginTxCount != null) {
                    txInfo.setOrginTxCount(orginTxCount.intValue());
                }
            }
        }

        if (!isChange) {
            EthContext.logger().warn("Not a change transaction");
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
    public HeterogeneousTransactionInfo parseUpgradeTransaction(Transaction tx, TransactionReceipt txReceipt) {
        if (tx == null) {
            EthContext.logger().warn("Transaction does not exist");
            return null;
        }
        HeterogeneousTransactionInfo txInfo = EthUtil.newTransactionInfo(tx);
        boolean isUpgrade = false;
        String input, methodHash;
        if (ethListener.isListeningAddress(tx.getTo()) && (input = tx.getInput()).length() >= 10) {
            methodHash = input.substring(0, 10);
            if (methodHash.equals(EthConstant.METHOD_HASH_CREATEORSIGNUPGRADE)) {
                isUpgrade = true;
            }
        }
        if (!isUpgrade) {
            EthContext.logger().warn("Not a contract upgrade authorization transaction");
            return null;
        }
        txInfo.setTxType(HeterogeneousChainTxType.UPGRADE);
        // Parsing multi signature lists
        this.loadSigners(txReceipt, txInfo);
        return txInfo;
    }

    public List<HeterogeneousAddress> parseSigners(TransactionReceipt txReceipt) {
        List<Object> eventResult = this.loadSignersFromEvent(txReceipt);
        if (eventResult == null || eventResult.isEmpty()) {
            return null;
        }
        List<String> addressList = (List<String>) ((List) eventResult.get(0)).stream().map(address -> ((Address) address).getValue()).collect(Collectors.toList());
        List<HeterogeneousAddress> signers = addressList.stream().map(address -> new HeterogeneousAddress(EthConstant.ETH_CHAIN_ID, address)).collect(Collectors.toList());
        return signers;
    }

    private void loadSigners(TransactionReceipt txReceipt, HeterogeneousTransactionInfo txInfo) {
        List<Object> eventResult = this.loadSignersFromEvent(txReceipt);
        if (eventResult != null && !eventResult.isEmpty()) {
            txInfo.setNerveTxHash(eventResult.get(eventResult.size() - 1).toString());
            List<String> addressList = (List<String>) ((List) eventResult.get(0)).stream().map(address -> ((Address) address).getValue()).collect(Collectors.toList());
            List<HeterogeneousAddress> signers = addressList.stream().map(address -> new HeterogeneousAddress(EthConstant.ETH_CHAIN_ID, address)).collect(Collectors.toList());
            txInfo.setSigners(signers);
        }
    }

    private List<Object> loadSignersFromEvent(TransactionReceipt txReceipt) {
        List<Log> logs = txReceipt.getLogs();
        if (logs == null || logs.isEmpty()) {
            return null;
        }
        Log log = logs.get(logs.size() - 1);
        List<String> topics = log.getTopics();
        if (topics.size() == 0) {
            return null;
        }
        String eventHash = topics.get(0);
        // topics Parsing event names, Event triggered upon signature completion
        // Parse event data to obtain a signature list for transactions
        List<Object> eventResult = null;
        switch (eventHash) {
            case EthConstant.EVENT_HASH_TRANSACTION_WITHDRAW_COMPLETED:
                eventResult = EthUtil.parseEvent(log.getData(), EthConstant.EVENT_TRANSACTION_WITHDRAW_COMPLETED);
                break;
            case EthConstant.EVENT_HASH_TRANSACTION_MANAGER_CHANGE_COMPLETED:
                eventResult = EthUtil.parseEvent(log.getData(), EthConstant.EVENT_TRANSACTION_MANAGER_CHANGE_COMPLETED);
                break;
            case EthConstant.EVENT_HASH_TRANSACTION_UPGRADE_COMPLETED:
                eventResult = EthUtil.parseEvent(log.getData(), EthConstant.EVENT_TRANSACTION_UPGRADE_COMPLETED);
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
                if (topics.get(0).equals(EthConstant.EVENT_HASH_ERC20_TRANSFER)) {
                    String toAddress = EthConstant.HEX_PREFIX + topics.get(2).substring(26, topics.get(1).length()).toString();
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
                // byETHWithdrawal
                if (topics.get(0).equals(EthConstant.EVENT_HASH_TRANSFERFUNDS)) {
                    String data = log.getData();
                    String to = EthConstant.HEX_PREFIX + data.substring(26, 66);
                    String amountStr = data.substring(66, 130);
                    // Transfer amount
                    BigInteger amount = new BigInteger(amountStr, 16);
                    if (amount.compareTo(BigInteger.ZERO) > 0) {
                        po.setTo(to.toLowerCase());
                        po.setValue(amount);
                        po.setDecimals(EthConstant.ETH_DECIMALS);
                        po.setAssetId(EthConstant.ETH_ASSET_ID);
                        return true;
                    }
                    return false;
                }
            }
        }
        return false;
    }

}

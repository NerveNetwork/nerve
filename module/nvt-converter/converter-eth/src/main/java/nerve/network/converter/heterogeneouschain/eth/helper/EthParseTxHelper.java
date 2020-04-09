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
package nerve.network.converter.heterogeneouschain.eth.helper;

import nerve.network.converter.enums.HeterogeneousChainTxType;
import nerve.network.converter.heterogeneouschain.eth.constant.EthConstant;
import nerve.network.converter.heterogeneouschain.eth.context.EthContext;
import nerve.network.converter.heterogeneouschain.eth.core.ETHWalletApi;
import nerve.network.converter.heterogeneouschain.eth.storage.EthTxStorageService;
import nerve.network.converter.heterogeneouschain.eth.utils.EthUtil;
import nerve.network.converter.model.bo.HeterogeneousAddress;
import nerve.network.converter.model.bo.HeterogeneousTransactionBaseInfo;
import nerve.network.converter.model.bo.HeterogeneousTransactionInfo;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import org.web3j.abi.datatypes.Address;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

import static nerve.network.converter.heterogeneouschain.eth.constant.EthConstant.METHOD_HASH_CREATEORSIGNWITHDRAW;
import static nerve.network.converter.heterogeneouschain.eth.context.EthContext.logger;

/**
 * @author: Chino
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

    /**
     * 解析提现交易数据
     */
    public HeterogeneousTransactionInfo parseWithdrawTransaction(Transaction tx, TransactionReceipt txReceipt) throws Exception {
        if (tx == null) {
            logger().warn("解析交易的数据不存在或不完整");
            return null;
        }
        String txHash = tx.getHash();
        HeterogeneousTransactionInfo txInfo = EthUtil.newTransactionInfo(tx);
        boolean isWithdraw;
        if (tx.getInput().length() < 10) {
            logger().warn("不是提现交易[0]");
            return null;
        }
        String methodNameHash = tx.getInput().substring(0, 10);
        // 提现交易的固定地址
        if (EthContext.MULTY_SIGN_ADDRESS_HISTORY_SET.contains(tx.getTo()) &&
                METHOD_HASH_CREATEORSIGNWITHDRAW.equals(methodNameHash)) {
            if(txReceipt == null) {
                txReceipt = ethWalletApi.getTxReceipt(txHash);
            }
            isWithdraw = this.parseWithdrawTxReceipt(txReceipt, txInfo);
            if (!isWithdraw) {
                logger().warn("不是提现交易[1]");
                return null;
            }
            if (txInfo.isIfContractAsset()) {
                ethERC20Helper.loadERC20(txInfo.getContractAddress(), txInfo);
            }
        } else {
            logger().warn("不是提现交易[2]");
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
        Transaction tx = ethWalletApi.getTransactionByHash(txHash);
        if (tx == null) {
            logger().warn("交易不存在");
            return null;
        }
        if(tx.getTo() == null) {
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
    public HeterogeneousTransactionInfo parseDepositTransaction(Transaction tx, TransactionReceipt txReceipt) throws Exception {
        if (tx == null) {
            logger().warn("交易不存在");
            return null;
        }
        String txHash = tx.getHash();
        HeterogeneousTransactionInfo txInfo = EthUtil.newTransactionInfo(tx);
        boolean isDeposit = false;
        do {
            // ETH充值交易的固定接收地址,金额大于0, 没有input
            if (EthContext.MULTY_SIGN_ADDRESS_HISTORY_SET.contains(tx.getTo()) &&
                    tx.getValue().compareTo(BigInteger.ZERO) > 0 &&
                    tx.getInput().equals(EthConstant.HEX_PREFIX)) {
                isDeposit = true;
                txInfo.setDecimals(EthConstant.ETH_DECIMALS);
                txInfo.setAssetId(EthConstant.ETH_ASSET_ID);
                txInfo.setValue(tx.getValue());
                txInfo.setIfContractAsset(false);
                break;
            }
            // ERC20充值交易
            if (ethERC20Helper.isERC20(tx.getTo(), txInfo)) {
                if(txReceipt == null) {
                    txReceipt = ethWalletApi.getTxReceipt(txHash);
                }
                if (ethERC20Helper.hasERC20WithListeningAddress(txReceipt, txInfo, address -> EthContext.MULTY_SIGN_ADDRESS_HISTORY_SET.contains(address))) {
                    isDeposit = true;
                    break;
                }
            }
        } while (false);
        if (!isDeposit) {
            logger().debug("不是充值交易");
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
            logger().warn("交易不存在");
            return null;
        }
        if(tx.getTo() == null) {
            logger().warn("不是充值交易");
            return null;
        }
        tx.setFrom(tx.getFrom().toLowerCase());
        tx.setTo(tx.getTo().toLowerCase());
        return this.parseDepositTransaction(tx, null);
    }

    /**
     * 解析管理员变更交易数据
     */
    public HeterogeneousTransactionInfo parseManagerChangeTransaction(Transaction tx, TransactionReceipt txReceipt) {
        if (tx == null) {
            logger().warn("交易不存在");
            return null;
        }
        HeterogeneousTransactionInfo txInfo = EthUtil.newTransactionInfo(tx);
        boolean isChange = false;
        String input, methodHash;
        if (EthContext.MULTY_SIGN_ADDRESS_HISTORY_SET.contains(tx.getTo()) && (input = tx.getInput()).length() >= 10) {
            methodHash = input.substring(0, 10);
            if(methodHash.equals(EthConstant.METHOD_HASH_CREATEORSIGNMANAGERCHANGE)) {
                isChange = true;
                List<Object> inputData = EthUtil.parseInput(input, EthConstant.INPUT_CHANGE);
                List<Address> adds = (List<Address>) inputData.get(1);
                List<Address> quits = (List<Address>) inputData.get(2);
                if(!adds.isEmpty()) {
                    txInfo.setAddAddresses(EthUtil.list2array(adds.stream().map(a -> a.getValue()).collect(Collectors.toList())));
                }
                if(!quits.isEmpty()) {
                    txInfo.setRemoveAddresses(EthUtil.list2array(quits.stream().map(q -> q.getValue()).collect(Collectors.toList())));
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

    public List<HeterogeneousAddress> parseSigners(TransactionReceipt txReceipt) {
        List<Object> eventResult = this.loadSignersFromEvent(txReceipt);
        if(eventResult == null || eventResult.isEmpty()) {
            return null;
        }
        List<String> addressList = (List<String>) ((List) eventResult.get(0)).stream().map(address -> ((Address) address).getValue()).collect(Collectors.toList());
        List<HeterogeneousAddress> signers = addressList.stream().map(address -> new HeterogeneousAddress(EthConstant.ETH_CHAIN_ID, address)).collect(Collectors.toList());
        return signers;
    }

    private void loadSigners(TransactionReceipt txReceipt, HeterogeneousTransactionInfo txInfo) {
        List<Object> eventResult = this.loadSignersFromEvent(txReceipt);
        if(eventResult != null && !eventResult.isEmpty()) {
            txInfo.setNerveTxHash(eventResult.get(eventResult.size() - 1).toString());
            List<String> addressList = (List<String>) ((List) eventResult.get(0)).stream().map(address -> ((Address) address).getValue()).collect(Collectors.toList());
            List<HeterogeneousAddress> signers = addressList.stream().map(address -> new HeterogeneousAddress(EthConstant.ETH_CHAIN_ID, address)).collect(Collectors.toList());
            txInfo.setSigners(signers);
        }
    }

    private List<Object> loadSignersFromEvent(TransactionReceipt txReceipt) {
        List<Log> logs = txReceipt.getLogs();
        if(logs == null || logs.isEmpty()) {
            return null;
        }
        Log log = logs.get(logs.size() - 1);
        List<String> topics = log.getTopics();
        String eventHash = topics.get(0);
        // topics 解析事件名, 签名完成会触发的事件
        // 解析事件数据，获得交易的签名列表
        List<Object> eventResult = null;
        switch (eventHash) {
            case EthConstant.EVENT_HASH_TRANSACTION_WITHDRAW_COMPLETED:
                eventResult = EthUtil.parseEvent(log.getData(), EthConstant.EVENT_TRANSACTION_WITHDRAW_COMPLETED);
                break;
            case EthConstant.EVENT_HASH_TRANSACTION_MANAGER_CHANGE_COMPLETED:
                eventResult = EthUtil.parseEvent(log.getData(), EthConstant.EVENT_TRANSACTION_MANAGER_CHANGE_COMPLETED);
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
            for(Log log : logs) {
                List<String> topics = log.getTopics();
                // 为ERC20提现
                if (topics.get(0).equals(EthConstant.EVENT_HASH_ERC20_TRANSFER)) {
                    String toAddress = EthConstant.HEX_PREFIX + topics.get(2).substring(26, topics.get(1).length()).toString();
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
                // 为ETH提现
                if (topics.get(0).equals(EthConstant.EVENT_HASH_TRANSFERFUNDS)) {
                    String data = log.getData();
                    String to = EthConstant.HEX_PREFIX + data.substring(26, 66);
                    String amountStr = data.substring(66, 130);
                    // 转账金额
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

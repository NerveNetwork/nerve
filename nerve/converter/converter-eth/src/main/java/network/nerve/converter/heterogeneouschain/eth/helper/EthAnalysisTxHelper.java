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
import io.nuls.core.model.StringUtils;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import network.nerve.converter.heterogeneouschain.eth.helper.interfaces.IEthAnalysisTx;
import network.nerve.converter.heterogeneouschain.eth.listener.EthListener;
import network.nerve.converter.heterogeneouschain.eth.model.EthInput;
import network.nerve.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.eth.storage.EthUnconfirmedTxStorageService;
import network.nerve.converter.heterogeneouschain.eth.utils.EthUtil;
import network.nerve.converter.heterogeneouschain.lib.enums.MultiSignatureStatus;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import org.springframework.beans.BeanUtils;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * @author: Mimi
 * @date: 2020-03-20
 */
@Component
public class EthAnalysisTxHelper implements IEthAnalysisTx {

    @Autowired
    private EthUnconfirmedTxStorageService ethUnconfirmedTxStorageService;
    @Autowired
    private EthERC20Helper ethERC20Helper;
    @Autowired
    private ETHWalletApi ethWalletApi;
    @Autowired
    private EthListener ethListener;
    @Autowired
    private EthParseTxHelper ethParseTxHelper;
    @Autowired
    private EthStorageHelper ethStorageHelper;
    @Autowired
    private EthResendHelper ethResendHelper;

    @Override
    public void analysisTx(org.web3j.protocol.core.methods.response.Transaction tx, long txTime, long blockHeight) throws Exception {
        boolean isDepositTx = false;
        boolean isBroadcastTx = false;
        String nerveTxHash = null;
        String ethTxHash = tx.getHash();
        EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
        HeterogeneousChainTxType txType = null;
        do {
            if (tx == null) {
                break;
            }
            if (tx.getTo() == null) {
                break;
            }

            // Broadcasting transactions
            if (ethListener.isListeningTx(tx.getHash())) {
                EthContext.logger().info("Listening to local broadcastsETHOnline transactions[{}]", tx.getHash());
                isBroadcastTx = true;
                break;
            }

            tx.setFrom(tx.getFrom().toLowerCase());
            tx.setTo(tx.getTo().toLowerCase());
            // applyinputDetermine whether it is a withdrawalorChange transaction,input.substring(0, 10).equals("0xaaaaaaaa")Save the complete transaction data for parsing
            // Broadcasting transactions
            if (ethListener.isListeningAddress(tx.getTo())) {
                EthInput ethInput = this.parseInput(tx.getInput());
                if(ethInput.isBroadcastTx()) {
                    isBroadcastTx = true;
                    txType = ethInput.getTxType();
                    nerveTxHash = ethInput.getNerveTxHash();
                    EthContext.logger().info("Listening toETHNetwork based[{}]transaction[{}], nerveTxHash: {}", txType, tx.getHash(), nerveTxHash);
                    break;
                }
            }

            // ETHRecharge transaction condition: Fixed receiving address, Amount greater than0, absenceinput
            if (ethListener.isListeningAddress(tx.getTo()) &&
                    tx.getValue().compareTo(BigInteger.ZERO) > 0 &&
                    tx.getInput().equals(EthConstant.HEX_PREFIX)) {
                if (!ethParseTxHelper.validationEthDeposit(tx)) {
                    EthContext.logger().error("[{}]No, it's notETHRecharge transaction[2]", ethTxHash);
                    break;
                }
                isDepositTx = true;
                txType = HeterogeneousChainTxType.DEPOSIT;
                po.setIfContractAsset(false);
                po.setTo(tx.getTo());
                po.setValue(tx.getValue());
                po.setDecimals(EthConstant.ETH_DECIMALS);
                po.setAssetId(EthConstant.ETH_ASSET_ID);
                po.setNerveAddress(EthUtil.covertNerveAddressByEthTx(tx));
                EthContext.logger().info("Listening toETHNetwork basedETHRecharge transaction[{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                        tx.getHash(),
                        tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress());
                break;
            }
            // ERC20Recharge transaction
            if (ethERC20Helper.isERC20(tx.getTo(), po)) {
                TransactionReceipt txReceipt = ethWalletApi.getTxReceipt(ethTxHash);
                if (ethERC20Helper.hasERC20WithListeningAddress(txReceipt, po, toAddress -> ethListener.isListeningAddress(toAddress))) {
                    isDepositTx = true;
                    txType = HeterogeneousChainTxType.DEPOSIT;
                    po.setNerveAddress(EthUtil.covertNerveAddressByEthTx(tx));
                    EthContext.logger().info("Listening toETHNetwork basedERC20Recharge transaction[{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                            tx.getHash(),
                            tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
                    break;
                }
            }

        } while (false);
        // Check if it has been affectedNerveNetwork confirmation, the cause is the current node parsingethThe transaction is slower than other nodes, and the current node only resolves this transaction after other nodes confirm it
        EthUnconfirmedTxPo txPoFromDB = null;
        if(isBroadcastTx || isDepositTx) {
            txPoFromDB = ethUnconfirmedTxStorageService.findByTxHash(ethTxHash);
            if(txPoFromDB != null && txPoFromDB.isDelete()) {
                EthContext.logger().info("ETHtransaction[{}]Has been[Nervenetwork]Confirm, no further processing", ethTxHash);
                return;
            }
        }
        // If it is a transaction sent out, such as withdrawal and administrator changes, complete the transaction information
        if (isBroadcastTx) {
            if (txType == null) {
                EthInput ethInput = this.parseInput(tx.getInput());
                txType = ethInput.getTxType();
                nerveTxHash = ethInput.getNerveTxHash();
            }
            this.dealBroadcastTx(nerveTxHash, txType, tx, blockHeight, txTime, txPoFromDB);
            return;
        }
        // Confirmation required for deposit of recharge transactions30In the pending confirmation transaction queue of blocks
        if (isDepositTx) {
            po.setTxType(txType);
            po.setTxHash(ethTxHash);
            po.setBlockHeight(blockHeight);
            po.setFrom(tx.getFrom());
            po.setTxTime(txTime);
            // Save analyzed recharge transactions
            ethStorageHelper.saveTxInfo(po);

            ethUnconfirmedTxStorageService.save(po);
            EthContext.UNCONFIRMED_TX_QUEUE.offer(po);
        }
    }

    private void dealBroadcastTx(String nerveTxHash, HeterogeneousChainTxType txType, Transaction tx, long blockHeight, long txTime, EthUnconfirmedTxPo txPoFromDB) throws Exception {
        String ethTxHash = tx.getHash();
        // inspectnerveTxHashIs it legal
        String realNerveTxHash = nerveTxHash;
        if (nerveTxHash.startsWith(EthConstant.ETH_RECOVERY_I) || nerveTxHash.startsWith(EthConstant.ETH_RECOVERY_II)) {
            realNerveTxHash = nerveTxHash.substring(EthConstant.ETH_RECOVERY_I.length());
            txType = HeterogeneousChainTxType.RECOVERY;
        }
        if(EthContext.getConverterCoreApi().getNerveTx(realNerveTxHash) == null) {
            EthContext.logger().warn("Illegal transaction business[{}], not foundNERVETransaction, Type: {}, Key: {}", ethTxHash, txType, realNerveTxHash);
            return;
        }
        EthUnconfirmedTxPo txPo = txPoFromDB;
        boolean isLocal = true;
        if (txPo == null) {
            txPo = new EthUnconfirmedTxPo();
            isLocal = false;
        }
        txPo.setNerveTxHash(nerveTxHash);
        txPo.setTxHash(ethTxHash);
        txPo.setTxType(txType);
        txPo.setBlockHeight(blockHeight);
        txPo.setTxTime(txTime);
        // Determine whether it is a signed transaction, change the status, and parse the multi signature address list of the transaction
        TransactionReceipt txReceipt = ethWalletApi.getTxReceipt(ethTxHash);
        if (txReceipt == null || !txReceipt.isStatusOK()) {
            // The transaction sent by the current node has failed. If it is resent thirty times and still fails, the transaction will be discarded
            if (ethResendHelper.currentNodeSent(ethTxHash) && ethResendHelper.canResend(nerveTxHash)) {
                ethResendHelper.increase(nerveTxHash);
                txPo.setStatus(MultiSignatureStatus.RESEND);
            } else {
                txPo.setStatus(MultiSignatureStatus.FAILED);
            }
        } else {
            // Set it as a transaction with multiple signatures by default
            txPo.setStatus(MultiSignatureStatus.DOING);
            HeterogeneousTransactionInfo txInfo = null;
            // To determine if it is the first time saving
            if (StringUtils.isBlank(txPo.getFrom())) {
                // Analyze transaction data and supplement basic information、Multiple signature list information
                switch (txType) {
                    case WITHDRAW:
                        txInfo = ethParseTxHelper.parseWithdrawTransaction(tx, txReceipt);
                        break;
                    case RECOVERY:
                    case CHANGE:
                        txInfo = ethParseTxHelper.parseManagerChangeTransaction(tx, txReceipt);
                        break;
                    case UPGRADE:
                        txInfo = ethParseTxHelper.parseUpgradeTransaction(tx, txReceipt);
                        break;
                }
                if (txInfo != null) {
                    txInfo.setNerveTxHash(nerveTxHash);
                    txInfo.setTxHash(ethTxHash);
                    txInfo.setTxType(txType);
                    txInfo.setBlockHeight(blockHeight);
                    txInfo.setTxTime(txTime);
                    BeanUtils.copyProperties(txInfo, txPo);
                }
            } else {
                // Analyze the multi signature list of transactions
                txPo.setSigners(ethParseTxHelper.parseSigners(txReceipt));
            }
            // Transactions set to complete with signatures, when multiple signatures exist
            if (txPo.getSigners() != null && !txPo.getSigners().isEmpty()) {
                EthContext.logger().info("Multiple signatures completed[{}]transaction[{}], signers: {}",
                        txType,
                        ethTxHash, Arrays.toString(txPo.getSigners().toArray()));
                txPo.setStatus(MultiSignatureStatus.COMPLETED);
            }
            // Save parsed signed completed transactions
            if (txInfo != null) {
                ethStorageHelper.saveTxInfo(txInfo);
            } else {
                ethStorageHelper.saveTxInfo(txPo);
            }
        }
        ethUnconfirmedTxStorageService.save(txPo);
        if (!isLocal) {
            EthContext.logger().info("fromethTransactions analyzed by the network[{}], newly added to the pending confirmation queue", ethTxHash);
            EthContext.UNCONFIRMED_TX_QUEUE.offer(txPo);
        }
        // Remove listening
        ethListener.removeListeningTx(ethTxHash);
    }

    private EthInput parseInput(String input) {
        if (input.length() < 10) {
            return EthInput.empty();
        }
        String methodHash;
        if ((methodHash = input.substring(0, 10)).equals(EthConstant.METHOD_HASH_CREATEORSIGNWITHDRAW)) {
            return new EthInput(true, HeterogeneousChainTxType.WITHDRAW, EthUtil.parseInput(input, EthConstant.INPUT_WITHDRAW).get(0).toString());
        }
        if (methodHash.equals(EthConstant.METHOD_HASH_CREATEORSIGNMANAGERCHANGE)) {
            return new EthInput(true, HeterogeneousChainTxType.CHANGE, EthUtil.parseInput(input, EthConstant.INPUT_CHANGE).get(0).toString());
        }
        if (methodHash.equals(EthConstant.METHOD_HASH_CREATEORSIGNUPGRADE)) {
            return new EthInput(true, HeterogeneousChainTxType.UPGRADE, EthUtil.parseInput(input, EthConstant.INPUT_UPGRADE).get(0).toString());
        }
        return EthInput.empty();
    }
}

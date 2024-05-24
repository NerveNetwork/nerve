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

import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.lib.context.HtgConstant;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.heterogeneouschain.lib.enums.MultiSignatureStatus;
import network.nerve.converter.heterogeneouschain.lib.helper.interfaces.IHtgAnalysisTx;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgInput;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgUnconfirmedTxStorageService;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
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
public class HtgAnalysisTxHelper implements IHtgAnalysisTx, BeanInitial {

    private HtgUnconfirmedTxStorageService htUnconfirmedTxStorageService;
    private HtgERC20Helper htgERC20Helper;
    private HtgWalletApi htWalletApi;
    private HtgListener htListener;
    private HtgParseTxHelper htgParseTxHelper;
    private HtgStorageHelper htgStorageHelper;
    private HtgPendingTxHelper htgPendingTxHelper;
    private HtgContext htgContext;

    @Override
    public void analysisTx(Transaction tx, long txTime, long blockHeight) throws Exception {
        boolean isDepositTx = false;
        boolean isBroadcastTx = false;
        String nerveTxHash = null;
        String htTxHash = tx.getHash();
        HtgUnconfirmedTxPo po = new HtgUnconfirmedTxPo();
        HeterogeneousChainTxType txType = null;
        do {
            if (tx == null) {
                return;
            }
            if (tx.getTo() == null) {
                return;
            }
            String from = tx.getFrom().toLowerCase();
            if (htgContext.FILTER_ACCOUNT_SET().contains(from)) {
                htgContext.logger().warn("filterFrom [{}] Transaction [{}]", from, tx.getHash());
                return;
            }
            // Broadcasting transactions
            if (htListener.isListeningTx(tx.getHash())) {
                htgContext.logger().info("Listening to local broadcasts {} Online transactions[{}]", htgContext.getConfig().getSymbol(), tx.getHash());
                isBroadcastTx = true;
                break;
            }
            tx.setFrom(from);
            tx.setTo(tx.getTo().toLowerCase());
            // applyinputDetermine whether it is a withdrawalorChange transaction,input.substring(0, 10).equals("0xaaaaaaaa")Save the complete transaction data for parsing
            if (htListener.isListeningAddress(tx.getTo())) {
                HtgInput htInput = htgParseTxHelper.parseInput(tx.getInput());
                // New recharge transaction method, calling for multi contract signingcrossOutfunction
                if (htInput.isDepositTx()) {
                    isDepositTx = this.parseNewDeposit(tx, po);
                    if (isDepositTx) {
                        txType = HeterogeneousChainTxType.DEPOSIT;
                    }
                    break;
                }
                // New recharge transaction method, calling for multi contract signingcrossOutIIfunction
                if (htInput.isDepositIITx()) {
                    isDepositTx = this.parseNewDepositII(tx, po);
                    if (isDepositTx) {
                        txType = HeterogeneousChainTxType.DEPOSIT;
                    }
                    break;
                }
                // Broadcasting transactions
                if (htInput.isBroadcastTx()) {
                    isBroadcastTx = true;
                    txType = htInput.getTxType();
                    nerveTxHash = htInput.getNerveTxHash();
                    htgContext.logger().info("Listening to {} Network based [{}] transaction [{}], nerveTxHash: {}", htgContext.getConfig().getSymbol(), txType, tx.getHash(), nerveTxHash);
                    break;
                }
            }

            // MainAssetRecharge transaction condition: Fixed receiving address, Amount greater than0, absenceinput
            if (htListener.isListeningAddress(tx.getTo()) &&
                    tx.getValue().compareTo(BigInteger.ZERO) > 0 &&
                    tx.getInput().equals(HtgConstant.HEX_PREFIX)) {
                if (!htgParseTxHelper.validationEthDeposit(tx)) {
                    htgContext.logger().error("[{}] No, it's not MainAsset Recharge transaction[2]", htTxHash);
                    break;
                }
                isDepositTx = true;
                txType = HeterogeneousChainTxType.DEPOSIT;
                po.setIfContractAsset(false);
                po.setTo(tx.getTo());
                po.setValue(tx.getValue());
                po.setDecimals(htgContext.getConfig().getDecimals());
                po.setAssetId(htgContext.HTG_ASSET_ID());
                po.setNerveAddress(HtgUtil.covertNerveAddressByEthTx(tx, htgContext.NERVE_CHAINID()));
                htgContext.logger().info("Listening to {} Network based MainAsset Recharge transaction[1][{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                        htgContext.getConfig().getSymbol(), tx.getHash(),
                        from, po.getTo(), po.getValue(), po.getNerveAddress());
                break;
            }
            // ERC20Recharge transaction
            if (htgERC20Helper.isERC20(tx.getTo(), po) && htgERC20Helper.hasERC20WithListeningAddress(tx.getInput(), toAddress -> htListener.isListeningAddress(toAddress))) {
                TransactionReceipt txReceipt = htWalletApi.getTxReceipt(htTxHash);
                if (htgERC20Helper.hasERC20WithListeningAddress(txReceipt, po, toAddress -> htListener.isListeningAddress(toAddress))) {
                    // Check if it isNERVEAsset boundERC20If yes, check if the customized item has already been registered in the multi signed contractERC20Otherwise, the recharge will be abnormal
                    if (htgContext.getConverterCoreApi().isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                            && !htgParseTxHelper.isMinterERC20(po.getContractAddress())) {
                        htgContext.logger().warn("[{}] Illegal {} Online recharge transactions[5], ERC20 [{}] Bound NERVE Assets, but not registered in the contract", htTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
                        break;
                    }
                    isDepositTx = true;
                    txType = HeterogeneousChainTxType.DEPOSIT;
                    po.setNerveAddress(HtgUtil.covertNerveAddressByEthTx(tx, htgContext.NERVE_CHAINID()));
                    htgContext.logger().info("Listening to {} Network based ERC20 Recharge transaction[1][{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                            htgContext.getConfig().getSymbol(), tx.getHash(),
                            from, po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
                    break;
                }
            }

        } while (false);
        if (isDepositTx) {
            if (!htgContext.getConverterCoreApi().validNerveAddress(po.getNerveAddress())) {
                htgContext.logger().warn("[Abnormal recharge address] transaction[{}], [0]Recharge address: {}", htTxHash, po.getNerveAddress());
                return;
            }
            // add by pierre at 2022/6/29 Add recharge pause mechanism
            if (htgContext.getConverterCoreApi().isPauseInHeterogeneousAsset(htgContext.HTG_CHAIN_ID(), po.getAssetId())) {
                htgContext.logger().warn("[Recharge pause] transaction [{}]", htTxHash);
                return;
            }
        }
        // Check if it has been affected NerveNetwork confirmation, the cause is the current node parsing eth The transaction is slower than other nodes, and the current node only resolves this transaction after other nodes confirm it
        HtgUnconfirmedTxPo txPoFromDB = null;
        if (isBroadcastTx || isDepositTx) {
            txPoFromDB = htUnconfirmedTxStorageService.findByTxHash(htTxHash);
            if (txPoFromDB != null && txPoFromDB.isDelete()) {
                htgContext.logger().info("{} transaction [{}] Has been [Nervenetwork] Confirm, no further processing", htgContext.getConfig().getSymbol(), htTxHash);
                return;
            }
        }
        // If it is a transaction sent out, such as withdrawal and administrator changes, complete the transaction information
        if (isBroadcastTx) {
            if (txType == null) {
                HtgInput htInput = htgParseTxHelper.parseInput(tx.getInput());
                txType = htInput.getTxType();
                nerveTxHash = htInput.getNerveTxHash();
            }
            this.dealBroadcastTx(nerveTxHash, txType, tx, blockHeight, txTime, txPoFromDB);
            return;
        }
        // Confirmation required for deposit of recharge transactions 30 In the pending confirmation transaction queue of blocks
        if (isDepositTx) {
            po.setTxType(txType);
            po.setTxHash(htTxHash);
            po.setBlockHeight(blockHeight);
            po.setFrom(tx.getFrom());
            po.setTxTime(txTime);
            // Save analyzed recharge transactions
            htgStorageHelper.saveTxInfo(po);
            htUnconfirmedTxStorageService.save(po);
            htgContext.UNCONFIRMED_TX_QUEUE().offer(po);
            // towards NERVE Online recharge pending confirmation transaction
            htgPendingTxHelper.commitNervePendingDepositTx(po,
                    (extend, logger) -> {
                        return htgParseTxHelper.parseOneClickCrossChainData(extend, logger);
                    },
                    (extend, logger) -> {
                        return htgParseTxHelper.parseAddFeeCrossChainData(extend, logger);
                    }
            );
        }
    }

    private boolean parseNewDeposit(Transaction tx, HtgUnconfirmedTxPo po) throws Exception {
        String htTxHash = tx.getHash();
        // Calling for multiple signed contractscrossOutThe recharge method of functions
        if (!htgParseTxHelper.validationEthDepositByCrossOut(tx, po)) {
            htgContext.logger().error("[{}] Illegal {} Online recharge transactions[3]", htTxHash, htgContext.getConfig().getSymbol());
            return false;
        }
        if (!po.isIfContractAsset()) {
            htgContext.logger().info("Listening to {} Network based MainAsset Recharge transaction[2][{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                    htgContext.getConfig().getSymbol(), tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress());
        } else {
            // Check if it isNERVEAsset boundERC20If yes, check if the customized item has already been registered in the multi signed contractERC20Otherwise, the recharge will be abnormal
            if (htgContext.getConverterCoreApi().isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                    && !htgParseTxHelper.isMinterERC20(po.getContractAddress())) {
                htgContext.logger().warn("[{}] Illegal {} Online recharge transactions[4], ERC20 [{}] Bound NERVE Assets, but not registered in the contract", htTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
                return false;
            }
            htgContext.logger().info("Listening to {} Network based ERC20 Recharge transaction[2][{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                    htgContext.getConfig().getSymbol(), tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
        }
        return true;
    }

    private boolean parseNewDepositII(Transaction tx, HtgUnconfirmedTxPo po) throws Exception {
        String htTxHash = tx.getHash();
        // Calling for multiple signed contractscrossOutIIThe recharge method of functions
        if (!htgParseTxHelper.validationEthDepositByCrossOutII(tx, null, po)) {
            htgContext.logger().error("[{}] Illegal {} Network rechargeII transaction[0]", htTxHash, htgContext.getConfig().getSymbol());
            return false;
        }
        // Check if it isNERVEAsset boundERC20If yes, check if the customized item has already been registered in the multi signed contractERC20Otherwise, the recharge will be abnormal
        if (po.isIfContractAsset() && htgContext.getConverterCoreApi().isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                && !htgParseTxHelper.isMinterERC20(po.getContractAddress())) {
            htgContext.logger().warn("[{}] Illegal {} Network rechargeII transaction[0], ERC20 [{}] Bound NERVE Assets, but not registered in the contract", htTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
            return false;
        }
        if (po.isDepositIIMainAndToken()) {
            htgContext.logger().info("Listening to {} Network based ERC20/{} Simultaneously rechargeII transaction[0][{}], from: {}, to: {}, erc20Value: {}, nerveAddress: {}, contract: {}, decimals: {}, mainAssetValue: {}",
                    htgContext.getConfig().getSymbol(), htgContext.getConfig().getSymbol(), tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals(), po.getDepositIIMainAssetValue());
        } else if (po.isIfContractAsset()) {
            htgContext.logger().info("Listening to {} Network based ERC20 RechargeII transaction[0][{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                    htgContext.getConfig().getSymbol(), tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
        } else {
            htgContext.logger().info("Listening to {} Network based MainAsset RechargeII transaction[0][{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                    htgContext.getConfig().getSymbol(), tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress());
        }

        return true;
    }

    private void dealBroadcastTx(String nerveTxHash, HeterogeneousChainTxType txType, Transaction tx, long blockHeight, long txTime, HtgUnconfirmedTxPo txPoFromDB) throws Exception {
        String htTxHash = tx.getHash();
        // inspectnerveTxHashIs it legal
        if (htgContext.getConverterCoreApi().getNerveTx(nerveTxHash) == null) {
            htListener.removeListeningTx(htTxHash);
            htgContext.logger().warn("Illegal transaction business [{}], not found NERVE Transaction, Type: {}, Key: {}", htTxHash, txType, nerveTxHash);
            return;
        }
        HtgUnconfirmedTxPo txPo = txPoFromDB;
        boolean isLocalSent = true;
        if (txPo == null) {
            txPo = new HtgUnconfirmedTxPo();
            isLocalSent = false;
        }
        txPo.setNerveTxHash(nerveTxHash);
        txPo.setTxHash(htTxHash);
        txPo.setTxType(txType);
        txPo.setBlockHeight(blockHeight);
        txPo.setTxTime(txTime);
        txPo.setFrom(tx.getFrom());
        // Determine whether the transaction was successful, change the status, and analyze the event of the transaction
        TransactionReceipt txReceipt = htWalletApi.getTxReceipt(htTxHash);
        if (txReceipt == null || !txReceipt.isStatusOK()) {
            txPo.setStatus(MultiSignatureStatus.FAILED);
        } else {
            HeterogeneousTransactionInfo txInfo = null;
            // Analyze transaction data and supplement basic information
            switch (txType) {
                case WITHDRAW:
                    txInfo = htgParseTxHelper.parseWithdrawTransaction(tx, txReceipt);
                    break;
                case CHANGE:
                    txInfo = htgParseTxHelper.parseManagerChangeTransaction(tx, txReceipt);
                    break;
                case UPGRADE:
                    txInfo = htgParseTxHelper.parseUpgradeTransaction(tx, txReceipt);
                    break;
            }
            if (txInfo != null) {
                txInfo.setNerveTxHash(nerveTxHash);
                txInfo.setTxHash(htTxHash);
                txInfo.setTxType(txType);
                txInfo.setBlockHeight(blockHeight);
                txInfo.setTxTime(txTime);
                BeanUtils.copyProperties(txInfo, txPo);
            }
            // Transactions set to complete with signatures, when multiple signatures exist
            if (txPo.getSigners() != null && !txPo.getSigners().isEmpty()) {
                htgContext.logger().info("Multiple signatures completed [{}] transaction [{}], signers: {}", txType, htTxHash, Arrays.toString(txPo.getSigners().toArray()));
                txPo.setStatus(MultiSignatureStatus.COMPLETED);
                // Save parsed signed completed transactions
                if (txInfo != null) {
                    htgStorageHelper.saveTxInfo(txInfo);
                } else {
                    htgStorageHelper.saveTxInfo(txPo);
                }
            } else {
                htgContext.logger().error("[fail] Failed to resolve the event of completing multiple signatures [{}] transaction[ {}]", txType, htTxHash);
                txPo.setStatus(MultiSignatureStatus.FAILED);
            }

        }
        htUnconfirmedTxStorageService.save(txPo);
        if (!isLocalSent) {
            htgContext.logger().info("from {} Transactions analyzed by the network [{}], newly added to the pending confirmation queue", htgContext.getConfig().getSymbol(), htTxHash);
            htgContext.UNCONFIRMED_TX_QUEUE().offer(txPo);
        }
        // Remove listening
        htListener.removeListeningTx(htTxHash);
    }

}

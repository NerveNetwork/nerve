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

import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.enums.MultiSignatureStatus;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgPendingTxHelper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgStorageHelper;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgInput;
import network.nerve.converter.heterogeneouschain.lib.model.HtgUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgUnconfirmedTxStorageService;
import network.nerve.converter.heterogeneouschain.trx.core.TrxWalletApi;
import network.nerve.converter.heterogeneouschain.trx.model.TrxTransaction;
import network.nerve.converter.heterogeneouschain.trx.utils.TrxUtil;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import org.springframework.beans.BeanUtils;
import org.tron.trident.proto.Chain;
import org.tron.trident.proto.Response;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * @author: Mimi
 * @date: 2020-03-20
 */
public class TrxAnalysisTxHelper implements BeanInitial {

    private TrxERC20Helper trxERC20Helper;
    private TrxWalletApi trxWalletApi;
    private TrxParseTxHelper trxParseTxHelper;
    private HtgUnconfirmedTxStorageService htUnconfirmedTxStorageService;
    private HtgListener htListener;
    private HtgStorageHelper htgStorageHelper;
    private HtgPendingTxHelper htgPendingTxHelper;
    private HtgContext htgContext;

    public void analysisTx(Chain.Transaction tx, long txTime, long blockHeight) throws Exception {
        boolean isDepositTx = false;
        boolean isBroadcastTx = false;
        String nerveTxHash = null;
        HtgUnconfirmedTxPo po;
        HeterogeneousChainTxType txType = null;
        String trxTxHash;
        TrxTransaction txInfo;
        do {
            if (tx == null) {
                return;
            }
            if (tx.getRetCount() == 0) {
                return;
            }
            if (tx.getRet(0).getContractRet() != Chain.Transaction.Result.contractResult.SUCCESS) {
                return;
            }
            Chain.Transaction.raw txRawData = tx.getRawData();
            if (txRawData.getContractCount() == 0) {
                return;
            }

            txInfo = TrxUtil.generateTxInfo(tx);
            // filter wrongTRXTransfer and Call Contract Transactions
            if (txInfo == null) {
                return;
            }
            trxTxHash = txInfo.getHash();
            String from = txInfo.getFrom(), to = txInfo.getTo(), input = txInfo.getInput();
            BigInteger value = txInfo.getValue();
            Chain.Transaction.Contract.ContractType type = txInfo.getType();
            //htgContext.logger().debug("[{}] hash: {}, from: {}, to: {}, value: {}, input: {}", txInfo.getType(), trxTxHash, from, to, value, input);

            po = new HtgUnconfirmedTxPo();

            if (htgContext.FILTER_ACCOUNT_SET().contains(from)) {
                htgContext.logger().warn("filterFrom[{}]Transaction[{}]", from, trxTxHash);
                return;
            }
            // Broadcasting transactions
            if (htListener.isListeningTx(trxTxHash)) {
                htgContext.logger().info("Listening to local broadcasts{}Online transactions[{}]", htgContext.getConfig().getSymbol(), trxTxHash);
                isBroadcastTx = true;
                break;
            }
            // applyinputDetermine whether it is a withdrawalorChange transaction,input.substring(0, 10).equals("0xaaaaaaaa")Save the complete transaction data for parsing
            if (htListener.isListeningAddress(to)) {
                HtgInput htInput = trxParseTxHelper.parseInput(input);
                // New recharge transaction method, calling for multi contract signingcrossOutfunction
                if (htInput.isDepositTx()) {
                    isDepositTx = this.parseNewDeposit(txInfo, po);
                    if (isDepositTx) {
                        txType = HeterogeneousChainTxType.DEPOSIT;
                    }
                    break;
                }
                // New recharge transaction method, calling for multi contract signingcrossOutIIfunction
                if (htInput.isDepositIITx()) {
                    isDepositTx = this.parseNewDepositII(txInfo, po);
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
                    htgContext.logger().info("Listening to{}Network based[{}]transaction[{}], nerveTxHash: {}", htgContext.getConfig().getSymbol(), txType, trxTxHash, nerveTxHash);
                    break;
                }
            }

            // MainAssetRecharge transaction condition: Fixed receiving address, Amount greater than0, Transfer transaction type
            if (htListener.isListeningAddress(to) && value.compareTo(BigInteger.ZERO) > 0) {
                if (Chain.Transaction.Contract.ContractType.TriggerSmartContract == type && !trxParseTxHelper.validationDeposit(txInfo)) {
                    htgContext.logger().error("[{}]No, it's notMainAssetRecharge transaction[2]", trxTxHash);
                    break;
                }
                isDepositTx = true;
                txType = HeterogeneousChainTxType.DEPOSIT;
                po.setIfContractAsset(false);
                po.setTo(to);
                po.setValue(value);
                po.setDecimals(htgContext.getConfig().getDecimals());
                po.setAssetId(htgContext.HTG_ASSET_ID());
                po.setNerveAddress(TrxUtil.covertNerveAddressByTx(tx, htgContext.NERVE_CHAINID(), from));
                htgContext.logger().info("Listening to{}Network basedMainAssetRecharge transaction[1][{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                        htgContext.getConfig().getSymbol(), trxTxHash,
                        from, po.getTo(), po.getValue(), po.getNerveAddress());
                break;
            }
            // ERC20Recharge transaction
            if (trxERC20Helper.isERC20(to, po) && trxERC20Helper.hasERC20WithListeningAddress(input, toAddress -> htListener.isListeningAddress(toAddress))) {
                Response.TransactionInfo txReceipt = trxWalletApi.getTransactionReceipt(trxTxHash);
                if (trxERC20Helper.hasERC20WithListeningAddress(txReceipt, po, toAddress -> htListener.isListeningAddress(toAddress))) {
                    // Check if it isNERVEAsset boundERC20If yes, check if the customized item has already been registered in the multi signed contractERC20Otherwise, the recharge will be abnormal
                    if (htgContext.getConverterCoreApi().isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                            && !trxParseTxHelper.isMinterERC20(po.getContractAddress())) {
                        String msg = String.format("[%s]不合法的%s网络的充值交易[5], ERC20[%s]已绑定NERVE资产，但合约内未注册", trxTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
                        htgContext.logger().warn(msg);
                        htgContext.getConverterCoreApi().putWechatMsg(msg);
                        break;
                    }
                    isDepositTx = true;
                    txType = HeterogeneousChainTxType.DEPOSIT;
                    po.setNerveAddress(TrxUtil.covertNerveAddressByTx(tx, htgContext.NERVE_CHAINID(), from));
                    htgContext.logger().info("Listening to{}Network basedERC20Recharge transaction[1][{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                            htgContext.getConfig().getSymbol(), trxTxHash,
                            from, po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
                    break;
                }
            }

        } while (false);
        if (isDepositTx) {
            if (!htgContext.getConverterCoreApi().validNerveAddress(po.getNerveAddress())) {
                String msg = String.format("[充值地址异常] 交易[%s], [0]充值地址: %s", trxTxHash, po.getNerveAddress());
                htgContext.logger().warn(msg);
                htgContext.getConverterCoreApi().putWechatMsg(msg);
                return;
            }
            // add by pierre at 2022/6/29 Add recharge pause mechanism
            if (htgContext.getConverterCoreApi().isPauseInHeterogeneousAsset(htgContext.HTG_CHAIN_ID(), po.getAssetId())) {
                htgContext.logger().warn("[Recharge pause] transaction[{}]", trxTxHash);
                return;
            }
        }
        // Check if it has been affectedNerveNetwork confirmation, the cause is the current node parsingethThe transaction is slower than other nodes, and the current node only resolves this transaction after other nodes confirm it
        HtgUnconfirmedTxPo txPoFromDB = null;
        if (isBroadcastTx || isDepositTx) {
            txPoFromDB = htUnconfirmedTxStorageService.findByTxHash(trxTxHash);
            if (txPoFromDB != null && txPoFromDB.isDelete()) {
                htgContext.logger().info("{}transaction[{}]Has been[Nervenetwork]Confirm, no further processing", htgContext.getConfig().getSymbol(), trxTxHash);
                return;
            }
        }
        // If it is a transaction sent out, such as withdrawal and administrator changes, complete the transaction information
        if (isBroadcastTx) {
            if (txType == null) {
                HtgInput htInput = trxParseTxHelper.parseInput(txInfo.getInput());
                txType = htInput.getTxType();
                nerveTxHash = htInput.getNerveTxHash();
            }
            this.dealBroadcastTx(nerveTxHash, txType, tx, txInfo, blockHeight, txTime, txPoFromDB);
            return;
        }
        // Confirmation required for deposit of recharge transactions30In the pending confirmation transaction queue of blocks
        if (isDepositTx) {
            po.setTxType(txType);
            po.setTxHash(trxTxHash);
            po.setBlockHeight(blockHeight);
            po.setFrom(txInfo.getFrom());
            po.setTxTime(txTime);
            // Save analyzed recharge transactions
            htgStorageHelper.saveTxInfo(po);
            htUnconfirmedTxStorageService.save(po);
            htgContext.UNCONFIRMED_TX_QUEUE().offer(po);
            // towardsNERVEOnline recharge pending confirmation transaction
            htgPendingTxHelper.commitNervePendingDepositTx(po,
                    (extend, logger) -> {
                        return trxParseTxHelper.parseOneClickCrossChainData(extend, logger);
                    },
                    (extend, logger) -> {
                        return trxParseTxHelper.parseAddFeeCrossChainData(extend, logger);
                    }
            );
        }
    }

    private boolean parseNewDeposit(TrxTransaction tx, HtgUnconfirmedTxPo po) throws Exception {
        String htTxHash = tx.getHash();
        // Calling for multiple signed contractscrossOutThe recharge method of functions
        if (!trxParseTxHelper.validationDepositByCrossOut(tx, po)) {
            String msg = String.format("[%s]不合法的%s网络的充值交易[3]", htTxHash, htgContext.getConfig().getSymbol());
            htgContext.logger().error(msg);
            htgContext.getConverterCoreApi().putWechatMsg(msg);
            return false;
        }
        if (!po.isIfContractAsset()) {
            htgContext.logger().info("Listening to{}Network basedMainAssetRecharge transaction[2][{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                    htgContext.getConfig().getSymbol(), tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress());
        } else {
            // Check if it isNERVEAsset boundERC20If yes, check if the customized item has already been registered in the multi signed contractERC20Otherwise, the recharge will be abnormal
            if (htgContext.getConverterCoreApi().isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                    && !trxParseTxHelper.isMinterERC20(po.getContractAddress())) {
                String msg = String.format("[%s]不合法的%s网络的充值交易[4], ERC20[%s]已绑定NERVE资产，但合约内未注册", htTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
                htgContext.logger().warn(msg);
                htgContext.getConverterCoreApi().putWechatMsg(msg);
                return false;
            }
            htgContext.logger().info("Listening to{}Network basedERC20Recharge transaction[2][{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                    htgContext.getConfig().getSymbol(), tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
        }
        return true;
    }

    private boolean parseNewDepositII(TrxTransaction tx, HtgUnconfirmedTxPo po) throws Exception {
        String htTxHash = tx.getHash();
        // Calling for multiple signed contractscrossOutThe recharge method of functions
        if (!trxParseTxHelper.validationDepositByCrossOutII(tx, null, po)) {
            String msg = String.format("[%s]不合法的%s网络的充值II交易[0]", htTxHash, htgContext.getConfig().getSymbol());
            htgContext.logger().error(msg);
            htgContext.getConverterCoreApi().putWechatMsg(msg);
            return false;
        }
        // Check if it isNERVEAsset boundERC20If yes, check if the customized item has already been registered in the multi signed contractERC20Otherwise, the recharge will be abnormal
        if (po.isIfContractAsset() && htgContext.getConverterCoreApi().isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                && !trxParseTxHelper.isMinterERC20(po.getContractAddress())) {
            String msg = String.format("[%s]不合法的%s网络的充值II交易[0], ERC20[%s]已绑定NERVE资产，但合约内未注册", htTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
            htgContext.logger().warn(msg);
            htgContext.getConverterCoreApi().putWechatMsg(msg);
            return false;
        }
        if (po.isDepositIIMainAndToken()) {
            htgContext.logger().info("Listening to{}Network basedERC20/{}Simultaneously rechargeIItransaction[0][{}], from: {}, to: {}, erc20Value: {}, nerveAddress: {}, contract: {}, decimals: {}, mainAssetValue: {}",
                    htgContext.getConfig().getSymbol(), htgContext.getConfig().getSymbol(), tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals(), po.getDepositIIMainAssetValue());
        } else if (po.isIfContractAsset()) {
            htgContext.logger().info("Listening to{}Network basedERC20RechargeIItransaction[0][{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                    htgContext.getConfig().getSymbol(), tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
        } else {
            htgContext.logger().info("Listening to{}Network basedMainAssetRechargeIItransaction[0][{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                    htgContext.getConfig().getSymbol(), tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress());
        }

        return true;
    }

    private void dealBroadcastTx(String nerveTxHash, HeterogeneousChainTxType txType, Chain.Transaction tx, TrxTransaction trxTxInfo, long blockHeight, long txTime, HtgUnconfirmedTxPo txPoFromDB) throws Exception {
        String trxTxHash = trxTxInfo.getHash();
        // inspectnerveTxHashIs it legal
        if (htgContext.getConverterCoreApi().getNerveTx(nerveTxHash) == null) {
            String warnMsg = String.format("[%s]网络交易业务不合法[%s]!!! 未找到NERVE交易，类型: %s, Key: %s，请重点检查此交易！", htgContext.getConfig().getSymbol(), trxTxHash, txType, nerveTxHash);
            htgContext.getConverterCoreApi().putWechatMsg(warnMsg + "1st");
            htgContext.getConverterCoreApi().putWechatMsg(warnMsg + "2nd");
            htgContext.getConverterCoreApi().putWechatMsg(warnMsg + "3rd");
            htgContext.logger().warn(warnMsg);
            return;
        }
        HtgUnconfirmedTxPo txPo = txPoFromDB;
        boolean isLocalSent = true;
        if (txPo == null) {
            txPo = new HtgUnconfirmedTxPo();
            isLocalSent = false;
        }
        txPo.setNerveTxHash(nerveTxHash);
        txPo.setTxHash(trxTxHash);
        txPo.setTxType(txType);
        txPo.setBlockHeight(blockHeight);
        txPo.setTxTime(txTime);
        txPo.setFrom(trxTxInfo.getFrom());
        // Determine whether the transaction was successful, change the status, and analyze the event of the transaction
        Response.TransactionInfo txReceipt = trxWalletApi.getTransactionReceipt(trxTxHash);
        if (!TrxUtil.checkTransactionSuccess(txReceipt)) {
            txPo.setStatus(MultiSignatureStatus.FAILED);
        } else {
            HeterogeneousTransactionInfo txInfo = null;
            // Analyze transaction data and supplement basic information
            switch (txType) {
                case WITHDRAW:
                    txInfo = trxParseTxHelper.parseWithdrawTransaction(trxTxInfo, txReceipt);
                    break;
                case CHANGE:
                    txInfo = trxParseTxHelper.parseManagerChangeTransaction(trxTxInfo, txReceipt);
                    break;
                case UPGRADE:
                    txInfo = trxParseTxHelper.parseUpgradeTransaction(trxTxInfo, txReceipt);
                    break;
            }
            if (txInfo != null) {
                txInfo.setNerveTxHash(nerveTxHash);
                txInfo.setTxHash(trxTxHash);
                txInfo.setTxType(txType);
                txInfo.setBlockHeight(blockHeight);
                txInfo.setTxTime(txTime);
                BeanUtils.copyProperties(txInfo, txPo);
            }
            // Transactions set to complete with signatures, when multiple signatures exist
            if (txPo.getSigners() != null && !txPo.getSigners().isEmpty()) {
                htgContext.logger().info("Multiple signatures completed[{}]transaction[{}], signers: {}", txType, trxTxHash, Arrays.toString(txPo.getSigners().toArray()));
                txPo.setStatus(MultiSignatureStatus.COMPLETED);
                // Save parsed signed completed transactions
                if (txInfo != null) {
                    htgStorageHelper.saveTxInfo(txInfo);
                } else {
                    htgStorageHelper.saveTxInfo(txPo);
                }
            } else {
                String msg = String.format("[失败]没有解析到完成多签的事件[%s]交易[%s]", txType, trxTxHash);
                htgContext.logger().error(msg);
                htgContext.getConverterCoreApi().putWechatMsg(msg);
                txPo.setStatus(MultiSignatureStatus.FAILED);
            }

        }
        htUnconfirmedTxStorageService.save(txPo);
        if (!isLocalSent) {
            htgContext.logger().info("from{}Transactions analyzed by the network[{}], newly added to the pending confirmation queue", htgContext.getConfig().getSymbol(), trxTxHash);
            htgContext.UNCONFIRMED_TX_QUEUE().offer(txPo);
        }
        // Remove listening
        htListener.removeListeningTx(trxTxHash);
    }

}

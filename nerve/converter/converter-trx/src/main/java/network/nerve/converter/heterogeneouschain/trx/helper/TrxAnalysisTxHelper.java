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
            // 过滤 非TRX转账和调用合约交易
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
                htgContext.logger().warn("过滤From[{}]的交易[{}]", from, trxTxHash);
                return;
            }
            // 广播的交易
            if (htListener.isListeningTx(trxTxHash)) {
                htgContext.logger().info("监听到本地广播到{}网络的交易[{}]", htgContext.getConfig().getSymbol(), trxTxHash);
                isBroadcastTx = true;
                break;
            }
            // 使用input判断是否为提现or变更交易，input.substring(0, 10).equals("0xaaaaaaaa")，保存解析的完整交易数据
            if (htListener.isListeningAddress(to)) {
                HtgInput htInput = trxParseTxHelper.parseInput(input);
                // 新的充值交易方式，调用多签合约的crossOut函数
                if (htInput.isDepositTx()) {
                    isDepositTx = this.parseNewDeposit(txInfo, po);
                    if (isDepositTx) {
                        txType = HeterogeneousChainTxType.DEPOSIT;
                    }
                    break;
                }
                // 新的充值交易方式，调用多签合约的crossOutII函数
                if (htInput.isDepositIITx()) {
                    isDepositTx = this.parseNewDepositII(txInfo, po);
                    if (isDepositTx) {
                        txType = HeterogeneousChainTxType.DEPOSIT;
                    }
                    break;
                }
                // 广播的交易
                if (htInput.isBroadcastTx()) {
                    isBroadcastTx = true;
                    txType = htInput.getTxType();
                    nerveTxHash = htInput.getNerveTxHash();
                    htgContext.logger().info("监听到{}网络的[{}]交易[{}], nerveTxHash: {}", htgContext.getConfig().getSymbol(), txType, trxTxHash, nerveTxHash);
                    break;
                }
            }

            // MainAsset充值交易 条件: 固定接收地址, 金额大于0, 转账交易类型
            if (htListener.isListeningAddress(to) && value.compareTo(BigInteger.ZERO) > 0) {
                if (Chain.Transaction.Contract.ContractType.TriggerSmartContract == type && !trxParseTxHelper.validationDeposit(txInfo)) {
                    htgContext.logger().error("[{}]不是MainAsset充值交易[2]", trxTxHash);
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
                htgContext.logger().info("监听到{}网络的MainAsset充值交易[1][{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                        htgContext.getConfig().getSymbol(), trxTxHash,
                        from, po.getTo(), po.getValue(), po.getNerveAddress());
                break;
            }
            // ERC20充值交易
            if (trxERC20Helper.isERC20(to, po) && trxERC20Helper.hasERC20WithListeningAddress(input, toAddress -> htListener.isListeningAddress(toAddress))) {
                Response.TransactionInfo txReceipt = trxWalletApi.getTransactionReceipt(trxTxHash);
                if (trxERC20Helper.hasERC20WithListeningAddress(txReceipt, po, toAddress -> htListener.isListeningAddress(toAddress))) {
                    // 检查是否是NERVE资产绑定的ERC20，是则检查多签合约内是否已经注册此定制的ERC20，否则充值异常
                    if (htgContext.getConverterCoreApi().isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                            && !trxParseTxHelper.isMinterERC20(po.getContractAddress())) {
                        htgContext.logger().warn("[{}]不合法的{}网络的充值交易[5], ERC20[{}]已绑定NERVE资产，但合约内未注册", trxTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
                        break;
                    }
                    isDepositTx = true;
                    txType = HeterogeneousChainTxType.DEPOSIT;
                    po.setNerveAddress(TrxUtil.covertNerveAddressByTx(tx, htgContext.NERVE_CHAINID(), from));
                    htgContext.logger().info("监听到{}网络的ERC20充值交易[1][{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                            htgContext.getConfig().getSymbol(), trxTxHash,
                            from, po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
                    break;
                }
            }

        } while (false);
        if (isDepositTx && !htgContext.getConverterCoreApi().validNerveAddress(po.getNerveAddress())) {
            htgContext.logger().warn("[充值地址异常] 交易[{}], [0]充值地址: {}", trxTxHash, po.getNerveAddress());
            return;
        }
        // 检查是否被Nerve网络确认，产生原因是当前节点解析eth交易比其他节点慢，其他节点确认了此交易后，当前节点才解析到此交易
        HtgUnconfirmedTxPo txPoFromDB = null;
        if (isBroadcastTx || isDepositTx) {
            txPoFromDB = htUnconfirmedTxStorageService.findByTxHash(trxTxHash);
            if (txPoFromDB != null && txPoFromDB.isDelete()) {
                htgContext.logger().info("{}交易[{}]已被[Nerve网络]确认，不再处理", htgContext.getConfig().getSymbol(), trxTxHash);
                return;
            }
        }
        // 如果是发出去的交易，例如提现和管理员变更，则补全交易信息
        if (isBroadcastTx) {
            if (txType == null) {
                HtgInput htInput = trxParseTxHelper.parseInput(txInfo.getInput());
                txType = htInput.getTxType();
                nerveTxHash = htInput.getNerveTxHash();
            }
            this.dealBroadcastTx(nerveTxHash, txType, tx, txInfo, blockHeight, txTime, txPoFromDB);
            return;
        }
        // 充值交易放入需要确认30个区块的待确认交易队列中
        if (isDepositTx) {
            po.setTxType(txType);
            po.setTxHash(trxTxHash);
            po.setBlockHeight(blockHeight);
            po.setFrom(txInfo.getFrom());
            po.setTxTime(txTime);
            // 保存解析的充值交易
            htgStorageHelper.saveTxInfo(po);
            htUnconfirmedTxStorageService.save(po);
            htgContext.UNCONFIRMED_TX_QUEUE().offer(po);
            // 向NERVE网络发出充值待确认交易
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
        // 调用多签合约的crossOut函数的充值方式
        if (!trxParseTxHelper.validationDepositByCrossOut(tx, po)) {
            htgContext.logger().error("[{}]不合法的{}网络的充值交易[3]", htTxHash, htgContext.getConfig().getSymbol());
            return false;
        }
        if (!po.isIfContractAsset()) {
            htgContext.logger().info("监听到{}网络的MainAsset充值交易[2][{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                    htgContext.getConfig().getSymbol(), tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress());
        } else {
            // 检查是否是NERVE资产绑定的ERC20，是则检查多签合约内是否已经注册此定制的ERC20，否则充值异常
            if (htgContext.getConverterCoreApi().isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                    && !trxParseTxHelper.isMinterERC20(po.getContractAddress())) {
                htgContext.logger().warn("[{}]不合法的{}网络的充值交易[4], ERC20[{}]已绑定NERVE资产，但合约内未注册", htTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
                return false;
            }
            htgContext.logger().info("监听到{}网络的ERC20充值交易[2][{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                    htgContext.getConfig().getSymbol(), tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
        }
        return true;
    }

    private boolean parseNewDepositII(TrxTransaction tx, HtgUnconfirmedTxPo po) throws Exception {
        String htTxHash = tx.getHash();
        // 调用多签合约的crossOut函数的充值方式
        if (!trxParseTxHelper.validationDepositByCrossOutII(tx, null, po)) {
            htgContext.logger().error("[{}]不合法的{}网络的充值II交易[0]", htTxHash, htgContext.getConfig().getSymbol());
            return false;
        }
        // 检查是否是NERVE资产绑定的ERC20，是则检查多签合约内是否已经注册此定制的ERC20，否则充值异常
        if (po.isIfContractAsset() && htgContext.getConverterCoreApi().isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                && !trxParseTxHelper.isMinterERC20(po.getContractAddress())) {
            htgContext.logger().warn("[{}]不合法的{}网络的充值II交易[0], ERC20[{}]已绑定NERVE资产，但合约内未注册", htTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
            return false;
        }
        if (po.isDepositIIMainAndToken()) {
            htgContext.logger().info("监听到{}网络的ERC20/{}同时充值II交易[0][{}], from: {}, to: {}, erc20Value: {}, nerveAddress: {}, contract: {}, decimals: {}, mainAssetValue: {}",
                    htgContext.getConfig().getSymbol(), htgContext.getConfig().getSymbol(), tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals(), po.getDepositIIMainAssetValue());
        } else if (po.isIfContractAsset()) {
            htgContext.logger().info("监听到{}网络的ERC20充值II交易[0][{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                    htgContext.getConfig().getSymbol(), tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
        } else {
            htgContext.logger().info("监听到{}网络的MainAsset充值II交易[0][{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                    htgContext.getConfig().getSymbol(), tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress());
        }

        return true;
    }

    private void dealBroadcastTx(String nerveTxHash, HeterogeneousChainTxType txType, Chain.Transaction tx, TrxTransaction trxTxInfo, long blockHeight, long txTime, HtgUnconfirmedTxPo txPoFromDB) throws Exception {
        String trxTxHash = trxTxInfo.getHash();
        // 检查nerveTxHash是否合法
        if (htgContext.getConverterCoreApi().getNerveTx(nerveTxHash) == null) {
            htgContext.logger().warn("交易业务不合法[{}]，未找到NERVE交易，类型: {}, Key: {}", trxTxHash, txType, nerveTxHash);
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
        // 判断交易是否成功，更改状态，解析交易的事件
        Response.TransactionInfo txReceipt = trxWalletApi.getTransactionReceipt(trxTxHash);
        if (!TrxUtil.checkTransactionSuccess(txReceipt)) {
            txPo.setStatus(MultiSignatureStatus.FAILED);
        } else {
            HeterogeneousTransactionInfo txInfo = null;
            // 解析交易数据，补充基本信息
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
            // 设置为签名完成的交易，当多签存在时
            if (txPo.getSigners() != null && !txPo.getSigners().isEmpty()) {
                htgContext.logger().info("已完成签名的多签[{}]交易[{}], signers: {}", txType, trxTxHash, Arrays.toString(txPo.getSigners().toArray()));
                txPo.setStatus(MultiSignatureStatus.COMPLETED);
                // 保存解析的已签名完成的交易
                if (txInfo != null) {
                    htgStorageHelper.saveTxInfo(txInfo);
                } else {
                    htgStorageHelper.saveTxInfo(txPo);
                }
            } else {
                htgContext.logger().error("[失败]没有解析到完成多签的事件[{}]交易[{}]", txType, trxTxHash);
                txPo.setStatus(MultiSignatureStatus.FAILED);
            }

        }
        htUnconfirmedTxStorageService.save(txPo);
        if (!isLocalSent) {
            htgContext.logger().info("从{}网络解析到的交易[{}]，新添加入待确认队列", htgContext.getConfig().getSymbol(), trxTxHash);
            htgContext.UNCONFIRMED_TX_QUEUE().offer(txPo);
        }
        // 移除监听
        htListener.removeListeningTx(trxTxHash);
    }

}

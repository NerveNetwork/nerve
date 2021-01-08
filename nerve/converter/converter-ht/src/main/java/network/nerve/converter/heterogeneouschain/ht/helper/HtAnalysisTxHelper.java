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
package network.nerve.converter.heterogeneouschain.ht.helper;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.ht.constant.HtConstant;
import network.nerve.converter.heterogeneouschain.ht.context.HtContext;
import network.nerve.converter.heterogeneouschain.ht.core.HtWalletApi;
import network.nerve.converter.heterogeneouschain.ht.enums.MultiSignatureStatus;
import network.nerve.converter.heterogeneouschain.ht.helper.interfaces.IHtAnalysisTx;
import network.nerve.converter.heterogeneouschain.ht.listener.HtListener;
import network.nerve.converter.heterogeneouschain.ht.model.HtUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.ht.model.HtInput;
import network.nerve.converter.heterogeneouschain.ht.storage.HtUnconfirmedTxStorageService;
import network.nerve.converter.heterogeneouschain.ht.utils.HtUtil;
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
public class HtAnalysisTxHelper implements IHtAnalysisTx {

    @Autowired
    private HtUnconfirmedTxStorageService htUnconfirmedTxStorageService;
    @Autowired
    private HtERC20Helper htERC20Helper;
    @Autowired
    private HtWalletApi htWalletApi;
    @Autowired
    private HtListener htListener;
    @Autowired
    private HtParseTxHelper htParseTxHelper;
    @Autowired
    private HtStorageHelper htStorageHelper;
    @Autowired
    private HtPendingTxHelper htPendingTxHelper;

    @Override
    public void analysisTx(Transaction tx, long txTime, long blockHeight) throws Exception {
        boolean isDepositTx = false;
        boolean isBroadcastTx = false;
        String nerveTxHash = null;
        String htTxHash = tx.getHash();
        HtUnconfirmedTxPo po = new HtUnconfirmedTxPo();
        HeterogeneousChainTxType txType = null;
        do {
            if (tx == null) {
                return;
            }
            if (tx.getTo() == null) {
                return;
            }
            String from = tx.getFrom().toLowerCase();
            if (HtContext.FILTER_ACCOUNT_SET.contains(from)) {
                HtContext.logger().warn("过滤From[{}]的交易[{}]", from, tx.getHash());
                return;
            }
            // 广播的交易
            if (htListener.isListeningTx(tx.getHash())) {
                HtContext.logger().info("监听到本地广播到Huobi网络的交易[{}]", tx.getHash());
                isBroadcastTx = true;
                break;
            }
            tx.setFrom(from);
            tx.setTo(tx.getTo().toLowerCase());
            // 使用input判断是否为提现or变更交易，input.substring(0, 10).equals("0xaaaaaaaa")，保存解析的完整交易数据
            if (htListener.isListeningAddress(tx.getTo())) {
                HtInput htInput = htParseTxHelper.parseInput(tx.getInput());
                // 新的充值交易方式，调用多签合约的crossOut函数
                if (htInput.isDepositTx()) {
                    isDepositTx = this.parseNewDeposit(tx, po);
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
                    HtContext.logger().info("监听到Huobi网络的[{}]交易[{}], nerveTxHash: {}", txType, tx.getHash(), nerveTxHash);
                    break;
                }
            }

            // HT充值交易 条件: 固定接收地址, 金额大于0, 没有input
            if (htListener.isListeningAddress(tx.getTo()) &&
                    tx.getValue().compareTo(BigInteger.ZERO) > 0 &&
                    tx.getInput().equals(HtConstant.HEX_PREFIX)) {
                if (!htParseTxHelper.validationEthDeposit(tx)) {
                    HtContext.logger().error("[{}]不是HT充值交易[2]", htTxHash);
                    break;
                }
                isDepositTx = true;
                txType = HeterogeneousChainTxType.DEPOSIT;
                po.setIfContractAsset(false);
                po.setTo(tx.getTo());
                po.setValue(tx.getValue());
                po.setDecimals(HtConstant.HT_DECIMALS);
                po.setAssetId(HtConstant.HT_ASSET_ID);
                po.setNerveAddress(HtUtil.covertNerveAddressByEthTx(tx));
                HtContext.logger().info("监听到Huobi网络的HT充值交易[1][{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                        tx.getHash(),
                        from, po.getTo(), po.getValue(), po.getNerveAddress());
                break;
            }
            // ERC20充值交易
            if (htERC20Helper.isERC20(tx.getTo(), po)) {
                TransactionReceipt txReceipt = htWalletApi.getTxReceipt(htTxHash);
                if (htERC20Helper.hasERC20WithListeningAddress(txReceipt, po, toAddress -> htListener.isListeningAddress(toAddress))) {
                    // 检查是否是NERVE资产绑定的ERC20，是则检查多签合约内是否已经注册此定制的ERC20，否则充值异常
                    if (HtContext.getConverterCoreApi().isBoundHeterogeneousAsset(HtConstant.HT_CHAIN_ID, po.getAssetId())
                            && !htParseTxHelper.isMinterERC20(po.getContractAddress())) {
                        HtContext.logger().warn("[{}]不合法的Huobi网络的充值交易[5], ERC20[{}]已绑定NERVE资产，但合约内未注册", htTxHash, po.getContractAddress());
                        break;
                    }
                    isDepositTx = true;
                    txType = HeterogeneousChainTxType.DEPOSIT;
                    po.setNerveAddress(HtUtil.covertNerveAddressByEthTx(tx));
                    HtContext.logger().info("监听到Huobi网络的ERC20充值交易[1][{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                            tx.getHash(),
                            from, po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
                    break;
                }
            }

        } while (false);
        // 检查是否被Nerve网络确认，产生原因是当前节点解析eth交易比其他节点慢，其他节点确认了此交易后，当前节点才解析到此交易
        HtUnconfirmedTxPo txPoFromDB = null;
        if (isBroadcastTx || isDepositTx) {
            txPoFromDB = htUnconfirmedTxStorageService.findByTxHash(htTxHash);
            if (txPoFromDB != null && txPoFromDB.isDelete()) {
                HtContext.logger().info("HT交易[{}]已被[Nerve网络]确认，不再处理", htTxHash);
                return;
            }
        }
        // 如果是发出去的交易，例如提现和管理员变更，则补全交易信息
        if (isBroadcastTx) {
            if (txType == null) {
                HtInput htInput = htParseTxHelper.parseInput(tx.getInput());
                txType = htInput.getTxType();
                nerveTxHash = htInput.getNerveTxHash();
            }
            this.dealBroadcastTx(nerveTxHash, txType, tx, blockHeight, txTime, txPoFromDB);
            return;
        }
        // 充值交易放入需要确认30个区块的待确认交易队列中
        if (isDepositTx) {
            po.setTxType(txType);
            po.setTxHash(htTxHash);
            po.setBlockHeight(blockHeight);
            po.setFrom(tx.getFrom());
            po.setTxTime(txTime);
            // 保存解析的充值交易
            htStorageHelper.saveTxInfo(po);
            htUnconfirmedTxStorageService.save(po);
            HtContext.UNCONFIRMED_TX_QUEUE.offer(po);
            // 向NERVE网络发出充值待确认交易
            htPendingTxHelper.commitNervePendingDepositTx(po);
        }
    }

    private boolean parseNewDeposit(Transaction tx, HtUnconfirmedTxPo po) throws Exception {
        String htTxHash = tx.getHash();
        // 调用多签合约的crossOut函数的充值方式
        if (!htParseTxHelper.validationEthDepositByCrossOut(tx, po)) {
            HtContext.logger().error("[{}]不合法的Huobi网络的充值交易[3]", htTxHash);
            return false;
        }
        if (!po.isIfContractAsset()) {
            HtContext.logger().info("监听到Huobi网络的HT充值交易[2][{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                    tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress());
        } else {
            // 检查是否是NERVE资产绑定的ERC20，是则检查多签合约内是否已经注册此定制的ERC20，否则充值异常
            if (HtContext.getConverterCoreApi().isBoundHeterogeneousAsset(HtConstant.HT_CHAIN_ID, po.getAssetId())
                    && !htParseTxHelper.isMinterERC20(po.getContractAddress())) {
                HtContext.logger().warn("[{}]不合法的Huobi网络的充值交易[4], ERC20[{}]已绑定NERVE资产，但合约内未注册", htTxHash, po.getContractAddress());
                return false;
            }
            HtContext.logger().info("监听到Huobi网络的ERC20充值交易[2][{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                    tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
        }
        return true;
    }

    private void dealBroadcastTx(String nerveTxHash, HeterogeneousChainTxType txType, Transaction tx, long blockHeight, long txTime, HtUnconfirmedTxPo txPoFromDB) throws Exception {
        String htTxHash = tx.getHash();
        // 检查nerveTxHash是否合法
        if (HtContext.getConverterCoreApi().getNerveTx(nerveTxHash) == null) {
            HtContext.logger().warn("交易业务不合法[{}]，未找到NERVE交易，类型: {}, Key: {}", htTxHash, txType, nerveTxHash);
            return;
        }
        HtUnconfirmedTxPo txPo = txPoFromDB;
        boolean isLocalSent = true;
        if (txPo == null) {
            txPo = new HtUnconfirmedTxPo();
            isLocalSent = false;
        }
        txPo.setNerveTxHash(nerveTxHash);
        txPo.setTxHash(htTxHash);
        txPo.setTxType(txType);
        txPo.setBlockHeight(blockHeight);
        txPo.setTxTime(txTime);
        txPo.setFrom(tx.getFrom());
        // 判断交易是否成功，更改状态，解析交易的事件
        TransactionReceipt txReceipt = htWalletApi.getTxReceipt(htTxHash);
        if (txReceipt == null || !txReceipt.isStatusOK()) {
            txPo.setStatus(MultiSignatureStatus.FAILED);
        } else {
            HeterogeneousTransactionInfo txInfo = null;
            // 解析交易数据，补充基本信息
            switch (txType) {
                case WITHDRAW:
                    txInfo = htParseTxHelper.parseWithdrawTransaction(tx, txReceipt);
                    break;
                case CHANGE:
                    txInfo = htParseTxHelper.parseManagerChangeTransaction(tx, txReceipt);
                    break;
                case UPGRADE:
                    txInfo = htParseTxHelper.parseUpgradeTransaction(tx, txReceipt);
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
            // 设置为签名完成的交易，当多签存在时
            if (txPo.getSigners() != null && !txPo.getSigners().isEmpty()) {
                HtContext.logger().info("已完成签名的多签[{}]交易[{}], signers: {}", txType, htTxHash, Arrays.toString(txPo.getSigners().toArray()));
                txPo.setStatus(MultiSignatureStatus.COMPLETED);
                // 保存解析的已签名完成的交易
                if (txInfo != null) {
                    htStorageHelper.saveTxInfo(txInfo);
                } else {
                    htStorageHelper.saveTxInfo(txPo);
                }
            } else {
                HtContext.logger().error("[失败]没有解析到完成多签的事件[{}]交易[{}]", txType, htTxHash);
                txPo.setStatus(MultiSignatureStatus.FAILED);
            }

        }
        htUnconfirmedTxStorageService.save(txPo);
        if (!isLocalSent) {
            HtContext.logger().info("从Huobi网络解析到的交易[{}]，新添加入待确认队列", htTxHash);
            HtContext.UNCONFIRMED_TX_QUEUE.offer(txPo);
        }
        // 移除监听
        htListener.removeListeningTx(htTxHash);
    }

}

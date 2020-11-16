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
package network.nerve.converter.heterogeneouschain.bnb.helper;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.bnb.constant.BnbConstant;
import network.nerve.converter.heterogeneouschain.bnb.context.BnbContext;
import network.nerve.converter.heterogeneouschain.bnb.core.BNBWalletApi;
import network.nerve.converter.heterogeneouschain.bnb.enums.MultiSignatureStatus;
import network.nerve.converter.heterogeneouschain.bnb.helper.interfaces.IBnbAnalysisTx;
import network.nerve.converter.heterogeneouschain.bnb.listener.BnbListener;
import network.nerve.converter.heterogeneouschain.bnb.model.BnbUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.bnb.model.BnbInput;
import network.nerve.converter.heterogeneouschain.bnb.storage.BnbUnconfirmedTxStorageService;
import network.nerve.converter.heterogeneouschain.bnb.utils.BnbUtil;
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
public class BnbAnalysisTxHelper implements IBnbAnalysisTx {

    @Autowired
    private BnbUnconfirmedTxStorageService bnbUnconfirmedTxStorageService;
    @Autowired
    private BnbERC20Helper bnbERC20Helper;
    @Autowired
    private BNBWalletApi bnbWalletApi;
    @Autowired
    private BnbListener bnbListener;
    @Autowired
    private BnbParseTxHelper bnbParseTxHelper;
    @Autowired
    private BnbStorageHelper bnbStorageHelper;
    @Autowired
    private BnbPendingTxHelper bnbPendingTxHelper;

    @Override
    public void analysisTx(Transaction tx, long txTime, long blockHeight) throws Exception {
        boolean isDepositTx = false;
        boolean isBroadcastTx = false;
        String nerveTxHash = null;
        String bnbTxHash = tx.getHash();
        BnbUnconfirmedTxPo po = new BnbUnconfirmedTxPo();
        HeterogeneousChainTxType txType = null;
        do {
            if (tx == null) {
                return;
            }
            if (tx.getTo() == null) {
                return;
            }
            String from = tx.getFrom().toLowerCase();
            if (BnbContext.FILTER_ACCOUNT_SET.contains(from)) {
                BnbContext.logger().warn("过滤From[{}]的交易[{}]", from, tx.getHash());
                return;
            }
            // 广播的交易
            if (bnbListener.isListeningTx(tx.getHash())) {
                BnbContext.logger().info("监听到本地广播到Binance网络的交易[{}]", tx.getHash());
                isBroadcastTx = true;
                break;
            }
            tx.setFrom(from);
            tx.setTo(tx.getTo().toLowerCase());
            // 使用input判断是否为提现or变更交易，input.substring(0, 10).equals("0xaaaaaaaa")，保存解析的完整交易数据
            if (bnbListener.isListeningAddress(tx.getTo())) {
                BnbInput bnbInput = bnbParseTxHelper.parseInput(tx.getInput());
                // 新的充值交易方式，调用多签合约的crossOut函数
                if (bnbInput.isDepositTx()) {
                    isDepositTx = this.parseNewDeposit(tx, po);
                    if (isDepositTx) {
                        txType = HeterogeneousChainTxType.DEPOSIT;
                    }
                    break;
                }
                // 广播的交易
                if (bnbInput.isBroadcastTx()) {
                    isBroadcastTx = true;
                    txType = bnbInput.getTxType();
                    nerveTxHash = bnbInput.getNerveTxHash();
                    BnbContext.logger().info("监听到Binance网络的[{}]交易[{}], nerveTxHash: {}", txType, tx.getHash(), nerveTxHash);
                    break;
                }
            }

            // BNB充值交易 条件: 固定接收地址, 金额大于0, 没有input
            if (bnbListener.isListeningAddress(tx.getTo()) &&
                    tx.getValue().compareTo(BigInteger.ZERO) > 0 &&
                    tx.getInput().equals(BnbConstant.HEX_PREFIX)) {
                if (!bnbParseTxHelper.validationEthDeposit(tx)) {
                    BnbContext.logger().error("[{}]不是BNB充值交易[2]", bnbTxHash);
                    break;
                }
                isDepositTx = true;
                txType = HeterogeneousChainTxType.DEPOSIT;
                po.setIfContractAsset(false);
                po.setTo(tx.getTo());
                po.setValue(tx.getValue());
                po.setDecimals(BnbConstant.BNB_DECIMALS);
                po.setAssetId(BnbConstant.BNB_ASSET_ID);
                po.setNerveAddress(BnbUtil.covertNerveAddressByEthTx(tx));
                BnbContext.logger().info("监听到Binance网络的BNB充值交易[1][{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                        tx.getHash(),
                        from, po.getTo(), po.getValue(), po.getNerveAddress());
                break;
            }
            // ERC20充值交易
            if (bnbERC20Helper.isERC20(tx.getTo(), po)) {
                TransactionReceipt txReceipt = bnbWalletApi.getTxReceipt(bnbTxHash);
                if (bnbERC20Helper.hasERC20WithListeningAddress(txReceipt, po, toAddress -> bnbListener.isListeningAddress(toAddress))) {
                    // 检查是否是NERVE资产绑定的ERC20，是则检查多签合约内是否已经注册此定制的ERC20，否则充值异常
                    if (BnbContext.getConverterCoreApi().isBoundHeterogeneousAsset(BnbConstant.BNB_CHAIN_ID, po.getAssetId())
                            && !bnbParseTxHelper.isMinterERC20(po.getContractAddress())) {
                        BnbContext.logger().warn("[{}]不合法的Binance网络的充值交易[5], ERC20[{}]已绑定NERVE资产，但合约内未注册", bnbTxHash, po.getContractAddress());
                        break;
                    }
                    isDepositTx = true;
                    txType = HeterogeneousChainTxType.DEPOSIT;
                    po.setNerveAddress(BnbUtil.covertNerveAddressByEthTx(tx));
                    BnbContext.logger().info("监听到Binance网络的ERC20充值交易[1][{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                            tx.getHash(),
                            from, po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
                    break;
                }
            }

        } while (false);
        // 检查是否被Nerve网络确认，产生原因是当前节点解析eth交易比其他节点慢，其他节点确认了此交易后，当前节点才解析到此交易
        BnbUnconfirmedTxPo txPoFromDB = null;
        if (isBroadcastTx || isDepositTx) {
            txPoFromDB = bnbUnconfirmedTxStorageService.findByTxHash(bnbTxHash);
            if (txPoFromDB != null && txPoFromDB.isDelete()) {
                BnbContext.logger().info("BNB交易[{}]已被[Nerve网络]确认，不再处理", bnbTxHash);
                return;
            }
        }
        // 如果是发出去的交易，例如提现和管理员变更，则补全交易信息
        if (isBroadcastTx) {
            if (txType == null) {
                BnbInput bnbInput = bnbParseTxHelper.parseInput(tx.getInput());
                txType = bnbInput.getTxType();
                nerveTxHash = bnbInput.getNerveTxHash();
            }
            this.dealBroadcastTx(nerveTxHash, txType, tx, blockHeight, txTime, txPoFromDB);
            return;
        }
        // 充值交易放入需要确认30个区块的待确认交易队列中
        if (isDepositTx) {
            po.setTxType(txType);
            po.setTxHash(bnbTxHash);
            po.setBlockHeight(blockHeight);
            po.setFrom(tx.getFrom());
            po.setTxTime(txTime);
            // 保存解析的充值交易
            bnbStorageHelper.saveTxInfo(po);
            bnbUnconfirmedTxStorageService.save(po);
            BnbContext.UNCONFIRMED_TX_QUEUE.offer(po);
            // 向NERVE网络发出充值待确认交易
            bnbPendingTxHelper.commitNervePendingDepositTx(po);
        }
    }

    private boolean parseNewDeposit(Transaction tx, BnbUnconfirmedTxPo po) throws Exception {
        String bnbTxHash = tx.getHash();
        // 调用多签合约的crossOut函数的充值方式
        if (!bnbParseTxHelper.validationEthDepositByCrossOut(tx, po)) {
            BnbContext.logger().error("[{}]不合法的Binance网络的充值交易[3]", bnbTxHash);
            return false;
        }
        if (!po.isIfContractAsset()) {
            BnbContext.logger().info("监听到Binance网络的BNB充值交易[2][{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                    tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress());
        } else {
            // 检查是否是NERVE资产绑定的ERC20，是则检查多签合约内是否已经注册此定制的ERC20，否则充值异常
            if (BnbContext.getConverterCoreApi().isBoundHeterogeneousAsset(BnbConstant.BNB_CHAIN_ID, po.getAssetId())
                    && !bnbParseTxHelper.isMinterERC20(po.getContractAddress())) {
                BnbContext.logger().warn("[{}]不合法的Binance网络的充值交易[4], ERC20[{}]已绑定NERVE资产，但合约内未注册", bnbTxHash, po.getContractAddress());
                return false;
            }
            BnbContext.logger().info("监听到Binance网络的ERC20充值交易[2][{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                    tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
        }
        return true;
    }

    private void dealBroadcastTx(String nerveTxHash, HeterogeneousChainTxType txType, Transaction tx, long blockHeight, long txTime, BnbUnconfirmedTxPo txPoFromDB) throws Exception {
        String bnbTxHash = tx.getHash();
        // 检查nerveTxHash是否合法
        if (BnbContext.getConverterCoreApi().getNerveTx(nerveTxHash) == null) {
            BnbContext.logger().warn("交易业务不合法[{}]，未找到NERVE交易，类型: {}, Key: {}", bnbTxHash, txType, nerveTxHash);
            return;
        }
        BnbUnconfirmedTxPo txPo = txPoFromDB;
        boolean isLocalSent = true;
        if (txPo == null) {
            txPo = new BnbUnconfirmedTxPo();
            isLocalSent = false;
        }
        txPo.setNerveTxHash(nerveTxHash);
        txPo.setTxHash(bnbTxHash);
        txPo.setTxType(txType);
        txPo.setBlockHeight(blockHeight);
        txPo.setTxTime(txTime);
        txPo.setFrom(tx.getFrom());
        // 判断交易是否成功，更改状态，解析交易的事件
        TransactionReceipt txReceipt = bnbWalletApi.getTxReceipt(bnbTxHash);
        if (txReceipt == null || !txReceipt.isStatusOK()) {
            txPo.setStatus(MultiSignatureStatus.FAILED);
        } else {
            HeterogeneousTransactionInfo txInfo = null;
            // 解析交易数据，补充基本信息
            switch (txType) {
                case WITHDRAW:
                    txInfo = bnbParseTxHelper.parseWithdrawTransaction(tx, txReceipt);
                    break;
                case CHANGE:
                    txInfo = bnbParseTxHelper.parseManagerChangeTransaction(tx, txReceipt);
                    break;
                case UPGRADE:
                    txInfo = bnbParseTxHelper.parseUpgradeTransaction(tx, txReceipt);
                    break;
            }
            if (txInfo != null) {
                txInfo.setNerveTxHash(nerveTxHash);
                txInfo.setTxHash(bnbTxHash);
                txInfo.setTxType(txType);
                txInfo.setBlockHeight(blockHeight);
                txInfo.setTxTime(txTime);
                BeanUtils.copyProperties(txInfo, txPo);
            }
            // 设置为签名完成的交易，当多签存在时
            if (txPo.getSigners() != null && !txPo.getSigners().isEmpty()) {
                BnbContext.logger().info("已完成签名的多签[{}]交易[{}], signers: {}", txType, bnbTxHash, Arrays.toString(txPo.getSigners().toArray()));
                txPo.setStatus(MultiSignatureStatus.COMPLETED);
                // 保存解析的已签名完成的交易
                if (txInfo != null) {
                    bnbStorageHelper.saveTxInfo(txInfo);
                } else {
                    bnbStorageHelper.saveTxInfo(txPo);
                }
            } else {
                BnbContext.logger().error("[失败]没有解析到完成多签的事件[{}]交易[{}]", txType, bnbTxHash);
                txPo.setStatus(MultiSignatureStatus.FAILED);
            }

        }
        bnbUnconfirmedTxStorageService.save(txPo);
        if (!isLocalSent) {
            BnbContext.logger().info("从Binance网络解析到的交易[{}]，新添加入待确认队列", bnbTxHash);
            BnbContext.UNCONFIRMED_TX_QUEUE.offer(txPo);
        }
        // 移除监听
        bnbListener.removeListeningTx(bnbTxHash);
    }

}

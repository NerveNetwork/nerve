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

    //public HtgAnalysisTxHelper(BeanMap beanMap) {
    //    this.htUnconfirmedTxStorageService = (HtgUnconfirmedTxStorageService) beanMap.get("htUnconfirmedTxStorageService");
    //    this.htgERC20Helper = (HtgERC20Helper) beanMap.get("htgERC20Helper");
    //    this.htWalletApi = (HtgWalletApi) beanMap.get("htWalletApi");
    //    this.htListener = (HtgListener) beanMap.get("htListener");
    //    this.htgParseTxHelper = (HtgParseTxHelper) beanMap.get("htgParseTxHelper");
    //    this.htgStorageHelper = (HtgStorageHelper) beanMap.get("htgStorageHelper");
    //    this.htgPendingTxHelper = (HtgPendingTxHelper) beanMap.get("htgPendingTxHelper");
    //    this.htgContext = (HtgContext) beanMap.get("htgContext");
    //}

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
                htgContext.logger().warn("过滤From[{}]的交易[{}]", from, tx.getHash());
                return;
            }
            // 广播的交易
            if (htListener.isListeningTx(tx.getHash())) {
                htgContext.logger().info("监听到本地广播到{}网络的交易[{}]", htgContext.getConfig().getSymbol(), tx.getHash());
                isBroadcastTx = true;
                break;
            }
            tx.setFrom(from);
            tx.setTo(tx.getTo().toLowerCase());
            // 使用input判断是否为提现or变更交易，input.substring(0, 10).equals("0xaaaaaaaa")，保存解析的完整交易数据
            if (htListener.isListeningAddress(tx.getTo())) {
                HtgInput htInput = htgParseTxHelper.parseInput(tx.getInput());
                // 新的充值交易方式，调用多签合约的crossOut函数
                if (htInput.isDepositTx()) {
                    isDepositTx = this.parseNewDeposit(tx, po);
                    if (isDepositTx) {
                        txType = HeterogeneousChainTxType.DEPOSIT;
                    }
                    break;
                }
                // 新的充值交易方式，调用多签合约的crossOutII函数
                if (htInput.isDepositIITx()) {
                    isDepositTx = this.parseNewDepositII(tx, po);
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
                    htgContext.logger().info("监听到{}网络的[{}]交易[{}], nerveTxHash: {}", htgContext.getConfig().getSymbol(), txType, tx.getHash(), nerveTxHash);
                    break;
                }
            }

            // MainAsset充值交易 条件: 固定接收地址, 金额大于0, 没有input
            if (htListener.isListeningAddress(tx.getTo()) &&
                    tx.getValue().compareTo(BigInteger.ZERO) > 0 &&
                    tx.getInput().equals(HtgConstant.HEX_PREFIX)) {
                if (!htgParseTxHelper.validationEthDeposit(tx)) {
                    htgContext.logger().error("[{}]不是MainAsset充值交易[2]", htTxHash);
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
                htgContext.logger().info("监听到{}网络的MainAsset充值交易[1][{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                        htgContext.getConfig().getSymbol(), tx.getHash(),
                        from, po.getTo(), po.getValue(), po.getNerveAddress());
                break;
            }
            // ERC20充值交易
            if (htgERC20Helper.isERC20(tx.getTo(), po) && htgERC20Helper.hasERC20WithListeningAddress(tx.getInput(), toAddress -> htListener.isListeningAddress(toAddress))) {
                TransactionReceipt txReceipt = htWalletApi.getTxReceipt(htTxHash);
                if (htgERC20Helper.hasERC20WithListeningAddress(txReceipt, po, toAddress -> htListener.isListeningAddress(toAddress))) {
                    // 检查是否是NERVE资产绑定的ERC20，是则检查多签合约内是否已经注册此定制的ERC20，否则充值异常
                    if (htgContext.getConverterCoreApi().isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                            && !htgParseTxHelper.isMinterERC20(po.getContractAddress())) {
                        htgContext.logger().warn("[{}]不合法的{}网络的充值交易[5], ERC20[{}]已绑定NERVE资产，但合约内未注册", htTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
                        break;
                    }
                    isDepositTx = true;
                    txType = HeterogeneousChainTxType.DEPOSIT;
                    po.setNerveAddress(HtgUtil.covertNerveAddressByEthTx(tx, htgContext.NERVE_CHAINID()));
                    htgContext.logger().info("监听到{}网络的ERC20充值交易[1][{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                            htgContext.getConfig().getSymbol(), tx.getHash(),
                            from, po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
                    break;
                }
            }

        } while (false);
        if (isDepositTx && !htgContext.getConverterCoreApi().validNerveAddress(po.getNerveAddress())) {
            htgContext.logger().warn("[充值地址异常] 交易[{}], [0]充值地址: {}", htTxHash, po.getNerveAddress());
            return;
        }
        // 检查是否被Nerve网络确认，产生原因是当前节点解析eth交易比其他节点慢，其他节点确认了此交易后，当前节点才解析到此交易
        HtgUnconfirmedTxPo txPoFromDB = null;
        if (isBroadcastTx || isDepositTx) {
            txPoFromDB = htUnconfirmedTxStorageService.findByTxHash(htTxHash);
            if (txPoFromDB != null && txPoFromDB.isDelete()) {
                htgContext.logger().info("{}交易[{}]已被[Nerve网络]确认，不再处理", htgContext.getConfig().getSymbol(), htTxHash);
                return;
            }
        }
        // 如果是发出去的交易，例如提现和管理员变更，则补全交易信息
        if (isBroadcastTx) {
            if (txType == null) {
                HtgInput htInput = htgParseTxHelper.parseInput(tx.getInput());
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
            htgStorageHelper.saveTxInfo(po);
            htUnconfirmedTxStorageService.save(po);
            htgContext.UNCONFIRMED_TX_QUEUE().offer(po);
            // 向NERVE网络发出充值待确认交易
            htgPendingTxHelper.commitNervePendingDepositTx(po);
        }
    }

    private boolean parseNewDeposit(Transaction tx, HtgUnconfirmedTxPo po) throws Exception {
        String htTxHash = tx.getHash();
        // 调用多签合约的crossOut函数的充值方式
        if (!htgParseTxHelper.validationEthDepositByCrossOut(tx, po)) {
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
                    && !htgParseTxHelper.isMinterERC20(po.getContractAddress())) {
                htgContext.logger().warn("[{}]不合法的{}网络的充值交易[4], ERC20[{}]已绑定NERVE资产，但合约内未注册", htTxHash, htgContext.getConfig().getSymbol(), po.getContractAddress());
                return false;
            }
            htgContext.logger().info("监听到{}网络的ERC20充值交易[2][{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                    htgContext.getConfig().getSymbol(), tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
        }
        return true;
    }

    private boolean parseNewDepositII(Transaction tx, HtgUnconfirmedTxPo po) throws Exception {
        String htTxHash = tx.getHash();
        // 调用多签合约的crossOutII函数的充值方式
        if (!htgParseTxHelper.validationEthDepositByCrossOutII(tx, null, po)) {
            htgContext.logger().error("[{}]不合法的{}网络的充值II交易[0]", htTxHash, htgContext.getConfig().getSymbol());
            return false;
        }
        // 检查是否是NERVE资产绑定的ERC20，是则检查多签合约内是否已经注册此定制的ERC20，否则充值异常
        if (po.isIfContractAsset() && htgContext.getConverterCoreApi().isBoundHeterogeneousAsset(htgContext.getConfig().getChainId(), po.getAssetId())
                && !htgParseTxHelper.isMinterERC20(po.getContractAddress())) {
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

    private void dealBroadcastTx(String nerveTxHash, HeterogeneousChainTxType txType, Transaction tx, long blockHeight, long txTime, HtgUnconfirmedTxPo txPoFromDB) throws Exception {
        String htTxHash = tx.getHash();
        // 检查nerveTxHash是否合法
        if (htgContext.getConverterCoreApi().getNerveTx(nerveTxHash) == null) {
            htgContext.logger().warn("交易业务不合法[{}]，未找到NERVE交易，类型: {}, Key: {}", htTxHash, txType, nerveTxHash);
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
        // 判断交易是否成功，更改状态，解析交易的事件
        TransactionReceipt txReceipt = htWalletApi.getTxReceipt(htTxHash);
        if (txReceipt == null || !txReceipt.isStatusOK()) {
            txPo.setStatus(MultiSignatureStatus.FAILED);
        } else {
            HeterogeneousTransactionInfo txInfo = null;
            // 解析交易数据，补充基本信息
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
            // 设置为签名完成的交易，当多签存在时
            if (txPo.getSigners() != null && !txPo.getSigners().isEmpty()) {
                htgContext.logger().info("已完成签名的多签[{}]交易[{}], signers: {}", txType, htTxHash, Arrays.toString(txPo.getSigners().toArray()));
                txPo.setStatus(MultiSignatureStatus.COMPLETED);
                // 保存解析的已签名完成的交易
                if (txInfo != null) {
                    htgStorageHelper.saveTxInfo(txInfo);
                } else {
                    htgStorageHelper.saveTxInfo(txPo);
                }
            } else {
                htgContext.logger().error("[失败]没有解析到完成多签的事件[{}]交易[{}]", txType, htTxHash);
                txPo.setStatus(MultiSignatureStatus.FAILED);
            }

        }
        htUnconfirmedTxStorageService.save(txPo);
        if (!isLocalSent) {
            htgContext.logger().info("从{}网络解析到的交易[{}]，新添加入待确认队列", htgContext.getConfig().getSymbol(), htTxHash);
            htgContext.UNCONFIRMED_TX_QUEUE().offer(txPo);
        }
        // 移除监听
        htListener.removeListeningTx(htTxHash);
    }

}

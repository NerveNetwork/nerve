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
package network.nerve.converter.heterogeneouschain.ethII.helper;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import network.nerve.converter.heterogeneouschain.eth.enums.MultiSignatureStatus;
import network.nerve.converter.heterogeneouschain.eth.helper.EthERC20Helper;
import network.nerve.converter.heterogeneouschain.eth.helper.EthResendHelper;
import network.nerve.converter.heterogeneouschain.eth.helper.EthStorageHelper;
import network.nerve.converter.heterogeneouschain.eth.helper.interfaces.IEthAnalysisTx;
import network.nerve.converter.heterogeneouschain.eth.listener.EthListener;
import network.nerve.converter.heterogeneouschain.eth.model.EthInput;
import network.nerve.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.eth.storage.EthUnconfirmedTxStorageService;
import network.nerve.converter.heterogeneouschain.eth.utils.EthUtil;
import network.nerve.converter.heterogeneouschain.ethII.context.EthIIContext;
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
public class EthIIAnalysisTxHelper implements IEthAnalysisTx {

    @Autowired
    private EthUnconfirmedTxStorageService ethUnconfirmedTxStorageService;
    @Autowired
    private EthERC20Helper ethERC20Helper;
    @Autowired
    private ETHWalletApi ethWalletApi;
    @Autowired
    private EthListener ethListener;
    @Autowired
    private EthIIParseTxHelper ethIIParseTxHelper;
    @Autowired
    private EthStorageHelper ethStorageHelper;
    @Autowired
    private EthResendHelper ethResendHelper;
    @Autowired
    private EthIIPendingTxHelper ethIIPendingTxHelper;

    @Override
    public void analysisTx(Transaction tx, long txTime, long blockHeight) throws Exception {
        boolean isDepositTx = false;
        boolean isBroadcastTx = false;
        String nerveTxHash = null;
        String ethTxHash = tx.getHash();
        EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
        HeterogeneousChainTxType txType = null;
        do {
            if (tx == null) {
                return;
            }
            if (tx.getTo() == null) {
                return;
            }
            String from = tx.getFrom().toLowerCase();
            if (EthIIContext.FILTER_ACCOUNT_SET.contains(from)) {
                EthContext.logger().warn("过滤From[{}]的交易[{}]", from, tx.getHash());
                return;
            }
            // 广播的交易
            if (ethListener.isListeningTx(tx.getHash())) {
                EthContext.logger().info("监听到本地广播到Ethereum网络的交易[{}]", tx.getHash());
                isBroadcastTx = true;
                break;
            }
            tx.setFrom(from);
            tx.setTo(tx.getTo().toLowerCase());
            // 使用input判断是否为提现or变更交易，input.substring(0, 10).equals("0xaaaaaaaa")，保存解析的完整交易数据
            if (ethListener.isListeningAddress(tx.getTo())) {
                EthInput ethInput = ethIIParseTxHelper.parseInput(tx.getInput());
                // 新的充值交易方式，调用多签合约的crossOut函数
                if (ethInput.isDepositTx()) {
                    isDepositTx = this.parseNewDeposit(tx, po);
                    if (isDepositTx) {
                        txType = HeterogeneousChainTxType.DEPOSIT;
                    }
                    break;
                }
                // 广播的交易
                if (ethInput.isBroadcastTx()) {
                    isBroadcastTx = true;
                    txType = ethInput.getTxType();
                    nerveTxHash = ethInput.getNerveTxHash();
                    EthContext.logger().info("监听到Ethereum网络的[{}]交易[{}], nerveTxHash: {}", txType, tx.getHash(), nerveTxHash);
                    break;
                }
            }

            // ETH充值交易 条件: 固定接收地址, 金额大于0, 没有input
            if (ethListener.isListeningAddress(tx.getTo()) &&
                    tx.getValue().compareTo(BigInteger.ZERO) > 0 &&
                    tx.getInput().equals(EthConstant.HEX_PREFIX)) {
                if (!ethIIParseTxHelper.validationEthDeposit(tx)) {
                    EthContext.logger().error("[{}]不是ETH充值交易[2]", ethTxHash);
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
                EthContext.logger().info("监听到Ethereum网络的ETH充值交易[1][{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                        tx.getHash(),
                        from, po.getTo(), po.getValue(), po.getNerveAddress());
                break;
            }
            // ERC20充值交易
            if (ethERC20Helper.isERC20(tx.getTo(), po)) {
                TransactionReceipt txReceipt = ethWalletApi.getTxReceipt(ethTxHash);
                if (ethERC20Helper.hasERC20WithListeningAddress(txReceipt, po, toAddress -> ethListener.isListeningAddress(toAddress))) {
                    // 检查是否是NERVE资产绑定的ERC20，是则检查多签合约内是否已经注册此定制的ERC20，否则充值异常
                    if (EthContext.getConverterCoreApi().isBoundHeterogeneousAsset(EthConstant.ETH_CHAIN_ID, po.getAssetId())
                            && !ethIIParseTxHelper.isMinterERC20(po.getContractAddress())) {
                        EthContext.logger().warn("[{}]不合法的Ethereum网络的充值交易[5], ERC20[{}]已绑定NERVE资产，但合约内未注册", ethTxHash, po.getContractAddress());
                        break;
                    }
                    isDepositTx = true;
                    txType = HeterogeneousChainTxType.DEPOSIT;
                    po.setNerveAddress(EthUtil.covertNerveAddressByEthTx(tx));
                    EthContext.logger().info("监听到Ethereum网络的ERC20充值交易[1][{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                            tx.getHash(),
                            from, po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
                    break;
                }
            }

        } while (false);
        // 检查是否被Nerve网络确认，产生原因是当前节点解析eth交易比其他节点慢，其他节点确认了此交易后，当前节点才解析到此交易
        EthUnconfirmedTxPo txPoFromDB = null;
        if (isBroadcastTx || isDepositTx) {
            txPoFromDB = ethUnconfirmedTxStorageService.findByTxHash(ethTxHash);
            if (txPoFromDB != null && txPoFromDB.isDelete()) {
                EthContext.logger().info("ETH交易[{}]已被[Nerve网络]确认，不再处理", ethTxHash);
                return;
            }
        }
        // 如果是发出去的交易，例如提现和管理员变更，则补全交易信息
        if (isBroadcastTx) {
            if (txType == null) {
                EthInput ethInput = ethIIParseTxHelper.parseInput(tx.getInput());
                txType = ethInput.getTxType();
                nerveTxHash = ethInput.getNerveTxHash();
            }
            this.dealBroadcastTx(nerveTxHash, txType, tx, blockHeight, txTime, txPoFromDB);
            return;
        }
        // 充值交易放入需要确认30个区块的待确认交易队列中
        if (isDepositTx) {
            po.setTxType(txType);
            po.setTxHash(ethTxHash);
            po.setBlockHeight(blockHeight);
            po.setFrom(tx.getFrom());
            po.setTxTime(txTime);
            // 保存解析的充值交易
            ethStorageHelper.saveTxInfo(po);
            ethUnconfirmedTxStorageService.save(po);
            EthContext.UNCONFIRMED_TX_QUEUE.offer(po);
            // 向NERVE网络发出充值待确认交易
            ethIIPendingTxHelper.commitNervePendingDepositTx(po);
        }
    }

    private boolean parseNewDeposit(Transaction tx, EthUnconfirmedTxPo po) throws Exception {
        String ethTxHash = tx.getHash();
        // 调用多签合约的crossOut函数的充值方式
        if (!ethIIParseTxHelper.validationEthDepositByCrossOut(tx, po)) {
            EthContext.logger().error("[{}]不合法的Ethereum网络的充值交易[3]", ethTxHash);
            return false;
        }
        if (!po.isIfContractAsset()) {
            EthContext.logger().info("监听到Ethereum网络的ETH充值交易[2][{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                    tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress());
        } else {
            // 检查是否是NERVE资产绑定的ERC20，是则检查多签合约内是否已经注册此定制的ERC20，否则充值异常
            if (EthContext.getConverterCoreApi().isBoundHeterogeneousAsset(EthConstant.ETH_CHAIN_ID, po.getAssetId())
                    && !ethIIParseTxHelper.isMinterERC20(po.getContractAddress())) {
                EthContext.logger().warn("[{}]不合法的Ethereum网络的充值交易[4], ERC20[{}]已绑定NERVE资产，但合约内未注册", ethTxHash, po.getContractAddress());
                return false;
            }
            EthContext.logger().info("监听到Ethereum网络的ERC20充值交易[2][{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                    tx.getHash(),
                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
        }
        return true;
    }

    private void dealBroadcastTx(String nerveTxHash, HeterogeneousChainTxType txType, Transaction tx, long blockHeight, long txTime, EthUnconfirmedTxPo txPoFromDB) throws Exception {
        String ethTxHash = tx.getHash();
        // 检查nerveTxHash是否合法
        if (EthContext.getConverterCoreApi().getNerveTx(nerveTxHash) == null) {
            EthContext.logger().warn("交易业务不合法[{}]，未找到NERVE交易，类型: {}, Key: {}", ethTxHash, txType, nerveTxHash);
            return;
        }
        EthUnconfirmedTxPo txPo = txPoFromDB;
        boolean isLocalSent = true;
        if (txPo == null) {
            txPo = new EthUnconfirmedTxPo();
            isLocalSent = false;
        }
        txPo.setNerveTxHash(nerveTxHash);
        txPo.setTxHash(ethTxHash);
        txPo.setTxType(txType);
        txPo.setBlockHeight(blockHeight);
        txPo.setTxTime(txTime);
        txPo.setFrom(tx.getFrom());
        // 判断交易是否成功，更改状态，解析交易的事件
        TransactionReceipt txReceipt = ethWalletApi.getTxReceipt(ethTxHash);
        if (txReceipt == null || !txReceipt.isStatusOK()) {
            txPo.setStatus(MultiSignatureStatus.FAILED);
        } else {
            HeterogeneousTransactionInfo txInfo = null;
            // 解析交易数据，补充基本信息
            switch (txType) {
                case WITHDRAW:
                    txInfo = ethIIParseTxHelper.parseWithdrawTransaction(tx, txReceipt);
                    break;
                case CHANGE:
                    txInfo = ethIIParseTxHelper.parseManagerChangeTransaction(tx, txReceipt);
                    break;
                case UPGRADE:
                    txInfo = ethIIParseTxHelper.parseUpgradeTransaction(tx, txReceipt);
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
            // 设置为签名完成的交易，当多签存在时
            if (txPo.getSigners() != null && !txPo.getSigners().isEmpty()) {
                EthContext.logger().info("已完成签名的多签[{}]交易[{}], signers: {}", txType, ethTxHash, Arrays.toString(txPo.getSigners().toArray()));
                txPo.setStatus(MultiSignatureStatus.COMPLETED);
                // 保存解析的已签名完成的交易
                if (txInfo != null) {
                    ethStorageHelper.saveTxInfo(txInfo);
                } else {
                    ethStorageHelper.saveTxInfo(txPo);
                }
            } else {
                EthContext.logger().error("[失败]没有解析到完成多签的事件[{}]交易[{}]", txType, ethTxHash);
                txPo.setStatus(MultiSignatureStatus.FAILED);
            }

        }
        ethUnconfirmedTxStorageService.save(txPo);
        if (!isLocalSent) {
            EthContext.logger().info("从Ethereum网络解析到的交易[{}]，新添加入待确认队列", ethTxHash);
            EthContext.UNCONFIRMED_TX_QUEUE.offer(txPo);
        }
        // 移除监听
        ethListener.removeListeningTx(ethTxHash);
    }

}

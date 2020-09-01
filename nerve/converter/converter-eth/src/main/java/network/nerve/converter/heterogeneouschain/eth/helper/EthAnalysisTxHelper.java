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
package network.nerve.converter.heterogeneouschain.eth.helper;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.eth.constant.EthConstant;
import network.nerve.converter.heterogeneouschain.eth.context.EthContext;
import network.nerve.converter.heterogeneouschain.eth.core.ETHWalletApi;
import network.nerve.converter.heterogeneouschain.eth.enums.MultiSignatureStatus;
import network.nerve.converter.heterogeneouschain.eth.listener.EthListener;
import network.nerve.converter.heterogeneouschain.eth.model.EthInput;
import network.nerve.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import network.nerve.converter.heterogeneouschain.eth.storage.EthUnconfirmedTxStorageService;
import network.nerve.converter.heterogeneouschain.eth.utils.EthUtil;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import org.springframework.beans.BeanUtils;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Mimi
 * @date: 2020-03-20
 */
@Component
public class EthAnalysisTxHelper {

    private Set<String> analysisTxHashSet = ConcurrentHashMap.newKeySet();
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

            // 广播的交易
            if (ethListener.isListeningTx(tx.getHash())) {
                EthContext.logger().info("监听到本地广播到ETH网络的交易[{}]", tx.getHash());
                isBroadcastTx = true;
                break;
            }

            tx.setFrom(tx.getFrom().toLowerCase());
            tx.setTo(tx.getTo().toLowerCase());
            // 使用input判断是否为提现or变更交易，input.substring(0, 10).equals("0xaaaaaaaa")，保存解析的完整交易数据
            // 广播的交易
            if (ethListener.isListeningAddress(tx.getTo())) {
                EthInput ethInput = this.parseInput(tx.getInput());
                if(ethInput.isBroadcastTx()) {
                    isBroadcastTx = true;
                    txType = ethInput.getTxType();
                    nerveTxHash = ethInput.getNerveTxHash();
                    EthContext.logger().info("监听到ETH网络的[{}]交易[{}], nerveTxHash: {}", txType, tx.getHash(), nerveTxHash);
                    break;
                }
            }

            // ETH充值交易 条件: 固定接收地址, 金额大于0, 没有input
            if (ethListener.isListeningAddress(tx.getTo()) &&
                    tx.getValue().compareTo(BigInteger.ZERO) > 0 &&
                    tx.getInput().equals(EthConstant.HEX_PREFIX)) {
                if (!ethParseTxHelper.validationEthDeposit(tx)) {
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
                EthContext.logger().info("监听到ETH网络的ETH充值交易[{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                        tx.getHash(),
                        tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress());
                break;
            }
            // ERC20充值交易
            if (ethERC20Helper.isERC20(tx.getTo(), po)) {
                TransactionReceipt txReceipt = ethWalletApi.getTxReceipt(ethTxHash);
                if (ethERC20Helper.hasERC20WithListeningAddress(txReceipt, po, toAddress -> ethListener.isListeningAddress(toAddress))) {
                    isDepositTx = true;
                    txType = HeterogeneousChainTxType.DEPOSIT;
                    po.setNerveAddress(EthUtil.covertNerveAddressByEthTx(tx));
                    EthContext.logger().info("监听到ETH网络的ERC20充值交易[{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                            tx.getHash(),
                            tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
                    break;
                }
            }

        } while (false);
        // 检查是否被Nerve网络确认，产生原因是当前节点解析eth交易比其他节点慢，其他节点确认了此交易后，当前节点才解析到此交易
        EthUnconfirmedTxPo txPoFromDB = null;
        if(isBroadcastTx || isDepositTx) {
            txPoFromDB = ethUnconfirmedTxStorageService.findByTxHash(ethTxHash);
            if(txPoFromDB != null && txPoFromDB.isDelete()) {
                EthContext.logger().info("ETH交易[{}]已被[Nerve网络]确认，不再处理", ethTxHash);
                return;
            }
        }
        // 如果是发出去的交易，例如提现和管理员变更，则补全交易信息
        if (isBroadcastTx) {
            if (txType == null) {
                EthInput ethInput = this.parseInput(tx.getInput());
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
        }
    }

    public boolean constainHash(String hash) {
        return analysisTxHashSet.contains(hash);
    }

    public boolean removeHash(String hash) {
        return analysisTxHashSet.remove(hash);
    }

    public boolean addHash(String hash) {
        return analysisTxHashSet.add(hash);
    }

    public void clearHash() {
        analysisTxHashSet.clear();
    }

    private void dealBroadcastTx(String nerveTxHash, HeterogeneousChainTxType txType, Transaction tx, long blockHeight, long txTime, EthUnconfirmedTxPo txPoFromDB) throws Exception {
        String ethTxHash = tx.getHash();
        // 检查nerveTxHash是否合法
        String realNerveTxHash = nerveTxHash;
        if (nerveTxHash.startsWith(EthConstant.ETH_RECOVERY_I) || nerveTxHash.startsWith(EthConstant.ETH_RECOVERY_II)) {
            realNerveTxHash = nerveTxHash.substring(EthConstant.ETH_RECOVERY_I.length());
            txType = HeterogeneousChainTxType.RECOVERY;
        }
        if(EthContext.getConverterCoreApi().getNerveTx(realNerveTxHash) == null) {
            EthContext.logger().warn("交易业务不合法[{}]，未找到NERVE交易，类型: {}, Key: {}", ethTxHash, txType, realNerveTxHash);
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
        // 判断是否为签名完成的交易，更改状态，解析交易的多签地址列表
        TransactionReceipt txReceipt = ethWalletApi.getTxReceipt(ethTxHash);
        if (txReceipt == null || !txReceipt.isStatusOK()) {
            // 当前节点发出的交易，交易失败，重发三十次，仍然失败的话，则丢弃交易
            if (ethResendHelper.currentNodeSent(ethTxHash) && ethResendHelper.canResend(nerveTxHash)) {
                ethResendHelper.increase(nerveTxHash);
                txPo.setStatus(MultiSignatureStatus.RESEND);
            } else {
                txPo.setStatus(MultiSignatureStatus.FAILED);
            }
        } else {
            // 先默认设置为正在多签的交易
            txPo.setStatus(MultiSignatureStatus.DOING);
            HeterogeneousTransactionInfo txInfo = null;
            // 以此判断是否为首次保存
            if (StringUtils.isBlank(txPo.getFrom())) {
                // 解析交易数据，补充基本信息、多签列表信息
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
                // 解析交易多签列表
                txPo.setSigners(ethParseTxHelper.parseSigners(txReceipt));
            }
            // 设置为签名完成的交易，当多签存在时
            if (txPo.getSigners() != null && !txPo.getSigners().isEmpty()) {
                EthContext.logger().info("已完成签名的多签[{}]交易[{}], signers: {}",
                        txType,
                        ethTxHash, Arrays.toString(txPo.getSigners().toArray()));
                txPo.setStatus(MultiSignatureStatus.COMPLETED);
            }
            // 保存解析的已签名完成的交易
            if (txInfo != null) {
                ethStorageHelper.saveTxInfo(txInfo);
            } else {
                ethStorageHelper.saveTxInfo(txPo);
            }
        }
        ethUnconfirmedTxStorageService.save(txPo);
        if (!isLocal) {
            EthContext.logger().info("从eth网络解析到的交易[{}]，新添加入待确认队列", ethTxHash);
            EthContext.UNCONFIRMED_TX_QUEUE.offer(txPo);
        }
        // 移除监听
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

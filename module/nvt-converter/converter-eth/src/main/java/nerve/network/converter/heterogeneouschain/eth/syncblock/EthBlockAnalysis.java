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
package nerve.network.converter.heterogeneouschain.eth.syncblock;

import nerve.network.converter.enums.HeterogeneousChainTxType;
import nerve.network.converter.heterogeneouschain.eth.callback.EthCallBackManager;
import nerve.network.converter.heterogeneouschain.eth.constant.EthConstant;
import nerve.network.converter.heterogeneouschain.eth.context.EthContext;
import nerve.network.converter.heterogeneouschain.eth.core.ETHWalletApi;
import nerve.network.converter.heterogeneouschain.eth.enums.MultiSignatureStatus;
import nerve.network.converter.heterogeneouschain.eth.helper.EthERC20Helper;
import nerve.network.converter.heterogeneouschain.eth.helper.EthLocalBlockHelper;
import nerve.network.converter.heterogeneouschain.eth.helper.EthParseTxHelper;
import nerve.network.converter.heterogeneouschain.eth.helper.EthStorageHelper;
import nerve.network.converter.heterogeneouschain.eth.listener.EthListener;
import nerve.network.converter.heterogeneouschain.eth.model.EthSimpleBlockHeader;
import nerve.network.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import nerve.network.converter.heterogeneouschain.eth.storage.EthTxRelationStorageService;
import nerve.network.converter.heterogeneouschain.eth.storage.EthUnconfirmedTxStorageService;
import nerve.network.converter.heterogeneouschain.eth.utils.EthUtil;
import nerve.network.converter.model.bo.HeterogeneousTransactionInfo;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import org.springframework.beans.BeanUtils;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static nerve.network.converter.heterogeneouschain.eth.context.EthContext.logger;

/**
 * 解析以太坊区块，监听指定地址和指定交易并回调Nerve核心
 *
 * @author: Chino
 * @date: 2020-02-20
 */
@Component
public class EthBlockAnalysis {

    @Autowired
    private EthListener ethListener;
    @Autowired
    private EthCallBackManager ethCallBackManager;
    @Autowired
    private EthTxRelationStorageService ethTxRelationStorageService;
    @Autowired
    private ETHWalletApi ethWalletApi;
    @Autowired
    private EthUnconfirmedTxStorageService ethUnconfirmedTxStorageService;
    @Autowired
    private EthStorageHelper ethStorageHelper;
    @Autowired
    private EthERC20Helper ethERC20Helper;
    @Autowired
    private EthLocalBlockHelper ethLocalBlockHelper;
    @Autowired
    private EthParseTxHelper ethParseTxHelper;

    /**
     * 解析以太坊区块
     */
    public void analysisEthBlock(EthBlock.Block block) throws Exception {
        List<EthBlock.TransactionResult> ethTransactionResults = block.getTransactions();
        long blockHeight = block.getNumber().longValue();
        int size;
        if (ethTransactionResults != null && (size = ethTransactionResults.size()) > 0) {
            long txTime = block.getTimestamp().longValue();
            for (int i = 0; i < size; i++) {
                org.web3j.protocol.core.methods.response.Transaction tx = (org.web3j.protocol.core.methods.response.Transaction) ethTransactionResults.get(i).get();
                boolean addUnconfirmTx = false;
                boolean isBroadcastTx = false;
                String ethTxHash = tx.getHash();
                EthUnconfirmedTxPo po = new EthUnconfirmedTxPo();
                HeterogeneousChainTxType txType = null;
                do {
                    if (tx == null) {
                        break;
                    }
                    if(tx.getTo() == null) {
                        break;
                    }

                    // 广播的交易
                    if(ethListener.isListeningTx(tx.getHash())) {
                        logger().info("监听到本地广播到ETH网络的交易[{}]", tx.getHash());
                        isBroadcastTx = true;
                        break;
                    }

                    tx.setFrom(tx.getFrom().toLowerCase());
                    tx.setTo(tx.getTo().toLowerCase());
                    // 使用input判断是否为提现or变更交易，input.substring(0, 10).equals("0xaaaaaaaa")，保存解析的完整交易数据
                    // 广播的交易
                    String input, methodHash;
                    if(ethListener.isListeningAddress(tx.getTo()) && (input = tx.getInput()).length() >= 10) {
                        if((methodHash = input.substring(0, 10)).equals(EthConstant.METHOD_HASH_CREATEORSIGNWITHDRAW)) {
                            isBroadcastTx = true;
                            txType = HeterogeneousChainTxType.WITHDRAW;
                            logger().info("监听到ETH网络的提现交易[{}]", tx.getHash());
                            break;
                        }
                        if(methodHash.equals(EthConstant.METHOD_HASH_CREATEORSIGNMANAGERCHANGE)) {
                            isBroadcastTx = true;
                            txType = HeterogeneousChainTxType.CHANGE;
                            logger().info("监听到ETH网络的管理员变更交易[{}]", tx.getHash());
                            break;
                        }
                    }

                    // ETH充值交易 条件: 固定接收地址, 金额大于0, 没有input
                    if (ethListener.isListeningAddress(tx.getTo()) &&
                            tx.getValue().compareTo(BigInteger.ZERO) > 0 &&
                            tx.getInput().equals(EthConstant.HEX_PREFIX)) {
                        addUnconfirmTx = true;
                        txType = HeterogeneousChainTxType.DEPOSIT;
                        po.setIfContractAsset(false);
                        po.setTo(tx.getTo());
                        po.setValue(tx.getValue());
                        po.setDecimals(EthConstant.ETH_DECIMALS);
                        po.setAssetId(EthConstant.ETH_ASSET_ID);
                        po.setNerveAddress(EthUtil.covertNerveAddressByEthTx(tx));
                        logger().info("监听到ETH网络的ETH充值交易[{}], from: {}, to: {}, value: {}, nerveAddress: {}",
                                tx.getHash(),
                                tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress());
                        break;
                    }
                    // ERC20充值交易
                    if (ethERC20Helper.isERC20(tx.getTo(), po)) {
                        TransactionReceipt txReceipt = ethWalletApi.getTxReceipt(ethTxHash);
                        if (ethERC20Helper.hasERC20WithListeningAddress(txReceipt, po, toAddress -> ethListener.isListeningAddress(toAddress))) {
                            addUnconfirmTx = true;
                            txType = HeterogeneousChainTxType.DEPOSIT;
                            po.setNerveAddress(EthUtil.covertNerveAddressByEthTx(tx));
                            logger().info("监听到ETH网络的ETH充值交易[{}], from: {}, to: {}, value: {}, nerveAddress: {}, contract: {}, decimals: {}",
                                    tx.getHash(),
                                    tx.getFrom(), po.getTo(), po.getValue(), po.getNerveAddress(), po.getContractAddress(), po.getDecimals());
                            break;
                        }
                    }

                } while (false);
                // 如果是发出去的交易，例如提现和管理员变更，则补全交易信息
                if (isBroadcastTx) {
                    if(txType == null) {
                        String methodHash = tx.getInput().substring(0, 10);
                        if(methodHash.equals(EthConstant.METHOD_HASH_CREATEORSIGNWITHDRAW)) {
                            txType = HeterogeneousChainTxType.WITHDRAW;
                        } else if(methodHash.equals(EthConstant.METHOD_HASH_CREATEORSIGNMANAGERCHANGE)) {
                            txType = HeterogeneousChainTxType.CHANGE;
                        }
                    }
                    this.dealBroadcastTx(txType, tx, blockHeight, txTime);
                    continue;
                }
                // 充值交易放入需要确认30个区块的待确认交易队列中
                if (addUnconfirmTx) {
                    po.setTxType(txType);
                    po.setTxHash(ethTxHash);
                    po.setBlockHeight(blockHeight);
                    po.setFrom(tx.getFrom());
                    po.setTxTime(txTime);
                    ethUnconfirmedTxStorageService.save(po);
                    // 保存解析的充值交易
                    ethStorageHelper.saveTxInfo(po);
                    EthContext.UNCONFIRMED_TX_QUEUE.offer(po);
                }
            }
        }
        // 保存本地区块
        EthSimpleBlockHeader simpleBlockHeader = new EthSimpleBlockHeader();
        simpleBlockHeader.setHash(block.getHash());
        simpleBlockHeader.setPreHash(block.getParentHash());
        simpleBlockHeader.setHeight(block.getNumber().longValue());
        simpleBlockHeader.setCreateTime(System.currentTimeMillis());
        ethLocalBlockHelper.saveLocalBlockHeader(simpleBlockHeader);
        // 只保留最近的三个区块
        ethLocalBlockHelper.deleteByHeight(blockHeight - 3);
        logger().info("同步ETH高度[{}]完成", block.getNumber().longValue());
    }

    private void dealBroadcastTx(HeterogeneousChainTxType txType, Transaction tx, long blockHeight, long txTime) throws Exception {
        String ethTxHash = tx.getHash();
        EthUnconfirmedTxPo txPo = ethUnconfirmedTxStorageService.findByTxHash(ethTxHash);
        boolean isLocal = true;
        if(txPo == null) {
            txPo = new EthUnconfirmedTxPo();
            isLocal = false;
        }
        // 判断是否为签名完成的交易，更改状态，解析交易的多签地址列表
        TransactionReceipt txReceipt = ethWalletApi.getTxReceipt(ethTxHash);
        if (txReceipt == null || !txReceipt.isStatusOK()) {
            txPo.setStatus(MultiSignatureStatus.FAILED);
        } else {
            // 先默认设置为正在多签的交易
            txPo.setStatus(MultiSignatureStatus.DOING);
            HeterogeneousTransactionInfo txInfo = null;
            // 以此判断是否为首次保存
            if(StringUtils.isBlank(txPo.getNerveTxHash())) {
                // 解析交易数据，补充基本信息、多签列表信息
                switch (txType) {
                    case WITHDRAW:
                        txInfo = ethParseTxHelper.parseWithdrawTransaction(tx, txReceipt);
                        break;
                    case CHANGE:
                        txInfo = ethParseTxHelper.parseManagerChangeTransaction(tx, txReceipt);
                        break;
                }
                if(txInfo != null) {
                    BeanUtils.copyProperties(txInfo, txPo);
                }
            } else {
                // 解析交易多签列表
                txPo.setSigners(ethParseTxHelper.parseSigners(txReceipt));
            }
            // 设置为签名完成的交易，当多签存在时
            if(txPo.getSigners() != null && !txPo.getSigners().isEmpty()) {
                logger().info("已完成签名的多签[{}]交易[{}], signers: {}",
                        txType,
                        ethTxHash, Arrays.toString(txPo.getSigners().toArray()));
                txPo.setStatus(MultiSignatureStatus.COMPLETED);
            }
            txPo.setBlockHeight(blockHeight);
            txPo.setTxTime(txTime);
            // 保存解析的已签名完成的交易
            if(txInfo != null) {
                ethStorageHelper.saveTxInfo(txInfo);
            } else {
                ethStorageHelper.saveTxInfo(txPo);
            }
        }
        ethUnconfirmedTxStorageService.save(txPo);
        if(!isLocal) {
            logger().info("从eth网络解析到的交易[{}]，新添加入待确认队列", ethTxHash);
            EthContext.UNCONFIRMED_TX_QUEUE.offer(txPo);
        }
        // 移除监听
        ethListener.removeListeningTx(ethTxHash);
    }

}

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

package nerve.network.converter.v1;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import nerve.network.converter.constant.ConverterConstant;
import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import nerve.network.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import nerve.network.converter.manager.ChainManager;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.bo.HeterogeneousTransactionInfo;
import nerve.network.converter.model.txdata.RechargeTxData;
import nerve.network.converter.utils.ConverterSignValidUtil;
import nerve.network.converter.utils.ConverterUtil;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Chino
 * @date: 2020-02-28
 */
@Component("RechargeV1")
public class RechargeProcessor implements TransactionProcessor {

    @Override
    public int getType() {
        return TxType.RECHARGE;
    }

    @Autowired
    private ChainManager chainManager;

    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;

    /**
     * 主要验证逻辑
     * 1.验证原始交易hash
     * 2.验证充值金额与原始充值金额是否一致
     * 3.验证到账地址是否一致
     *
     * @param chainId     链Id
     * @param txs         类型为{@link #getType()}的所有交易集合
     * @param txMap       不同交易类型与其对应交易列表键值对
     * @param blockHeader 区块头
     * @return
     */
    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        Chain chain = null;
        Map<String, Object> result = null;
        try {
            chain = chainManager.getChain(chainId);
            NulsLogger log = chain.getLogger();
            String errorCode = null;
            result = new HashMap<>(ConverterConstant.INIT_CAPACITY_4);
            List<Transaction> failsList = new ArrayList<>();
            for (Transaction tx : txs) {
                CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
                if (null != coinData.getFrom() && !coinData.getFrom().isEmpty()) {
                    failsList.add(tx);
                    // 充值不能有from
                    errorCode = ConverterErrorCode.RECHARGE_NOT_INCLUDE_COINFROM.getCode();
                    log.error(ConverterErrorCode.RECHARGE_NOT_INCLUDE_COINFROM.getMsg());
                    continue;
                }
                List<CoinTo> listCoinTo = coinData.getTo();
                if (null == listCoinTo || listCoinTo.size() > 1) {
                    failsList.add(tx);
                    // 充值to有且只能有一个
                    errorCode = ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO.getCode();
                    log.error(ConverterErrorCode.RECHARGE_HAVE_EXACTLY_ONE_COINTO.getMsg());
                    continue;
                }
                CoinTo coinTo = listCoinTo.get(0);
                int assetsChainId = coinTo.getAssetsChainId();
                IHeterogeneousChainDocking heterogeneousInterface = heterogeneousDockingManager.getHeterogeneousDocking(assetsChainId);
                if (null == heterogeneousInterface) {
                    failsList.add(tx);
                    // 异构chainId
                    errorCode = ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR.getCode();
                    log.error(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR.getMsg());
                    continue;
                }

                RechargeTxData txData = ConverterUtil.getInstance(tx.getTxData(), RechargeTxData.class);
                HeterogeneousTransactionInfo info = heterogeneousInterface.getDepositTransaction(txData.getHeterogeneousTxHash());
                if (null == info) {
                    failsList.add(tx);
                    // 异构交易信息未找到
                    errorCode = ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST.getCode();
                    log.error(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST.getMsg());
                    continue;
                }
                if (info.getAssetId() != coinTo.getAssetsId()) {
                    failsList.add(tx);
                    // 充值资产错误
                    errorCode = ConverterErrorCode.RECHARGE_ASSETID_ERROR.getCode();
                    log.error(ConverterErrorCode.RECHARGE_ASSETID_ERROR.getMsg());
                    continue;
                }
                if (info.getValue().compareTo(coinTo.getAmount()) != 0) {
                    failsList.add(tx);
                    // 充值金额错误
                    errorCode = ConverterErrorCode.RECHARGE_AMOUNT_ERROR.getCode();
                    log.error(ConverterErrorCode.RECHARGE_AMOUNT_ERROR.getMsg());
                    continue;
                }
                if (!info.getNerveAddress().equals(AddressTool.getStringAddressByBytes(coinTo.getAddress()))) {
                    failsList.add(tx);
                    // 充值到账地址错误
                    errorCode = ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR.getCode();
                    log.error(ConverterErrorCode.RECHARGE_ARRIVE_ADDRESS_ERROR.getMsg());
                    continue;
                }
                // 签名验证
                //验签名
                try {
                    ConverterSignValidUtil.validateSign(chain, tx);
                } catch (NulsException e) {
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    log.error(e.getErrorCode().getMsg());
                    continue;
                }
            }
            result.put("txList", failsList);
            result.put("errorCode", errorCode);
        } catch (Exception e) {
            chain.getLogger().error(e);
            result.put("txList", txs);
            result.put("errorCode", ConverterErrorCode.SYS_UNKOWN_EXCEPTION.getCode());
        }
        return result;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        return commit(chainId, txs, blockHeader, syncStatus, true);
    }

    private boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus, boolean failRollback) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        try {
            // 更新异构链组件交易状态 // add by pierre at 2020-03-12
            for(Transaction tx : txs) {
                RechargeTxData txData = ConverterUtil.getInstance(tx.getTxData(), RechargeTxData.class);
                CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
                CoinTo coinTo = coinData.getTo().get(0);
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(coinTo.getAssetsChainId());
                docking.txConfirmedCompleted(txData.getHeterogeneousTxHash(), blockHeader.getHeight());
                chain.getLogger().debug("[commit]Recharge 充值交易 hash:{}", tx.getHash().toHex());
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            if (failRollback) {
                rollback(chainId, txs, blockHeader, false);
            }
            return false;
        }
    }


    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return rollback(chainId, txs, blockHeader, true);
    }

    private boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader, boolean failCommit) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        try {
            // 回滚异构链组件交易状态 // add by pierre at 2020-03-13
            for(Transaction tx : txs) {
                RechargeTxData txData = ConverterUtil.getInstance(tx.getTxData(), RechargeTxData.class);
                CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
                CoinTo coinTo = coinData.getTo().get(0);
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(coinTo.getAssetsChainId());
                docking.txConfirmedRollback(txData.getHeterogeneousTxHash());
                chain.getLogger().debug("[rollback]Recharge 充值交易 hash:{}", tx.getHash().toHex());
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            if (failCommit) {
                commit(chainId, txs, blockHeader, 0, false);
            }
            return false;
        }
    }
}

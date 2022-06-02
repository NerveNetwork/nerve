/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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

package network.nerve.converter.tx.v1;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.data.TxData;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.base.protocol.TxDefine;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousHash;
import network.nerve.converter.model.txdata.OneClickCrossChainUnconfirmedTxData;
import network.nerve.converter.model.txdata.RechargeUnconfirmedTxData;
import network.nerve.converter.storage.RechargeStorageService;
import network.nerve.converter.utils.ConverterSignValidUtil;
import network.nerve.converter.utils.ConverterUtil;

import java.util.*;

/**
 * @author: PierreLuo
 * @date: 2022/3/24
 */
@Component("OneClickCrossChainUnconfirmedV1")
public class OneClickCrossChainUnconfirmedProcessor implements TransactionProcessor {
    @Override
    public int getType() {
        return TxType.ONE_CLICK_CROSS_CHAIN_UNCONFIRMED;
    }

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private RechargeStorageService rechargeStorageService;
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
            //区块内业务重复交易检查
            Set<String> setDuplicate = new HashSet<>();
            for (Transaction tx : txs) {
                OneClickCrossChainUnconfirmedTxData txData = ConverterUtil.getInstance(tx.getTxData(), OneClickCrossChainUnconfirmedTxData.class);
                if(!setDuplicate.add(txData.getOriginalTxHash().getHeterogeneousHash().toLowerCase())){
                    // 区块内业务重复交易
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                    log.error("The originalTxHash in the block is repeated (Repeat business) txHash:{}, originalTxHash:{}",
                            tx.getHash().toHex(), txData.getOriginalTxHash().getHeterogeneousHash());
                    continue;
                }
                // 签名拜占庭验证
                try {
                    ConverterSignValidUtil.validateByzantineSign(chain, tx);
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
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        try {
            for (Transaction tx : txs) {
                OneClickCrossChainUnconfirmedTxData txData = ConverterUtil.getInstance(tx.getTxData(), OneClickCrossChainUnconfirmedTxData.class);
                HeterogeneousHash heterogeneousHash = txData.getOriginalTxHash();
                chain.getLogger().info("[commit] [{}]异构链组件解析到一键跨链交易[Pending] hash:{}, 异构链交易hash:{}", heterogeneousHash.getHeterogeneousChainId(), tx.getHash().toHex(), heterogeneousHash.getHeterogeneousHash());
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return true;
    }
}

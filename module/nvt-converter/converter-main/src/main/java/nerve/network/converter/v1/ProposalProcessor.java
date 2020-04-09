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

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import nerve.network.converter.constant.ConverterConstant;
import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.manager.ChainManager;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.po.ProposalPo;
import nerve.network.converter.model.txdata.ProposalTxData;
import nerve.network.converter.storage.ProposalStorageService;
import nerve.network.converter.v1.proposal.ProposalValidater;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.logback.NulsLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Chino
 * @date: 2020-02-28
 */
@Component("ProposalV1")
public class ProposalProcessor implements TransactionProcessor {

    @Override
    public int getType() {
        return TxType.PROPOSAL;
    }

    @Autowired
    private ChainManager chainManager;

    @Autowired
    private ProposalStorageService proposalStorageService;

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
            String errorCode = ConverterErrorCode.DATA_ERROR.getMsg();
            result = new HashMap<>(ConverterConstant.INIT_CAPACITY_4);
            List<Transaction> failsList = new ArrayList<>();

            for (Transaction tx : txs) {
                ProposalTxData txData = new ProposalTxData();
                txData.parse(tx.getTxData(), 0);
                CoinData coinData = new CoinData();
                coinData.parse(tx.getCoinData(), 0);
                // 基础字段验证
                boolean res = ProposalValidater.validate(txData, coinData, chain);
                if (!res) {
                    failsList.add(tx);
                    continue;
                }
                //不进行业务验证，在确认时保证处理业务冲突。通过高额的提案费用避免重复提案
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

    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus, boolean failRollback) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = null;
        try {
            chain = chainManager.getChain(chainId);
            NulsLogger log = chain.getLogger();
            for (Transaction tx : txs) {
                ProposalTxData txData = new ProposalTxData();
                txData.parse(tx.getTxData(), 0);
                ProposalPo po = new ProposalPo();
                po.setAddress(txData.getAddress());
                po.setHash(tx.getHash());
                po.setHeterogeneousTxHash(txData.getHeterogeneousTxHash());
                po.setType(txData.getType());
                po.setTime(blockHeader.getTime());
                boolean res = this.proposalStorageService.save(chain, po);
                if (!res && failRollback) {
                    log.error(" Save proposal failed.");
                    this.rollback(chainId, txs, blockHeader, false);
                    return false;
                }
                chain.getLogger().debug("[commit] 提案交易 hash:{}", tx.getHash().toHex());
            }
        } catch (Exception e) {
            if (failRollback) {
                this.rollback(chainId, txs, blockHeader, false);
            }
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return rollback(chainId, txs, blockHeader, true);
    }

    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader, boolean failCommit) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = null;
        try {
            chain = chainManager.getChain(chainId);
            NulsLogger log = chain.getLogger();
            for (Transaction tx : txs) {
                boolean res = this.proposalStorageService.delete(chain, tx.getHash());
                if (!res && failCommit) {
                    log.error(" Save proposal failed.");
                    this.commit(chainId, txs, blockHeader, 0, false);
                    return false;
                }
                chain.getLogger().debug("[rollback] 提案交易 hash:{}", tx.getHash().toHex());
            }
        } catch (Exception e) {
            if (failCommit) {
                this.commit(chainId, txs, blockHeader, 0, false);
            }
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }
}

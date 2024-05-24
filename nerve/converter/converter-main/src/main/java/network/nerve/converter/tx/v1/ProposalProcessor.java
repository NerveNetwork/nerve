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

package network.nerve.converter.tx.v1;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.enums.ProposalVoteStatusEnum;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.txdata.ProposalTxData;
import network.nerve.converter.rpc.call.SwapCall;
import network.nerve.converter.storage.ProposalStorageService;
import network.nerve.converter.storage.ProposalVotingStorageService;
import network.nerve.converter.utils.ConverterSignValidUtil;
import network.nerve.converter.utils.ConverterUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Loki
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
    @Autowired
    private ProposalVotingStorageService proposalVotingStorageService;
    @Autowired
    private ConverterCoreApi converterCoreApi;

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
                // Signature Byzantine Verification
                try {
                    ConverterSignValidUtil.validateByzantineSign(chain, tx);
                } catch (NulsException e) {
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    log.error(e.getErrorCode().getMsg());
                    continue;
                }
                // swapCustomization of handling fees, verification of setting scope
                ProposalTxData txData = ConverterUtil.getInstance(tx.getTxData(), ProposalTxData.class);
                if (txData.getType() == ProposalTypeEnum.TRANSACTION_WHITELIST.value() && !converterCoreApi.isProtocol34()) {
                    failsList.add(tx);
                    // Heterogeneous chain address cannot be empty
                    errorCode = ConverterErrorCode.PROPOSAL_REJECTED.getCode();
                    log.error("[Validate TRANSACTION_WHITELIST] error");
                    continue;
                }
                if (txData.getType() == ProposalTypeEnum.CLOSE_HTG_CHAIN.value() && !converterCoreApi.isProtocol35()) {
                    failsList.add(tx);
                    // Heterogeneous chain address cannot be empty
                    errorCode = ConverterErrorCode.PROPOSAL_REJECTED.getCode();
                    log.error("[Validate CLOSE_HTG_CHAIN] error");
                    continue;
                }
                if (txData.getType() != ProposalTypeEnum.MANAGE_SWAP_PAIR_FEE_RATE.value()) {
                    continue;
                }
                Integer feeRate = Integer.parseInt(txData.getContent());
                String swapPairAddress = AddressTool.getStringAddressByBytes(txData.getAddress());
                if (!SwapCall.isLegalSwapFeeRate(chainId, swapPairAddress, feeRate)) {
                    failsList.add(tx);
                    // Heterogeneous chain address cannot be empty
                    errorCode = ConverterErrorCode.PROPOSAL_REJECTED.getCode();
                    log.error("[Validate SwapFeeRate] error, swapPairAddress: {}. feeRate: {}", swapPairAddress, feeRate);
                    continue;
                }
            }
            //Do not conduct business validation and ensure the resolution of business conflicts during confirmation. Avoiding duplicate proposals through high proposal costs
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
                ProposalTxData txData = ConverterUtil.getInstance(tx.getTxData(), ProposalTxData.class);
                ProposalPO po = new ProposalPO();
                po.setHash(tx.getHash());
                po.setType(txData.getType());
                po.setAddress(txData.getAddress());
                po.setNerveHash(txData.getHash());
                po.setHeterogeneousChainId(txData.getHeterogeneousChainId());
                po.setHeterogeneousTxHash(txData.getHeterogeneousTxHash());
                po.setVoteRangeType(txData.getVoteRangeType());
                po.setContent(txData.getContent());
                // Block height for locking voting deadline(Current height + Number of blocks corresponding to voting duration)
                po.setVoteEndHeight(blockHeader.getHeight() + ConverterContext.PROPOSAL_VOTE_TIME_BLOCKS);
                po.setStatus(ProposalVoteStatusEnum.VOTING.value());
                boolean res = this.proposalStorageService.save(chain, po);
                // Votable proposals placed in cachemap
                chain.getVotingProposalMap().put(po.getHash(), po);
                boolean resVote = this.proposalVotingStorageService.save(chain, po);
                boolean fail = !res || !resVote;
                if (fail) {
                    log.error("[commit] Save proposal failed. hash:{}, proposalType:{}", tx.getHash().toHex(), txData.getType());
                    throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR);
                }
                chain.getLogger().info("[commit] Proposal transaction hash:{}", tx.getHash().toHex());
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
                chain.getVotingProposalMap().remove(tx.getHash());
                boolean resVote = this.proposalVotingStorageService.delete(chain, tx.getHash());
                boolean fail = !res || !resVote;
                if (fail) {
                    chain.getLogger().error("[rollback] remove proposal failed. hash:{}, type:{}", tx.getHash().toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_DELETE_ERROR);
                }
                chain.getLogger().debug("[rollback] Proposal transaction hash:{}", tx.getHash().toHex());
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

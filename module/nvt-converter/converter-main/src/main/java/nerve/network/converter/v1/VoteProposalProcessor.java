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
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.base.signture.TransactionSignature;
import nerve.network.converter.constant.ConverterConstant;
import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.manager.ChainManager;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.po.ProposalPo;
import nerve.network.converter.model.po.VoteProposalPo;
import nerve.network.converter.model.txdata.VoteProposalTxData;
import nerve.network.converter.storage.ProposalStorageService;
import nerve.network.converter.storage.VoteProposalStorageService;
import nerve.network.converter.v1.proposal.VoteValidater;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;

import java.util.*;

/**
 * @author: Chino
 * @date: 2020-02-28
 */
@Component("VoteProposalV1")
public class VoteProposalProcessor implements TransactionProcessor {

    @Override
    public int getType() {
        return TxType.VOTE_PROPOSAL;
    }

    @Autowired
    private ChainManager chainManager;

    @Autowired
    private ProposalStorageService proposalStorageService;
    @Autowired
    private VoteProposalStorageService voteProposalStorageService;

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
            Set<String> keySet = new HashSet<>();
            for (Transaction tx : txs) {
                VoteProposalTxData txData = new VoteProposalTxData();
                txData.parse(tx.getTxData(), 0);
                ProposalPo proposalPo = this.proposalStorageService.find(chain, txData.getProposalTxHash());
                boolean res = false;
                if (null == proposalPo) {
                    failsList.add(tx);
                    continue;
                }
                TransactionSignature signature = new TransactionSignature();
                signature.parse(tx.getTransactionSignature(), 0);
                if (signature.getP2PHKSignatures() == null || signature.getSignersCount() != 1) {
                    log.warn("Signatures count is not one.");
                    failsList.add(tx);
                    continue;
                }
                byte[] address = AddressTool.getAddress(signature.getP2PHKSignatures().get(0).getPublicKey(), chainId);
                if (!keySet.add(proposalPo.getHash().toHex() + HexUtil.encode(address))) {
                    //去重
                    failsList.add(tx);
                    continue;
                }
                res = VoteValidater.validate(proposalPo, txData, address, chain, voteProposalStorageService);
                if (!res) {
                    failsList.add(tx);
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

    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus, boolean failRollback) {
        Chain chain = chainManager.getChain(chainId);
        try {
            if (null == txs || txs.isEmpty()) {
                return true;
            }
            //存储基本数据，
            // 更新提案状态，提案状态以区块为单位更新，同一个区块中的投票都可以被确认
            Map<NulsHash, ProposalPo> proposalPoMap = new HashMap<>();
            for (Transaction tx : txs) {
                VoteProposalTxData txData = new VoteProposalTxData();
                txData.parse(tx.getTxData(), 0);
                ProposalPo po = proposalPoMap.get(txData.getProposalTxHash());
                if (null == po) {
                    po = this.proposalStorageService.find(chain, txData.getProposalTxHash());
                }
                if (null == po) {
                    chain.getLogger().error("Proposal is not exist.");
                    throw new NulsException(ConverterErrorCode.DATA_ERROR);
                }
                TransactionSignature signature = new TransactionSignature();
                signature.parse(tx.getTransactionSignature(), 0);
                if (signature.getP2PHKSignatures() == null || signature.getSignersCount() != 1) {
                    chain.getLogger().error("VoteProposal tx has wrong signatures count.");
                    throw new NulsException(ConverterErrorCode.DATA_ERROR);
                }
                byte[] address = AddressTool.getAddress(signature.getP2PHKSignatures().get(0).getPublicKey(), chainId);
                VoteProposalPo votePo = new VoteProposalPo();
                votePo.setProposalTxHash(txData.getProposalTxHash());
                votePo.setAddress(address);
                votePo.setChoice(txData.getChoice());
                votePo.setHash(tx.getHash());
                boolean res = this.voteProposalStorageService.save(chain,votePo);
                if(!res){
                    chain.getLogger().error("VoteProposal data save fiailed.");
                    throw new NulsException(ConverterErrorCode.DATA_ERROR);
                }
                po.vote(chain,votePo);
                proposalPoMap.put(po.getHash(),po);
            }
            for(Map.Entry<NulsHash, ProposalPo> entry:proposalPoMap.entrySet()){
                //todo
                //根据类型、状态，生成异步调用列表，并以此调用（调用异构链退回，调用本链生成资产，调用账本更新黑名单，调用本地服务取缔银行资格）
            }




            return true;

        } catch (NulsException e) {
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

    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader, boolean failCommit) {
        Chain chain = chain = chainManager.getChain(chainId);
        try {
            if (true) {
                // todo 占位
                throw new NulsException(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
            }
            return true;
        } catch (NulsException e) {
            chain.getLogger().error(e);
            if (failCommit) {
                commit(chainId, txs, blockHeader, 0, false);
            }
            return false;
        }
    }
}

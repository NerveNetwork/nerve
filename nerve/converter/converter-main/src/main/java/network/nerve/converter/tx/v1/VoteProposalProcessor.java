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
import io.nuls.base.data.*;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.base.signture.MultiSignTxSignature;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.HeterogeneousService;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.enums.ProposalVoteChoiceEnum;
import network.nerve.converter.enums.ProposalVoteRangeTypeEnum;
import network.nerve.converter.enums.ProposalVoteStatusEnum;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.AgentBasic;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.ExeProposalPO;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.po.VoteProposalPO;
import network.nerve.converter.model.txdata.VoteProposalTxData;
import network.nerve.converter.rpc.call.ConsensusCall;
import network.nerve.converter.storage.*;
import network.nerve.converter.utils.ConverterUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: Loki
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
    @Autowired
    private ProposalVotingStorageService proposalVotingStorageService;
    @Autowired
    private ExeProposalStorageService exeProposalStorageService;
    @Autowired
    private DisqualificationStorageService disqualificationStorageService;
    @Autowired
    private HeterogeneousService heterogeneousService;

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
            Set<String> setDuplicate = new HashSet<>();
            for (Transaction tx : txs) {
                VoteProposalTxData txData = ConverterUtil.getInstance(tx.getTxData(), VoteProposalTxData.class);
                if (null == txData.getProposalTxHash() || txData.getProposalTxHash().isBlank()) {
                    // 投票交易中提案hash不存在
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.PROPOSAL_NOT_EXIST.getCode();
                    log.error("There is no proposal hash on the Vote. vote_txHash:{}", tx.getHash().toHex());
                    continue;
                }
                if (null == ProposalVoteChoiceEnum.getEnum(txData.getChoice())) {
                    // 投票选项错误
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.VOTE_CHOICE_ERROR.getCode();
                    log.error("Choice error. choice:{}", txData.getChoice());
                    continue;
                }

                ProposalPO proposalPo = this.proposalStorageService.find(chain, txData.getProposalTxHash());
                if (null == proposalPo) {
                    // 提案不存在
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.PROPOSAL_NOT_EXIST.getCode();
                    log.error(ConverterErrorCode.PROPOSAL_NOT_EXIST.getMsg());
                    continue;
                }

                if (proposalPo.getStatus() != ProposalVoteStatusEnum.VOTING.value()) {
                    // 该提案已不能投票
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.VOTING_STOPPED.getCode();
                    log.error("Proposal can not vote. Status:{}", ProposalVoteStatusEnum.getEnum(proposalPo.getStatus()));
                    continue;
                }
                // 本地最新高度(如果是同步模式中, 则为同步的最新高度)
                long currentHeight = chain.getLatestBasicBlock().getHeight();
                if (proposalPo.getVoteEndHeight() < currentHeight) {
                    // 该提案投票已截止
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.VOTING_STOPPED.getCode();
                    log.error("The vote on the proposal has closed. VoteEndHeight:{}, currentHeight:{}",
                            proposalPo.getVoteEndHeight(), currentHeight);
                    continue;
                }

                byte[] address;
                if (tx.isMultiSignTx()) {
                    MultiSignTxSignature mts = ConverterUtil.getInstance(tx.getTransactionSignature(), MultiSignTxSignature.class);
                    List<String> pubKeys = mts.getPubKeyList().stream().map(p -> HexUtil.encode(p)).collect(Collectors.toList());
                    Address addressObj = new Address(chainId, BaseConstant.P2SH_ADDRESS_TYPE,
                            SerializeUtils.sha256hash160(AddressTool.createMultiSigAccountOriginBytes(chainId, mts.getM(), pubKeys)));
                    address = addressObj.getAddressBytes();
                } else {
                    TransactionSignature signature = ConverterUtil.getInstance(tx.getTransactionSignature(), TransactionSignature.class);
                    //第一个签名为投票人签名
                    address = AddressTool.getAddress(signature.getP2PHKSignatures().get(0).getPublicKey(), chainId);
                }

                CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
                boolean addrMatched = false;
                for (CoinFrom coinFrom : coinData.getFrom()) {
                    if (Arrays.equals(address, coinFrom.getAddress())
                            && coinFrom.getAssetsChainId() == chain.getConfig().getChainId()
                            && coinFrom.getAssetsId() == chain.getConfig().getAssetId()) {
                        addrMatched = true;
                        break;
                    }
                }
                if (!addrMatched) {
                    // 该投票签名者与投票人不匹配, 该投票无效
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.VOTER_SIGNER_MISMATCH.getCode();
                    log.error("The vote tx coinFrom address and signer address is mismatch. ");
                    continue;
                }

                String voterAddress = AddressTool.getStringAddressByBytes(address);
                String key = proposalPo.getHash().toHex().toLowerCase() + voterAddress;
                if (setDuplicate.contains(key)) {
                    // 业务重复的投票
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.DUPLICATE_VOTE.getCode();
                    log.error(ConverterErrorCode.DUPLICATE_VOTE.getMsg());
                    continue;
                }
                // 投票资格
                boolean rs = suffrage(chain,
                        ProposalTypeEnum.getEnum(proposalPo.getType()),
                        ProposalVoteRangeTypeEnum.getEnum(proposalPo.getVoteRangeType()),
                        voterAddress,
                        currentHeight);
                if (!rs) {
                    // 投票人没有投票权
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.NO_VOTING_RIGHTS.getCode();
                    log.error("The voter has no right to vote. Proposal_voter_range:{}",
                            ProposalVoteRangeTypeEnum.getEnum(proposalPo.getVoteRangeType()));
                    continue;
                }
                // 投票人没有重复投票（一经确认，不可修改）
                VoteProposalPO votePO = voteProposalStorageService.find(chain, txData.getProposalTxHash(), address);
                if (null != votePO) {
                    // 该地址已对该提案投过票
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.DUPLICATE_VOTE.getCode();
                    log.error("The voter have voted on the proposal.  voter:{}, proposalHash:{}",
                            voterAddress, txData.getProposalTxHash());
                    continue;
                }
                setDuplicate.add(key);
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

    /**
     * 验证是否有投票资格
     *
     * @param chain
     * @param rangeEnum
     * @param address
     * @param height
     * @return
     */
    private boolean suffrage(Chain chain, ProposalTypeEnum typeEnum, ProposalVoteRangeTypeEnum rangeEnum, String address, long height) {
        if (ProposalTypeEnum.OTHER == typeEnum) {
            switch (rangeEnum) {
                case BANK:
                    return chain.isVirtualBankByAgentAddr(address);
                case AGENT:
                    List<AgentBasic> agentList = ConsensusCall.getAgentList(chain, height);
                    for (AgentBasic agent : agentList) {
                        if (address.equals(agent.getAgentAddress())) {
                            return true;
                        }
                    }
                    return false;
                case TOKEN_POSSESSOR:
                default:
                    return true;
            }
        } else {
            return chain.isVirtualBankByAgentAddr(address);
        }
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
            // 存储基本数据, 更新提案状态.
            Set<ExeProposalPO> exeProposalPOSet = null;
            for (Transaction tx : txs) {
                VoteProposalTxData txData = ConverterUtil.getInstance(tx.getTxData(), VoteProposalTxData.class);
                ProposalPO po = this.proposalStorageService.find(chain, txData.getProposalTxHash());

                byte[] address;
                if (tx.isMultiSignTx()) {
                    MultiSignTxSignature mts = ConverterUtil.getInstance(tx.getTransactionSignature(), MultiSignTxSignature.class);
                    List<String> pubKeys = mts.getPubKeyList().stream().map(p -> HexUtil.encode(p)).collect(Collectors.toList());
                    Address addressObj = new Address(chainId, BaseConstant.P2SH_ADDRESS_TYPE,
                            SerializeUtils.sha256hash160(AddressTool.createMultiSigAccountOriginBytes(chainId, mts.getM(), pubKeys)));
                    address = addressObj.getAddressBytes();
                } else {
                    TransactionSignature signature = ConverterUtil.getInstance(tx.getTransactionSignature(), TransactionSignature.class);
                    //第一个签名为投票人签名
                    address = AddressTool.getAddress(signature.getP2PHKSignatures().get(0).getPublicKey(), chainId);
                }

                VoteProposalPO votePo = new VoteProposalPO();
                votePo.setProposalTxHash(txData.getProposalTxHash());
                votePo.setAddress(address);
                votePo.setChoice(txData.getChoice());
                votePo.setHash(tx.getHash());
                boolean res = this.voteProposalStorageService.save(chain, votePo);
                if (!res) {
                    chain.getLogger().error("[commit] Save vote failed. hash:{}, type:{}", tx.getHash().toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR);
                }
                // 为提案处理票数
                po.voteCommit(chain, votePo);
                res = this.proposalStorageService.save(chain, po);
                if (!res) {
                    chain.getLogger().error("[commit] Save vote failed, update proposal voteNumber faild. hash:{}, type:{}", tx.getHash().toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_SAVE_ERROR);
                }
                if (po.getStatus() == ProposalVoteStatusEnum.REJECTED.value()) {
                    // 从投票中的提案移除
                    chain.getVotingProposalMap().remove(po.getHash());
                    proposalVotingStorageService.delete(chain, po.getHash());
                } else if (po.getStatus() == ProposalVoteStatusEnum.ADOPTED.value()) {
                    // 投票通过 可以执行提案
                    ExeProposalPO exeProposalPO = new ExeProposalPO();
                    exeProposalPO.setProposalTxHash(txData.getProposalTxHash());
                    exeProposalPO.setHeight(blockHeader.getHeight());
                    exeProposalPO.setTime(blockHeader.getTime());
                    exeProposalPO.setSyncStatusEnum(SyncStatusEnum.getEnum(syncStatus));
                    exeProposalPO.setCurrenVirtualBankTotal(chain.getMapVirtualBank().size());
                    if (null == exeProposalPOSet) {
                        exeProposalPOSet = new HashSet<>();
                    }
                    exeProposalPOSet.add(exeProposalPO);

                    // 如果是撤销虚拟银行节点,则先执行提案,保证节点数据一致性
                    if (ProposalTypeEnum.getEnum(po.getType()) == ProposalTypeEnum.EXPELLED) {
                        if (syncStatus == SyncStatusEnum.RUNNING.value()) {
                            heterogeneousService.saveExeDisqualifyBankProposalStatus(chain, true);
                        }
                        if (!disqualificationStorageService.save(chain, po.getAddress())) {
                            chain.getLogger().error("[commit] 地址加入[撤销银行资格列表]失败 address:{}", AddressTool.getStringAddressByBytes(po.getAddress()));
                            throw new NulsException(ConverterErrorCode.DISQUALIFICATION_FAILED);
                        }
                        chain.getLogger().info("[commit] 撤销银行资格提案投票通过, 银行节点地址加入[撤销银行资格列表] address:{}", AddressTool.getStringAddressByBytes(po.getAddress()));
                    }
                    // 从投票中的提案移除
                    chain.getVotingProposalMap().remove(po.getHash());
                    proposalVotingStorageService.delete(chain, po.getHash());
                }
                chain.getLogger().info("[commit] 提案投票成功 hash:{}, 提案hash:{}, 投票地址:{}",
                        tx.getHash().toHex(), txData.getProposalTxHash().toHex(), AddressTool.getStringAddressByBytes(address));
            }
            if (null != exeProposalPOSet) {
                for (ExeProposalPO exeProposalPO : exeProposalPOSet) {
                    // 放入队列
                    chain.getExeProposalQueue().offer(exeProposalPO);
                    exeProposalStorageService.save(chain, exeProposalPO);
                }
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

    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader, boolean failCommit) {
        if (null == txs || txs.isEmpty()) {
            return true;
        }
        Chain chain = null;
        try {
            chain = chainManager.getChain(chainId);
            for (Transaction tx : txs) {
                VoteProposalTxData txData = ConverterUtil.getInstance(tx.getTxData(), VoteProposalTxData.class);

                byte[] address;
                if (tx.isMultiSignTx()) {
                    MultiSignTxSignature mts = ConverterUtil.getInstance(tx.getTransactionSignature(), MultiSignTxSignature.class);
                    List<String> pubKeys = mts.getPubKeyList().stream().map(p -> HexUtil.encode(p)).collect(Collectors.toList());
                    Address addressObj = new Address(chainId, BaseConstant.P2SH_ADDRESS_TYPE,
                            SerializeUtils.sha256hash160(AddressTool.createMultiSigAccountOriginBytes(chainId, mts.getM(), pubKeys)));
                    address = addressObj.getAddressBytes();
                } else {
                    TransactionSignature signature = ConverterUtil.getInstance(tx.getTransactionSignature(), TransactionSignature.class);
                    //第一个签名为投票人签名
                    address = AddressTool.getAddress(signature.getP2PHKSignatures().get(0).getPublicKey(), chainId);
                }

                boolean res = this.voteProposalStorageService.delete(chain, txData.getProposalTxHash(), address);
                if (!res) {
                    chain.getLogger().error("[rollback] remove vote failed. hash:{}, type:{}", tx.getHash().toHex(), tx.getType());
                    throw new NulsException(ConverterErrorCode.DB_DELETE_ERROR);
                }
                // 为提案处理票数
                ProposalPO po = this.proposalStorageService.find(chain, txData.getProposalTxHash());
                po.voteRollback(chain, txData.getChoice(), AddressTool.getStringAddressByBytes(address));
                if (ProposalTypeEnum.getEnum(po.getType()) == ProposalTypeEnum.EXPELLED) {
                    heterogeneousService.saveExeDisqualifyBankProposalStatus(chain, false);
                    if (!disqualificationStorageService.delete(chain, po.getAddress())) {
                        chain.getLogger().error("[rollback]地址移除[撤销银行资格列表]失败 address:{}", AddressTool.getStringAddressByBytes(po.getAddress()));
                        throw new NulsException(ConverterErrorCode.DISQUALIFICATION_FAILED);
                    }
                }
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

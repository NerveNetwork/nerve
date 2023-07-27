package io.nuls.consensus.utils.validator;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.*;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.constant.ConsensusErrorCode;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.round.MeetingMember;
import io.nuls.consensus.model.bo.round.MeetingRound;
import io.nuls.consensus.model.bo.round.RoundValidResult;
import io.nuls.consensus.model.bo.tx.txdata.Agent;
import io.nuls.consensus.utils.compare.CoinFromComparator;
import io.nuls.consensus.utils.compare.CoinToComparator;
import io.nuls.consensus.utils.enumeration.PunishReasonEnum;
import io.nuls.consensus.utils.manager.AgentManager;
import io.nuls.consensus.utils.manager.CoinDataManager;
import io.nuls.consensus.utils.manager.ConsensusManager;
import io.nuls.consensus.utils.manager.PunishManager;
import io.nuls.consensus.v1.RoundController;
import io.nuls.consensus.v1.utils.RoundUtils;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.DoubleUtils;

import java.io.IOException;
import java.util.*;

@Component
public class BlockValidator {

    @Autowired
    private CoinDataManager coinDataManager;

    @Autowired
    public PunishManager punishManager;

    @Autowired
    private AgentManager agentManager;

    /**
     * 区块头验证
     * Block verification
     *
     * @param chain chain info
     * @param block block info
     */
    public void validate(Chain chain, Block block, boolean settleConsensusAward) throws NulsException, IOException {
        BlockHeader blockHeader = block.getHeader();
        //验证梅克尔哈希
        if (!blockHeader.getMerkleHash().equals(NulsHash.calcMerkleHash(block.getTxHashList()))) {
            throw new NulsException(ConsensusErrorCode.MERKEL_HASH_ERROR);
        }
        //区块头签名验证
        if (blockHeader.getBlockSignature().verifySignature(blockHeader.getHash()).isFailed()) {
            chain.getLogger().error("Block Header Verification Error!");
            throw new NulsException(ConsensusErrorCode.SIGNATURE_ERROR);
        }
        //区块轮次信息验证
        RoundValidResult roundValidResult;
        String blockHeaderHash = blockHeader.getHash().toHex();

        roundValidResult = roundValidate(chain, blockHeader, blockHeaderHash);

        MeetingRound currentRound = roundValidResult.getRound();
        BlockExtendsData extendsData = blockHeader.getExtendsData();
        MeetingMember member = currentRound.getMemberByOrder(extendsData.getPackingIndexOfRound());
        boolean validResult = punishValidate(block, currentRound, member, chain, blockHeaderHash);
        if (!validResult) {
            throw new NulsException(ConsensusErrorCode.BLOCK_PUNISH_VALID_ERROR);
        }
        validResult = coinBaseValidate(block, member, chain, blockHeaderHash, settleConsensusAward);
        if (!validResult) {
            throw new NulsException(ConsensusErrorCode.BLOCK_COINBASE_VALID_ERROR);
        }

    }

    /**
     * 区块轮次验证
     * Block round validation
     *
     * @param chain       chain info
     * @param blockHeader block header info
     */
    private RoundValidResult roundValidate(Chain chain, BlockHeader blockHeader, String blockHeaderHash) throws NulsException {

        BlockExtendsData extendsData = blockHeader.getExtendsData();
        BlockHeader bestBlockHeader = chain.getBestHeader();
        BlockExtendsData bestExtendsData = bestBlockHeader.getExtendsData();
        RoundValidResult roundValidResult = new RoundValidResult();

        /*
        该区块为本地最新区块之前的区块
        * */
        boolean isBeforeBlock = extendsData.getRoundIndex() < bestExtendsData.getRoundIndex()
                || (extendsData.getRoundIndex() == bestExtendsData.getRoundIndex() && extendsData.getPackingIndexOfRound() <= bestExtendsData.getPackingIndexOfRound());
        if (isBeforeBlock) {
            chain.getLogger().error("new block roundData error, block height : " + blockHeader.getHeight() + " , hash :" + blockHeaderHash);
            chain.getLogger().error("1:{}={}.{}={}", extendsData.getRoundIndex(), bestExtendsData.getRoundIndex(),
                    extendsData.getPackingIndexOfRound(), bestExtendsData.getPackingIndexOfRound());
            throw new NulsException(ConsensusErrorCode.BLOCK_ROUND_VALIDATE_ERROR);
        }

        RoundController roundController = RoundUtils.getRoundController();
        if (null == roundController) {
            throw new NulsException(ConsensusErrorCode.BLOCK_ROUND_VALIDATE_ERROR);
        }
        MeetingRound currentRound = roundController.getCurrentRound();
        if (null == currentRound || !currentRound.isConfirmed() || currentRound.getIndex() < extendsData.getRoundIndex()) {
            currentRound = roundController.getRound(extendsData.getRoundIndex(), extendsData.getRoundStartTime());
        }
        //容错机制，避免本地轮次切换时出现错误
        if (currentRound.getIndex() == extendsData.getRoundIndex() && currentRound.getStartTime() != extendsData.getRoundStartTime() && bestExtendsData.getRoundIndex() != extendsData.getRoundIndex()) {
            currentRound.setStartTime(extendsData.getRoundStartTime());
            currentRound.resetMemberOrder();
        }

        if (chain.getBestHeader().getHeight() == 0) {
            chain.getRoundList().clear();
        }

        boolean hasChangeRound = false;
        if (currentRound == null) {
            chain.getLogger().warn(currentRound.toString());
            throw new NulsException(ConsensusErrorCode.BLOCK_ROUND_VALIDATE_ERROR);
        }
        //验证轮次共识节点数量是否一致
        if (extendsData.getConsensusMemberCount() != currentRound.getMemberCount()) {
            chain.getLogger().error("block height " + blockHeader.getHeight() + " packager count is error! hash :" + blockHeaderHash);
            chain.getLogger().warn(currentRound.toString());
            throw new NulsException(ConsensusErrorCode.BLOCK_ROUND_VALIDATE_ERROR);
        }
        // 验证打包人是否正确
        MeetingMember member = currentRound.getMemberByOrder(extendsData.getPackingIndexOfRound());
        if (!Arrays.equals(member.getAgent().getPackingAddress(), blockHeader.getPackingAddress(chain.getConfig().getChainId()))) {
            chain.getLogger().error("block height " + blockHeader.getHeight() + " packager error! hash :" + blockHeaderHash);
            chain.getLogger().warn(currentRound.toString());
            throw new NulsException(ConsensusErrorCode.BLOCK_ROUND_VALIDATE_ERROR);
        }
        roundValidResult.setRound(currentRound);
        roundValidResult.setHasRoundChange(hasChangeRound);
        return roundValidResult;
    }

    /**
     * 区块惩罚交易验证
     * Block Penalty Trading Verification
     *
     * @param block        block info
     * @param currentRound Block round information
     * @param member       Node packing information
     * @param chain        chain info
     */
    private boolean punishValidate(Block block, MeetingRound currentRound, MeetingMember member, Chain chain, String blockHeaderHash) throws NulsException {
        List<Transaction> txs = block.getTxs();
        List<Transaction> redPunishTxList = new ArrayList<>();
        Transaction yellowPunishTx = null;
        Transaction tx;
        /*
        检查区块交中是否存在多个黄牌交易
        Check whether there are multiple yellow trades in block handover
        */
        for (int index = 1; index < txs.size(); index++) {
            tx = txs.get(index);
            if (tx.getType() == TxType.COIN_BASE) {
                chain.getLogger().info("Coinbase transaction more than one! height: " + block.getHeader().getHeight() + " , hash : " + blockHeaderHash);
                return false;
            }
            if (tx.getType() == TxType.YELLOW_PUNISH) {
                if (yellowPunishTx == null) {
                    yellowPunishTx = tx;
                } else {
                    chain.getLogger().info("Yellow punish transaction more than one! height: " + block.getHeader().getHeight() + " , hash : " + blockHeaderHash);
                    return false;
                }
            } else if (tx.getType() == TxType.RED_PUNISH) {
                redPunishTxList.add(tx);
            }
        }
        /*
        校验区块交易中的黄牌交易是否正确
         Check the correctness of yellow card trading in block trading
        */
        if (!verifyYellowPunish(block, currentRound, member, chain, blockHeaderHash, yellowPunishTx)) {
            chain.getLogger().info("yellow punish tx wrong.");
            return false;
        }

        /*
        区块中红牌交易验证
        Verification of Red Card Trading in Blocks
         */
        if (!redPunishTxList.isEmpty()) {
            Set<String> punishAddress = new HashSet<>();
            if (null != yellowPunishTx) {
                YellowPunishData yellowPunishData = new YellowPunishData();
                yellowPunishData.parse(yellowPunishTx.getTxData(), 0);
                List<byte[]> addressList = yellowPunishData.getAddressList();
                for (byte[] address : addressList) {
                    MeetingMember item = currentRound.getMemberByAgentAddress(address);
                    if (null == item) {
                        item = currentRound.getPreRound().getMemberByAgentAddress(address);
                    }
                    if (DoubleUtils.compare(item.getAgent().getRealCreditVal(), ConsensusConstant.RED_PUNISH_CREDIT_VAL) <= 0 &&
                            //如果已经红牌了，就不需要再来一次了
                            item.getAgent().getDelHeight() <= 0) {
                        punishAddress.add(AddressTool.getStringAddressByBytes(item.getAgent().getAgentAddress()));
                    }
                }
            }
            return verifyRedPunish(chain, redPunishTxList, blockHeaderHash, punishAddress, block);
        }
        return true;
    }

    /**
     * 区块CoinBase交易验证
     * Block CoinBase transaction verification
     *
     * @param block  block info
     * @param member Node packing information
     * @param chain  chain info
     */
    private boolean coinBaseValidate(Block block, MeetingMember member, Chain chain, String blockHeaderHash, boolean settleConsensusAward) throws NulsException, IOException {
        Transaction tx = block.getTxs().get(0);
        if (tx.getType() != TxType.COIN_BASE) {
            //chain.getLogger().debug("CoinBase transaction order wrong! height: " + block.getHeader().getHeight() + " , hash : " + blockHeaderHash);
            return false;
        }
        Transaction coinBaseTransaction;
        try {
            coinBaseTransaction = ConsensusManager.createCoinBaseTx(chain, member.getAgent().getRewardAddress(), block.getTxs(), block.getHeader().getTime(), settleConsensusAward, false);
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }

        if (null == coinBaseTransaction) {
            chain.getLogger().error("the coin base tx is wrong! height: " + block.getHeader().getHeight() + " , hash : " + blockHeaderHash);
            return false;
        } else if (!tx.getHash().equals(coinBaseTransaction.getHash())) {
            CoinToComparator toComparator = new CoinToComparator();

            CoinData coinBaseCoinData = coinBaseTransaction.getCoinDataInstance();
            coinBaseCoinData.getTo().sort(toComparator);
            coinBaseTransaction.setCoinData(coinBaseCoinData.serialize());

            Transaction originTransaction = new Transaction();
            originTransaction.parse(tx.serialize(), 0);
            CoinData originCoinData = originTransaction.getCoinDataInstance();
            originCoinData.getTo().sort(toComparator);
            originTransaction.setCoinData(originCoinData.serialize());

            if (!originTransaction.getHash().equals(coinBaseTransaction.getHash())) {
                chain.getLogger().error("the coin base tx is wrong! height: " + block.getHeader().getHeight() + " , hash : " + blockHeaderHash);
//                chain.getLogger().error("区块中：" + tx.getCoinData().length + ", 本地生成：" + coinBaseTransaction.getCoinData().length);
                CoinData coins = new CoinData();
                try {
                    coins.parse(tx.getCoinData(), 0);
                } catch (NulsException e) {
                    chain.getLogger().error(e);
                }
                if (null != coins.getTo() && !coins.getTo().isEmpty()) {
                    for (CoinTo coinTo : coins.getTo()) {
                        chain.getLogger().info("coinbase : " + AddressTool.getStringAddressByBytes(coinTo.getAddress()) + ", " + coinTo.getAmount().toString());
                    }
                } else {
                    chain.getLogger().info("coinbase : nothing");
                }
                chain.getLogger().error("===========================");
                coins = new CoinData();
                try {
                    coins.parse(coinBaseTransaction.getCoinData(), 0);
                } catch (NulsException e) {
                    chain.getLogger().error(e);
                }
                if (null != coins.getTo() && !coins.getTo().isEmpty()) {
                    for (CoinTo coinTo : coins.getTo()) {
                        chain.getLogger().info("coinbase_local : " + AddressTool.getStringAddressByBytes(coinTo.getAddress()) + ", " + coinTo.getAmount().toString());
                    }
                } else {
                    chain.getLogger().info("coinbase_local : nothing");
                }
                return false;
            }
        }
        return true;
    }

    /**
     * 黄牌交易验证
     * Block Penalty Trading Verification
     *
     * @param block          block info
     * @param currentRound   Block round information
     * @param member         Node packing information
     * @param chain          chain info
     * @param yellowPunishTx yellow Punish transaction
     */
    private boolean verifyYellowPunish(Block block, MeetingRound currentRound, MeetingMember member, Chain chain, String blockHeaderHash, Transaction yellowPunishTx) {
        try {
            Transaction newYellowPunishTX = punishManager.createYellowPunishTx(chain, chain.getBestHeader(), member, currentRound, block.getHeader().getTime());
            boolean isMatch = (yellowPunishTx == null && newYellowPunishTX == null) || (yellowPunishTx != null && newYellowPunishTX != null);
            if (!isMatch) {
                chain.getLogger().info("The yellow punish tx is wrong! height: " + block.getHeader().getHeight() + " , hash : " + blockHeaderHash);
                return false;
            } else if (yellowPunishTx != null && !yellowPunishTx.getHash().equals(newYellowPunishTX.getHash())) {
                chain.getLogger().info("The yellow punish tx's hash is wrong! height: " + block.getHeader().getHeight() + " , hash : " + blockHeaderHash);
                return false;
            }
        } catch (Exception e) {
            chain.getLogger().error("The tx's wrong! height: " + block.getHeader().getHeight() + " , hash : " + blockHeaderHash, e);
            return false;
        }
        return true;
    }

    /**
     * 红牌验证
     *
     * @param chain           链信息
     * @param redPunishTxs    红牌交易列表
     * @param blockHeaderHash 区块Hash
     * @param punishAddress   黄牌处罚地址
     * @param block           区块
     */
    private boolean verifyRedPunish(Chain chain, List<Transaction> redPunishTxs, String blockHeaderHash, Set<String> punishAddress, Block block) throws NulsException {
        int countOfTooMuchYP = 0;
        for (Transaction redTx : redPunishTxs) {
            io.nuls.consensus.model.bo.tx.txdata.RedPunishData data = new io.nuls.consensus.model.bo.tx.txdata.RedPunishData();
            data.parse(redTx.getTxData(), 0);
            if (data.getReasonCode() == PunishReasonEnum.TOO_MUCH_YELLOW_PUNISH.getCode()) {
                countOfTooMuchYP++;
                if (!punishAddress.contains(AddressTool.getStringAddressByBytes(data.getAddress()))) {
                    chain.getLogger().info("There is a wrong red punish tx!" + blockHeaderHash);
                    return false;
                }
                if (redTx.getTime() != block.getHeader().getTime()) {
                    chain.getLogger().info("red punish CoinData & TX time is wrong! " + blockHeaderHash);
                    return false;
                }
            } else if (data.getReasonCode() == PunishReasonEnum.BIFURCATION.getCode()) {
                boolean result = verifyBifurcate(chain, redTx, data);
                if (!result) {
                    chain.getLogger().info("red punish CoinData & TX time is wrong! " + blockHeaderHash);
                    return false;
                }
            }
            /*
             CoinData验证
             CoinData verification
             */
            if (!coinDataValidate(chain, redTx, data)) {
                throw new NulsException(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
            }
        }
        if (countOfTooMuchYP != punishAddress.size()) {
            chain.getLogger().info("There is a wrong red punish tx!" + blockHeaderHash);
            return false;
        }
        return true;
    }

    /**
     * 红牌交易验证
     *
     * @param chain chain info
     * @param tx    transaction info
     */
    private boolean verifyBifurcate(Chain chain, Transaction tx, io.nuls.consensus.model.bo.tx.txdata.RedPunishData punishData) throws NulsException {
        /*
         红牌交易类型为连续分叉
        The type of red card transaction is continuous bifurcation
        */
        NulsByteBuffer byteBuffer = new NulsByteBuffer(punishData.getEvidence());
        long[] roundIndex = new long[ConsensusConstant.REDPUNISH_BIFURCATION];
        Set<Long> heightSet = new HashSet<>();
        for (int i = 0; i < ConsensusConstant.REDPUNISH_BIFURCATION && !byteBuffer.isFinished(); i++) {
            BlockHeader header1 = null;
            BlockHeader header2 = null;
            try {
                header1 = byteBuffer.readNulsData(new BlockHeader());
                header2 = byteBuffer.readNulsData(new BlockHeader());
            } catch (NulsException e) {
                chain.getLogger().error(e.getMessage());
            }
            if (null == header1 || null == header2) {
                throw new NulsException(ConsensusErrorCode.DATA_NOT_EXIST);
            }
            //分叉块是不是同一高度
            if (header1.getHeight() != header2.getHeight()) {
                throw new NulsException(ConsensusErrorCode.TX_DATA_VALIDATION_ERROR);
            }
            //分叉块是不是同一个人
            if (!Arrays.equals(header1.getBlockSignature().getPublicKey(), header2.getBlockSignature().getPublicKey())) {
                throw new NulsException(ConsensusErrorCode.BLOCK_SIGNATURE_ERROR);
            }
            BlockExtendsData blockExtendsData = header1.getExtendsData();
            roundIndex[i] = blockExtendsData.getRoundIndex();
            //证据是否重复
            if (heightSet.contains(header1.getHeight())) {
                chain.getLogger().error("Bifurcated evidence with duplicate data");
                return false;
            }
            //验证区块头签名是否正确
            if (header1.getBlockSignature().verifySignature(header1.getHash()).isFailed() || header2.getBlockSignature().verifySignature(header2.getHash()).isFailed()) {
                chain.getLogger().error("Signature verification failed in branching evidence");
                return false;
            }
            //区块签名者是否为节点处罚者
            Agent agent = agentManager.getAgentByAddress(chain, punishData.getAddress());
            if (!Arrays.equals(AddressTool.getAddress(header1.getBlockSignature().getPublicKey(), chain.getConfig().getChainId()), agent.getPackingAddress())
                    || !Arrays.equals(AddressTool.getAddress(header2.getBlockSignature().getPublicKey(), chain.getConfig().getChainId()), agent.getPackingAddress())) {
                chain.getLogger().error("Whether there is a block out of the penalty node in the branch evidence block!");
                return false;
            }
            heightSet.add(header1.getHeight());
        }
        //验证三次分叉是否是100轮以内
        if (roundIndex[ConsensusConstant.REDPUNISH_BIFURCATION - 1] - roundIndex[0] > ConsensusConstant.VALUE_OF_ONE_HUNDRED) {
            throw new NulsException(ConsensusErrorCode.BLOCK_RED_PUNISH_ERROR);
        }
        return true;
    }


    /**
     * CoinData 验证
     * CoinData Verification
     *
     * @param tx    red punish transaction
     * @param chain chain info
     * @return 验证是否成功/Verify success
     */
    private boolean coinDataValidate(Chain chain, Transaction tx, io.nuls.consensus.model.bo.tx.txdata.RedPunishData punishData) throws NulsException {
        Agent punishAgent = null;
        for (Agent agent : chain.getAgentList()) {
            if (agent.getDelHeight() > 0 && (tx.getBlockHeight() <= 0 || agent.getDelHeight() < tx.getBlockHeight())) {
                continue;
            }
            if (Arrays.equals(punishData.getAddress(), agent.getAgentAddress())) {
                punishAgent = agent;
                break;
            }
        }
        if (null == punishAgent) {
            Log.info(ConsensusErrorCode.AGENT_NOT_EXIST.getMsg());
            return false;
        }
        CoinData coinData = coinDataManager.getStopAgentCoinData(chain, punishAgent, tx.getTime() + chain.getConfig().getRedPublishLockTime());
        try {
            CoinFromComparator fromComparator = new CoinFromComparator();
            CoinToComparator toComparator = new CoinToComparator();
            coinData.getFrom().sort(fromComparator);
            coinData.getTo().sort(toComparator);
            CoinData txCoinData = new CoinData();
            txCoinData.parse(tx.getCoinData(), 0);
            txCoinData.getFrom().sort(fromComparator);
            txCoinData.getTo().sort(toComparator);
            if (!Arrays.equals(coinData.serialize(), txCoinData.serialize())) {
                chain.getLogger().error("++++++++++ RedPunish verification does not pass, redPunish type:{}, - height:{}, - redPunish tx timestamp:{}", punishData.getReasonCode(), tx.getBlockHeight(), tx.getTime());
                return false;
            }
        } catch (IOException e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }
}

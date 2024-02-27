package io.nuls.consensus.utils.manager;

import io.nuls.consensus.model.bo.BlockData;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.ChargeResult;
import io.nuls.consensus.model.bo.ChargeResultData;
import io.nuls.common.NerveCoreConfig;
import io.nuls.consensus.model.bo.consensus.AssembleBlockTxResult;
import io.nuls.consensus.model.bo.round.MeetingMember;
import io.nuls.consensus.model.bo.round.MeetingRound;
import io.nuls.consensus.model.po.RandomSeedStatusPo;
import io.nuls.consensus.rpc.call.CallMethodUtils;
import io.nuls.consensus.task.VersionTask;
import io.nuls.consensus.utils.ConsensusAwardUtil;
import io.nuls.consensus.utils.RandomSeedUtils;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.ProtocolVersion;
import io.nuls.base.data.*;
import io.nuls.base.protocol.ModuleHelper;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.consensus.economic.nuls.constant.NulsEconomicConstant;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.constant.ConsensusErrorCode;
import io.nuls.consensus.constant.ParameterConstant;
import io.nuls.consensus.storage.RandomSeedsStorageService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

@Component
public class ConsensusManager {
    @Autowired
    private static RandomSeedsStorageService randomSeedsStorageService;
    @Autowired
    private static PunishManager punishManager;
    @Autowired
    private static NerveCoreConfig config;

    @SuppressWarnings("unchecked")
    public static Block doPacking(Chain chain, MeetingMember member, MeetingRound round, long packingTime, boolean settleConsensusAward) throws Exception {
        int chainId = chain.getConfig().getChainId();
        BlockHeader bestBlock = chain.getBestHeader();
        long packageHeight = bestBlock.getHeight() + 1;
        BlockExtendsData extendsData = new BlockExtendsData(round.getIndex(), round.getMemberCount(), round.getStartTime(), member.getPackingIndexOfRound());
        BlockData bd = new BlockData(packageHeight, bestBlock.getHash(), packingTime, extendsData);
        fillProtocol(extendsData, chainId);
        /*
         * Add support for underlying random numbers
         */
        byte[] packingAddress = member.getAgent().getPackingAddress();
        String packingAddressString = AddressTool.getStringAddressByBytes(packingAddress);
        //supportRandomSeed(extendsData, chainId, packingAddress, packageHeight);

        //Assembly block packaging transaction
//        chain.getLogger().info("Start obtaining transactions：{}", NulsDateUtils.timeStamp2Str(bd.getTime() * 1000));
        bd.setTxList(assembleBlockTx(chain, bd));
//        chain.getLogger().info("Acquisition completed：");

        /*
        Assembly system transactions（CoinBase/Red card/Yellow card）
        Assembly System Transactions (CoinBase/Red/Yellow)
        */
        assembleSystemTx(chain, bestBlock, bd.getTxList(), member, round, packingTime, settleConsensusAward);

        //Assembling blocks
        Block newBlock = createBlock(chain, bd, packingAddress, packingAddressString);

        if (newBlock == null) {
            chain.getLogger().error("Block failure");
            return null;
        }
        VersionTask.exec(packingAddressString);
        chain.getLogger().info("made block height:" + newBlock.getHeader().getHeight() + ",txCount: " + newBlock.getTxs().size() + " , block size: " + newBlock.size() + " , time:" + NulsDateUtils.convertDate(new Date(newBlock.getHeader().getTime() * 1000)) + ",hash:" + newBlock.getHeader().getHash().toHex() + ",preHash:" + newBlock.getHeader().getPreHash().toHex());
        return newBlock;
    }


    /**
     * Low level random number support
     *
     * @param extendsData    Block expansion data
     * @param chainId        chainID
     * @param packingAddress Delivery address
     * @param blockHeight    block height
     */
    private static void supportRandomSeed(BlockExtendsData extendsData, int chainId, byte[] packingAddress, long blockHeight) {
        RandomSeedStatusPo status = randomSeedsStorageService.getAddressStatus(chainId, packingAddress);
        byte[] seed = ConsensusConstant.EMPTY_SEED;
        if (null != status && status.getNextSeed() != null) {
            seed = status.getNextSeed();
        }
        extendsData.setSeed(seed);
        byte[] nextSeed = RandomSeedUtils.createRandomSeed();
        byte[] nextSeedHash = RandomSeedUtils.getLastDigestEightBytes(nextSeed);
        extendsData.setNextSeedHash(nextSeedHash);
        RandomSeedStatusPo po = new RandomSeedStatusPo();
        po.setAddress(packingAddress);
        po.setSeedHash(nextSeedHash);
        po.setNextSeed(nextSeed);
        po.setHeight(blockHeight);
        RandomSeedUtils.CACHE_SEED = po;
    }

    @SuppressWarnings("unchecked")
    private static List<Transaction> assembleBlockTx(Chain chain, BlockData bd) throws Exception {
        /*
         * Get packaged transactions
         */
        Map<String, Object> resultMap = CallMethodUtils.getPackingTxList(chain, bd.getTime());
        List<Transaction> packingTxList = new ArrayList<>();
        BlockExtendsData bestExtendsData = chain.getBestHeader().getExtendsData();
        if (resultMap != null) {
            List<String> txHexList = (List) resultMap.get(ParameterConstant.PARAM_LIST);
            String stateRoot = (String) resultMap.get(ParameterConstant.PARAM_STATE_ROOT);
            if (StringUtils.isBlank(stateRoot)) {
                bd.getExtendsData().setStateRoot(bestExtendsData.getStateRoot());
            } else {
                bd.getExtendsData().setStateRoot(RPCUtil.decode(stateRoot));
            }
            for (String txHex : txHexList) {
                Transaction tx = new Transaction();
                tx.parse(RPCUtil.decode(txHex), 0);
                packingTxList.add(tx);
            }
        } else {
            bd.getExtendsData().setStateRoot(bestExtendsData.getStateRoot());
        }
        return packingTxList;
    }


    /**
     * Assemble block transactions and check if new blocks have been received midway
     *
     * @param chain                Chain information
     * @param bd                   Block assembly data
     * @param packageHeight        loading height
     * @param packingAddressString Block address
     */
    @SuppressWarnings("unchecked")
    private static AssembleBlockTxResult assembleBlockTxResult(Chain chain, BlockData bd, String packingAddressString, long packageHeight) throws Exception {
        /*
         * Get packaged transactions
         */
        Map<String, Object> resultMap = CallMethodUtils.getPackingTxList(chain, bd.getTime());
        List<Transaction> packingTxList = new ArrayList<>();
        /*
         * Check if new blocks have been received during the assembly transaction process
         * Verify that new blocks are received halfway through packaging
         * */
        BlockHeader bestBlock = chain.getBestHeader();
        long realPackageHeight = bestBlock.getHeight() + 1;
        if (!(bd.getPreHash().equals(bestBlock.getHash()) && realPackageHeight > packageHeight)) {
            bd.setHeight(realPackageHeight);
            bd.setPreHash(bestBlock.getHash());
        }
        BlockExtendsData bestExtendsData = chain.getBestHeader().getExtendsData();
        boolean stateRootIsNull = false;
        if (resultMap == null) {
            bd.getExtendsData().setStateRoot(bestExtendsData.getStateRoot());
            stateRootIsNull = true;
        } else {
            long txPackageHeight = Long.parseLong(resultMap.get(ParameterConstant.PARAM_PACKAGE_HEIGHT).toString());
            String stateRoot = (String) resultMap.get(ParameterConstant.PARAM_STATE_ROOT);
            if (StringUtils.isBlank(stateRoot)) {
                bd.getExtendsData().setStateRoot(bestExtendsData.getStateRoot());
                stateRootIsNull = true;
            } else {
                bd.getExtendsData().setStateRoot(RPCUtil.decode(stateRoot));
            }
            if (realPackageHeight >= txPackageHeight) {
                List<String> txHexList = (List) resultMap.get(ParameterConstant.PARAM_LIST);
                for (String txHex : txHexList) {
                    Transaction tx = new Transaction();
                    tx.parse(RPCUtil.decode(txHex), 0);
                    packingTxList.add(tx);
                }
            }
        }
        return new AssembleBlockTxResult(packingTxList, stateRootIsNull);
    }

    /**
     * Assembly system transactions
     * CoinBase transaction & Punish transaction
     *
     * @param chain                chain info
     * @param bestBlock            local highest block/Latest local blocks
     * @param txList               all tx of block/List of transactions that need to be packaged
     * @param member               agent meeting entity/Node packaging information
     * @param round                latest local round/Latest local round information
     * @param time                 Blocking time
     * @param settleConsensusAward Whether to liquidate consensus rewards
     */
    private static void assembleSystemTx(Chain chain, BlockHeader bestBlock, List<Transaction> txList, MeetingMember member, MeetingRound round, long time, boolean settleConsensusAward) throws Exception {
        Transaction coinBaseTransaction = createCoinBaseTx(chain, member.getAgent().getRewardAddress(), txList, time, settleConsensusAward, true);
        txList.add(0, coinBaseTransaction);
        punishManager.punishTx(chain, bestBlock, txList, member, round, time);
    }


    /**
     * Protocol data assembly
     *
     * @param extendsData Block extension data
     * @param chainId     chainID
     */
    private static void fillProtocol(BlockExtendsData extendsData, int chainId) throws NulsException {
        if (ModuleHelper.isSupportProtocolUpdate()) {
            Map map = CallMethodUtils.getVersion(chainId);
            if (map == null || map.isEmpty()) {
                throw new NulsException(ConsensusErrorCode.INTERFACE_CALL_FAILED);
            }
            ProtocolVersion currentProtocolVersion = JSONUtils.map2pojo((Map) map.get(ParameterConstant.PARAM_CURRENT_PROTOCOL_VERSION), ProtocolVersion.class);
            ProtocolVersion localProtocolVersion = JSONUtils.map2pojo((Map) map.get(ParameterConstant.PARAM_LOCAL_PROTOCOL_VERSION), ProtocolVersion.class);
            extendsData.setMainVersion(currentProtocolVersion.getVersion());
            extendsData.setBlockVersion(localProtocolVersion.getVersion());
            extendsData.setEffectiveRatio(localProtocolVersion.getEffectiveRatio());
            extendsData.setContinuousIntervalCount(localProtocolVersion.getContinuousIntervalCount());
        } else {
            extendsData.setMainVersion((short) 1);
            extendsData.setBlockVersion((short) 1);
            extendsData.setEffectiveRatio((byte) 80);
            extendsData.setContinuousIntervalCount((short) 100);
        }
    }

    /**
     * assembleCoinBasetransaction
     * Assembling CoinBase transactions
     *
     * @param chain                chain info
     * @param rewardAddress        packaging information/packing info
     * @param txList               Transaction List/transaction list
     * @param time                 Blocking time
     * @param settleConsensusAward Whether to liquidate consensus rewards
     * @param isPack               Is it during packaging or verification
     * @return Transaction
     */
    public static Transaction createCoinBaseTx(Chain chain, byte[] rewardAddress, List<Transaction> txList, long time, boolean settleConsensusAward, boolean isPack) throws Exception {
        Transaction tx = new Transaction(TxType.COIN_BASE);
        tx.setTime(time);
        /*
        Calculate consensus rewards
        Calculating consensus Awards
        */
        List<CoinTo> rewardList = calcReward(chain, txList, rewardAddress, time, settleConsensusAward, isPack);
        CoinData coinData = new CoinData();
        coinData.setTo(rewardList);
        try {
            tx.setCoinData(coinData.serialize());
        } catch (Exception e) {
            chain.getLogger().error("Coin data serializer error!");
            throw e;
        }
        tx.setTime(time);
        tx.setHash(NulsHash.calcHash(tx.serializeForHash()));
        return tx;
    }

    /**
     * Calculate consensus rewards
     * Calculating consensus Awards
     *
     * @param chain                chain info
     * @param txList               Transaction List/transaction list
     * @param rewardAddress        Local packaging information/local agent packing info
     * @param time                 Block time
     * @param settleConsensusAward Whether to liquidate consensus rewards
     * @param isPack               Is it during packaging or verification
     * @return List<CoinTo>
     */
    private static List<CoinTo> calcReward(Chain chain, List<Transaction> txList, byte[] rewardAddress, long time, boolean settleConsensusAward, boolean isPack) throws NulsException {
        /*
        Asset and consensus reward key value pairs
        Assets and Consensus Award Key Value Pairs
        Key：assetChainId_assetId
        Value: Consensus reward amount
        */
        Map<String, BigInteger> awardAssetMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_4);

        /*
        Calculate intra chain and cross chain transaction fees generated by transactions in blocks
        Calculating intra-chain and cross-chain handling fees for transactions in blocks
        */
        for (Transaction tx : txList) {
            int txType = tx.getType();
            if (txType != TxType.COIN_BASE && txType != TxType.YELLOW_PUNISH && txType != TxType.RED_PUNISH && txType != TxType.RECHARGE && txType != TxType.ONE_CLICK_CROSS_CHAIN && txType != TxType.ADD_FEE_OF_CROSS_CHAIN_BY_CROSS_CHAIN) {
                ChargeResult chargeResult = getFee(tx, chain);
                chargeResult.addOtherCharge(chargeResult.getMainCharge());
                for (ChargeResultData resultData : chargeResult.getOtherCharge()) {
                    if (resultData.getFee().equals(BigInteger.ZERO)) {
                        continue;
                    }
                    String key = resultData.getKey();
                    if (resultData.getFee().compareTo(BigInteger.ZERO) < 0) {
                        chain.getLogger().error("CoinTo < 0 ::tyType={}", txType);
                    }
                    if (awardAssetMap.containsKey(key)) {
                        awardAssetMap.put(key, awardAssetMap.get(key).add(resultData.getFee()));
                    } else {
                        awardAssetMap.put(key, resultData.getFee());
                    }
                }
            }
        }
        List<CoinTo> coinToList = new ArrayList<>();
        //Assembly handling feeCoinData
        if (!awardAssetMap.isEmpty()) {
            for (Map.Entry<String, BigInteger> entry : awardAssetMap.entrySet()) {
                String[] assetInfo = entry.getKey().split(NulsEconomicConstant.SEPARATOR);
                CoinTo coinTo = new CoinTo(rewardAddress, Integer.valueOf(assetInfo[0]), Integer.valueOf(assetInfo[1]), entry.getValue(), 0);
                coinToList.add(coinTo);
            }
        }

        //If this block requires settlement of consensus rewards on the same day, then the consensus rewards on the settlement day
        if (settleConsensusAward) {
            if (isPack) {
                ConsensusAwardUtil.packConsensusAward(chain, coinToList, time);
            } else {
                ConsensusAwardUtil.validBlockConsensusAward(chain, coinToList, time);
            }
        }
        return coinToList;
    }

    /**
     * Calculate transaction fees
     * Calculating transaction fees
     *
     * @param tx    transaction/transaction
     * @param chain chain info
     * @return ChargeResultData
     */
    private static ChargeResult getFee(Transaction tx, Chain chain) throws NulsException {
        CoinData coinData = new CoinData();
        int feeChainId = chain.getConfig().getChainId();
        int feeAssetId = chain.getConfig().getAssetId();
        coinData.parse(tx.getCoinData(), 0);
        /*
        Cross chain transaction calculation fees
        Cross-Chain Transactions Calculate Processing Fees
        */
        if (tx.getType() == TxType.CROSS_CHAIN) {
            /*
            Calculate in chain transaction fees,fromMain assets within the medium chain - toSum of main assets within the mid chain
            Calculate in-chain handling fees, from in-chain main assets - to in-chain main assets and
            */
            if (AddressTool.getChainIdByAddress(coinData.getFrom().get(0).getAddress()) == feeChainId) {
                return getFee(coinData, feeChainId, feeAssetId);
            }
            /*
            Calculate main chain and friend chain transaction fees,First, calculateCoinDataTotal cross chain handling fees in the middle, and then divide the cross chain handling fees according to the proportion
            Calculate the main chain and friendship chain handling fees, first calculate the total cross-chain handling fees in CoinData,
            and then divide the cross-chain handling fees according to the proportion.
            */
            ChargeResult feeData = getFee(coinData, config.getMainChainId(), config.getMainAssetId());
            /*
            If the current chain is the main chain,If the cross chain transaction target is the main chain, the main chain will charge all cross chain transaction fees. If the target is other chains, the main chain will charge a certain proportion of cross chain transaction fees
            If the current chain is the main chain and the target of cross-chain transaction is connected to the main chain, the main chain charges all cross-chain handling fees,
            and if the target is connected to other chains, the main chain charges a certain proportion of cross-chain handling fees.
            */
            int mainCommissionRatio = config.getMainChainCommissionRatio();
            if (feeChainId == config.getMainChainId()) {
                int toChainId = AddressTool.getChainIdByAddress(coinData.getTo().get(0).getAddress());
                if (toChainId == config.getMainChainId()) {
                    return feeData;
                }
                ChargeResultData main = new ChargeResultData(feeData.getMainCharge().getFee().multiply(new BigInteger(String.valueOf(mainCommissionRatio))).divide(new BigInteger(String.valueOf(ConsensusConstant.VALUE_OF_ONE_HUNDRED))), config.getMainChainId(), config.getMainAssetId());
                feeData.setMainCharge(main);
                return feeData;
            }

            ChargeResultData main = new ChargeResultData(feeData.getMainCharge().getFee().multiply(new BigInteger(String.valueOf(ConsensusConstant.VALUE_OF_ONE_HUNDRED - mainCommissionRatio))).divide(new BigInteger(String.valueOf(ConsensusConstant.VALUE_OF_ONE_HUNDRED))), config.getMainChainId(), config.getMainAssetId());
            feeData.setMainCharge(main);
            return feeData;
        } else if (tx.getType() == TxType.REGISTER_AGENT || tx.getType() == TxType.STOP_AGENT || tx.getType() == TxType.DEPOSIT || tx.getType() == TxType.CANCEL_DEPOSIT) {
            feeChainId = chain.getConfig().getAgentChainId();
            feeAssetId = chain.getConfig().getAgentAssetId();
        }
        return getFee(coinData, feeChainId, feeAssetId);
    }

    /**
     * Calculate designated handling fees
     *
     * @param coinData     coinData
     * @param assetChainId Designated asset chainID
     * @param assetId      Designated assetsID
     * @return Handling fee size
     */
    public static ChargeResult getFee(CoinData coinData, int assetChainId, int assetId) {
        ChargeResult result = new ChargeResult();

        Map<String, BigInteger> fromAmountMap = new HashMap<>();
        Map<String, BigInteger> toAmountMap = new HashMap<>();

        BigInteger fromAmount = BigInteger.ZERO;
        BigInteger toAmount = BigInteger.ZERO;


        for (CoinFrom from : coinData.getFrom()) {
            if (from.getAssetsChainId() == assetChainId && from.getAssetsId() == assetId) {
                fromAmount = fromAmount.add(from.getAmount());
            } else {
                String key = from.getAssetsChainId() + ConsensusConstant.SEPARATOR + from.getAssetsId();
                BigInteger fromTotal = fromAmountMap.computeIfAbsent(key, val -> BigInteger.ZERO);
                fromTotal = fromTotal.add(from.getAmount());
                fromAmountMap.put(key, fromTotal);
            }
        }
        for (CoinTo to : coinData.getTo()) {
            if (to.getAssetsChainId() == assetChainId && to.getAssetsId() == assetId) {
                toAmount = toAmount.add(to.getAmount());
            } else {
                String key = to.getAssetsChainId() + ConsensusConstant.SEPARATOR + to.getAssetsId();
                BigInteger toTotal = toAmountMap.computeIfAbsent(key, val -> BigInteger.ZERO);
                toTotal = toTotal.add(to.getAmount());
                toAmountMap.put(key, toTotal);
            }
        }
        fromAmount = fromAmount.subtract(toAmount);
        result.setMainCharge(new ChargeResultData(fromAmount, assetChainId, assetId));
        for (Map.Entry<String, BigInteger> entry : fromAmountMap.entrySet()) {
            BigInteger from = entry.getValue();
            BigInteger to = toAmountMap.get(entry.getKey());
            if (to == null) {
                to = BigInteger.ZERO;
            }
            BigInteger val = from.subtract(to);
            if (val.compareTo(BigInteger.ZERO) > 0) {
                String[] arr = entry.getKey().split(ConsensusConstant.SEPARATOR);
                result.addOtherCharge(new ChargeResultData(val, Integer.parseInt(arr[0]), Integer.parseInt(arr[1])));
            }
        }
        return result;
    }

    /**
     * Create blocks
     * create block
     *
     * @param chain                chain info
     * @param blockData            block entity/Block data
     * @param packingAddress       packing address/Packaging address
     * @param packingAddressString packing address/Packaging address
     * @return Block
     */
    private static Block createBlock(Chain chain, BlockData blockData, byte[] packingAddress, String packingAddressString) {
        try {
            String password = chain.getConfig().getPassword();
            CallMethodUtils.accountValid(chain.getConfig().getChainId(), packingAddressString, password);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return null;
        }
        Block block = new Block();
        block.setTxs(blockData.getTxList());
        BlockHeader header = new BlockHeader();
        block.setHeader(header);
        try {
            header.setExtend(blockData.getExtendsData().serialize());
        } catch (IOException e) {
            chain.getLogger().error(e.getMessage());
            return null;
        }
        header.setHeight(blockData.getHeight());
        header.setTime(blockData.getTime());
        header.setPreHash(blockData.getPreHash());
        header.setTxCount(blockData.getTxList().size());
        header.setPackingAddress(packingAddress);
        List<NulsHash> txHashList = new ArrayList<>();
        for (int i = 0; i < blockData.getTxList().size(); i++) {
            Transaction tx = blockData.getTxList().get(i);
            tx.setBlockHeight(header.getHeight());
            txHashList.add(tx.getHash());
        }
        header.setMerkleHash(NulsHash.calcMerkleHash(txHashList));
        try {
            CallMethodUtils.blockSignature(chain, packingAddressString, header);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return null;
        }
        return block;
    }
}

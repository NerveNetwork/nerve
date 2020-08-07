package network.nerve.pocbft.utils.manager;

import network.nerve.pocbft.model.bo.BlockData;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.ChargeResult;
import network.nerve.pocbft.model.bo.ChargeResultData;
import network.nerve.pocbft.model.bo.config.ConsensusChainConfig;
import network.nerve.pocbft.model.bo.consensus.AssembleBlockTxResult;
import network.nerve.pocbft.model.bo.round.MeetingMember;
import network.nerve.pocbft.model.bo.round.MeetingRound;
import network.nerve.pocbft.model.po.RandomSeedStatusPo;
import network.nerve.pocbft.rpc.call.CallMethodUtils;
import network.nerve.pocbft.utils.ConsensusAwardUtil;
import network.nerve.pocbft.utils.RandomSeedUtils;
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
import io.nuls.economic.nuls.constant.NulsEconomicConstant;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.constant.ConsensusErrorCode;
import network.nerve.pocbft.constant.ParameterConstant;
import network.nerve.pocbft.storage.RandomSeedsStorageService;

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
    private static ConsensusChainConfig config;

    @SuppressWarnings("unchecked")
    public static Block doPacking(Chain chain, MeetingMember member, MeetingRound round, long packingTime, boolean settleConsensusAward) throws Exception {
        int chainId = chain.getConfig().getChainId();
        BlockHeader bestBlock = chain.getBestHeader();
        long packageHeight = bestBlock.getHeight() + 1;
        BlockExtendsData extendsData = new BlockExtendsData(round.getIndex(), round.getMemberCount(), round.getStartTime(), member.getPackingIndexOfRound());
        BlockData bd = new BlockData(packageHeight, bestBlock.getHash(), packingTime, extendsData);
        fillProtocol(extendsData, chainId);
        /*
         * 添加底层随机数支持
         */
        byte[] packingAddress = member.getAgent().getPackingAddress();
        String packingAddressString = AddressTool.getStringAddressByBytes(packingAddress);
        //supportRandomSeed(extendsData, chainId, packingAddress, packageHeight);

        //组装区块打包交易
//        chain.getLogger().info("开始获取交易：{}", NulsDateUtils.timeStamp2Str(bd.getTime() * 1000));
        bd.setTxList(assembleBlockTx(chain, bd));
//        chain.getLogger().info("获取完成：");

        /*
        组装系统交易（CoinBase/红牌/黄牌）
        Assembly System Transactions (CoinBase/Red/Yellow)
        */
        assembleSystemTx(chain, bestBlock, bd.getTxList(), member, round, packingTime, settleConsensusAward);

        //组装区块
        Block newBlock = createBlock(chain, bd, packingAddress, packingAddressString);

        if (newBlock == null) {
            chain.getLogger().error("Block failure");
            return null;
        }

        chain.getLogger().info("==============made block height:" + newBlock.getHeader().getHeight() + ",txCount: " + newBlock.getTxs().size() + " , block size: " + newBlock.size() + " , time:" + NulsDateUtils.convertDate(new Date(newBlock.getHeader().getTime() * 1000)) + ",hash:" + newBlock.getHeader().getHash().toHex() + ",preHash:" + newBlock.getHeader().getPreHash().toHex());
        return newBlock;
    }


    /**
     * 底层随机数支持
     *
     * @param extendsData    区块拓展数据
     * @param chainId        链ID
     * @param packingAddress 处快地址
     * @param blockHeight    区块高度
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
         * 获取打包的交易
         */
        Map<String, Object> resultMap = CallMethodUtils.getPackingTxList(chain, bd.getTime());
        List<Transaction> packingTxList = new ArrayList<>();
        if (resultMap != null) {
            List<String> txHexList = (List) resultMap.get(ParameterConstant.PARAM_LIST);
            for (String txHex : txHexList) {
                Transaction tx = new Transaction();
                tx.parse(RPCUtil.decode(txHex), 0);
                packingTxList.add(tx);
            }
        }
        return packingTxList;
    }


    /**
     * 组装区块交易并检查是否中途收到新区块
     *
     * @param chain                链信息
     * @param bd                   区块组装数据
     * @param packageHeight        打包高度
     * @param packingAddressString 出块地址
     */
    @SuppressWarnings("unchecked")
    private static AssembleBlockTxResult assembleBlockTxResult(Chain chain, BlockData bd, String packingAddressString, long packageHeight) throws Exception {
        /*
         * 获取打包的交易
         */
        Map<String, Object> resultMap = CallMethodUtils.getPackingTxList(chain, bd.getTime());
        List<Transaction> packingTxList = new ArrayList<>();
        /*
         * 检查组装交易过程中是否收到新区块
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
     * 组装系统交易
     * CoinBase transaction & Punish transaction
     *
     * @param chain                chain info
     * @param bestBlock            local highest block/本地最新区块
     * @param txList               all tx of block/需打包的交易列表
     * @param member               agent meeting entity/节点打包信息
     * @param round                latest local round/本地最新轮次信息
     * @param time                 出块时间
     * @param settleConsensusAward 是否清算共识奖励
     */
    private static void assembleSystemTx(Chain chain, BlockHeader bestBlock, List<Transaction> txList, MeetingMember member, MeetingRound round, long time, boolean settleConsensusAward) throws Exception {
        Transaction coinBaseTransaction = createCoinBaseTx(chain, member.getAgent().getRewardAddress(), txList, time, settleConsensusAward, true);
        txList.add(0, coinBaseTransaction);
        punishManager.punishTx(chain, bestBlock, txList, member, round, time);
    }


    /**
     * 协议数据组装
     *
     * @param extendsData 区块扩展数据
     * @param chainId     链ID
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
     * 组装CoinBase交易
     * Assembling CoinBase transactions
     *
     * @param chain                chain info
     * @param rewardAddress        打包信息/packing info
     * @param txList               交易列表/transaction list
     * @param time                 出块时间
     * @param settleConsensusAward 是否清算共识奖励
     * @param isPack               是打包途中还是验证中
     * @return Transaction
     */
    public static Transaction createCoinBaseTx(Chain chain, byte[] rewardAddress, List<Transaction> txList, long time, boolean settleConsensusAward, boolean isPack) throws Exception {
        Transaction tx = new Transaction(TxType.COIN_BASE);
        tx.setTime(time);
        /*
        计算共识奖励
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
     * 计算共识奖励
     * Calculating consensus Awards
     *
     * @param chain                chain info
     * @param txList               交易列表/transaction list
     * @param rewardAddress        本地打包信息/local agent packing info
     * @param time                 区块时间
     * @param settleConsensusAward 是否清算共识奖励
     * @param isPack               是打包途中还是验证中
     * @return List<CoinTo>
     */
    private static List<CoinTo> calcReward(Chain chain, List<Transaction> txList, byte[] rewardAddress, long time, boolean settleConsensusAward, boolean isPack) throws NulsException {
        /*
        资产与共识奖励键值对
        Assets and Consensus Award Key Value Pairs
        Key：assetChainId_assetId
        Value: 共识奖励金额
        */
        Map<String, BigInteger> awardAssetMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_4);

        /*
        计算区块中交易产生的链内和跨链手续费
        Calculating intra-chain and cross-chain handling fees for transactions in blocks
        */
        for (Transaction tx : txList) {
            int txType = tx.getType();
            if (txType != TxType.COIN_BASE && txType != TxType.YELLOW_PUNISH && txType != TxType.RED_PUNISH) {
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
        //组装手续费CoinData
        if (!awardAssetMap.isEmpty()) {
            for (Map.Entry<String, BigInteger> entry : awardAssetMap.entrySet()) {
                String[] assetInfo = entry.getKey().split(NulsEconomicConstant.SEPARATOR);
                CoinTo coinTo = new CoinTo(rewardAddress, Integer.valueOf(assetInfo[0]), Integer.valueOf(assetInfo[1]), entry.getValue(), 0);
                coinToList.add(coinTo);
            }
        }

        //如果本区块需要结算当天共识奖励，则结算当天的共识奖励
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
     * 计算交易手续费
     * Calculating transaction fees
     *
     * @param tx    transaction/交易
     * @param chain chain info
     * @return ChargeResultData
     */
    private static ChargeResult getFee(Transaction tx, Chain chain) throws NulsException {
        CoinData coinData = new CoinData();
        int feeChainId = chain.getConfig().getChainId();
        int feeAssetId = chain.getConfig().getAssetId();
        coinData.parse(tx.getCoinData(), 0);
        /*
        跨链交易计算手续费
        Cross-Chain Transactions Calculate Processing Fees
        */
        if (tx.getType() == TxType.CROSS_CHAIN) {
            /*
            计算链内手续费，from中链内主资产 - to中链内主资产的和
            Calculate in-chain handling fees, from in-chain main assets - to in-chain main assets and
            */
            if (AddressTool.getChainIdByAddress(coinData.getFrom().get(0).getAddress()) == feeChainId) {
                return getFee(coinData, feeChainId, feeAssetId);
            }
            /*
            计算主链和友链手续费,首先计算CoinData中总的跨链手续费，然后根据比例分跨链手续费
            Calculate the main chain and friendship chain handling fees, first calculate the total cross-chain handling fees in CoinData,
            and then divide the cross-chain handling fees according to the proportion.
            */
            ChargeResult feeData = getFee(coinData, config.getMainChainId(), config.getMainAssetId());
            /*
            如果当前链为主链,且跨链交易目标连为主链则主链收取全部跨链手续费，如果目标连为其他链则主链收取一定比例的跨链手续费
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
     * 计算指定手续费
     *
     * @param coinData     coinData
     * @param assetChainId 指定资产链ID
     * @param assetId      指定资产ID
     * @return 手续费大小
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
     * 创建区块
     * create block
     *
     * @param chain                chain info
     * @param blockData            block entity/区块数据
     * @param packingAddress       packing address/打包地址
     * @param packingAddressString packing address/打包地址
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

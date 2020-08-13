package io.nuls.api.analysis;

import io.nuls.api.ApiContext;
import io.nuls.api.cache.ApiCache;
import io.nuls.api.constant.*;
import io.nuls.api.db.SymbolRegService;
import io.nuls.api.exception.UnableAssetException;
import io.nuls.api.manager.CacheManager;
import io.nuls.api.model.dto.LedgerAssetDTO;
import io.nuls.api.model.entity.*;
import io.nuls.api.model.po.*;
import io.nuls.api.model.po.mini.CancelDepositInfo;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.*;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.TxStatusEnum;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AnalysisHandler {

    /**
     * Convert block information to blockInfo information
     * 将block信息转换为blockInfo信息
     *
     * @param blockHex
     * @param chainId
     * @return
     * @throws Exception
     */
    public static BlockInfo toBlockInfo(String blockHex, int chainId) throws Exception {
        byte[] bytes = RPCUtil.decode(blockHex);
        Block block = new Block();
        block.parse(new NulsByteBuffer(bytes));

        BlockInfo blockInfo = new BlockInfo();
        BlockHeaderInfo blockHeader = toBlockHeaderInfo(block.getHeader(), chainId);
        blockHeader.setSize(bytes.length);
        blockHeader.setTxHashList(new ArrayList<>());

        BlockHexInfo hexInfo = new BlockHexInfo();
        hexInfo.setHeight(blockHeader.getHeight());
        hexInfo.setBlockHex(blockHex);
        blockInfo.setBlockHexInfo(hexInfo);

        blockInfo.setTxList(toTxs(chainId, block.getTxs(), blockHeader));
        //计算coinBase奖励
        blockHeader.setReward(calcCoinBaseReward(chainId, blockInfo.getTxList().get(0)));

        //计算总手续费
        blockHeader.setTotalFee(calcFee(blockInfo.getTxList(), chainId));

        //重新计算区块打包的交易个数
        blockHeader.setTxCount(blockInfo.getTxList().size());
        blockInfo.setHeader(blockHeader);
        return blockInfo;
    }


//    public static BlockInfo toBlockInfo(String blockHex, Map<String, ContractResultInfo> resultInfoMap, int chainId) throws Exception {
//        byte[] bytes = RPCUtil.decode(blockHex);
//        Block block = new Block();
//        block.parse(new NulsByteBuffer(bytes));
//
//        BlockInfo blockInfo = new BlockInfo();
//        BlockHeaderInfo blockHeader = toBlockHeaderInfo(block.getHeader(), chainId);
//        blockHeader.setSize(bytes.length);
//        blockHeader.setTxHashList(new ArrayList<>());
//
//        //执行成功的智能合约可能会产生系统内部交易，内部交易的序列化信息存放在执行结果中,将内部交易反序列后，一起解析
//        //A successful intelligent contract execution may result in system internal trading.
//        // The serialized information of internal trading is stored in the execution result, and the internal trading is reversed and parsed together
//        if (resultInfoMap != null) {
//            for (ContractResultInfo resultInfo : resultInfoMap.values()) {
//                if (resultInfo.getContractTxList() != null) {
//                    for (String txHex : resultInfo.getContractTxList()) {
//                        Transaction tx = new Transaction();
//                        tx.parse(new NulsByteBuffer(RPCUtil.decode(txHex)));
//                        tx.setBlockHeight(blockHeader.getHeight());
//                        block.getTxs().add(tx);
//                    }
//                }
//            }
//        }
//        blockInfo.setTxList(toTxs(chainId, block.getTxs(), blockHeader, resultInfoMap));
//        //计算coinBase奖励
//        blockHeader.setReward(calcCoinBaseReward(chainId, blockInfo.getTxList().get(0)));
//        //计算总手续费
//        blockHeader.setTotalFee(calcFee(blockInfo.getTxList(), chainId));
//        //重新计算区块打包的交易个数
//        blockHeader.setTxCount(blockInfo.getTxList().size());
//        blockInfo.setHeader(blockHeader);
//        return blockInfo;
//    }


    public static BlockHeaderInfo toBlockHeaderInfo(BlockHeader blockHeader, int chainId) throws IOException {
        BlockExtendsData extendsData = blockHeader.getExtendsData();

        BlockHeaderInfo info = new BlockHeaderInfo();
        info.setHash(blockHeader.getHash().toHex());
        info.setHeight(blockHeader.getHeight());
        info.setPreHash(blockHeader.getPreHash().toHex());
        info.setMerkleHash(blockHeader.getMerkleHash().toHex());
        info.setCreateTime(blockHeader.getTime());
        info.setPackingAddress(AddressTool.getStringAddressByBytes(blockHeader.getPackingAddress(chainId)));
        info.setTxCount(blockHeader.getTxCount());
        info.setRoundIndex(extendsData.getRoundIndex());
        info.setPackingIndexOfRound(extendsData.getPackingIndexOfRound());
        info.setScriptSign(HexUtil.encode(blockHeader.getBlockSignature().serialize()));
        info.setAgentVersion(extendsData.getBlockVersion());
        info.setRoundStartTime(extendsData.getRoundStartTime());
        info.setAgentVersion(extendsData.getBlockVersion());
        //是否是种子节点打包的区块
        ApiCache apiCache = CacheManager.getCache(chainId);
        if (apiCache.getChainInfo().getSeeds().contains(info.getPackingAddress()) || info.getHeight() == 0) {
            info.setSeedPacked(true);
        }
        return info;
    }

    public static List<TransactionInfo> toTxs(int chainId, List<Transaction> txList, BlockHeaderInfo blockHeader) throws Exception {
        List<TransactionInfo> txs = new ArrayList<>();
        for (int i = 0; i < txList.size(); i++) {
            Transaction tx = txList.get(i);
            tx.setStatus(TxStatusEnum.CONFIRMED);
            TransactionInfo txInfo;
            try {
                txInfo = toTransaction(chainId, tx);
                txInfo.setCreateTime(blockHeader.getCreateTime());
            }catch (UnableAssetException unableAssetException){
                Log.error("解析交易失败:{}",unableAssetException.getCustomMessage());
                continue;
            }
            if (txInfo.getType() == TxType.RED_PUNISH) {
                PunishLogInfo punishLog = (PunishLogInfo) txInfo.getTxData();
                punishLog.setRoundIndex(blockHeader.getRoundIndex());
                punishLog.setPackageIndex(blockHeader.getPackingIndexOfRound());
            } else if (txInfo.getType() == TxType.YELLOW_PUNISH) {
                for (TxDataInfo txData : txInfo.getTxDataList()) {
                    PunishLogInfo punishLog = (PunishLogInfo) txData;
                    punishLog.setRoundIndex(blockHeader.getRoundIndex());
                    punishLog.setPackageIndex(blockHeader.getPackingIndexOfRound());
                }
            }
            txs.add(txInfo);
            blockHeader.getTxHashList().add(txInfo.getHash());
        }
        return txs;
    }

    public static TransactionInfo toTransaction(int chainId, Transaction tx) throws Exception {
        TransactionInfo info = new TransactionInfo();
        info.setHash(tx.getHash().toHex());
        info.setHeight(tx.getBlockHeight());
        info.setType(tx.getType());
        info.setSize(tx.getSize());
        info.setCreateTime(tx.getTime());
        if (tx.getTxData() != null) {
            info.setTxDataHex(RPCUtil.encode(tx.getTxData()));
        }
        if (tx.getRemark() != null) {
            info.setRemark(new String(tx.getRemark(), StandardCharsets.UTF_8));
        }
        if (tx.getStatus() == TxStatusEnum.CONFIRMED) {
            info.setStatus(ApiConstant.TX_CONFIRM);
        } else {
            info.setStatus(ApiConstant.TX_UNCONFIRM);
        }

        CoinData coinData = new CoinData();
        if (tx.getCoinData() != null) {
            coinData.parse(new NulsByteBuffer(tx.getCoinData()));
            info.setCoinFroms(toCoinFromList(coinData));
            info.setCoinTos(toCoinToList(coinData));
        }
        info.setTxData(toTxData(chainId, tx));
        info.setTxDataList(toTxDataList(chainId, tx));
        info.calcValue();
        info.calcFee(chainId);
        return info;
    }

    public static List<CoinFromInfo> toCoinFromList(CoinData coinData) throws UnableAssetException {
        if (coinData == null || coinData.getFrom() == null) {
            return null;
        }
        List<CoinFromInfo> fromInfoList = new ArrayList<>();
        SymbolRegService symbolRegService = SpringLiteContext.getBean(SymbolRegService.class);
        for (CoinFrom from : coinData.getFrom()) {
            CoinFromInfo fromInfo = new CoinFromInfo();
            fromInfo.setAddress(AddressTool.getStringAddressByBytes(from.getAddress()));
            fromInfo.setAssetsId(from.getAssetsId());
            fromInfo.setChainId(from.getAssetsChainId());
            fromInfo.setLocked(from.getLocked());
            fromInfo.setAmount(from.getAmount());
            fromInfo.setNonce(HexUtil.encode(from.getNonce()));
            SymbolRegInfo symbolRegInfo = symbolRegService.get(from.getAssetsChainId(),from.getAssetsId());
            if(symbolRegInfo == null){
                Log.error("数据异常，未找到对应资产注册信息:{}",from.getAssetsChainId() + "-" + from.getAssetsId());
                throw new UnableAssetException(from.getAssetsChainId(),from.getAssetsId());
            }
            fromInfo.setSymbol(symbolRegInfo.getSymbol());
            fromInfoList.add(fromInfo);
        }
        return fromInfoList;
    }

    public static List<CoinToInfo> toCoinToList(CoinData coinData) throws UnableAssetException {
        if (coinData == null || coinData.getTo() == null) {
            return null;
        }
        List<CoinToInfo> toInfoList = new ArrayList<>();
        SymbolRegService symbolRegService = SpringLiteContext.getBean(SymbolRegService.class);
        for (CoinTo to : coinData.getTo()) {
            CoinToInfo coinToInfo = new CoinToInfo();
            coinToInfo.setAddress(AddressTool.getStringAddressByBytes(to.getAddress()));
            coinToInfo.setAssetsId(to.getAssetsId());
            coinToInfo.setChainId(to.getAssetsChainId());
            coinToInfo.setLockTime(to.getLockTime());
            coinToInfo.setAmount(to.getAmount());
            SymbolRegInfo symbolRegInfo = symbolRegService.get(coinToInfo.getChainId(),coinToInfo.getAssetsId());
            if(symbolRegInfo == null){
                Log.error("数据异常，未找到对应资产注册信息:{}",coinToInfo.getChainId() + "-" + coinToInfo.getAssetsId());
                throw new UnableAssetException(coinToInfo.getChainId(),coinToInfo.getAssetsId());
            }
            coinToInfo.setSymbol(symbolRegInfo.getSymbol());
            toInfoList.add(coinToInfo);
        }
        return toInfoList;
    }

    public static TxDataInfo toTxData(int chainId, Transaction tx) throws NulsException {
//        Log.info("接收到新交易，交易类型:{}",tx.getType());
        if (tx.getType() == TxType.ACCOUNT_ALIAS) {
            return toAlias(tx);
        } else if (tx.getType() == TxType.REGISTER_AGENT ) {
            return toAgent(tx);
        } else if (tx.getType() == TxType.DEPOSIT) {
            return toDeposit(tx);
        } else if (tx.getType() == TxType.CANCEL_DEPOSIT) {
            return toCancelDeposit(tx);
        } else if (tx.getType() == TxType.STOP_AGENT) {
            return toStopAgent(tx);
        } else if (tx.getType() == TxType.RED_PUNISH) {
            return toRedPublishLog(tx);
        } else if (tx.getType() == TxType.APPEND_AGENT_DEPOSIT || tx.getType() == TxType.REDUCE_AGENT_DEPOSIT) {
            return toChangeAgentDepositData(tx);
        } else if (tx.getType() == TxType.CONFIRM_WITHDRAWAL || tx.getType() == TxType.RECHARGE) {
            return toConverterTxInfo(chainId,tx);
        } else if (tx.getType() == TxType.LEDGER_ASSET_REG_TRANSFER) {
            return toLedgerAssetInfo(chainId, tx);
        } else if (tx.getType() == TxType.CHANGE_VIRTUAL_BANK ) {
            return toChangeVirtualBank(tx);
        }
//        Log.warn("未处理交易类型：{}",tx.getType());
        return null;
    }

    private static List<TxDataInfo> toTxDataList(int chainId, Transaction tx) throws NulsException {
        switch (tx.getType()) {
            case TxType.YELLOW_PUNISH:
                return toYellowPunish(tx);
            case TxType.FINAL_QUOTATION:
                return toSymbolPriceInfo(tx);
            case TxType.QUOTATION:
                return toSymbolQuotationRecordInfo(tx);
            case TxType.CROSS_CHAIN:
                return toNativePlatformCrossTxInfo(chainId,tx);
            default:
                return List.of();
        }
    }

    /**
     * 解析最终喂价交易
     *
     * @param tx
     * @return
     * @throws Exception
     */
    public static List<TxDataInfo> toSymbolPriceInfo(Transaction tx) throws NulsException {
        Log.info("开始处理喂价最终确认交易:{}", tx.getBlockHeight());
        SymbolPrices symbolPrices = new SymbolPrices();
        symbolPrices.parse(new NulsByteBuffer(tx.getTxData()));
        return toSymbolPriceInfo(symbolPrices, tx, StackSymbolPriceInfo.class);
    }

    public static List<TxDataInfo> toSymbolPriceInfo(SymbolPrices symbolPrices, Transaction tx, Class<? extends StackSymbolPriceInfo> clazs) throws NulsException {
        return symbolPrices.getPrices().stream().map(p -> {
            StackSymbolPriceInfo spi = null;
            try {
                spi = clazs.getConstructor().newInstance();
            } catch (Exception e) {
                Log.error("create {} instance error", clazs);
                System.exit(0);
            }
            spi.setBlockHeight(tx.getBlockHeight());
            spi.setCreateTime(tx.getTime() * 1000L);
            spi.setCurrency(p.getCurrency());
            spi.setPrice(p.getPrice());
            spi.setSymbol(p.getSymbol());
            spi.setTxHash(tx.getHash().toHex());
            return spi;
        }).collect(Collectors.toList());
    }

    /**
     * 解析喂价提交交易
     *
     * @param tx
     * @return
     * @throws NulsException
     */
    public static List<TxDataInfo> toSymbolQuotationRecordInfo(Transaction tx) throws NulsException {
        Log.info("开始处理喂价提交交易:{}", tx.getBlockHeight());
        Quotation quotation = new Quotation();
        quotation.parse(new NulsByteBuffer(tx.getTxData()));
        List<TxDataInfo> prices = toSymbolPriceInfo(quotation.getPrices(), tx, SymbolQuotationRecordInfo.class);
        return prices.stream().map(d -> {
            SymbolQuotationRecordInfo info = (SymbolQuotationRecordInfo) d;
            info.setAddress(AddressTool.getStringAddressByBytes(quotation.getAddress()));
            info.setCreateTime(tx.getTime() * 1000L);
            return info;
        }).collect(Collectors.toList());
    }


    public static AliasInfo toAlias(Transaction tx) throws NulsException {
        Alias alias = new Alias();
        alias.parse(new NulsByteBuffer(tx.getTxData()));
        AliasInfo info = new AliasInfo();
        info.setAddress(AddressTool.getStringAddressByBytes(alias.getAddress()));
        info.setAlias(alias.getAlias());
        info.setBlockHeight(tx.getBlockHeight());
        return info;
    }

    public static AgentInfo toAgent(Transaction tx) throws NulsException {
        Agent agent = new Agent();
        agent.parse(new NulsByteBuffer(tx.getTxData()));
        AgentInfo agentInfo = new AgentInfo();
        agentInfo.init();
        agentInfo.setAgentAddress(AddressTool.getStringAddressByBytes(agent.getAgentAddress()));
        agentInfo.setPackingAddress(AddressTool.getStringAddressByBytes(agent.getPackingAddress()));
        agentInfo.setRewardAddress(AddressTool.getStringAddressByBytes(agent.getRewardAddress()));
        agentInfo.setDeposit(agent.getDeposit());
        agentInfo.setCreateTime(tx.getTime());
        agentInfo.setTxHash(tx.getHash().toHex());
        agentInfo.setAgentId(agentInfo.getTxHash().substring(agentInfo.getTxHash().length() - 8));
        agentInfo.setBlockHeight(tx.getBlockHeight());
        return agentInfo;
    }

    public static DepositInfo toChangeAgentDepositData(Transaction tx) throws NulsException {
        ChangeAgentDepositData changeAgentDepositData = new ChangeAgentDepositData();
        changeAgentDepositData.parse(new NulsByteBuffer(tx.getTxData()));
        DepositInfo depositInfo = new DepositInfo();
        depositInfo.setTxHash(tx.getHash().toHex());
        depositInfo.setAmount(changeAgentDepositData.getAmount());
        depositInfo.setAgentHash(changeAgentDepositData.getAgentHash().toHex());
        depositInfo.setAddress(AddressTool.getStringAddressByBytes(changeAgentDepositData.getAddress()));
        depositInfo.setCreateTime(tx.getTime());
        depositInfo.setBlockHeight(tx.getBlockHeight());
        depositInfo.setFee(tx.getFee());
        return depositInfo;
    }

    public static DepositInfo toDeposit(Transaction tx) throws NulsException {
        Deposit deposit = new Deposit();
        deposit.parse(new NulsByteBuffer(tx.getTxData()));
        DepositInfo info = new DepositInfo();
        info.setTxHash(tx.getHash().toHex());
        info.setAmount(deposit.getDeposit());
        info.setAddress(AddressTool.getStringAddressByBytes(deposit.getAddress()));
        info.setTxHash(tx.getHash().toHex());
        info.setCreateTime(tx.getTime());
        info.setBlockHeight(tx.getBlockHeight());
        info.setFee(tx.getFee());
        info.setAssetChainId(deposit.getAssetChainId());
        info.setAssetId(deposit.getAssetId());
        if (deposit.getDepositType() == StacKType.REGULAR) {
            DepositFixedType depositFixedType = DepositFixedType.getValue(deposit.getTimeType());
            if (depositFixedType == null) {
                Log.warn("未知的定期时间类型:{}", deposit.getTimeType());
            } else {
                info.setFixedType(depositFixedType.name());
            }
        }
        return info;
    }

    public static CancelDepositInfo toCancelDeposit(Transaction tx) throws NulsException {
        CancelDeposit cancelDeposit = new CancelDeposit();
        cancelDeposit.parse(new NulsByteBuffer(tx.getTxData()));
        CancelDepositInfo deposit = new CancelDepositInfo();
        deposit.setJoinTxHash(cancelDeposit.getJoinTxHash().toHex());
        deposit.setAddress(AddressTool.getStringAddressByBytes(cancelDeposit.getAddress()));
        deposit.setCreateTime(tx.getTime());
        deposit.setBlockHeight(tx.getBlockHeight());
        deposit.setTxHash(tx.getHash().toHex());
        return deposit;
    }

    public static AgentInfo toStopAgent(Transaction tx) throws NulsException {
        StopAgent stopAgent = new StopAgent();
        stopAgent.parse(new NulsByteBuffer(tx.getTxData()));

        AgentInfo agentNode = new AgentInfo();
        agentNode.setTxHash(stopAgent.getCreateTxHash().toHex());

        return agentNode;
    }

    public static List<TxDataInfo> toYellowPunish(Transaction tx) throws NulsException {
        YellowPunishData data = new YellowPunishData();
        data.parse(new NulsByteBuffer(tx.getTxData()));
        List<TxDataInfo> logList = new ArrayList<>();
        for (byte[] address : data.getAddressList()) {
            PunishLogInfo log = new PunishLogInfo();
            log.setTxHash(tx.getHash().toHex());
            log.setAddress(AddressTool.getStringAddressByBytes(address));
            log.setBlockHeight(tx.getBlockHeight());
            log.setTime(tx.getTime());
            log.setType(ApiConstant.PUBLISH_YELLOW);
            log.setReason("No packaged blocks");
            logList.add(log);
        }
        return logList;
    }

    public static PunishLogInfo toRedPublishLog(Transaction tx) throws NulsException {
        RedPunishData data = new RedPunishData();
        data.parse(new NulsByteBuffer(tx.getTxData()));

        PunishLogInfo punishLog = new PunishLogInfo();
        punishLog.setTxHash(tx.getHash().toHex());
        punishLog.setType(ApiConstant.PUBLISH_RED);
        punishLog.setAddress(AddressTool.getStringAddressByBytes(data.getAddress()));
        if (data.getReasonCode() == ApiConstant.TRY_FORK) {
            punishLog.setReason("Trying to bifurcate many times");
        } else if (data.getReasonCode() == ApiConstant.TOO_MUCH_YELLOW_PUNISH) {
            punishLog.setReason("too much yellow publish");
        }
        punishLog.setBlockHeight(tx.getBlockHeight());
        punishLog.setTime(tx.getTime());
        return punishLog;
    }

    public static ChangeVirtualBankInfo toChangeVirtualBank(Transaction tx) throws NulsException {
        ChangeVirtualBankTxData changeVirtualBankTxData = new ChangeVirtualBankTxData();
        changeVirtualBankTxData.parse(new NulsByteBuffer(tx.getTxData()));
        ChangeVirtualBankInfo info = new ChangeVirtualBankInfo();
        if(changeVirtualBankTxData.getInAgents() != null){
            info.setInAgents(changeVirtualBankTxData.getInAgents().stream().map(AddressTool::getStringAddressByBytes).collect(Collectors.toList()));
        }else{
            info.setInAgents(List.of());
        }

        if(changeVirtualBankTxData.getOutAgents() != null){
            info.setOutAgents(changeVirtualBankTxData.getOutAgents().stream().map(AddressTool::getStringAddressByBytes).collect(Collectors.toList()));
        }else{
            info.setOutAgents(List.of());
        };
        return info;
    }

//
//
//    private static ChainInfo toChainInfo(Transaction tx) throws NulsException {
//        ChainInfo chainInfo = new ChainInfo();
//        if (ApiContext.protocolVersion < 4) {
//            TxChain txChain = new TxChain();
//            txChain.parse(new NulsByteBuffer(tx.getTxData()));
//            chainInfo.setChainId(txChain.getDefaultAsset().getChainId());
//
//            AssetInfo assetInfo = new AssetInfo();
//            assetInfo.setAssetId(txChain.getDefaultAsset().getAssetId());
//            assetInfo.setChainId(txChain.getDefaultAsset().getChainId());
//            assetInfo.setSymbol(txChain.getDefaultAsset().getSymbol());
//            assetInfo.setInitCoins(txChain.getDefaultAsset().getInitNumber());
//            chainInfo.setDefaultAsset(assetInfo);
//            chainInfo.getAssets().add(assetInfo);
//        } else {
//            io.nuls.api.model.entity.v4.TxChain txChain = new io.nuls.api.model.entity.v4.TxChain();
//            txChain.parse(new NulsByteBuffer(tx.getTxData()));
//            chainInfo.setChainId(txChain.getDefaultAsset().getChainId());
//
//            AssetInfo assetInfo = new AssetInfo();
//            assetInfo.setAssetId(txChain.getDefaultAsset().getAssetId());
//            assetInfo.setChainId(txChain.getDefaultAsset().getChainId());
//            assetInfo.setSymbol(txChain.getDefaultAsset().getSymbol());
//            assetInfo.setInitCoins(txChain.getDefaultAsset().getInitNumber());
//            chainInfo.setDefaultAsset(assetInfo);
//            chainInfo.getAssets().add(assetInfo);
//        }
//
//
//        return chainInfo;
//    }
//
//    private static AssetInfo toAssetInfo(Transaction tx) throws NulsException {
//        AssetInfo assetInfo = new AssetInfo();
//        if (ApiContext.protocolVersion >= 4) {
//            io.nuls.api.model.entity.v4.TxAsset txAsset = new io.nuls.api.model.entity.v4.TxAsset();
//            txAsset.parse(new NulsByteBuffer(tx.getTxData()));
//
//            assetInfo.setAssetId(txAsset.getAssetId());
//            assetInfo.setChainId(txAsset.getChainId());
//            assetInfo.setSymbol(txAsset.getSymbol());
//            assetInfo.setInitCoins(txAsset.getInitNumber());
//            assetInfo.setAddress("");
//        } else {
//            TxAsset txAsset = new TxAsset();
//            txAsset.parse(new NulsByteBuffer(tx.getTxData()));
//
//            assetInfo.setAssetId(txAsset.getAssetId());
//            assetInfo.setChainId(txAsset.getChainId());
//            assetInfo.setSymbol(txAsset.getSymbol());
//            assetInfo.setInitCoins(txAsset.getInitNumber());
//            assetInfo.setAddress(AddressTool.getStringAddressByBytes(txAsset.getAddress()));
//        }
//
//        return assetInfo;
//    }

    private static ConverterTxInfo coinToConverterTxInfo(Coin coin, Transaction tx,ConverterTxType converterTxType){
        ConverterTxInfo info = new ConverterTxInfo();
        info.setBlockHeight(tx.getBlockHeight());
        info.setCreateTime(tx.getTime() * 1000L);
        info.setTxHash(tx.getHash().toHex());
        info.setCrossChainType(CrossChainType.NulsPlatform.name());
        info.setConverterType(converterTxType.name());
        info.setAmount(coin.getAmount());
        info.setAssetChainId(coin.getAssetsChainId());
        info.setAssetId(coin.getAssetsId());
        info.setAddress(AddressTool.getStringAddressByBytes(coin.getAddress()));
        return info;
    }

    private static List<TxDataInfo> toNativePlatformCrossTxInfo(int chainId, Transaction tx) throws NulsException {
        boolean isIn = tx.getCoinDataInstance().getFrom().stream().anyMatch(d->AddressTool.getChainIdByAddress(d.getAddress()) != ApiContext.defaultChainId);
        ConverterTxType converterTxType = isIn ? ConverterTxType.IN : ConverterTxType.OUT;
        return tx.getCoinDataInstance().getTo().stream().map(d-> coinToConverterTxInfo(d,tx,converterTxType)).collect(Collectors.toList());
    }

    private static ConverterTxInfo toConverterTxInfo(int chainId, Transaction tx) throws NulsException {
        ConverterTxInfo info = new ConverterTxInfo();
        info.setBlockHeight(tx.getBlockHeight());
        info.setCreateTime(tx.getTime() * 1000L);
        info.setCrossChainType(CrossChainType.CrossPlatform.name());
        if(tx.getType() == TxType.CONFIRM_WITHDRAWAL){
            ConfirmWithdrawalTxData txData = new ConfirmWithdrawalTxData();
            txData.parse(new NulsByteBuffer(tx.getTxData()));
            Result<TransactionInfo> transactionInfoResult = WalletRpcHandler.getTx(chainId,txData.getWithdrawalTxHash().toHex());
            if(transactionInfoResult.isFailed()){
                Log.error("获取异构跨链提现交易失败");
                throw new NulsException(CommonCodeConstanst.DATA_ERROR);
            }
            TransactionInfo txInfo = transactionInfoResult.getData();
            CoinToInfo coinTo = txInfo.getCoinTos().stream().filter(d -> d.getChainId() != ApiContext.defaultChainId || d.getAssetsId() != ApiContext.defaultAssetId).findFirst().get();
            info.setTxHash(txInfo.getHash());
            info.setAmount(coinTo.getAmount());
            info.setAssetChainId(coinTo.getChainId());
            info.setAssetId(coinTo.getAssetsId());
            info.setAddress(coinTo.getAddress());
            info.setConverterType(ConverterTxType.OUT.name());
            info.setOuterTxHash(txData.getHeterogeneousTxHash());
        }else{
            info.setTxHash(tx.getHash().toHex());
            CoinTo coinTo = tx.getCoinDataInstance().getTo().stream().filter(d -> d.getAssetsChainId() != ApiContext.defaultChainId || d.getAssetsId() != ApiContext.defaultAssetId).findFirst().get();
            info.setAmount(coinTo.getAmount());
            info.setAssetChainId(coinTo.getAssetsChainId());
            info.setAssetId(coinTo.getAssetsId());
            info.setAddress(AddressTool.getStringAddressByBytes(coinTo.getAddress()));
            RechargeTxData txData = new RechargeTxData();
            txData.parse(new NulsByteBuffer(tx.getTxData()));
            info.setConverterType(ConverterTxType.IN.name());
            info.setOuterTxHash(txData.getHeterogeneousTxHash());
        }
        return info;
    }


    /**
     * 转换账本链上资产注册交易
     *
     * @param tx
     * @return
     * @throws NulsException
     */
    private static LedgerRegAssetInfo toLedgerAssetInfo(int chainId, Transaction tx) throws NulsException {
        TxLedgerAsset txLedgerAsset = new TxLedgerAsset();
        txLedgerAsset.parse(new NulsByteBuffer(tx.getTxData()));
        LedgerRegAssetInfo info = new LedgerRegAssetInfo();
        info.setAddress(AddressTool.getStringAddressByBytes(txLedgerAsset.getAddress()));
        Result<LedgerAssetDTO> res = WalletRpcHandler.getAssetInfo(chainId, tx.getHash().toHex());
        if (res.isSuccess()) {
            LedgerAssetDTO dto = res.getData();
            info.setAssetChainId(chainId);
            info.setAssetId(dto.getAssetId());
            info.setAddress(dto.getAssetOwnerAddress());
            info.setInitNumber(dto.getInitNumber().multiply(BigInteger.TEN.pow(dto.getDecimalPlace())));
            info.setSymbol(dto.getAssetSymbol());
            info.setDecimal(dto.getDecimalPlace());
            info.set_id(tx.getHash().toHex());
            info.setBlockHeight(tx.getBlockHeight());
            return info;
        } else {
            return null;
        }
    }

    public static BigInteger calcCoinBaseReward(int chainId, TransactionInfo coinBaseTx) {
        BigInteger reward = BigInteger.ZERO;
        if (coinBaseTx.getCoinTos() == null) {
            return reward;
        }
        //奖励只计算本链的共识资产
        AssetInfo assetInfo = CacheManager.getCacheChain(chainId).getDefaultAsset();
        for (CoinToInfo coinTo : coinBaseTx.getCoinTos()) {
            if (coinTo.getChainId() == assetInfo.getChainId() || coinTo.getAssetsId() == assetInfo.getAssetId()) {
                reward = reward.add(coinTo.getAmount());
            }
        }
        return reward;
    }

    public static BigInteger calcFee(List<TransactionInfo> txs, int chainId) {
        BigInteger fee = BigInteger.ZERO;
        //手续费只计算本链的共识资产
        AssetInfo assetInfo = CacheManager.getCacheChain(chainId).getDefaultAsset();
        for (int i = 1; i < txs.size(); i++) {
            FeeInfo feeInfo = txs.get(i).getFee();
            if (feeInfo.getChainId() == assetInfo.getChainId() && feeInfo.getAssetId() == assetInfo.getAssetId()) {
                fee = fee.add(feeInfo.getValue());
            }
        }
        return fee;
    }


}

package io.nuls.api.rpc.controller;

import io.nuls.api.ApiContext;
import io.nuls.api.analysis.WalletRpcHandler;
import io.nuls.api.cache.ApiCache;
import io.nuls.api.constant.AddressType;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.constant.config.ApiConfig;
import io.nuls.api.db.*;
import io.nuls.api.exception.JsonRpcException;
import io.nuls.api.manager.CacheManager;
import io.nuls.api.model.dto.SymbolUsdPercentDTO;
import io.nuls.api.model.po.*;
import io.nuls.api.model.rpc.RpcErrorCode;
import io.nuls.api.model.rpc.RpcResult;
import io.nuls.api.model.rpc.RpcResultError;
import io.nuls.api.model.rpc.SearchResultDTO;
import io.nuls.api.service.SymbolUsdtPriceProviderService;
import io.nuls.api.utils.AssetTool;
import io.nuls.api.utils.DBUtil;
import io.nuls.api.utils.VerifyUtils;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Controller;
import io.nuls.core.core.annotation.RpcMethod;
import io.nuls.core.core.config.ConfigurationLoader;
import io.nuls.core.parse.MapUtils;
import io.nuls.core.rpc.model.ModuleE;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ChainController {

    @Autowired
    private BlockService blockService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private ContractService contractService;
    @Autowired
    private StatisticalService statisticalService;
    @Autowired
    private AgentService agentService;

    @Autowired
    SymbolQuotationPriceService symbolPriceService;

    @Autowired
    BlockTimeService blockTimeService;

    @Autowired
    SymbolRegService symbolRegService;

    @Autowired
    SymbolUsdtPriceProviderService symbolUsdtPriceProviderService;

    @Autowired
    ConfigurationLoader configurationLoader;

    @Autowired
    ApiConfig apiConfig;

    @RpcMethod("getChainInfo")
    public RpcResult getChainInfo(List<Object> params) {
        return RpcResult.success(CacheManager.getCache(ApiContext.defaultChainId).getChainInfo());
    }

    @RpcMethod("getOtherChainList")
    public RpcResult getOtherChainList(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is invalid");
        }

        List<Map<String, Object>> chainInfoList = new ArrayList<>();
        for (ChainInfo chainInfo : CacheManager.getChainInfoMap().values()) {
            if (chainInfo.getChainId() != chainId) {
                Map<String, Object> map = new HashMap<>();
                map.put("chainId", chainInfo.getChainId());
                map.put("chainName", chainInfo.getChainName());
                chainInfoList.add(map);
            }
        }
        return RpcResult.success(chainInfoList);

    }

    @RpcMethod("getInfo")
    public RpcResult getInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is invalid");
        }
        if (!CacheManager.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }

        Map<String, Object> map = new HashMap<>();
        map.put("chainId", chainId);
        map.put("networkHeight", ApiContext.networkHeight);
        map.put("localHeight", ApiContext.localHeight);
        map.put("bestBlockCreateTime",ApiContext.bestBlockCreateTime);
        ApiCache apiCache = CacheManager.getCache(chainId);
        AssetInfo assetInfo = apiCache.getChainInfo().getDefaultAsset();
        Map<String, Object> assetMap = new HashMap<>();
        assetMap.put("chainId", assetInfo.getChainId());
        assetMap.put("assetId", assetInfo.getAssetId());
        assetMap.put("symbol", assetInfo.getSymbol());
        assetMap.put("decimals", assetInfo.getDecimals());
        map.put("defaultAsset", assetMap);
        //agentAsset
        assetInfo = CacheManager.getRegisteredAsset(DBUtil.getAssetKey(apiCache.getConfigInfo().getAgentChainId(), apiCache.getConfigInfo().getAgentAssetId()));
        if (assetInfo != null) {
            assetMap = new HashMap<>();
            assetMap.put("chainId", assetInfo.getChainId());
            assetMap.put("assetId", assetInfo.getAssetId());
            assetMap.put("symbol", assetInfo.getSymbol());
            assetMap.put("decimals", assetInfo.getDecimals());
            map.put("agentAsset", assetMap);
        } else {
            map.put("agentAsset", null);
        }
        map.put("magicNumber", ApiContext.magicNumber);
        map.put("isRunCrossChain", ApiContext.isRunCrossChain);
        map.put("isRunSmartContract", ApiContext.isRunSmartContract);
        map.put("feePubkey", configurationLoader.getValue(ModuleE.Constant.CONVERTER,"feePubkey"));
        map.put("proposalPrice",configurationLoader.getValue(ModuleE.Constant.CONVERTER,"proposalPrice"));
        map.put("blackHolePublicKey",apiConfig.getBlackHolePublicKey());
        return RpcResult.success(map);
    }

    @RpcMethod("getCoinInfo")
    public RpcResult getCoinInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is invalid");
        }
        if (!CacheManager.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        ApiCache apiCache = CacheManager.getCache(chainId);
        return RpcResult.success(apiCache.getCoinContextInfo());
    }

    @RpcMethod("search")
    public RpcResult search(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);

        int chainId;
        String text;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is invalid");
        }
        try {
            text = params.get(1).toString().trim();
        } catch (Exception e) {
            return RpcResult.paramError("[text] is invalid");
        }

        if (!CacheManager.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        int length = text.length();
        SearchResultDTO result = null;
        if (length < 20) {
            result = getBlockByHeight(chainId, text);
        } else if (length < 40) {
            boolean isAddress = AddressTool.validAddress(chainId, text);
            if (isAddress) {
                byte[] address = AddressTool.getAddress(text);
                if (address[2] == AddressType.CONTRACT_ADDRESS_TYPE) {
                    result = getContractByAddress(chainId, text);
                } else {
                    result = getAccountByAddress(chainId, text);
                }
            }
        } else {
            result = getResultByHash(chainId, text);
        }
        if (null == result) {
            return RpcResult.dataNotFound();
        }
        return new RpcResult().setResult(result);
    }

    private SearchResultDTO getContractByAddress(int chainId, String text) {
        ContractInfo contractInfo;
        contractInfo = contractService.getContractInfo(chainId, text);
        SearchResultDTO dto = new SearchResultDTO();
        dto.setData(contractInfo);
        dto.setType("contract");
        return dto;
    }

    private SearchResultDTO getResultByHash(int chainId, String hash) {

        BlockHeaderInfo blockHeaderInfo = blockService.getBlockHeaderByHash(chainId, hash);
        if (blockHeaderInfo != null) {
            return getBlockInfo(chainId, blockHeaderInfo);
        }

        Result<TransactionInfo> result = WalletRpcHandler.getTx(chainId, hash);
        if (result == null) {
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.DATA_NOT_EXISTS));
        }
        if (result.isFailed()) {
            throw new JsonRpcException(result.getErrorCode());
        }
        TransactionInfo tx = result.getData();
        SearchResultDTO dto = new SearchResultDTO();
        dto.setData(tx);
        dto.setType("tx");
        return dto;
    }

    private SearchResultDTO getAccountByAddress(int chainId, String address) {
        if (!AddressTool.validAddress(chainId, address)) {
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[address] is inValid"));
        }

        AccountInfo accountInfo = accountService.getAccountInfo(chainId, address);
        if (accountInfo == null) {
            accountInfo = new AccountInfo(address);

        }
        SearchResultDTO dto = new SearchResultDTO();
        dto.setData(accountInfo);
        dto.setType("account");
        return dto;
    }

    private SearchResultDTO getBlockByHeight(int chainId, String text) {
        Long height;
        try {
            height = Long.parseLong(text);
        } catch (Exception e) {
            return null;
        }
        BlockHeaderInfo blockHeaderInfo = blockService.getBlockHeader(chainId, height);
        if (blockHeaderInfo == null) {
            return null;
        }
        return getBlockInfo(chainId, blockHeaderInfo);
    }

    private SearchResultDTO getBlockInfo(int chainId, BlockHeaderInfo blockHeaderInfo) {
        Result<BlockInfo> result = WalletRpcHandler.getBlockInfo(chainId, blockHeaderInfo.getHash());
        if (result.isFailed()) {
            throw new JsonRpcException(result.getErrorCode());
        }
        BlockInfo block = result.getData();
        if (null == block) {
            return null;
        } else {
            SearchResultDTO dto = new SearchResultDTO();
            dto.setData(block);
            dto.setType("block");
            return dto;
        }
    }

    @RpcMethod("getByzantineCount")
    public RpcResult getByzantineCount(List<Object> params) {
        int chainId;
        String txHash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            txHash = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[txHash] is inValid");
        }

        Result result = WalletRpcHandler.getByzantineCount(chainId, txHash);
        if (result.isFailed()) {
            throw new JsonRpcException(result.getErrorCode());
        }
        Map<String, Object> map = (Map<String, Object>) result.getData();
        return RpcResult.success(map);
    }

    @RpcMethod("assetGet")
    public RpcResult assetGet(List<Object> params) {
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        ApiCache apiCache = CacheManager.getCache(chainId);
        if (apiCache == null) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        CoinContextInfo coinContextInfo = apiCache.getCoinContextInfo();
        Map<String, Object> map = new HashMap<>();
//        nvtInflationAmount": 1,   //nvt当前通胀量  通过stacking快照表获取
        BigInteger nvtInflationAmount = coinContextInfo.getRewardTotal();
        map.put("nvtInflationAmount", nvtInflationAmount);
//        nvtInflationTotal": 1,    //通胀总量      通过共识模块配置获取
        map.put("nvtInflationTotal", ApiContext.totalInflationAmount);
//        nvtInitialAmount": 1,     //初始发行量    通过区块模块配置文件获
        BigInteger nvtInitialAmount = ApiContext.initialAmount;
        map.put("nvtInitialAmount", nvtInitialAmount);
//        nvtPublishAmount": 1,     //当前发行量    当前通胀量 + 初始发行量
        BigInteger nvtPublishAmount = nvtInflationAmount.add(nvtInitialAmount);
        map.put("nvtPublishAmount", nvtPublishAmount);
//        nvtDepositTotal": 1,      //当前抵押量
        BigInteger nvtDepositTotal = agentService.getNvtConsensusCoinTotal(chainId);
        map.put("nvtDepositTotal", nvtDepositTotal);
        //nvtStackTotal  //使用nvt参与stack的总量
        BigInteger nvtStackTotal = coinContextInfo.getNvtStackTotal();
        map.put("nvtStackTotal",nvtStackTotal);
        //allAssetStackTotal //所有参与stack的资产总数对应的NVT数量
        BigInteger allAssetStackTotal = coinContextInfo.getStackTotalForNvtValue();
        map.put("allAssetStackTotal",allAssetStackTotal);
        // nvtStackRate           //当前抵押率   示例 0.41 抵押率  41%
        BigDecimal nvtStackRate = new BigDecimal(nvtStackTotal).divide(new BigDecimal(nvtPublishAmount), MathContext.DECIMAL64).setScale(4,RoundingMode.HALF_DOWN);
        map.put("nvtStackRate", nvtStackRate);
//          nvtLockedAmount          //当期锁定量   查询几个固定的锁定资产
        BigInteger nvtLockedAmount = coinContextInfo.getBusiness().add(coinContextInfo.getCommunity()).add(coinContextInfo.getTeam()).add(coinContextInfo.getDestroy());
        map.put("nvtLockedAmount", nvtLockedAmount);
//        nvtTurnoverAmount": 1,     //当前流通量   当前发行量 - 当期锁定量
        map.put("nvtTurnoverAmount", nvtPublishAmount.subtract(nvtLockedAmount));
//        nvtTotal": 1,              //nvt总量     初始发行量 + 通胀总量
        map.put("nvtTotal", ApiContext.totalInflationAmount.add(nvtInitialAmount));
//        nvtUsdValue": 1,
        StackSymbolPriceInfo symbolPrice = symbolPriceService.getFreshUsdtPrice(ApiContext.defaultSymbol);
        BigDecimal nvtTotalUsdtValue = symbolPrice.getPrice().multiply(new BigDecimal(nvtPublishAmount).movePointLeft(ApiContext.defaultDecimals));
        //nvtUsdtValue nvt的美元价格
        map.put("nvtUsdtValue", symbolPrice.getPrice().setScale(ApiConstant.USDT_DECIMAL,RoundingMode.HALF_DOWN));
        map.put("nvtTotalUsdtValue", nvtTotalUsdtValue.movePointRight(ApiContext.defaultDecimals).toBigInteger());
        //nvtUsdtValueChangeRate nvt价格较上一天的涨跌幅，负数为跌，示例： 0.1 较上一日上涨10%
        map.put("nvtUsdtValueChangeRate",symbolPrice.getChange());
//        crossChainAssetValue": 1
        return RpcResult.success(map);
    }


    @RpcMethod("getNodeInfo")
    public RpcResult getNodeInfo(List<Object> params) {
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
//                "nodeCount": 1,                //普通节点数量          共识模块RPC接口实时获取
//                "bankNodeCount": 1,            //银行节点数量          虚拟银行模块RPC接口实时获取
//                "blockHeight": 1,              //区块高度              BlockTimeInfo
//                "avgBlockTimeConsuming": 1,    //平均出块耗时           BlockTimeInfo
//                "lastBlockTimeConsuming": 1    //最后一个块出块耗时       BlockTimeInfo

        ApiCache apiCache = CacheManager.getCache(chainId);
        if(apiCache == null) {
            return RpcResult.dataNotFound();
        }

        Map<String, Object> map = new HashMap<>();
        long count = 0;

        if (apiCache.getBestHeader() != null) {
            count = agentService.agentsCount(chainId, apiCache.getBestHeader().getHeight());
        }
        Result<Set<String>> bankNodeList = WalletRpcHandler.getVirtualBankAddressList(chainId);
        if(bankNodeList.isFailed()){
            return RpcResult.failed(bankNodeList.getErrorCode());
        }

        map.put("nodeCount", count);
        //银行节点数量需要减去种子节点数量
        map.put("bankNodeCount", bankNodeList.getData().size() - ApiContext.seedCount);
        BlockTimeInfo blockTimeInfo = blockTimeService.get(chainId);
        map.put("blockHeight", blockTimeInfo.getBlockHeight());
        map.put("avgBlockTimeConsuming", new BigDecimal(blockTimeInfo.getAvgConsumeTime()).setScale(2, RoundingMode.HALF_UP));
        map.put("lastBlockTimeConsuming", blockTimeInfo.getLastConsumeTime());
        map.put("bestBlockCreateTime",ApiContext.bestBlockCreateTime);
        return RpcResult.success(map);
    }

    /**
     * 获取所有币种的基础信息
     *
     * @return
     */
    @RpcMethod("getSymbolBaseInfo")
    public RpcResult getSymbolBaseInfo(List<Object> params) {
        List<SymbolRegInfo> symbolList = symbolRegService.getAll();
        SymbolPrice usd = symbolUsdtPriceProviderService.getSymbolPriceForUsdt("USD");
        SymbolRegInfo usdInfo = symbolRegService.get(0, 0);
        return RpcResult.success(
                symbolList.stream().map(d -> {
                    Map<String, Object> map = MapUtils.beanToMap(d);
                    SymbolPrice price = symbolUsdtPriceProviderService.getSymbolPriceForUsdt(d.getSymbol());
                    map.put("usdPrice", usd.transfer(price, BigDecimal.ONE).setScale(usdInfo.getDecimals(), RoundingMode.HALF_DOWN));
                    return map;
                }).collect(Collectors.toList()));
    }

    /**
     * 获取所有币种的基础信息
     *
     * @return
     */
    @RpcMethod("getSymbolInfo")
    public RpcResult getSymbolInfo(List<Object> params) {
        int chainId, assetId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            assetId = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[assetId] is inValid");
        }
        SymbolRegInfo symbolRegInfo = symbolRegService.get(chainId, assetId);
        if (symbolRegInfo == null) {
            return RpcResult.dataNotFound();
        }
        SymbolPrice usd = symbolUsdtPriceProviderService.getSymbolPriceForUsdt("USD");
        SymbolRegInfo usdInfo = symbolRegService.get(0, 0);
        Map<String, Object> map = MapUtils.beanToMap(symbolRegInfo);
        SymbolPrice price = symbolUsdtPriceProviderService.getSymbolPriceForUsdt(symbolRegInfo.getSymbol());
        map.put("usdPrice", usd.transfer(price, BigDecimal.ONE).setScale(usdInfo.getDecimals(), RoundingMode.HALF_DOWN));
        return RpcResult.success(map);
    }


//    @RpcMethod("assetGet")
//    public RpcResult assetGet(List<Object> params) {
//        int chainId;
//        try {
//            chainId = (int) params.get(0);
//        } catch (Exception e) {
//            return RpcResult.paramError("[chainId] is inValid");
//        }
//
//        ApiCache apiCache = CacheManager.getCache(chainId);
//        if (apiCache == null) {
//            return RpcResult.paramError("[chainId] is inValid");
//        }
//        CoinContextInfo coinContextInfo = apiCache.getCoinContextInfo();
//        Map<String, Object> map = new HashMap<>();
//        map.put("trades", coinContextInfo.getTxCount());
//        map.put("totalAssets", AssetTool.toDouble(coinContextInfo.getTotal()));
//        map.put("circulation", AssetTool.toDouble(coinContextInfo.getCirculation()));
//        map.put("deposit", AssetTool.toDouble(coinContextInfo.getConsensusTotal()));
//        map.put("business", AssetTool.toDouble(coinContextInfo.getBusiness()));
//        map.put("team", AssetTool.toDouble(coinContextInfo.getTeam()));
//        map.put("community", AssetTool.toDouble(coinContextInfo.getCommunity()));
//        int consensusCount = apiCache.getCurrentRound().getMemberCount() - apiCache.getChainInfo().getSeeds().size();
//        if (consensusCount < 0) {
//            consensusCount = 0;
//        }
//        map.put("consensusNodes", consensusCount);
//        long count = 0;
//        if (apiCache.getBestHeader() != null) {
//            count = agentService.agentsCount(chainId, apiCache.getBestHeader().getHeight());
//        }
//        map.put("totalNodes", count);
//        return RpcResult.success(map);
//    }

    @RpcMethod("getTotalSupply")
    public RpcResult getTotalSupply(List<Object> params) {
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }

        ApiCache apiCache = CacheManager.getCache(chainId);
        if (apiCache == null) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        CoinContextInfo coinContextInfo = apiCache.getCoinContextInfo();
        Map<String, Object> map = new HashMap<>();
        BigInteger supply = coinContextInfo.getTotal().subtract(coinContextInfo.getDestroy());
        map.put("supplyCoin", AssetTool.toCoinString(supply) + "");
        return RpcResult.success(map);
    }


    @RpcMethod("getCirculation")
    public RpcResult getCirculation(List<Object> params) {
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }

        ApiCache apiCache = CacheManager.getCache(chainId);
        if (apiCache == null) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        CoinContextInfo coinContextInfo = apiCache.getCoinContextInfo();
        Map<String, Object> map = new HashMap<>();
        map.put("circulation", AssetTool.toCoinString(coinContextInfo.getCirculation()) + "");
        return RpcResult.success(map);
    }

    @RpcMethod("getDestroy")
    public RpcResult getDestroy(List<Object> params) {
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }

        ApiCache apiCache = CacheManager.getCache(chainId);
        if (apiCache == null) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        CoinContextInfo coinContextInfo = apiCache.getCoinContextInfo();
        Map<String, Object> map = new HashMap<>();
        map.put("destroy", AssetTool.toCoinString(coinContextInfo.getDestroy()) + "");
        map.put("list", coinContextInfo.getDestroyInfoList());
        return RpcResult.success(map);
    }

    /**
     * 币种统计数据
     *
     * @return
     */
    @RpcMethod("symbolReport")
    public RpcResult symbolReport(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is invalid");
        }
        List<AssetSnapshotInfo> dataList = statisticalService.getAssetSnapshotAggSum(chainId, 4);
        Map<String, BigInteger> symbolTotalList = dataList.stream().map(d -> Map.of(d.getSymbol(), d.getTotal())).reduce(new HashMap<>(dataList.size()),
                (d1, d2) -> {
                    d1.putAll(d2);
                    return d1;
                });
        Map<String, BigInteger> symbolConvert24List = dataList.stream().map(d -> Map.of(d.getSymbol(), d.getConverterInTotal())).reduce(new HashMap<>(dataList.size()),
                (d1, d2) -> {
                    d1.putAll(d2);
                    return d1;
                });
        Map<String, BigInteger> symbolRedeem24List = dataList.stream().map(d -> Map.of(d.getSymbol(), d.getConverterOutTotal())).reduce(new HashMap<>(dataList.size()),
                (d1, d2) -> {
                    d1.putAll(d2);
                    return d1;
                });
        Map<String, BigInteger> symbolTransfer24List = dataList.stream().map(d -> Map.of(d.getSymbol(), d.getTxTotal())).reduce(new HashMap<>(dataList.size()),
                (d1, d2) -> {
                    d1.putAll(d2);
                    return d1;
                });

        return RpcResult.success(dataList.stream().map(d -> {
            SymbolRegInfo asset = symbolRegService.get(d.getAssetChainId(), d.getAssetId());
            Map<String, Object> res = new HashMap<>(9);
            res.put("symbol", d.getSymbol());
            SymbolUsdPercentDTO totalPer = symbolUsdtPriceProviderService.calcRate(d.getSymbol(),symbolTotalList);
            res.put("total", d.getTotal());
            res.put("totalRate",totalPer.getPer());
            res.put("totalUsdVal",totalPer.getUsdVal());
            SymbolUsdPercentDTO conver24Per = symbolUsdtPriceProviderService.calcRate(d.getSymbol(),symbolConvert24List);
            res.put("convert24", d.getConverterInTotal());
            res.put("convert24Rate",conver24Per.getPer());
            res.put("convert24UsdVal",conver24Per.getUsdVal());
            SymbolUsdPercentDTO redeem24Per = symbolUsdtPriceProviderService.calcRate(d.getSymbol(),symbolRedeem24List);
            res.put("redeem24", d.getConverterOutTotal());
            res.put("redeem24Rate",redeem24Per.getPer());
            res.put("redeem24UsdVal",redeem24Per.getUsdVal());
            SymbolUsdPercentDTO transfer24Per = symbolUsdtPriceProviderService.calcRate(d.getSymbol(),symbolTransfer24List);
            res.put("transfer24", d.getTxTotal());
            res.put("transfer24Rate",transfer24Per.getPer());
            res.put("transfer24UsdVal",transfer24Per.getUsdVal());
            res.put("addressCount", d.getAddressCount());
            res.put("icon", asset.getIcon());
            res.put("assetChainId", d.getAssetChainId());
            res.put("assetId", d.getAssetId());
            return res;
        }).collect(Collectors.toList()));
    }

}

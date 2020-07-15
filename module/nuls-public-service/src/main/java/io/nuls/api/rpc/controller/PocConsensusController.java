/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.api.rpc.controller;

import io.nuls.api.ApiContext;
import io.nuls.api.analysis.WalletRpcHandler;
import io.nuls.api.cache.ApiCache;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.constant.DepositFixedType;
import io.nuls.api.constant.DepositInfoType;
import io.nuls.api.constant.ReportDataTimeType;
import io.nuls.api.db.*;
import io.nuls.api.manager.CacheManager;
import io.nuls.api.manager.HeterogeneousChainAssetBalanceManager;
import io.nuls.api.model.dto.SymbolUsdPercentDTO;
import io.nuls.api.model.po.*;
import io.nuls.api.model.rpc.RpcErrorCode;
import io.nuls.api.model.rpc.RpcResult;
import io.nuls.api.rpc.RpcCall;
import io.nuls.api.service.StackingService;
import io.nuls.api.service.SymbolUsdtPriceProviderService;
import io.nuls.api.utils.AgentComparator;
import io.nuls.api.utils.VerifyUtils;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.consensus.ConsensusProvider;
import io.nuls.base.api.provider.consensus.facade.GetCanStackingAssetListReq;
import io.nuls.base.api.provider.consensus.facade.GetReduceNonceReq;
import io.nuls.base.api.provider.consensus.facade.ReduceNonceInfo;
import io.nuls.base.api.provider.converter.ConverterService;
import io.nuls.base.api.provider.converter.facade.GetVirtualBankInfoReq;
import io.nuls.base.api.provider.converter.facade.VirtualBankDirectorDTO;
import io.nuls.base.api.provider.ledger.facade.AssetInfo;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Controller;
import io.nuls.core.core.annotation.RpcMethod;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.Log;
import io.nuls.core.model.DateUtils;
import io.nuls.core.model.DoubleUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.MapUtils;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.util.NulsDateUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.nuls.api.constant.DBTableConstant.CONSENSUS_LOCKED;

/**
 * @author Eva
 */
@Controller
public class PocConsensusController {

    @Autowired
    private RoundManager roundManager;
    @Autowired
    private AgentService agentService;
    @Autowired
    private PunishService punishService;
    @Autowired
    private DepositService depositService;
    @Autowired
    private RoundService roundService;
    @Autowired
    private StatisticalService statisticalService;

    @Autowired
    private BlockService headerService;

    @Autowired
    private AliasService aliasService;

    @Autowired
    SymbolUsdtPriceProviderService symbolUsdtPriceProviderService;

    @Autowired
    SymbolRegService symbolRegService;

    @Autowired
    StackSnapshootService stackSnapshootService;

    @Autowired
    StackingService stackingService;

    @Autowired
    BlockService blockService;

    @Autowired
    SymbolQuotationPriceService symbolQuotationPriceService;

    @Autowired
    HeterogeneousChainAssetBalanceManager heterogeneousChainAssetBalanceManager;

    ConsensusProvider consensusProvider = ServiceManager.get(ConsensusProvider.class);

    ConverterService converterService = ServiceManager.get(ConverterService.class);

    @RpcMethod("getBestRoundItemList")
    public RpcResult getBestRoundItemList(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }

        if (!CacheManager.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        ApiCache apiCache = CacheManager.getCache(chainId);
        List<PocRoundItem> itemList = apiCache.getCurrentRound().getItemList();
        RpcResult rpcResult = new RpcResult();
        itemList.addAll(itemList);
        rpcResult.setResult(itemList);
        return rpcResult;
    }

    @RpcMethod("getConsensusNodeCount")
    public RpcResult getConsensusNodeCount(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }

        ApiCache apiCache = CacheManager.getCache(chainId);
        if (apiCache == null) {
            return RpcResult.dataNotFound();
        }
        Map<String, Long> resultMap = new HashMap<>();
        resultMap.put("seedsCount", (long) apiCache.getChainInfo().getSeeds().size());
        int consensusCount = apiCache.getCurrentRound().getMemberCount() - apiCache.getChainInfo().getSeeds().size();
        if (consensusCount < 0) {
            consensusCount = 0;
        }
        resultMap.put("consensusCount", (long) consensusCount);
        long count = 0;
        if (apiCache.getBestHeader() != null) {
            count = agentService.agentsCount(chainId, apiCache.getBestHeader().getHeight());
        }
        resultMap.put("agentCount", count);
        resultMap.put("totalCount", count + apiCache.getChainInfo().getSeeds().size());
        RpcResult result = new RpcResult();
        result.setResult(resultMap);
        return result;
    }

    @RpcMethod("getConsensusNodes")
    public RpcResult getConsensusNodes(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        int chainId, pageNumber, pageSize, type;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            pageNumber = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[pageNumber] is inValid");
        }
        try {
            pageSize = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[pageSize] is inValid");
        }
        try {
            type = (int) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[type] is inValid");
        }
        if (type < 0 || type > 3) {
            return RpcResult.paramError("[type] is invalid");
        }
        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 1000) {
            pageSize = 10;
        }

        PageInfo<AgentInfo> pageInfo;
        if (!CacheManager.isChainExist(chainId)) {
            pageInfo = new PageInfo<>(pageNumber, pageSize);
            return new RpcResult().setResult(pageInfo);
        }
        Result<Set<String>> bankListRes = WalletRpcHandler.getVirtualBankAddressList(chainId);
        if(bankListRes.isFailed()){
            return RpcResult.failed(bankListRes.getErrorCode());
        }
        Set<String> bankList = bankListRes.getData();
        Map<String,VirtualBankDirectorDTO> virtualBankDirectorDTOMap = new HashMap<>();
        io.nuls.base.api.provider.Result<VirtualBankDirectorDTO> virtualBankDirectorDTOResult = converterService.getVirtualBankInfo(new GetVirtualBankInfoReq(false));
        if(virtualBankDirectorDTOResult.isSuccess()){
            virtualBankDirectorDTOResult.getList().forEach(d->{
                virtualBankDirectorDTOMap.put(d.getSignAddress(),d);
            });
        }
        List<AgentInfo> agentInfoList = agentService.getAllAgentList(chainId);
        NavigableSet<AgentInfo> ranking = new TreeSet<>(AgentComparator.getInstance());
        for (AgentInfo agentInfo : agentInfoList) {
            long count = punishService.getYellowCount(chainId, agentInfo.getAgentAddress());
            if (agentInfo.getTotalPackingCount() != 0 || count != 0) {
                agentInfo.setLostRate(DoubleUtils.div(count, count + agentInfo.getTotalPackingCount()));
            }
            agentInfo.setYellowCardCount((int) count);
            Result<AgentInfo> clientResult = WalletRpcHandler.getAgentInfo(chainId, agentInfo.getTxHash());
            if (clientResult.isSuccess()) {
                agentInfo.setCreditValue(clientResult.getData().getCreditValue());
//                agentInfo.setDepositCount(clientResult.getData().getDepositCount());
                agentInfo.setStatus(clientResult.getData().getStatus());
                if (agentInfo.getAgentAlias() == null) {
                    AliasInfo info = aliasService.getAliasByAddress(chainId, agentInfo.getAgentAddress());
                    if (null != info) {
                        agentInfo.setAgentAlias(info.getAlias());
                    }
                }
            }
            agentInfo.setBankNode(bankList.contains(agentInfo.getAgentAddress()));
            ranking.add(agentInfo);
        }
        Collections.sort(agentInfoList, AgentComparator.getInstance());
        switch (type){
            case 1:  //筛选共识节点
                agentInfoList = agentInfoList.stream().filter(d->d.getStatus() == 1).collect(Collectors.toList());
                break;
            case 2:  //筛选虚拟银行节点
                agentInfoList = agentInfoList.stream().filter(d->d.isBankNode()).collect(Collectors.toList());
                break;
        }
        Stream<Map> stream = agentInfoList.stream().map(agent->{
            agent.setRanking(ranking.headSet(agent,true).size());
            Map data = MapUtils.beanToMap(agent);
            VirtualBankDirectorDTO virtualBankDirectorDTO = virtualBankDirectorDTOMap.get(agent.getPackingAddress());

            if(virtualBankDirectorDTO != null){
                data.put("networkList",virtualBankDirectorDTO.getHeterogeneousAddresses().parallelStream().map(d->{
                    BigDecimal balance = heterogeneousChainAssetBalanceManager.getBalance(d.getChainId(),d.getAddress());
                    d.setBalance(balance.toPlainString());
                    return d;
                }).collect(Collectors.toList()));
                data.put("order",virtualBankDirectorDTO.getOrder());
            }
            return data;
        });
        if(type == 2){
            //虚拟银行 需要重新排序
            stream = stream.sorted((o1,o2)->{
                Integer s1 = (Integer) o1.get("order");
                Integer s2 = (Integer) o2.get("order");
                return s1.compareTo(s2);
            });
        }
        List<Map> result = stream.collect(Collectors.toList());
        return new RpcResult().setResult(new PageInfo<>(1,Integer.MAX_VALUE,agentInfoList.size(),result));
    }


    @RpcMethod("getAllConsensusNodes")
    public RpcResult getAllConsensusNodes(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int chainId, pageNumber, pageSize;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            pageNumber = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[pageNumber] is inValid");
        }
        try {
            pageSize = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[pageSize] is inValid");
        }
        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 1000) {
            pageSize = 10;
        }
        PageInfo<AgentInfo> pageInfo;
        if (!CacheManager.isChainExist(chainId)) {
            pageInfo = new PageInfo<>(pageNumber, pageSize);
            return new RpcResult().setResult(pageInfo);
        }
        Result<Set<String>> bankListRes = WalletRpcHandler.getVirtualBankAddressList(chainId);
        if(bankListRes.isFailed()){
            return RpcResult.failed(bankListRes.getErrorCode());
        }
        Set<String> bankList = bankListRes.getData();
        pageInfo = agentService.getAgentList(chainId, pageNumber, pageSize);
        for (AgentInfo agentInfo : pageInfo.getList()) {
            long count = punishService.getYellowCount(chainId, agentInfo.getAgentAddress());
            if (agentInfo.getTotalPackingCount() != 0 || count != 0) {
                agentInfo.setLostRate(DoubleUtils.div(count, count + agentInfo.getTotalPackingCount()));
            }
            agentInfo.setYellowCardCount((int) count);
            Result<AgentInfo> clientResult = WalletRpcHandler.getAgentInfo(chainId, agentInfo.getTxHash());
            if (clientResult.isSuccess()) {
                agentInfo.setCreditValue(clientResult.getData().getCreditValue());
//                agentInfo.setDepositCount(clientResult.getData().getDepositCount());
                agentInfo.setStatus(clientResult.getData().getStatus());
                if (agentInfo.getAgentAlias() == null) {
                    AliasInfo info = aliasService.getAliasByAddress(chainId, agentInfo.getAgentAddress());
                    if (null != info) {
                        agentInfo.setAgentAlias(info.getAlias());
                    }
                }
            }
            agentInfo.setBankNode(bankList.contains(agentInfo.getAgentAddress()));
        }
        Collections.sort(pageInfo.getList(), AgentComparator.getInstance());
        return new RpcResult().setResult(pageInfo);
    }

    @RpcMethod("getConsensusNode")
    public RpcResult getConsensusNode(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String agentHash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            agentHash = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[agentHash] is inValid");
        }

        if (!CacheManager.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        AgentInfo agentInfo = agentService.getAgentByHash(chainId, agentHash);
        if (agentInfo == null) {
            return RpcResult.dataNotFound();
        }
        Result<Set<String>> bankListRes = WalletRpcHandler.getVirtualBankAddressList(chainId);
        if(bankListRes.isFailed()){
            return RpcResult.failed(bankListRes.getErrorCode());
        }
        Set<String> bankList = bankListRes.getData();
        long count = punishService.getYellowCount(chainId, agentInfo.getAgentAddress());
        if (agentInfo.getTotalPackingCount() != 0 || count != 0) {
            agentInfo.setLostRate(DoubleUtils.div(count, count + agentInfo.getTotalPackingCount()));
        }
        agentInfo.setYellowCardCount((int) count);
        ApiCache apiCache = CacheManager.getCache(chainId);
        List<PocRoundItem> itemList = apiCache.getCurrentRound().getItemList();
        PocRoundItem roundItem = null;
        if (null != itemList) {
            for (PocRoundItem item : itemList) {
                if (item.getPackingAddress().equals(agentInfo.getPackingAddress())) {
                    roundItem = item;
                    break;
                }
            }
        }



        Result<AgentInfo> result = WalletRpcHandler.getAgentInfo(chainId, agentHash);
        if (result.isSuccess()) {
            AgentInfo agent = result.getData();
            agentInfo.setCreditValue(agent.getCreditValue());
            agentInfo.setStatus(agent.getStatus());
            if (agentInfo.getAgentAlias() == null) {
                AliasInfo info = aliasService.getAliasByAddress(chainId, agentInfo.getAgentAddress());
                if (null != info) {
                    agentInfo.setAgentAlias(info.getAlias());
                }
            }
        }
        agentInfo.setBankNode(bankList.contains(agentInfo.getAgentAddress()));
        double consusensWeight = 1D;
        if(agentInfo.isBankNode()){
            consusensWeight = ApiContext.superAgentDepositBase;
        }else if(agentInfo.getStatus() == 1){
            consusensWeight = ApiContext.superAgentDepositBase;
        }
        BigDecimal interest = stackingService.getInterestRate(ApiContext.localAssertBase,consusensWeight);

        ApiCache cache = CacheManager.getCache(chainId);
        //计算节点排名
        int ranking = cache.getAgentMap().entrySet().stream().filter(d->d.getValue().getDeposit().compareTo(agentInfo.getDeposit()) >= 0).collect(Collectors.toList()).size();
        agentInfo.setRanking(ranking);
        Map<String,Object> res = MapUtils.beanToLinkedMap(agentInfo);
        res.put("interestRate",interest.setScale(4, RoundingMode.HALF_DOWN));
        io.nuls.base.api.provider.Result<VirtualBankDirectorDTO> virtualBankDirectorDTOResult = converterService.getVirtualBankInfo(new GetVirtualBankInfoReq(false));
        if(virtualBankDirectorDTOResult.isSuccess()){
            virtualBankDirectorDTOResult.getList().stream().filter(d->d.getSignAddress().equals(agentInfo.getPackingAddress())).findFirst().ifPresent(d->{
                res.put("networkList",d.getHeterogeneousAddresses().parallelStream().map(da->{
                    BigDecimal balance = heterogeneousChainAssetBalanceManager.getBalance(da.getChainId(),da.getAddress());
                    da.setBalance(balance.toPlainString());
                    return da;
                }).collect(Collectors.toList()));
            });
        }
        return RpcResult.success(res);
    }

    @RpcMethod("getAccountConsensusNode")
    public RpcResult getAccountConsensusNode(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String address;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            address = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is invalid");
        }
        if (!CacheManager.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        Result<Set<String>> bankListRes = WalletRpcHandler.getVirtualBankAddressList(chainId);
        if(bankListRes.isFailed()){
            return RpcResult.failed(bankListRes.getErrorCode());
        }
        Set<String> bankList = bankListRes.getData();
        Long maxHeight = blockService.getMaxHeight(chainId,Long.MAX_VALUE);
        List<AgentInfo> agentInfoList = agentService.getAgentListByStartHeight(chainId,maxHeight);
//        agentInfoList.sort(Comparator.comparing(AgentInfo::getDeposit));
        for (var i = 0;i<agentInfoList.size();i++){
            AgentInfo agentInfo = agentInfoList.get(i);
            if(agentInfo.getAgentAddress().equals(address)){
                agentInfo.setRanking(i + 1);
                long count = punishService.getYellowCount(chainId, agentInfo.getAgentAddress());
                if (agentInfo.getTotalPackingCount() != 0 || count != 0) {
                    agentInfo.setLostRate(DoubleUtils.div(count, count + agentInfo.getTotalPackingCount()));
                }
                agentInfo.setYellowCardCount((int) count);
                Result<AgentInfo> clientResult = WalletRpcHandler.getAgentInfo(chainId, agentInfo.getTxHash());
                if (clientResult.isSuccess()) {
                    agentInfo.setCreditValue(clientResult.getData().getCreditValue());
                    agentInfo.setStatus(clientResult.getData().getStatus());
                    if (agentInfo.getAgentAlias() == null) {
                        AliasInfo info = aliasService.getAliasByAddress(chainId, agentInfo.getAgentAddress());
                        if (null != info) {
                            agentInfo.setAgentAlias(info.getAlias());
                        }
                    }
                }
                agentInfo.setBankNode(bankList.contains(agentInfo.getAgentAddress()));
                return RpcResult.success(agentInfo);
            }
        }
//        AgentInfo agentInfo = agentService.getAgentByAgentAddress(chainId, address);
//        if (agentInfo != null) {
//            long count = punishService.getYellowCount(chainId, agentInfo.getAgentAddress());
//            if (agentInfo.getTotalPackingCount() != 0 || count != 0) {
//                agentInfo.setLostRate(DoubleUtils.div(count, count + agentInfo.getTotalPackingCount()));
//            }
//            agentInfo.setYellowCardCount((int) count);
//            Result<AgentInfo> clientResult = WalletRpcHandler.getAgentInfo(chainId, agentInfo.getTxHash());
//            if (clientResult.isSuccess()) {
//                agentInfo.setCreditValue(clientResult.getData().getCreditValue());
////                agentInfo.setDepositCount(clientResult.getData().getDepositCount());
//                agentInfo.setStatus(clientResult.getData().getStatus());
//                if (agentInfo.getAgentAlias() == null) {
//                    AliasInfo info = aliasService.getAliasByAddress(chainId, agentInfo.getAgentAddress());
//                    if (null != info) {
//                        agentInfo.setAgentAlias(info.getAlias());
//                    }
//                }
//            }
//        }
//        return RpcResult.success(agentInfo);
        return RpcResult.success(null);
    }

    @RpcMethod("getConsensusStatistical")
    public RpcResult getConsensusStatistical(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId, type;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            type = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[type] is inValid");
        }
        if (type < 0 || type > 5) {
            return RpcResult.paramError("[type] is invalid");
        }

        if (!CacheManager.isChainExist(chainId)) {
            return RpcResult.success(new ArrayList<>());
        }
        List list = this.statisticalService.getStatisticalList(chainId, type, CONSENSUS_LOCKED,0);
        return new RpcResult().setResult(list);
    }

    @RpcMethod("getConsensusNodeStatistical")
    public RpcResult getConsensusNodeStatistical(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId, type;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            type = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[type] is inValid");
        }
        if (type < 0 || type > 4) {
            return RpcResult.paramError("[type] is invalid");
        }

        if (!CacheManager.isChainExist(chainId)) {
            return RpcResult.success(new ArrayList<>());
        }
        List list = this.statisticalService.getStatisticalList(chainId, type, "nodeCount",0);
        return new RpcResult().setResult(list);
    }

    @RpcMethod("getAnnulizedRewardStatistical")
    public RpcResult getAnnulizedRewardStatistical(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId, type;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            type = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[type] is inValid");
        }
        if (type < 0 || type > 4) {
            return RpcResult.paramError("[type] is invalid");
        }

        if (!CacheManager.isChainExist(chainId)) {
            return RpcResult.success(new ArrayList<>());
        }
        List list = this.statisticalService.getStatisticalList(chainId, type, "annualizedReward",0);
        return new RpcResult().setResult(list);
    }

    @RpcMethod("getPunishList")
    public RpcResult getPunishList(List<Object> params) {
        VerifyUtils.verifyParams(params, 5);
        int chainId, pageNumber, pageSize, type;
        String agentAddress;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            pageNumber = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[pageNumber] is inValid");
        }
        try {
            pageSize = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[pageSize] is inValid");
        }
        try {
            type = (int) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[type] is inValid");
        }
        try {
            agentAddress = (String) params.get(4);
        } catch (Exception e) {
            return RpcResult.paramError("[agentAddress] is inValid");
        }
        if (type < 0 || type > 2) {
            return RpcResult.paramError("[type] is invalid");
        }
        if (!StringUtils.isBlank(agentAddress) && !AddressTool.validAddress(chainId, agentAddress)) {
            return RpcResult.paramError("[agentAddress] is inValid");
        }
        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 1000) {
            pageSize = 10;
        }

        PageInfo<PunishLogInfo> list;
        if (!CacheManager.isChainExist(chainId)) {
            list = new PageInfo<>(pageNumber, pageSize);
        } else {
            list = punishService.getPunishLogList(chainId, type, agentAddress, pageNumber, pageSize);
        }
        return new RpcResult().setResult(list);
    }

    @RpcMethod("getConsensusDeposit")
    public RpcResult getConsensusDeposit(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        int chainId, pageNumber, pageSize;
        String agentHash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            pageNumber = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[pageNumber] is inValid");
        }
        try {
            pageSize = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[pageSize] is inValid");
        }
        try {
            agentHash = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[agentHash] is inValid");
        }
        if (StringUtils.isBlank(agentHash)) {
            return RpcResult.paramError("[agentHash] is inValid");
        }
        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 1000) {
            pageSize = 10;
        }

        PageInfo<DepositInfo> list;
        if (!CacheManager.isChainExist(chainId)) {
            list = new PageInfo<>(pageNumber, pageSize);
        } else {
            list = this.depositService.pageDepositListByAgentHash(chainId, agentHash, pageNumber, pageSize);
        }
        return new RpcResult().setResult(list);
    }

    @RpcMethod("getAllConsensusDeposit")
    public RpcResult getAllConsensusDeposit(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        int chainId, pageNumber, pageSize, type;
        String agentHash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            pageNumber = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[pageNumber] is inValid");
        }
        try {
            pageSize = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[pageSize] is inValid");
        }
        try {
            agentHash = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[agentHash] is inValid");
        }
        if (StringUtils.isBlank(agentHash)) {
            return RpcResult.paramError("[agentHash] is inValid");
        }
        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 1000) {
            pageSize = 10;
        }

        PageInfo<DepositInfo> list;
        if (!CacheManager.isChainExist(chainId)) {
            list = new PageInfo<>(pageNumber, pageSize);
        } else {
            list = this.depositService.pageDepositListByAgentHash(chainId, agentHash, pageNumber, pageSize,
                    DepositInfoType.CREATE_AGENT,
                    DepositInfoType.STOP_AGENT,
                    DepositInfoType.APPEND_AGENT_DEPOSIT,
                    DepositInfoType.REDUCE_AGENT_DEPOSIT);
        }
        return new RpcResult().setResult(list);
    }

    @RpcMethod("getAccountConsensus")
    public RpcResult getAccountConsensus(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        int chainId, pageNumber, pageSize;
        String address;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            pageNumber = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[pageNumber] is inValid");
        }
        try {
            pageSize = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[pageSize] is inValid");
        }
        try {
            address = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is invalid");
        }
        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 1000) {
            pageSize = 10;
        }

        PageInfo<AgentInfo> pageInfo;
        if (!CacheManager.isChainExist(chainId)) {
            pageInfo = new PageInfo(pageNumber, pageSize);
            return RpcResult.success(pageInfo);
        }
        List<String> hashList = depositService.getAgentHashList(chainId, address);
        AgentInfo agentInfo = agentService.getAliveAgentByAgentAddress(chainId, address);
        if (agentInfo != null && !hashList.contains(agentInfo.getTxHash())) {
            hashList.add(agentInfo.getTxHash());
        }

        pageInfo = agentService.getAgentByHashList(chainId, pageNumber, pageSize, hashList);
        for (AgentInfo info : pageInfo.getList()) {
            Result<AgentInfo> clientResult = WalletRpcHandler.getAgentInfo(chainId, info.getTxHash());
            if (clientResult.isSuccess()) {
                info.setCreditValue(clientResult.getData().getCreditValue());
//                info.setDepositCount(clientResult.getData().getDepositCount());
                info.setStatus(clientResult.getData().getStatus());
                if (info.getAgentAlias() == null) {
                    AliasInfo aliasInfo = aliasService.getAliasByAddress(chainId, info.getAgentAddress());
                    if (null != aliasInfo) {
                        info.setAgentAlias(aliasInfo.getAlias());
                    }
                }
            }
        }
        return RpcResult.success(pageInfo);
    }

    @RpcMethod("getAccountDeposit")
    public RpcResult getAccountDeposit(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        int chainId, pageNumber, pageSize;
        String address, agentHash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            pageNumber = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[pageNumber] is inValid");
        }
        try {
            pageSize = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[pageSize] is inValid");
        }
        try {
            address = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is invalid");
        }
        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 1000) {
            pageSize = 10;
        }

        PageInfo<DepositInfo> list;
        if (!CacheManager.isChainExist(chainId)) {
            list = new PageInfo<>(pageNumber, pageSize);
        } else {
            list = this.depositService.getDepositListByAddress(chainId, address, pageNumber, pageSize);
        }
        return new RpcResult().setResult(list);
    }

    @RpcMethod("getAccountDepositValue")
    public RpcResult getAccountDepositValue(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String address, agentHash;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            address = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        try {
            agentHash = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[agentHash] is inValid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is invalid");
        }
        if (!CacheManager.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        BigInteger value = depositService.getDepositAmount(chainId, address, agentHash);
        return new RpcResult().setResult(value);
    }

    @RpcMethod("getBestRoundInfo")
    public RpcResult getBestRoundInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        ApiCache apiCache = CacheManager.getCache(chainId);
        if (apiCache == null) {
            return RpcResult.dataNotFound();
        }
        return new RpcResult().setResult(apiCache.getCurrentRound());
    }

    @RpcMethod("getRoundList")
    public RpcResult getRoundList(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int chainId, pageNumber, pageSize;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            pageNumber = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[pageNumber] is inValid");
        }
        try {
            pageSize = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[pageSize] is inValid");
        }

        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        if (pageSize <= 0 || pageSize > 1000) {
            pageSize = 10;
        }

        if (!CacheManager.isChainExist(chainId)) {
            return RpcResult.success(new PageInfo<>(pageNumber, pageSize));
        }
        long count = roundService.getTotalCount(chainId);
        List<PocRound> roundList = roundService.getRoundList(chainId, pageNumber, pageSize);
        PageInfo<PocRound> pageInfo = new PageInfo<>();
        pageInfo.setPageNumber(pageNumber);
        pageInfo.setPageSize(pageSize);
        pageInfo.setTotalCount(count);
        pageInfo.setList(roundList);
        return new RpcResult().setResult(pageInfo);
    }

    @RpcMethod("getRoundInfo")
    public RpcResult getRoundInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        long roundIndex;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            roundIndex = Long.parseLong(params.get(1) + "");
        } catch (Exception e) {
            return RpcResult.paramError("[roundIndex] is inValid");
        }

        if (!CacheManager.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        if (roundIndex == 1) {
            return getFirstRound(chainId);
        }
        CurrentRound round = new CurrentRound();
        PocRound pocRound = roundService.getRound(chainId, roundIndex);
        if (pocRound == null) {
            return RpcResult.dataNotFound();
        }
        List<PocRoundItem> itemList = roundService.getRoundItemList(chainId, roundIndex);
        Optional<PocRoundItem> lastItem = itemList.stream().sorted(Comparator.comparing(d->-d.getOrder())).filter(d->d.getBlockHeight() != 0).findFirst();
        int lastIndex;
        if(lastItem.isEmpty()){
            lastIndex = itemList.size() + 1;
        }else{
            lastIndex = lastItem.get().getOrder();
        }
        itemList.forEach(d->{
            if(d.getOrder() < lastIndex && d.getBlockHeight() == 0){
                d.setYellow(true);
            }else{
                if(d.getTime() < NulsDateUtils.getCurrentTimeSeconds() && d.getBlockHeight() == 0){
                    d.setYellow(true);
                }else{
                    d.setYellow(false);
                }
            }
        });
        round.setItemList(itemList);
        round.initByPocRound(pocRound);
        return new RpcResult().setResult(round);
    }

    private RpcResult getFirstRound(int chainId) {
        BlockHeaderInfo headerInfo = headerService.getBlockHeader(chainId, 0);
        if (null == headerInfo) {
            return new RpcResult();
        }
        CurrentRound round = new CurrentRound();
        round.setStartTime(headerInfo.getRoundStartTime());
        round.setStartHeight(0);
        round.setProducedBlockCount(1);
        round.setMemberCount(1);
        round.setIndex(1);
        round.setEndTime(headerInfo.getCreateTime());
        round.setEndHeight(0);
        List<PocRoundItem> itemList = new ArrayList<>();
        PocRoundItem item = new PocRoundItem();
        itemList.add(item);
        item.setTime(headerInfo.getCreateTime());
        item.setTxCount(1);
        item.setBlockHash(headerInfo.getHash());
        item.setBlockHeight(0);
        item.setPackingAddress(headerInfo.getPackingAddress());
        item.setRoundIndex(1);
        item.setOrder(1);
        round.setItemList(itemList);
        return new RpcResult().setResult(round);
    }

    @RpcMethod("getStackingInfo")
    public RpcResult getStackingInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        List<DepositInfo> list = depositService.getDepositSumList(chainId,DepositInfoType.STACKING,DepositInfoType.CANCEL_STACKING);
        Map<String, BigInteger> symbolList = list.stream().map(d -> Map.of(d.getSymbol(), d.getAmount())).reduce(new HashMap<>(list.size()),
                (d1, d2) -> {
                    d1.putAll(d2);
                    return d1;
                });
        List<Map<String, Object>> resList = list.stream().map(d -> {
            SymbolUsdPercentDTO dto = symbolUsdtPriceProviderService.calcRate(d.getSymbol(),symbolList);
            Map<String, Object> m = Map.of(
                    "rate", dto.getPer(),
                    "usdValue", dto.getUsdVal(),
                    "assetChainId", d.getAssetChainId(),
                    "assetId", d.getAssetId(),
                    "amount", d.getAmount(),
                    "symbol", d.getSymbol(),
                    "decimal", d.getDecimal()
            );
            return m;
        }).collect(Collectors.toList());
        return RpcResult.success(resList);
    }

    /**
     * 获取stacking历史统计数据
     *
     * @return
     */
    @RpcMethod("getStackingDataHistory")
    public RpcResult getStackingDataHistory(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        String timeType = (String) params.get(1);
        ReportDataTimeType reportDataTimeType;
        try {
            reportDataTimeType = ReportDataTimeType.valueOf(timeType);
        } catch (Exception e) {
            return RpcResult.paramError("[timeType] is inValid : " + Arrays.toString(ReportDataTimeType.values()));
        }
        Date[] date = reportDataTimeType.getTimeRangeForDay(new Date(),TimeZone.getTimeZone("UTC+0"));
        List<StackSnapshootInfo> list = stackSnapshootService.queryList(chainId, date[0].getTime(), date[1].getTime());
        if(reportDataTimeType.equals(ReportDataTimeType.Year)){
            Map<String,Map> monthData = new LinkedHashMap<>(12);
            list.stream().forEach(d->{
                String month = DateUtils.convertDate(new Date(d.getDay()),"YYYY-MM");
                Map<String, Object> res = Map.of("time", d.getDay(), "total", d.getRewardTotal(), "interest", d.getBaseInterest());
                monthData.put(month,res);
            });
            return RpcResult.success(monthData.values());
        }else{
            return RpcResult.success(list.stream().map(d -> {
                        Map<String, Object> res = Map.of("time", d.getDay(), "total", d.getRewardTotal(), "interest", d.getBaseInterest());
                        return res;
                    }).collect(Collectors.toList()
                    ));
        }

    }

    @RpcMethod("getStackingRate")
    public RpcResult getStackingRate(List<Object> params){
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        Map res = null;
        try {
            res = (Map) RpcCall.request(ModuleE.CS.abbr, "cs_getRateAddition", Map.of());
        } catch (Exception e) {
            Log.error("调用rpc :cs_getRateAddition 异常",e);
            return RpcResult.failed(RpcErrorCode.SYS_UNKNOWN_EXCEPTION);
        }
        List list = (List) res.get("list");
        return RpcResult.success(list.stream().map(d->{
            Map<String,Object> item = (Map<String, Object>) d;
            String symbol = (String) item.get("symbol");
            List<Map> detail = (List<Map>) item.get("detailList");
            detail = detail.stream().map(de->{
                int depositInfoType = (int) de.get("depositType");
                BigDecimal rate;
                if(depositInfoType == 0){
                    //活期
                    rate = stackingService.getAssetStackingRate(symbol,DepositFixedType.NONE);
                }else{
                    byte timeType = ((Integer) de.get("timeType")).byteValue();
                    DepositFixedType depositFixedType = DepositFixedType.getValue(timeType);
                    rate = stackingService.getAssetStackingRate(symbol,depositFixedType);
                }
                return Map.of("timeType",de.get("timeType"),"totalAddition",rate,"depositType",depositInfoType);
            }).collect(Collectors.toList());
            return Map.of("symbol",symbol,"detailList",detail);
        }).collect(Collectors.toList()));
    }

    public static boolean validHash(String hex) {
        try {
            HexUtil.decode(hex);
            return true;
        } catch (Exception var2) {
            return false;
        }
    }

    @RpcMethod("getReduceNonceList")
    public RpcResult getReduceNonceList(List<Object> params) {
        int chainId,quitAll;
        String agentHash,reduceAmount;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            agentHash = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[agentHash] is inValid");
        }
        if (!validHash(agentHash)) {
            return RpcResult.paramError("[agentHash] is inValid");
        }
        try {
            reduceAmount = params.get(2).toString();
        } catch (Exception e) {
            return RpcResult.paramError("[reduceAmount] is inValid");
        }
        try {
            if(params.size() < 4){
                quitAll = 0;
            }else{
                quitAll = (int)params.get(3);
            }
        } catch (Exception e) {
            return RpcResult.paramError("[reduceAmount] is inValid");
        }
        GetReduceNonceReq req = new GetReduceNonceReq(agentHash,quitAll,reduceAmount);
        req.setChainId(chainId);
        ConsensusProvider consensusProvider = ServiceManager.get(ConsensusProvider.class);
        io.nuls.base.api.provider.Result<ReduceNonceInfo> result = consensusProvider.getReduceNonceList(req);
        if (result.isSuccess()) {
            return RpcResult.success(result.getList());
        }else {
            return RpcResult.failed(CommonCodeConstanst.FAILED,result.getMessage());
        }
    }


    @RpcMethod("pageStackRecordByAddress")
    public RpcResult pageStackRecordByAddress(List<Object> params){
        VerifyUtils.verifyParams(params, 4);
        String address;
        int chainId, pageNumber, pageSize;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            pageNumber = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[pageNumber] is inValid");
        }
        try {
            pageSize = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[pageSize] is inValid");
        }

        try {
            address = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is invalid");
        }
        PageInfo<DepositInfo> pageInfo = depositService
                .getStackRecordByAddress(chainId,pageNumber,pageSize,address);
        return RpcResult.success(pageInfo);
    }


    @RpcMethod("pageFinishStackingListByAddress")
    public RpcResult pageFinishStackingListByAddress(List<Object> params){
        return pageStackingListByAddress(params,false);
    }

    @RpcMethod("pageStackingListByAddress")
    public RpcResult getStackingListByAddress(List<Object> params){
        return pageStackingListByAddress(params,true);
    }

    public RpcResult pageStackingListByAddress(List<Object> params,boolean isActive){
        VerifyUtils.verifyParams(params, 4);
        String address;
        int chainId, pageNumber, pageSize;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            pageNumber = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[pageNumber] is inValid");
        }
        try {
            pageSize = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[pageSize] is inValid");
        }

        try {
            address = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is invalid");
        }
        PageInfo<DepositInfo> pageInfo = depositService
                .getStackingListByAddress(chainId,pageNumber,pageSize,address,isActive);
        return RpcResult.success(new PageInfo<>(pageInfo.getPageNumber(), pageSize, pageInfo.getTotalCount(),
                pageInfo.getList().stream().map(d->{
                    Map res = MapUtils.beanToLinkedMap(d);
                    DepositFixedType depositFixedType = DepositFixedType.valueOf(d.getFixedType());
                    res.put("interest",stackingService.getAssetStackingRate(d.getSymbol(),depositFixedType));
                    if(!depositFixedType.equals(DepositFixedType.NONE)){
                        res.put("endTime",d.getCreateTime() + depositFixedType.getTime());
                    }
                    return res;
                }).collect(Collectors.toList())));
    }


    @RpcMethod("getCanStackingAssetList")
    public RpcResult getCanStackingAssetList(List<Object> params){
        VerifyUtils.verifyParams(params, 1);
        int chainId;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        io.nuls.base.api.provider.Result<AssetInfo> res = consensusProvider.getCanStackingAssetList(new GetCanStackingAssetListReq(chainId));

        if(res.isSuccess()){
            return RpcResult.success(res.getList().stream().map(d->{
                Map<String,Object> map = MapUtils.beanToLinkedMap(d);
                if(d.getAssetChainId() == ApiContext.defaultChainId && d.getAssetId() == ApiContext.defaultAssetId){
                    map.put("nvtPrice",BigDecimal.ONE);
                }else{
                    SymbolPrice symbolPrice = symbolQuotationPriceService.getFreshUsdtPrice(d.getSymbol());
                    map.put("nvtPrice",getNvtPrice().transfer(symbolPrice,BigDecimal.ONE));
                }
                map.put("rate",getStackingRate(d.getSymbol()));
                return map;
            }).collect(Collectors.toList()));
        }else{
            return RpcResult.failed(CommonCodeConstanst.FAILED,res.getMessage());
        }
    }

    private SymbolPrice getNvtPrice(){
        StackSymbolPriceInfo nvtPrice = symbolQuotationPriceService.getFreshUsdtPrice(ApiContext.defaultChainId,ApiContext.defaultAssetId);
        if(nvtPrice.getPrice().compareTo(BigDecimal.ZERO) == 0){
            SymbolPrice nulsPrice = symbolQuotationPriceService.getFreshUsdtPrice(ApiContext.mainSymbol);
            nvtPrice = new StackSymbolPriceInfo();
            nvtPrice.setPrice(nulsPrice.getPrice().divide(BigDecimal.TEN));
            nvtPrice.setCurrency(SymbolQuotationPriceService.USDT);
        }
        return nvtPrice;
    }


    private Map<DepositFixedType,BigDecimal> getStackingRate(String symbol) {
        Map<DepositFixedType,BigDecimal> res = new HashMap<>(DepositFixedType.values().length);
        Arrays.stream(DepositFixedType.values()).forEach(d->{
            BigDecimal rate = stackingService.getAssetStackingRate(symbol,d);
            if(d.equals(DepositFixedType.NONE)){
                res.put(d,rate);
            }else{
                Long day = d.getTime() / 3600L / 24;
                BigDecimal totalRate = rate.divide(BigDecimal.valueOf(365), MathContext.DECIMAL64).multiply(new BigDecimal(day));
                res.put(d,totalRate.setScale(ApiConstant.RATE_DECIMAL,RoundingMode.HALF_UP));
            }

        });
        return res;
    }


}

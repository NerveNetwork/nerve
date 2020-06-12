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

package io.nuls.api.task;

import io.nuls.api.ApiContext;
import io.nuls.api.analysis.WalletRpcHandler;
import io.nuls.api.constant.ApiConstant;
import io.nuls.api.constant.ConverterTxType;
import io.nuls.api.constant.DepositInfoType;
import io.nuls.api.db.*;
import io.nuls.api.db.mongo.*;
import io.nuls.api.model.po.*;
import io.nuls.api.service.SymbolUsdtPriceProviderService;
import io.nuls.api.utils.LoggerUtil;
import io.nuls.core.basic.Result;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.model.DateUtils;
import org.bson.Document;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * @author Niels
 */
public class StatisticalTask implements Runnable {

    private int chainId;

    private StatisticalService statisticalService;

    private BlockService blockService;

    private DepositService depositService;

    private AgentService agentService;

    private SymbolQuotationPriceService symbolPriceService;

    private BlockTimeService blockTimeService;

    private SymbolRegService symbolRegService;

    private SymbolUsdtPriceProviderService symbolUsdtPriceProviderService;

    private ConverterTxService converterTxService;

    private AccountLedgerService accountLedgerService;

    public StatisticalTask(int chainId) {
        this.chainId = chainId;
        statisticalService = SpringLiteContext.getBean(MongoStatisticalServiceImpl.class);
        blockService = SpringLiteContext.getBean(MongoBlockServiceImpl.class);
        depositService = SpringLiteContext.getBean(MongoDepositServiceImpl.class);
        agentService = SpringLiteContext.getBean(MongoAgentServiceImpl.class);
        blockTimeService = SpringLiteContext.getBean(MongoBlockTimeServiceImpl.class);
        converterTxService = SpringLiteContext.getBean(MongoConverterTxServiceImpl.class);
        accountLedgerService = SpringLiteContext.getBean(MongoAccountLedgerServiceImpl.class);
        symbolRegService = SpringLiteContext.getBean(SymbolRegService.class);
        symbolUsdtPriceProviderService = SpringLiteContext.getBean(SymbolUsdtPriceProviderService.class);
        symbolPriceService = SpringLiteContext.getBean(SymbolQuotationPriceService.class);
    }

    public StatisticalTask() {
    }

    @Override
    public void run() {
        try {
            this.doCalc();
        } catch (Exception e) {
            LoggerUtil.commonLog.error(e);
        }
    }

    private void doCalc() {
        long bestId = statisticalService.getBestId(chainId);
        BlockHeaderInfo header = blockService.getBestBlockHeader(chainId);
        if (null == header || header.getHeight() == 0) {
            return;
        }
        long hour = 3600 * 1000;
        long start = bestId + 1;
        long end = 0;
        if (bestId == -1) {
            BlockHeaderInfo header0 = blockService.getBlockHeader(chainId, 1);
            start = header0.getCreateTime() * 1000 - 10 * DateUtils.SECOND_TIME;
            end = start + hour;
            this.statisticalService.saveBestId(chainId, start);
        } else {
            end = start + hour - 1;
        }
        while (true) {
            if (end > header.getCreateTime() * 1000) {
                break;
            }
            statistical(start, end);
            start = end + 1;
            end = end + hour;
            BlockHeaderInfo newBlockHeader = blockService.getBestBlockHeader(chainId);
            if (null != newBlockHeader) {
                header = newBlockHeader;
            }
        }
    }

    private void statistical(long start, long end) {
        //计算区间内交易总量
        long txCount = statisticalService.calcTxCount(chainId, start / 1000, end / 1000);


        long height = blockService.getMaxHeight(chainId, end / 1000);
        List<AgentInfo> agentList = agentService.getAgentListByStartHeight(chainId, height);
        //计算区间内节点抵押总量
        BigInteger consensusLocked = agentList.stream().map(d->d.getDeposit()).reduce(BigInteger.ZERO,(d,d1)->d.add(d1));
        List<DepositInfo> depositList = depositService.getDepositList(chainId, height, DepositInfoType.STACKING);
        //获取NVT与USDT的汇率
        SymbolPrice nvtUsdtPrice = symbolPriceService.getFreshUsdtPrice(ApiContext.defaultChainId,ApiContext.defaultAssetId);
        BigInteger stackingTotal = depositList.stream().map(d->{
            //抵押的是NVT直接返回数量
            if(d.getAssetChainId() == ApiContext.defaultChainId && d.getAssetId() == ApiContext.defaultAssetId){
                return d.getAmount();
            }
            //需要将异构资产换算成NVT
            //获取当前抵押的资产与USDT的汇率
            SymbolPrice symbolPrice = symbolPriceService.getFreshUsdtPrice(d.getAssetChainId(),d.getAssetId());
            //将当前抵押资产转换成NVT
            BigDecimal amount = nvtUsdtPrice.transfer(symbolPrice,new BigDecimal(d.getAmount()).movePointLeft(d.getDecimal()));
            return amount.movePointRight(ApiContext.defaultDecimals).toBigInteger();
        }).reduce(BigInteger.ZERO,(d1,d2)->d1.add(d2));
        int nodeCount = agentList.size();
        for (AgentInfo agent : agentList) {
            consensusLocked = consensusLocked.add(agent.getDeposit());
        }
        for (DepositInfo deposit : depositList) {
            consensusLocked = consensusLocked.add(deposit.getAmount());
        }
//        double annualizedReward = 0L;
//        if (consensusLocked.compareTo(BigInteger.ZERO) != 0) {
//            Result<Map> result = WalletRpcHandler.getConsensusConfig(chainId);
//            Map map = result.getData();
//            String inflationAmount = map.get("inflationAmount").toString();
//            double d = DoubleUtils.mul(365, new BigInteger(inflationAmount).doubleValue());
//            d = DoubleUtils.div(d, 30, 0);
//            annualizedReward = DoubleUtils.mul(100, DoubleUtils.div(d, consensusLocked.doubleValue(), 4), 2);
//        }


        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(end);
        StatisticalInfo info = new StatisticalInfo();
        info.setTime(end);
        info.setTxCount(txCount);
        info.setNodeCount(nodeCount);
        info.setConsensusLocked(consensusLocked);
        info.setStackingTotal(stackingTotal);
        info.setDate(calendar.get(Calendar.DATE));
        info.setMonth(calendar.get(Calendar.MONTH) + 1);
        info.setYear(calendar.get(Calendar.YEAR));

        BlockTimeInfo nowBlockHeight = blockTimeService.get(ApiContext.defaultChainId);
        if(nowBlockHeight != null){
            Long lastBlockHeight = nowBlockHeight.getBlockHeight();
            StatisticalInfo lastStatisticalInfo = statisticalService.getLastStatisticalInfo(ApiContext.defaultChainId);
            Long startBlockHeight = 1L;
            if(lastStatisticalInfo != null){
                startBlockHeight = lastStatisticalInfo.getLastBlockHeight() + 1L;
            }
            info.setAssetSnapshotList(buildAsssetSnapshoot(startBlockHeight,lastBlockHeight));
            info.setLastBlockHeight(lastBlockHeight);
        }

        try {
            this.statisticalService.insert(chainId, info);
        } catch (Exception e) {
            LoggerUtil.commonLog.error(e);
        }
        this.statisticalService.updateBestId(chainId, info.getTime());
    }


    public List<AssetSnapshotInfo> buildAsssetSnapshoot(long startBlockHeight, long lastBlockHeight){
        //各种资产交易数量汇总
        Map<String,BigInteger> txMapping = new HashMap<>();
        //重新获取每个区块对交易进行解析
        for (var i = startBlockHeight;i <= lastBlockHeight;i++){
            Result<BlockInfo> blockInfo = WalletRpcHandler.getBlockInfo(ApiContext.defaultChainId,i);
            if(blockInfo == null || blockInfo.getData() == null){
                break;
            }
            List<TransactionInfo> txList = blockInfo.getData().getTxList();
            txList.forEach(tx->{
                //如果to的locked不等于0，需要将form和to里面地址相同的数量去掉，不纳入统计范围。 当这种情况下认为是自我发起的锁定
                //from为null时，不纳入统计
                if(tx.getCoinFroms() == null || tx.getCoinTos().isEmpty()){
                    return ;
                }
                if(tx.getCoinTos() == null || tx.getCoinTos().isEmpty()){
                    return ;
                }

                Map<String,BigInteger> from = tx.getCoinFroms().stream()
                        //将每个from 按照 地址--资产chainId-资产id : 交易数量 的方式组装成List<Map>   注意地址后面是两个分隔符，用于与下次快速分割出地址和assetKey
                        .map(d->Map.of(d.getAddress() + ApiConstant.SPACE.repeat(2) + d.getAssetKey(),d.getAmount()))
                        //对相同key进行合并，最终生成一个Map<String,BigInteger>
                        .reduce(new HashMap<>(tx.getCoinFroms().size()),this::merge);

                tx.getCoinTos().stream()
                        //将每个to 按照 地址--资产chainId-资产id : 交易数量 的方式组装成List<Map>   注意地址后面是两个分隔符，用于与下次快速分割出地址和assetKey
                        .map(d->{
                    //将每一条to按照上面的规则进行汇总
                    String key = d.getAddress() + ApiConstant.SPACE.repeat(2) +d.getAssetKey();
                    BigInteger amount = d.getAmount();
                    //如果from里面有相同的key，认为是自己转给自己，或者是对资产进行锁定操作，不纳入统计范围
                    BigInteger formAmount = from.get(key);
                    if(formAmount != null){
                        if(amount.compareTo(formAmount) < -1){
                            from.put(key,formAmount.subtract(amount));
                            amount = BigInteger.ZERO;
                        }else{
                            from.remove(key);
                            amount = amount.subtract(formAmount);
                        }
                    }
                    return Map.of(key,amount);
                }).reduce(txMapping,(d1,d2)->{
                    //将key里面的address去掉，再进行按照AssetKey进行合并，将合并结果放入txMapping中，最终txMapping中是按照assetChainId-assetId 为分组计算出来的交易总数
                    d2.entrySet().forEach(entry->{
                        String key = entry.getKey().split(ApiConstant.SPACE.repeat(2))[1];
                        d1.merge(key,entry.getValue(),(v1,v2)->v1.add(v2));
                    });
                    return d1;
                });
            });
        }
        //存储异构跨链转出交易的交易量汇总
        Map<String,BigInteger> withdrawalMap = new HashMap<>();
        //存储异构跨链转入交易的交易量汇总
        Map<String,BigInteger> rechargeMap = new HashMap<>();
        //查询指定高度内的所有异构跨链交易
        List<ConverterTxInfo> converterTxInfoList = converterTxService.queryList(chainId,startBlockHeight,lastBlockHeight);
        converterTxInfoList.forEach(d->{
            String key = d.getAssetChainId() + ApiConstant.SPACE + d.getAssetId();
            if(ConverterTxType.IN.name().equals(d.getConverterType())){
                //转入交易
                rechargeMap.merge(key,d.getAmount(),(v1,v2)->v1.add(v2));
            }else {
                //转出交易
                withdrawalMap.merge(key,d.getAmount(),(v1,v2)->v1.add(v2));
            }
        });

        Map<String,AssetSnapshotInfo> snapshotMap = new HashMap<>();
        //设置各个币种当前快照计算区间的交易总数
        txMapping.entrySet().stream().forEach(entry-> createSnapshot(snapshotMap,entry.getKey()).setTxTotal(entry.getValue()));
        //设置各个币种当前快照计算区间的异构转出数量
        withdrawalMap.entrySet().forEach(entry-> createSnapshot(snapshotMap,entry.getKey()).setConverterOutTotal(entry.getValue()));
        //设置各个币种当前快照计算区间的异构转入数量
        rechargeMap.entrySet().forEach(entry->createSnapshot(snapshotMap,entry.getKey()).setConverterInTotal(entry.getValue()));
        //汇总查询各个币种
        accountLedgerService.aggAssetAddressCount(chainId).entrySet().forEach(entry-> createSnapshot(snapshotMap,entry.getKey()).setAddressCount(entry.getValue()));
        //汇总查询各个币种的持币数
        computeAssetTotal(chainId).entrySet().forEach(entry->createSnapshot(snapshotMap,entry.getKey()).setTotal(entry.getValue()));
        return new ArrayList<>(snapshotMap.values());
    }

    /**
     * 计算各个资产的总余额
     * @param chainId
     * @return
     */
    private Map<String,BigInteger> computeAssetTotal(int chainId) {
        List<Document> balanceList = accountLedgerService.getAllBalance(chainId);
        Map<String,BigInteger> res = new HashMap<>();
        balanceList.stream().map(d->{
            String assetChainId = d.get("chainId").toString();
            String assetId = d.get("assetId").toString();
            BigInteger balance = new BigInteger(d.get("balance").toString());
            return Map.of(assetChainId + ApiConstant.SPACE + assetId,balance);
        }).reduce(res,(d1,d2)->{
            d2.entrySet().forEach(entry->{
                d1.merge(entry.getKey(),entry.getValue(),(v1,v2)->v1.add(v2));
            });
            return d1;
        });
        return res;
    }


    /**
     * 如果传入的map里没有对象资产快照对象就创建一个
     * @param snapshotMap
     * @param keyStr
     * @return
     */
    private AssetSnapshotInfo createSnapshot(Map<String,AssetSnapshotInfo> snapshotMap,String keyStr){
        return snapshotMap.computeIfAbsent(keyStr,k->{
            String[] key = keyStr.split(ApiConstant.SPACE);
            AssetSnapshotInfo snapshoot = new AssetSnapshotInfo();
            snapshoot.setAssetChainId(Integer.parseInt(key[0]));
            snapshoot.setAssetId(Integer.parseInt(key[1]));
            //获取资产注册信息
            SymbolRegInfo regInfo = symbolRegService.get(snapshoot.getAssetChainId(),snapshoot.getAssetId());
            snapshoot.setSymbol(regInfo.getSymbol());
            //获取资产usdt汇率
            SymbolPrice symbolPrice = symbolUsdtPriceProviderService.getSymbolPriceForUsdt(snapshoot.getSymbol());
            snapshoot.setUsdtPrice(symbolPrice.getPrice().movePointRight(ApiConstant.USDT_DECIMAL).toBigInteger());
            return snapshoot;
        });
    }

    private Map<String,BigInteger> merge(Map<String,BigInteger> d1,Map<String,BigInteger> d2){
        d2.entrySet().forEach(entry-> d1.merge(entry.getKey(),entry.getValue(),(v1, v2)->v1.add(v2)));
        return d1;
    }

    public void setStatisticalService(StatisticalService statisticalService) {
        this.statisticalService = statisticalService;
    }

    public void setBlockService(BlockService blockService) {
        this.blockService = blockService;
    }

    public void setDepositService(DepositService depositService) {
        this.depositService = depositService;
    }

    public void setAgentService(AgentService agentService) {
        this.agentService = agentService;
    }

    public void setSymbolPriceService(SymbolQuotationPriceService symbolPriceService) {
        this.symbolPriceService = symbolPriceService;
    }

    public void setBlockTimeService(BlockTimeService blockTimeService) {
        this.blockTimeService = blockTimeService;
    }

    public void setSymbolRegService(SymbolRegService symbolRegService) {
        this.symbolRegService = symbolRegService;
    }

    public void setSymbolUsdtPriceProviderService(SymbolUsdtPriceProviderService symbolUsdtPriceProviderService) {
        this.symbolUsdtPriceProviderService = symbolUsdtPriceProviderService;
    }
}

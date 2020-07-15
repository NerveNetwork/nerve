package io.nuls.api.task;

import io.nuls.api.ApiContext;
import io.nuls.api.manager.HeterogeneousChainAssetBalanceManager;
import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.converter.ConverterService;
import io.nuls.base.api.provider.converter.facade.GetVirtualBankInfoReq;
import io.nuls.base.api.provider.converter.facade.VirtualBankDirectorDTO;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: zhoulijun
 * @Time: 2020-06-23 14:55
 * @Description: 功能描述
 */
public class QueryHeterogeneousChainBalanceTask implements Runnable {

    ConverterService converterService = ServiceManager.get(ConverterService.class);

    @Override
    public void run() {
        HeterogeneousChainAssetBalanceManager manager = SpringLiteContext.getBean(HeterogeneousChainAssetBalanceManager.class);
        Map<Integer, Map<String,BigDecimal>> balanceList = manager.getBalanceList();
        Log.info("查询虚拟银行节点异构资产余额");
        Result<VirtualBankDirectorDTO> virtualBankDirectorDTOResult = converterService.getVirtualBankInfo(new GetVirtualBankInfoReq());
        if(virtualBankDirectorDTOResult.isFailed()){
            Log.error("查询虚拟银行资产信息失败:{}:{}",virtualBankDirectorDTOResult.getStatus(),virtualBankDirectorDTOResult.getMessage());
        }
        virtualBankDirectorDTOResult.getList().forEach(bank->{
            bank.getHeterogeneousAddresses().forEach(address->{
                balanceList.putIfAbsent(address.getChainId(),new HashMap<>());
                balanceList.get(address.getChainId()).put(address.getAddress(),new BigDecimal(address.getBalance()).setScale(ApiContext.defaultDecimals));
                Log.info("{}地址:{},余额:{}",address.getSymbol(),address.getAddress(),address.getBalance());
            });
        });
    }


}

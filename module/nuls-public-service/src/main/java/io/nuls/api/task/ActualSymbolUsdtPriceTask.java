package io.nuls.api.task;

import io.nuls.api.service.impl.SymbolUsdtPriceProviderServiceImpl;
import io.nuls.core.core.ioc.SpringLiteContext;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-08 21:11
 * @Description: 定时获取币种兑USDT价格
 */
public class ActualSymbolUsdtPriceTask implements Runnable {

    @Override
    public void run() {
        SymbolUsdtPriceProviderServiceImpl service = SpringLiteContext.getBean(SymbolUsdtPriceProviderServiceImpl.class);
        if(!service.isInited()){
            service.init();
        }
        service.doQuery();
    }

}

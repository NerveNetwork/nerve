package io.nuls.api.test;

import io.nuls.api.PublicServiceBootstrap;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.rpc.modulebootstrap.RpcModuleState;
import org.junit.Before;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-26 13:53
 * @Description: 功能描述
 */
public class BaseTestCase {

    @Before
    public void before(){
        System.setProperty("active.config","/Users/zhoulijun/workspace/nuls/nerve-network-package/NULS_WALLET/nuls.ncf");
        PublicServiceBootstrap.main(new String[]{});
        PublicServiceBootstrap publicServiceBootstrap = SpringLiteContext.getBean(PublicServiceBootstrap.class);
        while (publicServiceBootstrap.getState() != RpcModuleState.Running){
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}

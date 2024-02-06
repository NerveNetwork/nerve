package io.nuls.provider;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rpc.modulebootstrap.RpcModuleState;
import io.nuls.provider.rpctools.TransactionTools;

/**
 * @Author: zhoulijun
 * @Time: 2019-06-10 20:54
 * @Description: Module Business Implementation Class
 */
@Component
public class MyModule {

    @Autowired
    TransactionTools transactionTools;

    /**
     * Start module
     * After the module is started, this function will be called when all declared dependent modules are ready
     * @param moduleName
     * @return
     */
    public RpcModuleState startModule(String moduleName){
        //Registration transaction
        //transactionTools.registerTx(moduleName,200);
        return RpcModuleState.Running;
    }

}

package io.nuls.protocol;

import io.nuls.common.INerveCoreBootstrap;
import io.nuls.common.NerveCoreConfig;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.modulebootstrap.Module;
import io.nuls.protocol.manager.ChainManager;

/**
 * 协议升级模块启动类
 *
 * @author captain
 * @version 1.0
 * @date 19-3-4 下午4:09
 */
@Component
public class ProtocolUpdateBootstrap implements INerveCoreBootstrap {

    @Autowired
    public static NerveCoreConfig protocolConfig;

    @Autowired
    private ChainManager chainManager;


    @Override
    public int order() {
        return 1;
    }

    /**
     * 返回当前模块的描述信息
     * @return
     */
    @Override
    public Module moduleInfo() {
        return new Module(ModuleE.PU.abbr, "1.0");
    }

    @Override
    public void mainFunction(String[] args) {
        this.init();

    }


    private void init() {
        try {
            chainManager.initChain();
        } catch (Exception e) {
            Log.error("ProtocolUpdateBootstrap init error!");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDependenciesReady() {
        //启动链
        chainManager.runChain();
        Log.info("protocol onDependenciesReady");
    }


}

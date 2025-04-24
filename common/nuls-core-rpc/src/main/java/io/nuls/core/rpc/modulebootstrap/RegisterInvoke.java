package io.nuls.core.rpc.modulebootstrap;

import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.channel.manager.ConnectManager;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.core.rpc.invoke.BaseInvoke;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
import io.nuls.core.parse.MapUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @Author: zhoulijun
 * @Time: 2019-02-28 14:52
 * @Description: Function Description
 */
public class RegisterInvoke extends BaseInvoke {


    Set<Module> dependenices;

    Module module;

    public RegisterInvoke(Module module, Set<Module> dependenices) {
        this.dependenices = dependenices;
        this.module = module;
    }

    @Override
    public void callBack(Response response) {
        Map responseData = (Map) response.getResponseData();
        if (response.isSuccess()) {
            RpcModule rpcModule = SpringLiteContext.getBean(RpcModule.class);
            if (rpcModule.getDependencies().isEmpty()) {
                Log.info("RMB:module rpc is ready");
                return;
            }
            Map methodMap = (Map) responseData.get("RegisterAPI");
            Map dependMap = (Map) methodMap.get("Dependencies");
            StringBuilder logInfo = new StringBuilder("\nModule information has changed, please resynchronize：\n");
            for (Object object : dependMap.entrySet()) {
                Map.Entry<String, Map> entry = (Map.Entry<String, Map>) object;
                logInfo.append("injection：[key=").append(entry.getKey()).append(",value=").append(entry.getValue()).append("]\n");
                ConnectManager.ROLE_MAP.put(entry.getKey(), entry.getValue());
            }
            Log.debug(logInfo.toString());
            ConnectManager.updateStatus();
            if (!ConnectManager.isReady()) {
                return;
            }
            Log.info("RMB:module rpc is ready");
            dependMap.entrySet().forEach(obj -> {
                Map.Entry<String, Map> entry = (Map.Entry<String, Map>) obj;
                if (dependenices.stream().anyMatch(d -> d.getName().equals(entry.getKey()))) {
                    if(rpcModule.isDependencieReady(entry.getKey())) {
                        return ;
                    }
                    NotifySender notifySender = SpringLiteContext.getBean(NotifySender.class);
                    notifySender.send("registerModuleDependent_" + entry.getKey(),10, new RegisterModuleCallable(entry, module));
                }
            });

        }

    }

    static class RegisterModuleCallable implements Callable<Boolean> {

        Map.Entry<String, Map> entry;
        Module module;

        public RegisterModuleCallable(Map.Entry<String, Map> entry, Module module) {
            this.entry = entry;
            this.module = module;
        }

        @Override
        public Boolean call() throws Exception {
            Response cmdResp = null;
            try {
                Map map = new HashMap();
                map.put("name", module.getName());
                map.put("version", module.getVersion());
                cmdResp = ResponseMessageProcessor.requestAndResponse(entry.getKey(), "registerModuleDependencies", map);
                Log.debug("registerModuleDependent : {},result:{}", entry.getKey(),cmdResp);
                return cmdResp.isSuccess();
            } catch (Exception e) {
                Log.error("Calling remote interface failed. module:{} - interface:{} - message:{}", module, "registerModuleDependencies", e.getMessage());
                return false;
            }
        }
    }

}

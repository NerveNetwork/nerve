/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package network.nerve.hetool;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.protocol.ModuleHelper;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.modulebootstrap.Module;
import io.nuls.core.rpc.modulebootstrap.NulsRpcModuleBootstrap;
import io.nuls.core.rpc.modulebootstrap.RpcModule;
import io.nuls.core.rpc.modulebootstrap.RpcModuleState;
import io.nuls.core.rpc.util.AddressPrefixDatas;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.hetool.constant.Constant;
import network.nerve.hetool.context.HeToolContext;
import network.nerve.hetool.manager.ChainManager;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author: PierreLuo
 * @date: 2021-03-11
 */
@Component
public class HeToolBootstrap extends RpcModule {

    @Autowired
    private AddressPrefixDatas addressPrefixDatas;
    @Autowired
    private ChainManager chainManager;

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        initSys();
        NulsRpcModuleBootstrap.run("network.nerve.hetool,io.nuls", args);
    }

    @Override
    public void init() {
        try {
            super.init();
            //Add address tool class initialization
            AddressTool.init(addressPrefixDatas);
            initContext();
            chainManager.initChain();
            ModuleHelper.init(this);
        } catch (Exception e) {
            Log.error("module init error!", e);
            throw new RuntimeException(e);
        }
    }


    @Override
    public boolean doStart() {
        try {
            Log.info("module chain do start");
            return true;
        } catch (Exception e) {
            Log.error("module start error!");
            Log.error(e);
            return false;
        }
    }


    @Override
    public void onDependenciesReady(Module module) {
        Log.info("dependencies [{}] ready", module.getName());
    }

    @Override
    public RpcModuleState onDependenciesReady() {
        Log.info("all dependency module ready");
        NulsDateUtils.getInstance().start();
        return RpcModuleState.Running;
    }

    @Override
    public RpcModuleState onDependenciesLoss(Module module) {
        return RpcModuleState.Ready;
    }

    @Override
    public Module[] declareDependent() {
        return new Module[]{
                Module.build(ModuleE.CV)
        };
    }

    @Override
    public Module moduleInfo() {
        return new Module(ModuleE.HT.abbr, Constant.RPC_VERSION);
    }

    @Override
    public Set<String> getRpcCmdPackage() {
        return Set.of(Constant.CMD_PATH);
    }

    /**
     * Initialize system encoding
     */
    private static void initSys() throws NoSuchFieldException, IllegalAccessException {
        try {
            System.setProperty(Constant.SYS_ALLOW_NULL_ARRAY_ELEMENT, "true");
            System.setProperty(Constant.SYS_FILE_ENCODING, UTF_8.name());
            Field charset = Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, UTF_8);
            ObjectMapper objectMapper = JSONUtils.getInstance();
            objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        } catch (Exception e) {
            Log.error(e);
            throw e;
        }
    }


    /**
     * Initialize configuration item context
     */
    private void initContext() throws IOException{
        Map<String, String> options = new HashMap<>();
        options.put("js.webassembly", "true");
        ClassLoader classLoader = HeToolBootstrap.class.getClassLoader();
        String scriptContent;
        try (InputStream inputStream = classLoader.getResourceAsStream("js/tbc.contract.js")) {
            if (inputStream == null) {
                throw new IOException("not found js/tbc.contract.js");
            }
            scriptContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        // 创建 Source
        Source mysource = Source.newBuilder("js", scriptContent, "tbc")
                .mimeType("application/javascript")
                .build();

        Context context = Context.newBuilder()
                .allowAllAccess(true)
                .allowIO(true)
                .options(options)
                .build();
        context.eval(mysource);
        HeToolContext.context = context;
    }

}

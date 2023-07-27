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
package io.nuls.common;

import io.nuls.base.api.provider.Provider;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.CmdAnnotation;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.NerveCoreCmd;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2023/6/27
 */
public class CommonTest {

    @Test
    public void nerveCoreCmdScanTest() {
        Provider.ProviderType providerType = Provider.ProviderType.RPC;
        ServiceManager.init(9, providerType);
        SpringLiteContext.init("io.nuls", "io.nuls.core.rpc.modulebootstrap", "io.nuls.core.rpc.cmd", "io.nuls.base.protocol");
        List<BaseCmd> cmdList = SpringLiteContext.getBeanList(BaseCmd.class);
        for (BaseCmd cmd : cmdList) {
            Class<?> clazs = cmd.getClass();
            NerveCoreCmd nerveCoreCmd = clazs.getAnnotation(NerveCoreCmd.class);
            if (nerveCoreCmd == null) {
                continue;
            }
            ModuleE module = nerveCoreCmd.module();
            String moduleName = module.getName();
            Method[] methods = clazs.getMethods();
            for (Method method : methods) {
                CmdAnnotation annotation = method.getAnnotation(CmdAnnotation.class);
                if (annotation == null) {
                    continue;
                }
                String cmdName = annotation.cmd();
                System.out.println(String.format(
                        "moduleName: %s, cmd: %s, class instance: %s, method: %s",
                        moduleName, cmdName, cmd.getClass().getName(), method.getName()
                ));
            }
        }
    }
}

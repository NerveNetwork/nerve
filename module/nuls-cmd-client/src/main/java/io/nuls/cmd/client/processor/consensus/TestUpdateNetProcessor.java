/*
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.cmd.client.processor.consensus;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.consensus.TestNetProvider;
import io.nuls.base.api.provider.consensus.facade.UpdateNet;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.core.core.annotation.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: ljs
 * @Time: 2019-08-06 17:34
 * @Description: 功能描述
 */
@Component
public class TestUpdateNetProcessor extends ConsensusBaseProcessor {
    TestNetProvider provider = ServiceManager.get(TestNetProvider.class);

    @Override
    public String getCommand() {
        return "testupdatenet";
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription())
                .newLine("\t<chainId>  chainId - require")
                .newLine("\t<consensusPubKey>  consensusPubKey - require")
                .newLine("\t<updateType>  updateType - require");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "testupdatenet <chainId> <consensusPubKey> <updateType>--test update net";
    }

    @Override
    public boolean argsValidate(String[] args) {
        checkArgsNumber(args, 3);
        return true;
    }

    @Override
    public CommandResult execute(String[] args) {
        Integer chainId = Integer.parseInt(args[1]);
        String consensusPubKey = args[2];
        Short updateType = Short.parseShort(args[3]);
        UpdateNet req = new UpdateNet();
        req.setChainId( chainId);
        req.setUpdateType(updateType);
        req.setConsensusPubKey(consensusPubKey);
        Result<Boolean> result = provider.updateNet(req);
        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result);
    }
}

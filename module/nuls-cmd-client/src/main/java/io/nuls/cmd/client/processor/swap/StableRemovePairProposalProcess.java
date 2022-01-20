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

package io.nuls.cmd.client.processor.swap;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.swap.SwapService;
import io.nuls.base.api.provider.swap.facade.StableManagePairReq;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.config.Config;
import io.nuls.cmd.client.processor.CommandGroup;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

/**
 * @author: Loki
 * @date: 2020/9/15
 */
@Component
public class StableRemovePairProposalProcess implements CommandProcessor {

    @Autowired
    Config config;

    SwapService swapService = ServiceManager.get(SwapService.class);


    @Override
    public CommandGroup getGroup() {
        return CommandGroup.Swap;
    }

    @Override
    public String getCommand() {
        return "stableremovepair";
    }


    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription())
                .newLine("\t<sender> cmd sender - Required")
                .newLine("\t<stablePairAddress> \t\tthe pair address of stable swap - Required");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "stableremovepair <sender> <stablePairAddress>";
    }

    @Override
    public boolean argsValidate(String[] args) {
        checkArgsNumber(args,2);
        checkAddress(config.getChainId(),args[1]);
        checkAddress(config.getChainId(),args[2]);
        return true;
    }

    private StableManagePairReq build(String[] args) {
        String sender = args[1];
        String stablePairAddress = args[2];
        StableManagePairReq req = new StableManagePairReq(
                10,
                stablePairAddress,
                "REMOVE",
                sender);
        req.setPassword(getPwd("\nEnter your account password:"));
        return req;
    }

    @Override
    public CommandResult execute(String[] args) {
        Result<String> result = swapService.stableManagePair(build(args));
        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result.getData());
    }
}

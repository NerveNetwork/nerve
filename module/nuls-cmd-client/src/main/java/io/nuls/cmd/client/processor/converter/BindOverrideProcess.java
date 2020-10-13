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

package io.nuls.cmd.client.processor.converter;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.converter.ConverterService;
import io.nuls.base.api.provider.converter.facade.BindOverrideReq;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.config.Config;
import io.nuls.cmd.client.processor.CommandGroup;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

/**
 * @author: Loki
 * @date: 2020/9/17
 */
@Component
public class BindOverrideProcess implements CommandProcessor {

    @Autowired
    Config config;

    ConverterService converterService = ServiceManager.get(ConverterService.class);


    @Override
    public CommandGroup getGroup() {
        return CommandGroup.Converter;
    }

    @Override
    public String getCommand() {
        return "bindoverride";
    }


    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription())
                .newLine("\t<sender> cmd sender - require")
                .newLine("\t<heterogeneousChainId> heterogeneous chainId - require")
                .newLine("\t<assetAddress> asset address - require")
                .newLine("\t<nerveAssetChainId> nerve asset chain id - require")
                .newLine("\t<nerveAssetId> nerve asset id - require");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "bindoverride <sender> <heterogeneousChainId> <assetAddress> <nerveAssetChainId> <nerveAssetId> -- cover binding heterogeneous chain asset";
        // bindoverride TNVTdTSPEn3kK94RqiMffiKkXTQ2anRwhN1J9 101 0x7938dA083594d83f2E8a2926fcAEB88828C06BE1 5 2
    }


    @Override
    public boolean argsValidate(String[] args) {
        checkArgsNumber(args, 5);
        checkAddress(config.getChainId(), args[1]);
        checkIsNumeric(args[2], "heterogeneousChainId");
        checkIsNumeric(args[4], "nerveAssetChainId");
        checkIsNumeric(args[5], "nerveAssetId");
        return true;
    }

    @Override
    public CommandResult execute(String[] args) {
        String sender = args[1];
        int heterogeneousChainId = Integer.parseInt(args[2]);
        String assetAddress = args[3];
        int nerveAssetChainId =  Integer.parseInt(args[4]);
        int nerveAssetId =  Integer.parseInt(args[5]);
        BindOverrideReq req = new BindOverrideReq(sender,
                heterogeneousChainId, assetAddress, nerveAssetChainId, nerveAssetId);

        req.setPassword(getPwd());
        Result<String> result = converterService.bindOverride(req);
        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result);
    }
}

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
import io.nuls.base.api.provider.converter.facade.CancelHtgTxReq;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.processor.CommandGroup;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.core.core.annotation.Component;

/**
 * @author: Mimi
 * @date: 2021/5/31
 */
@Component
public class CancelHtgTxProcess implements CommandProcessor {

    ConverterService converterService = ServiceManager.get(ConverterService.class);

    @Override
    public String getCommand() {
        return "cancelHtgTx";
    }

    @Override
    public CommandGroup getGroup() {
        return CommandGroup.Converter;
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription())
                .newLine("\t<heterogeneousChainId> \t\theterogeneous chain id - Required")
                .newLine("\t<heterogeneousAddress> \t\theterogeneous address - Required")
                .newLine("\t<nonce> \t\taddress nonce - Required")
                .newLine("\t<priceGwei> \t\tpriceGwei - Required");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "cancelHtgTx <heterogeneousChainId> <heterogeneousAddress> <nonce> <priceGwei>--cancel heterogeneous transaction";
    }

    @Override
    public boolean argsValidate(String[] args) {
        checkArgsNumber(args,4);
        checkIsNumeric(args[1],"heterogeneousChainId");
        checkIsNumeric(args[3],"nonce");
        checkIsNumeric(args[4],"priceGwei");
        return true;
    }

    private CancelHtgTxReq buildCancelHtgTxReq(String[] args) {
        int heterogeneousChainId = Integer.parseInt(args[1]);
        String address = args[2];
        String nonce = args[3];
        String priceGwei = args[4];
        CancelHtgTxReq cancelHtgTxReq = new CancelHtgTxReq();
        cancelHtgTxReq.setHeterogeneousChainId(heterogeneousChainId);
        cancelHtgTxReq.setHeterogeneousAddress(address);
        cancelHtgTxReq.setNonce(nonce);
        cancelHtgTxReq.setPriceGwei(priceGwei);
        return cancelHtgTxReq;
    }

    @Override
    public CommandResult execute(String[] args) {
        Result<Boolean> result = converterService.cancelHtgTx(buildCancelHtgTxReq(args));
        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result.getData().toString());
    }
}

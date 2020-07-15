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
import io.nuls.base.api.provider.converter.facade.VoteReq;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.config.Config;
import io.nuls.cmd.client.processor.CommandGroup;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

/**
 * @author: Loki
 * @date: 2020/6/9
 */
@Component
public class VoteProposalProcess implements CommandProcessor {

    @Autowired
    Config config;

    ConverterService converterService = ServiceManager.get(ConverterService.class);

    @Override
    public CommandGroup getGroup(){
        return CommandGroup.Converter;
    }
    @Override
    public String getCommand() {
        return "vote";
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription())
                .newLine("\t<proposalTxHash> \t\tproposal transaction hash - Required")
                .newLine("\t<choice> \t\t0:against, 1:favor - Required")
                .newLine("\t<address> \t\tvote transaction address - Required")
                .newLine("\t[remark] \t\tremark - ");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "vote <proposalTxHash> <choice> <address>  [remark] --vote";
    }

    @Override
    public boolean argsValidate(String[] args) {
        checkArgsNumber(args,3,4);
        checkIsNumeric(args[2],"choice");
        checkAddress(config.getChainId(),args[3]);
        return true;
    }

    private VoteReq buildVoteReq(String[] args) {
        String hash = args[1];
        byte choice = Byte.parseByte(args[2]);

        String address = args[3];
        VoteReq voteReq = new VoteReq(
                hash,
                choice,
                address);
        if(args.length == 5){
            voteReq.setRemark(args[4]);
        }
        voteReq.setPassword(getPwd("\nEnter your account password:"));

        return voteReq;
    }
    @Override
    public CommandResult execute(String[] args) {
        Result<String> result = converterService.vote(buildVoteReq(args));
        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result.getData());
    }
}

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
import io.nuls.base.api.provider.consensus.facade.CreateAgentReq;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.cmd.client.utils.Na;
import io.nuls.core.core.annotation.Component;

import java.math.BigDecimal;
import java.math.BigInteger;

import static io.nuls.cmd.client.CommandHelper.getPwd;

/**
 * @author: zhoulijun
 */
@Component
public class CreateAgentProcessor extends ConsensusBaseProcessor implements CommandProcessor {


    @Override
    public String getCommand() {
        return "createagent";
    }

    @Override
    public String getHelp() {
        CommandBuilder bulider = new CommandBuilder();
        bulider.newLine(getCommandDescription())
                .newLine("\t<agentAddress>   agent owner address   -required")
                .newLine("\t<packingAddress>    packing address    -required")
                .newLine("\t<deposit>   amount you want to deposit, you can have up to 8 valid digits after the decimal point -required")
                .newLine("\t[rewardAddress]  Billing address    -not required")
                .newLine("\t[password]       password   -not required");
        return bulider.toString();
    }

    @Override
    public String getCommandDescription() {
        return "createagent <agentAddress> <packingAddress> <deposit> [rewardAddress] [password] --create a agent";
    }

    @Override
    public boolean argsValidate(String[] args) {
        checkIsAmount(args[3], "deposit");
        return true;
    }

    @Override
    public CommandResult execute(String[] args) {
        String agentAddress = args[1];
        String packingAddress = args[2];
        BigInteger deposit = config.toSmallUnit(args[3]);
        String rewardAddress = null;
        String password = null;
        if (args.length == 5) {
            rewardAddress = args[4];
        }
        if (args.length == 6) {
            password = args[5];
        }else{
            password = getPwd("\nEnter agent address password:");
        }
        CreateAgentReq req = new CreateAgentReq(agentAddress, packingAddress, rewardAddress, deposit, password);
        Result<String> result = consensusProvider.createAgent(req);
        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result);
    }
}

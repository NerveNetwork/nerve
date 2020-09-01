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
import io.nuls.base.api.provider.consensus.ConsensusProvider;
import io.nuls.base.api.provider.consensus.facade.BatchStakingMergeReq;
import io.nuls.base.api.provider.consensus.facade.WithdrawReq;
import io.nuls.base.data.NulsHash;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.ParameterException;
import io.nuls.cmd.client.config.Config;
import io.nuls.cmd.client.enums.DepositTimeType;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;

import java.util.Arrays;

/**
 * @author: zhoulijun
 */
@Component
public class BatchStakingMergeProcessor extends ConsensusBaseProcessor implements CommandProcessor {

    @Autowired
    Config config;

    ConsensusProvider consensusProvider = ServiceManager.get(ConsensusProvider.class);

    @Override
    public String getCommand() {
        return "batchStakingMerge";
    }

    @Override
    public String getHelp() {
        CommandBuilder bulider = new CommandBuilder();
        bulider.newLine(getCommandDescription())
                .newLine("\t<address>   address -required")
                .newLine("\t<txHashes>    your deposit transaction hashes, xxx,xxx,xxx  -required")
                .newLine("\t<deposit time> this lock time,options ：  " + Arrays.toString(DepositTimeType.values()) + " -required");
        return bulider.toString();
    }

    @Override
    public String getCommandDescription() {
        return "batchStakingMerge <address> <txHashes> [timeType]-- merge the staking txs";
    }

    @Override
    public boolean argsValidate(String[] args) {
        checkArgsNumber(args,2,3);
        checkAddress(config.getChainId(), args[1]);
        checkArgs(StringUtils.isNotBlank(args[2]) && args[2].length() >= NulsHash.HASH_LENGTH * 2, "txHashes format error");
        if(args.length == 4){
            checkArgs(Arrays.stream(DepositTimeType.values()).anyMatch(d->d.name().equals(args[3])),"deposit time error. you can import " + Arrays.toString(DepositTimeType.values()));
        }
        return true;
    }

    @Override
    public CommandResult execute(String[] args) {
        String address = args[1];
        String txHash = args[2];
        String password = getPwd();
        int timeType = -1;
        if(args.length>3){
            timeType = DepositTimeType.valueOf(args[3]).getType();
        }
        Result<String> result = consensusProvider.batchStakingMerge(new BatchStakingMergeReq(address, txHash, password,timeType));
        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result);
    }
}

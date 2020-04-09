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
import io.nuls.base.api.provider.consensus.facade.DepositReq;
import io.nuls.base.api.provider.consensus.facade.DepositToAgentReq;
import io.nuls.base.data.NulsHash;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.config.Config;
import io.nuls.cmd.client.enums.DepositTimeType;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: zhoulijun
 */
public abstract class DepositProcessor extends ConsensusBaseProcessor implements CommandProcessor {

    @Component
    public static class Deposit extends DepositProcessor {

        @Override
        public String getCommand() {
            return "deposit";
        }

        @Override
        public String getHelp() {
            CommandBuilder bulider = new CommandBuilder();
            bulider.newLine(getCommandDescription())
                    .newLine("\t<address>   Your own account address -required")
                    .newLine("\t<deposit>   the amount you want to deposit, you can have up to 8 valid digits after the decimal point -required")
                    .newLine("\t[symbol]    staking symbol. default NVT ");
            return bulider.toString();
        }

        @Override
        public String getCommandDescription() {
            return getCommand() + " <address> <deposit> [symbol] --apply for deposit";
        }

        @Override
        public boolean argsValidate(String[] args) {
            checkArgsNumber(args,2,3);
            checkAddress(config.getChainId(),args[1]);
            checkIsAmount(args[2],"deposit");
            return true;
        }


    }

    @Component
    public static class DepositFixed extends DepositProcessor {

        public static final int DEPOSIT_TYPE = 1;


        @Override
        public String getCommand() {
            return "depositFixed";
        }

        @Override
        public String getHelp() {
            CommandBuilder bulider = new CommandBuilder();
            bulider.newLine(getCommandDescription())
                    .newLine("\t<address>   Your own account address -required")
                    .newLine("\t<deposit>   the amount you want to deposit, you can have up to 8 valid digits after the decimal point -required")
                    .newLine("\t<deposit time> this lock time,options ：  " + Arrays.toString(DepositTimeType.values()) + " -required")
                    .newLine("\t[symbol]    staking symbol. default NVT ");
            return bulider.toString();
        }

        @Override
        public String getCommandDescription() {
            return getCommand() + " <address> <deposit> <deposit time> [symbol] --apply for deposit";
        }

        @Override
        public boolean argsValidate(String[] args) {
            checkArgsNumber(args,3,4);
            checkAddress(config.getChainId(),args[1]);
            checkIsAmount(args[2],"deposit");
            checkArgs(Arrays.stream(DepositTimeType.values()).anyMatch(d->d.name().equals(args[3])),"deposit time error. you can import " + Arrays.toString(DepositTimeType.values()));
            return true;
        }

        @Override
        public DepositReq getParam(String[] args) {
            DepositReq req = super.getParam(args);
            req.setDepositType(DEPOSIT_TYPE);
            req.setTimeType(DepositTimeType.valueOf(args[3]).getType());
            return req;
        }
    }

    ConsensusProvider consensusProvider = ServiceManager.get(ConsensusProvider.class);

    @Autowired
    Config config;


    public DepositReq getParam(String[] args){
        return new DepositReq();
    }


    @Override
    public CommandResult execute(String[] args) {
        String address = args[1];
        String password = getPwd();

        int assetChainId = config.getChainId();
        int assetId = config.getAssetsId();
        int decimals = config.getDecimals();
        if(args.length > 3){
            String symbol = args[3];
//            //获取其他币种的id  cv_get_heterogeneous_chain_asset_info
//            Map<String,Object> map = new HashMap<>();
//            assetChainId = map.get("chainId");
//            assetId = map.get("assetId");
//            decimals = map.get("decimals");
        }
        DepositReq req = getParam(args);
        req.setAddress(address);
        req.setPassword(password);
        req.setAssetChainId(assetChainId);
        req.setAssetId(assetId);
        req.setDeposit(config.toSmallUnit(args[2],decimals));
        Result<String> result = consensusProvider.deposit(req);
        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result.getData());
    }
}

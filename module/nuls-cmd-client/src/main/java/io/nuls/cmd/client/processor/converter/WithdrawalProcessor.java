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
import io.nuls.base.api.provider.converter.facade.WithdrawalReq;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.config.Config;
import io.nuls.cmd.client.processor.CommandGroup;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.cmd.client.utils.AssetsUtil;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * 用于异构链提现(赎回)资产
 * @author: Charlie
 * @date: 2020/4/28
 */
@Component
public class WithdrawalProcessor implements CommandProcessor {

    @Autowired
    Config config;

    ConverterService converterService = ServiceManager.get(ConverterService.class);

    @Override
    public CommandGroup getGroup(){
        return CommandGroup.Converter;
    }
    @Override
    public String getCommand() {
        return "redeem";
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription())
                .newLine("\t<assetId> \t\tasset id - Required")
                .newLine("\t<heterogeneousAddress> \t\theterogeneous receiving address - Required")
                .newLine("\t<amount> \t\tWithdrawal amount - Required")
                .newLine("\t<redeemAddress> \t\tRedeem address - Required")
                .newLine("\t[remark] \t\tremark - ");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "redeem <assetId> <heterogeneousAddress> <amount> <redeemAddress> [remark] --redeem";
    }

    @Override
    public boolean argsValidate(String[] args) {
        checkArgsNumber(args,4,5);
        checkIsNumeric(args[1],"assetId");
        checkIsAmount(args[3],"amount");
        checkAddress(config.getChainId(),args[4]);
        return true;
    }

    /**
     * @param heterogeneousChainId
     * @param heterogeneousAssetId
     * @param value
     * @return
     */
    public BigInteger toSimallUnit(int heterogeneousChainId, int heterogeneousAssetId, BigDecimal value){
        // 默认8位
        int decimals = AssetsUtil.getAssetDecimal(heterogeneousChainId,heterogeneousAssetId);
        return config.toSmallUnit(value, decimals);
    }

    private WithdrawalReq buildWithdrawalReq(String[] args) {

        int chainId = config.getChainId();
        int assetId = Integer.parseInt(args[1]);
        String heterogeneousAddress = args[2];
        BigInteger amount = toSimallUnit(chainId, assetId, new BigDecimal(args[3]));
        String redeemAddress = args[4];
        WithdrawalReq withdrawalReq = new WithdrawalReq(
                assetId,
                heterogeneousAddress,
                amount,
                redeemAddress);
        if(args.length == 6){
            withdrawalReq.setRemark(args[5]);
        }
        withdrawalReq.setPassword(getPwd("\nEnter your account password:"));

        return withdrawalReq;
    }

    @Override
    public CommandResult execute(String[] args) {
        Result<String> result = converterService.withdrawal(buildWithdrawalReq(args));
        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result.getData());
    }
}

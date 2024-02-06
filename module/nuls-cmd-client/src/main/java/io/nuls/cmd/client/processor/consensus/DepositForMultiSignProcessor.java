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
import io.nuls.base.api.provider.consensus.facade.GetStackingAssetBySymbolReq;
import io.nuls.base.api.provider.consensus.facade.MultiSignJoinStackingReq;
import io.nuls.base.api.provider.ledger.facade.AssetInfo;
import io.nuls.base.api.provider.transaction.facade.MultiSignTransferRes;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.config.Config;
import io.nuls.cmd.client.enums.DepositTimeType;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.cmd.client.utils.AssetsUtil;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * @author: zhoulijun
 * Using multiple signed accounts for delegated consensus（Not delegated to nodes created by multiple signed accounts）
 */
public abstract class DepositForMultiSignProcessor extends ConsensusBaseProcessor implements CommandProcessor {

    @Component
    public static class MultiDeposit extends DepositForMultiSignProcessor {

        @Override
        public String getCommand() {
            return "multiDeposit";
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

        @Override
        public MultiSignJoinStackingReq getParam(String[] args) {
            MultiSignJoinStackingReq req = new MultiSignJoinStackingReq();
            Integer decimals =  config.getDecimals();
            if(args.length > 3){
                String symbol = args[3];
                decimals = AssetsUtil.getAssetDecimal(symbol);
                if(decimals == null){
                    throw new RuntimeException("not support " + symbol);
                }
                Result<AssetInfo> assetInfo = consensusProvider.getStatcingAssetBySymbol(new GetStackingAssetBySymbolReq(symbol));
                if(assetInfo.isFailed()){
                    throw new RuntimeException(assetInfo.getMessage());
                }
                req.setAssetChainId(assetInfo.getData().getAssetChainId());
                req.setAssetId(assetInfo.getData().getAssetId());
            }else{
                int assetChainId = config.getChainId();
                int assetId = config.getAssetsId();
                req.setAssetChainId(assetChainId);
                req.setAssetId(assetId);
            }
            req.setDeposit(config.toSmallUnit(args[2],decimals));
            return req;
        }
    }

    @Component
    public static class MultiDepositFixed extends DepositForMultiSignProcessor {

        public static final int DEPOSIT_TYPE = 1;


        @Override
        public String getCommand() {
            return "multiDepositFixed";
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
        public MultiSignJoinStackingReq getParam(String[] args) {
            MultiSignJoinStackingReq req = super.getParam(args);
            req.setDepositType(DEPOSIT_TYPE);
            req.setTimeType(DepositTimeType.valueOf(args[3]).getType());
            return req;
        }
    }

    ConsensusProvider consensusProvider = ServiceManager.get(ConsensusProvider.class);

    @Autowired
    Config config;


    public MultiSignJoinStackingReq getParam(String[] args){
        MultiSignJoinStackingReq req = new MultiSignJoinStackingReq();
        Integer decimals =  config.getDecimals();
        if(args.length > 4){
            String symbol = args[4];
            decimals = AssetsUtil.getAssetDecimal(symbol);
            if(decimals == null){
                throw new RuntimeException("not support " + symbol);
            }
            Result<AssetInfo> assetInfo = consensusProvider.getStatcingAssetBySymbol(new GetStackingAssetBySymbolReq(symbol));
            if(assetInfo.isFailed()){
                throw new RuntimeException(assetInfo.getMessage());
            }
            req.setAssetChainId(assetInfo.getData().getAssetChainId());
            req.setAssetId(assetInfo.getData().getAssetId());
        }else{
            int assetChainId = config.getChainId();
            int assetId = config.getAssetsId();
            req.setAssetChainId(assetChainId);
            req.setAssetId(assetId);
        }
        req.setDeposit(config.toSmallUnit(args[2],decimals));
        return req;
    }


    @Override
    public CommandResult execute(String[] args) {
        String address = args[1];
        try{
            MultiSignJoinStackingReq req = getParam(args);
            req.setAddress(address);
            Result<MultiSignTransferRes> result= consensusProvider.multiSignJoinStacking(req);
            if (result.isFailed()) {
                return CommandResult.getFailed(result);
            }
            return CommandResult.getSuccess(result);
        }catch (Exception e){
            return CommandResult.getFailed(e.getMessage());
        }

    }
}

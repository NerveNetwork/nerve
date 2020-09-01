package io.nuls.cmd.client.processor.consensus;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.consensus.ConsensusProvider;
import io.nuls.base.api.provider.consensus.facade.MultiAgentDepositChangeReq;
import io.nuls.base.api.provider.transaction.facade.MultiSignTransferRes;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.config.Config;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

import java.math.BigInteger;

public abstract class ChangeMultiAgentDepositProcessor extends ConsensusBaseProcessor implements CommandProcessor {
    @Component
    public static class AppendMultiAgentDeposit extends ChangeMultiAgentDepositProcessor {

        @Override
        public String getCommand() {
            return "appendMultiAgentDeposit";
        }

    }

    @Component
    public static class ReduceMutilAgentDeposit extends ChangeMultiAgentDepositProcessor {

        @Override
        public String getCommand() {
            return "reduceMultiAgentDeposit";
        }

        @Override
        public MultiAgentDepositChangeReq getParam(MultiAgentDepositChangeReq req) {
            req.setAmount(req.getAmount().negate());
            return req;
        }
    }

    ConsensusProvider consensusProvider = ServiceManager.get(ConsensusProvider.class);

    @Autowired
    Config config;

    @Override
    public String getHelp() {
        CommandBuilder bulider = new CommandBuilder();
        bulider.newLine(getCommandDescription())
                .newLine("\t<address>   Your own account address -required")
                .newLine("\t<deposit>   the amount you want to deposit, you can have up to 8 valid digits after the decimal point -required")
                .newLine("\t[sign address] first sign address -- not required");
        return bulider.toString();
    }

    @Override
    public String getCommandDescription() {
        return getCommand() + " <address> <deposit> [sign address] --apply for deposit";
    }

    @Override
    public boolean argsValidate(String[] args) {
        checkArgsNumber(args,2,3);
        checkAddress(config.getChainId(),args[1]);
        checkIsAmount(args[2],"deposit");
        return true;
    }

    public MultiAgentDepositChangeReq getParam(MultiAgentDepositChangeReq req){
        return req;
    }

    @Override
    public CommandResult execute(String[] args) {
        String address = args[1];
        BigInteger deposit = config.toSmallUnit(args[2]);
        Result<MultiSignTransferRes> result;
        if(args.length == 4){
            String password = getPwd("\nEnter agent address password:");
            String signAddress = args[3];
            result = consensusProvider.changeMultiAgentDeposit(getParam(new MultiAgentDepositChangeReq(address,deposit,signAddress,password)));
        }else{
            result = consensusProvider.changeMultiAgentDeposit(getParam(new MultiAgentDepositChangeReq(address,deposit)));
        }
        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result);
    }
}

package io.nuls.cmd.client.processor.farm;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.farm.FarmProvider;
import io.nuls.base.api.provider.farm.facade.FarmStakeReq;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.processor.CommandGroup;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.core.core.annotation.Component;

/**
 * @author Niels
 */
@Component
public class FarmStakeProcessor implements CommandProcessor {

    private FarmProvider farmProvider = ServiceManager.get(FarmProvider.class);

    @Override
    public String getCommand() {
        return "farmstake";
    }

    @Override
    public CommandGroup getGroup() {
        return CommandGroup.Farm;
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription())
                .newLine("\t<address> Address to send the transaction - Required")
                .newLine("\t<farmHash> the ID of the farm - Required")
                .newLine("\t<amount> Stake amount - Required");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "farmstake <address> <farmHash> <amount> -- Stake assets to farm";
    }

    @Override
    public boolean argsValidate(String[] args) {
        checkArgsNumber(args, 3);
        return true;
    }

    @Override
    public CommandResult execute(String[] args) {
        String password = getPwd();
        Result<String> result = farmProvider.stake(new FarmStakeReq(args[1], args[2], Double.parseDouble(args[3]),password));

        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result);
    }
}

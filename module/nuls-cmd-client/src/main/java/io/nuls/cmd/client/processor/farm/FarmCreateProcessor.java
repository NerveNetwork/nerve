package io.nuls.cmd.client.processor.farm;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.farm.FarmProvider;
import io.nuls.base.api.provider.farm.facade.CreateFarmReq;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.processor.CommandGroup;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.core.core.annotation.Component;

/**
 * @author Niels
 */
@Component
public class FarmCreateProcessor implements CommandProcessor {

    private FarmProvider farmProvider = ServiceManager.get(FarmProvider.class);

    @Override
    public String getCommand() {
        return "createfarm";
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
                .newLine("\t<stakeToken> Types of pledge assets e.g：1-1 - Required")
                .newLine("\t<syrupToken> Types of syrup assets e.g：9-1 - Required")
                .newLine("\t<totalSyrupAmount> Total amount of syrup assets - Required")
                .newLine("\t<syrupPerBlock> Number of syrup awarded per block - Required")
                .newLine("\t<startHeight> Function effective height - Required")
                .newLine("\t<lockedHeight> The minimum height of unlocking the pledged assets - Required");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "createfarm <address> <stakeToken> <syrupToken> <totalSyrupAmount> <syrupPerBlock> <startHeight> <lockedHeight> -- Create a pool of pledged assets for rewards";
    }

    @Override
    public boolean argsValidate(String[] args) {
        checkArgsNumber(args, 7);
        return true;
    }

    @Override
    public CommandResult execute(String[] args) {
        String password = getPwd();
        Result<String> result = farmProvider.createFarm(new CreateFarmReq(args[1], args[2], args[3], Double.parseDouble(args[4]),Double.parseDouble(args[5]), Long.parseLong(args[6]), Long.parseLong(args[7]),password));

        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result);
    }
}

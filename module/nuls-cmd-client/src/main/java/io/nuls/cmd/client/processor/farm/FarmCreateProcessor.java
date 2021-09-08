package io.nuls.cmd.client.processor.farm;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.farm.FarmProvider;
import io.nuls.base.api.provider.farm.facade.CreateFarmReq;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.ParameterException;
import io.nuls.cmd.client.processor.CommandGroup;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.core.core.annotation.Component;

import java.util.Arrays;

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
                .newLine("\t<lockedHeight> The minimum height of unlocking the pledged assets - Required")
                .newLine("\t[modifiable] Can I modify it? Default = 0, yes = 1")
                .newLine("\t[withdrawLockTime] Lock out duration,Default = 0s");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "createfarm <address> <stakeToken> <syrupToken> <totalSyrupAmount> <syrupPerBlock> <startHeight> <lockedHeight> [modifiable] [withdrawLockTime] -- Create a pool of pledged assets for rewards";
    }

    @Override
    public boolean argsValidate(String[] args) {
        if (args.length<8) {
            ParameterException.throwParameterException();
        }
        return true;
    }

    @Override
    public CommandResult execute(String[] args) {
        String password = getPwd();
        boolean modifiable = false;
        long withdrawLockTime = 0;
        if (args.length > 8) {
            modifiable = "1".equals(args[8]);
        }
        if (args.length > 9) {
            withdrawLockTime = Long.parseLong(args[9]);
        }
        Result<String> result = farmProvider.createFarm(new CreateFarmReq(args[1], args[2], args[3], Double.parseDouble(args[4]), Double.parseDouble(args[5]), Long.parseLong(args[6]), Long.parseLong(args[7]), modifiable, withdrawLockTime, password));

        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result);
    }
}

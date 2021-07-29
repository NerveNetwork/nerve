package io.nuls.cmd.client.processor.farm;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.farm.FarmProvider;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.processor.CommandGroup;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.core.core.annotation.Component;

/**
 * @author Niels
 */
@Component
public class FarmUserInfoProcessor implements CommandProcessor {

    private FarmProvider farmProvider = ServiceManager.get(FarmProvider.class);

    @Override
    public String getCommand() {
        return "getstakeinfo";
    }

    @Override
    public CommandGroup getGroup() {
        return CommandGroup.Farm;
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription())
                .newLine("\t<farmHash> the ID of the farm - Required")
                .newLine("\t<address> Account address to query - Required");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "getstakeinfo <farmHash> <address> -- View Farm details";
    }

    @Override
    public boolean argsValidate(String[] args) {
        checkArgsNumber(args, 2);
        return true;
    }

    @Override
    public CommandResult execute(String[] args) {

        Result<String> result = farmProvider.farmUserInfo(args[1],args[2]);

        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result);
    }
}

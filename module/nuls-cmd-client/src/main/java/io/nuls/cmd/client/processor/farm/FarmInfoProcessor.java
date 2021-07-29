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
public class FarmInfoProcessor implements CommandProcessor {

    private FarmProvider farmProvider = ServiceManager.get(FarmProvider.class);

    @Override
    public String getCommand() {
        return "getfarm";
    }

    @Override
    public CommandGroup getGroup() {
        return CommandGroup.Farm;
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription())
                .newLine("\t<farmHash> the ID of the farm - Required");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "getfarm <farmHash> -- View Farm details";
    }

    @Override
    public boolean argsValidate(String[] args) {
        checkArgsNumber(args, 1);
        return true;
    }

    @Override
    public CommandResult execute(String[] args) {

        Result<String> result = farmProvider.farmInfo(args[1]);

        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result);
    }
}

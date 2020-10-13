package io.nuls.cmd.client.processor.dex;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.dex.DexProvider;
import io.nuls.base.api.provider.dex.facode.DexQueryReq;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.config.Config;
import io.nuls.cmd.client.processor.CommandGroup;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

import java.util.Map;

@Component
public class QueryTradingProcess implements CommandProcessor {

    @Autowired
    Config config;

    DexProvider dexProvider = ServiceManager.get(DexProvider.class);

    @Override
    public String getCommand() {
        return "getTrading";
    }

    @Override
    public CommandGroup getGroup() {
        return CommandGroup.Dex;
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription())
                .newLine("\t<tradingHash> txHash of trading   -required");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "getTrading <tradingHash> --get the coin trading info";
    }

    @Override
    public boolean argsValidate(String[] args) {
        checkArgsNumber(args, 1);
        return true;
    }

    @Override
    public CommandResult execute(String[] args) {
        String tradingHash = args[1];
        DexQueryReq req = new DexQueryReq(tradingHash);
        Result<Map> result = dexProvider.getTrading(req);
        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result);
    }
}

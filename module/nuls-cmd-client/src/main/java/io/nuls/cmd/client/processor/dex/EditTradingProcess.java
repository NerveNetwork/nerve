package io.nuls.cmd.client.processor.dex;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.dex.DexProvider;
import io.nuls.base.api.provider.dex.facode.DexQueryReq;
import io.nuls.base.api.provider.dex.facode.EditTradingReq;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.config.Config;
import io.nuls.cmd.client.processor.CommandGroup;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

import java.math.BigInteger;
import java.util.Map;

@Component
public class EditTradingProcess implements CommandProcessor {

    @Autowired
    Config config;

    DexProvider dexProvider = ServiceManager.get(DexProvider.class);

    @Override
    public String getCommand() {
        return "editTrading";
    }

    @Override
    public CommandGroup getGroup() {
        return CommandGroup.Dex;
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription())
                .newLine("\t<address> trading owner address   -required")
                .newLine("\t<password> password   -required")
                .newLine("\t<tradingHash> txHash of trading   -required")
                .newLine("\t<scaleQuoteDecimal> minimum reserved number of quote coin   -required")
                .newLine("\t<scaleBaseDecimal> minimum reserved number of base coin   -required")
                .newLine("\t<minQuoteAmount> Minimum transaction amount of quote coin   -required")
                .newLine("\t<minBaseAmount> Minimum transaction amount of base coin   -required");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "editTrading <address> <password> <tradingHash> <scaleQuoteDecimal> <scaleBaseDecimal> <minQuoteAmount> <minBaseAmount> --edit the trading config";
    }

    @Override
    public boolean argsValidate(String[] args) {
        checkArgsNumber(args, 7);
        checkAddress(config.getChainId(), args[1]);
        checkIsNumeric(args[4], "scaleQuoteDecimal");
        checkIsNumeric(args[5], "scaleBaseDecimal");
        checkIsDouble(args[6], "minQuoteAmount");
        checkIsDouble(args[7], "minBaseAmount");
        return true;
    }

    @Override
    public CommandResult execute(String[] args) {
        String address = args[1];
        String password = args[2];
        String tradingHash = args[3];
        int scaleQuoteDecimal = Integer.parseInt(args[4]);
        int scaleBaseDecimal = Integer.parseInt(args[5]);

        DexQueryReq queryReq = new DexQueryReq(tradingHash);
        Result<Map> result = dexProvider.getTrading(queryReq);
        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        Map map = result.getData();
        int quoteDecimal = (int) map.get("quoteDecimal");
        int baseDecimal = (int) map.get("baseDecimal");

        BigInteger minQuoteAmount = config.toSmallUnit(args[6], quoteDecimal);
        BigInteger minBaseAmount = config.toSmallUnit(args[7], baseDecimal);

        EditTradingReq req = new EditTradingReq(address, password, tradingHash, scaleQuoteDecimal, scaleBaseDecimal, minQuoteAmount, minBaseAmount);
        Result<String> editResult = dexProvider.editTrading(req);
        if (editResult.isFailed()) {
            return CommandResult.getFailed(editResult);
        }
        return CommandResult.getSuccess(editResult);
    }
}

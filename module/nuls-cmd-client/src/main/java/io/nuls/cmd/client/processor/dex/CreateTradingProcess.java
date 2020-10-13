package io.nuls.cmd.client.processor.dex;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.dex.DexProvider;
import io.nuls.base.api.provider.dex.facode.CoinTradingReq;
import io.nuls.base.api.provider.dex.facode.EditTradingReq;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.config.Config;
import io.nuls.cmd.client.processor.CommandGroup;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.cmd.client.utils.AssetsUtil;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

import java.math.BigInteger;

@Component
public class CreateTradingProcess implements CommandProcessor {

    @Autowired
    Config config;

    DexProvider dexProvider = ServiceManager.get(DexProvider.class);

    @Override
    public String getCommand() {
        return "createTrading";
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
                .newLine("\t<quoteAssetChainId> assetChainId of quote coin  -required")
                .newLine("\t<quoteAssetId> assetId of quote coin  -required")
                .newLine("\t<baseAssetChainId> assetChainId of base coin  -required")
                .newLine("\t<baseAssetId> assetId of base coin  -required")
                .newLine("\t<scaleQuoteDecimal> minimum reserved number of quote coin   -required")
                .newLine("\t<scaleBaseDecimal> minimum reserved number of base coin   -required")
                .newLine("\t<minQuoteAmount> Minimum transaction amount of quote coin   -required")
                .newLine("\t<minBaseAmount> Minimum transaction amount of base coin   -required");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "createTrading <address> <password> <quoteAssetChainId> <quoteAssetId> <baseAssetChainId> <baseAssetId> <scaleQuoteDecimal> <scaleBaseDecimal> <minQuoteAmount> <minBaseAmount> --create the coin trading";
    }

    @Override
    public boolean argsValidate(String[] args) {
        checkArgsNumber(args, 10);
        checkAddress(config.getChainId(), args[1]);
        checkIsNumeric(args[3], "quoteAssetChainId");
        checkIsNumeric(args[4], "quoteAssetId");
        checkIsNumeric(args[5], "baseAssetChainId");
        checkIsNumeric(args[6], "baseAssetId");

        checkIsNumeric(args[7], "scaleQuoteDecimal");
        checkIsNumeric(args[8], "scaleBaseDecimal");
        checkIsDouble(args[9], "minQuoteAmount");
        checkIsDouble(args[10], "minBaseAmount");
        return true;
    }

    @Override
    public CommandResult execute(String[] args) {
        String address = args[1];
        String password = args[2];

        int quoteAssetChainId = Integer.parseInt(args[3]);
        int quoteAssetId = Integer.parseInt(args[4]);
        int baseAssetChainId = Integer.parseInt(args[5]);
        int baseAssetId = Integer.parseInt(args[6]);

        int scaleQuoteDecimal = Integer.parseInt(args[7]);
        int scaleBaseDecimal = Integer.parseInt(args[8]);

        int quoteDecimal = AssetsUtil.getAssetDecimal(quoteAssetChainId, quoteAssetId);
        int baseDecimal = AssetsUtil.getAssetDecimal(baseAssetChainId, baseAssetId);

        BigInteger minQuoteAmount = config.toSmallUnit(args[9], quoteDecimal);
        BigInteger minBaseAmount = config.toSmallUnit(args[10], baseDecimal);

        CoinTradingReq req = new CoinTradingReq(address, password, quoteAssetChainId,quoteAssetId,baseAssetChainId,baseAssetId, scaleQuoteDecimal, scaleBaseDecimal, minQuoteAmount, minBaseAmount);
        Result<String> result = dexProvider.createTrading(req);
        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result);
    }
}

package io.nuls.cmd.client.processor.transaction;

import io.nuls.base.api.provider.Result;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.config.Config;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

@Component
public class TransferTestProcessor extends TransactionBaseProcessor implements CommandProcessor {

    @Autowired
    Config config;

    @Override
    public String getCommand() {
        return "transferTest";
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
//        builder.newLine(getCommandDescription())
//                .newLine("\t<method> \t\tWhich method is used to send the transaction 1:Send fixed50Wpen 2:Exchange of 20000 accounts- Required")
//                .newLine("\t<address1> \tRich address1 - Required")
//                .newLine("\t[address2] \t\tRich address2 Required when exchanging 20000 accounts - Required");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "transferTest <method> <address1> [address2] --transfer test";
    }

    @Override
    public boolean argsValidate(String[] args) {
        return true;
    }


    @Override
    public CommandResult execute(String[] args) {
        Integer method = Integer.parseInt(args[1]);
        String address1 = args[2];
        String address2 = null;
        String amount = null;
        if(method == 2) {
            address2 = args[3];
            if(args.length == 5) {
                amount = args[4];
            }
        }

        Result<String> result = transferService.transferTest(method, address1, address2,amount);
        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result.getData());
    }


}

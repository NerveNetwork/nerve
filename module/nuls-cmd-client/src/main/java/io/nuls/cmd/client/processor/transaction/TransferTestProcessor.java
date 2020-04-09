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
//                .newLine("\t<method> \t\t用哪个方法发送交易 1:发送固定50W笔 2:两万账户互发- Required")
//                .newLine("\t<address1> \t有钱地址1 - Required")
//                .newLine("\t[address2] \t\t有钱地址2 两万账户互发时必填 - Required");
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
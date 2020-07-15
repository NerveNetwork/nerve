/*
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.nuls.cmd.client.processor.transaction;


import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.transaction.facade.TransferReq;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.ParameterException;
import io.nuls.cmd.client.config.Config;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author: zhoulijun
 */
@Component
public class BatchTransferProcessor extends TransactionBaseProcessor implements CommandProcessor {

    @Autowired
    Config config;

    @Override
    public String getCommand() {
        return "batchtransfer";
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription())
                .newLine("\t<address> \t\tsource address or alias - Required")
                .newLine("\t<toAddress file path> \treceiving address list for file - Required")
                .newLine("\t[assetChainId] \tassetChain Id")
                .newLine("\t[assetId] \tasset Id")
                .newLine("\t[remark] \t\tremark - ")
                .newLine("\t[password] \t\tpassword - ");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "batchtransfer <address>|<alias> <toAddress file path> [remark] [password] --transfer";
    }

    @Override
    public boolean argsValidate(String[] args) {
        checkArgsNumber(args,2,3,4,5,6);
        return true;
    }

    private TransferReq buildTransferReq(String[] args) {
        String formAddress = args[1];
        String toAddress = args[2];
        BigInteger amount = config.toSmallUnit(new BigDecimal(args[3]));
        String password;
        if(args.length == 6){
            password = args[5];
        }else{
            password = getPwd("\nEnter your account password:");
        }
        TransferReq.TransferReqBuilder builder =
                new TransferReq.TransferReqBuilder(config.getChainId(),config.getAssetsId())
                        .addForm(formAddress,password, amount)
                        .addTo(toAddress,amount);
        if(args.length == 5){
            builder.setRemark(args[4]);
        }
        return builder.build(new TransferReq());
    }

    @Override
    public CommandResult execute(String[] args) {
        String filePath = args[3];
        File file = new File(filePath);
        if(!file.exists()){
            ParameterException.throwParameterException("to address file not exists");
        }

        Result<String> result = transferService.transfer(buildTransferReq(args));
        if (result.isFailed()) {
            return CommandResult.getFailed(result);
        }
        return CommandResult.getSuccess(result.getData());
    }

//    public static class Item {
//        String address;
//        BigInteger amount;
//
//        public Item(String address, BigInteger amount) {
//            this.address = address;
//            this.amount = amount;
//        }
//
//        public String getAddress() {
//            return address;
//        }
//
//        public void setAddress(String address) {
//            this.address = address;
//        }
//
//        public BigInteger getAmount() {
//            return amount;
//        }
//
//        public void setAmount(BigInteger amount) {
//            this.amount = amount;
//        }
//    }
//
//    private void send(List<Item> to,String fromAddress){
//        TransferService transferService = ServiceManager.get(TransferService.class);
//        StringBuilder buf = new StringBuilder();
//        to = to.stream().filter(d->
//                !d.getAddress().equals(fromAddress)
//        ).collect(Collectors.toList());
//        to.forEach(toAddress->{
//            buf.append(toAddress.getAddress()).append(":").append(toAddress.getAmount()).append("\n");
//        });
//        BigInteger fromAmount = to.stream().map(d->d.getAmount()).reduce(BigInteger::add).orElse(BigInteger.ZERO);
//        if(fromAmount.equals(BigInteger.ZERO)){
//            Log.error("to is empty");
//            System.exit(0);
//        }
////        TransferReq.TransferReqBuilder builder =
////                new TransferReq.TransferReqBuilder(CHAIN_ID, NVT_ASSET_ID)
////                        .addForm(NVT_CHAIN_ID, NVT_ASSET_ID, fromAddress, password, fromAmount);
////        to.forEach(toAddress->{
////            if(toAddress.getAddress().equals(fromAddress)){
////                return ;
////            }
////            builder.addTo(NVT_CHAIN_ID, NVT_ASSET_ID, toAddress.getAddress(), toAddress.getAmount());
////        });
//        Result<String> result = transferService.transfer(builder.build(new TransferReq()));
//        if(result.isFailed()){
//            Log.error("转账失败,原因:{}",result.getMessage());
//            Log.error("{}",result);
//            Log.error("失败地址列表:\n{}",buf.toString());
//            System.exit(0);
//        }
//        Log.info("转账成功:hash:{}\n{}",result.getData(),buf.toString());
//    }
//
//    private List<Item> read(File file) throws IOException {
//        BufferedReader reader = new BufferedReader(new FileReader(file));
//        String line = reader.readLine();
//        List<Item> res = new ArrayList<>();
//        while(line != null) {
//            String[] key = line.split(":");
//            String address = key[0];
//            BigInteger amount = new BigInteger(key[1]);
//            res.add(new Item(address,amount));
//            line = reader.readLine();
//        }
//        return res;
//    }
//
//    static void sendNVT(List<Item> list,String fromAddress) {
//        int index=0;
//        int toSize=500;
//        while(true){
//            if(index >= list.size()){
//                break;
//            }
//            int targetIndex = index + toSize;
//            if(targetIndex > list.size()){
//                targetIndex = list.size();
//            }
//            List<Item> temp = list.subList(index,targetIndex);
//            index += toSize;
//            send(temp,fromAddress);
//            try {
//                TimeUnit.MILLISECONDS.sleep(2L);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
}

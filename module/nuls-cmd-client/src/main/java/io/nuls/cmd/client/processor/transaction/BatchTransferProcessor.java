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
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.transaction.TransferService;
import io.nuls.base.api.provider.transaction.facade.TransferReq;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.ParameterException;
import io.nuls.cmd.client.config.Config;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.cmd.client.utils.AssetsUtil;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Override
    public CommandResult execute(String[] args) {
        String fromAddress = args[1];
        String filePath = args[2];
        String remark = "";
        int assetChainId = config.getChainId();
        int assetId = config.getAssetsId();
        if (args.length > 3){
            assetChainId = Integer.parseInt(args[3]);
        }
        if (args.length > 4){
            assetId = Integer.parseInt(args[4]);
        }
        if (args.length > 5){
            remark = args[5];
        }
        File file = new File(filePath);
        if(!file.exists()){
            ParameterException.throwParameterException("to address file not exists");
        }
        Integer decimalInt = AssetsUtil.getAssetDecimal(assetChainId, assetId);
        List<Item> list = null;
        try {
            list = read(file).stream().map(d-> new Item(d.getAddress(),config.toSmallUnit(new BigDecimal(d.getAmount()),decimalInt))).collect(Collectors.toList());
        } catch (IOException e) {
            ParameterException.throwParameterException("read to address file error");
        }
        List<String> hash = sendNVT(list,fromAddress,getPwd(),assetChainId,assetId,remark);
        return CommandResult.getSuccess("done \n" + hash.stream().reduce("",(d1,d2)->d1 + "," + d2));
    }

    public static class Item {
        String address;
        BigInteger amount;

        public Item(String address, BigInteger amount) {
            this.address = address;
            this.amount = amount;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public BigInteger getAmount() {
            return amount;
        }

        public void setAmount(BigInteger amount) {
            this.amount = amount;
        }
    }

    private String send(List<Item> to, String fromAddress, String password, int assetChainId, int assetId, String remark){
        TransferService transferService = ServiceManager.get(TransferService.class);
        StringBuilder buf = new StringBuilder();
        to = to.stream().filter(d->
                !d.getAddress().equals(fromAddress)
        ).collect(Collectors.toList());
        to.forEach(toAddress->{
            buf.append(toAddress.getAddress()).append(":").append(toAddress.getAmount()).append("\n");
        });
        BigInteger fromAmount = to.stream().map(d->d.getAmount()).reduce(BigInteger::add).orElse(BigInteger.ZERO);
        if(fromAmount.equals(BigInteger.ZERO)){
            Log.error("to is empty");
            System.exit(0);
        }
        TransferReq.TransferReqBuilder builder =
                new TransferReq.TransferReqBuilder(config.getChainId(), config.getAssetsId())
                        .addForm(assetChainId, assetId, fromAddress, password, fromAmount);
        to.forEach(toAddress->{
            if(toAddress.getAddress().equals(fromAddress)){
                return ;
            }
            builder.addTo(assetChainId, assetId, toAddress.getAddress(), toAddress.getAmount());
        });
        builder.setRemark(remark);
        Log.info("{}",builder.build(new TransferReq()));
        return "a";
//        Result<String> result = transferService.transfer(builder.build(new TransferReq()));
//        if(result.isFailed()){
//            Log.error("转账失败,原因:{}",result.getMessage());
//            Log.error("{}",result);
//            Log.error("失败地址列表:\n{}",buf.toString());
//            return null;
//        }
//        Log.info("转账成功:hash:{}\n{}",result.getData(),buf.toString());
//        return result.getData();
    }

    private List<Item> read(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        List<Item> res = new ArrayList<>();
        while(line != null) {
            String[] key = line.split(":");
            String address = key[0];
            BigInteger amount = new BigInteger(key[1]);
            res.add(new Item(address,amount));
            line = reader.readLine();
        }
        return res;
    }

    private List<String> sendNVT(List<Item> list,String fromAddress,String password,int assetChainId,int assetId,String remark) {
        int index=0;
        int toSize=50;
        List<String> hash = new ArrayList<>();
        while(true){
            if(index >= list.size()){
                break;
            }
            int targetIndex = index + toSize;
            if(targetIndex > list.size()){
                targetIndex = list.size();
            }
            List<Item> temp = list.subList(index,targetIndex);
            index += toSize;
            String txHash = send(temp,fromAddress,password,assetChainId,assetId,remark);
            if(txHash == null){
                return hash;
            }
            hash.add(txHash);
            try {
                TimeUnit.MILLISECONDS.sleep(2L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return hash;
    }
}

package network.nerve.distribute;

import io.nuls.base.api.provider.Provider;
import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.ledger.LedgerProvider;
import io.nuls.base.api.provider.ledger.facade.AccountBalanceInfo;
import io.nuls.base.api.provider.ledger.facade.GetBalanceReq;
import io.nuls.base.api.provider.transaction.TransferService;
import io.nuls.base.api.provider.transaction.facade.TransferReq;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.info.NoUse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author: zhoulijun
 * @Time: 2020/7/2 12:04
 * @Description: 空投转账
 */
public class SendNvt extends Base {

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

    static String password = "nuls123456";

    static void send(List<Item> to,String fromAddress){
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
                new TransferReq.TransferReqBuilder(CHAIN_ID, NVT_ASSET_ID)
                        .addForm(NVT_CHAIN_ID, NVT_ASSET_ID, fromAddress, password, fromAmount);
        to.forEach(toAddress->{
            if(toAddress.getAddress().equals(fromAddress)){
                return ;
            }
            builder.addTo(NVT_CHAIN_ID, NVT_ASSET_ID, toAddress.getAddress(), toAddress.getAmount());
        });
        Result<String> result = transferService.transfer(builder.build(new TransferReq()));
        if(result.isFailed()){
            Log.error("转账失败,原因:{}",result.getMessage());
            Log.error("{}",result);
            Log.error("失败地址列表:\n{}",buf.toString());
            System.exit(0);
        }
        Log.info("转账成功:hash:{}\n{}",result.getData(),buf.toString());
    }

    static List<Item> read(File file) throws IOException {
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

    static void sendNvtForPocm() throws IOException {
        List<Item> list = read(POCM).stream().map(d-> new Item(d.getAddress(),d.getAmount().divide(BigInteger.valueOf(10)))).collect(Collectors.toList());
        sendNVT(list,NVT_FROM_ADDRESS_FOR_NULS);
    }

    static void sendNvtForNuls() throws IOException {
        List<Item> list = read(NULS).stream().map(d-> new Item(d.getAddress(),d.getAmount().divide(BigInteger.valueOf(10)))).collect(Collectors.toList());
        sendNVT(list,NVT_FROM_ADDRESS_FOR_NULS);
    }

    static void sendNvtForNRC20() throws IOException {
        sendNVT(read(NRC20),NVT_FROM_ADDRESS_FOR_NRC20);
    }

    static void sendNVT(List<Item> list,String fromAddress) {
        int index=0;
        int toSize=500;
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
            send(temp,fromAddress);
            try {
                TimeUnit.MILLISECONDS.sleep(2L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }



    public static void main(String[] args) throws Exception {

        ServiceManager.init(CHAIN_ID, Provider.ProviderType.RPC);
        NoUse.mockModule(7771);
        LedgerProvider ledgerProvider = ServiceManager.get(LedgerProvider.class);
        List<Item> list = read(NULS);
        BigInteger total = list.stream().map(d->{
            GetBalanceReq req = new GetBalanceReq(1,9,d.getAddress());
            Result<AccountBalanceInfo> res = ledgerProvider.getBalance(req);
            Log.info("{}",res.getData());
            return res.getData().getTotal();
        }).reduce(BigInteger::add).orElse(BigInteger.ZERO);
        Log.info("total : {}",total);
//        //向NRC20持币地址转账
//        sendNvtForNRC20();
//        TimeUnit.SECONDS.sleep(20);
//        //向NULS持币地址空投
//        sendNvtForNuls();
//        //向pocm委托地址空投
////        sendNvtForPocm();
//        System.exit(0);
    }

}

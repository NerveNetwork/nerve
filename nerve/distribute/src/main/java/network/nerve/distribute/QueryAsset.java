package network.nerve.distribute;


import io.nuls.base.basic.AddressTool;
import io.nuls.core.log.Log;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @Author: zhoulijun
 * @Time: 2020/7/1 14:48
 * @Description: 快照数据
 * 准备快照
 */
public class QueryAsset extends Base {

    static void readPocmDeposit(Consumer<String> call) {
        Object res = get(POCM_URL);
        List<Map<String,Object>> pocmDeposit = (List<Map<String, Object>>) res;
        pocmDeposit.stream().sorted((o1,o2)->{
            BigInteger b1 = new BigInteger(o1.get("totalDepositAmount").toString());
            BigInteger b2 = new BigInteger(o2.get("totalDepositAmount").toString());
            return b1.negate().compareTo(b2.negate());
        }).forEach(account->{
            BigInteger amount = new BigInteger(account.get("totalDepositAmount").toString());
            String address = account.get("depositAddress").toString();
            Log.info("{}:{}",address,amount);
            if(amount.compareTo(MIN) == -1){
                return ;
            }
            if(EXCLUSION.contains(address)){
                return ;
            }
            call.accept(address + ":" + amount);
        });
    }

    static void readPocmDepositToFile() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(POCM));
        readPocmDeposit(d->{
            try {
                writer.write(d);
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        writer.close();
    }

    static BigDecimal calcPocmDeposit(){
        List<BigDecimal> balanceiList = new ArrayList<>();
        readPocmDeposit(d->{
            BigDecimal balance = new BigDecimal(d.split(":")[1]);
            balanceiList.add(balance);
        });
        BigDecimal pocmDepositTotal = balanceiList.stream().reduce(BigDecimal::add).orElse(BigDecimal.ZERO).movePointLeft(8);
        BigDecimal nvtTotal = pocmDepositTotal.movePointLeft(1);
        Log.info("POCM deposit total : {}",pocmDepositTotal);
        Log.info("NVT TOTAL : {}",nvtTotal);
        return pocmDepositTotal;
    }

    static void readNrc20(Consumer<String> call) {
        Object res = post(QUERY_PS_URL,toMap("getNRC20Snapshot",List.of(CHAIN_ID,NVT_NRC20)));
        List<Map<String,Object>> nrc20Account = (List<Map<String, Object>>) res;
        nrc20Account.stream().sorted((o1,o2)->{
            BigInteger b1 = new BigInteger(o1.get("balance").toString());
            BigInteger b2 = new BigInteger(o2.get("balance").toString());
            return b1.negate().compareTo(b2.negate());
        }).forEach(account->{
            String address = account.get("address").toString();
            BigInteger balance = new BigInteger(account.get("balance").toString());
            if(AddressTool.validContractAddress(AddressTool.getAddress(address),CHAIN_ID)){
                Log.warn("{}是一个智能合约地址，放弃转账",address);
                return ;
            }
            if(balance.compareTo(BigInteger.ZERO) <= 0){
                return ;
            }
            Log.info("{}:{}",address,balance);
            call.accept(address + ":" + balance);
        });
    }

    static void readRrc20ToFile() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(NRC20));
        readNrc20(d->{
            try {
                writer.write(d);
                writer.newLine();
            } catch (IOException e) {
                Log.error("写入文件失败");
                System.exit(0);
            }
        });
        writer.close();
    }


    static BigDecimal calcNRC20Total() {
        List<BigDecimal> balanceiList = new ArrayList<>();
        readNrc20(d->{
            BigDecimal balance = new BigDecimal(d.split(":")[1]);
            balanceiList.add(balance);
        });
        BigDecimal nrc20Total = balanceiList.stream().reduce(BigDecimal::add).orElse(BigDecimal.ZERO).movePointLeft(8);
        Log.info("nvt nrc20 total : {}",nrc20Total);
        return nrc20Total;
    }

    static void readNulsToFile() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(NULS));
        readNuls(d->{
            try {
                writer.write(d);
                writer.newLine();
            } catch (IOException e) {

            }
        });
        writer.close();
    }

    static void readNuls(Consumer<String> call) {
        int index = 1;
        while(true){
            try{
                if(!readNuls(index,call)){
                    break;
                }
                index++;
            }catch (Exception e){
                Log.error(e.getMessage());
            }
        }
    }

    private static boolean readNuls(int pageIndex, Consumer<String> call) {
        Object res = post(QUERY_PS_URL,toMap("getCoinRanking",List.of(CHAIN_ID,pageIndex,50)));
        List<Map<String,Object>> accountList = (List<Map<String, Object>>) (((Map<String, Object>) res).get("list"));
        if(accountList.isEmpty()){
            return false;
        }
        for (Map<String,Object> account : accountList){
            String address = (String) account.get("address");
            BigInteger balance = new BigInteger(account.get("totalBalance").toString());
            if(balance.compareTo(MIN) < 0){
                return false;
            }
            if(AddressTool.validContractAddress(AddressTool.getAddress(address),CHAIN_ID)){
                continue;
            }
            if(EXCLUSION.contains(address)){
                continue ;
            }
            call.accept(address + ":" + balance);
        }
        Log.info("page index : {} done",pageIndex);
        return true;
    }

    static BigDecimal calcNulsTotal(){
        List<BigDecimal> balanceiList = new ArrayList<>();
        readNuls(d->{
            BigDecimal balance = new BigDecimal(d.split(":")[1]);
            balanceiList.add(balance);
        });
        BigDecimal nulsTotal = balanceiList.stream().reduce(BigDecimal::add).orElse(BigDecimal.ZERO).movePointLeft(8);
        BigDecimal nvtTotal = nulsTotal.movePointLeft(1);
        Log.info("NULS TOTAL : {}",nulsTotal);
        Log.info("NVT TOTAL : {}",nvtTotal);
        return nulsTotal;
    }

    static BigDecimal readTotalByFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        BigDecimal total = BigDecimal.ZERO;
        while(line != null) {
            total = total.add( new BigDecimal(line.split(":")[1]));
            line = reader.readLine();
        }
        reader.close();
        return total.movePointLeft(8);
    }

    public static void main(String[] args) throws Exception {
        //将资产数据读取到文件中
        readRrc20ToFile();
        readNulsToFile();
//        readPocmDepositToFile();
        //从文件数据中计算总数
        BigDecimal nrc20TotalForFile = readTotalByFile(NRC20);
//        BigDecimal pocmTotalForFile =  readTotalByFile(POCM);
        BigDecimal nulsTotalForFile =  readTotalByFile(NULS);
        //====================================
        //重新从节点中查询总数与文件数据的总数进行比对验证
        Log.info("=".repeat(100));
        BigDecimal nrc20Total = calcNRC20Total();
        if(nrc20Total.compareTo(nrc20TotalForFile) != 0 ){
            Log.error("nrc20的数量不一致");
            System.exit(0);
        }
        Log.info("=".repeat(100));
        BigDecimal pocmTotal = BigDecimal.ZERO;
//        BigDecimal pocmTotal = calcPocmDeposit();
//        if(pocmTotal.compareTo(pocmTotalForFile) != 0){
//            Log.error("pocm委托的数量不一致");
//            System.exit(0);
//        }
        Log.info("=".repeat(100));
        BigDecimal nulsTotal = calcNulsTotal();
        if(nulsTotal.compareTo(nulsTotalForFile) != 0){
            Log.error("nuls资产列表不一致");
            System.exit(0);
        }
        Log.info("=".repeat(100));
        Log.info("空投总需要NVT数量:{}",pocmTotal.add(nulsTotal).movePointLeft(1));
    }

}

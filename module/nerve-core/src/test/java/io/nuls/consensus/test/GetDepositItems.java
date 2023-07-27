package io.nuls.consensus.test;

import io.nuls.core.exception.NulsException;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.consensus.model.po.DepositPo;

import java.util.List;

public class GetDepositItems {
    public static final String path = "/Users/niels/workspace/nerve-network/data/consensus";

    public static void main(String[] args) throws NulsException {
        RocksDBService.init(path);
        List<byte[]> list = RocksDBService.valueList("deposit_9");
        int count = 0;
        for (byte[] val : list) {
            DepositPo po = new DepositPo();
            po.parse(val, 0);
            int decimals = 8;
            if(po.getDelHeight()<0) {
                count ++;
                System.out.println("list.add(\""+po.getTxHash().toHex() +"\");");
//                System.out.println(po.getTxHash().toHex() + " , " + AddressTool.getStringAddressByBytes(po.getAddress()) + "(" + po.getAssetChainId() + "-" + po.getAssetId() + ") , " + DoubleUtils.getRoundStr(new BigDecimal(po.getDeposit(), decimals).doubleValue()));
            }
        }
        System.out.println("listSize : total-" + list.size()+" , active-"+count);
    }
}

package io.nuls.test.storage;

import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.crosschain.base.model.bo.txdata.RegisteredChainMessage;
import network.nerve.constant.NulsCrossChainConstant;

import java.util.List;

/**
 * @Author: zhoulijun
 * @Time: 2020/9/21 16:25
 * @Description: 功能描述
 */
public class RegisteredCrossChainTest {

    public static void main(String[] args) {
        RocksDBService.init("/Users/zhoulijun/workspace/nuls/nerve-network-package/cross-chain");
        List<byte[]> list = RocksDBService.valueList(NulsCrossChainConstant.DB_NAME_REGISTERED_CHAIN);
        list.stream().forEach(d->{
            RegisteredChainMessage rcm = new RegisteredChainMessage();
            try {
                rcm.parse(d,0);
                rcm.getChainInfoList().forEach(chain->{
                    Log.info("{}",chain);
                });
            } catch (NulsException e) {
                e.printStackTrace();
            }

        });
    }

}

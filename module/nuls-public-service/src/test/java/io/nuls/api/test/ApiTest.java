package io.nuls.api.test;

import io.nuls.api.ApiContext;
import io.nuls.api.analysis.WalletRpcHandler;
import io.nuls.api.cache.ApiCache;
import io.nuls.api.manager.CacheManager;
import io.nuls.api.model.po.ContractInfo;
import io.nuls.api.model.po.ContractResultInfo;
import io.nuls.api.model.po.CurrentRound;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Address;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.DoubleUtils;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rpc.info.HostInfo;
import io.nuls.core.rpc.info.NoUse;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.core.basic.Result;
import io.nuls.core.exception.NulsException;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static io.nuls.api.constant.DBTableConstant.TX_RELATION_SHARDING_COUNT;

public class ApiTest {

//    protected Chain chain;
//    protected static int chainId = 2;
//    protected static int assetId = 1;

    @Before
    public void before() throws Exception {
//        NoUse.mockModule();
//        ResponseMessageProcessor.syncKernel("ws://" + HostInfo.getLocalIP() + ":7771");
//        chain = new Chain();
//        chain.setConfig(new ConfigBean(chainId, assetId, 100000000L));
    }

    @Test
    public void testCmdCall() {
//
//        for (int i = 0; i < 10000; i++) {
//            BlockInfo block = WalletRpcHandler.getBlockInfo(2, i);
//            for (TransactionInfo tx : block.getTxList()) {
//                if (tx.getType() == 1) {
//
//                }
//            }
//        }
    }

    @Before
    public void initApiCache() {
        ApiCache apiCache = new ApiCache();
        CurrentRound currentRound = new CurrentRound();
        currentRound.setStartHeight(111);
        currentRound.setEndHeight(222);
        apiCache.setCurrentRound(currentRound);

        CacheManager.addApiCache(2, apiCache);
    }


    @Test
    public void updateCurrentRound() {
        ApiCache apiCache = CacheManager.getCache(2);
        CurrentRound currentRound = apiCache.getCurrentRound();
        System.out.println(currentRound.getStartHeight() + "----" + currentRound.getEndHeight());
        CurrentRound beforeRound = new CurrentRound();
        beforeRound.setStartHeight(3333);
        beforeRound.setEndHeight(4444);

        apiCache.setCurrentRound(beforeRound);
        System.out.println(apiCache.getCurrentRound().getStartHeight() + "----" + apiCache.getCurrentRound().getEndHeight());

        testUpdateCurrentRound(currentRound);

        System.out.println(currentRound.getStartHeight() + "----" + currentRound.getEndHeight());
    }

    private void testUpdateCurrentRound(CurrentRound currentRound) {
        currentRound.setStartHeight(7777);
        currentRound.setEndHeight(8888);
    }




    @Test
    public void test() {
        Set<String> set = new HashSet<>();
        set.add("aaaa1");
        set.add("aaaa11");
        set.add("aaaa112");
        set.add("aaaa13");
        set.add("aaaa14");
        set.add("aaaa15");
        set.add("aaaa16");
        set.add("aaaa17");
        set.add("aaaa18");
        set.add("aaaa19");
        set.add("aaaa10");

        int i = 0;
        for (String key : set) {
            System.out.println(key);
            i++;
            if (i == 6) {
                break;
            }
        }

    }

    public static void main(String[] args) {
//        ECKey key = ECKey.fromPrivate(HexUtil.decode(""));
//        System.out.println(key.getPublicKeyAsHex());
//        Address address = new Address(2, "tNULS", BaseConstant.DEFAULT_ADDRESS_TYPE, SerializeUtils.sha256hash160(key.getPubKey()));
//        System.out.println("=".repeat(100));
//        System.out.println("address   :" + AddressTool.getStringAddressByBytes(address.getAddressBytes(), address.getPrefix()));

        System.out.println(Math.abs("tNULSeBaMgZGCXZhauCZpPcxtqZmQNNdNg9X8W".hashCode()) % TX_RELATION_SHARDING_COUNT);
    }

}

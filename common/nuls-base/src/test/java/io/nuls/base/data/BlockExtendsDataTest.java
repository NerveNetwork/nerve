package io.nuls.base.data;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import org.junit.Test;

import java.io.IOException;

/**
 * testextendfield
 *
 * @author captain
 * @version 1.0
 * @date 2019/6/21 11:13
 */
public class BlockExtendsDataTest {

    /**
     * Test Extension Fieldshex
     *
     * @throws NulsException
     * @throws IOException
     */
    @Test
    public void test() throws NulsException, IOException {
        String string = "010000000100010000000100010001003c64002056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421";
        BlockExtendsData data = new BlockExtendsData();
        data.parse(new NulsByteBuffer(HexUtil.decode(string)));
        System.out.println(data.getMainVersion());
        System.out.println(data.getBlockVersion());
        System.out.println(data.getEffectiveRatio());
        System.out.println(data.getContinuousIntervalCount());
        System.out.println(data.getConsensusMemberCount());
        System.out.println(data.getRoundIndex());
        System.out.println(data.getRoundStartTime());
        System.out.println(data.getPackingIndexOfRound());
        System.out.println(HexUtil.encode(data.getStateRoot()));
    }

    /**
     * Set the value of the extended field and return ithex, usually used to set up Genesis blocks
     *
     * @throws NulsException
     * @throws IOException
     */
    @Test
    public void test1() throws NulsException, IOException {
        BlockExtendsData data = new BlockExtendsData();
        data.setConsensusMemberCount(1);
        data.setPackingIndexOfRound(1);
        data.setRoundIndex(1);
        data.setRoundStartTime(1594278000L);
        data.setMainVersion((short) 1);
        data.setBlockVersion((short) 1);
        data.setEffectiveRatio((byte) 90);
        data.setContinuousIntervalCount((short) 100);
        data.setStateRoot(HexUtil.decode("56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"));
        System.out.println(HexUtil.encode(data.serialize()));
    }

    @Test
    public void test2() {
//        ,
//        {
//            "address": "NERVEepb6FhfgWLyqHUQgHoHwRGuw3huvchBus",
//                "amount": 2000000000000000,
//                "lockTime": 1628524800
//        }
        long start = 1628524800L;
        StringBuilder ss = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            ss.append(",{\"address\":\"NERVEepb6FhfgWLyqHUQgHoHwRGuw3huvchBus\",\"amount\": 2000000000000000,\"lockTime\":");
            long lockTime = start+i*30*24*60*60;
            ss.append(lockTime);
            ss.append("}");
        }
        System.out.println(ss.toString());
    }
}

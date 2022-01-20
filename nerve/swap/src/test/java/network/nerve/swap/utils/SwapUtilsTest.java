package network.nerve.swap.utils;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import network.nerve.swap.JunitUtils;
import network.nerve.swap.JunitCase;
import network.nerve.swap.JunitExecuter;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.txdata.linkswap.StableLpSwapTradeData;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Niels
 */
public class SwapUtilsTest {

    @Test
    public void getStringPairAddress() {
        List<JunitCase> items = new ArrayList<>();
        items.add(new JunitCase("case0", null, new Object[]{9, new NerveToken(1, 1), new NerveToken(9, 1)}, "NERVEepb7JCJ3LUjfDw2aM61HZcSs1zEoA4ars", false, null, NerveCallback.NULL_CALLBACK));
        items.add(new JunitCase("case1", null, new Object[]{9, new NerveToken(9, 1), new NerveToken(1, 1)}, "NERVEepb7JCJ3LUjfDw2aM61HZcSs1zEoA4ars", false, null, NerveCallback.NULL_CALLBACK));
        JunitExecuter executer = new JunitExecuter() {

            @Override
            public Object execute(JunitCase testCase) {
                return SwapUtils.getStringPairAddress(9, (NerveToken) testCase.getParams()[1], (NerveToken) testCase.getParams()[2]);
            }
        };
        JunitUtils.execute(items, executer);
    }

    @Test
    public void tokenSort() {
        List<JunitCase> items = new ArrayList<>();
        items.add(new JunitCase("测试空2", null, new Object[]{null, new NerveToken(1, 1)}, null, true, NulsRuntimeException.class, NerveCallback.NULL_CALLBACK));
        items.add(new JunitCase("测试空1", null, new Object[]{new NerveToken(1, 1), null}, null, true, NulsRuntimeException.class, NerveCallback.NULL_CALLBACK));
        items.add(new JunitCase("测试空3", null, new Object[]{null, null}, null, true, NulsRuntimeException.class, NerveCallback.NULL_CALLBACK));

        items.add(new JunitCase("测试相同token", null, new Object[]{new NerveToken(1, 1), new NerveToken(1, 1)}, null, true, NulsRuntimeException.class, NerveCallback.NULL_CALLBACK));

        items.add(new JunitCase("测试正常数据1", null, new Object[]{new NerveToken(9, 1), new NerveToken(1, 1)}, new NerveToken[]{new NerveToken(1, 1), new NerveToken(9, 1)}, false, null, NerveCallback.NULL_CALLBACK));
        items.add(new JunitCase("测试正常数据2", null, new Object[]{new NerveToken(9, 1), new NerveToken(1, 2)}, new NerveToken[]{new NerveToken(1, 2), new NerveToken(9, 1)}, false, null, NerveCallback.NULL_CALLBACK));
        items.add(new JunitCase("测试正常数据3", null, new Object[]{new NerveToken(9, 1), new NerveToken(9, 2)}, new NerveToken[]{new NerveToken(9, 1), new NerveToken(9, 2)}, false, null, NerveCallback.NULL_CALLBACK));

        JunitExecuter executer = new JunitExecuter() {

            @Override
            public Object execute(JunitCase testCase) {
                return SwapUtils.tokenSort((NerveToken) testCase.getParams()[0], (NerveToken) testCase.getParams()[1]);
            }
        };

        JunitUtils.execute(items, executer);

    }

    @Test
    public void quote() {
        System.out.println(SwapUtils.quote(
                BigInteger.valueOf(596),
                BigInteger.valueOf(2222),
                BigInteger.valueOf(3355)
                ));
    }

    @Test
    public void getAmountIn() {
        System.out.println(SwapUtils.getAmountIn(
                BigInteger.valueOf(7403190),
                BigInteger.valueOf(171132069136L),
                BigInteger.valueOf(16887743)
                ));
    }

    @Test
    public void calcStablePairAddress() {
        NulsHash txHash = NulsHash.fromHex("f3fea55eebb6d0b262a30504de69833ff9dacf6253a31a3ec0bca776b6c7b33c");
        byte[] stablePairAddressBytes = AddressTool.getAddress(txHash.getBytes(), 5, SwapConstant.STABLE_PAIR_ADDRESS_TYPE);
        String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);
        System.out.println(stablePairAddress);
    }

    @Test
    public void calcPairAddress() {
        System.out.println(AddressTool.getStringAddressByBytes(
                SwapUtils.getPairAddress(
                        5,
                        new NerveToken(5, 1),
                        new NerveToken(5, 3)
                )));
        System.out.println(AddressTool.getStringAddressByBytes(
                SwapUtils.getPairAddress(
                        5,
                        new NerveToken(5, 1),
                        new NerveToken(5, 4)
                )));
        System.out.println(AddressTool.getStringAddressByBytes(
                SwapUtils.getPairAddress(
                        5,
                        new NerveToken(5, 1),
                        new NerveToken(5, 5)
                )));
    }

    @Test
    public void test() throws Exception {
        String data = "0000000000000000000000000000000000000000000000000000000000000000050001b9978dbea4abebb613b6be2d0d66b4179d2511cb00faefdb6103050048000500660005000100";
        StableLpSwapTradeData txData = new StableLpSwapTradeData();
        txData.parse(HexUtil.decode(data), 0);
        System.out.println();
    }
}
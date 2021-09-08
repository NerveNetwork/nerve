package network.nerve.swap.utils;

import io.nuls.core.exception.NulsRuntimeException;
import network.nerve.swap.JunitUtils;
import network.nerve.swap.JunitCase;
import network.nerve.swap.JunitExecuter;
import network.nerve.swap.model.NerveToken;
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
}
package network.nerve.swap.utils;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.parse.JSONUtils;
import network.nerve.swap.JunitUtils;
import network.nerve.swap.JunitCase;
import network.nerve.swap.JunitExecuter;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.business.AddLiquidityBus;
import network.nerve.swap.model.business.RemoveLiquidityBus;
import network.nerve.swap.model.business.SwapTradeBus;
import network.nerve.swap.model.business.linkswap.StableLpSwapTradeBus;
import network.nerve.swap.model.business.linkswap.SwapTradeStableRemoveLpBus;
import network.nerve.swap.model.business.stable.StableAddLiquidityBus;
import network.nerve.swap.model.business.stable.StableRemoveLiquidityBus;
import network.nerve.swap.model.business.stable.StableSwapTradeBus;
import network.nerve.swap.model.txdata.linkswap.StableLpSwapTradeData;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;

import static network.nerve.swap.constant.SwapConstant.BI_3;

/**
 * @author Niels
 */
public class SwapUtilsTest {

    @Test
    public void stableAddressTest() {
        NulsHash txHash = NulsHash.fromHex("fd2506674f788829d2c03409f7e72c307374ca688e87444ea041624e8b96be82");
        byte[] stablePairAddressBytes = AddressTool.getAddress(txHash.getBytes(), 9, SwapConstant.STABLE_PAIR_ADDRESS_TYPE);
        String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);
        System.out.println(stablePairAddress);
    }

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
        items.add(new JunitCase("Test Empty2", null, new Object[]{null, new NerveToken(1, 1)}, null, true, NulsRuntimeException.class, NerveCallback.NULL_CALLBACK));
        items.add(new JunitCase("Test Empty1", null, new Object[]{new NerveToken(1, 1), null}, null, true, NulsRuntimeException.class, NerveCallback.NULL_CALLBACK));
        items.add(new JunitCase("Test Empty3", null, new Object[]{null, null}, null, true, NulsRuntimeException.class, NerveCallback.NULL_CALLBACK));

        items.add(new JunitCase("Test Sametoken", null, new Object[]{new NerveToken(1, 1), new NerveToken(1, 1)}, null, true, NulsRuntimeException.class, NerveCallback.NULL_CALLBACK));

        items.add(new JunitCase("Test normal data1", null, new Object[]{new NerveToken(9, 1), new NerveToken(1, 1)}, new NerveToken[]{new NerveToken(1, 1), new NerveToken(9, 1)}, false, null, NerveCallback.NULL_CALLBACK));
        items.add(new JunitCase("Test normal data2", null, new Object[]{new NerveToken(9, 1), new NerveToken(1, 2)}, new NerveToken[]{new NerveToken(1, 2), new NerveToken(9, 1)}, false, null, NerveCallback.NULL_CALLBACK));
        items.add(new JunitCase("Test normal data3", null, new Object[]{new NerveToken(9, 1), new NerveToken(9, 2)}, new NerveToken[]{new NerveToken(9, 1), new NerveToken(9, 2)}, false, null, NerveCallback.NULL_CALLBACK));

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
                BigInteger.valueOf(16887743),
                BI_3
                ));
    }

    @Test
    public void calcStablePairAddress() {
        NulsHash txHash = NulsHash.fromHex("cd7e092ec37d4b0cacdd64a0ddb1e15702e4e36059620b954b5c76b45c239de7");
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

    @Test
    public void getResult() throws Exception {
        //Object bus = this.desBusStr(TxType.SWAP_TRADE_STABLE_COIN, "0bfa073b6e6574776f726b2e6e657276652e737761702e6d6f64656c2e627573696e6573732e737461626c652e537461626c655377617054726164654275730889dcb70d109f89d793061a170900068c5f7a41f250ffa9de0a51c46ab7d3083b9ae3f523080b1201001209056bc75e2d631000001209fa9438a1d29cf00000120100120100120100120100120100120100120100120100242b080b12050145b85b6c120a1cae33a7773946f756af120a02b522e71b2378845115120a03dc0aa78dcd6f2d30001205057c6f7bf2120100120100120505a7282b5e120400b653e81201001201002c33080b1201001209056bc75e2d63100000120100120100120100120100120100120100120100120100120100343b080b1201001201001201001201001201001201001201001201001201001201001201003c40024a09056bc75e2d6310000052170900010a0d74699e06a292a77fa620ec24ef0bdc8b24060c");
        //System.out.println(bus != null ? bus.toString() : "");
        //bus = this.desBusStr(TxType.SWAP_TRADE_STABLE_COIN,        "0bfa073b6e6574776f726b2e6e657276652e737761702e6d6f64656c2e627573696e6573732e737461626c652e537461626c65537761705472616465427573088adcb70d10a689d793061a170900068c5f7a41f250ffa9de0a51c46ab7d3083b9ae3f523080b1201001209056bc75e2d631000001209fa9438a1d29cf00000120100120100120100120100120100120100120100120100242b080b12050145b85b6c120a1ca8c7e0190be3e756af120a02ba8eae7950db945115120a03dc0aa78dcd6f2d30001205057c6f7bf2120100120100120505a7282b5e120400b653e81201001201002c33080b1201001209056bc75e2d63100000120100120100120100120100120100120100120100120100120100343b080b1201001201001201001201001201001201001201001201001201001201001201003c40024a09056bc75e2d6310000052170900010a0d74699e06a292a77fa620ec24ef0bdc8b24060c");
        //System.out.println(bus != null ? bus.toString() : "");
        Object bus = this.desBusStr(TxType.SWAP_TRADE,        "0bfa072e6e6574776f726b2e6e657276652e737761702e6d6f64656c2e627573696e6573732e5377617054726164654275730b08d1aa861b10c8b2ceaf061a1709000407020a20ad4566e41897e2acd9139dfb17e310ff22061494f6d9f7712a0c1cc943a93cd61f7a3144b6533206149b044b74e13a0c1cc0cabb7193f9ead5379985430801109201444a0b087b19987783a4c5b88fa2520a022bcd355e1569ab72d45b080110015c6205060d717d706a170900048c71e3f994274a36ef16ad19b2ee361b1cbf7c000c0b08a8ad861b10fbb7ceaf061a170900048c71e3f994274a36ef16ad19b2ee361b1cbf7c0022061bd182df19c82a0a19aa2228769c07bce7a832061bcb76fa44a93a0a19afb48f3bb0d409caf94308011001444a05060d717d705204018ca8515b080910dc015c6209059266c514cc4ce3516a170900016baa485461f883cd40ec2d7ebd49f82ae77c03580c0c");
        System.out.println(bus != null ? bus.toString() : "");
        SwapTradeBus swapTradeBus = (SwapTradeBus) bus;
        BigInteger amountOut = swapTradeBus.getTradePairBuses().get(swapTradeBus.getTradePairBuses().size() - 1).getAmountOut();
    }

    @Test
    public void indexesTest() throws NulsException {
        byte[] indexes = new byte[]{6,7,0,3,4,5};
        int lengthOfCoins = 8;
        int lengthOfIndexes = indexes.length;

        byte[] _indexes = new byte[lengthOfCoins];
        byte[] temp = new byte[lengthOfCoins];
        for (int i = 0; i < lengthOfIndexes; i++) {
            byte index = indexes[i];
            if (index > lengthOfCoins - 1) {
                throw new NulsException(SwapErrorCode.INVALID_COINS);
            }
            temp[index] = 1;
            _indexes[i] = index;
        }
        int k = 0;
        for (int i = 0; i < lengthOfCoins; i++) {
            if (temp[i] == 1) {
                continue;
            }
            _indexes[k++ + lengthOfIndexes] = (byte) i;
        }
        indexes = _indexes;
        System.out.println(Arrays.toString(indexes));
    }

    protected static Map<Integer, Class> busClassMap = new HashMap<>();

    static {
        busClassMap.put(TxType.SWAP_ADD_LIQUIDITY, AddLiquidityBus.class);
        busClassMap.put(TxType.SWAP_REMOVE_LIQUIDITY, RemoveLiquidityBus.class);
        busClassMap.put(TxType.SWAP_TRADE, SwapTradeBus.class);
        busClassMap.put(TxType.SWAP_ADD_LIQUIDITY_STABLE_COIN, StableAddLiquidityBus.class);
        busClassMap.put(TxType.SWAP_REMOVE_LIQUIDITY_STABLE_COIN, StableRemoveLiquidityBus.class);
        busClassMap.put(TxType.SWAP_TRADE_STABLE_COIN, StableSwapTradeBus.class);
        busClassMap.put(TxType.SWAP_STABLE_LP_SWAP_TRADE, StableLpSwapTradeBus.class);
        busClassMap.put(TxType.SWAP_TRADE_SWAP_STABLE_REMOVE_LP, SwapTradeStableRemoveLpBus.class);
    }

    protected Object desBusStr(Object txType, Object busStr) {
        if (txType == null || busStr == null) {
            return null;
        }
        Class aClass = busClassMap.get(Integer.parseInt(txType.toString()));
        if (aClass == null) {
            return null;
        }
        System.out.println(aClass.getSimpleName());
        return SwapDBUtil.getModel(HexUtil.decode(busStr.toString()), aClass);
    }
}

package network.nerve.pocbft.constant;

import network.nerve.pocbft.model.bo.Chain;

/**
 * @author Niels
 */
public class PocbftConstant {

    /**
     * unit:round of consensus
     * 用于计算信誉值（表示只用最近这么多轮的轮次信息来计算信誉值）
     */
    public static int getRANGE_OF_CAPACITY_COEFFICIENT(Chain chain) {
        if (chain.getBestHeader().getHeight() > chain.getConfig().getV1_6_0Height()) {
            return 2000;
        }
        return 1000;
    }
}

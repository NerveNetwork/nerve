package io.nuls.consensus.constant;

import io.nuls.consensus.model.bo.Chain;

/**
 * @author Niels
 */
public class PocbftConstant {

    public static long VERSION_1_19_0_HEIGHT = Long.MAX_VALUE;

    /**
     * unit:round of consensus
     * Used to calculate reputation value（Indicates that only the most recent round information is used to calculate the reputation value）
     */
    public static int getRANGE_OF_CAPACITY_COEFFICIENT(Chain chain) {
        if (chain.getBestHeader().getHeight() > chain.getConfig().getV1_6_0Height()) {
            return 2000;
        }
        return 1000;
    }
}

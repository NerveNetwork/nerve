package io.nuls.crosschain.srorage;

import java.math.BigInteger;

/**
 * Amount management for transferring out of the external chain
 */
public interface TotalOutAmountService {
    /**
     * Add transfer out data
     *
     * @param chainId
     * @param assetId
     * @param amount
     * @return
     */
    boolean addOutAmount(int chainId, int assetId, BigInteger amount);

    /**
     * Returning original assets
     *
     * @param chainId
     * @param assetId
     * @param amount
     * @return
     */
    boolean addBackAmount(int chainId, int assetId, BigInteger amount);

    /**
     * Obtain the total number of transfers out for a certain asset
     * @param chainId
     * @param assetId
     * @return
     */
    BigInteger getOutTotalAmount(int chainId, int assetId);
}

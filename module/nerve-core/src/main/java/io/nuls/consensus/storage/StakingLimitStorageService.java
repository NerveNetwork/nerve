package io.nuls.consensus.storage;

import io.nuls.consensus.model.bo.Chain;

import java.math.BigDecimal;

/**
 * @author Niels
 */
public interface StakingLimitStorageService {
    /**
     * accumulationstakingmoney
     *
     * @param key
     * @param Amount
     * @return
     */
    boolean addStaking(Chain chain, String key, BigDecimal Amount);

    /**
     * reducestakingmoney
     *
     * @param key
     * @param Amount
     * @return
     */
    boolean exitStaking(Chain chain, String key, BigDecimal Amount);

    /**
     * Obtain the number of available participants
     *
     * @param key
     * @return
     */
    BigDecimal getStakingAmount(Chain chain, String key);
}

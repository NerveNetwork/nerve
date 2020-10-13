package network.nerve.pocbft.storage;

import network.nerve.pocbft.model.bo.Chain;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Niels
 */
public interface StakingLimitStorageService {
    /**
     * 累加staking金额
     *
     * @param key
     * @param Amount
     * @return
     */
    boolean addStaking(Chain chain, String key, BigDecimal Amount);

    /**
     * 减少staking金额
     *
     * @param key
     * @param Amount
     * @return
     */
    boolean exitStaking(Chain chain, String key, BigDecimal Amount);

    /**
     * 获取可以参与的数量
     *
     * @param key
     * @return
     */
    BigDecimal getStakingAmount(Chain chain, String key);
}

package network.nerve.srorage;

import network.nerve.model.bo.config.ConfigBean;

import java.math.BigInteger;
import java.util.Map;

/**
 * 转出外链的金额管理
 */
public interface TotalOutAmountService {
    /**
     * 增加转出数据
     *
     * @param chainId
     * @param assetId
     * @param amount
     * @return
     */
    boolean addOutAmount(int chainId, int assetId, BigInteger amount);

    /**
     * 转回本来的资产
     *
     * @param chainId
     * @param assetId
     * @param amount
     * @return
     */
    boolean addBackAmount(int chainId, int assetId, BigInteger amount);

    /**
     * 得到某个资产的总的转出数量
     * @param chainId
     * @param assetId
     * @return
     */
    BigInteger getOutTotalAmount(int chainId, int assetId);
}

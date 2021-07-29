package network.nerve.swap.storage;

import network.nerve.swap.model.po.stable.StableSwapUserLiquidityPo;

/**
 * @author Niels
 */
@Deprecated
public interface SwapStableUserLiquidityStorageService {

    boolean save(byte[] address, StableSwapUserLiquidityPo po);

    StableSwapUserLiquidityPo get(byte[] address);

    boolean delele(byte[] address);

}

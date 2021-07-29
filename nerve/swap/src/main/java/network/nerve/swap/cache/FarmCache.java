package network.nerve.swap.cache;

import io.nuls.base.data.NulsHash;
import network.nerve.swap.model.po.FarmPoolPO;

import java.util.Collection;

/**
 * @author Niels
 */
public interface FarmCache {

    FarmPoolPO get(NulsHash hash);

    FarmPoolPO put(NulsHash hash, FarmPoolPO po);

    FarmPoolPO remove(NulsHash hash);

    Collection<FarmPoolPO> getList();
}

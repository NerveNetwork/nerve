package network.nerve.swap.cache.impl;

import io.nuls.base.data.NulsHash;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.ioc.SpringLiteContext;
import network.nerve.swap.cache.FarmCache;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.storage.FarmStorageService;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 */
@Component
public class FarmCacheImpl implements FarmCache {
    private static Map<NulsHash, FarmPoolPO> FARM_MAP = new HashMap<>();

    public static void init(Chain chain) {
        FarmStorageService service = SpringLiteContext.getBean(FarmStorageService.class);
        init(service.getList(chain.getChainId()));
    }

    protected static void init(List<FarmPoolPO> list) {
        for (FarmPoolPO po : list) {
            FARM_MAP.put(po.getFarmHash(), po);
        }
    }

    @Override
    public FarmPoolPO get(NulsHash hash) {
        return FARM_MAP.get(hash);
    }

    @Override
    public FarmPoolPO put(NulsHash hash, FarmPoolPO po) {
        return FARM_MAP.put(hash, po);
    }

    @Override
    public FarmPoolPO remove(NulsHash hash) {
        return FARM_MAP.remove(hash);
    }

    @Override
    public Collection<FarmPoolPO> getList() {
        return FARM_MAP.values();
    }
}

package network.nerve.swap.manager;

import network.nerve.swap.model.po.FarmPoolPO;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Niels
 */
public class FarmTempManager {
    private final Map<String, FarmPoolPO> poolsMap = new HashMap<>();

    public FarmPoolPO getFarm(String farmHash) {
        return poolsMap.get(farmHash);
    }

    public void putFarm(FarmPoolPO po) {
        poolsMap.put(po.getFarmHash().toHex(), po);
    }
}

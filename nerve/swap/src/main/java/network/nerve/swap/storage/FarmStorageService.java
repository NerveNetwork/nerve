package network.nerve.swap.storage;

import network.nerve.swap.model.po.FarmPoolPO;

import java.util.List;

/**
 * @author Niels
 */
public interface FarmStorageService {

    FarmPoolPO save(int chainId, FarmPoolPO po);

    boolean delete(int chainId, byte[] hash);

    List<FarmPoolPO> getList(int chainId);
}

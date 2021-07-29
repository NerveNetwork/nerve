package network.nerve.swap.storage;

import io.nuls.base.data.NulsHash;
import network.nerve.swap.model.po.FarmUserInfoPO;

/**
 * @author Niels
 */
public interface FarmUserInfoStorageService {

    FarmUserInfoPO save(int chainId,FarmUserInfoPO po);

    boolean delete(int chainId,NulsHash farmHash, byte[] address);

    FarmUserInfoPO load(int chainId,NulsHash farmHash, byte[] address);

    FarmUserInfoPO loadByKey(int chainId,byte[] key);
}

package network.nerve.swap.storage.impl;

import io.nuls.base.data.NulsHash;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.Log;
import io.nuls.core.model.ArraysTool;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.swap.constant.SwapDBConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.model.po.FarmUserInfoPO;
import network.nerve.swap.storage.FarmUserInfoStorageService;
import network.nerve.swap.utils.SwapDBUtil;

/**
 * @author Niels
 */
@Component
public class FarmUserInfoStorageServiceImpl implements FarmUserInfoStorageService {
    @Override
    public FarmUserInfoPO save(int chainId, FarmUserInfoPO po) {
        try {
            boolean b = RocksDBService.put(SwapDBConstant.DB_NAME_FARM_USER + chainId, ArraysTool.concatenate(po.getFarmHash().getBytes(), po.getUserAddress()), SwapDBUtil.getModelSerialize(po));
            if (b) {
                return po;
            }
        } catch (Exception e) {
            Log.error(e.getMessage());
            throw new NulsRuntimeException(SwapErrorCode.DB_SAVE_ERROR);
        }
        return null;
    }

    @Override
    public boolean delete(int chainId, NulsHash farmHash, byte[] address) {
        try {
            return RocksDBService.delete(SwapDBConstant.DB_NAME_FARM_USER + chainId, ArraysTool.concatenate(farmHash.getBytes(), address));
        } catch (Exception e) {
            Log.error(e.getMessage());
            throw new NulsRuntimeException(SwapErrorCode.DB_SAVE_ERROR);
        }
    }

    @Override
    public FarmUserInfoPO load(int chainId, NulsHash farmHash, byte[] address) {
        return loadByKey(chainId, ArraysTool.concatenate(farmHash.getBytes(), address));
    }

    @Override
    public FarmUserInfoPO loadByKey(int chainId, byte[] key) {
        byte[] value = RocksDBService.get(SwapDBConstant.DB_NAME_FARM_USER + chainId, key);
        if (null == value) {
            return null;
        }
        return SwapDBUtil.getModel(value, FarmUserInfoPO.class);
    }
}

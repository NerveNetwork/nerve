package network.nerve.swap.storage.impl;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.Log;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.swap.constant.SwapDBConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.storage.FarmStorageService;
import network.nerve.swap.utils.SwapDBUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Niels
 */
@Component
public class FarmStorageServiceImpl implements FarmStorageService {
    @Override
    public FarmPoolPO save(int chainId, FarmPoolPO po) {
        try {
            boolean b = RocksDBService.put(SwapDBConstant.DB_NAME_FARM + chainId, po.getFarmHash().getBytes(), SwapDBUtil.getModelSerialize(po));
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
    public boolean delete(int chainId, byte[] hash) {
        try {
            return RocksDBService.delete(SwapDBConstant.DB_NAME_FARM + chainId, hash);
        } catch (Exception e) {
            Log.error(e.getMessage());
            throw new NulsRuntimeException(SwapErrorCode.DB_SAVE_ERROR);
        }
    }

    @Override
    public List<FarmPoolPO> getList(int chainId) {

        List<byte[]> list = RocksDBService.valueList(SwapDBConstant.DB_NAME_FARM + chainId);
        List<FarmPoolPO> farmList = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            for (byte[] value : list) {
                FarmPoolPO po = SwapDBUtil.getModel(value, FarmPoolPO.class);
                farmList.add(po);
            }
        }
        return farmList;
    }
}

package network.nerve.pocbft.storage.impl;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.Log;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.util.SerializeUtil;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.constant.ConsensusErrorCode;
import network.nerve.pocbft.storage.StakingLimitStorageService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Niels
 */
@Component
public class StakingLimitStorageServiceImpl implements StakingLimitStorageService {
    private Lock lock = new ReentrantLock();

    @Override
    public boolean addStaking(String key, BigDecimal amount) {
        lock.lock();
        try {
            BigDecimal oldAmount = getAvailableAmount(key);
            BigDecimal newAmount = amount.add(oldAmount);

            try {
                return RocksDBService.put(ConsensusConstant.DB_NAME_STAKING_LIMIT, key.getBytes("UTF-8"), SerializeUtils.bigDecimal2Bytes(newAmount));
            } catch (Exception e) {
                Log.error(e);
                throw new NulsRuntimeException(ConsensusErrorCode.DEPOSIT_ASSET_ERROR);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean exitStaking(String key, BigDecimal amount) {
        lock.lock();
        try {
            BigDecimal oldAmount = getAvailableAmount(key);
            BigDecimal newAmount = oldAmount.subtract(amount);
            try {
                return RocksDBService.put(ConsensusConstant.DB_NAME_STAKING_LIMIT, key.getBytes("UTF-8"), SerializeUtils.bigDecimal2Bytes(newAmount));
            } catch (Exception e) {
                Log.error(e);
                throw new NulsRuntimeException(ConsensusErrorCode.DEPOSIT_ASSET_ERROR);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public BigDecimal getAvailableAmount(String key) {
        if (StringUtils.isBlank(key)) {
            throw new NulsRuntimeException(ConsensusErrorCode.PARAM_ERROR);
        }
        try {
            byte[] bytes = RocksDBService.get(ConsensusConstant.DB_NAME_STAKING_LIMIT, key.getBytes("UTF-8"));
            if (null == bytes) {
                return BigDecimal.ZERO;
            }
            return SerializeUtils.bytes2BigDecimal(bytes);
        } catch (UnsupportedEncodingException e) {
            Log.error(e);
            throw new NulsRuntimeException(ConsensusErrorCode.DEPOSIT_ASSET_ERROR);
        }
    }
}

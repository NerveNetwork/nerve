package io.nuls.crosschain.srorage.imp;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.Log;
import io.nuls.core.model.ArraysTool;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.crosschain.base.constant.CrossChainErrorCode;
import io.nuls.crosschain.constant.NulsCrossChainErrorCode;
import io.nuls.crosschain.srorage.TotalOutAmountService;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static io.nuls.crosschain.constant.NulsCrossChainConstant.DB_NAME_TOTAL_OUT_AMOUNT;

/**
 * Registration of total amount of assets transferred out of the chain
 */
@Component
public class TotalOutAmountServiceImpl implements TotalOutAmountService {

    private Lock lock = new ReentrantLock();

    private Map<String, BigInteger> cacheMap = new HashMap<>();

    @Override
    public boolean addOutAmount(int chainId, int assetId, BigInteger amount) {
        lock.lock();
        try {
            return out(chainId, assetId, amount);
        } catch (Exception e) {
            Log.error(e);
            throw new NulsRuntimeException(NulsCrossChainErrorCode.FILE_OPERATION_FAILD);
        } finally {
            lock.unlock();
        }
    }


    private boolean out(int chainId, int assetId, BigInteger amount) throws Exception {
        if (amount.compareTo(BigInteger.ZERO) < 0) {
            throw new NulsRuntimeException(CrossChainErrorCode.PARAMETER_ERROR);
        }
        byte[] key = getKey(chainId, assetId);
        byte[] value = RocksDBService.get(DB_NAME_TOTAL_OUT_AMOUNT, key);
        BigInteger out = BigInteger.ZERO;
        if (null != value) {
            out = SerializeUtils.bigIntegerFromBytes(value);
        }
        out = out.add(amount);
        cacheMap.put(chainId + "_" + assetId, out);
        return RocksDBService.put(DB_NAME_TOTAL_OUT_AMOUNT, key, SerializeUtils.bigInteger2Bytes(out));
    }

    private byte[] getKey(int chainId, int assetId) {
        return ArraysTool.concatenate(ByteUtils.intToBytes(chainId), ByteUtils.intToBytes(assetId));
    }

    private boolean back(int chainId, int assetId, BigInteger amount) throws Exception {
        if (amount.compareTo(BigInteger.ZERO) < 0) {
            throw new NulsRuntimeException(CrossChainErrorCode.PARAMETER_ERROR);
        }
        byte[] key = getKey(chainId, assetId);
        byte[] value = RocksDBService.get(DB_NAME_TOTAL_OUT_AMOUNT, key);
        BigInteger out = BigInteger.ZERO;
        if (null != value) {
            out = SerializeUtils.bigIntegerFromBytes(value);
        }
        out = out.subtract(amount);
        if (out.compareTo(BigInteger.ZERO) < 0) {
            throw new NulsRuntimeException(NulsCrossChainErrorCode.FILE_OPERATION_FAILD);
        }
        cacheMap.put(chainId + "_" + assetId, out);
        return RocksDBService.put(DB_NAME_TOTAL_OUT_AMOUNT, key, SerializeUtils.bigInteger2Bytes(out));
    }

    @Override
    public boolean addBackAmount(int chainId, int assetId, BigInteger amount) {
        lock.lock();
        try {
            return back(chainId, assetId, amount);
        } catch (Exception e) {
            Log.error(e);
            throw new NulsRuntimeException(NulsCrossChainErrorCode.FILE_OPERATION_FAILD);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public BigInteger getOutTotalAmount(int chainId, int assetId) {
        BigInteger amount = cacheMap.get(chainId + "_" + assetId);
        if (null != amount) {
            return amount;
        }
        byte[] key = getKey(chainId, assetId);
        byte[] value = RocksDBService.get(DB_NAME_TOTAL_OUT_AMOUNT, key);
        if (null == value) {
            return BigInteger.ZERO;
        }
        return SerializeUtils.bigIntegerFromBytes(value);
    }
}

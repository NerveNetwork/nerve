package io.nuls.consensus.storage.impl;

import io.nuls.consensus.utils.manager.ChainManager;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.Log;
import io.nuls.core.model.DoubleUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.constant.ConsensusErrorCode;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.StackingAsset;
import io.nuls.consensus.model.bo.config.AssetsStakingLimitCfg;
import io.nuls.consensus.model.bo.config.AssetsType;
import io.nuls.consensus.model.bo.tx.txdata.Deposit;
import io.nuls.consensus.storage.StakingLimitStorageService;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Niels
 */
@Component
public class StakingLimitStorageServiceImpl implements StakingLimitStorageService {
    private Lock lock = new ReentrantLock();

    @Autowired
    private ChainManager chainManager;

    @Override
    public boolean addStaking(Chain chain, String key, BigDecimal amount) {
        lock.lock();
        try {
            BigDecimal oldAmount = getStakingAmount(chain, key);
            BigDecimal newAmount = amount.add(oldAmount);

            try {
                return RocksDBService.put(ConsensusConstant.DB_NAME_CONFIG, key.getBytes("UTF-8"), SerializeUtils.bigDecimal2Bytes(newAmount));
            } catch (Exception e) {
                Log.error(e);
                throw new NulsRuntimeException(ConsensusErrorCode.DEPOSIT_ASSET_ERROR);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean exitStaking(Chain chain, String key, BigDecimal amount) {
        lock.lock();
        try {
            BigDecimal oldAmount = getStakingAmount(chain, key);
            BigDecimal newAmount = oldAmount.subtract(amount);
            try {
                return RocksDBService.put(ConsensusConstant.DB_NAME_CONFIG, key.getBytes("UTF-8"), SerializeUtils.bigDecimal2Bytes(newAmount));
            } catch (Exception e) {
                Log.error(e);
                throw new NulsRuntimeException(ConsensusErrorCode.DEPOSIT_ASSET_ERROR);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public BigDecimal getStakingAmount(Chain chain, String key) {
        if (StringUtils.isBlank(key)) {
            throw new NulsRuntimeException(ConsensusErrorCode.PARAM_ERROR);
        }
        try {
            byte[] bytes = RocksDBService.get(ConsensusConstant.DB_NAME_STAKING_LIMIT, key.getBytes("UTF-8"));
            if (null == bytes) {
                return getTotalAmount(chain, key);
            }
            if (null == bytes) {
                //代码合并错误买单的兼容代码
                bytes = RocksDBService.get(ConsensusConstant.DB_NAME_CONFIG, key.getBytes("UTF-8"));
            }
            if (null == bytes) {
                return BigDecimal.ZERO;
            }
            return SerializeUtils.bytes2BigDecimal(bytes);
        } catch (UnsupportedEncodingException e) {
            Log.error(e);
            throw new NulsRuntimeException(ConsensusErrorCode.DEPOSIT_ASSET_ERROR);
        }
    }

    private BigDecimal getTotalAmount(Chain chain, String key) {
        List<AssetsStakingLimitCfg> list = chain.getConfig().getLimitCfgList();
        AssetsStakingLimitCfg config = null;
        for (AssetsStakingLimitCfg cfg : list) {
            if (cfg.getKey().equals(key)) {
                config = cfg;
            }
        }
        if (null == config) {
            return BigDecimal.ZERO;
        }

        BigDecimal result = BigDecimal.ZERO;
        List<Deposit> deposits = chain.getDepositList();

        for (Deposit deposit : deposits) {
            for (AssetsType assetsType : config.getAssetsTypeList()) {
                if (deposit.getAssetChainId() == assetsType.getChainId() && deposit.getAssetId() == assetsType.getAssetsId()) {
                    StackingAsset asset = chainManager.getAssetByAsset(assetsType.getChainId(), assetsType.getAssetsId());
                    BigDecimal realAmount = DoubleUtils.div(new BigDecimal(deposit.getDeposit()), Math.pow(10, asset.getDecimal()));
                    result = result.add(realAmount);
                }
            }
        }

        return result;
    }
}

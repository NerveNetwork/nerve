package network.nerve.swap.tx.v1;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.swap.cache.FarmCache;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.ValidaterResult;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.FarmBus;
import network.nerve.swap.model.po.FarmPoolPO;
import network.nerve.swap.storage.FarmStorageService;
import network.nerve.swap.storage.SwapExecuteResultStorageService;
import network.nerve.swap.tx.v1.helpers.FarmUpdateTxHelper;
import network.nerve.swap.utils.SwapDBUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 */
@Component("FarmUpdateTxProcessorV1")
public class FarmUpdateTxProcessor implements TransactionProcessor {

    @Autowired
    private FarmUpdateTxHelper helper;

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private FarmCache farmCache;

    @Autowired
    private SwapExecuteResultStorageService swapExecuteResultStorageService;
    @Autowired
    private FarmStorageService farmStorageService;

    @Override
    public int getType() {
        return TxType.FARM_UPDATE;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        Chain chain = chainManager.getChain(chainId);
        Map<String, Object> resultMap = new HashMap<>(SwapConstant.INIT_CAPACITY_2);
        if (chain == null) {
            Log.error("Chains do not exist.");
            resultMap.put("txList", txs);
            resultMap.put("errorCode", SwapErrorCode.CHAIN_NOT_EXIST.getCode());
            return resultMap;
        }
        NulsLogger logger = chain.getLogger();
        List<Transaction> failsList = new ArrayList<>();
        String errorCode = SwapErrorCode.SUCCESS.getCode();
        for (Transaction tx : txs) {
            if (tx.getType() != getType()) {
                logger.error("Tx type is wrong! hash-{}", tx.getHash().toHex());
                failsList.add(tx);
                errorCode = SwapErrorCode.DATA_ERROR.getCode();
                continue;
            }
            ValidaterResult result = helper.validate(chain, tx);
            if (result.isFailed()) {
                failsList.add(tx);
                errorCode = result.getErrorCode().getCode();
                continue;
            }
        }
        resultMap.put("txList", failsList);
        resultMap.put("errorCode", errorCode);
        return resultMap;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = null;
        try {
            chain = chainManager.getChain(chainId);
            Map<String, SwapResult> swapResultMap = chain.getBatchInfo().getSwapResultMap();
            for (Transaction tx : txs) {
                SwapResult result = swapResultMap.get(tx.getHash().toHex());
                this.swapExecuteResultStorageService.save(chainId, tx.getHash(), result);
                if (!result.isSuccess()) {
                    continue;
                }
                FarmBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), FarmBus.class);
                //updateFarm
                FarmPoolPO farm = farmCache.get(bus.getFarmHash());
                farm.setLastRewardBlock(bus.getLastRewardBlockNew());
                farm.setAccSyrupPerShare(bus.getAccSyrupPerShareNew());
                farm.setSyrupTokenBalance(bus.getSyrupBalanceNew());
                farm.setStakeTokenBalance(bus.getStakingBalanceNew());
                farm.setSyrupPerBlock(bus.getSyrupPerBlockNew());
                farm.setTotalSyrupAmount(bus.getTotalSyrupAmountNew());
                farm.setStopHeight(bus.getStopHeightNew());
                farm.setWithdrawLockTime(bus.getWithdrawLockTimeNew() == null ? 0L : bus.getWithdrawLockTimeNew().longValue());
                farm.setSyrupLockTime(bus.getSyrupLockTimeNew() == null ? 0L : bus.getSyrupLockTimeNew().longValue());
                farmCache.put(bus.getFarmHash(), farm);
                farmStorageService.save(chainId, farm);
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = null;
        try {
            chain = chainManager.getChain(chainId);
            Map<String, SwapResult> swapResultMap = chain.getBatchInfo().getSwapResultMap();
            for (int i = txs.size() - 1; i >= 0; i--) {
                Transaction tx = txs.get(i);
                SwapResult result = swapResultMap.get(tx.getHash().toHex());
                if (!result.isSuccess()) {
                    continue;
                }
                FarmBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), FarmBus.class);
                //updateFarm
                FarmPoolPO farm = farmCache.get(bus.getFarmHash());
                farm.setLastRewardBlock(bus.getLastRewardBlockOld());
                farm.setAccSyrupPerShare(bus.getAccSyrupPerShareOld());
                farm.setSyrupTokenBalance(bus.getSyrupBalanceOld());
                farm.setStakeTokenBalance(bus.getStakingBalanceOld());
                farm.setSyrupPerBlock(bus.getSyrupPerBlockOld());
                farm.setTotalSyrupAmount(bus.getTotalSyrupAmountOld());
                farm.setStopHeight(bus.getStopHeightOld());
                farm.setWithdrawLockTime(bus.getWithdrawLockTimeOld() == null ? 0L : bus.getWithdrawLockTimeOld().longValue());
                farm.setSyrupLockTime(bus.getSyrupLockTimeOld() == null ? 0L : bus.getSyrupLockTimeOld().longValue());
                farmCache.put(bus.getFarmHash(), farm);
                farmStorageService.save(chainId, farm);
                this.swapExecuteResultStorageService.delete(chainId, tx.getHash());
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }
}

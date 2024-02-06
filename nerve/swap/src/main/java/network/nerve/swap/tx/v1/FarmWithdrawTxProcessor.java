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
import network.nerve.swap.model.po.FarmUserInfoPO;
import network.nerve.swap.storage.FarmStorageService;
import network.nerve.swap.storage.FarmUserInfoStorageService;
import network.nerve.swap.storage.SwapExecuteResultStorageService;
import network.nerve.swap.tx.v1.helpers.FarmWithdrawHelper;
import network.nerve.swap.utils.SwapDBUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 */
@Component("FarmWithdrawTxProcessorV1")
public class FarmWithdrawTxProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private FarmWithdrawHelper helper;
    @Autowired
    private FarmCache farmCache;
    @Autowired
    private FarmUserInfoStorageService userInfoStorageService;
    @Autowired
    private FarmStorageService farmStorageService;
    @Autowired
    private SwapExecuteResultStorageService swapExecuteResultStorageService;

    @Override
    public int getType() {
        return TxType.FARM_WITHDRAW;
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
            ValidaterResult result = helper.validate(chain, tx, blockHeader == null ? 0 : blockHeader.getTime());
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
            NulsLogger logger = chain.getLogger();
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
                farm.setStopHeight(bus.getStopHeightNew());
                farmCache.put(bus.getFarmHash(), farm);
                farmStorageService.save(chainId, farm);
                //updateUser
                FarmUserInfoPO user = this.userInfoStorageService.load(chainId, bus.getFarmHash(), bus.getUserAddress());
                if (null == user) {
                    user = new FarmUserInfoPO();
                    user.setUserAddress(bus.getUserAddress());
                    user.setFarmHash(bus.getFarmHash());
                }
                user.setRewardDebt(bus.getUserRewardDebtNew());
                user.setAmount(bus.getUserAmountNew());
                this.userInfoStorageService.save(chainId, user);
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
            NulsLogger logger = chain.getLogger();
            Map<String, SwapResult> swapResultMap = chain.getBatchInfo().getSwapResultMap();
            for (Transaction tx : txs) {
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
                farm.setStopHeight(bus.getStopHeightOld());
                farmCache.put(bus.getFarmHash(), farm);
                farmStorageService.save(chainId, farm);
                //updateUser
                do {
                    FarmUserInfoPO user = this.userInfoStorageService.load(chainId, bus.getFarmHash(), bus.getUserAddress());
                    if (null == user) {
                        break;
                    }
                    if (bus.getUserAmountOld().compareTo(BigInteger.ZERO) == 0) {
                        this.userInfoStorageService.delete(chainId, bus.getFarmHash(), bus.getUserAddress());
                        break;
                    }
                    user.setRewardDebt(bus.getUserRewardDebtOld());
                    user.setAmount(bus.getUserAmountOld());
                    this.userInfoStorageService.save(chainId, user);
                } while (false);
                swapExecuteResultStorageService.delete(chainId, tx.getHash());
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }
}

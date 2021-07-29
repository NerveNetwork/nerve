package network.nerve.swap.tx.v1.stable;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.handler.impl.stable.StableRemoveLiquidityHandler;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.IStablePair;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.stable.StableRemoveLiquidityBus;
import network.nerve.swap.model.dto.stable.StableRemoveLiquidityDTO;
import network.nerve.swap.model.txdata.stable.StableRemoveLiquidityData;
import network.nerve.swap.storage.SwapExecuteResultStorageService;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 */
@Component("StableRemoveLiquidityTxProcessorV1")
public class StableRemoveLiquidityTxProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired("PersistencePairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private SwapExecuteResultStorageService swapExecuteResultStorageService;
    @Autowired
    private StableRemoveLiquidityHandler stableRemoveLiquidityHandler;

    @Override
    public int getType() {
        return TxType.SWAP_REMOVE_LIQUIDITY_STABLE_COIN;
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
            try {
                if (tx.getType() != getType()) {
                    logger.error("Tx type is wrong! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.DATA_ERROR.getCode();
                    continue;
                }
                StableRemoveLiquidityData txData = new StableRemoveLiquidityData();
                txData.parse(tx.getTxData(), 0);
                CoinData coinData = tx.getCoinDataInstance();
                StableRemoveLiquidityDTO dto = stableRemoveLiquidityHandler.getStableRemoveLiquidityInfo(chainId, coinData);
                SwapUtils.calStableRemoveLiquidityBusiness(chainId, iPairFactory, dto.getLiquidity(), txData.getIndexs(), dto.getPairAddress(), txData.getTo());
            } catch (Exception e) {
                Log.error(e);
                failsList.add(tx);
                errorCode = SwapUtils.extractErrorCode(e).getCode();
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
                logger.info("[commit] Stable Swap Remove Liquidity, hash: {}", tx.getHash().toHex());
                // 从执行结果中提取业务数据
                SwapResult result = swapResultMap.get(tx.getHash().toHex());
                swapExecuteResultStorageService.save(chainId, tx.getHash(), result);
                if (!result.isSuccess()) {
                    continue;
                }
                CoinData coinData = tx.getCoinDataInstance();
                StableRemoveLiquidityDTO dto = stableRemoveLiquidityHandler.getStableRemoveLiquidityInfo(chainId, coinData);
                String pairAddress = AddressTool.getStringAddressByBytes(dto.getPairAddress());
                IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
                StableRemoveLiquidityBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), StableRemoveLiquidityBus.class);
                // 更新Pair的资金池和发行总量
                stablePair.update(dto.getUserAddress(), bus.getLiquidity().negate(), SwapUtils.convertNegate(bus.getAmounts()), bus.getBalances(), blockHeader.getHeight(), blockHeader.getTime());
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
            for (Transaction tx : txs) {
                SwapResult result = swapExecuteResultStorageService.getResult(chainId, tx.getHash());
                if (result == null) {
                    continue;
                }
                if (!result.isSuccess()) {
                    continue;
                }
                CoinData coinData = tx.getCoinDataInstance();
                StableRemoveLiquidityDTO dto = stableRemoveLiquidityHandler.getStableRemoveLiquidityInfo(chainId, coinData);
                String pairAddress = AddressTool.getStringAddressByBytes(dto.getPairAddress());
                IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
                StableRemoveLiquidityBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), StableRemoveLiquidityBus.class);
                // 回滚Pair的资金池
                stablePair.rollback(dto.getUserAddress(), bus.getLiquidity().negate(), SwapUtils.convertNegate(bus.getAmounts()), bus.getBalances(), bus.getPreBlockHeight(), bus.getPreBlockTime());
                swapExecuteResultStorageService.delete(chainId, tx.getHash());
                logger.info("[rollback] Stable Swap Remove Liquidity, hash: {}", tx.getHash().toHex());
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }

}

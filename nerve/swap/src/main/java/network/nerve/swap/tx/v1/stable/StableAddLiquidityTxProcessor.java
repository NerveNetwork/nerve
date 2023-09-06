package network.nerve.swap.tx.v1.stable;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import io.nuls.core.parse.JSONUtils;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.handler.impl.stable.StableAddLiquidityHandler;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.IStablePair;
import network.nerve.swap.help.SwapHelper;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.stable.StableAddLiquidityBus;
import network.nerve.swap.model.dto.stable.StableAddLiquidityDTO;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.model.txdata.stable.StableAddLiquidityData;
import network.nerve.swap.storage.SwapExecuteResultStorageService;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Niels
 */
@Component("StableAddLiquidityTxProcessorV1")
public class StableAddLiquidityTxProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired("PersistencePairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private SwapExecuteResultStorageService swapExecuteResultStorageService;
    @Autowired
    private StableAddLiquidityHandler stableAddLiquidityHandler;
    @Autowired
    private SwapHelper swapHelper;

    @Override
    public int getType() {
        return TxType.SWAP_ADD_LIQUIDITY_STABLE_COIN;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (swapHelper.isSupportProtocol21()) {
            return this.validateP21(chainId, txs, txMap, blockHeader);
        } else {
            return this.validateP0(chainId, txs, txMap, blockHeader);
        }
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
                logger.info("[{}][commit] Stable Swap Add Liquidity, hash: {}", blockHeader.getHeight(), tx.getHash().toHex());
                // 从执行结果中提取业务数据
                SwapResult result = swapResultMap.get(tx.getHash().toHex());
                swapExecuteResultStorageService.save(chainId, tx.getHash(), result);
                if (!result.isSuccess()) {
                    continue;
                }
                StableAddLiquidityDTO dto = stableAddLiquidityHandler.getStableAddLiquidityInfo(chainId, tx.getCoinDataInstance(), iPairFactory);
                IStablePair stablePair = iPairFactory.getStablePair(dto.getPairAddress());
                StableAddLiquidityBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), StableAddLiquidityBus.class);
                //logger.info("[{}]processor add bus: {}", blockHeader.getHeight(), bus.toString());
                // 更新Pair的资金池和发行总量
                stablePair.update(bus.getLiquidity(), bus.getRealAmounts(), bus.getBalances(), blockHeader.getHeight(), blockHeader.getTime());
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
                StableAddLiquidityDTO dto = stableAddLiquidityHandler.getStableAddLiquidityInfo(chainId, tx.getCoinDataInstance(), iPairFactory);
                IStablePair stablePair = iPairFactory.getStablePair(dto.getPairAddress());
                StableAddLiquidityBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), StableAddLiquidityBus.class);
                // 回滚Pair的资金池
                stablePair.rollback(bus.getLiquidity(), bus.getBalances(), bus.getPreBlockHeight(), bus.getPreBlockTime());
                swapExecuteResultStorageService.delete(chainId, tx.getHash());
                logger.info("[{}][rollback] Stable Swap Add Liquidity, hash: {}", blockHeader.getHeight(), tx.getHash().toHex());
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }

    private Map<String, Object> validateP0(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
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
                StableAddLiquidityData txData = new StableAddLiquidityData();
                try {
                    txData.parse(tx.getTxData(), 0);
                } catch (NulsException e) {
                    Log.error(e);
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    continue;
                }
                CoinData coinData = tx.getCoinDataInstance();
                StableAddLiquidityDTO dto = stableAddLiquidityHandler.getStableAddLiquidityInfo(chainId, coinData, iPairFactory);
                SwapUtils.calStableAddLiquididy(chainId, iPairFactory, dto.getPairAddress(), dto.getFrom(), dto.getAmounts(), txData.getTo());
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

    private Map<String, Object> validateP21(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
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
                StableAddLiquidityData txData = new StableAddLiquidityData();
                try {
                    txData.parse(tx.getTxData(), 0);
                } catch (NulsException e) {
                    Log.error(e);
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    continue;
                }
                CoinData coinData = tx.getCoinDataInstance();
                StableAddLiquidityDTO dto = stableAddLiquidityHandler.getStableAddLiquidityInfo(chainId, coinData, iPairFactory);
                if (chainId == 9 && SwapConstant.UNAVAILABLE_STABLE_PAIR.equals(dto.getPairAddress())) {
                    logger.error("UNAVAILABLE_STABLE_PAIR! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.PAIR_ADDRESS_ERROR.getCode();
                    continue;
                }
                SwapUtils.calStableAddLiquididy(chainId, iPairFactory, dto.getPairAddress(), dto.getFrom(), dto.getAmounts(), txData.getTo());
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

}

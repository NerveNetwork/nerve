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
import io.nuls.core.parse.JSONUtils;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.handler.impl.stable.StableSwapTradeHandler;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.IStablePair;
import network.nerve.swap.help.SwapHelper;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.TradePairBus;
import network.nerve.swap.model.business.stable.StableSwapTradeBus;
import network.nerve.swap.model.dto.stable.StableSwapTradeDTO;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.model.txdata.stable.StableSwapTradeData;
import network.nerve.swap.storage.SwapExecuteResultStorageService;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Niels
 */
@Component("StableSwapTradeTxProcessorV1")
public class StableSwapTradeTxProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired("PersistencePairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private SwapExecuteResultStorageService swapExecuteResultStorageService;
    @Autowired
    private StableSwapTradeHandler stableSwapTradeHandler;
    @Autowired
    private SwapHelper swapHelper;

    @Override
    public int getType() {
        return TxType.SWAP_TRADE_STABLE_COIN;
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
            if (swapResultMap == null) {
                return true;
            }
            for (Transaction tx : txs) {
                logger.info("[commit] Stable Swap Trade, hash: {}", tx.getHash().toHex());
                // Extracting business data from execution results
                SwapResult result = swapResultMap.get(tx.getHash().toHex());
                swapExecuteResultStorageService.save(chainId, tx.getHash(), result);
                if (!result.isSuccess()) {
                    continue;
                }
                StableSwapTradeBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), StableSwapTradeBus.class);
                String pairAddress = AddressTool.getStringAddressByBytes(bus.getPairAddress());
                IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
                // updatePairThe fund pool and total issuance amount of
                stablePair.update(BigInteger.ZERO, bus.getChangeBalances(), bus.getBalances(), blockHeader.getHeight(), blockHeader.getTime());
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
                StableSwapTradeBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), StableSwapTradeBus.class);
                String pairAddress = AddressTool.getStringAddressByBytes(bus.getPairAddress());
                IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
                // RollBACKPairOur fund pool
                stablePair.rollback(BigInteger.ZERO, bus.getBalances(), bus.getPreBlockHeight(), bus.getPreBlockTime());
                swapExecuteResultStorageService.delete(chainId, tx.getHash());
                logger.info("[rollback] Stable Swap Trade, hash: {}", tx.getHash().toHex());
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }


    // Persistent update, after integrating stable coin pools, ordinarySWAPCalling the stablecoin pool function, stablecoin currency1:1exchange
    public void updatePersistence(StableSwapTradeBus bus, long height, long time) throws Exception {
        String pairAddress = AddressTool.getStringAddressByBytes(bus.getPairAddress());
        IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
        // updatePairThe fund pool and total issuance amount of
        stablePair.update(BigInteger.ZERO, bus.getChangeBalances(), bus.getBalances(), height, time);
    }

    // Rolling back persistent updates, integrating stable coin pools, ordinarySWAPCalling the stablecoin pool function, stablecoin currency1:1exchange
    public void rollbackPersistence(TradePairBus tradePairBus) throws Exception {
        String pairAddress = AddressTool.getStringAddressByBytes(tradePairBus.getPairAddress());
        IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
        BigInteger[] balances = stablePair.getBalances();
        NerveToken[] coins = stablePair.getPair().getCoins();
        NerveToken tokenIn = tradePairBus.getTokenIn();
        NerveToken tokenOut = tradePairBus.getTokenOut();
        int tokenInIndex = 0, tokenOutIndex = 0, length = coins.length;
        for (int i = 0; i < length; i++) {
            NerveToken token = coins[i];
            if (token.equals(tokenIn)) {
                tokenInIndex = i;
            } else if (token.equals(tokenOut)) {
                tokenOutIndex = i;
            }
        }
        BigInteger[] oldBalances = SwapUtils.cloneBigIntegerArray(balances);
        oldBalances[tokenInIndex] = oldBalances[tokenInIndex].subtract(tradePairBus.getAmountIn());
        oldBalances[tokenOutIndex] = oldBalances[tokenOutIndex].add(tradePairBus.getAmountOut());
        stablePair.rollback(BigInteger.ZERO, oldBalances, tradePairBus.getPreBlockHeight(), tradePairBus.getPreBlockTime());
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
                StableSwapTradeData txData = new StableSwapTradeData();
                try {
                    txData.parse(tx.getTxData(), 0);
                } catch (NulsException e) {
                    Log.error(e);
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    continue;
                }
                byte tokenOutIndex = txData.getTokenOutIndex();
                CoinData coinData = tx.getCoinDataInstance();
                StableSwapTradeDTO dto = stableSwapTradeHandler.getStableSwapTradeInfo(chainId, coinData, iPairFactory, tokenOutIndex);
                SwapUtils.calStableSwapTradeBusiness(swapHelper, chainId, iPairFactory, dto.getAmountsIn(), tokenOutIndex, dto.getPairAddress(), txData.getTo(), txData.getFeeTo(), null);
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
                StableSwapTradeData txData = new StableSwapTradeData();
                try {
                    txData.parse(tx.getTxData(), 0);
                } catch (NulsException e) {
                    Log.error(e);
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    continue;
                }
                byte tokenOutIndex = txData.getTokenOutIndex();
                CoinData coinData = tx.getCoinDataInstance();
                StableSwapTradeDTO dto = stableSwapTradeHandler.getStableSwapTradeInfoP21(chainId, coinData, iPairFactory, tokenOutIndex, txData.getFeeTo());
                if (chainId == 9 && SwapConstant.UNAVAILABLE_STABLE_PAIR.equals(dto.getPairAddress())) {
                    logger.error("UNAVAILABLE_STABLE_PAIR! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.PAIR_ADDRESS_ERROR.getCode();
                    continue;
                }
                SwapUtils.calStableSwapTradeBusiness(swapHelper, chainId, iPairFactory, dto.getAmountsIn(), tokenOutIndex, dto.getPairAddress(), txData.getTo(), null, dto.getFeeTo());
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

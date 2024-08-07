package network.nerve.swap.tx.v1;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.handler.impl.SwapTradeHandler;
import network.nerve.swap.help.IPair;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.SwapHelper;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.SwapTradeBus;
import network.nerve.swap.model.business.TradePairBus;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.dto.SwapTradeDTO;
import network.nerve.swap.model.txdata.SwapTradeData;
import network.nerve.swap.storage.SwapExecuteResultStorageService;
import network.nerve.swap.tx.v1.stable.StableSwapTradeTxProcessor;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;
import java.util.*;

/**
 * @author Niels
 */
@Component("SwapTradeTxProcessorV1")
public class SwapTradeTxProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired("PersistencePairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private SwapExecuteResultStorageService swapExecuteResultStorageService;
    @Autowired
    private SwapTradeHandler swapTradeHandler;
    @Autowired
    private LedgerAssetCache ledgerAssetCache;
    @Autowired
    private SwapPairCache swapPairCache;
    @Autowired
    private SwapHelper swapHelper;
    @Autowired
    private StableSwapTradeTxProcessor stableSwapTradeTxProcessor;

    @Override
    public int getType() {
        return TxType.SWAP_TRADE;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (swapHelper.isSupportProtocol17()) {
            //protocol17: After integrating the stablecoin pool, stablecoin currencies1:1exchange
            return this.validateProtocol17(chainId, txs, txMap, blockHeader);
        } else {
            return this._validate(chainId, txs, txMap, blockHeader);
        }
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        if (swapHelper.isSupportProtocol17()) {
            //protocol17: After integrating the stablecoin pool, stablecoin currencies1:1exchange
            return this.commitProtocol17(chainId, txs, blockHeader, syncStatus);
        } else {
            return this._commit(chainId, txs, blockHeader, syncStatus);
        }
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        if (swapHelper.isSupportProtocol17()) {
            //protocol17: After integrating the stablecoin pool, stablecoin currencies1:1exchange
            return this.rollbackProtocol17(chainId, txs, blockHeader);
        } else {
            return this._rollback(chainId, txs, blockHeader);
        }
    }

    public Map<String, Object> _validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        Chain chain = chainManager.getChain(chainId);
        if (blockHeader == null) blockHeader = chain.getLatestBasicBlock().toBlockHeader();

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
        C1:
        for (Transaction tx : txs) {
            try {
                if (tx.getType() != getType()) {
                    logger.error("Tx type is wrong! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.DATA_ERROR.getCode();
                    continue;
                }
                SwapTradeData txData = new SwapTradeData();
                txData.parse(tx.getTxData(), 0);

                long deadline = txData.getDeadline();
                if (blockHeader.getTime() > deadline) {
                    logger.error("Tx EXPIRED! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.EXPIRED.getCode();
                    continue;
                }
                NerveToken[] path = txData.getPath();
                int pathLength = path.length;
                if (pathLength < 2) {
                    logger.error("INVALID_PATH! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.INVALID_PATH.getCode();
                    continue;
                }
                for (int i = 0; i < pathLength; i++) {
                    NerveToken token = path[i];
                    LedgerAssetDTO asset = ledgerAssetCache.getLedgerAsset(chainId, token);
                    if (asset == null) {
                        logger.error("Ledger asset not exist! hash-{}", tx.getHash().toHex());
                        failsList.add(tx);
                        errorCode = SwapErrorCode.LEDGER_ASSET_NOT_EXIST.getCode();
                        continue C1;
                    }
                    if (i == pathLength - 1) {
                        continue;
                    }
                    if (!swapPairCache.isExist(SwapUtils.getStringPairAddress(chainId, token, path[i + 1]))) {
                        logger.error("PAIR_ADDRESS_ERROR! hash-{}", tx.getHash().toHex());
                        failsList.add(tx);
                        errorCode = SwapErrorCode.PAIR_ADDRESS_ERROR.getCode();
                        continue C1;
                    }
                }
                CoinData coinData = tx.getCoinDataInstance();
                SwapTradeDTO dto = swapTradeHandler.getSwapTradeInfo(chainId, coinData);
                if (!swapPairCache.isExist(AddressTool.getStringAddressByBytes(dto.getFirstPairAddress()))) {
                    logger.error("PAIR_NOT_EXIST! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.PAIR_NOT_EXIST.getCode();
                    continue;
                }
                if (!Arrays.equals(SwapUtils.getPairAddress(chainId, path[0], path[1]), dto.getFirstPairAddress())) {
                    logger.error("PAIR_INCONSISTENCY! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.PAIR_INCONSISTENCY.getCode();
                    continue;
                }
                swapTradeHandler.calSwapTradeBusiness(chainId, iPairFactory, dto.getAmountIn(),
                        txData.getTo(), txData.getPath(), txData.getAmountOutMin(), txData.getFeeTo());
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

    public Map<String, Object> validateProtocol17(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        Chain chain = chainManager.getChain(chainId);
        if (blockHeader == null) blockHeader = chain.getLatestBasicBlock().toBlockHeader();

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
        C1:
        for (Transaction tx : txs) {
            try {
                if (tx.getType() != getType()) {
                    logger.error("Tx type is wrong! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.DATA_ERROR.getCode();
                    continue;
                }
                SwapTradeData txData = new SwapTradeData();
                txData.parse(tx.getTxData(), 0);

                long deadline = txData.getDeadline();
                if (blockHeader.getTime() > deadline) {
                    logger.error("Tx EXPIRED! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.EXPIRED.getCode();
                    continue;
                }
                NerveToken[] path = txData.getPath();
                int pathLength = path.length;
                if (pathLength < 2) {
                    logger.error("INVALID_PATH! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.INVALID_PATH.getCode();
                    continue;
                }
                for (int i = 0; i < pathLength; i++) {
                    NerveToken token = path[i];
                    LedgerAssetDTO asset = ledgerAssetCache.getLedgerAsset(chainId, token,blockHeader.getHeight());
                    if (asset == null) {
                        logger.error("Ledger asset not exist! hash-{}, {}", tx.getHash().toHex(),token.toString());
                        failsList.add(tx);
                        errorCode = SwapErrorCode.LEDGER_ASSET_NOT_EXIST.getCode();
                        continue C1;
                    }
                    if (i == pathLength - 1) {
                        continue;
                    }
                    // Check the address, it may be the address of the stablecoin pool
                    if (!SwapUtils.groupCombining(token, path[i + 1]) && !swapPairCache.isExist(SwapUtils.getStringPairAddress(chainId, token, path[i + 1]))) {
                        logger.error("PAIR_ADDRESS_ERROR! hash-{}", tx.getHash().toHex());
                        failsList.add(tx);
                        errorCode = SwapErrorCode.PAIR_ADDRESS_ERROR.getCode();
                        continue C1;
                    }
                }
                CoinData coinData = tx.getCoinDataInstance();
                SwapTradeDTO dto = swapTradeHandler.getSwapTradeInfo(chainId, coinData);
                // When the first transaction pair is a stablecoin pool transaction pair, check if it is a stablecoin pool address
                int groupIndex;
                if ((groupIndex = SwapContext.stableCoinGroup.groupIndex(path[0], path[1])) != -1) {
                    if (!Arrays.equals(AddressTool.getAddress(SwapContext.stableCoinGroup.getAddressByIndex(groupIndex)), dto.getFirstPairAddress())) {
                        logger.error("PAIR_INCONSISTENCY!(Stable Pair) hash-{}", tx.getHash().toHex());
                        failsList.add(tx);
                        errorCode = SwapErrorCode.PAIR_INCONSISTENCY.getCode();
                        continue;
                    }
                } else {
                    if (!swapPairCache.isExist(AddressTool.getStringAddressByBytes(dto.getFirstPairAddress()))) {
                        logger.error("PAIR_NOT_EXIST! hash-{}", tx.getHash().toHex());
                        failsList.add(tx);
                        errorCode = SwapErrorCode.PAIR_NOT_EXIST.getCode();
                        continue;
                    }
                    if (!Arrays.equals(SwapUtils.getPairAddress(chainId, path[0], path[1]), dto.getFirstPairAddress())) {
                        logger.error("PAIR_INCONSISTENCY! hash-{}", tx.getHash().toHex());
                        failsList.add(tx);
                        errorCode = SwapErrorCode.PAIR_INCONSISTENCY.getCode();
                        continue;
                    }
                }
                SwapTradeBus swapTradeBus = swapTradeHandler.calSwapTradeBusiness(chainId, iPairFactory, dto.getAmountIn(),
                        txData.getTo(), txData.getPath(), txData.getAmountOutMin(), txData.getFeeTo());
                //protocol17: Add verification and logic for stablecoin exchange
                if (swapTradeBus.isExistStablePair()) {
                    swapTradeHandler.makeSystemDealTx(chainId, iPairFactory, swapTradeBus, tx.getHash().toHex(), blockHeader.getTime(), LedgerTempBalanceManager.newInstance(chainId), txData.getFeeTo(), dto.getUserAddress());
                }
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

    private boolean _commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = null;
        try {
            chain = chainManager.getChain(chainId);
            NulsLogger logger = chain.getLogger();
            Map<String, SwapResult> swapResultMap = chain.getBatchInfo().getSwapResultMap();
            for (Transaction tx : txs) {
                logger.info("[commit] Swap Trade, hash: {}", tx.getHash().toHex());
                // Extracting business data from execution results
                SwapResult result = swapResultMap.get(tx.getHash().toHex());
                swapExecuteResultStorageService.save(chainId, tx.getHash(), result);
                if (!result.isSuccess()) {
                    continue;
                }
                SwapTradeBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), SwapTradeBus.class);
                // updatePairThe fund pool and total issuance amount of
                List<TradePairBus> busList = bus.getTradePairBuses();
                for (TradePairBus pairBus : busList) {
                    IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairBus.getPairAddress()));
                    pair.update(BigInteger.ZERO, pairBus.getBalance0(), pairBus.getBalance1(), pairBus.getReserve0(), pairBus.getReserve1(), blockHeader.getHeight(), blockHeader.getTime());
                }
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }

    private boolean commitProtocol17(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = null;
        try {
            chain = chainManager.getChain(chainId);
            NulsLogger logger = chain.getLogger();
            Map<String, SwapResult> swapResultMap = chain.getBatchInfo().getSwapResultMap();
            for (Transaction tx : txs) {
                logger.info("[commit] Swap Trade, hash: {}", tx.getHash().toHex());
                // Extracting business data from execution results
                SwapResult result = swapResultMap.get(tx.getHash().toHex());
                swapExecuteResultStorageService.save(chainId, tx.getHash(), result);
                if (!result.isSuccess()) {
                    continue;
                }
                SwapTradeBus bus = result.getSwapTradeBus();
                // updatePairThe fund pool and total issuance amount of
                List<TradePairBus> busList = bus.getTradePairBuses();
                for (TradePairBus pairBus : busList) {
                    //protocol17: Update stablecoin pool After integrating the stablecoin pool, stablecoin currencies1:1exchange
                    if (bus.isExistStablePair() && SwapUtils.groupCombining(pairBus.getTokenIn(), pairBus.getTokenOut())) {
                        // Persistently updating data from the stablecoin pool
                        stableSwapTradeTxProcessor.updatePersistence(pairBus.getStableSwapTradeBus(), blockHeader.getHeight(), blockHeader.getTime());
                        continue;
                    }
                    IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairBus.getPairAddress()));
                    pair.update(BigInteger.ZERO, pairBus.getBalance0(), pairBus.getBalance1(), pairBus.getReserve0(), pairBus.getReserve1(), blockHeader.getHeight(), blockHeader.getTime());
                }
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }

    private boolean _rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
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
                SwapTradeBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), SwapTradeBus.class);
                // RollBACKPairOur fund pool
                List<TradePairBus> busList = bus.getTradePairBuses();
                for (TradePairBus pairBus : busList) {
                    IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairBus.getPairAddress()));
                    pair.rollback(BigInteger.ZERO, pairBus.getReserve0(), pairBus.getReserve1(), pairBus.getPreBlockHeight(), pairBus.getPreBlockTime());
                }
                swapExecuteResultStorageService.delete(chainId, tx.getHash());
                logger.info("[rollback] Swap Trade, hash: {}", tx.getHash().toHex());
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }

    private boolean rollbackProtocol17(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
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

                SwapTradeBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), SwapTradeBus.class);
                // RollBACKPairOur fund pool
                List<TradePairBus> busList = bus.getTradePairBuses();
                for (TradePairBus pairBus : busList) {
                    //protocol17: Rolling back and updating the stablecoin pool After integrating the stablecoin pool, stablecoin currencies1:1exchange
                    if (SwapUtils.groupCombining(pairBus.getTokenIn(), pairBus.getTokenOut())) {
                        // Rolling back persistent updates to stablecoin pool data
                        stableSwapTradeTxProcessor.rollbackPersistence(pairBus);
                        continue;
                    }
                    IPair pair = iPairFactory.getPair(AddressTool.getStringAddressByBytes(pairBus.getPairAddress()));
                    pair.rollback(BigInteger.ZERO, pairBus.getReserve0(), pairBus.getReserve1(), pairBus.getPreBlockHeight(), pairBus.getPreBlockTime());
                }
                swapExecuteResultStorageService.delete(chainId, tx.getHash());
                logger.info("[rollback] Swap Trade, hash: {}", tx.getHash().toHex());
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }
}

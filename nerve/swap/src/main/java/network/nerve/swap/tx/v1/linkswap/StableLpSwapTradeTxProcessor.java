package network.nerve.swap.tx.v1.linkswap;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.handler.impl.SwapTradeHandler;
import network.nerve.swap.handler.impl.linkswap.StableLpSwapTradeHandler;
import network.nerve.swap.handler.impl.stable.StableAddLiquidityHandler;
import network.nerve.swap.handler.impl.stable.StableSwapTradeHandler;
import network.nerve.swap.help.IPair;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.IStablePair;
import network.nerve.swap.help.SwapHelper;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.manager.LedgerTempBalanceManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.SwapTradeBus;
import network.nerve.swap.model.business.TradePairBus;
import network.nerve.swap.model.business.linkswap.StableLpSwapTradeBus;
import network.nerve.swap.model.business.stable.StableAddLiquidityBus;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.dto.stable.StableAddLiquidityDTO;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.model.txdata.linkswap.StableLpSwapTradeData;
import network.nerve.swap.model.txdata.stable.StableAddLiquidityData;
import network.nerve.swap.storage.SwapExecuteResultStorageService;
import network.nerve.swap.tx.v1.stable.StableSwapTradeTxProcessor;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;
import java.util.*;

import static network.nerve.swap.constant.SwapErrorCode.INVALID_PATH;
import static network.nerve.swap.constant.SwapErrorCode.PAIR_NOT_EXIST;

/**
 * @author Niels
 */
@Component("StableLpSwapTradeTxProcessorV1")
public class StableLpSwapTradeTxProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired("PersistencePairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private SwapExecuteResultStorageService swapExecuteResultStorageService;
    @Autowired
    private StableLpSwapTradeHandler stableLpSwapTradeHandler;
    @Autowired
    private StableSwapPairCache stableSwapPairCache;
    @Autowired
    private SwapPairCache swapPairCache;
    @Autowired
    private StableAddLiquidityHandler stableAddLiquidityHandler;
    @Autowired
    private SwapTradeHandler swapTradeHandler;
    @Autowired
    private StableSwapTradeHandler stableSwapTradeHandler;
    @Autowired
    private LedgerAssetCache ledgerAssetCache;
    @Autowired
    private StableSwapTradeTxProcessor stableSwapTradeTxProcessor;
    @Autowired
    private SwapHelper swapHelper;

    @Override
    public int getType() {
        return TxType.SWAP_STABLE_LP_SWAP_TRADE;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        if (!swapHelper.isSupportProtocol17()) {
            throw new NulsRuntimeException(SwapErrorCode.TX_TYPE_INVALID);
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
        for (Transaction tx : txs) {
            try {
                if (tx.getType() != getType()) {
                    logger.error("Tx type is wrong! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.DATA_ERROR.getCode();
                    continue;
                }
                CoinData coinData = tx.getCoinDataInstance();
                StableAddLiquidityDTO dto = stableAddLiquidityHandler.getStableAddLiquidityInfo(chainId, coinData, iPairFactory);
                List<CoinTo> tos = coinData.getTo();
                if (tos.size() > 1) {
                    throw new NulsException(SwapErrorCode.INVALID_TO);
                }
                // Extract business parameters
                StableLpSwapTradeData txData = new StableLpSwapTradeData();
                txData.parse(tx.getTxData(), 0);
                long deadline = txData.getDeadline();
                if (blockHeader.getTime() > deadline) {
                    throw new NulsException(SwapErrorCode.EXPIRED);
                }
                // Check transaction path
                NerveToken[] path = txData.getPath();
                int pathLength = path.length;
                if (pathLength < 3) {
                    throw new NulsException(INVALID_PATH);
                }
                for (int i = 0; i < pathLength; i++) {
                    NerveToken token = path[i];
                    LedgerAssetDTO asset = ledgerAssetCache.getLedgerAsset(chainId, token);
                    if (asset == null) {
                        throw new NulsException(SwapErrorCode.LEDGER_ASSET_NOT_EXIST);
                    }
                    if (i == 0 || i == pathLength - 1) {
                        continue;
                    }
                    // Check the address, it may be the address of the stablecoin pool
                    if (!SwapUtils.groupCombining(token, path[i + 1]) && !swapPairCache.isExist(SwapUtils.getStringPairAddress(chainId, token, path[i + 1]))) {
                        throw new NulsException(SwapErrorCode.PAIR_ADDRESS_ERROR);
                    }
                }

                NerveToken firstToken = path[0];
                NerveToken stableLpToken = path[1];
                // Stable coins in the path
                CoinTo coinTo = coinData.getTo().get(0);
                if (coinTo.getAssetsChainId() != firstToken.getChainId() || coinTo.getAssetsId() != firstToken.getAssetId()) {
                    throw new NulsException(SwapErrorCode.INVALID_TO);
                }
                // Stable coins in the pathLP
                String pairAddressByTokenLP = stableSwapPairCache.getPairAddressByTokenLP(chainId, stableLpToken);
                if (!dto.getPairAddress().equals(pairAddressByTokenLP)) {
                    throw new NulsException(INVALID_PATH);
                }

                // The first ordinaryswapThe transaction is addressed by path[1], path[2] Transaction pairs composed of
                byte[] firstSwapPair = SwapUtils.getPairAddress(chainId, path[1], path[2]);
                if (!swapPairCache.isExist(AddressTool.getStringAddressByBytes(firstSwapPair))) {
                    throw new NulsException(PAIR_NOT_EXIST);
                }
                String pairAddress = dto.getPairAddress();
                // Integrate computing data
                StableAddLiquidityBus stableAddLiquidityBus = SwapUtils.calStableAddLiquididy(swapHelper, chainId, iPairFactory, pairAddress, dto.getFrom(), dto.getAmounts(), firstSwapPair);
                NerveToken[] swapTradePath = new NerveToken[path.length - 1];
                System.arraycopy(path, 1, swapTradePath, 0, path.length - 1);
                SwapTradeBus swapTradeBus = swapTradeHandler.calSwapTradeBusiness(chainId, iPairFactory, stableAddLiquidityBus.getLiquidity(), txData.getTo(), swapTradePath, txData.getAmountOutMin(), txData.getFeeTo());
                //protocol17: Add verification and logic for stablecoin exchange
                if (swapTradeBus.isExistStablePair()) {
                    swapTradeHandler.makeSystemDealTx(chainId, iPairFactory, swapTradeBus, tx.getHash().toHex(), blockHeader.getTime(), LedgerTempBalanceManager.newInstance(chainId), txData.getFeeTo(), dto.getFrom());
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
                logger.info("[commit] Stable LP Swap Trade, hash: {}", tx.getHash().toHex());
                // Extracting business data from execution results
                SwapResult result = swapResultMap.get(tx.getHash().toHex());
                swapExecuteResultStorageService.save(chainId, tx.getHash(), result);
                if (!result.isSuccess()) {
                    continue;
                }
                StableLpSwapTradeBus stableLpSwapTradeBus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), StableLpSwapTradeBus.class);
                StableAddLiquidityBus stableAddLiquidityBus = stableLpSwapTradeBus.getStableAddLiquidityBus();
                SwapTradeBus swapTradeBus = stableLpSwapTradeBus.getSwapTradeBus();

                StableAddLiquidityDTO dto = stableAddLiquidityHandler.getStableAddLiquidityInfo(chainId, tx.getCoinDataInstance(), iPairFactory);
                IStablePair stablePair = iPairFactory.getStablePair(dto.getPairAddress());

                // updateStablePairThe fund pool and total issuance amount of
                stablePair.update(stableAddLiquidityBus.getLiquidity(), stableAddLiquidityBus.getRealAmounts(), stableAddLiquidityBus.getBalances(), blockHeader.getHeight(), blockHeader.getTime());
                // updateSwapPairThe fund pool and total issuance amount of
                List<TradePairBus> busList = swapTradeBus.getTradePairBuses();
                for (TradePairBus pairBus : busList) {
                    //protocol17: Update stablecoin pool After integrating the stablecoin pool, stablecoin currencies1:1exchange
                    if (swapTradeBus.isExistStablePair() && SwapUtils.groupCombining(pairBus.getTokenIn(), pairBus.getTokenOut())) {
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
                StableLpSwapTradeBus stableLpSwapTradeBus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), StableLpSwapTradeBus.class);
                StableAddLiquidityBus stableAddLiquidityBus = stableLpSwapTradeBus.getStableAddLiquidityBus();
                SwapTradeBus swapTradeBus = stableLpSwapTradeBus.getSwapTradeBus();
                // RollBACKSwapPairOur fund pool
                List<TradePairBus> busList = swapTradeBus.getTradePairBuses();
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

                // RollBACKStablePairOur fund pool
                StableAddLiquidityDTO dto = stableAddLiquidityHandler.getStableAddLiquidityInfo(chainId, tx.getCoinDataInstance(), iPairFactory);
                IStablePair stablePair = iPairFactory.getStablePair(dto.getPairAddress());
                stablePair.rollback(stableAddLiquidityBus.getLiquidity(), stableAddLiquidityBus.getBalances(), stableAddLiquidityBus.getPreBlockHeight(), stableAddLiquidityBus.getPreBlockTime());
                swapExecuteResultStorageService.delete(chainId, tx.getHash());
                logger.info("[rollback] Stable LP Swap Trade, hash: {}", tx.getHash().toHex());
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }

}

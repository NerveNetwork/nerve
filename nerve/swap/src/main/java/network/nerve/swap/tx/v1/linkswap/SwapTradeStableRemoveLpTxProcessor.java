package network.nerve.swap.tx.v1.linkswap;

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
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.handler.impl.SwapTradeHandler;
import network.nerve.swap.handler.impl.SwapTradeStableRemoveLpHandler;
import network.nerve.swap.handler.impl.linkswap.StableLpSwapTradeHandler;
import network.nerve.swap.handler.impl.stable.StableAddLiquidityHandler;
import network.nerve.swap.handler.impl.stable.StableRemoveLiquidityHandler;
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
import network.nerve.swap.model.business.linkswap.SwapTradeStableRemoveLpBus;
import network.nerve.swap.model.business.stable.StableAddLiquidityBus;
import network.nerve.swap.model.business.stable.StableRemoveLiquidityBus;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.dto.SwapTradeDTO;
import network.nerve.swap.model.dto.stable.StableAddLiquidityDTO;
import network.nerve.swap.model.dto.stable.StableSwapPairDTO;
import network.nerve.swap.model.txdata.linkswap.SwapTradeStableRemoveLpData;
import network.nerve.swap.storage.SwapExecuteResultStorageService;
import network.nerve.swap.tx.v1.stable.StableSwapTradeTxProcessor;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.math.BigInteger;
import java.util.*;

import static network.nerve.swap.constant.SwapErrorCode.INVALID_PATH;

/**
 * @author Niels
 */
@Component("SwapTradeStableRemoveLpTxProcessorV1")
public class SwapTradeStableRemoveLpTxProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired("PersistencePairFactory")
    private IPairFactory iPairFactory;
    @Autowired
    private SwapExecuteResultStorageService swapExecuteResultStorageService;
    @Autowired
    private SwapTradeStableRemoveLpHandler swapTradeStableRemoveLpHandler;
    @Autowired
    private StableSwapPairCache stableSwapPairCache;
    @Autowired
    private SwapPairCache swapPairCache;
    @Autowired
    private StableRemoveLiquidityHandler stableRemoveLiquidityHandler;
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
        return TxType.SWAP_TRADE_SWAP_STABLE_REMOVE_LP;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        if (!swapHelper.isSupportProtocol22()) {
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

                // Extract business parameters
                SwapTradeStableRemoveLpData txData = new SwapTradeStableRemoveLpData();
                txData.parse(tx.getTxData(), 0);
                long deadline = txData.getDeadline();
                if (blockHeader.getTime() > deadline) {
                    throw new NulsException(SwapErrorCode.EXPIRED);
                }
                // Check transaction path
                NerveToken[] path = txData.getPath();
                int pathLength = path.length;
                if (pathLength < 2) {
                    throw new NulsException(INVALID_PATH);
                }

                for (int i = 0; i < pathLength; i++) {
                    NerveToken token = path[i];
                    LedgerAssetDTO asset = ledgerAssetCache.getLedgerAsset(chainId, token);
                    if (asset == null) {
                        throw new NulsException(SwapErrorCode.LEDGER_ASSET_NOT_EXIST);
                    }
                    if (i == pathLength - 1) {
                        continue;
                    }
                    // Check address
                    if (!swapPairCache.isExist(SwapUtils.getStringPairAddress(chainId, token, path[i + 1]))) {
                        throw new NulsException(SwapErrorCode.PAIR_ADDRESS_ERROR);
                    }
                }
                CoinData coinData = tx.getCoinDataInstance();
                SwapTradeDTO dto = swapTradeHandler.getSwapTradeInfo(chainId, coinData);
                if (!Arrays.equals(SwapUtils.getPairAddress(chainId, path[0], path[1]), dto.getFirstPairAddress())) {
                    logger.error("PAIR_INCONSISTENCY! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.PAIR_INCONSISTENCY.getCode();
                    continue;
                }
                // Stable coins in the pathLP
                NerveToken stableLpToken = path[pathLength - 1];
                String stablePairAddress = stableSwapPairCache.getPairAddressByTokenLP(chainId, stableLpToken);
                // Verify the last onetokenIs itstableLpoftoken
                if (StringUtils.isBlank(stablePairAddress)) {
                    throw new NulsException(SwapErrorCode.PAIR_ADDRESS_ERROR);
                }
                byte[] stablePairAddressBytes = AddressTool.getAddress(stablePairAddress);
                // validatetargetTokenIs it currentstableIn the pondtoken
                NerveToken targetToken = txData.getTargetToken();
                StableSwapPairDTO stableSwapPairDTO = stableSwapPairCache.get(stablePairAddress);
                NerveToken[] coins = stableSwapPairDTO.getPo().getCoins();
                int targetIndex = -1;
                for (int i = 0; i < coins.length; i++) {
                    NerveToken coin = coins[i];
                    if (targetToken.equals(coin)) {
                        targetIndex = i;
                        break;
                    }
                }
                if (targetIndex == -1) {
                    throw new NulsException(SwapErrorCode.INVALID_COINS);
                }

                // Integrate computing data
                SwapTradeBus swapTradeBus = swapTradeHandler.calSwapTradeBusiness(chainId, iPairFactory, dto.getAmountIn(),
                        stablePairAddressBytes, txData.getPath(), txData.getAmountOutMin(), txData.getFeeTo());
                //protocol17: Add verification and logic for stablecoin exchange
                if (swapTradeBus.isExistStablePair()) {
                    swapTradeHandler.makeSystemDealTx(chainId, iPairFactory, swapTradeBus, tx.getHash().toHex(), blockHeader.getTime(), LedgerTempBalanceManager.newInstance(chainId), txData.getFeeTo(), dto.getUserAddress());
                }
                List<TradePairBus> pairBuses = swapTradeBus.getTradePairBuses();
                TradePairBus lastPairBus = pairBuses.get(pairBuses.size() - 1);
                SwapUtils.calStableRemoveLiquidityBusiness(swapHelper, chainId, iPairFactory, lastPairBus.getAmountOut(), new byte[]{(byte) targetIndex}, stablePairAddressBytes, txData.getTo());
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
                logger.info("[commit] Swap Trade Stable Remove LP, hash: {}", tx.getHash().toHex());
                // Extracting business data from execution results
                SwapResult result = swapResultMap.get(tx.getHash().toHex());
                swapExecuteResultStorageService.save(chainId, tx.getHash(), result);
                if (!result.isSuccess()) {
                    continue;
                }
                SwapTradeStableRemoveLpBus swapTradeStableRemoveLpBus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), SwapTradeStableRemoveLpBus.class);
                StableRemoveLiquidityBus stableRemoveLiquidityBus = swapTradeStableRemoveLpBus.getStableRemoveLiquidityBus();
                SwapTradeBus swapTradeBus = swapTradeStableRemoveLpBus.getSwapTradeBus();
                // Extract business parameters
                SwapTradeStableRemoveLpData txData = new SwapTradeStableRemoveLpData();
                txData.parse(tx.getTxData(), 0);
                NerveToken[] path = txData.getPath();
                // Stable coins in the pathLP
                NerveToken stableLpToken = path[path.length - 1];
                String stablePairAddress = stableSwapPairCache.getPairAddressByTokenLP(chainId, stableLpToken);

                IStablePair stablePair = iPairFactory.getStablePair(stablePairAddress);

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
                // updatePairThe fund pool and total issuance amount of
                stablePair.update(stableRemoveLiquidityBus.getLiquidity().negate(), SwapUtils.convertNegate(stableRemoveLiquidityBus.getAmounts()), stableRemoveLiquidityBus.getBalances(), blockHeader.getHeight(), blockHeader.getTime());
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
                SwapTradeStableRemoveLpBus swapTradeStableRemoveLpBus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), SwapTradeStableRemoveLpBus.class);
                StableRemoveLiquidityBus stableRemoveLiquidityBus = swapTradeStableRemoveLpBus.getStableRemoveLiquidityBus();
                SwapTradeBus swapTradeBus = swapTradeStableRemoveLpBus.getSwapTradeBus();
                // Extract business parameters
                SwapTradeStableRemoveLpData txData = new SwapTradeStableRemoveLpData();
                txData.parse(tx.getTxData(), 0);
                NerveToken[] path = txData.getPath();
                // Stable coins in the pathLP
                NerveToken stableLpToken = path[path.length - 1];
                String stablePairAddress = stableSwapPairCache.getPairAddressByTokenLP(chainId, stableLpToken);
                IStablePair stablePair = iPairFactory.getStablePair(stablePairAddress);

                // RollBACKStablePairOur fund pool
                stablePair.rollback(stableRemoveLiquidityBus.getLiquidity().negate(), stableRemoveLiquidityBus.getBalances(), stableRemoveLiquidityBus.getPreBlockHeight(), stableRemoveLiquidityBus.getPreBlockTime());
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

                swapExecuteResultStorageService.delete(chainId, tx.getHash());
                logger.info("[rollback] Swap Trade Stable Remove LP, hash: {}", tx.getHash().toHex());
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }

}

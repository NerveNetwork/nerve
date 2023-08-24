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
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.parse.JSONUtils;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.handler.impl.stable.StableRemoveLiquidityHandler;
import network.nerve.swap.help.IPairFactory;
import network.nerve.swap.help.IStablePair;
import network.nerve.swap.help.SwapHelper;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.business.stable.StableRemoveLiquidityBus;
import network.nerve.swap.model.dto.stable.StableRemoveLiquidityDTO;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.model.txdata.stable.StableRemoveLiquidityData;
import network.nerve.swap.storage.SwapExecuteResultStorageService;
import network.nerve.swap.utils.SwapDBUtil;
import network.nerve.swap.utils.SwapUtils;

import java.util.*;
import java.util.stream.Collectors;

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
    @Autowired
    private SwapHelper swapHelper;

    @Override
    public int getType() {
        return TxType.SWAP_REMOVE_LIQUIDITY_STABLE_COIN;
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
                logger.info("[{}][commit] Stable Swap Remove Liquidity, hash: {}", blockHeader.getHeight(), tx.getHash().toHex());
                CoinData coinData = tx.getCoinDataInstance();
                StableRemoveLiquidityDTO dto = stableRemoveLiquidityHandler.getStableRemoveLiquidityInfo(chainId, coinData);
                String pairAddress = AddressTool.getStringAddressByBytes(dto.getPairAddress());
                IStablePair stablePair = iPairFactory.getStablePair(pairAddress);
                //StableSwapPairPo pair = stablePair.getPair();
                //NerveToken[] coins = pair.getCoins();
                // 从执行结果中提取业务数据
                SwapResult result = swapResultMap.get(tx.getHash().toHex());
                //result.setExtend(JSONUtils.obj2json(Arrays.asList(coins).stream().map(c -> c.str()).collect(Collectors.toList())));
                swapExecuteResultStorageService.save(chainId, tx.getHash(), result);
                if (!result.isSuccess()) {
                    continue;
                }
                StableRemoveLiquidityBus bus = SwapDBUtil.getModel(HexUtil.decode(result.getBusiness()), StableRemoveLiquidityBus.class);
                //logger.info("[{}]remove bus: {}", blockHeader.getHeight(), bus.toString());
                // 更新Pair的资金池和发行总量
                stablePair.update(bus.getLiquidity().negate(), SwapUtils.convertNegate(bus.getAmounts()), bus.getBalances(), blockHeader.getHeight(), blockHeader.getTime());
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
                stablePair.rollback(bus.getLiquidity().negate(), bus.getBalances(), bus.getPreBlockHeight(), bus.getPreBlockTime());
                swapExecuteResultStorageService.delete(chainId, tx.getHash());
                logger.info("[{}][rollback] Stable Swap Remove Liquidity, hash: {}", blockHeader.getHeight(), tx.getHash().toHex());
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
                StableRemoveLiquidityData txData = new StableRemoveLiquidityData();
                txData.parse(tx.getTxData(), 0);
                CoinData coinData = tx.getCoinDataInstance();
                StableRemoveLiquidityDTO dto = stableRemoveLiquidityHandler.getStableRemoveLiquidityInfo(chainId, coinData);
                if (chainId == 9 && SwapConstant.UNAVAILABLE_STABLE_PAIR.equals(dto.getPairAddress())) {
                    logger.error("UNAVAILABLE_STABLE_PAIR! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.PAIR_ADDRESS_ERROR.getCode();
                    continue;
                }
                SwapUtils.calStableRemoveLiquidityBusinessP21(chainId, iPairFactory, dto.getLiquidity(), txData.getIndexs(), dto.getPairAddress(), txData.getTo());
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

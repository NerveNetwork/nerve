package network.nerve.swap.tx.v1.stable;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.cache.StableSwapPairCache;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.help.LedgerAssetRegisterHelper;
import network.nerve.swap.help.SwapHelper;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.po.stable.StableSwapPairPo;
import network.nerve.swap.model.txdata.stable.CreateStablePairData;
import network.nerve.swap.storage.SwapExecuteResultStorageService;
import network.nerve.swap.utils.SwapUtils;

import java.util.*;

/**
 * @author Niels
 */
@Component("CreateStablePairTxProcessorV1")
public class CreateStablePairTxProcessor implements TransactionProcessor {

    @Autowired
    private StableSwapPairCache stableSwapPairCache;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private LedgerAssetCache ledgerAssetCache;
    @Autowired
    private LedgerAssetRegisterHelper ledgerAssetRegisterHelper;
    @Autowired
    private SwapExecuteResultStorageService swapExecuteResultStorageService;
    @Autowired
    private SwapHelper swapHelper;

    @Override
    public int getType() {
        return TxType.CREATE_SWAP_PAIR_STABLE_COIN;
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
        C1:
        for (Transaction tx : txs) {
            try {
                if (tx.getType() != getType()) {
                    logger.error("Tx type is wrong! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.DATA_ERROR.getCode();
                    continue;
                }
                CreateStablePairData txData = new CreateStablePairData();
                txData.parse(tx.getTxData(), 0);
                NerveToken[] coins = txData.getCoins();
                int length = coins.length;
                if (length < 2) {
                    logger.error("INVALID_COINS! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.INVALID_COINS.getCode();
                    continue;
                }
                String symbol = txData.getSymbol();
                if (StringUtils.isNotBlank(symbol) && !SwapUtils.validTokenNameOrSymbol(symbol, swapHelper.isSupportProtocol26())) {
                    logger.error("INVALID_SYMBOL! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.INVALID_SYMBOL.getCode();
                    continue;
                }
                Set<NerveToken> coinSet = new HashSet<>();
                for (int i = 0; i < length; i++) {
                    NerveToken token = coins[i];
                    if (!coinSet.add(token)) {
                        logger.error("IDENTICAL_TOKEN! hash-{}", tx.getHash().toHex());
                        failsList.add(tx);
                        errorCode = SwapErrorCode.IDENTICAL_TOKEN.getCode();
                        continue C1;
                    }
                    LedgerAssetDTO asset = ledgerAssetCache.getLedgerAsset(chainId, token);
                    if (asset == null) {
                        logger.error("Ledger asset not exist! hash-{}", tx.getHash().toHex());
                        failsList.add(tx);
                        errorCode = SwapErrorCode.LEDGER_ASSET_NOT_EXIST.getCode();
                        continue C1;
                    }
                    if (asset.getDecimalPlace() > 18) {
                        logger.error("coin_decimal_exceeded! hash-{}", tx.getHash().toHex());
                        failsList.add(tx);
                        errorCode = SwapErrorCode.COIN_DECIMAL_EXCEEDED.getCode();
                        continue C1;
                    }
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
                logger.info("[commit] Swap Stable Create Pair, hash: {}", tx.getHash().toHex());
                SwapResult result = swapResultMap.get(tx.getHash().toHex());
                swapExecuteResultStorageService.save(chainId, tx.getHash(), result);
                if (!result.isSuccess()) {
                    continue;
                }
                byte[] stablePairAddressBytes = AddressTool.getAddress(tx.getHash().getBytes(), chainId, SwapConstant.STABLE_PAIR_ADDRESS_TYPE);
                String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);
                CreateStablePairData txData = new CreateStablePairData();
                txData.parse(tx.getTxData(), 0);
                LedgerAssetDTO dto = ledgerAssetRegisterHelper.lpAssetRegForStable(chainId, stablePairAddress, txData.getCoins(), txData.getSymbol());
                logger.info("[commit] Swap Stable Create Pair Info: {}-{}, symbol: {}, decimals: {}, stablePairAddress: {}", dto.getChainId(), dto.getAssetId(), dto.getAssetSymbol(), dto.getDecimalPlace(), stablePairAddress);
                // load cache
                stableSwapPairCache.get(stablePairAddress);
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
                byte[] stablePairAddressBytes = AddressTool.getAddress(tx.getHash().getBytes(), chainId, SwapConstant.STABLE_PAIR_ADDRESS_TYPE);
                String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);
                CreateStablePairData txData = new CreateStablePairData();
                txData.parse(tx.getTxData(), 0);
                StableSwapPairPo pairPO = ledgerAssetRegisterHelper.deleteLpAssetForStable(chainId, stablePairAddress);
                logger.info("[rollback] Remove Stable Pair: {}-{}", pairPO.getTokenLP().getChainId(), pairPO.getTokenLP().getAssetId());
                swapExecuteResultStorageService.delete(chainId, tx.getHash());
                // remove cache
                stableSwapPairCache.remove(stablePairAddress);
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }
}

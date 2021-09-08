package network.nerve.swap.tx.v1;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.swap.cache.LedgerAssetCache;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.help.LedgerAssetRegisterHelper;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.ValidaterResult;
import network.nerve.swap.model.bo.SwapResult;
import network.nerve.swap.model.dto.LedgerAssetDTO;
import network.nerve.swap.model.po.SwapPairPO;
import network.nerve.swap.model.txdata.CreatePairData;
import network.nerve.swap.storage.SwapExecuteResultStorageService;
import network.nerve.swap.tx.v1.vals.CreatePairTxValidater;
import network.nerve.swap.utils.SwapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 */
@Component("CreatePairTxProcessorV1")
public class CreatePairTxProcessor implements TransactionProcessor {

    @Autowired
    private CreatePairTxValidater validater;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private LedgerAssetRegisterHelper ledgerAssetRegisterHelper;
    @Autowired
    private SwapExecuteResultStorageService swapExecuteResultStorageService;
    @Autowired
    private LedgerAssetCache ledgerAssetCache;

    @Override
    public int getType() {
        return TxType.CREATE_SWAP_PAIR;
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
                CreatePairData txData = new CreatePairData();
                txData.parse(tx.getTxData(), 0);
                NerveToken token0 = txData.getToken0();
                NerveToken token1 = txData.getToken1();
                if (token0.equals(token1)) {
                    logger.error("Identical addresses! hash-{}", tx.getHash().toHex());
                    failsList.add(tx);
                    errorCode = SwapErrorCode.IDENTICAL_ADDRESSES.getCode();
                    continue;
                }
                if (ledgerAssetCache.getLedgerAsset(chainId, token0) == null) {
                    logger.error("Ledger asset not exist! hash-{}", tx.getHash().toHex());
                    throw new NulsException(SwapErrorCode.LEDGER_ASSET_NOT_EXIST);
                }
                if (ledgerAssetCache.getLedgerAsset(chainId, token1) == null) {
                    logger.error("Ledger asset not exist! hash-{}", tx.getHash().toHex());
                    throw new NulsException(SwapErrorCode.LEDGER_ASSET_NOT_EXIST);
                }

                String address = SwapUtils.getStringPairAddress(chainId, token0, token1);
                ValidaterResult result = validater.isPairNotExist(address);
                if (result.isFailed()) {
                    Log.error(result.getErrorCode().getMsg());
                    failsList.add(tx);
                    errorCode = result.getErrorCode().getCode();
                    continue;
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
                logger.info("[commit] Swap Create Pair, hash: {}", tx.getHash().toHex());
                SwapResult result = swapResultMap.get(tx.getHash().toHex());
                swapExecuteResultStorageService.save(chainId, tx.getHash(), result);
                if (!result.isSuccess()) {
                    continue;
                }
                CreatePairData txData = new CreatePairData();
                txData.parse(tx.getTxData(), 0);
                LedgerAssetDTO dto = ledgerAssetRegisterHelper.lpAssetReg(chainId, txData.getToken0(), txData.getToken1());
                logger.info("[commit] Create Pair Info: {}-{}, symbol: {}, decimals: {}", dto.getChainId(), dto.getAssetId(), dto.getAssetSymbol(), dto.getDecimalPlace());
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
                CreatePairData txData = new CreatePairData();
                txData.parse(tx.getTxData(), 0);
                SwapPairPO pairPO = ledgerAssetRegisterHelper.deleteLpAsset(chainId, txData.getToken0(), txData.getToken1());
                logger.info("[rollback] Remove Pair: {}-{}", pairPO.getTokenLP().getChainId(), pairPO.getTokenLP().getAssetId());
                swapExecuteResultStorageService.delete(chainId, tx.getHash());
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
        return true;
    }
}

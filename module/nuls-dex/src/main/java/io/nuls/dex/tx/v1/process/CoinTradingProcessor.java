package io.nuls.dex.tx.v1.process;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.dex.manager.DexManager;
import io.nuls.dex.model.bean.AssetInfo;
import io.nuls.dex.model.po.CoinTradingPo;
import io.nuls.dex.model.txData.CoinTrading;
import io.nuls.dex.storage.CoinTradingStorageService;
import io.nuls.dex.tx.v1.validate.CoinTradingValidator;
import io.nuls.dex.util.LoggerUtil;

import java.util.List;
import java.util.Map;

/**
 * 创建币对交易处理器
 */
@Component("CoinTradingProcessorV1")
public class CoinTradingProcessor implements TransactionProcessor {

    @Autowired
    private CoinTradingValidator validator;
    @Autowired
    private CoinTradingStorageService coinTradingStorageService;
    @Autowired
    private DexManager dexManager;

    @Override
    public int getType() {
        return TxType.COIN_TRADING;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        return validator.validateTxs(txs);
    }


    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        //底层保存币对信息
        //创建币对盘口容器，并缓存
        CoinTrading coinTrading;
        AssetInfo assetInfo;
        for (int i = 0; i < txs.size(); i++) {
            try {
                coinTrading = new CoinTrading();
                coinTrading.parse(new NulsByteBuffer(txs.get(i).getTxData()));
                CoinTradingPo tradingPo = new CoinTradingPo(txs.get(i).getHash(), coinTrading);
                assetInfo = dexManager.getAssetInfo(AssetInfo.toKey(tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId()));
                tradingPo.setBaseDecimal((byte) assetInfo.getDecimal());
                assetInfo = dexManager.getAssetInfo(AssetInfo.toKey(tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId()));
                tradingPo.setQuoteDecimal((byte) assetInfo.getDecimal());

                coinTradingStorageService.save(tradingPo);
                dexManager.addCoinTrading(tradingPo);
            } catch (NulsException e) {
                LoggerUtil.dexLog.error("Failure to CoinTrading commit, hash:" + txs.get(i).getHash().toHex());
                LoggerUtil.dexLog.error(e);
                return false;
            } catch (Exception e) {
                LoggerUtil.dexLog.error("Failure to CoinTrading commit, hash:" + txs.get(i).getHash().toHex());
                LoggerUtil.dexLog.error(e);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        CoinTrading coinTrading;

        for (int i = txs.size() - 1; i >= 0; i--) {
            try {
                coinTrading = new CoinTrading();
                coinTrading.parse(new NulsByteBuffer(txs.get(i).getTxData()));
                CoinTradingPo tradingPo = new CoinTradingPo(txs.get(i).getHash(), coinTrading);
                dexManager.deleteCoinTrading(tradingPo);
                coinTradingStorageService.delete(tradingPo);
            } catch (Exception e) {
                LoggerUtil.dexLog.error("Failure to CoinTrading rollback, hash:" + txs.get(i).getHash().toHex());
                LoggerUtil.dexLog.error(e);
                return false;
            }
        }
        return true;
    }

    /**
     * 注册新币对交易放在第4位处理
     *
     * @return
     */
    @Override
    public int getPriority() {
        return 4;
    }
}

package network.nerve.dex.tx.v1.process;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import network.nerve.dex.manager.DexManager;
import network.nerve.dex.model.bean.AssetInfo;
import network.nerve.dex.model.po.CoinTradingEditInfoPo;
import network.nerve.dex.model.po.CoinTradingPo;
import network.nerve.dex.model.po.EditCoinTradingPo;
import network.nerve.dex.model.txData.CoinTrading;
import network.nerve.dex.storage.CoinTradingStorageService;
import network.nerve.dex.tx.v1.validate.CoinTradingValidator;
import network.nerve.dex.util.LoggerUtil;

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
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return true;
    }


    public void coinTradingCommit(Transaction tx) {
        //底层保存币对信息
        //创建币对盘口容器，并缓存
        CoinTrading coinTrading;
        AssetInfo assetInfo;
        try {
            coinTrading = new CoinTrading();
            coinTrading.parse(new NulsByteBuffer(tx.getTxData()));
            CoinTradingPo tradingPo = new CoinTradingPo(tx.getHash(), coinTrading);

            //查询币对信息
            assetInfo = dexManager.getAssetInfo(AssetInfo.toKey(tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId()));
            tradingPo.setBaseDecimal((byte) assetInfo.getDecimal());
            assetInfo = dexManager.getAssetInfo(AssetInfo.toKey(tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId()));
            tradingPo.setQuoteDecimal((byte) assetInfo.getDecimal());
            //持久化
            coinTradingStorageService.save(tradingPo);
            //同时持久化一个EditCoinTrading记录，供EditCoinTradingProcessor回滚时使用
            EditCoinTradingPo editCoinTrading = new EditCoinTradingPo();
            editCoinTrading.setTxHash(tx.getHash());
            editCoinTrading.setScaleBaseDecimal(coinTrading.getScaleBaseDecimal());
            editCoinTrading.setScaleQuoteDecimal(coinTrading.getScaleQuoteDecimal());
            editCoinTrading.setMinBaseAmount(coinTrading.getMinBaseAmount());
            editCoinTrading.setMinQuoteAmount(coinTrading.getMinQuoteAmount());
            CoinTradingEditInfoPo editInfoPo = new CoinTradingEditInfoPo(editCoinTrading);
            coinTradingStorageService.saveEditInfo(tradingPo.getHash(), editInfoPo);
            //添加到缓存
            dexManager.addCoinTrading(tradingPo);
        } catch (NulsException e) {
            LoggerUtil.dexLog.error("Failure to CoinTrading commit, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.dexLog.error("Failure to CoinTrading commit, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e);
        }
    }

    public void coinTradingRollback(Transaction tx) {
        try {
            CoinTrading coinTrading = new CoinTrading();
            coinTrading.parse(new NulsByteBuffer(tx.getTxData()));
            CoinTradingPo tradingPo = new CoinTradingPo(tx.getHash(), coinTrading);
            dexManager.deleteCoinTrading(tradingPo);
            coinTradingStorageService.delete(tradingPo);
            coinTradingStorageService.deleteEditInfo(tradingPo.getHash());
        } catch (Exception e) {
            LoggerUtil.dexLog.error("Failure to CoinTrading rollback, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e);
        }
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

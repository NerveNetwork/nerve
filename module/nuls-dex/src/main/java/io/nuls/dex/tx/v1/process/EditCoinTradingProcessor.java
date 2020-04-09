package io.nuls.dex.tx.v1.process;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.dex.context.DexConfig;
import io.nuls.dex.context.DexErrorCode;
import io.nuls.dex.manager.DexManager;
import io.nuls.dex.manager.TradingContainer;
import io.nuls.dex.model.bean.AssetInfo;
import io.nuls.dex.model.po.CoinTradingPo;
import io.nuls.dex.model.txData.EditCoinTrading;
import io.nuls.dex.storage.CoinTradingStorageService;
import io.nuls.dex.util.LoggerUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

@Component("editCoinTradingProcessorV1")
public class EditCoinTradingProcessor implements TransactionProcessor {
    @Autowired
    private DexConfig dexConfig;
    @Autowired
    private DexManager dexManager;
    @Autowired
    private CoinTradingStorageService coinTradingStorageService;

    @Override
    public int getType() {
        return 232;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> map, BlockHeader blockHeader) {
        //存放验证不通过的交易
        List<Transaction> invalidTxList = new ArrayList<>();
        ErrorCode errorCode = null;

        Transaction tx;
        EditCoinTrading c;
        CoinTradingPo tradingPo;
        CoinFrom from;

        for (int i = 0; i < txs.size(); i++) {
            tx = txs.get(i);
            try {
                c = new EditCoinTrading();
                c.parse(new NulsByteBuffer(tx.getTxData()));
                TradingContainer container = dexManager.getTradingContainer(c.getTradingHash().toHex());
                if (container == null) {
                    throw new NulsException(CommonCodeConstanst.DATA_NOT_FOUND, "coinTrading not exist");
                }
                tradingPo = container.getCoinTrading();
                from = tx.getCoinDataInstance().getFrom().get(0);
                //修改地址和创建地址要一致
                if (!Arrays.equals(from.getAddress(), tradingPo.getAddress())) {
                    throw new NulsException(DexErrorCode.DATA_ERROR, "create address error");
                }
                //查询币对信息的币种是否存在
                AssetInfo baseAsset = dexManager.getAssetInfo(AssetInfo.toKey(tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId()));
                AssetInfo quoteAsset = dexManager.getAssetInfo(AssetInfo.toKey(tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId()));
                //检查小数位数是否正确
                if (c.getScaleBaseDecimal() > baseAsset.getDecimal()) {
                    throw new NulsException(DexErrorCode.DATA_ERROR, "base coin minDecimal error");
                }
                if (c.getScaleQuoteDecimal() > quoteAsset.getDecimal()) {
                    throw new NulsException(DexErrorCode.DATA_ERROR, "quote coin minDecimal error");
                }

                if (c.getMinTradingAmount().compareTo(BigInteger.ZERO) <= 0) {
                    throw new NulsException(DexErrorCode.DATA_ERROR, "min tradingAmount error");
                }
                //单笔最小交易数是否小于最小支持小数位数
                BigDecimal minDecimalValue = new BigDecimal(1);
                minDecimalValue = minDecimalValue.movePointRight(baseAsset.getDecimal() - c.getScaleBaseDecimal());
                BigDecimal minTradingAmount = new BigDecimal(c.getMinTradingAmount());
                BigDecimal divideValue = minTradingAmount.divide(minDecimalValue, 2, RoundingMode.DOWN);
                if (minTradingAmount.compareTo(minDecimalValue) < 0 || divideValue.doubleValue() > divideValue.longValue()) {
                    throw new NulsException(DexErrorCode.DATA_ERROR, "the minTradingAmount is not supported by the coin");
                }

            } catch (NulsException e) {
                LoggerUtil.dexLog.error(e);
                errorCode = e.getErrorCode();
                invalidTxList.add(tx);
            }
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("txList", invalidTxList);
        resultMap.put("errorCode", errorCode);
        return resultMap;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        EditCoinTrading c;
        CoinTradingPo tradingPo;
        for (int i = 0; i < txs.size(); i++) {
            try {
                c = new EditCoinTrading();
                c.parse(new NulsByteBuffer(txs.get(i).getTxData()));
                TradingContainer container = dexManager.getTradingContainer(c.getTradingHash().toHex());
                tradingPo = container.getCoinTrading();

                tradingPo.setScaleQuoteDecimal(c.getScaleQuoteDecimal());
                tradingPo.setScaleBaseDecimal(c.getScaleBaseDecimal());
                tradingPo.setMinTradingAmount(c.getMinTradingAmount());

                coinTradingStorageService.save(tradingPo);
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

        Transaction tx;
        for (int i = txs.size() - 1; i >= 0; i--) {

        }
        return true;
    }
}

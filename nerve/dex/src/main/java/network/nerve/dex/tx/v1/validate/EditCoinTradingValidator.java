package network.nerve.dex.tx.v1.validate;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.dex.context.DexErrorCode;
import network.nerve.dex.manager.DexManager;
import network.nerve.dex.manager.TradingContainer;
import network.nerve.dex.model.bean.AssetInfo;
import network.nerve.dex.model.po.CoinTradingPo;
import network.nerve.dex.model.txData.EditCoinTrading;
import network.nerve.dex.util.LoggerUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

@Component
public class EditCoinTradingValidator {

    @Autowired
    private DexManager dexManager;

    public Map<String, Object> validateTxs(List<Transaction> txs) {
        //Store transactions that fail verification
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
                //Modifying and creating addresses must be consistent
                if (!Arrays.equals(from.getAddress(), tradingPo.getAddress())) {
                    throw new NulsException(DexErrorCode.DATA_ERROR, "create address error");
                }
                //Does the currency used to query currency pair information exist
                AssetInfo baseAsset = dexManager.getAssetInfo(AssetInfo.toKey(tradingPo.getBaseAssetChainId(), tradingPo.getBaseAssetId()));
                AssetInfo quoteAsset = dexManager.getAssetInfo(AssetInfo.toKey(tradingPo.getQuoteAssetChainId(), tradingPo.getQuoteAssetId()));
                //Check if the decimal places are correct
                if (c.getScaleBaseDecimal() > baseAsset.getDecimal()) {
                    throw new NulsException(DexErrorCode.DATA_ERROR, "base coin minDecimal error");
                }
                if (c.getScaleQuoteDecimal() > quoteAsset.getDecimal()) {
                    throw new NulsException(DexErrorCode.DATA_ERROR, "quote coin minDecimal error");
                }

                if (c.getMinBaseAmount().compareTo(BigInteger.ZERO) <= 0) {
                    throw new NulsException(DexErrorCode.DATA_ERROR, "min tradingAmount error");
                }
                //Is the minimum number of transactions per transaction less than the minimum supported decimal places
                BigDecimal minDecimalValue = new BigDecimal(1);
                minDecimalValue = minDecimalValue.movePointRight(baseAsset.getDecimal() - c.getScaleBaseDecimal());
                BigDecimal minTradingAmount = new BigDecimal(c.getMinBaseAmount());
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
        resultMap.put("errorCode", errorCode == null ? null : errorCode.getCode());
        return resultMap;
    }

}

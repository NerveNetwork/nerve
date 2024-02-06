package network.nerve.dex.tx.v1.validate;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.dex.context.DexConfig;
import network.nerve.dex.context.DexContext;
import network.nerve.dex.context.DexErrorCode;
import network.nerve.dex.manager.DexManager;
import network.nerve.dex.model.bean.AssetInfo;
import network.nerve.dex.model.txData.CoinTrading;
import network.nerve.dex.rpc.call.LedgerCall;
import network.nerve.dex.storage.CoinTradingStorageService;
import network.nerve.dex.util.DexUtil;
import network.nerve.dex.util.LoggerUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

/**
 * Create a transaction validator for coin pairs
 */
@Component
public class CoinTradingValidator {

    @Autowired
    private CoinTradingStorageService coinTradingStorageService;
    @Autowired
    private DexManager dexManager;
    @Autowired
    private DexConfig dexConfig;

    /**
     * Verification of the legality of all created coin pair transactions in this block packaging process
     * Return transactions that fail verification
     * Definition of Same Currency Pairs：
     * case 1:  o1(btc/nuls)  o2(btc/nuls)
     * case 2:  o1(btc/nuls)  o2(nuls/btc)
     *
     * @param txs
     * @return
     */
    public Map<String, Object> validateTxs(List<Transaction> txs) {
        //Store currency pair data for verified transactions
        List<CoinTrading> coinTradingList = new ArrayList<>();
        //Store transactions that fail verification
        List<Transaction> invalidTxList = new ArrayList<>();
        ErrorCode errorCode = null;
        Transaction tx;
        CoinTrading c1;

        for (int i = 0; i < txs.size(); i++) {
            tx = txs.get(i);
            c1 = new CoinTrading();
            try {
                c1.parse(new NulsByteBuffer(tx.getTxData()));
                //Firstly, check the legality of the coin pair information
                validate(tx.getCoinDataInstance().getFrom().get(0), tx.getCoinDataInstance().getTo().get(0), c1);
                //Re check if the same coin pair has already been stored
                if (dexManager.containsCoinTrading(DexUtil.toCoinTradingKey(c1.getBaseAssetChainId(), c1.getBaseAssetId(), c1.getQuoteAssetChainId(), c1.getQuoteAssetId()))
//                        ||
//                        dexManager.containsCoinTrading(DexUtil.toCoinTradingKey(c1.getQuoteAssetChainId(), c1.getQuoteAssetId(), c1.getBaseA
//                        ssetChainId(), c1.getBaseAssetId()))
            ) {
                    throw new NulsException(DexErrorCode.COIN_TRADING_EXIST);
                }
                //Finally, check if there are identical coin pairs in this packaging
                for (CoinTrading c2 : coinTradingList) {
                    validate(c1, c2);
                }
                //Put verified transaction pairs into the set
                coinTradingList.add(c1);
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

    /**
     * Basic information verification of transaction pairs
     *
     * @param c
     * @return
     */
    private void validate(CoinFrom from, CoinTo to, CoinTrading c) throws NulsException {
        //The base currency and transaction currency of a currency pair cannot be the same currency
        if (c.getBaseAssetChainId() == c.getQuoteAssetChainId() &&
                c.getBaseAssetId() == c.getQuoteAssetId()) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "base coin can not equals quote coin");
        }
        //Verify if the creation address is consistent
        if (c.getAddress() == null || !Arrays.equals(from.getAddress(), c.getAddress())) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "create address error");
        }
        //Verify the creation of coin pair transactions and whether the paid destruction coins are sufficient
        if (!Arrays.equals(to.getAddress(), DexContext.sysFeeAddress)) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "destroy coin not enough");
        }
        if (to.getAssetsChainId() != dexConfig.getChainId() || to.getAssetsId() != dexConfig.getAssetId() ||
                to.getAmount().compareTo(DexContext.createTradingAmount) < 0) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "destroy coin not enough");
        }
        getAllAssets(dexConfig.getChainId());
        //Does the currency used to query currency pair information exist
        AssetInfo baseAsset = dexManager.getAssetInfo(AssetInfo.toKey(c.getBaseAssetChainId(), c.getBaseAssetId()));
        AssetInfo quoteAsset = dexManager.getAssetInfo(AssetInfo.toKey(c.getQuoteAssetChainId(), c.getQuoteAssetId()));
        if (baseAsset == null || quoteAsset == null) {
            throw new NulsException(DexErrorCode.DATA_NOT_FOUND, "coin info can not find");
        }
        //Check if the decimal places are correct
        if (c.getScaleBaseDecimal() > baseAsset.getDecimal()) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "base coin minDecimal error");
        }
        if (c.getScaleQuoteDecimal() > quoteAsset.getDecimal()) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "quote coin minDecimal error");
        }

        if (c.getMinBaseAmount().compareTo(BigInteger.TEN) < 0) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "base coin min tradingAmount error");
        }
        if (c.getMinQuoteAmount().compareTo(BigInteger.TEN) < 0) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "quote coin min tradingAmount error");
        }

        //Is the minimum number of transactions per transaction less than the minimum supported decimal places
        //Verify transaction currency
        BigDecimal minDecimalValue = new BigDecimal(1);
        minDecimalValue = minDecimalValue.movePointRight(baseAsset.getDecimal() - c.getScaleBaseDecimal());
        BigDecimal minTradingAmount = new BigDecimal(c.getMinBaseAmount());
        BigDecimal divideValue = minTradingAmount.divide(minDecimalValue, 2, RoundingMode.DOWN);
        if (minTradingAmount.compareTo(minDecimalValue) < 0 || divideValue.doubleValue() > divideValue.longValue()) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "the minTradingAmount is not supported by the coin");
        }
        //Verify pricing currency
        minDecimalValue = new BigDecimal(1);
        minDecimalValue = minDecimalValue.movePointRight(quoteAsset.getDecimal() - c.getScaleQuoteDecimal());
        minTradingAmount = new BigDecimal(c.getMinQuoteAmount());
        if (minTradingAmount.compareTo(minDecimalValue) < 0) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "the minTradingAmount is not supported by the coin");
        }
    }

    /**
     * Definition of Same Currency Pairs：
     * case 1:  c1(btc/nuls)  c2(btc/nuls)
     * case 2:  c1(btc/nuls)  c2(nuls/btc)
     *
     * @param c1
     * @param c2
     * @return
     */
    private void validate(CoinTrading c1, CoinTrading c2) throws NulsException {
        if (c1.getBaseAssetChainId() == c2.getBaseAssetChainId() &&
                c1.getBaseAssetId() == c2.getBaseAssetId() &&
                c1.getQuoteAssetChainId() == c2.getQuoteAssetChainId() &&
                c1.getQuoteAssetId() == c2.getQuoteAssetId()) {
            throw new NulsException(DexErrorCode.COIN_TRADING_EXIST);
        } else if (c1.getBaseAssetChainId() == c2.getQuoteAssetChainId() &&
                c1.getBaseAssetId() == c2.getQuoteAssetId() &&
                c1.getQuoteAssetChainId() == c2.getBaseAssetChainId() &&
                c1.getQuoteAssetId() == c2.getBaseAssetId()) {
            throw new NulsException(DexErrorCode.COIN_TRADING_EXIST);
        }
    }

    private void getAllAssets(int chainId) {
        Result<List<AssetInfo>> result = LedgerCall.getAllAssets(chainId);
        if (result.isSuccess()) {
            List<AssetInfo> assetInfoList = result.getData();
            for (AssetInfo assetInfo : assetInfoList) {
                dexManager.addAssetInfo(assetInfo);
            }
        }
    }
}

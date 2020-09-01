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
 * 创建币对的交易验证器
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
     * 本次打包区块的中所有创建币对交易的合法性验证
     * 将验证不通过的交易返回
     * 相同币对定义：
     * case 1:  o1(btc/nuls)  o2(btc/nuls)
     * case 2:  o1(btc/nuls)  o2(nuls/btc)
     *
     * @param txs
     * @return
     */
    public Map<String, Object> validateTxs(List<Transaction> txs) {
        //存放验证通过的交易的币对数据
        List<CoinTrading> coinTradingList = new ArrayList<>();
        //存放验证不通过的交易
        List<Transaction> invalidTxList = new ArrayList<>();
        ErrorCode errorCode = null;
        Transaction tx;
        CoinTrading c1;

        for (int i = 0; i < txs.size(); i++) {
            tx = txs.get(i);
            c1 = new CoinTrading();
            try {
                c1.parse(new NulsByteBuffer(tx.getTxData()));
                //首先检测币对信息的合法性
                validate(tx.getCoinDataInstance().getFrom().get(0), tx.getCoinDataInstance().getTo().get(0), c1);
                //再检测是否已经存储了相同币对
                if (dexManager.containsCoinTrading(DexUtil.toCoinTradingKey(c1.getBaseAssetChainId(), c1.getBaseAssetId(), c1.getQuoteAssetChainId(), c1.getQuoteAssetId()))
//                        ||
//                        dexManager.containsCoinTrading(DexUtil.toCoinTradingKey(c1.getQuoteAssetChainId(), c1.getQuoteAssetId(), c1.getBaseA
//                        ssetChainId(), c1.getBaseAssetId()))
            ) {
                    throw new NulsException(DexErrorCode.COIN_TRADING_EXIST);
                }
                //最后检测本次打包是否有相同币对
                for (CoinTrading c2 : coinTradingList) {
                    validate(c1, c2);
                }
                //将验证通过的交易对放入集合中
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
     * 交易对的基础信息验证
     *
     * @param c
     * @return
     */
    private void validate(CoinFrom from, CoinTo to, CoinTrading c) throws NulsException {
        //币对的基础币种和交易币种不能是同一币种
        if (c.getBaseAssetChainId() == c.getQuoteAssetChainId() &&
                c.getBaseAssetId() == c.getQuoteAssetId()) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "base coin can not equals quote coin");
        }
        //验证创建地址是否一致
        if (c.getAddress() == null || !Arrays.equals(from.getAddress(), c.getAddress())) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "create address error");
        }
        //验证创建币对交易，缴纳的销毁币是否足够
        if (!Arrays.equals(to.getAddress(), DexContext.sysFeeAddress)) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "destroy coin not enough");
        }
        if (to.getAssetsChainId() != dexConfig.getChainId() || to.getAssetsId() != dexConfig.getAssetId() ||
                to.getAmount().compareTo(DexContext.createTradingAmount) < 0) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "destroy coin not enough");
        }
        getAllAssets(dexConfig.getChainId());
        //查询币对信息的币种是否存在
        AssetInfo baseAsset = dexManager.getAssetInfo(AssetInfo.toKey(c.getBaseAssetChainId(), c.getBaseAssetId()));
        AssetInfo quoteAsset = dexManager.getAssetInfo(AssetInfo.toKey(c.getQuoteAssetChainId(), c.getQuoteAssetId()));
        if (baseAsset == null || quoteAsset == null) {
            throw new NulsException(DexErrorCode.DATA_NOT_FOUND, "coin info can not find");
        }
        //检查小数位数是否正确
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

        //单笔最小交易数是否小于最小支持小数位数
        //验证交易币
        BigDecimal minDecimalValue = new BigDecimal(1);
        minDecimalValue = minDecimalValue.movePointRight(baseAsset.getDecimal() - c.getScaleBaseDecimal());
        BigDecimal minTradingAmount = new BigDecimal(c.getMinBaseAmount());
        BigDecimal divideValue = minTradingAmount.divide(minDecimalValue, 2, RoundingMode.DOWN);
        if (minTradingAmount.compareTo(minDecimalValue) < 0 || divideValue.doubleValue() > divideValue.longValue()) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "the minTradingAmount is not supported by the coin");
        }
        //验证计价币
        minDecimalValue = new BigDecimal(1);
        minDecimalValue = minDecimalValue.movePointRight(quoteAsset.getDecimal() - c.getScaleQuoteDecimal());
        minTradingAmount = new BigDecimal(c.getMinQuoteAmount());
        if (minTradingAmount.compareTo(minDecimalValue) < 0) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "the minTradingAmount is not supported by the coin");
        }
    }

    /**
     * 相同币对定义：
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

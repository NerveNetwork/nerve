package network.nerve.dex.tx.v1.validate;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.*;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import network.nerve.dex.context.DexConfig;
import network.nerve.dex.context.DexConstant;
import network.nerve.dex.context.DexContext;
import network.nerve.dex.context.DexErrorCode;
import network.nerve.dex.manager.DexManager;
import network.nerve.dex.manager.TradingContainer;
import network.nerve.dex.model.po.CoinTradingPo;
import network.nerve.dex.model.txData.TradingOrder;
import network.nerve.dex.storage.TradingOrderStorageService;
import network.nerve.dex.util.LoggerUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

/**
 * User order authorization validator
 * 1.Verify the existence of currency pairs
 */
@Component
public class TradingOrderValidator {

    @Autowired
    private DexManager dexManager;
    @Autowired
    private DexConfig config;
    @Autowired
    private TradingOrderStorageService tradingOrderStorageService;

    public Map<String, Object> validateTxs(List<Transaction> txs) {
//        long time1, time2;
//        time1 = System.currentTimeMillis();
        //Store transactions that fail verification
        List<Transaction> invalidTxList = new ArrayList<>();
        ErrorCode errorCode = null;
        TradingOrder order;
        Transaction tx;
        long blockHeight = tradingOrderStorageService.getHeight();
        for (int i = 0; i < txs.size(); i++) {
            tx = txs.get(i);
            try {
                order = new TradingOrder();
                order.parse(new NulsByteBuffer(tx.getTxData()));
                validate(order, tx.getCoinDataInstance(), blockHeight);
            } catch (NulsException e) {
                LoggerUtil.dexLog.error(e);
                LoggerUtil.dexLog.error("txHash: " + tx.getHash().toHex());
                errorCode = e.getErrorCode();
                invalidTxList.add(tx);
            }
        }
//        time2 = System.currentTimeMillis();
//        if (time2 - time1 > 50) {
//            LoggerUtil.dexLog.info("----TradingOrderValidator----, txCount:{}, use:{} ", blockHeader.getHeight(), txs.size(), (time2 - time1));
//        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("txList", invalidTxList);
        resultMap.put("errorCode", errorCode == null ? null : errorCode.getCode());
        return resultMap;
    }

    /**
     * Determine if the dividend address is correct
     * Determine if the dividend ratio is correct(1000 - 10000)
     * Determine whether the currency pair transaction of the order exists
     * validatecoinFromIs the asset in the order equal to the asset on the order placement order
     * Determine the minimum transaction amount
     *
     * @param order
     * @return
     */
    private void validate(TradingOrder order, CoinData coinData, long blockHeight) throws NulsException {
        //Verify the legality of transaction data
        if (order.getType() != DexConstant.TRADING_ORDER_BUY_TYPE && order.getType() != DexConstant.TRADING_ORDER_SELL_TYPE) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "tradingOrder type error");
        }

        if (order.getPrice().compareTo(BigInteger.ZERO) <= 0) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "price error");
        }
        if (order.getAmount().compareTo(BigInteger.ZERO) <= 0) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "orderAmount error");
        }
        if (Arrays.equals(DexContext.sysFeeAddress, order.getAddress())) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "sysFeeAddress can't create tradingOrder");
        }
        if (order.getFeeAddress() != null) {
            if (!AddressTool.validNormalAddress(order.getFeeAddress(), config.getChainId())) {
                throw new NulsException(DexErrorCode.DATA_ERROR, "feeAddress error");
            }
        }
        if (order.getFeeScale() > 98) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "feeScale error");
        }
        //judgefromIs the address inside consistent with the entrusted address
        for (CoinFrom from : coinData.getFrom()) {
            if (!Arrays.equals(from.getAddress(), order.getAddress())) {
                throw new NulsException(DexErrorCode.ACCOUNT_VALID_ERROR);
            }
        }

        CoinTo coinTo = coinData.getTo().get(0);
        //Verify transactionstoformat
        if (coinTo.getLockTime() != DexConstant.DEX_LOCK_TIME) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "coinTo error");
        }

        //Determine whether the currency pair transaction of the order exists
        String hashHex = HexUtil.encode(order.getTradingHash());
        TradingContainer container = dexManager.getTradingContainer(hashHex);
        if (container == null) {
            throw new NulsException(DexErrorCode.DATA_NOT_FOUND, "coinTrading not exist");
        }

        //judgecoinToAre the assets in it related toorderMatching the quantity of orders
        CoinTradingPo coinTrading = container.getCoinTrading();
        if (blockHeight > DexContext.priceSkipHeight) {
            BigDecimal one = BigDecimal.ONE;
            one = one.movePointRight(coinTrading.getQuoteDecimal()).movePointLeft(coinTrading.getScaleQuoteDecimal());
            if (order.getPrice().compareTo(one.toBigInteger()) < 0) {
                throw new NulsException(DexErrorCode.DATA_ERROR, "price error");
            }
        } else {
            if (order.getPrice().compareTo(coinTrading.getMinQuoteAmount()) < 0) {
                throw new NulsException(DexErrorCode.BELOW_TRADING_MIN_SIZE);
            }
        }

        //Verify minimum transaction amount
        //price * Minimum trading volume = Actual transaction amount
        BigDecimal price = new BigDecimal(order.getPrice()).movePointLeft(coinTrading.getQuoteDecimal());
        BigDecimal amount = new BigDecimal(order.getAmount()).movePointLeft(coinTrading.getBaseDecimal());
        if (blockHeight > DexContext.priceSkipHeight) {
            amount = amount.multiply(price).setScale(coinTrading.getQuoteDecimal(), RoundingMode.DOWN);
            amount = amount.movePointRight(coinTrading.getQuoteDecimal());
            if (amount.toBigInteger().compareTo(coinTrading.getMinQuoteAmount()) < 0) {
                throw new NulsException(DexErrorCode.BELOW_TRADING_MIN_SIZE);
            }
        }

        if (order.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
            //validatecoinFromIs the asset in the order equal to the asset on the order placement order
            if (coinTo.getAssetsChainId() != coinTrading.getQuoteAssetChainId() ||
                    coinTo.getAssetsId() != coinTrading.getQuoteAssetId()) {
                throw new NulsException(DexErrorCode.ORDER_COIN_NOT_EQUAL);
            }
            //Verify minimum transaction volume
            if (blockHeight > DexContext.skipHeight) {
                if (order.getAmount().compareTo(coinTrading.getMinBaseAmount()) < 0) {
                    throw new NulsException(DexErrorCode.BELOW_TRADING_MIN_SIZE);
                }
            }

            //Calculate the number of convertible transaction currencies
            //Quantity of Valuation Currency / price = Actual number of convertible transaction currencies
            amount = new BigDecimal(coinTo.getAmount()).movePointLeft(coinTrading.getQuoteDecimal());
            amount = amount.divide(price, coinTrading.getBaseDecimal(), RoundingMode.DOWN);
            amount = amount.movePointRight(coinTrading.getBaseDecimal());
            if (amount.toBigInteger().compareTo(order.getAmount()) < 0) {
                LoggerUtil.dexLog.error("-------TradingOrder validate error!");
                LoggerUtil.dexLog.error("-------TradingHash:" + coinTrading.getHash().toHex());
                LoggerUtil.dexLog.error("----------order.type:{},order.price:{}, order.amount:{}, calc amount:{}", order.getType(), order.getPrice(), order.getAmount(), amount);

                throw new NulsException(DexErrorCode.DATA_ERROR, "coinTo amount error");
            }
        } else {
            if (coinTo.getAssetsChainId() != coinTrading.getBaseAssetChainId() ||
                    coinTo.getAssetsId() != coinTrading.getBaseAssetId()) {
                throw new NulsException(DexErrorCode.ORDER_COIN_NOT_EQUAL);
            }
            //Verify minimum transaction volume
            if (order.getAmount().compareTo(coinTrading.getMinBaseAmount()) < 0) {
                LoggerUtil.dexLog.error("-------TradingOrder validate error!");
                LoggerUtil.dexLog.error("-------TradingHash:" + coinTrading.getHash().toHex());
                LoggerUtil.dexLog.error("----------order.type:{},order.price:{}, order.amount:{}, calc amount:{}", order.getType(), order.getPrice(), order.getAmount(), amount);
                throw new NulsException(DexErrorCode.BELOW_TRADING_MIN_SIZE);
            }
            if (coinTo.getAmount().compareTo(order.getAmount()) != 0) {
                throw new NulsException(DexErrorCode.DATA_ERROR, "coinTo amount error");
            }
        }
    }
}

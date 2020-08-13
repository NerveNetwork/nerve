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
import network.nerve.dex.util.LoggerUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

/**
 * 用户挂单委托验证器
 * 1.验证币对是否存在
 */
@Component
public class TradingOrderValidator {

    @Autowired
    private DexManager dexManager;
    @Autowired
    private DexConfig config;

    public Map<String, Object> validateTxs(List<Transaction> txs) {
//        long time1, time2;
//        time1 = System.currentTimeMillis();
        //存放验证不通过的交易
        List<Transaction> invalidTxList = new ArrayList<>();
        ErrorCode errorCode = null;
        TradingOrder order;
        Transaction tx;
        for (int i = 0; i < txs.size(); i++) {
            tx = txs.get(i);
            try {
                order = new TradingOrder();
                order.parse(new NulsByteBuffer(tx.getTxData()));
                validate(order, tx.getCoinDataInstance());
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
     * 判断分红地址是否正确
     * 判断分红比例是否正确(1000 - 10000)
     * 判断订单的币对交易是否存在
     * 验证coinFrom里的资产是否等于挂单委托单资产
     * 判断最小交易额
     *
     * @param order
     * @return
     */
    private void validate(TradingOrder order, CoinData coinData) throws NulsException {
        //验证交易数据合法性
        if (order.getType() != DexConstant.TRADING_ORDER_BUY_TYPE && order.getType() != DexConstant.TRADING_ORDER_SELL_TYPE) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "tradingOrder type error");
        }

        if (order.getPrice().compareTo(BigInteger.ZERO) == 0) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "price error");
        }
        if (order.getAmount().compareTo(BigInteger.ZERO) == 0) {
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
        //判断from里的地址是否和委托地址一致
        for (CoinFrom from : coinData.getFrom()) {
            if (!Arrays.equals(from.getAddress(), order.getAddress())) {
                throw new NulsException(DexErrorCode.ACCOUNT_VALID_ERROR);
            }
        }

        CoinTo coinTo = coinData.getTo().get(0);
        //验证交易to格式
        if (coinTo.getLockTime() != DexConstant.DEX_LOCK_TIME) {
            throw new NulsException(DexErrorCode.DATA_ERROR, "coinTo error");
        }

        //判断订单的币对交易是否存在
        String hashHex = HexUtil.encode(order.getTradingHash());
        TradingContainer container = dexManager.getTradingContainer(hashHex);
        if (container == null) {
            throw new NulsException(DexErrorCode.DATA_NOT_FOUND, "coinTrading not exist");
        }

        //判断coinTo里的资产是否和order订单的数量匹配
        CoinTradingPo coinTrading = container.getCoinTrading();
        if (order.getType() == DexConstant.TRADING_ORDER_BUY_TYPE) {
            //验证coinFrom里的资产是否等于挂单委托单资产
            if (coinTo.getAssetsChainId() != coinTrading.getQuoteAssetChainId() ||
                    coinTo.getAssetsId() != coinTrading.getQuoteAssetId()) {
                throw new NulsException(DexErrorCode.ORDER_COIN_NOT_EQUAL);
            }
            //验证最小交易额，以及最小小数位数
            if (order.getAmount().compareTo(coinTrading.getMinQuoteAmount()) < 0) {
                throw new NulsException(DexErrorCode.BELOW_TRADING_MIN_SIZE);
            }

            //计算可兑换交易币种数量
            //计价货币数量 / 价格 = 实际可兑换交易币种数量
            BigDecimal price = new BigDecimal(order.getPrice()).movePointLeft(coinTrading.getQuoteDecimal());
            BigDecimal amount = new BigDecimal(coinTo.getAmount()).movePointLeft(coinTrading.getQuoteDecimal());
            amount = amount.divide(price, coinTrading.getBaseDecimal(), RoundingMode.DOWN);
            amount = amount.movePointRight(coinTrading.getBaseDecimal());

            if (amount.toBigInteger().compareTo(order.getAmount()) < 0) {
                throw new NulsException(DexErrorCode.DATA_ERROR, "coinTo amount error");
            }

        } else {
            if (coinTo.getAssetsChainId() != coinTrading.getBaseAssetChainId() ||
                    coinTo.getAssetsId() != coinTrading.getBaseAssetId()) {
                throw new NulsException(DexErrorCode.ORDER_COIN_NOT_EQUAL);
            }
            //验证最小交易额
            if (order.getAmount().compareTo(coinTrading.getMinBaseAmount()) < 0) {
                throw new NulsException(DexErrorCode.BELOW_TRADING_MIN_SIZE);
            }
            if (coinTo.getAmount().compareTo(order.getAmount()) != 0) {
                throw new NulsException(DexErrorCode.DATA_ERROR, "coinTo amount error");
            }
        }
    }
}

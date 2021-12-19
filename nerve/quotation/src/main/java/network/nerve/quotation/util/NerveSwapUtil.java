package network.nerve.quotation.util;

import io.nuls.core.model.DoubleUtils;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.bo.QuotationContractCfg;
import network.nerve.quotation.model.bo.SwapLpPriceCfg;
import network.nerve.quotation.rpc.call.QuotationCall;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

public class NerveSwapUtil {

    public static Double getPrice(Chain chain, QuotationContractCfg quContractCfg) {
        SwapLpPriceCfg cfg = new SwapLpPriceCfg(quContractCfg);
        Map<String, Object> pairInfoMap = QuotationCall.getSwapPairByTokenLP(chain, cfg.getLpAssetChainId(), cfg.getLpAssetId());
        if (null == pairInfoMap || pairInfoMap.isEmpty()) {
            return null;
        }
        String totalLP = (String) pairInfoMap.get("totalLP");
        BigInteger lpTotalCount = new BigInteger(totalLP);
        Map<String, Object> po = (Map<String, Object>) pairInfoMap.get("po");
        String key0 = (String) po.get("token0");
        String cfgKey0 = getTokenKey(cfg.getaAssetChainId(), cfg.getaAssetId());
        String reserve0 = (String) pairInfoMap.get("reserve0");
        BigInteger aCount = new BigInteger(reserve0);
        String reserve1 = (String) pairInfoMap.get("reserve1");
        BigInteger bCount = new BigInteger(reserve1);
        if (!cfgKey0.equals(key0)) {
            aCount = new BigInteger(reserve1);
            bCount = new BigInteger(reserve0);
        }
        //三种情况
        BigDecimal lpValue = new BigDecimal(lpTotalCount, cfg.getLpAssetDecimals());
        if (cfg.getaAssetChainId() == cfg.getBaseAssetChainId() && cfg.getaAssetId() == cfg.getBaseAssetId()) {
            //a就是计价资产
            BigDecimal aValue = new BigDecimal(aCount, cfg.getaAssetDecimals()).multiply(BigDecimal.valueOf(2));
            return DoubleUtils.round(DoubleUtils.div(aValue, lpValue).doubleValue(), 2);
        } else if (cfg.getbAssetChainId() == cfg.getBaseAssetChainId() && cfg.getbAssetId() == cfg.getBaseAssetId()) {
            //b就是计价资产
            BigDecimal bValue = new BigDecimal(bCount, cfg.getbAssetDecimals()).multiply(BigDecimal.valueOf(2));
            return DoubleUtils.round(DoubleUtils.div(bValue, lpValue).doubleValue(), 2);
        }
        int decimals = 0;
        int doit = 0;
        if (cfg.getTokenPath()[0].equals(getTokenKey(cfg.getaAssetChainId(), cfg.getaAssetId()))) {
            decimals = cfg.getaAssetDecimals();
            doit = 1;
        } else if (cfg.getTokenPath()[0].equals(getTokenKey(cfg.getbAssetChainId(), cfg.getbAssetId()))) {
            decimals = cfg.getbAssetDecimals();
            doit = 2;
        }
        BigInteger one = BigInteger.ONE.multiply(BigInteger.valueOf((long) Math.pow(10, decimals)));
        Map<String, Object> priceResult = QuotationCall.getSwapPrice(chain, one.toString(), cfg.getTokenPath());
        if (null == priceResult) {
            return null;
        }
        Long amountOutMin = (Long) priceResult.get("amountOutMin");
        if (null == amountOutMin) {
            return null;
        }
        BigDecimal priceValue = new BigDecimal(BigInteger.valueOf(amountOutMin), cfg.getBaseAssetDecimals());
        if (doit == 1) {
            BigDecimal aValue = new BigDecimal(aCount, cfg.getaAssetDecimals());
            aValue = aValue.multiply(priceValue).multiply(BigDecimal.valueOf(2));
            return DoubleUtils.round(DoubleUtils.div(aValue, lpValue).doubleValue(), 2);
        }
        if (doit == 2) {
            BigDecimal bValue = new BigDecimal(bCount, cfg.getbAssetDecimals());
            bValue = bValue.multiply(priceValue).multiply(BigDecimal.valueOf(2));
            return DoubleUtils.round(DoubleUtils.div(bValue, lpValue).doubleValue(), 2);
        }
        return null;
    }

    private static String getTokenKey(int chainId, int assetId) {
        return chainId + "-" + assetId;
    }
}

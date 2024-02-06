package io.nuls.cmd.client.utils;

import io.nuls.cmd.client.processor.ErrorCodeConstants;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.core.rpc.util.RpcCall;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Asset instruments
 * @author lanjinsheng
 * @description
 * @date 2019/11/07
 **/
public class AssetsUtil {

    public static class Asset {
        int assetChainId;
        int assetId;
        String symbol;
        int decimals;

        public int getAssetChainId() {
            return assetChainId;
        }

        public void setAssetChainId(int assetChainId) {
            this.assetChainId = assetChainId;
        }

        public int getAssetId() {
            return assetId;
        }

        public void setAssetId(int assetId) {
            this.assetId = assetId;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public int getDecimals() {
            return decimals;
        }

        public void setDecimals(int decimals) {
            this.decimals = decimals;
        }
    }

    private static Map<String,Asset> ASSET_MAP = new HashMap<>();

    private static Map<String,Asset> ASSET_MAP_FOR_SYMBOL = new HashMap<>();

    static String getAssetKey(int chainId, int assetId) {
        return chainId + "-" + assetId;
    }

    public static Integer getAssetDecimal(int chainId, int assetId) {
        String key = getAssetKey(chainId, assetId);
        if (null == ASSET_MAP.get(key)) {
            initRegisteredChainInfo(chainId);
        }
        return ASSET_MAP.get(key).getDecimals();
    }

    public static Integer getAssetDecimal(String symbol) {
        if (null == ASSET_MAP_FOR_SYMBOL.get(symbol)) {
            return null;
        }
        return ASSET_MAP_FOR_SYMBOL.get(symbol).getDecimals();
    }

    public static void initRegisteredChainInfo(int chainId) {
        try {
            Map<String, Object> data = (Map) request(ModuleE.LG.abbr, "lg_get_all_asset", Map.of("chainId",chainId), null);
            List<Map<String,Object>> list = (List<Map<String, Object>>) data.get("assets");
            list.stream().map(d->{
                Asset r = new Asset();
                r.setAssetChainId((Integer) d.get("assetChainId"));
                r.setAssetId((Integer) d.get("assetId"));
                r.setSymbol(d.get("assetSymbol").toString());
                r.setDecimals((Integer) d.get("decimalPlace"));
                return r;
            }).forEach(d->{
                ASSET_MAP.put(getAssetKey(d.getAssetChainId() ,d.getAssetId()),d);
                ASSET_MAP_FOR_SYMBOL.put(d.getSymbol(),d);
            });
        } catch (NulsException e) {
            Log.error("Failed to initialize asset information,{}",e.getErrorCode(),e);
            System.exit(0);
        }
    }

    public static Object request(String moduleCode, String cmd, Map params, Long timeout) throws NulsException {
        try {
            Response response;
            try {
                if (null == timeout) {
                    response = ResponseMessageProcessor.requestAndResponse(moduleCode, cmd, params);
                } else {
                    response = ResponseMessageProcessor.requestAndResponse(moduleCode, cmd, params, timeout);
                }
            } catch (Exception e) {
                LoggerUtil.logger.error(e);
                throw new NulsException(CommonCodeConstanst.SYS_UNKOWN_EXCEPTION, e.getMessage());
            }
            if (!response.isSuccess()) {
                String comment = response.getResponseComment();
                if (StringUtils.isBlank(comment)) {
                    comment = "";
                }

                String errorCode = response.getResponseErrorCode();
                LoggerUtil.logger.error("Call interface [{}] error, ErrorCode is {}, ResponseComment:{}", cmd, errorCode, response.getResponseComment());
                if (response.getResponseStatus() == Response.FAIL) {
                    //business error
                    if (StringUtils.isBlank(errorCode)) {
                        throw new NulsException(CommonCodeConstanst.SYS_UNKOWN_EXCEPTION, comment);
                    }
                    throw new NulsException(ErrorCode.init(errorCode), comment);
                } else {
                    if (StringUtils.isNotBlank(comment)) {
                        throw new NulsException(CommonCodeConstanst.FAILED, comment);
                    }
                    throw new NulsException(CommonCodeConstanst.SYS_UNKOWN_EXCEPTION, "unknown error");
                }
            }
            Map data = (Map) response.getResponseData();
            return data.get(cmd);
        } catch (Exception e) {
            LoggerUtil.logger.error(e);
            if (e instanceof NulsException) {
                throw (NulsException) e;
            }
            throw new NulsException(ErrorCodeConstants.SYSTEM_ERR, e.getMessage());
        }
    }

}

package network.nerve.dex.rpc;

import io.nuls.base.data.NulsHash;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.CmdAnnotation;
import io.nuls.core.rpc.model.Parameter;
import io.nuls.core.rpc.model.Parameters;
import io.nuls.core.rpc.model.TypeDescriptor;
import io.nuls.core.rpc.model.message.Response;
import network.nerve.dex.context.DexConfig;
import network.nerve.dex.context.DexErrorCode;
import network.nerve.dex.manager.DexManager;
import network.nerve.dex.model.po.CoinTradingPo;
import network.nerve.dex.model.po.TradingOrderPo;
import network.nerve.dex.storage.CoinTradingStorageService;
import network.nerve.dex.storage.TradingOrderStorageService;
import network.nerve.dex.util.LoggerUtil;

import java.util.HashMap;
import java.util.Map;

@Component
public class DexTradingResource extends BaseCmd {

    @Autowired
    private DexConfig dexConfig;
    @Autowired
    private TradingOrderStorageService orderStorageService;
    @Autowired
    private CoinTradingStorageService coinTradingStorageService;
    @Autowired
    private DexManager dexManager;

    @CmdAnnotation(cmd = "dx_getCoinTrading", version = 1.0, description = "get CoinTrading info")
    @Parameters(value = {
            @Parameter(parameterName = "tradingHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "交易对hash")
    })
    public Response getCoinTrading(Map params) {
        try {
            String tradingHash = (String) params.get("tradingHash");
            NulsHash hash = NulsHash.fromHex(tradingHash);
            CoinTradingPo po = coinTradingStorageService.query(hash);

            Map<String, Object> map = new HashMap<>();
            map.put("tradingHash", po.getHash().toHex());
            map.put("quoteAssetChainId", po.getQuoteAssetChainId());
            map.put("quoteAssetId", po.getQuoteAssetId());
            map.put("quoteDecimal", po.getQuoteDecimal());
            map.put("scaleQuoteDecimal", po.getScaleQuoteDecimal());
            map.put("minQuoteAmount", po.getMinQuoteAmount().toString());

            map.put("baseAssetChainId", po.getBaseAssetChainId());
            map.put("baseAssetId", po.getBaseAssetId());
            map.put("baseDecimal", po.getBaseDecimal());
            map.put("scaleBaseDecimal", po.getScaleBaseDecimal());
            map.put("minBaseAmount", po.getMinBaseAmount().toString());

            return success(map);
        } catch (NulsException e) {
            LoggerUtil.dexLog.error(e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.dexLog.error(e);
            return failed(DexErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = "dx_getTradingOrder", version = 1.0, description = "get TradingOrder info")
    @Parameters(value = {
            @Parameter(parameterName = "orderHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "挂单hash")
    })
    public Response getTradingOrderTx(Map params) {
        try {
            String orderHash = (String) params.get("orderHash");
            TradingOrderPo orderPo = orderStorageService.queryFromBack(HexUtil.decode(orderHash));
            if (orderPo == null) {
                throw new NulsException(DexErrorCode.DATA_NOT_FOUND);
            }
            Map<String, Object> map = new HashMap<>();
            map.put("tradingHash", orderPo.getTradingHash().toHex());
            map.put("baseAmount", orderPo.getLeftAmount());
            return success(map);
        } catch (NulsException e) {
            LoggerUtil.dexLog.error(e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.dexLog.error(e);
            return failed(DexErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }
}

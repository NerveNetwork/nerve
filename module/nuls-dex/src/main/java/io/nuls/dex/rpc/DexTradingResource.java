package io.nuls.dex.rpc;

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
import io.nuls.dex.context.DexConfig;
import io.nuls.dex.context.DexErrorCode;
import io.nuls.dex.manager.DexManager;
import io.nuls.dex.model.po.TradingOrderPo;
import io.nuls.dex.storage.TradingOrderStorageService;
import io.nuls.dex.util.LoggerUtil;

import java.util.HashMap;
import java.util.Map;

@Component
public class DexTradingResource extends BaseCmd {

    @Autowired
    private DexConfig dexConfig;
    @Autowired
    private TradingOrderStorageService orderStorageService;
    @Autowired
    private DexManager dexManager;


    @CmdAnnotation(cmd = "dx_getTradingOrder", version = 1.0, description = "create TradingCancelOrderTransaction and broadcast")
    @Parameters(value = {
            @Parameter(parameterName = "orderHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "挂单hash")
    })
    public Response createTradingCancelOrderTx(Map params) {
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

package io.nuls.dex.rpc;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.dex.context.DexErrorCode;
import io.nuls.dex.manager.DexService;
import io.nuls.dex.util.LoggerUtil;
import org.bouncycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DexPackingResource extends BaseCmd {

    @Autowired
    private DexService dexService;

    /**
     * 节点打包时，根据委托挂单交易，生成撮合成交交易
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = "dx_package", version = 1.0, description = "create tradingDealTransaction when packing block")
    @Parameters({
            @Parameter(parameterName = "txList", requestType = @TypeDescriptor(value = List.class, collectionElement = String.class), parameterDes = "区块打包的dex模块所有交易")
    })
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "list", valueType = List.class, valueElement = String.class, description = "创建的账户地址集合"),
    }))
    public Response createTradingDeal(Map params) {
        //解析params参数为本模块的交易
        List<String> list = (List<String>) params.get("list");
        if (list == null) {
            return failed(DexErrorCode.DATA_NOT_FOUND);
        }

        List<Transaction> txList = new ArrayList<>();
        try {
            Transaction tx;
            for (int i = 0; i < list.size(); i++) {
                tx = new Transaction();
                tx.parse(new NulsByteBuffer(Hex.decode(list.get(i))));
                txList.add(tx);
            }
            if (txList.size() > 0) {
                txList = dexService.doPacking(txList);
                list.clear();

                for (int i = 0; i < txList.size(); i++) {
                    list.add(Hex.toHexString(txList.get(i).serialize()));
                }
            }
            Map<String, Object> resultMap = new HashMap<>(2);
            resultMap.put("list", list);
            return success(resultMap);
        } catch (NulsException e) {
            LoggerUtil.dexLog.error(e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.dexLog.error(e);
            return failed(DexErrorCode.DATA_PARSE_ERROR);
        }
    }
}

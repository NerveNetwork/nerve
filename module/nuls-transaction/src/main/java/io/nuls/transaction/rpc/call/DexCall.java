package io.nuls.transaction.rpc.call;

import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.model.bo.Chain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DexCall {

    /**
     * 根据新打包的挂单交易，生成撮合成交交易
     *
     * @param txList
     * @return
     */
    public static List<String> doPacking(Chain chain, List<String> txList) throws NulsException {
        Map<String, Object> param = new HashMap<>(2);
        param.put("list", txList);
        try {
            Map result = (Map) TransactionCall.requestAndResponse(ModuleE.DX.abbr, "dx_package", param);
            List<String> list = (List<String>) result.get("list");
            return list;
        } catch (Exception e) {
            chain.getLogger().error(e);
            throw new NulsException(TxErrorCode.RPC_REQUEST_FAILD);
        }
    }
}

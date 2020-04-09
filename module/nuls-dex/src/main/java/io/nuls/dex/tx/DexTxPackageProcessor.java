package io.nuls.dex.tx;

import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.ModuleTxPackageProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.dex.context.DexErrorCode;
import io.nuls.dex.manager.DexService;
import io.nuls.dex.util.LoggerUtil;
import org.bouncycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

@Component(ModuleE.Constant.DEX)
public class DexTxPackageProcessor implements ModuleTxPackageProcessor {

    @Autowired
    private DexService dexService;

    @Override
    public List<String> packProduce(int chainId, List<Transaction> txs) throws NulsException {
        List<String> list = new ArrayList<>();
        try {
            if (txs.size() > 0) {
                txs = dexService.doPacking(txs);
                list.clear();

                for (int i = 0; i < txs.size(); i++) {
                    list.add(Hex.toHexString(txs.get(i).serialize()));
                }
            }
        } catch (Exception e) {
            LoggerUtil.dexLog.error(e);
            throw new NulsException(DexErrorCode.DATA_PARSE_ERROR);
        }
        return list;
    }

    @Override
    public String getModuleCode() {
        return ModuleE.DX.abbr;
    }
}

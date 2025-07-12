package network.nerve.dex.tx.v1.validate;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.Address;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.dex.context.DexConfig;
import network.nerve.dex.context.DexErrorCode;
import network.nerve.dex.model.po.TradingOrderPo;
import network.nerve.dex.model.txData.TradingOrderCancel;
import network.nerve.dex.storage.TradingOrderStorageService;
import network.nerve.dex.util.LoggerUtil;

import java.util.*;

@Component
public class AdminOrderCancelValidator {

    private static final byte[] admin_prod = AddressTool.getAddress("NERVEepb65afgHUAfzVb9fW2wEqFpTU4Jk5rP6");
    private static final byte[] admin_beta = AddressTool.getAddress("TNVTdTSPK5Q1aXNZp8ctEeo43qv5HwvCfLjHS");

    @Autowired
    private DexConfig dexConfig;
    @Autowired
    private TradingOrderStorageService orderStorageService;

    public Map<String, Object> validateTxs(int chainId, List<Transaction> txs) {
        byte[] admin = null;
        if (chainId == 9) {
            admin = admin_prod;
        } else {
            admin = admin_beta;
        }

        List<Transaction> invalidTxList = new ArrayList<>();
        ErrorCode errorCode = null;
        Transaction tx;
        TradingOrderCancel orderCancel;
        TradingOrderPo orderPo;
        CoinFrom coinFrom;
        for (int i = 0; i < txs.size(); i++) {
            tx = txs.get(i);
            try {
                orderCancel = new TradingOrderCancel();
                orderCancel.parse(new NulsByteBuffer(tx.getTxData()));
                //Check if there are any orders that need to be cancelled
                orderPo = orderStorageService.query(orderCancel.getOrderHash());
                if (orderPo != null) {
                    if (tx.getCoinDataInstance().getFrom().size() != 1) {
                        throw new NulsException(DexErrorCode.DATA_ERROR, "coinFrom error");
                    }
                    //Verify whether canceling the commission and placing the order commission are initiated by the same person
                    coinFrom = tx.getCoinDataInstance().getFrom().get(0);
                    if (!Arrays.equals(coinFrom.getAddress(), admin)) {
                        throw new NulsException(DexErrorCode.DATA_ERROR, "coinFrom error");
                    }
                    if (coinFrom.getAssetsChainId() != dexConfig.getChainId() || coinFrom.getAssetsId() != dexConfig.getAssetId() || coinFrom.getLocked() == -1) {
                        throw new NulsException(DexErrorCode.DATA_ERROR, "coinFrom error");
                    }
                }

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

}

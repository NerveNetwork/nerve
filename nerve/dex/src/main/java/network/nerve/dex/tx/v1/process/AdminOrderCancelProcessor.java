package network.nerve.dex.tx.v1.process;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.dex.manager.DexManager;
import network.nerve.dex.storage.TradingOrderCancelStorageService;
import network.nerve.dex.storage.TradingOrderStorageService;
import network.nerve.dex.tx.v1.validate.AdminOrderCancelValidator;
import network.nerve.dex.tx.v1.validate.OrderCancelValidator;

import java.util.List;
import java.util.Map;

/**
 * Cancel delegated order processing
 */
@Component("AdminOrderCancelProcessorV1")
public class AdminOrderCancelProcessor implements TransactionProcessor {

    @Autowired
    private TradingOrderStorageService orderStorageService;
    @Autowired
    private TradingOrderCancelStorageService orderCancelStorageService;
    @Autowired
    private DexManager dexManager;
    @Autowired
    private AdminOrderCancelValidator orderCancelValidator;

    @Override
    public int getType() {
        return TxType.TRADING_ORDER_CANCEL_ADMIN;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        return orderCancelValidator.validateTxs(chainId,txs);
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return true;
    }

    /**
     * Place the entrusted transaction on the order placement page1Bit processing
     *
     * @return
     */
    @Override
    public int getPriority() {
        return 1;
    }
}

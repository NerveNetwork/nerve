package network.nerve.utils.thread;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import network.nerve.model.bo.Chain;
import network.nerve.utils.TxUtil;

public class CrossTxHandler implements Runnable {
    private final BlockHeader header;
    private Chain chain;
    private Transaction transaction;
    private int syncStatus;

    public CrossTxHandler(Chain chain, Transaction transaction, BlockHeader header, int syncStatus) {
        this.chain = chain;
        this.transaction = transaction;
        this.syncStatus = syncStatus;
        this.header = header;
    }

    @Override
    public void run() {
        /*
         * 1.如果为同步过程中，签名并广播，返回
         * 2.处理跨链交易
         * */
        if (syncStatus == 0) {
            TxUtil.signAndBroad(chain, transaction, header);
            return;
        }
        if (transaction.getType() == TxType.CROSS_CHAIN) {
            TxUtil.localCtxByzantine(transaction, chain);

        } else {
            TxUtil.handleNewCtx(transaction, chain, header, null);
        }
    }
}

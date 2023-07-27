package io.nuls.crosschain.utils.thread;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.crosschain.model.bo.Chain;
import io.nuls.crosschain.utils.TxUtil;

import java.util.List;

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

    public void run() {
        if (this.syncStatus == 0) {
            TxUtil.signAndBroad(this.chain, this.transaction, this.header);
        } else {
            if (this.transaction.getType() == 10) {
                TxUtil.localCtxByzantine(this.transaction, this.chain);
            } else {
                TxUtil.handleNewCtx(this.transaction, this.chain, this.header, (List)null);
            }

        }
    }
}


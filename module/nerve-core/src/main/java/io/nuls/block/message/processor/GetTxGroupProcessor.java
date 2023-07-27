package io.nuls.block.message.processor;

import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.message.HashListMessage;
import io.nuls.block.message.TxGroupMessage;
import io.nuls.block.rpc.call.NetworkCall;
import io.nuls.block.rpc.call.TransactionCall;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static io.nuls.block.constant.CommandConstant.TXGROUP_MESSAGE;

/**
 * @author Niels
 */
public class GetTxGroupProcessor implements Runnable {

    private LinkedBlockingQueue<HashListMessage> queue = new LinkedBlockingQueue<>(1024);

    @Override
    public void run() {
        while (true) {
            try {
                process();
            } catch (Throwable e) {
                Log.error(e);
            }
        }
    }

    private void process() throws InterruptedException {
        HashListMessage message = queue.take();
        int chainId = message.getChainId();
        String nodeId = message.getNodeId();
        NulsLogger logger = ContextManager.getContext(chainId).getLogger();
        List<NulsHash> hashList = message.getTxHashList();
        logger.debug("recieve HashListMessage from node-" + nodeId + ", txcount:" + hashList.size());
        TxGroupMessage request = new TxGroupMessage();
        List<Transaction> transactions = TransactionCall.getTransactions(chainId, hashList, true);
        if (transactions.isEmpty()) {
            return;
        }
        logger.debug("transactions size:" + transactions.size());
        request.setBlockHash(message.getBlockHash());
        request.setTransactions(transactions);
        NetworkCall.sendToNode(chainId, request, nodeId, TXGROUP_MESSAGE);
    }

    public void offer(HashListMessage message) {
        queue.offer(message);
    }
}

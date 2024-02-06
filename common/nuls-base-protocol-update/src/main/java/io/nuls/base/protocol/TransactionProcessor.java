package io.nuls.base.protocol;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.rpc.model.Key;
import io.nuls.core.rpc.model.ResponseData;
import io.nuls.core.rpc.model.TypeDescriptor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Transaction processor
 *
 * @author captain
 * @version 1.0
 * @date 2019/5/23 17:43
 */
public interface TransactionProcessor {

    /**
     * Obtain the transaction type bound to this trader,See also{@link TxType}
     *
     * @return
     */
    int getType();

    /**
     * Sort based on processing priority
     */
    Comparator<TransactionProcessor> COMPARATOR = Comparator.comparingInt(TransactionProcessor::getPriority);

    /**
     * Verify Interface
     *
     * @param chainId     chainId
     * @param txs         Type is{@link #getType()}All transaction sets for
     * @param txMap       Different transaction types and their corresponding transaction list key value pairs
     * @param blockHeader Block head
     * @return Verification error codes and transactions that did not pass verification, Need to discard
     */
    @ResponseData(description = "Return amap,mapIt contains verification error codes and transactions that did not pass verification", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "errorCode", description = "Error code"),
            @Key(name = "txList", valueType = List.class, valueElement = Transaction.class, description = "The return type isList<Transaction>")
    }))
    Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader);

    /**
     * Submit Interface
     *
     * @param chainId     chainId
     * @param txs         Type is{@link #getType()}All transaction sets for
     * @param blockHeader Block head
     * @param syncStatus  running state 0:synchronization 1:normal operation
     * @return Whether the submission was successful
     */
    boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus);

    /**
     * Rollback interface
     *
     * @param chainId     chainId
     * @param txs         Type is{@link #getType()}All transaction sets for
     * @param blockHeader Block head
     * @return Is the rollback successful
     */
    boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader);

    /**
     * When packaging transaction modules, If the corresponding transaction is registered packProduceThe logo istrue,
     * It will display all the corresponding flag bits under this moduletrueTransaction, Call the interface as parameters together
     * For example, internal transaction generation and other processing, asdex
     * Return newly generated transactions
     * @param chainId
     * @param txs
     * @param blockHeader
     * @return
     */
//    default List<String> packProduce(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
//        return null;
//    }

    /**
     * Get processing priority,The smaller the number, the smaller the number,The higher the priority, the higher the priority
     *
     * @return
     */
    default int getPriority() {
        return 1;
    }
}

package io.nuls.transaction.service;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.exception.NulsException;
import io.nuls.transaction.model.bo.*;
import io.nuls.transaction.model.dto.ModuleTxRegisterDTO;
import io.nuls.transaction.model.po.TransactionConfirmedPO;
import io.nuls.transaction.model.po.TransactionNetPO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: Charlie
 * @date: 2018/11/22
 */
public interface TxService {

    /**
     * 注册交易
     * Register transaction
     *
     * @param chain
     * @param moduleTxRegisterDto
     * @return boolean
     */
    boolean register(Chain chain, ModuleTxRegisterDTO moduleTxRegisterDto);

    /**
     * 收到一个新的交易
     * Received a new transaction
     *
     * @param tx
     * @return boolean
     * @throws NulsException NulsException
     */
    void newBroadcastTx(Chain chain, TransactionNetPO tx);


    /**
     * 由节点产生的新交易,该交易已通过验证器验证和账本验证,可放入待打包队列以及未确认存储
     * @param chain
     * @param transaction
     * @throws NulsException
     */
    void newTx(Chain chain, Transaction transaction) throws NulsException;


    /**
     * 验证交易
     * @param chain
     * @param tx
     * @return
     */
    VerifyResult verify(Chain chain, Transaction tx);

    /**
     * 交易基础验证
     *
     * @param chain
     * @param tx
     * @param txRegister
     * @throws NulsException
     */
    void baseValidateTx(Chain chain, Transaction tx, TxRegister txRegister) throws NulsException;

    /**
     * Get a transaction, first check the database from the confirmation transaction,
     * if not found, then query from the confirmed transaction
     *
     * 获取一笔交易, 先从未确认交易数据库中查询, 如果没有找到再从已确认的交易中查询
     *
     * @param chain chain
     * @param hash  tx hash
     * @return Transaction 如果没有找到则返回null
     */
    TransactionConfirmedPO getTransaction(Chain chain, NulsHash hash);

    /**
     * 查询交易是否存在，先从未确认库中查，再从已确认中查
     * @param chain
     * @param hash
     * @return
     */
    boolean isTxExists(Chain chain, NulsHash hash);

    /**
     *  打包交易
     *  适用于包含智能合约交易的区块链
     * @param chain
     * @param endtimestamp 获取交易截止时间
     * @param maxTxDataSize
     * @param blockTime 区块时间
     * @param packingAddress
     * @param preStateRoot
     * @return
     */
    @Deprecated
    TxPackage getPackableTxs(Chain chain, long endtimestamp, long maxTxDataSize, long blockTime,
                             String packingAddress, String preStateRoot);


    /**
     * 收到新区快时，验证完整交易列表
     * @param chain
     * @param list
     * @param preStateRoot
     * @return
     * @throws NulsException
     */
    Map<String, Object> batchVerify(Chain chain, List<String> list, BlockHeader blockHeader, String blockHeaderStr, String preStateRoot) throws Exception;


    /**
     * 清理交易基础部分
     * @param chain
     * @param tx
     */
    void baseClearTx(Chain chain, Transaction tx);

    /**
     * 从已验证未打包交易中删除单个无效的交易
     *
     * @param chain
     * @param tx
     * @return
     */
    void clearInvalidTx(Chain chain, Transaction tx);

    /**
     * 从已验证未打包交易中删除单个无效的交易
     * @param chain
     * @param tx
     * @param changeStatus
     */
    void clearInvalidTx(Chain chain, Transaction tx, boolean changeStatus);


    /**
     * 将交易加回到待打包队列
     * 将孤儿交易(如果有),加入到验证通过的交易集合中,按取出的顺序排倒序,再依次加入待打包队列的最前端
     *
     * @param chain
     * @param txList      验证通过的交易
     * @param orphanTxSet 孤儿交易
     */
    void putBackPackablePool(Chain chain, List<TxPackageWrapper> txList, Set<TxPackageWrapper> orphanTxSet);
    void putBackPackablePool(Chain chain, Set<TxPackageWrapper> orphanTxSet);

    /**
     * 将孤儿交易加回待打包队列时, 要判断加了几次(因为下次打包时又验证为孤儿交易会再次被加回), 达到阈值就不再加回了
     * @param chain
     * @param orphanTxSet
     * @param txPackageWrapper
     */
    void addOrphanTxSet(Chain chain, Set<TxPackageWrapper> orphanTxSet, TxPackageWrapper txPackageWrapper);

    /**
     * 1.统一验证
     * 2a:如果没有不通过的验证的交易则结束!!
     * 2b.有不通过的验证时，moduleVerifyMap过滤掉不通过的交易.
     * 3.重新验证同一个模块中不通过交易后面的交易(包括单个verify和coinData)，再执行1.递归？
     *
     * @param moduleVerifyMap
     */
    boolean txModuleValidatorPackable(Chain chain, Map<String, List<String>> moduleVerifyMap, List<TxPackageWrapper> packingTxList, Set<TxPackageWrapper> orphanTxSet) throws NulsException;
}

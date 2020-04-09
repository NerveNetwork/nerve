package io.nuls.api.db;

import io.nuls.api.model.po.StackSnapshootInfo;
import io.nuls.api.model.po.TransactionInfo;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-10 16:27
 * @Description: stack 快照
 */
public interface StackSnapshootService {
    /**
     * id 字段
     */
   String ID = "day";

    void save(int chainId,StackSnapshootInfo stackSnapshootInfo);

    /**
     * 获取最新的快照数据
     */
    Optional<StackSnapshootInfo> getLastSnapshoot(int chainId);


    /**
     * 获取快照列表
     * @param start
     * @param end
     * @return
     */
    List<StackSnapshootInfo> queryList(int chainId,long start,long end);

    /**
     * 构造一个快照对象
     * @param chainId
     * @param tx
     * @return
     */
    Optional<StackSnapshootInfo> buildSnapshoot(int chainId, TransactionInfo tx);

    /**
     * 获取合计的收益发放数量
     * @return
     */
    BigInteger queryRewardTotal(int chainId);

}

package io.nuls.api.db;

import io.nuls.api.model.po.BlockTimeInfo;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-04 16:19
 * @Description: 区块出块消耗时间统计表
 */
public interface BlockTimeService {

    void save(int chainId,BlockTimeInfo info);

    BlockTimeInfo get(int chainId);

}

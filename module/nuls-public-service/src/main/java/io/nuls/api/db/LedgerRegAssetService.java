package io.nuls.api.db;

import io.nuls.api.model.po.LedgerRegAssetInfo;

import java.util.List;

/**
 * @Author: zhoulijun
 * @Time: 2020-04-24 14:10
 * @Description: 功能描述
 */
public interface LedgerRegAssetService {

    void save(final LedgerRegAssetInfo info);

    List<LedgerRegAssetInfo> queryByHeight(int chainId,Long blockHeight);

}

package io.nuls.api.db;

import io.nuls.api.model.po.ConverterTxInfo;

import java.util.List;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-23 17:50
 * @Description: 异构跨链交易
 */
public interface ConverterTxService {

    void save(int chainId,ConverterTxInfo info);

    List<ConverterTxInfo> queryList(int chainId,long startBlockHeight,long endBlockHeight);

    ConverterTxInfo getByTxHash(int chainId,String txHash);

}

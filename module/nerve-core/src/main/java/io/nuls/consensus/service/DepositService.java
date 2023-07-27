package io.nuls.consensus.service;

import io.nuls.core.basic.Result;

import java.util.Map;

/**
 * @author tag
 * 2019/04/01
 * */
public interface DepositService {
    /**
     * 委托共识
     * @param params
     * @return Result
     * */
    Result depositToAgent(Map<String,Object> params);

    /**
     * 退出共识
     * @param params
     * @return Result
     * */
    Result withdraw(Map<String,Object> params);


    /**
     * 查询委托信息列表
     * @param params
     * @return Result
     * */
    Result getDepositList(Map<String,Object> params);

    /**
     * 通过Simple查询抵押资产的资产id和资产链id
     * @param params
     * @return Result
     * */
    Result getAssetBySymbol(Map<String,Object> params);

    /**
     * 查询可参与stacking的资产列表
     * @param params
     * @return Result
     * */
    Result getCanStackingAssetList(Map<String,Object> params);

    Result batchWithdraw(Map<String, Object> params);

    Result batchStakingMerge(Map<String, Object> params);
}

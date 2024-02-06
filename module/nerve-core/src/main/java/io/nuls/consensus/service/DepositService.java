package io.nuls.consensus.service;

import io.nuls.core.basic.Result;

import java.util.Map;

/**
 * @author tag
 * 2019/04/01
 * */
public interface DepositService {
    /**
     * Commission consensus
     * @param params
     * @return Result
     * */
    Result depositToAgent(Map<String,Object> params);

    /**
     * Exit consensus
     * @param params
     * @return Result
     * */
    Result withdraw(Map<String,Object> params);


    /**
     * Query delegation information list
     * @param params
     * @return Result
     * */
    Result getDepositList(Map<String,Object> params);

    /**
     * adoptSimpleQuery the assets of mortgaged assetsidAnd asset chainid
     * @param params
     * @return Result
     * */
    Result getAssetBySymbol(Map<String,Object> params);

    /**
     * Query participationstackingAsset List for
     * @param params
     * @return Result
     * */
    Result getCanStackingAssetList(Map<String,Object> params);

    Result batchWithdraw(Map<String, Object> params);

    Result batchStakingMerge(Map<String, Object> params);
}

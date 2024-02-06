package io.nuls.base.api.provider.farm;

import io.nuls.base.api.provider.Provider;
import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.farm.facade.*;

/**
 * @author Niels
 */

public interface FarmProvider {

    /**
     * establishfarm
     * create farm
     *
     * @param req
     * @return
     */
    Result<String> createFarm(CreateFarmReq req);

    Result<String> stake(FarmStakeReq req);

    Result<String> withdraw(FarmWithdrawReq req);

    Result<String> farmInfo(String farmHash);

    Result<String> farmUserInfo(String farmHash,String userAddress);


    Result<String> getFarmList();
}

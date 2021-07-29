package io.nuls.base.api.provider.farm.facade;

import io.nuls.base.api.provider.BaseReq;

/**
 * @author Niels
 */
public class FarmUserInfoReq  extends BaseReq {
    private String farmHash;
    private String userAddress;

    public FarmUserInfoReq(String farmHash, String userAddress) {
        this.farmHash = farmHash;
        this.userAddress = userAddress;
    }

    public String getFarmHash() {
        return farmHash;
    }

    public void setFarmHash(String farmHash) {
        this.farmHash = farmHash;
    }

    public String getUserAddress() {
        return userAddress;
    }

    public void setUserAddress(String userAddress) {
        this.userAddress = userAddress;
    }
}

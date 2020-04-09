package io.nuls.base.api.provider.consensus.facade;

import io.nuls.base.api.provider.BaseReq;

public class UpdateNet extends BaseReq {
    private String consensusPubKey;
    private int updateType;

    public String getConsensusPubKey() {
        return consensusPubKey;
    }

    public void setConsensusPubKey(String consensusPubKey) {
        this.consensusPubKey = consensusPubKey;
    }

    public int getUpdateType() {
        return updateType;
    }

    public void setUpdateType(int updateType) {
        this.updateType = updateType;
    }
}

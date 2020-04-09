package io.nuls.base.api.provider.consensus.facade;

import io.nuls.base.api.provider.BaseReq;

public class InitNet  extends BaseReq {
    private String selfPub;
    private String selfPriv;
    private String consensusPubKeys;


    public String getSelfPub() {
        return selfPub;
    }

    public void setSelfPub(String selfPub) {
        this.selfPub = selfPub;
    }

    public String getSelfPriv() {
        return selfPriv;
    }

    public void setSelfPriv(String selfPriv) {
        this.selfPriv = selfPriv;
    }

    public String getConsensusPubKeys() {
        return consensusPubKeys;
    }

    public void setConsensusPubKeys(String consensusPubKeys) {
        this.consensusPubKeys = consensusPubKeys;
    }
}

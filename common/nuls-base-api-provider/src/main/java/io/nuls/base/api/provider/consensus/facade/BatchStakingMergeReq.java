package io.nuls.base.api.provider.consensus.facade;

import io.nuls.base.api.provider.BaseReq;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-11 11:57
 * @Description: Exit consensus
 */
public class BatchStakingMergeReq extends BaseReq {

    int timeType;

    String address;

    String txHashes;

    String password;

    public BatchStakingMergeReq(String address, String txHashes, String password, int timeType) {
        this.address = address;
        this.txHashes = txHashes;
        this.password = password;
        this.timeType = timeType;
    }


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getTimeType() {
        return timeType;
    }

    public void setTimeType(int timeType) {
        this.timeType = timeType;
    }

    public String getTxHashes() {
        return txHashes;
    }

    public void setTxHashes(String txHashes) {
        this.txHashes = txHashes;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}

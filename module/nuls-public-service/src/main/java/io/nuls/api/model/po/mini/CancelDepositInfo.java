package io.nuls.api.model.po.mini;

import io.nuls.api.model.po.TxDataInfo;
import io.nuls.base.data.NulsHash;

import java.math.BigInteger;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-04 10:30
 * @Description: 功能描述
 */
public class CancelDepositInfo extends TxDataInfo {

    private String address;

    private String joinTxHash;

    private long createTime;

    private long blockHeight;

    private BigInteger fee;

    private String txHash;


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getJoinTxHash() {
        return joinTxHash;
    }

    public void setJoinTxHash(String joinTxHash) {
        this.joinTxHash = joinTxHash;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public BigInteger getFee() {
        return fee;
    }

    public void setFee(BigInteger fee) {
        this.fee = fee;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }
}


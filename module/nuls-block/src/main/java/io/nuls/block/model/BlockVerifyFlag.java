package io.nuls.block.model;


import io.nuls.core.rpc.util.NulsDateUtils;

public class BlockVerifyFlag {
    byte bztAndBaseVerify;
    long time = 0;

    public BlockVerifyFlag(byte value) {
        bztAndBaseVerify = value;
        this.time = NulsDateUtils.getCurrentTimeSeconds();
    }

    public byte getBztAndBaseVerify() {
        return bztAndBaseVerify;
    }

    public void setBztAndBaseVerify(byte bztAndBaseVerify) {
        this.bztAndBaseVerify = bztAndBaseVerify;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}

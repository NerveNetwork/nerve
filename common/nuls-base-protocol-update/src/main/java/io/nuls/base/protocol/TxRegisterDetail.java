package io.nuls.base.protocol;

import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;

/**
 * 交易注册类
 * Transaction registration class
 *
 */
@ApiModel(description = "交易注册")
public class TxRegisterDetail {
    /**
     * 交易类型
     * Transaction type
     */
    @ApiModelProperty(description = "交易类型")
    private int txType;
    /**
     * 是否是系统交易
     * Is it a system transaction
     */
    @ApiModelProperty(description = "是否是系统交易")
    private boolean systemTx;
    /**
     * 是否是解锁交易
     * Is it a unlock transaction
     */
    @ApiModelProperty(description = "是否是解锁交易")
    private boolean unlockTx;
    /**
     * 交易是否需要签名
     * Is it a sign-required transaction
     */
    @ApiModelProperty(description = "交易是否需要签名")
    private boolean verifySignature;

    /**
     * 交易是否需要验证手续费
     * Is it a fee-validate-required transaction
     */
    @ApiModelProperty(description = "交易是否需要验证手续费")
    private boolean verifyFee;

    @ApiModelProperty(description = "打包时该类型交易是否需要调用打包加工器")
    private boolean packProduce;

    @ApiModelProperty(description = "组装区块交易时产生的交易")
    private boolean packGenerate;


    public int getTxType() {
        return txType;
    }

    public void setTxType(int txType) {
        this.txType = txType;
    }

    public boolean getSystemTx() {
        return systemTx;
    }

    public void setSystemTx(boolean systemTx) {
        this.systemTx = systemTx;
    }

    public boolean getUnlockTx() {
        return unlockTx;
    }

    public void setUnlockTx(boolean unlockTx) {
        this.unlockTx = unlockTx;
    }

    public boolean getVerifySignature() {
        return verifySignature;
    }

    public void setVerifySignature(boolean verifySignature) {
        this.verifySignature = verifySignature;
    }

    public boolean getVerifyFee() {
        return verifyFee;
    }

    public void setVerifyFee(boolean verifyFee) {
        this.verifyFee = verifyFee;
    }

    public boolean getPackProduce() {
        return packProduce;
    }

    public void setPackProduce(boolean packProduce) {
        this.packProduce = packProduce;
    }

    public boolean getPackGenerate() {
        return packGenerate;
    }

    public void setPackGenerate(boolean packGenerate) {
        this.packGenerate = packGenerate;
    }

    @Override
    public String toString() {
        return "TxRegisterDetail{" +
                "txType=" + txType +
                ", systemTx=" + systemTx +
                ", unlockTx=" + unlockTx +
                ", verifySignature=" + verifySignature +
                ", verifyFee=" + verifyFee +
                '}';
    }
}

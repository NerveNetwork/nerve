package io.nuls.provider.model.dto;

import io.nuls.base.api.provider.consensus.facade.ReduceNonceInfo;
import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;


@ApiModel(name = "退出节点保证金nonce数据")
public class ReduceNonceDTO {
    @ApiModelProperty(description = "委托金额")
    private String deposit;
    @ApiModelProperty(description = "委托数据对应NONCE")
    private String nonce;
    public ReduceNonceDTO(ReduceNonceInfo reduceNonceInfo){
        this.deposit = reduceNonceInfo.getDeposit();
        this.nonce = reduceNonceInfo.getNonce();
    }
    public String getDeposit() {
        return deposit;
    }

    public void setDeposit(String deposit) {
        this.deposit = deposit;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }
}

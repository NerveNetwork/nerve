package io.nuls.provider.model.dto;

import io.nuls.base.api.provider.consensus.facade.ReduceNonceInfo;
import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;


@ApiModel(name = "Exit node marginnoncedata")
public class ReduceNonceDTO {
    @ApiModelProperty(description = "Entrusted amount")
    private String deposit;
    @ApiModelProperty(description = "Correspondence of entrusted dataNONCE")
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

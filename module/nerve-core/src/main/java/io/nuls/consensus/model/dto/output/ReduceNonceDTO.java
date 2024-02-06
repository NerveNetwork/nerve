package io.nuls.consensus.model.dto.output;

import io.nuls.consensus.model.po.nonce.NonceDataPo;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;

@ApiModel(name = "Exit node marginnoncedata")
public class ReduceNonceDTO {
    @ApiModelProperty(description = "Entrusted amount")
    private String deposit;
    @ApiModelProperty(description = "Correspondence of entrusted dataNONCE")
    private String nonce;

    public ReduceNonceDTO(NonceDataPo po){
        this.deposit = po.getDeposit().toString();
        this.nonce = HexUtil.encode(po.getNonce());
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

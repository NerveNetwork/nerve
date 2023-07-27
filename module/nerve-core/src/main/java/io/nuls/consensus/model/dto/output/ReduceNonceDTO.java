package io.nuls.consensus.model.dto.output;

import io.nuls.consensus.model.po.nonce.NonceDataPo;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;

@ApiModel(name = "退出节点保证金nonce数据")
public class ReduceNonceDTO {
    @ApiModelProperty(description = "委托金额")
    private String deposit;
    @ApiModelProperty(description = "委托数据对应NONCE")
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

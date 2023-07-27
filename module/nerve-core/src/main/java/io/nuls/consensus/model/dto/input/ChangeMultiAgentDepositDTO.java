package io.nuls.consensus.model.dto.input;

public class ChangeMultiAgentDepositDTO extends ChangeAgentDepositDTO {
    private String signAddress;

    public String getSignAddress() {
        return signAddress;
    }

    public void setSignAddress(String signAddress) {
        this.signAddress = signAddress;
    }
}

package io.nuls.consensus.model.dto.input;

/**
 * Node margin change（Additional margin, withdrawal of margin）
 * Node margin change (additional margin, withdrawal margin)
 *
 * @author tag
 * 2019/10/18
 * */
public class ChangeAgentDepositDTO {
    private int chainId;
    private String address;
    private String amount;
    private String password;

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}

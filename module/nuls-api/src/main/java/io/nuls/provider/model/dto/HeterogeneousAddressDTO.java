package io.nuls.provider.model.dto;

/**
 * @author: Loki
 * @date: 2020/6/10
 */
public class HeterogeneousAddressDTO {

    private int chainId;

    private String address;

    private String balance;

    public HeterogeneousAddressDTO() {
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }
}

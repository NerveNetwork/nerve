package io.nuls.base.api.provider.converter.facade;

/**
 * @author: Loki
 * @date: 2020/6/10
 */
public class HeterogeneousAddressDTO {

    private int chainId;

    private String address;

    private String balance;

    private String symbol;

    public HeterogeneousAddressDTO() {
    }

    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

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

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}

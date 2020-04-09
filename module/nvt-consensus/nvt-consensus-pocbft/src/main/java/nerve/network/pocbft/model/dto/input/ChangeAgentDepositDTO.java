package nerve.network.pocbft.model.dto.input;

/**
 * 节点保证金变更（追加保证金，退出保证金）
 * Node margin change (additional margin, withdrawal margin)
 *
 * @author: Jason
 * 2019/10/18
 * */
public class ChangeAgentDepositDTO {
    private int chainId;
    private String address;
    private String amount;
    private String password;
    private String agentHash;

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

    public String getAgentHash() {
        return agentHash;
    }

    public void setAgentHash(String agentHash) {
        this.agentHash = agentHash;
    }
}

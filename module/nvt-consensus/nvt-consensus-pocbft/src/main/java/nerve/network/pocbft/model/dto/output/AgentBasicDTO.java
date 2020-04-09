package nerve.network.pocbft.model.dto.output;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;
import nerve.network.pocbft.model.bo.tx.txdata.Agent;
/**
 * 节点基础信息类
 * Node basic information class
 *
 * @author: Jason
 * 2018/11/20
 */
@ApiModel(name = "节点基础信息")
public class AgentBasicDTO {
    @ApiModelProperty(description = "节点地址")
    private String agentAddress;
    @ApiModelProperty(description = "节点出块地址")
    private String packingAddress;
    @ApiModelProperty(description = "抵押金额")
    private String deposit;
    @ApiModelProperty(description = "是否为种子节点")
    private boolean isSeedNode;
    @ApiModelProperty(description = "公钥")
    private String pubKey;


    public AgentBasicDTO(Agent agent){
        this.agentAddress = AddressTool.getStringAddressByBytes(agent.getAgentAddress());
        this.packingAddress = AddressTool.getStringAddressByBytes(agent.getPackingAddress());
        this.deposit = BigIntegerUtils.bigIntegerToString(agent.getDeposit());
        this.isSeedNode = false;
        if(agent.getPubKey() != null){
            this.pubKey = HexUtil.encode(agent.getPubKey());
        }
    }

    public AgentBasicDTO(String packingAddress,String pubKey){
        this.packingAddress = packingAddress;
        this.isSeedNode = true;
        this.pubKey = pubKey;
    }

    public String getAgentAddress() {
        return agentAddress;
    }

    public void setAgentAddress(String agentAddress) {
        this.agentAddress = agentAddress;
    }

    public String getPackingAddress() {
        return packingAddress;
    }

    public void setPackingAddress(String packingAddress) {
        this.packingAddress = packingAddress;
    }

    public String getDeposit() {
        return deposit;
    }

    public void setDeposit(String deposit) {
        this.deposit = deposit;
    }

    public boolean isSeedNode() {
        return isSeedNode;
    }

    public void setSeedNode(boolean seedNode) {
        isSeedNode = seedNode;
    }

    public String getPubKey() {
        return pubKey;
    }

    public void setPubKey(String pubKey) {
        this.pubKey = pubKey;
    }
}

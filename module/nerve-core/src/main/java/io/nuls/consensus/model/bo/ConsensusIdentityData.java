package io.nuls.consensus.model.bo;

public class ConsensusIdentityData {
    private String nodeId;
    private String message;

    public ConsensusIdentityData(String nodeId,String message){
        this.nodeId = nodeId;
        this.message = message;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ConsensusIdentityData)){
            return false;
        }
        return ((ConsensusIdentityData) obj).getMessage().equals(this.message);
    }
}

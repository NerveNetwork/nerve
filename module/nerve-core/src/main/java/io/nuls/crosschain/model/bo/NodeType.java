package io.nuls.crosschain.model.bo;

import java.util.Objects;

/**
 * Node type
 * @author tag
 * 2019/5/22
 * */
public class NodeType {
    private String nodeId;
    private int nodeType;

    public NodeType(String nodeId, int nodeType) {
        this.nodeId = nodeId;
        this.nodeType = nodeType;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public int getNodeType() {
        return nodeType;
    }

    public void setNodeType(int nodeType) {
        this.nodeType = nodeType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeType nodeType1 = (NodeType) o;
        return nodeType == nodeType1.nodeType && Objects.equals(nodeId, nodeType1.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, nodeType);
    }
}

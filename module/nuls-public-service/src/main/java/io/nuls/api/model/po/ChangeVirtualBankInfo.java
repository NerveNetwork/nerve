package io.nuls.api.model.po;

import java.util.List;

/**
 * @Author: zhoulijun
 * @Time: 2020-06-09 15:53
 * @Description:  虚拟银行变更交易
 */
public class ChangeVirtualBankInfo extends TxDataInfo {

    private List<String> inAgents;

    private List<String> outAgents;

    public List<String> getInAgents() {
        return inAgents;
    }

    public void setInAgents(List<String> inAgents) {
        this.inAgents = inAgents;
    }

    public List<String> getOutAgents() {
        return outAgents;
    }

    public void setOutAgents(List<String> outAgents) {
        this.outAgents = outAgents;
    }
}

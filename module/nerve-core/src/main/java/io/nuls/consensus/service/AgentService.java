package io.nuls.consensus.service;

import io.nuls.core.basic.Result;

import java.util.Map;

/**
 * @author tag
 * 2019/04/01
 * */
public interface AgentService {
    /**
     * Create nodes
     * */
    Result createAgent(Map<String,Object> params);

    /**
     * Additional margin
     * */
    Result appendAgentDeposit (Map<String,Object> params);

    /**
     * Reduce margin
     * */
    Result reduceAgentDeposit (Map<String,Object> params);

    /**
     * Unregister node
     * @param params
     * return Result
     * */
    Result stopAgent(Map<String,Object> params);

    /**
     * Get node list
     * @param params
     * return Result
     * */
    Result getAgentList(Map<String,Object> params);

    /**
     * Heterogeneous cross chain query consensus node list
     * @param params
     * return Result
     * */
    Result getAgentBasicList(Map<String,Object> params);

    /**
     * Get specified node information
     * @param params
     * @return Result
     * */
    Result getAgentInfo(Map<String,Object> params);

    /**
     * Query the status of specified consensus nodes
     * @param params
     * @return Result
     * */
    Result getAgentStatus(Map<String,Object> params);

    /**
     * Modify node consensus status
     * @param params
     * @return Result
     */
    Result updateAgentConsensusStatus(Map<String, Object> params);

    /**
     * Modify node packaging status
     * @param params
     * @return Result
     * */
    Result updateAgentStatus(Map<String,Object> params);

    /**
     * Get the current node's outbound address
     * @param params
     * @return Result
     * */
    Result getNodePackingAddress(Map<String,Object> params);

    /**
     * Get all node block addresses/specifyNBlock assignment
     * @param params
     * @return Result
     * */
    Result getAgentAddressList(Map<String,Object> params);

    /**
     * Obtain the outbound account information of the current node
     * @param params
     * @return Result
     * */
    Result getPackerInfo(Map<String,Object> params);

    /**
     * Obtain node change information between two rounds of consensus
     * @param params
     * @return Result
     * */
    Result getAgentChangeInfo(Map<String,Object> params);

    /**
     * Obtain node change information between two rounds of consensus
     * @param params
     * @return Result
     * */
    Result getReduceDepositNonceList(Map<String,Object> params);

    /**
     * Virtual Bank Node Change
     * @param params
     * @return Result
     * */
    Result virtualAgentChange(Map<String,Object> params);
}

package io.nuls.api.db;

import io.nuls.api.model.po.AgentInfo;
import io.nuls.api.model.po.PageInfo;

import java.math.BigInteger;
import java.util.List;

public interface AgentService {

    void initCache();

    AgentInfo getAgentByHash(int chainID, String agentHash);

    PageInfo<AgentInfo> getAgentByHashList(int chainID, int pageNumber, int pageSize, List<String> hashList);

    AgentInfo getAgentByPackingAddress(int chainID, String packingAddress);

    AgentInfo getAgentByRewardAddress(int chainID, String rewardAddress);

    AgentInfo getAgentByAgentAddress(int chainID, String agentAddress);

    AgentInfo getAliveAgentByAgentAddress(int chainID, String agentAddress);

    void saveAgentList(int chainID, List<AgentInfo> agentInfoList);

    void rollbackAgentList(int chainId, List<AgentInfo> agentInfoList);

    List<AgentInfo> getAgentListByStartHeight(int chainId, long startHeight);

    List<AgentInfo> getAllAgentList(int chainId);

    PageInfo<AgentInfo> getAgentList(int chainId, int pageNumber, int pageSize);

    long agentsCount(int chainId, long startHeight);

    BigInteger getNvtConsensusCoinTotal(int chainId);
}

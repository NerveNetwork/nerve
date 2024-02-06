package io.nuls.consensus.storage;
import io.nuls.consensus.model.po.nonce.AgentDepositNoncePo;
import io.nuls.base.data.NulsHash;

public interface AgentDepositNonceService {
    /**
     * Node addition/Withdrawal of marginNoncedata
     *
     * @param  agentHash    nodeHASH
     * @param chainID    chainID/chain id
     * @return boolean
     * */
    boolean save(NulsHash agentHash, AgentDepositNoncePo po, int chainID);

    /**
     * Obtain the designated node depositNonceRelated information
     *
     * @param  agentHash    nodeHASH
     * @param chainID       chainID/chain id
     * */
    AgentDepositNoncePo get(NulsHash agentHash, int chainID);

    /**
     * Delete specified account depositNonceinformation
     *
     * @param agentHash    nodeHASH
     * @param chainID      chainID/chain id
     * @return boolean
     * */
    boolean delete(NulsHash agentHash, int chainID);
}

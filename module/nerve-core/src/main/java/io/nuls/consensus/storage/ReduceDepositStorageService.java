package io.nuls.consensus.storage;

import io.nuls.consensus.model.po.ChangeAgentDepositPo;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;

import java.util.List;

/**
 * Reduce margin database management
 * Reduce margin database management
 *
 * @author  tag
 * 2019/10/23
 * */
public interface ReduceDepositStorageService {
    /**
     * Save exit margin data
     * Save reduce margin data
     *
     * @param  po        Additional margin data
     * @param chainID    chainID/chain id
     * @return boolean
     * */
    boolean save(ChangeAgentDepositPo po, int chainID);

    /**
     * According to the transactionHashQuery detailed data on exit margin trading
     * Query exit margin transaction details according to transaction hash
     *
     * @param  txHash    transactionHash
     * @param chainID    chainID/chain id
     * @return           Specify detailed information corresponding to the transaction
     * */
    ChangeAgentDepositPo get(NulsHash txHash, int chainID);

    /**
     * Delete specified exit margin transaction details
     * Delete specified exit margin transaction details
     *
     * @param txHash     transactionHash
     * @param chainID    chainID/chain id
     * @return boolean
     * */
    boolean delete(NulsHash txHash,int chainID);


    /**
     * Obtain all exit margin information
     * Get all exit margin call information
     *
     * @param  chainID               chainID/chain id
     * @exception NulsException      Data serialization exception
     * @return                       Additional margin list
     * */
    List<ChangeAgentDepositPo> getList(int chainID) throws NulsException;
}

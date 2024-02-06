package io.nuls.consensus.storage;

import io.nuls.consensus.model.po.ChangeAgentDepositPo;
import io.nuls.base.data.NulsHash;
import io.nuls.core.exception.NulsException;

import java.util.List;

/**
 * Additional margin database management
 * Additional margin database management
 *
 * @author  tag
 * 2019/10/23
 * */
public interface AppendDepositStorageService {
    /**
     * Save additional margin data
     * Save additional margin data
     *
     * @param  po        Additional margin data
     * @param chainID    chainID/chain id
     * @return boolean
     * */
    boolean save(ChangeAgentDepositPo po, int chainID);

    /**
     * According to the transactionHashQuery detailed data on margin trading
     * Query the detailed data of margin call transaction according to the transaction hash
     *
     * @param  txHash    transactionHash
     * @param chainID    chainID/chain id
     * */
    ChangeAgentDepositPo get(NulsHash txHash, int chainID);

    /**
     * Delete specified margin trading details
     * Delete specified margin call transaction details
     *
     * @param txHash     transactionHash
     * @param chainID    chainID/chain id
     * @return boolean
     * */
    boolean delete(NulsHash txHash,int chainID);


    /**
     * Obtain all additional margin information
     * Get all margin call information
     *
     * @param  chainID    chainID/chain id
     * @return Additional margin list
     * @exception Exception
     * @return    All additional records
     * */
    List<ChangeAgentDepositPo> getList(int chainID) throws NulsException;
}

package io.nuls.consensus.storage;

import io.nuls.consensus.model.po.AwardSettleRecordPo;

import java.util.Map;

public interface AwardSettleRecordService {
    /**
     * Save consensus reward settlement records for the specified chain
     * Save configuration information for the specified chain
     *
     * @param recordPo          Consensus reward settlement record class/Consensus award settlement records
     * @param chainID           chainID/chain id
     * @return                  Whether the save was successful/Is preservation successful?
     * @exception Exception     Save midway exception
     * */
    boolean save(AwardSettleRecordPo recordPo, int chainID)throws Exception;

    /**
     * Query the configuration information of a certain chain
     * Query the configuration information of a chain
     *
     * @param chainID chainID/chain id
     * @return Configuration Information Class/config bean
     * */
    AwardSettleRecordPo get(int chainID);

    /**
     * Delete configuration information for a certain chain
     * Delete configuration information for a chain
     *
     * @param chainID chainID/chain id
     * @return Whether the deletion was successful/Delete success
     * */
    boolean delete(int chainID);

    /**
     * Obtain all chain information of the current node
     * Get all the chain information of the current node
     *
     * @return Node Information List/Node information list
     * */
    Map<Integer, AwardSettleRecordPo> getList();
}

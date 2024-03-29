package io.nuls.consensus.test;

import io.nuls.core.model.ObjectUtils;
import io.nuls.core.rockdb.constant.DBErrorCode;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.common.NerveCoreConfig;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.core.log.Log;

public class TestUtil {
    public static void initTable(int chainId){
        try {
            /*
            Create consensus node table
            Create consensus node tables
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_AGENT+chainId);

            /*
            Create consensus information table
            Create consensus information tables
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_DEPOSIT+chainId);

            /*
            Create a red and yellow card information table
            Creating Red and Yellow Card Information Table
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_PUNISH+chainId);
        }catch (Exception e){
            if (!DBErrorCode.DB_TABLE_EXIST.equals(e.getMessage())) {
                Log.info(e.getMessage());
            }
        }
    }
    public static void main(String []args){
        byte [] objs=ObjectUtils.objectToBytes(new NerveCoreConfig());
    }
}

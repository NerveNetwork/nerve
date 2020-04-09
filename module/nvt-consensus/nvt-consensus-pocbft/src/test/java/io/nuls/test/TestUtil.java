package io.nuls.test;

import io.nuls.core.model.ObjectUtils;
import io.nuls.core.rockdb.constant.DBErrorCode;
import io.nuls.core.rockdb.service.RocksDBService;
import nerve.network.pocbft.model.bo.config.ConsensusChainConfig;
import nerve.network.pocbft.constant.ConsensusConstant;
import io.nuls.core.log.Log;

public class TestUtil {
    public static void initTable(int chainId){
        try {
            /*
            创建共识节点表
            Create consensus node tables
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_AGENT+chainId);

            /*
            创建共识信息表
            Create consensus information tables
            */
            RocksDBService.createTable(ConsensusConstant.DB_NAME_DEPOSIT+chainId);

            /*
            创建红黄牌信息表
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
        byte [] objs=ObjectUtils.objectToBytes(new ConsensusChainConfig());
    }
}

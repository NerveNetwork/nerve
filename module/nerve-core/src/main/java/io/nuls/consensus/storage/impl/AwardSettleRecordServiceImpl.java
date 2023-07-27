package io.nuls.consensus.storage.impl;

import io.nuls.consensus.model.po.AwardSettleRecordPo;
import io.nuls.consensus.storage.AwardSettleRecordService;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.Log;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.rockdb.model.Entry;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.consensus.constant.ConsensusConstant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AwardSettleRecordServiceImpl implements AwardSettleRecordService {
    @Override
    public boolean save(AwardSettleRecordPo recordPo, int chainID) throws Exception {
        if(recordPo == null){
            return  false;
        }
        return RocksDBService.put(ConsensusConstant.DB_NAME_AWARD_SETTLE_RECORD, ByteUtils.intToBytes(chainID), recordPo.serialize());
    }

    @Override
    public AwardSettleRecordPo get(int chainID) {
        try {
            byte[] value = RocksDBService.get(ConsensusConstant.DB_NAME_AWARD_SETTLE_RECORD,ByteUtils.intToBytes(chainID));
            AwardSettleRecordPo recordPo = new AwardSettleRecordPo();
            recordPo.parse(value,0);
            return recordPo;
        }catch (Exception e){
            Log.error(e);
            return null;
        }
    }

    @Override
    public boolean delete(int chainID) {
        try {
            return RocksDBService.delete(ConsensusConstant.DB_NAME_AWARD_SETTLE_RECORD,ByteUtils.intToBytes(chainID));
        }catch (Exception e){
            Log.error(e);
            return  false;
        }
    }

    @Override
    public Map<Integer, AwardSettleRecordPo> getList() {
        try {
            List<Entry<byte[], byte[]>> list = RocksDBService.entryList(ConsensusConstant.DB_NAME_AWARD_SETTLE_RECORD);
            Map<Integer, AwardSettleRecordPo> recordPoMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_4);
            for (Entry<byte[], byte[]>entry:list) {
                int key = ByteUtils.bytesToInt(entry.getKey());
                AwardSettleRecordPo recordPo = new AwardSettleRecordPo();
                recordPo.parse(entry.getValue(),0);
                recordPoMap.put(key, recordPo);
            }
            return recordPoMap;
        }catch (Exception e){
            Log.error(e);
            return null;
        }
    }
}

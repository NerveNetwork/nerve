package io.nuls.consensus.storage.impl;

import io.nuls.consensus.model.po.ChangeAgentDepositPo;
import io.nuls.consensus.storage.AppendDepositStorageService;
import io.nuls.base.data.NulsHash;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.rockdb.model.Entry;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.consensus.constant.ConsensusConstant;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of additional margin
 * Additional margin implementation class
 *
 * @author tag
 * 2019/10/23
 * */
@Component
public class AppendDepositStorageServiceImpl implements AppendDepositStorageService {
    @Override
    public boolean save(ChangeAgentDepositPo po, int chainID) {
        NulsHash txHash = po.getTxHash();
        try {
            byte[] key = txHash.getBytes();
            byte[] value = po.serialize();
            return RocksDBService.put(ConsensusConstant.DB_NAME_APPEND_DEPOSIT+ConsensusConstant.SEPARATOR +chainID,key,value);
        }catch (Exception e){
            Log.error(e);
            return  false;
        }
    }

    @Override
    public ChangeAgentDepositPo get(NulsHash txHash, int chainID) {
        try {
            byte[] key = txHash.getBytes();
            byte[] value = RocksDBService.get(ConsensusConstant.DB_NAME_APPEND_DEPOSIT+ConsensusConstant.SEPARATOR +chainID,key);
            if(value == null){
                return  null;
            }
            ChangeAgentDepositPo po = new ChangeAgentDepositPo();
            po.parse(value,0);
            return po;
        }catch (Exception e){
            Log.error(e);
            return  null;
        }
    }

    @Override
    public boolean delete(NulsHash txHash, int chainID) {
        try {
            byte[] key = txHash.getBytes();
            return RocksDBService.delete(ConsensusConstant.DB_NAME_APPEND_DEPOSIT+ConsensusConstant.SEPARATOR +chainID,key);
        }catch (Exception e){
            Log.error(e);
            return  false;
        }
    }

    @Override
    public List<ChangeAgentDepositPo> getList(int chainID) throws NulsException {
        List<Entry<byte[], byte[]>> list = RocksDBService.entryList(ConsensusConstant.DB_NAME_APPEND_DEPOSIT+ConsensusConstant.SEPARATOR +chainID);
        List<ChangeAgentDepositPo> appendList = new ArrayList<>();
        if(list != null && !list.isEmpty()){
            for (Entry<byte[], byte[]> entry : list) {
                ChangeAgentDepositPo po = new ChangeAgentDepositPo();
                po.parse(entry.getValue(), 0);
                appendList.add(po);
            }
        }
        return  appendList;
    }
}

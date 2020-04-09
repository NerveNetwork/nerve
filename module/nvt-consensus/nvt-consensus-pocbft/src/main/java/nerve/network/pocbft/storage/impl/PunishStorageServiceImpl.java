package nerve.network.pocbft.storage.impl;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.rockdb.model.Entry;
import io.nuls.core.rockdb.service.RocksDBService;
import nerve.network.pocbft.constant.ConsensusConstant;
import nerve.network.pocbft.model.po.PunishLogPo;
import nerve.network.pocbft.storage.PunishStorageService;
import io.nuls.core.log.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 惩罚存贮管理实现类
 * Punishment Storage Management Implementation Class
 *
 * @author: Jason
 * 2018/11/6
 * */
@Component
public class PunishStorageServiceImpl implements PunishStorageService {
    @Override
    public boolean save(PunishLogPo po,int chainID) {
        if (po == null || po.getKey() == null) {
            return false;
        }
        try {
            return  RocksDBService.put(ConsensusConstant.DB_NAME_PUNISH+ConsensusConstant.SEPARATOR +chainID,po.getKey(),po.serialize());
        }catch (Exception  e){
            Log.error(e);
            return false;
        }
    }

    @Override
    public boolean delete(byte[] key,int chainID) {
        if(key == null){
            return false;
        }
        try {
            return RocksDBService.delete(ConsensusConstant.DB_NAME_PUNISH+ConsensusConstant.SEPARATOR +chainID,key);
        }catch (Exception e){
            Log.error(e);
            return false;
        }
    }

    @Override
    public List<PunishLogPo> getPunishList(int chainID)throws Exception{
        try {
            List<Entry<byte[], byte[]>> list = RocksDBService.entryList(ConsensusConstant.DB_NAME_PUNISH+ConsensusConstant.SEPARATOR +chainID);
            List<PunishLogPo> agentList = new ArrayList<>();
            if(list != null && !list.isEmpty()){
                for (Entry<byte[], byte[]> entry:list) {
                    PunishLogPo po = new PunishLogPo();
                    po.parse(entry.getValue(),0);
                    agentList.add(po);
                }
            }
            return  agentList;
        }catch (Exception e){
            Log.error(e);
            throw e;
        }
    }
}

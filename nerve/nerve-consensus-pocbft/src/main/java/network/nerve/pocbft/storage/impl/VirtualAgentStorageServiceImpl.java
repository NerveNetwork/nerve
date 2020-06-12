package network.nerve.pocbft.storage.impl;

import network.nerve.pocbft.model.po.VirtualAgentPo;
import network.nerve.pocbft.storage.VirtualAgentStorageService;
import network.nerve.pocbft.utils.LoggerUtil;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.rockdb.model.Entry;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.pocbft.constant.ConsensusConstant;

import java.util.*;

@Component
public class VirtualAgentStorageServiceImpl implements VirtualAgentStorageService {
    @Override
    public boolean save(VirtualAgentPo virtualAgentPo, long height){
        if(virtualAgentPo == null){
            LoggerUtil.commonLog.error("Save data is empty");
            return false;
        }
        try {
            return RocksDBService.put(ConsensusConstant.DB_NAME_VIRTUAL_AGENT_CHANGE, ByteUtils.longToBytes(height), virtualAgentPo.serialize());
        }catch (Exception e){
            LoggerUtil.commonLog.error(e);
            return false;
        }
    }

    @Override
    public VirtualAgentPo get(long height) {
        try {
            List<Entry<byte[], byte[]>> list = RocksDBService.entryList(ConsensusConstant.DB_NAME_VIRTUAL_AGENT_CHANGE);
            Map<Long, VirtualAgentPo> map = new HashMap<>(ConsensusConstant.INIT_CAPACITY_16);
            for (Entry<byte[], byte[]>entry:list) {
                long  key = ByteUtils.byteToLong(entry.getKey());
                VirtualAgentPo virtualAgentPo = new VirtualAgentPo();
                virtualAgentPo.parse(entry.getValue(),0);
                map.put(key, virtualAgentPo);
            }
            Set<Long> keySortSet = new TreeSet<>(map.keySet());
            long realKey = 0;
            for (Long key : keySortSet){
                if(key > height){
                    break;
                }
                realKey = key;
            }
            return map.get(realKey);
        }catch (Exception e){
            LoggerUtil.commonLog.error(e);
            return null;
        }
    }
}

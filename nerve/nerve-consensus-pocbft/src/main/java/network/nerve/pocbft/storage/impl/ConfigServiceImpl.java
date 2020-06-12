package network.nerve.pocbft.storage.impl;

import network.nerve.pocbft.model.bo.config.ChainConfig;
import network.nerve.pocbft.storage.ConfigService;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rockdb.model.Entry;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.pocbft.constant.ConsensusConstant;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.log.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置信息存储管理类
 * Configuration Information Storage Management Class
 *
 * @author tag
 * 2018/11/8
 * */
@Component
public class ConfigServiceImpl implements ConfigService {
    @Override
    public boolean save(ChainConfig bean, int chainID) throws Exception{
        if(bean == null){
            return  false;
        }
        return RocksDBService.put(ConsensusConstant.DB_NAME_CONFIG, ByteUtils.intToBytes(chainID), bean.serialize());
    }

    @Override
    public ChainConfig get(int chainID) {
        try {
            byte[] value = RocksDBService.get(ConsensusConstant.DB_NAME_CONFIG,ByteUtils.intToBytes(chainID));
            ChainConfig chainConfig = new ChainConfig();
            chainConfig.parse(value,0);
            return chainConfig;
        }catch (Exception e){
            Log.error(e);
            return null;
        }
    }

    @Override
    public boolean delete(int chainID) {
        try {
            return RocksDBService.delete(ConsensusConstant.DB_NAME_CONFIG,ByteUtils.intToBytes(chainID));
        }catch (Exception e){
            Log.error(e);
            return  false;
        }
    }

    @Override
    public Map<Integer, ChainConfig> getList() {
        try {
            List<Entry<byte[], byte[]>> list = RocksDBService.entryList(ConsensusConstant.DB_NAME_CONFIG);
            Map<Integer, ChainConfig> configBeanMap = new HashMap<>(ConsensusConstant.INIT_CAPACITY_4);
            for (Entry<byte[], byte[]>entry:list) {
                int key = ByteUtils.bytesToInt(entry.getKey());
                ChainConfig chainConfig = new ChainConfig();
                chainConfig.parse(entry.getValue(),0);
                configBeanMap.put(key, chainConfig);
            }
            return configBeanMap;
        }catch (Exception e){
            Log.error(e);
            return null;
        }
    }

    /*@Override
    public void afterPropertiesSet() throws NulsException {
        try {
            RocksDBService.createTable(ConsensusConstant.DB_NAME_CONFIG);
        }catch (Exception e){
            Log.error(e);
            throw new NulsException(e);
        }
    }*/
}

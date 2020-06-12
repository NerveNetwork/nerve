package network.nerve.quotation.storage.impl;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.rockdb.model.Entry;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.quotation.constant.QuotationConstant;
import network.nerve.quotation.model.bo.ConfigBean;
import network.nerve.quotation.storage.ConfigStorageService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static network.nerve.quotation.util.LoggerUtil.LOG;

@Component
public class ConfigStorageServiceImpl implements ConfigStorageService {

    @Override
    public boolean save(ConfigBean bean, int chainID) throws Exception{
        if(bean == null){
            return false;
        }
        return RocksDBService.put(QuotationConstant.DB_MODULE_CONGIF, ByteUtils.intToBytes(chainID), bean.serialize());
    }

    @Override
    public ConfigBean get(int chainID) {
        try {
            byte[] value = RocksDBService.get(QuotationConstant.DB_MODULE_CONGIF, ByteUtils.intToBytes(chainID));
            ConfigBean configBean = new ConfigBean();
            configBean.parse(new NulsByteBuffer(value));
            return configBean;
        }catch (Exception e){
            LOG.error(e);
            return null;
        }
    }

    @Override
    public boolean delete(int chainID) {
        try {
            return RocksDBService.delete(QuotationConstant.DB_MODULE_CONGIF,ByteUtils.intToBytes(chainID));
        }catch (Exception e){
            LOG.error(e);
            return  false;
        }
    }

    @Override
    public Map<Integer, ConfigBean> getList() {
        try {
            List<Entry<byte[], byte[]>> list = RocksDBService.entryList(QuotationConstant.DB_MODULE_CONGIF);
            Map<Integer, ConfigBean> configBeanMap = new HashMap<>(QuotationConstant.INIT_CAPACITY_2);
            for (Entry<byte[], byte[]>entry : list) {
                int key = ByteUtils.bytesToInt(entry.getKey());
                ConfigBean configBean = new ConfigBean();
                configBean.parse(new NulsByteBuffer(entry.getValue()));
                configBeanMap.put(key, configBean);
            }
            return configBeanMap;
        }catch (Exception e){
            LOG.error(e);
            return null;
        }
    }
}

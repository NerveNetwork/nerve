package io.nuls.test.storage;

import io.nuls.core.rockdb.service.RocksDBService;
import nerve.network.pocbft.constant.ConsensusConstant;
import nerve.network.pocbft.model.bo.config.ChainConfig;
import nerve.network.pocbft.storage.ConfigService;
import io.nuls.test.TestUtil;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
import io.nuls.core.parse.ConfigLoader;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

public class ConfigStorageTest {
    private ConfigService configService;
    @Before
    public void init(){
        try {
            Properties properties = ConfigLoader.loadProperties(ConsensusConstant.DB_CONFIG_NAME);
            String path = properties.getProperty(ConsensusConstant.DB_DATA_PATH, ConsensusConstant.DB_DATA_DEFAULT_PATH);
            RocksDBService.init(path);
            TestUtil.initTable(1);
        }catch (Exception e){
            Log.error(e);
        }
        SpringLiteContext.init(ConsensusConstant.CONTEXT_PATH);
        configService = SpringLiteContext.getBean(ConfigService.class);
    }

    @Test
    public void saveConfig()throws Exception{
        ChainConfig chainConfig = new ChainConfig();
        chainConfig.setAssetId(1);
        //chainConfig.setChainId(1);
        chainConfig.setChainId(2);
        chainConfig.setBlockMaxSize(5242880);
        chainConfig.setPackingInterval(10);
        System.out.println(configService.save(chainConfig,1));
        getConfig();
        getConfigList();
    }

    @Test
    public void getConfig(){
        ChainConfig chainConfig = configService.get(1);
        assertNotNull(chainConfig);
        System.out.println(chainConfig.getChainId());
    }

    @Test
    public void deleteConfig(){
        System.out.println(configService.delete(1));
        getConfig();
    }

    @Test
    public void getConfigList(){
        Map<Integer, ChainConfig> configBeanMap=configService.getList();
        assertNotNull(configBeanMap);
        System.out.println(configBeanMap.size());
    }
}

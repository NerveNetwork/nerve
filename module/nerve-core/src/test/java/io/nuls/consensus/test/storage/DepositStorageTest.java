package io.nuls.consensus.test.storage;

import io.nuls.base.data.NulsHash;
import io.nuls.consensus.test.TestUtil;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.model.po.DepositPo;
import io.nuls.consensus.storage.DepositStorageService;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
import io.nuls.core.parse.ConfigLoader;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

public class DepositStorageTest {
    private DepositStorageService depositStorageService;
    private NulsHash hash = NulsHash.calcHash(new byte[23]);

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
        depositStorageService = SpringLiteContext.getBean(DepositStorageService.class);
    }

    @Test
    public void saveDeposit()throws Exception{
        DepositPo po = new DepositPo();
        po.setTxHash(hash);
        po.setDelHeight(-1);
        po.setAddress(new byte[23]);
        po.setDeposit(BigInteger.valueOf(20000));
        po.setTime(NulsDateUtils.getCurrentTimeSeconds());
        po.setBlockHeight(100);
        System.out.println(depositStorageService.save(po,1));
        getDepositList();
    }

    @Test
    public void getDeposit(){
        DepositPo po = depositStorageService.get(hash,1);
        assertNotNull(po);
    }

    @Test
    public void deleteDeposit(){
        System.out.println(depositStorageService.delete(hash,1));
        getDeposit();
    }

    @Test
    public void getDepositList()throws  Exception{
        List<DepositPo> depositPos = depositStorageService.getList(1);
        System.out.println(depositPos.size());
    }
}

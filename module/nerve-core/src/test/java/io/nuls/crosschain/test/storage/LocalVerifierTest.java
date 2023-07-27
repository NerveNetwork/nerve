package io.nuls.crosschain.test.storage;

import io.nuls.common.NerveCoreConfig;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.crosschain.constant.NulsCrossChainConstant;
import io.nuls.crosschain.model.po.LocalVerifierPO;
import io.nuls.crosschain.srorage.LocalVerifierService;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static io.nuls.crosschain.constant.NulsCrossChainConstant.*;

public class LocalVerifierTest {

    private int chainId = 4;

    @BeforeClass
    public static void beforeTest() {
        /*CrossChainBootStrap.main(null);
        CrossChainBootStrap accountBootstrap = SpringLiteContext.getBean(CrossChainBootStrap.class);
        //初始化配置
        accountBootstrap.init();
        //启动时间同步线程
        localVerifierService = SpringLiteContext.getBean(LocalVerifierService.class);*/
    }

    @Test
    public void saveTest()throws Exception{
        SpringLiteContext.init(CONTEXT_PATH, "io.nuls.core.rpc.modulebootstrap", "io.nuls.core.rpc.cmd", "io.nuls.base.protocol");
        LocalVerifierService localVerifierService = SpringLiteContext.getBean(LocalVerifierService.class);
        NerveCoreConfig nulsCrossChainConfig = SpringLiteContext.getBean(NerveCoreConfig.class);
        RocksDBService.init(nulsCrossChainConfig.getDataPath() + File.separator + ModuleE.CC.name);
        RocksDBService.createTable(DB_NAME_CONSUME_LANGUAGE);
        RocksDBService.createTable(DB_NAME_CONSUME_CONGIF);
        RocksDBService.createTable(DB_NAME_LOCAL_VERIFIER);
        /*
            已注册跨链的链信息操作表
            Registered Cross-Chain Chain Information Operating Table
            key：RegisteredChain
            value:已注册链信息列表
            */
        RocksDBService.createTable(NulsCrossChainConstant.DB_NAME_REGISTERED_CHAIN);
        List<String> list = new ArrayList<>();
        list.add("11111111");
        list.add("21111111");
        LocalVerifierPO po = new LocalVerifierPO(list);
        System.out.println(localVerifierService.save(po, chainId));

        LocalVerifierPO localVerifierPO = localVerifierService.get(chainId);
        System.out.println(localVerifierPO.getVerifierList());
    }
}

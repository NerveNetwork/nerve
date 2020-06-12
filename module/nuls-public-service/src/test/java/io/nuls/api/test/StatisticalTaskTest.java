package io.nuls.api.test;

import io.nuls.api.db.mongo.MongoDBService;
import io.nuls.api.model.po.AssetSnapshotInfo;
import io.nuls.api.rpc.controller.ChainController;
import io.nuls.api.task.StatisticalTask;
import io.nuls.api.task.QueryChainInfoTask;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-23 15:53
 * @Description: 功能描述
 */
public class StatisticalTaskTest extends BaseTestCase{


    MongoDBService mongoDBService;

    StatisticalTask newStatisticalTask;



    @Before
    public void before() {
        super.before();
        new QueryChainInfoTask(2).run();
        newStatisticalTask = new StatisticalTask(2);
    }

    @Test
    public void testBuildTransactionSnapshoot(){
        List<AssetSnapshotInfo> list = newStatisticalTask.buildAsssetSnapshoot(1,860);
        list.forEach(d->{
            Log.info("{}",d);
        });
    }

    @Test
    public void testSaveStatistical(){
        newStatisticalTask.run();
    }

    @Test
    public void testSymbolReport(){
        ChainController chainController = SpringLiteContext.getBean(ChainController.class);
        Log.info("{}",chainController.symbolReport(List.of(2)));
    }



}

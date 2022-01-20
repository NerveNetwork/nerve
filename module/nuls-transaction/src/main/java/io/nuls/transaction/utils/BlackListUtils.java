package io.nuls.transaction.utils;

import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.transaction.constant.TxConfig;

import java.io.*;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author zhoulijun
 * @description TODO
 * @date 2022/1/18 16:57
 * @COPYRIGHT www.xianma360.com
 */
@Component
public class BlackListUtils implements InitializingBean,Runnable {

    public Set<String> blackList = new CopyOnWriteArraySet<>();

    @Autowired
    TxConfig config;

    /**
     * 是否不在黑名单中
     * @param address
     * @return 黑名单中存在返回false
     */
    public boolean isPass(String address){
        return !blackList.contains(address);
    }


    
    @Override
    public void afterPropertiesSet() throws NulsException {
        if(StringUtils.isBlank(config.getBlackListPath())){
            Log.warn("未配置黑名单地址");
            return ;
        }
        if(!new File(config.getBlackListPath()).exists()){
            Log.warn("黑名单地址文件不存在，黑名单地址为空");
            return ;
        }
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(this, 2, 60, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        try {
            blackList.clear();
            FileReader reader = new FileReader(config.getBlackListPath());
            BufferedReader buff = new BufferedReader(reader);
            String line = buff.readLine();
            while(line != null){
                blackList.add(line);
                line = buff.readLine();
            }
            buff.close();
            Log.info("重置黑名单地址完成，共记录{}个黑名单地址", blackList.size());
        } catch (FileNotFoundException e) {
            Log.error("黑名单地址错误，文件不存在",e);
            System.exit(0);
        } catch (IOException e) {
            Log.error("读取黑名单文件错误",e);
            System.exit(0);
        }
    }
}

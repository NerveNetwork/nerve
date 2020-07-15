/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.block.thread.monitor;

import io.nuls.block.constant.StatusEnum;
import io.nuls.block.manager.ContextManager;
import io.nuls.block.model.ChainContext;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;

import java.io.*;

/**
 * 系统停止时，先停止区块保存服务，确保不要产生事务性问题
 *
 * @author captain
 * @version 1.0
 * @date 18-11-14 下午3:54
 */
public class StopingMonitor extends BaseMonitor {

    private static String FILE_NAME;
    private static File STOP_FILE;
    private static final StopingMonitor INSTANCE = new StopingMonitor();

    private static final String ACTIVE = "1";

    private static final String DONE = "2";

    private StopingMonitor() {
        super();
        FILE_NAME = System.getenv("NERVE_STOP_FILE");
        Log.info("NERVE_STOP_FILE:{}", FILE_NAME);
        if (StringUtils.isBlank(FILE_NAME)) {
            Log.warn("There is no env:NERVE_STOP_FILE");
            FILE_NAME = "NERVE_STOP_FILE";
        }
        STOP_FILE = new File(FILE_NAME);
        this.clear();
    }

    public static StopingMonitor getInstance() {
        return INSTANCE;
    }


    @Override
    public void run() {
        for (Integer chainId : ContextManager.CHAIN_ID_LIST) {
            ChainContext context = ContextManager.getContext(chainId);
            NulsLogger logger = context.getLogger();
            try {
                process(chainId, context, logger);
            } catch (Exception e) {
                logger.error(symbol + " running fail", e);
            }
        }
    }


    @Override
    protected void process(int chainId, ChainContext context, NulsLogger commonLog) {
        if (!STOP_FILE.exists()) {
            return;
        }
        try {
            commonLog.debug("读取到停止信号文件:{}", STOP_FILE);
            String val = read();
            commonLog.info("读取到停止信号:{}", val);
            if (val == null) {
                return;
            }
            if (ACTIVE.equals(val)) {
                commonLog.info("开始进行停止节点准备工作");
                context.stopBlock();
                context.waitStopBlock();
                commonLog.info("停止节点准备工作完成");
                write(DONE);
            }
        } catch (Exception e) {
            commonLog.error(e);
        }
    }

    private void write(String val) throws IOException {
        FileWriter out = new FileWriter(STOP_FILE);
        try {
            out.write(val);
            out.flush();
        } catch (IOException e) {
            throw e;
        } finally {
            out.close();
        }
    }

    private String read() throws IOException {

        BufferedReader in = new BufferedReader(new FileReader(STOP_FILE));
        try {
            String line = in.readLine();
            if (line != null) {
                return line.substring(0, 1);
            }
            return null;
        } finally {
            in.close();
        }
    }

    public static void main(String[] args) throws IOException {
        StopingMonitor test = StopingMonitor.getInstance();

        test.write(ACTIVE);
        System.out.println(test.read());
    }

    public void clear() {
        if (!STOP_FILE.exists()) {
            return;
        }
        STOP_FILE.delete();
    }

}

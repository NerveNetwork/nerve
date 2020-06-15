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

    private StopingMonitor() {
        super();
        FILE_NAME = System.getenv("NERVE_STOP_FILE");
        Log.info("NERVE_STOP_FILE:{}",FILE_NAME);
        if(StringUtils.isBlank(FILE_NAME)){
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
    protected void process(int chainId, ChainContext context, NulsLogger commonLog) {
        if (!STOP_FILE.exists()) {
            return;
        }
        try {
            byte val = read();
            if (val == 0) {
                return;
            }
            if (val == 1) {
                Log.info("监听到停止节点信号");
                context.stopBlock();
                context.waitStopBlock();
                Log.info("停止节点操作准备就绪");
                write(2);
            }
        } catch (Exception e) {
            commonLog.error(e);
        }
    }

    private void write(int val) throws IOException {
        OutputStream out = new FileOutputStream(STOP_FILE);
        try {
            out.write(val);
            out.flush();
        } catch (IOException e) {
            throw e;
        } finally {
            out.close();
        }
    }

    private byte read() throws IOException {
        InputStream in = new FileInputStream(STOP_FILE);
        try {
            byte[] bytes = in.readAllBytes();
            if (bytes != null && bytes.length > 0) {
                return bytes[0];
            }
            return 0;
        } finally {
            in.close();
        }
    }

    public static void main(String[] args) throws IOException {
        StopingMonitor test = StopingMonitor.getInstance();
        test.write(1);


    }

    public void clear() {
        if (!STOP_FILE.exists()) {
            return;
        }
        STOP_FILE.delete();
    }

}

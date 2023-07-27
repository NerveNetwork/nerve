/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2019 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.transaction.model.po;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.Transaction;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.transaction.utils.TxUtil;

import java.io.IOException;
import java.util.Arrays;

import static io.nuls.transaction.utils.LoggerUtil.LOG;

/**
 * 收到网络广播的交易后，记录发送者，转发时排除发送者
 * @author: Charlie
 * @date: 2019/4/23
 */
public class TransactionNetPO extends BaseNulsData implements Comparable<TransactionNetPO>{

    private Transaction tx;

    private String excludeNode;

    /**
     * 主要用于孤儿交易排序
     */
    private transient long orphanSortSerial;

    public TransactionNetPO() {
    }

    public TransactionNetPO(Transaction tx) {
        this.tx = tx;
    }

    public TransactionNetPO(Transaction tx, String excludeNode) {
        this.tx = tx;
        this.excludeNode = excludeNode;
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeNulsData(tx);
        stream.writeString(excludeNode);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.tx = byteBuffer.readNulsData(new Transaction());
        this.excludeNode = byteBuffer.readString();
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfNulsData(tx);
        size += SerializeUtils.sizeOfString(excludeNode);
        return size;
    }

    public Transaction getTx() {
        return tx;
    }

    public void setTx(Transaction tx) {
        this.tx = tx;
    }

    public String getExcludeNode() {
        return excludeNode;
    }

    public void setExcludeNode(String excludeNode) {
        this.excludeNode = excludeNode;
    }

    public long getOrphanSortSerial() {
        return orphanSortSerial;
    }

    public void setOrphanSortSerial(long orphanSortSerial) {
        this.orphanSortSerial = orphanSortSerial;
    }

    @Override
    public int compareTo(TransactionNetPO o) {
        if(this.orphanSortSerial<o.getOrphanSortSerial()){
            return -1;
        }else if(this.orphanSortSerial>o.getOrphanSortSerial()){
            return 1;
        }
        Transaction o1 = this.getTx();
        Transaction o2 = o.getTx();
        if (null == o1 && null == o2) {
            return 0;
        }
        if (null == o1) {
            return 1;
        }
        if (null == o2) {
            return -1;
        }
        if (o1.equals(o2)) {
            return 0;
        }

        //比较交易hash和nonce的关系
        try {
            if (null == o1.getCoinData() && null == o2.getCoinData()) {
                return 0;
            }
            if (null == o1.getCoinData()) {
                return 1;
            }
            if (null == o2.getCoinData()) {
                return -1;
            }

            CoinData o1CoinData = o1.getCoinDataInstance();
            CoinData o2CoinData = o2.getCoinDataInstance();
            if (null == o1CoinData && null == o2CoinData) {
                return 0;
            }
            if (null == o1CoinData) {
                return 1;
            }
            if (null == o2CoinData) {
                return -1;
            }
            if (null == o1CoinData.getFrom() && null == o2CoinData.getFrom()) {
                return 0;
            }
            if (null == o1CoinData.getFrom()) {
                return 1;
            }
            if (null == o2CoinData.getFrom()) {
                return -1;
            }
            byte[] o2HashPrefix = TxUtil.getNonce(o2.getHash().getBytes());
            for (CoinFrom o1CoinFrom : o1CoinData.getFrom()) {
                if (Arrays.equals(o2HashPrefix, o1CoinFrom.getNonce())) {
                    //o1其中一个账户的nonce等于o2的hash，则需要交换位置(说明o2是o1的前一笔交易)
                    //命中一个from直接返回
                    return 1;
                }
            }

            byte[] o1HashPrefix = TxUtil.getNonce(o1.getHash().getBytes());
            for (CoinFrom o2CoinFrom : o2CoinData.getFrom()) {
                if (Arrays.equals(o1HashPrefix, o2CoinFrom.getNonce())) {
                    //o2其中一个账户的nonce等于o1的hash，则不需要交换位置(说明o1是o2的前一笔交易)
                    //命中一个from直接返回
                    return -1;
                }
            }
            return 0;
        } catch (NulsException e) {
            LOG.error(e);
            return 0;
        }
    }
}

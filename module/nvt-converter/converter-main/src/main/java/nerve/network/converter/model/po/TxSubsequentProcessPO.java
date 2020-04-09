/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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

package nerve.network.converter.model.po;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import nerve.network.converter.model.bo.VirtualBankDirector;
import io.nuls.core.constant.SyncStatusEnum;

import java.io.Serializable;
import java.util.List;

/**
 * 等待后续处理的确认交易
 * 可能是 调用组件[提现交易、银行变更交易（创建异构链地址，创建(修改)多签地址、）], 或生成新的交易
 * @author: Chino
 * @date: 2020-03-09
 */
public class TxSubsequentProcessPO implements Serializable {

    /**
     * 待处理交易
     */
    private Transaction tx;

    /**
     * 加入虚拟银行成员列表 (需要创建异构地址)
     */
    private List<VirtualBankDirector> listInDirector;

    /**
     * 退出虚拟银行成员列表
     */
    private List<VirtualBankDirector> listOutDirector;

    /**
     * 处理时先验证交易是否确认的验证次数（达到阈值交易没确认则丢弃）
     */
    private int isConfirmedVerifyCount;

    /**
     * 交易所在区块头
     */
    private BlockHeader blockHeader;

    /**
     * 节点区块同步模式
     */
    private SyncStatusEnum syncStatusEnum;


    public TxSubsequentProcessPO() {
    }

    public TxSubsequentProcessPO(Transaction tx) {
        this.tx = tx;
    }

    public int getIsConfirmedVerifyCount() {
        return isConfirmedVerifyCount;
    }

    public void setIsConfirmedVerifyCount(int isConfirmedVerifyCount) {
        this.isConfirmedVerifyCount = isConfirmedVerifyCount;
    }

    public Transaction getTx() {
        return tx;
    }

    public void setTx(Transaction tx) {
        this.tx = tx;
    }

    public List<VirtualBankDirector> getListInDirector() {
        return listInDirector;
    }

    public void setListInDirector(List<VirtualBankDirector> listInDirector) {
        this.listInDirector = listInDirector;
    }

    public List<VirtualBankDirector> getListOutDirector() {
        return listOutDirector;
    }

    public void setListOutDirector(List<VirtualBankDirector> listOutDirector) {
        this.listOutDirector = listOutDirector;
    }

    public BlockHeader getBlockHeader() {
        return blockHeader;
    }

    public void setBlockHeader(BlockHeader blockHeader) {
        this.blockHeader = blockHeader;
    }

    public SyncStatusEnum getSyncStatusEnum() {
        return syncStatusEnum;
    }

    public void setSyncStatusEnum(SyncStatusEnum syncStatusEnum) {
        this.syncStatusEnum = syncStatusEnum;
    }
}

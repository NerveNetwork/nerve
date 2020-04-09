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
package nerve.network.pocnetwork.model.message.sub;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import nerve.network.pocnetwork.model.ConsensusNet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lanjinsheng
 * @date 2019/10/17
 * @description 分享列表
 */
public class ConsensusShare extends BaseNulsData {
    private List<ConsensusNet> shareList = new ArrayList<>();
    public ConsensusShare() {
        super();
    }
    @Override
    public void serializeToStream(NulsOutputStreamBuffer buffer) throws IOException {
        buffer.writeUint16(shareList.size());
        for (ConsensusNet consensusNet : shareList) {
            buffer.writeNulsData(consensusNet);
        }
    }

    @Override
    public void parse(NulsByteBuffer nulsByteBuffer) throws NulsException {
        int size = nulsByteBuffer.readUint16();
        for (int i = 0; i < size; i++) {
            ConsensusNet consensusNet = new ConsensusNet();
            nulsByteBuffer.readNulsData(consensusNet);
            shareList.add(consensusNet);
        }

    }

    @Override
    public int size() {
        int size = SerializeUtils.sizeOfUint16();
        for (ConsensusNet consensusNet : shareList) {
            size += SerializeUtils.sizeOfNulsData(consensusNet);
        }
        return size;
    }

    public List<ConsensusNet> getShareList() {
        return shareList;
    }

    public void setShareList(List<ConsensusNet> shareList) {
        this.shareList = shareList;
    }
}

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
package network.nerve.pocbft.network.model.message;

import network.nerve.pocbft.network.model.ConsensusNet;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.base.data.NulsSignData;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.pocbft.network.model.message.sub.ConsensusIdentitiesSub;

import java.io.IOException;

/**
 * @author lanjinsheng
 * @date 2019/10/17
 * @description 用于广播身份消息
 */
public class ConsensusIdentitiesMsg extends BaseBusinessMessage {
    private ConsensusIdentitiesSub consensusIdentitiesSub = new ConsensusIdentitiesSub();
    //对identityList的签名
    private P2PHKSignature sign = null;
    private String nodeId;
    private String msgStr;

    public ConsensusIdentitiesMsg(ConsensusNet consensusNet) {
        consensusIdentitiesSub.setConsensusNet(consensusNet);
        consensusIdentitiesSub.setMessageTime(NulsDateUtils.getCurrentTimeSeconds());
    }

    public ConsensusIdentitiesMsg() {
        super();
    }

    public ConsensusIdentitiesSub getConsensusIdentitiesSub() {
        return consensusIdentitiesSub;
    }

    public void setConsensusIdentitiesSub(ConsensusIdentitiesSub consensusIdentitiesSub) {
        this.consensusIdentitiesSub = consensusIdentitiesSub;
    }

    public void signDatas(byte []privKey) throws IOException {
        ECKey  ecKey = ECKey.fromPrivate(privKey);
        NulsSignData nulsSignData = SignatureUtil.signDigest(consensusIdentitiesSub.serialize(),ecKey);
        sign = new P2PHKSignature(nulsSignData, ecKey.getPubKey());
    }
    public P2PHKSignature getSign() {
        return sign;
    }

    public void setSign(P2PHKSignature sign) {
        this.sign = sign;
    }

    public void addEncryptNodes(byte[] pubKey) throws IOException {
        consensusIdentitiesSub.addEncryptNodes(pubKey);
    }

    @Override
    public void serializeToStream(NulsOutputStreamBuffer buffer) throws IOException {

        buffer.writeNulsData(consensusIdentitiesSub);
        buffer.writeNulsData(sign);
    }

    @Override
    public void parse(NulsByteBuffer nulsByteBuffer) throws NulsException {

        this.consensusIdentitiesSub = nulsByteBuffer.readNulsData(new ConsensusIdentitiesSub());
        this.sign = nulsByteBuffer.readNulsData(new P2PHKSignature());
    }

    @Override
    public int size() {
        int size = 0;
        size += SerializeUtils.sizeOfNulsData(consensusIdentitiesSub);
        size += SerializeUtils.sizeOfNulsData(sign);
        return size;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getMsgStr() {
        return msgStr;
    }

    public void setMsgStr(String msgStr) {
        this.msgStr = msgStr;
    }
}

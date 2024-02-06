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
package io.nuls.consensus.network.model.message;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.network.model.message.sub.ConsensusShare;
import io.nuls.core.crypto.ECIESUtil;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.consensus.network.model.ShareMsgSignResult;
import io.nuls.consensus.rpc.call.CallMethodUtils;

import java.io.IOException;
import java.util.Map;

/**
 * @author lanjinsheng
 * @date 2019/10/17
 * @description Used for broadcasting identity messages
 */
public class ConsensusShareMsg extends BaseBusinessMessage {
    /**
     * ciphertext contain nodeId,pubKeyInformationList
     */
    private byte[] identityList;
    private long messageTime;
    //rightidentityListSignature of
    private P2PHKSignature sign = null;
    private transient ConsensusShare consensusShare;
    private String nodeId;

    public ConsensusShareMsg(Chain chain, ConsensusShare consensusShare, byte[] peerPubKey, String address) throws Exception {
        this.identityList = ECIESUtil.encrypt(peerPubKey, consensusShare.serialize());
        this.messageTime = NulsDateUtils.getCurrentTimeMillis();
        byte[] signResult = CallMethodUtils.signature(chain, address, identityList, Map.of("method", "idListEncryptAndSign",
                "share", HexUtil.encode(consensusShare.serialize()),
                "peerPubkey", HexUtil.encode(peerPubKey)));
        if (signResult.length > 120) {
            ShareMsgSignResult result = new ShareMsgSignResult();
            result.parse(signResult, 0);
            this.identityList = result.getIdentityList();
            P2PHKSignature _sign = new P2PHKSignature();
            _sign.parse(result.getSignatrue(), 0);
            sign = _sign;
        } else {
            P2PHKSignature _sign = new P2PHKSignature();
            _sign.parse(signResult, 0);
            sign = _sign;
        }
    }

    public static void main(String[] args) throws NulsException {
        String hex = "e104acbf88d166be5abf5ecf478f68e366b6b215c6894b0fef3a34a771cca1a677a57b1d3f1cf8877a276e13c0c28faa99d99b477939dd1517e10e139106171ed1cb00000000000000000000000000000000324f99da85e04630c5fe3fbd8abea38b3d27d1b1e7779c790c3eca416050317c1c7912a4d8aaa2ea0dc42bd15769bd386a4625c449ea9e1db2a8f79d9f34d23e213c8b362927a5385fef84a255e5cfa893dbbed7943ff38b86d20f987b63a342266af4228eb42dc06a4df75a336a4c5e6f0ec839754d61e818b9ac4e537d5ca42134dea33894d6fd4a474236db429d3a69210308784e3d4aff68a24964968877b39d22449596c1c789136a4e25e2db7819826046304402205025129f01f1a926660dd66031acae091f0ac54451f08d5b47df34469c41011202203ebba74f3131ac197c46edff5f86446170c78e2fb295ec48159512e80f094376";
        byte[] signResult = HexUtil.decode(hex);
        ShareMsgSignResult result = new ShareMsgSignResult();
        result.parse(signResult, 0);
        P2PHKSignature _sign = new P2PHKSignature();
        _sign.parse(result.getSignatrue(), 0);
        System.out.println(result.getSignatrue().length);
    }

    public ConsensusShareMsg() {
    }

    public ConsensusShare getDecryptConsensusShare(Chain chain, String address) {
        try {
//            byte[] decryptResult = ECIESUtil.decrypt(privKey, );
            String digest = HexUtil.encode(identityList);
            byte[] decryptResult = CallMethodUtils.prikeyDecrpyt(chain, address, digest, Map.of("method", "idListDecrypt", "identities", digest));

            ConsensusShare consensusShare = new ConsensusShare();
            consensusShare.parse(decryptResult, 0);
            return consensusShare;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeBytesWithLength(identityList);
        stream.writeUint48(messageTime);
        stream.writeNulsData(sign);
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.identityList = byteBuffer.readByLengthByte();
        this.messageTime = byteBuffer.readUint48();
        this.sign = byteBuffer.readNulsData(new P2PHKSignature());
    }

    @Override
    public int size() {
        int size = SerializeUtils.sizeOfBytes(identityList);
        size += SerializeUtils.sizeOfUint48();
        size += SerializeUtils.sizeOfNulsData(sign);
        return size;
    }


    public byte[] getIdentityList() {
        return identityList;
    }

    public void setIdentityList(byte[] identityList) {
        this.identityList = identityList;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }

    public ConsensusShare getConsensusShare() {
        return consensusShare;
    }

    public void setConsensusShare(ConsensusShare consensusShare) {
        this.consensusShare = consensusShare;
    }

    public P2PHKSignature getSign() {
        return sign;
    }

    public void setSign(P2PHKSignature sign) {
        this.sign = sign;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /*public static void main(String[] args) {

        ECKey ecKey = new ECKey();
        ConsensusShareMsg consensusShareMsg = new ConsensusShareMsg();
        ConsensusNet consensusNet = new ConsensusNet();
        consensusNet.setNodeId("192.168.1.155");
        try {
            consensusShareMsg.setIdentityList(consensusNet.serialize());
            NulsSignData nulsSignData = SignatureUtil.signDigest(consensusShareMsg.serialize(), ecKey);
            P2PHKSignature p2phks = new P2PHKSignature(nulsSignData, ecKey.getPubKey());
            System.out.println(SignatureUtil.validateSignture(consensusShareMsg.serialize(), p2phks));
            consensusNet.setNodeId("192.168.1.156");
            consensusShareMsg.setIdentityList(consensusNet.serialize());
            System.out.println(SignatureUtil.validateSignture(consensusShareMsg.serialize(), p2phks));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NulsException e) {
            e.printStackTrace();
        }
    }*/
}

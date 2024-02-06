/*
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.consensus.network.message.v1;

import io.nuls.consensus.network.constant.NetworkCmdConstant;
import io.nuls.consensus.utils.manager.ChainManager;
import io.nuls.consensus.v1.utils.HashSetDuplicateProcessor;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.network.model.message.ConsensusShareMsg;
import io.nuls.base.RPCUtil;
import io.nuls.base.data.NulsHash;
import io.nuls.base.protocol.MessageProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;


/**
 * Process received{@link ConsensusShareMsg},Consensus node information sharing
 *
 * @author lanjinsheng
 * @version 1.0
 * @date 2019-10-17
 */
@Component("ConsensusShareProcessorV1")
public class ConsensusShareProcessor implements MessageProcessor {
    @Autowired
    private ChainManager chainManager;

    private HashSetDuplicateProcessor<String> duplicateProcessor = new HashSetDuplicateProcessor<>(128);

    @Override
    public String getCmd() {
        return NetworkCmdConstant.POC_SHARE_MESSAGE;
    }

    public String getLast8ByteByNulsHash(NulsHash hash) {
        byte[] out = new byte[8];
        byte[] in = hash.getBytes();
        int copyEnd = in.length;
        System.arraycopy(in, (copyEnd - 8), out, 0, 8);
        return HexUtil.encode(out);
    }

    public boolean duplicateMsg(ConsensusShareMsg message) {
        String simpleHash = getLast8ByteByNulsHash(message.getMsgHash());

        return !duplicateProcessor.insertAndCheck(simpleHash);
    }

    @Override
    public void process(int chainId, String nodeId, String msgStr) {
        Chain chain = chainManager.getChainMap().get(chainId);
        ConsensusShareMsg message = RPCUtil.getInstanceRpcStr(msgStr, ConsensusShareMsg.class);
        if (message == null || duplicateMsg(message)) {
            return;
        }
        message.setNodeId(nodeId);
        chain.getConsensusCache().getShareMessageQueue().offer(message);
//        //chain.getLogger().debug("=====================ConsensusShareProcessor deal end");
    }
}

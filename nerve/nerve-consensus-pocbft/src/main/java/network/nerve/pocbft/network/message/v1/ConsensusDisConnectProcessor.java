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
package network.nerve.pocbft.network.message.v1;

import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.network.model.ConsensusKeys;
import network.nerve.pocbft.network.model.message.ConsensusIdentitiesMsg;
import network.nerve.pocbft.utils.manager.ChainManager;
import io.nuls.base.RPCUtil;
import io.nuls.base.data.NulsHash;
import io.nuls.base.protocol.MessageProcessor;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import network.nerve.pocbft.network.service.ConsensusNetService;
import network.nerve.pocbft.network.service.NetworkService;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static network.nerve.pocbft.network.constant.NetworkCmdConstant.POC_DIS_CONN_MESSAGE;


/**
 * 处理收到的{@link ConsensusIdentitiesMsg},清除IP
 *
 * @author lanjinsheng
 * @version 1.0
 * @date 2019-10-17
 */
@Component("ConsensusDisConnectProcessorV1")
public class ConsensusDisConnectProcessor implements MessageProcessor {
    @Autowired
    private ChainManager chainManager;
    Map<String, Integer> MSG_HASH_MAP = new ConcurrentHashMap<>();
    int WARNING_HASH_SIZE = 1000;
    int MAX_HASH_SIZE = 1200;
    Map<String, Integer> MSG_HASH_MAP_TEMP = new ConcurrentHashMap<>();
    @Autowired
    ConsensusNetService consensusNetService;
    @Autowired
    NetworkService networkService;

    @Override
    public String getCmd() {
        return POC_DIS_CONN_MESSAGE;
    }

    public String getLast8ByteByNulsHash(NulsHash hash) {
        byte[] out = new byte[8];
        byte[] in = hash.getBytes();
        int copyEnd = in.length;
        System.arraycopy(in, (copyEnd - 8), out, 0, 8);
        return HexUtil.encode(out);
    }

    public boolean duplicateMsg(ConsensusIdentitiesMsg message) {
        String simpleHash = getLast8ByteByNulsHash(message.getMsgHash());
        if (null != MSG_HASH_MAP.get(simpleHash)) {
            return true;
        }
        if (MSG_HASH_MAP.size() > MAX_HASH_SIZE) {
            MSG_HASH_MAP.clear();
            MSG_HASH_MAP.putAll(MSG_HASH_MAP_TEMP);
            MSG_HASH_MAP_TEMP.clear();
        }
        if (MSG_HASH_MAP.size() > WARNING_HASH_SIZE) {
            MSG_HASH_MAP_TEMP.put(simpleHash, 1);
        }
        MSG_HASH_MAP.put(simpleHash, 1);
        return false;
    }

    @Override
    public void process(int chainId, String nodeId, String msgStr) {
        Chain chain = chainManager.getChainMap().get(chainId);
        ConsensusIdentitiesMsg message = RPCUtil.getInstanceRpcStr(msgStr, ConsensusIdentitiesMsg.class);
        if (message == null || duplicateMsg(message)) {
            return;
        }
        message.setNodeId(nodeId);
        chain.getConsensusCache().getDisConnectMessageQueue().offer(message);

        //chain.getLogger().debug("=====================consensusDisConnProcessor deal end");
    }
}

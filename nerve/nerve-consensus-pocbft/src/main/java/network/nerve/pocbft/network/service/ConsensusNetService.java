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
package network.nerve.pocbft.network.service;

import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.network.model.ConsensusKeys;
import network.nerve.pocbft.network.model.ConsensusNet;

import java.util.List;
import java.util.Set;

/**
 * @author lanjinsheng
 * @date 2019/10/17
 * @description
 */
public interface ConsensusNetService {
//    /**
//     * @param consensusPubKeys
//     * @param consensusAddrs,
//     * @param updateType       1 增加  2 删除
//     * @return
//     * @description 更新共识列表, 增加或者减少节点时候调用. 只知道地址时候就只给地址
//     */
//    boolean updateConsensusList(int chainId, List<byte[]> consensusPubKeys, List<String> consensusAddrs, short updateType);
//    /**
//     * @param chainId
//     * @param selfPubKey
//     * @param selfPrivKey
//     * @param consensusPubKeyList
//     * @param consensusAddrList
//     * @return
//     * @description 在成为共识节点时候调用，有公钥的，不用在地址列表里。如果只有共识节点地址的，可以给地址列表consensusAddrList
//     */
//    boolean initConsensusNetwork(int chainId, byte[] selfPubKey, byte[] selfPrivKey, List<byte[]> consensusPubKeyList, Set<String> consensusAddrList);

    /**
     * @param chainId
     * @param selfPubKey
     * @param selfPrivKey
     * @param consensusSeedPubKeyList
     * @return
     * @description 自身从共识节点变为普通节点时候调用
     */
    boolean createConsensusNetwork(int chainId, byte[] selfPubKey, List<byte[]> consensusSeedPubKeyList, Set<String> consensusAddrList);

    /**
     * @param chainId
     * @param consensusAddrList
     * @return
     */
    boolean updateConsensusList(int chainId, Set<String> consensusAddrList);

    /**
     * @param chainId
     * @description 自身从共识节点变为普通节点时候调用
     */
    void cleanConsensusNetwork(int chainId);

    /**
     * 广播共识消息
     *
     * @param chainId
     * @param cmd
     * @param messageBodyHex
     * @return
     */
    List<String> broadCastConsensusNet(int chainId, String cmd, String messageBodyHex, String excludeNodes);


    ConsensusKeys getSelfConsensusKeys(int chainId);

    ConsensusNet getConsensusNode(int chainId, String address);

    boolean updateConsensusNode(int chainId, ConsensusNet consensusNet, boolean isConnect);

    boolean updateConsensusNode(Chain chain, ConsensusNet consensusNet);

    boolean disConnNode(Chain chain, byte[] pubKey);

    boolean netStatusChange(Chain chain);

    boolean reCalConsensusNet(Chain chain, List<String> ips);

    boolean getNetStatus(Chain chain);

    List<ConsensusNet> getHadConnConsensusNetList(Chain chain);

    List<ConsensusNet> getAllConsensusNetList(Chain chain);

    List<ConsensusNet> getUnConnectConsensusNetList(Chain chain);

    boolean reShareSelf(Chain chain);

    boolean allConnected(Chain chain);

    boolean broadCastConsensusNetSync(int chainId, String cmd, String messageBodyHex, String excludeNodes);
    boolean broadCastConsensusNetHalfSync(int chainId, String cmd, String messageBodyHex, String excludeNodes);
}

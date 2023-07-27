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
package io.nuls.consensus.network.constant;

public interface NetworkCmdConstant {
    //message
    String POC_IDENTITY_MESSAGE = "pocIdentity";
    String POC_SHARE_MESSAGE = "pocShare";
    String POC_DIS_CONN_MESSAGE = "pocDisConn";
    //call
    String NW_SEND_PEER = "nw_sendPeersMsg";
    String NW_GET_EXTRANET_IP = "nw_extranet_ip";
    String NW_BROADCAST_JOIN_CONSENSUS = "nw_broadcast_join_consensus";
    String NW_BROADCAST_CONSENSUS_NET = "nw_sendByIps";
    String NW_GROUP_FLAG="POC";
}

/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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

package network.nerve.converter.core.processor;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterCmdConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.enums.ByzantineStateEnum;
import network.nerve.converter.message.BroadcastHashSignMessage;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.TransactionPO;
import network.nerve.converter.rpc.call.NetWorkCall;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.TxStorageService;
import network.nerve.converter.utils.VirtualBankUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 消息交易处理类
 *
 * @author: Loki
 * @date: 2020/4/14
 */
@Component
public class TransactionMsgProcessor {

    @Autowired
    private static TxStorageService txStorageService;

    /**
     * 对本链广播的交易进行处理
     *
     * @param chain   本链信息
     * @param hash    交易缓存HASH
     * @param nodeId  发送节点ID
     * @param hashHex 交易Hash字符串（用于日志打印）
     */
    public static void handleSignMessageByzantine(Chain chain, NulsHash hash, String nodeId, BroadcastHashSignMessage messageBody, String hashHex) {
        try {
            TransactionPO txPO = txStorageService.get(chain, hash);
            //如果交易已经拜占庭成功了, 就不用再广播该交易的签名
            if (txPO.getStatus() != ByzantineStateEnum.UNTREATED.getStatus() || messageBody.getP2PHKSignature() == null) {
                chain.getLogger().info("交易在本节点已经处理完成,Hash:{}\n\n", hashHex);
                return;
            }
            P2PHKSignature p2PHKSignature = messageBody.getP2PHKSignature();
            Transaction tx = txPO.getTx();
            String signHex = HexUtil.encode(p2PHKSignature.getBytes());
            // 判断之前是否有收到过该签名(交易里面是否已含有该签名)
            TransactionSignature signature = new TransactionSignature();
            // 如果nodeId为空表示是自己节点创建的交易
            if (tx.getTransactionSignature() != null) {
                signature.parse(tx.getTransactionSignature(), 0);
                for (P2PHKSignature sign : signature.getP2PHKSignatures()) {
                    if (Arrays.equals(messageBody.getP2PHKSignature().getBytes(), sign.serialize())) {
                        chain.getLogger().debug("本节点已经收到过该交易的该签名,Hash:{}, 签名地址:{}\n\n", hashHex,
                                AddressTool.getStringAddressByBytes(AddressTool.getAddress(p2PHKSignature.getPublicKey(), chain.getChainId())));
                        return;
                    }
                }
            } else {
                List<P2PHKSignature> p2PHKSignatureList = new ArrayList<>();
                signature.setP2PHKSignatures(p2PHKSignatureList);
            }

            // 判断签名发送者是否有资格签名(是否当前虚拟银行成员)
            String signAddress = AddressTool.getStringAddressByBytes(AddressTool.getAddress(p2PHKSignature.getPublicKey(), chain.getChainId()));
            if (!chain.isVirtualBankBySignAddr(signAddress)) {
                //签名地址不是虚拟银行节点
                chain.getLogger().error(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT.getMsg());
                return;
            }

            //验证签名本身正确性
            if (!ECKey.verify(tx.getHash().getBytes(), p2PHKSignature.getSignData().getSignBytes(), p2PHKSignature.getPublicKey())) {
                chain.getLogger().error("[非法签名] 签名验证不通过!. hash:{}, 签名hex:{}\n\n", signHex);
                return;
            }
            // 把当前签名加入签名列表
            signature.getP2PHKSignatures().add(p2PHKSignature);
            signByzantine(chain, txPO, messageBody, signature, nodeId);
        } catch (NulsException e) {
            chain.getLogger().error(e);
        } catch (IOException io) {
            chain.getLogger().error(io);
        }
    }


    /**
     * 交易签名拜占庭处理
     * @param chain
     * @param txPO
     * @param messageBody
     * @param signature
     * @param excludeNodes
     * @throws NulsException
     * @throws IOException
     */
    public static void signByzantine(Chain chain, TransactionPO txPO, BroadcastHashSignMessage messageBody, TransactionSignature signature, String excludeNodes) throws NulsException, IOException {

        Transaction tx = txPO.getTx();
        if (signByzantineInChain(chain, tx, signature)) {
            // 表示交易已经拜占庭验证成功
            txPO.setStatus(ByzantineStateEnum.PASSED.getStatus());
            /**
             * 签名验证通过
             */
            try {
                TransactionCall.newTx(chain, tx);
            } catch (NulsException e) {
                if(!"tx_0013".equals(e.getErrorCode().getCode())){
                    throw e;
                }
            }
        }
        txStorageService.save(chain, txPO);
        // 广播这个收到的签名
        NetWorkCall.broadcast(chain, messageBody, excludeNodes, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
        chain.getLogger().info("广播签名消息给其他节点, Hash:{}, ", tx.getHash().toHex());
    }

    /**
     * 交易签名拜占庭验证
     *
     * @param chain                  本链信息
     * @param tx                     交易
     * @param signature              签名列表
     * @return 拜占庭验证是否通过
     */
    public static boolean signByzantineInChain(Chain chain, Transaction tx, TransactionSignature signature) throws IOException {
        //交易签名拜占庭
        int byzantineCount = VirtualBankUtil.getByzantineCount(chain);
        int signCount = signature.getSignersCount();
        if (signCount >= byzantineCount) {
            // 交易签名拜占庭, 获取所有虚拟银行成员签名地址
            Set<String> directorSignAddressSet = chain.getMapVirtualBank().keySet();
            // 统计有效的签名个数(排除不是当前虚拟银行成员的签名和重复签名)
            signCount = VirtualBankUtil.getSignCountWithoutMisMatchSigns(chain, signature, directorSignAddressSet);
            if (signCount >= byzantineCount) {
                tx.setTransactionSignature(signature.serialize());
                chain.getLogger().info("拜占庭签名数验证通过, hash:{}, 当前虚拟银行成员数:{}, 当前签名数:{}, 需达到的签名数:{}", tx.getHash().toHex(),chain.getMapVirtualBank().size(), signCount, byzantineCount);
                return true;
            } else {
                tx.setTransactionSignature(signature.serialize());
            }
        } else {
            tx.setTransactionSignature(signature.serialize());
        }
        chain.getLogger().info("拜占庭签名数不足, hash:{}, 需达到的签名数:{}, 当前签名数:{}", tx.getHash().toHex(), byzantineCount, signCount);
        return false;
    }

}

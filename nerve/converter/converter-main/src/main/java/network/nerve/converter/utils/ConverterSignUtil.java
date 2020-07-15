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

package network.nerve.converter.utils;

import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.dto.SignAccountDTO;
import network.nerve.converter.rpc.call.AccountCall;
import network.nerve.converter.rpc.call.ConsensusCall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Loki
 * @date: 2020/5/12
 */
public class ConverterSignUtil {

    /**
     * 签名(已存在签名,则追加签名)
     *
     * @param tx
     * @param signAccountDTO
     * @throws NulsException
     */
    public static P2PHKSignature signTx(Transaction tx, SignAccountDTO signAccountDTO) throws NulsException {
        if (null == signAccountDTO) {
            throw new NulsException(ConverterErrorCode.NULL_PARAMETER);
        }
        List<P2PHKSignature> p2PHKSignatures;
        TransactionSignature transactionSignature;

        byte[] signBytes = tx.getTransactionSignature();
        if(null != signBytes && signBytes.length > 0){
            // 如果已存在签名则追加签名
            transactionSignature = ConverterUtil.getInstance(signBytes, TransactionSignature.class);
            p2PHKSignatures = transactionSignature.getP2PHKSignatures();
        } else {
            p2PHKSignatures = new ArrayList<>();
            transactionSignature = new TransactionSignature();
        }

        P2PHKSignature p2PHKSignature = getP2PHKSignature(tx.getHash(), signAccountDTO);
        p2PHKSignatures.add(p2PHKSignature);
        transactionSignature.setP2PHKSignatures(p2PHKSignatures);
        try {
            tx.setTransactionSignature(transactionSignature.serialize());
            return p2PHKSignature;
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
    }

    public static P2PHKSignature getP2PHKSignature(NulsHash hash, SignAccountDTO signAccountDTO) throws NulsException {
        return AccountCall.signDigest(
                signAccountDTO.getAddress(),
                signAccountDTO.getPassword(),
                hash);
    }

    /**
     * 当前虚拟银行节点签名
     *
     * @param chain
     * @param tx
     */
    public static P2PHKSignature signTxCurrentVirtualBankAgent(Chain chain, Transaction tx) throws NulsException {
        SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        if (null == signAccountDTO) {
            chain.getLogger().error("[交易签名失败]当前由虚拟银行节点签名, 但当前节点非共识节点, 不具备签名资格! type:{}, hash:{}",
                    tx.getType(), tx.getHash().toHex());
            throw new NulsException(ConverterErrorCode.SIGNER_NOT_CONSENSUS_AGENT);
        }
        if (!chain.isVirtualBankBySignAddr(signAccountDTO.getAddress())) {
            chain.getLogger().error("[交易签名失败]当前由虚拟银行节点签名, 但当前节点非虚拟银行成员节点, 不具备签名资格! type:{}, hash:{}, signAddress:{}",
                    tx.getType(), tx.getHash().toHex(), signAccountDTO.getAddress());
            throw new NulsException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
        }
        return signTx(tx, signAccountDTO);
    }

    /**
     * 如果当前节点是虚拟银行成员, 则追加当前虚拟银行成员的签名
     * @param chain
     * @param tx
     */
    public static P2PHKSignature addSignatureByDirector(Chain chain, Transaction tx) throws NulsException {
        SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        if (null != signAccountDTO && chain.isVirtualBankBySignAddr(signAccountDTO.getAddress())) {
            return signTx(tx, signAccountDTO);
        }
        return null;
    }

    /**
     * 如果当前节点是虚拟银行成员, 则获取签名的信息 不追加到交易中
     * @param chain
     * @param txHash
     */
    public static P2PHKSignature getSignatureByDirector(Chain chain, NulsHash txHash) throws NulsException {
        SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        if (null != signAccountDTO && chain.isVirtualBankBySignAddr(signAccountDTO.getAddress())) {
            return getP2PHKSignature(txHash, signAccountDTO);
        }
        return null;
    }
}

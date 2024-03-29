/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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

import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.dto.SignAccountDTO;
import network.nerve.converter.rpc.call.AccountCall;
import network.nerve.converter.rpc.call.ConsensusCall;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Loki
 * @date: 2020/5/12
 */
public class ConverterSignUtil {

    /**
     * autograph(Signature already exists,Add signature)
     *
     * @param tx
     * @param signAccountDTO
     * @throws NulsException
     */
    public static P2PHKSignature signTx(int chainId, SignAccountDTO signAccountDTO, Transaction tx) throws NulsException {
        List<P2PHKSignature> p2PHKSignatures;
        TransactionSignature transactionSignature;

        byte[] signBytes = tx.getTransactionSignature();
        if(null != signBytes && signBytes.length > 0){
            // If a signature already exists, add a signature
            transactionSignature = ConverterUtil.getInstance(signBytes, TransactionSignature.class);
            p2PHKSignatures = transactionSignature.getP2PHKSignatures();
        } else {
            p2PHKSignatures = new ArrayList<>();
            transactionSignature = new TransactionSignature();
        }

        P2PHKSignature p2PHKSignature = getP2PHKSignature(chainId, signAccountDTO, tx);
        p2PHKSignatures.add(p2PHKSignature);
        transactionSignature.setP2PHKSignatures(p2PHKSignatures);
        try {
            tx.setTransactionSignature(transactionSignature.serialize());
            return p2PHKSignature;
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.SERIALIZE_ERROR);
        }
    }

    private static P2PHKSignature getP2PHKSignature(int chainId, SignAccountDTO signAccountDTO, Transaction tx) throws NulsException {
        Map<String, Object> extend = new HashMap<>();
        extend.put("method", "cvTxSign");
        try {
            extend.put("tx", HexUtil.encode(tx.serialize()));
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.IO_ERROR);
        }
        String signatureStr = AccountCall.signature(chainId,
                signAccountDTO.getAddress(),
                signAccountDTO.getPassword(),
                tx.getHash().toHex(),
                extend);
        return ConverterUtil.getInstanceRpcStr(signatureStr, P2PHKSignature.class);
    }

    /**
     * Current virtual bank node signature
     *
     * @param chain
     * @param tx
     */
    public static P2PHKSignature signTxCurrentVirtualBankAgent(Chain chain, Transaction tx) throws NulsException {
        SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        if (null == signAccountDTO) {
            chain.getLogger().error("[Transaction signature failed]Currently signed by virtual bank node, But the current node is not a consensus node, Not qualified for signature! type:{}, hash:{}",
                    tx.getType(), tx.getHash().toHex());
            throw new NulsException(ConverterErrorCode.SIGNER_NOT_CONSENSUS_AGENT);
        }
        if (!chain.isVirtualBankBySignAddr(signAccountDTO.getAddress())) {
            chain.getLogger().error("[Transaction signature failed]Currently signed by virtual bank node, But the current node is not a virtual bank member node, Not qualified for signature! type:{}, hash:{}, signAddress:{}",
                    tx.getType(), tx.getHash().toHex(), signAccountDTO.getAddress());
            throw new NulsException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
        }
        return signTx(chain.getChainId(), signAccountDTO, tx);
    }

    /**
     * If the current node is a virtual bank member, Then add the signature of the current virtual bank member
     * @param chain
     * @param tx
     */
    public static P2PHKSignature addSignatureByDirector(Chain chain, Transaction tx) throws NulsException {
        SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        if (null != signAccountDTO && chain.isVirtualBankBySignAddr(signAccountDTO.getAddress())) {
            return signTx(chain.getChainId(), signAccountDTO, tx);
        }
        return null;
    }

    /**
     * If the current node is a virtual bank member, Obtain signature information Not added to the transaction
     * @param chain
     * @param txHash
     */
    public static P2PHKSignature getSignatureByDirector(Chain chain, Transaction tx) throws NulsException {
        SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        if (null != signAccountDTO && chain.isVirtualBankBySignAddr(signAccountDTO.getAddress())) {
            return getP2PHKSignature(chain.getChainId(), signAccountDTO, tx);
        }
        return null;
    }
}

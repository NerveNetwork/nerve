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
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.enums.ByzantineStateEnum;
import network.nerve.converter.message.BroadcastHashSignMessage;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.TransactionPO;
import network.nerve.converter.rpc.call.NetWorkCall;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.TxStorageService;
import network.nerve.converter.utils.LoggerUtil;
import network.nerve.converter.utils.VirtualBankUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Message transaction processing class
 *
 * @author: Loki
 * @date: 2020/4/14
 */
@Component
public class TransactionMsgProcessor {

    @Autowired
    private static TxStorageService txStorageService;
    @Autowired
    private static ConverterCoreApi converterCoreApi;

    /**
     * Process transactions broadcasted on this chain
     *
     * @param chain   This chain information
     * @param hash    Transaction cacheHASH
     * @param nodeId  Sending nodeID
     * @param hashHex transactionHashcharacter string（Used for log printing）
     */
    public static void handleSignMessageByzantine(Chain chain, NulsHash hash, String nodeId, BroadcastHashSignMessage messageBody, String hashHex) {
        try {
            // v15Special handling after upgrading the wavefield protocolhashHistorical legacy issues,polygonThe issue of inconsistent transaction height and time caused by block rollback
            if (converterCoreApi.isSupportProtocol15TrxCrossChain() && "e7650127c55c7fa90e8cfded861b9aba0a71e025c318f0e31d53721d864d1e26".equalsIgnoreCase(hash.toHex())) {
                LoggerUtil.LOG.warn("Filter transactionshash: {}", hash.toHex());
                txStorageService.delete(chain, hash);
                return;
            }
            TransactionPO txPO = txStorageService.get(chain, hash);
            //If the transaction has already been successful in Byzantium, You don't need to broadcast the signature of the transaction anymore
            if (txPO.getStatus() != ByzantineStateEnum.UNTREATED.getStatus() || messageBody.getP2PHKSignature() == null) {
                LoggerUtil.LOG.info("The transaction has been processed at this node,Hash:{}\n\n", hashHex);
                return;
            }
            P2PHKSignature p2PHKSignature = messageBody.getP2PHKSignature();
            Transaction tx = txPO.getTx();
            String signHex = HexUtil.encode(p2PHKSignature.getBytes());
            // Determine if the signature has been received before(Does the signature already exist in the transaction)
            TransactionSignature signature = new TransactionSignature();
            if (tx.getTransactionSignature() != null) {
                signature.parse(tx.getTransactionSignature(), 0);
                for (P2PHKSignature sign : signature.getP2PHKSignatures()) {
                    if (Arrays.equals(messageBody.getP2PHKSignature().getBytes(), sign.serialize())) {
                        LoggerUtil.LOG.debug("This node has already received the signature for this transaction,Hash:{}, Signature address:{}\n\n", hashHex,
                                AddressTool.getStringAddressByBytes(AddressTool.getAddress(p2PHKSignature.getPublicKey(), chain.getChainId())));
                        return;
                    }
                }
            } else {
                List<P2PHKSignature> p2PHKSignatureList = new ArrayList<>();
                signature.setP2PHKSignatures(p2PHKSignatureList);
            }

            // Determine whether the signature sender is qualified to sign(Is it the current virtual bank member)
            String signAddress = AddressTool.getStringAddressByBytes(AddressTool.getAddress(p2PHKSignature.getPublicKey(), chain.getChainId()));
            if (!chain.isVirtualBankBySignAddr(signAddress)) {
                //The signature address is not a virtual bank node
                chain.getLogger().error(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT.getMsg());
                return;
            }
            //Verify the correctness of the signature itself
            if (!ECKey.verify(tx.getHash().getBytes(), p2PHKSignature.getSignData().getSignBytes(), p2PHKSignature.getPublicKey())) {
                LoggerUtil.LOG.error("[Illegal signature] Signature verification failed!. hash:{}, Signature address:{}, autographhex:{}\n\n",
                        tx.getHash().toHex(),
                        AddressTool.getStringAddressByBytes(AddressTool.getAddress(p2PHKSignature.getPublicKey(), chain.getChainId())),
                        signHex);
                return;
            }
            // Add the current signature to the signature list
            signature.getP2PHKSignatures().add(p2PHKSignature);
            signByzantine(chain, txPO, messageBody, signature, nodeId);
        } catch (NulsException e) {
            chain.getLogger().error(e);
        } catch (IOException io) {
            chain.getLogger().error(io);
        }
    }


    /**
     * Transaction Signature Byzantine Processing
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
            // Indicates that the transaction has been successfully Byzantine verified
            txPO.setStatus(ByzantineStateEnum.PASSED.getStatus());
            /**
             * Signature verification passed
             */
            try {
                TransactionCall.newTx(chain, tx);
            } catch (NulsException e) {
                if("tx_0033".equals(e.getErrorCode().getCode())){
                    try {
                        chain.getLogger().error("new tx address error, tx format: {}", tx.format());
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                    return;
                }
                if(!"tx_0013".equals(e.getErrorCode().getCode())){
                    throw e;
                }
            }
        }
        txStorageService.save(chain, txPO);
        // Broadcast the received signature
        NetWorkCall.broadcast(chain, messageBody, excludeNodes, ConverterCmdConstant.NEW_HASH_SIGN_MESSAGE);
        LoggerUtil.LOG.info("Broadcast signature messages to other nodes, Hash:{}, ", tx.getHash().toHex());
    }

    /**
     * Transaction signature Byzantine verification
     *
     * @param chain                  This chain information
     * @param tx                     transaction
     * @param signature              Signature List
     * @return Byzantine verification passed
     */
    public static boolean signByzantineInChain(Chain chain, Transaction tx, TransactionSignature signature) throws IOException {
        //Transaction Signature Byzantine
        int byzantineCount = VirtualBankUtil.getByzantineCount(chain);
        int signCount = signature.getSignersCount();
        if (signCount >= byzantineCount) {
            // Transaction Signature Byzantine, Obtain signature addresses for all virtual bank members
            Set<String> directorSignAddressSet = chain.getMapVirtualBank().keySet();
            // Count the number of valid signatures(Exclude signatures and duplicate signatures that are not members of the current virtual bank)
            signCount = VirtualBankUtil.getSignCountWithoutMisMatchSigns(chain, signature, directorSignAddressSet);
            if (signCount >= byzantineCount) {
                tx.setTransactionSignature(signature.serialize());
                LoggerUtil.LOG.info("Byzantine signature verification passed, hash:{}, Current number of virtual bank members:{}, Current number of signatures:{}, Number of signatures required:{}", tx.getHash().toHex(),chain.getMapVirtualBank().size(), signCount, byzantineCount);
                return true;
            } else {
                tx.setTransactionSignature(signature.serialize());
            }
        } else {
            tx.setTransactionSignature(signature.serialize());
        }
        List<P2PHKSignature> p2PHKSignatures = signature.getP2PHKSignatures();
        StringBuilder sb = new StringBuilder();
        for (P2PHKSignature s : p2PHKSignatures) {
            sb.append(String.format("pppppub: %s, addr: %s, signData: %s", HexUtil.encode(s.getPublicKey()), AddressTool.getAddressString(s.getPublicKey(), chain.getChainId()), HexUtil.encode(s.getSignData().getSignBytes()))).append("\n");
        }
        LoggerUtil.LOG.info("Insufficient Byzantine signatures, hash:{}, Number of signatures required:{}, Current number of signatures:{}, detail: {}", tx.getHash().toHex(), byzantineCount, signCount, sb.toString());
        return false;
    }

}

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
import io.nuls.base.signture.SignatureUtil;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.VirtualBankDirector;

import java.util.Set;

/**
 * @author: Loki
 * @date: 2020-03-05
 */
public class ConverterSignValidUtil {

    /**
     * Verify transaction signatures signed by virtual bank nodes
     * @param chain
     * @param tx
     * @throws NulsException
     */
    public static void validateVirtualBankSign(Chain chain, Transaction tx) throws NulsException {
        // Signature Qualification Obtain signature address from transaction signature
        Set<String> addressSet = SignatureUtil.getAddressFromTX(tx, chain.getChainId());
        if (addressSet == null) {
            throw new NulsException(ConverterErrorCode.SIGNATURE_ERROR);
        }
        boolean hasVirtualBankSigner = false;
        for (String address : addressSet) {
            if (chain.isVirtualBankBySignAddr(address)) {
                //The signature address belongs to the virtual bank node
                hasVirtualBankSigner = true;
                break;
            }
        }
        if (!hasVirtualBankSigner) {
            throw new NulsException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
        }
        // Verify the correctness of the signature itself
        boolean rs = SignatureUtil.validateTransactionSignture(chain.getChainId(), tx);
        if (!rs) {
            throw new NulsException(ConverterErrorCode.SIGNATURE_ERROR);
        }
    }

    /**
     * Verify Byzantine signatures
     *
     * @param chain
     * @param tx
     * @throws NulsException
     */
    public static void validateByzantineSign(Chain chain, Transaction tx) throws NulsException {
        // Obtain signature address from transaction signature(Deduplication)
        Set<String> addressSet = SignatureUtil.getAddressFromTX(tx, chain.getChainId());
        if (addressSet == null) {
            throw new NulsException(ConverterErrorCode.SIGNATURE_ERROR);
        }

        // Statistical signature addresses, Is the current number of virtual bank node addresses(Number of qualified signatures)
        int signCount = 0;
        for (String address : addressSet) {
            if (chain.isVirtualBankBySignAddr(address)) {
                //The signature address belongs to the virtual bank node
                signCount++;
            }
        }

        //The number of Byzantine signatures required for virtual bank nodes
        int byzantineCount = VirtualBankUtil.getByzantineCount(chain);
        // Check if the number of signatures is sufficient
        if (signCount < byzantineCount) {
            throw new NulsException(ConverterErrorCode.SIGNATURE_BYZANTINE_ERROR);
        }
        // Verify the correctness of all signatures in the transaction signature list
        boolean rs = SignatureUtil.validateTransactionSignture(chain.getChainId(), tx);
        if (!rs) {
            throw new NulsException(ConverterErrorCode.SIGNATURE_ERROR);
        }
    }


    /**
     * Verify by Seed(Virtual banking)Node Signature,And correctness
     * @param chain
     * @param tx
     * @throws NulsException
     */
    public static void validateSeedNodeSign(Chain chain, Transaction tx) throws NulsException {
        // Signature Qualification Obtain signature address from transaction signature
        Set<String> addressSet = SignatureUtil.getAddressFromTX(tx, chain.getChainId());
        if (addressSet == null) {
            throw new NulsException(ConverterErrorCode.SIGNATURE_ERROR);
        }
        boolean hasSeedSigner = false;
        for (String address : addressSet) {
            VirtualBankDirector director = chain.getDirectorByAgent(address);
            if(null != director && director.getSeedNode()){
                hasSeedSigner = true;
                break;
            }
        }
        if (!hasSeedSigner) {
            throw new NulsException(ConverterErrorCode.SIGNER_NOT_SEED);
        }
        // Verify the correctness of the signature itself
        boolean rs = SignatureUtil.validateTransactionSignture(chain.getChainId(), tx);
        if (!rs) {
            throw new NulsException(ConverterErrorCode.SIGNATURE_ERROR);
        }
    }
}

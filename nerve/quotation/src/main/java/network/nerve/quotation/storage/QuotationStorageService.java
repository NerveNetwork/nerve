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

package network.nerve.quotation.storage;

import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.po.NodeQuotationWrapperPO;

/**
 * @author: Loki
 * @date: 2019/11/28
 */
public interface QuotationStorageService {

    /**
     * Store quotation transaction business data for each node
     *
     * @param chain
     * @param key     structureyyyyMMdd-token, example 20191201-NULS
     * @param wrapper Store the sametoken All quotations in days
     * @return
     */
    boolean saveNodeQuotation(Chain chain, String key, NodeQuotationWrapperPO wrapper);

    /**
     * Obtain quotation transaction business data for each node
     * @param chain
     * @param key structureyyyyMMdd-token, example 20191201-NULS
     * @return
     */
    NodeQuotationWrapperPO getNodeQuotationsBykey(Chain chain, String key);

    /**
     * Save the final quotation after statistical analysis
     * @param chain
     * @param key structureyyyyMMdd-token, example 20191201-NULS
     * @param finalQuotationPO
     * @return
     */
//    boolean saveFinalQuotation(Chain chain, String key, FinalQuotationPO finalQuotationPO);

    /**
     * Get correspondingtokenLast quotation keyInclude date
     * @param chain
     * @param key structureyyyyMMdd-token, example 20191201-NULS
     * @return
     */
//    FinalQuotationPO getFinalQuotation(Chain chain, String key);


    /**
     * Store correspondingkeyLast quotation keyExcluding dates
     * @param chain
     * @param key Excluding dates example:NULSUSDT
     * @param finalQuotationPO
     * @return
     */
//    boolean saveFinalLastQuotation(Chain chain, String key, FinalQuotationPO finalQuotationPO);

    /**
     * Get correspondingkeyLast quotation keyExcluding dates
     * @param chain
     * @param key Excluding dates example:NULSUSDT
     * @return
     */
//    FinalQuotationPO getFinalLastTimeQuotation(Chain chain, String key);
}

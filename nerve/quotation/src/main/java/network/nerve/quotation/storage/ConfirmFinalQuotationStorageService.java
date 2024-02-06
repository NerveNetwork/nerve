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
import network.nerve.quotation.model.po.ConfirmFinalQuotationPO;

/**
 * @author: Loki
 * @date: 2020-02-19
 */
public interface ConfirmFinalQuotationStorageService {

    /**
     * Final quotation confirmed by blockchain storage
     * @param chain
     * @param key structureyyyyMMdd-token, example 20191201-NULS
     * @param cfrFinalQuotationPO
     * @return
     */
    boolean saveCfrFinalQuotation(Chain chain, String key, ConfirmFinalQuotationPO cfrFinalQuotationPO);

    /**
     * Obtain the corresponding blockchain confirmationtokenLast quotation keyInclude date
     * @param chain
     * @param key structureyyyyMMdd-token, example 20191201-NULS
     * @return
     */
    ConfirmFinalQuotationPO getCfrFinalQuotation(Chain chain, String key);


    boolean deleteCfrFinalQuotationByKey(Chain chain, String key);

    /**
     * Store corresponding blockchain confirmationskeyLast quotation keyExcluding dates
     * @param chain
     * @param key Excluding dates example:NULSUSDT
     * @param cfrFinalQuotationPO
     * @return
     */
    boolean saveCfrFinalLastQuotation(Chain chain, String key, ConfirmFinalQuotationPO cfrFinalQuotationPO);

    /**
     * Obtain the corresponding blockchain confirmationkeyLast quotation keyExcluding dates
     * @param chain
     * @param key Excluding dates example:NULSUSDT
     * @return
     */
    ConfirmFinalQuotationPO getCfrFinalLastTimeQuotation(Chain chain, String key);


    boolean deleteCfrFinalLastTimeQuotationByKey(Chain chain, String key);

}

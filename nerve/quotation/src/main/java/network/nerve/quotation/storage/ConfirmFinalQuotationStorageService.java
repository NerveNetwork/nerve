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

package network.nerve.quotation.storage;

import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.po.ConfirmFinalQuotationPO;

/**
 * @author: Loki
 * @date: 2020-02-19
 */
public interface ConfirmFinalQuotationStorageService {

    /**
     * 存区块链确认的最终报价
     * @param chain
     * @param key 结构yyyyMMdd-token, 例 20191201-NULS
     * @param cfrFinalQuotationPO
     * @return
     */
    boolean saveCfrFinalQuotation(Chain chain, String key, ConfirmFinalQuotationPO cfrFinalQuotationPO);

    /**
     * 获取区块链确认的对应token最后一次报价 key包含日期
     * @param chain
     * @param key 结构yyyyMMdd-token, 例 20191201-NULS
     * @return
     */
    ConfirmFinalQuotationPO getCfrFinalQuotation(Chain chain, String key);


    boolean deleteCfrFinalQuotationByKey(Chain chain, String key);

    /**
     * 储存区块链确认的对应key最后一次报价 key不含日期
     * @param chain
     * @param key 不含日期 例:NULSUSDT
     * @param cfrFinalQuotationPO
     * @return
     */
    boolean saveCfrFinalLastQuotation(Chain chain, String key, ConfirmFinalQuotationPO cfrFinalQuotationPO);

    /**
     * 获取区块链确认的对应key最后一次报价 key不含日期
     * @param chain
     * @param key 不含日期 例:NULSUSDT
     * @return
     */
    ConfirmFinalQuotationPO getCfrFinalLastTimeQuotation(Chain chain, String key);


    boolean deleteCfrFinalLastTimeQuotationByKey(Chain chain, String key);

}

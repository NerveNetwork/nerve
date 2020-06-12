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

package network.nerve.quotation.service;

import io.nuls.base.data.Transaction;
import io.nuls.core.exception.NulsException;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.dto.QuoteDTO;
import network.nerve.quotation.model.txdata.Prices;

import java.util.Map;

/**
 * @author: Loki
 * @date: 2019/11/25
 */
public interface QuotationService {

    /**
     * 报价, 将计算好的价格, 组装到交易广播到网络
     */
    Transaction quote(Chain chain, QuoteDTO quoteDTO) throws NulsException;

    /**
     * 组装报价交易txdata
     * @param address
     * @param pricesMap
     * @return
     * @throws NulsException
     */
    byte[] assemblyQuotationTxData(String address, Map<String, Double> pricesMap) throws NulsException;

    /**
     * 组装报价交易
     */
    Transaction createQuotationTransaction(byte[] txData, String address, String password) throws NulsException;

    /**
     * 组装最终报价
     * @param prices
     * @return
     * @throws NulsException
     */
    Transaction createFinalQuotationTransaction(Prices prices) throws NulsException;

    /**
     * 组装最终报价
     * @param pricesMap
     * @return
     * @throws NulsException
     */
    Transaction createFinalQuotationTransaction(Map<String, Double> pricesMap) throws NulsException;


}

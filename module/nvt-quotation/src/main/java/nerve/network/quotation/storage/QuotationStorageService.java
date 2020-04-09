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

package nerve.network.quotation.storage;

import nerve.network.quotation.model.bo.Chain;
import nerve.network.quotation.model.po.FinalQuotationPO;
import nerve.network.quotation.model.po.NodeQuotationWrapperPO;

/**
 * @author: Chino
 * @date: 2020/03/28
 */
public interface QuotationStorageService {

    /**
     * 存各节点的报价交易业务数据
     *
     * @param chain
     * @param key     结构yyyyMMdd-token, 例 20191201-NULS
     * @param wrapper 存放相同token 以天为单位的所有报价
     * @return
     */
    boolean saveNodeQuotation(Chain chain, String key, NodeQuotationWrapperPO wrapper);

    /**
     * 获取各节点的报价交易业务数据
     * @param chain
     * @param key 结构yyyyMMdd-token, 例 20191201-NULS
     * @return
     */
    NodeQuotationWrapperPO getNodeQuotationsBykey(Chain chain, String key);

    /**
     * 存经过统计后的最终报价
     * @param chain
     * @param key 结构yyyyMMdd-token, 例 20191201-NULS
     * @param finalQuotationPO
     * @return
     */
    boolean saveFinalQuotation(Chain chain, String key, FinalQuotationPO finalQuotationPO);

    /**
     * 获取对应token最后一次报价 key包含日期
     * @param chain
     * @param key 结构yyyyMMdd-token, 例 20191201-NULS
     * @return
     */
    FinalQuotationPO getFinalQuotation(Chain chain, String key);


    /**
     * 储存对应key最后一次报价 key不含日期
     * @param chain
     * @param key 不含日期 例:NULSUSDT
     * @param finalQuotationPO
     * @return
     */
    boolean saveFinalLastQuotation(Chain chain, String key, FinalQuotationPO finalQuotationPO);

    /**
     * 获取对应key最后一次报价 key不含日期
     * @param chain
     * @param key 不含日期 例:NULSUSDT
     * @return
     */
    FinalQuotationPO getFinalLastTimeQuotation(Chain chain, String key);
}

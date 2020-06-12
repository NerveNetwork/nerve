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

package network.nerve.quotation.task;

import io.nuls.base.data.Transaction;
import io.nuls.core.core.ioc.SpringLiteContext;
import network.nerve.quotation.constant.QuotationContext;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.bo.QuotationActuator;
import network.nerve.quotation.model.txdata.Prices;
import network.nerve.quotation.processor.Calculator;
import network.nerve.quotation.rpc.call.QuotationCall;
import network.nerve.quotation.service.QuotationService;
import network.nerve.quotation.util.CommonUtil;
import network.nerve.quotation.util.TimeUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 从交易中提取报价, 统计最终价格
 * Final Quotation
 *
 * @author: Loki
 * @date: 2019/12/4
 */
public class CalculatorTask implements Runnable {

    private Chain chain;

    public CalculatorTask(Chain chain) {
        this.chain = chain;
    }


    private QuotationService quotationService = SpringLiteContext.getBean(QuotationService.class);
    @Override
    public void run() {
        while (true) {
            try {

                //是否在允许的执行时间范围
                if (!TimeUtil.isNowDateTimeInRange(QuotationContext.quoteEndH, QuotationContext.quoteEndM)) {
                    break;
                }
                if(!CommonUtil.isCurrentConsensusNode(QuotationCall.getPackerInfo(chain))){
                    break;
                }
                //清空当天的节点报价缓存
                QuotationContext.NODE_QUOTED_TX_TOKENS_TEMP.clear();
                QuotationContext.NODE_QUOTED_TX_TOKENS_CONFIRMED.clear();
                List<QuotationActuator> quteList = chain.getQuote();
                Map<String, Double> pricesMap = new HashMap<>();
                for (QuotationActuator qa : quteList) {
                    String anchorToken = qa.getAnchorToken();
                    if(QuotationContext.INTRADAY_NEED_NOT_QUOTE_TOKENS.contains(anchorToken)){
                        //如果已确认的报价交易缓存中已存在该key, 则不再执行.
                        continue;
                    }
                    Calculator calculator = getCalculator(qa.getCalculator());
                    Double price = calculator.calcFinal(chain, anchorToken, TimeUtil.nowUTCDate());
                    if(null != price && price > 0) {
                        pricesMap.put(anchorToken, price);
                    }
                }
                //开始组装交易发送到交易模块（交易模块有特殊的接口 不广播）
                if(!pricesMap.isEmpty()){
                    Transaction tx = quotationService.createFinalQuotationTransaction(pricesMap);
                    chain.getLogger().info("{}", tx.format(Prices.class));
                    //发送到交易模块
                    QuotationCall.newFinalQuotationTx(chain, tx);
                    chain.getLogger().info("发布最终报价交易 hash:{}", tx.getHash().toHex());
                }
                break;
            } catch (Throwable e) {
                chain.getLogger().error("finalQuotationProcessTask error", e);
                break;
            }
        }
    }

    public Calculator getCalculator(String clazz) throws Exception {
        Class<?> clasz = Class.forName(clazz);
        return (Calculator) SpringLiteContext.getBean(clasz);
    }
}

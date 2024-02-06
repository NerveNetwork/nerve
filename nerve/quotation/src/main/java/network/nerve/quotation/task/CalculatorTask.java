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

package network.nerve.quotation.task;

import io.nuls.base.data.Transaction;
import io.nuls.core.core.ioc.SpringLiteContext;
import network.nerve.quotation.constant.QuotationContext;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.bo.QuotationActuator;
import network.nerve.quotation.model.bo.QuotationContractCfg;
import network.nerve.quotation.model.txdata.Prices;
import network.nerve.quotation.processor.Calculator;
import network.nerve.quotation.rpc.call.QuotationCall;
import network.nerve.quotation.service.QuotationService;
import network.nerve.quotation.storage.QuotationIntradayStorageService;
import network.nerve.quotation.util.CommonUtil;
import network.nerve.quotation.util.TimeUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracting quotes from transactions, Statistical final price
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
    private QuotationIntradayStorageService quotationIntradayStorageService = SpringLiteContext.getBean(QuotationIntradayStorageService.class);

    @Override
    public void run() {
        try {
            //Is it within the allowed execution time range
            if (!TimeUtil.isNowDateTimeInRange(QuotationContext.quoteEndH, QuotationContext.quoteEndM)) {
                return;
            }
            if (!CommonUtil.isCurrentConsensusNode(QuotationCall.getPackerInfo(chain))) {
                return;
            }
            //Clear the node quotation cache for the day
            QuotationContext.NODE_QUOTED_TX_TOKENS_TEMP.clear();
            QuotationContext.NODE_QUOTED_TX_TOKENS_CONFIRMED.clear();
            quotationIntradayStorageService.removeAll(chain);

            List<QuotationActuator> quteList = chain.getQuote();
            Map<String, Double> pricesMap = new HashMap<>();
            for (QuotationActuator qa : quteList) {
                String anchorToken = qa.getAnchorToken();
                if (QuotationContext.INTRADAY_NEED_NOT_QUOTE_TOKENS.contains(anchorToken)) {
                    //If the confirmed quote transaction already exists in the cachekey, Then it will no longer be executed.
                    continue;
                }
                Calculator calculator = getCalculator(qa.getCalculator());
                Double price = calculator.calcFinal(chain, anchorToken, TimeUtil.nowUTCDate());
                if (null != price && price > 0) {
                    pricesMap.put(anchorToken, price);
                }
            }
            // swapContract quotationkey
            for (QuotationContractCfg quContractCfg : chain.getContractQuote()) {
                String anchorToken = quContractCfg.getAnchorToken();
                if (QuotationContext.INTRADAY_NEED_NOT_QUOTE_TOKENS.contains(anchorToken)) {
                    //If the confirmed quote transaction already exists in the cachekey, Then it will no longer be executed.
                    continue;
                }
                Calculator calculator = getCalculator(quContractCfg.getCalculator());
                Double price = calculator.calcFinal(chain, anchorToken, TimeUtil.nowUTCDate());
                if (null != price && price > 0) {
                    pricesMap.put(anchorToken, price);
                }
            }
            //Start assembling transactions and sending them to the transaction module（The transaction module has a special interface Not broadcasting）
            if (!pricesMap.isEmpty()) {
                Transaction tx = quotationService.createFinalQuotationTransaction(pricesMap);
                chain.getLogger().info("{}", tx.format(Prices.class));
                //Send to transaction module
                QuotationCall.newTx(chain, tx);
                chain.getLogger().info("Publish final quotation transaction hash:{}", tx.getHash().toHex());
            }
        } catch (Throwable e) {
            chain.getLogger().error("finalQuotationProcessTask error", e);
        }
    }

    public Calculator getCalculator(String clazz) throws Exception {
        Class<?> clasz = Class.forName(clazz);
        return (Calculator) SpringLiteContext.getBean(clasz);
    }
}

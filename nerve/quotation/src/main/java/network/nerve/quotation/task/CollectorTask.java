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
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.quotation.constant.QuotationContext;
import network.nerve.quotation.constant.QuotationErrorCode;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.bo.QuotationActuator;
import network.nerve.quotation.model.dto.QuoteDTO;
import network.nerve.quotation.model.txdata.Quotation;
import network.nerve.quotation.processor.Collector;
import network.nerve.quotation.rpc.call.QuotationCall;
import network.nerve.quotation.service.QuotationService;
import network.nerve.quotation.storage.QuotationIntradayStorageService;
import network.nerve.quotation.util.CommonUtil;
import network.nerve.quotation.util.TimeUtil;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 获取第三方价格 发交易
 * Price Collect Process
 *
 * @author: Loki
 * @date: 2019/12/4
 */
public class CollectorTask implements Runnable {

    private Chain chain;

    private QuotationService quotationService = SpringLiteContext.getBean(QuotationService.class);
    private QuotationIntradayStorageService quotationIntradayStorageService = SpringLiteContext.getBean(QuotationIntradayStorageService.class);

    public CollectorTask(Chain chain) {
        this.chain = chain;
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (!TimeUtil.isNowDateTimeInRange(QuotationContext.quoteStartH, QuotationContext.quoteStartM, QuotationContext.quoteEndH, QuotationContext.quoteEndM)) {
                    break;
                }
                if (!CommonUtil.isCurrentConsensusNode(QuotationCall.getPackerInfo(chain))) {
                    break;
                }
                QuotationContext.INTRADAY_NEED_NOT_QUOTE_TOKENS.clear();
                List<QuotationActuator> quteList = chain.getQuote();
                Map<String, Double> pricesMap = new HashMap<>();
                for (QuotationActuator qa : quteList) {
                    String anchorToken = qa.getAnchorToken();
                    if(QuotationContext.NODE_QUOTED_TX_TOKENS_CONFIRMED.contains(anchorToken)){
                        //当天已存在该key的已确认报价交易
                        continue;
                    }
                    if (QuotationContext.NODE_QUOTED_TX_TOKENS_TEMP.containsKey(anchorToken)) {
                        //如果未确认的报价交易缓存中已存在该key, 则需要从交易模块获取该笔交易是否已确认
                        String txHash = QuotationContext.NODE_QUOTED_TX_TOKENS_TEMP.get(anchorToken);
                        boolean confirmed = QuotationCall.isConfirmed(chain, txHash);
                        chain.getLogger().info("[CollectorTask] 获取交易是否已确认：{}, hash:{}", confirmed, txHash);
                        if (confirmed) {
                            chain.getLogger().info("[CollectorTask] {}已报价", anchorToken);
                            //加入已报价确认
                            QuotationContext.NODE_QUOTED_TX_TOKENS_CONFIRMED.add(anchorToken);
                            continue;
                        } else {
                            quotationIntradayStorageService.delete(chain, anchorToken);
                        }
                    }
                    Collector collector = getCollector(qa.getCollector());
                    BigDecimal price = collector.enquiry(chain, anchorToken);
                    if (null == price || price.doubleValue() == 0) {
                        chain.getLogger().error("[CollectorTask] [{}]没有获取到第三方平均报价", anchorToken);
                        continue;
                    }
                    chain.getLogger().info("[CollectorTask] [{}]第三方平均报价为：{} \n\n", anchorToken, price.doubleValue());
                    pricesMap.put(anchorToken, price.doubleValue());
                }
                Transaction tx = null;
                if (pricesMap.isEmpty() || null == (tx = createAndSendTx(chain, pricesMap))) {
                    break;
                }
                chain.getLogger().info("[Quotation] 节点执行报价交易 txHash: {}", tx.getHash().toHex());
                chain.getLogger().info("{}", tx.format(Quotation.class));
                //记录成功发送报价交易的token (如果由自己节点交易确认的时候来记录,容易出现重复报价)
                String txHash = tx.getHash().toHex();
                pricesMap.forEach((key, value) -> {
                    QuotationContext.NODE_QUOTED_TX_TOKENS_TEMP.put(key, txHash);
                    quotationIntradayStorageService.save(chain, key);
                });

                break;
            } catch (Throwable e) {
                chain.getLogger().error("CollectorTask error", e);
                break;
            }
        }
    }

    /**
     * 组装交易, 并广播到网络中
     *
     * @param chain
     * @param pricesMap
     * @return
     * @throws NulsException
     */
    private Transaction createAndSendTx(Chain chain, Map<String, Double> pricesMap) throws NulsException {
        QuoteDTO quoteDTO = new QuoteDTO();
        quoteDTO.setChainId(chain.getChainId());
        quoteDTO.setPricesMap(pricesMap);
        Map<String, String> map = QuotationCall.getPackerInfo(chain);
        String address = map.get("address");
        if (StringUtils.isBlank(address)) {
            throw new NulsException(QuotationErrorCode.PACKER_ADDRESS_NOT_FOUND);
        }
        String password = map.get("password");
        if (StringUtils.isBlank(password)) {
            throw new NulsException(QuotationErrorCode.PACKER_PASSWORD_NOT_FOUND);
        }
        quoteDTO.setAddress(map.get("address"));
        quoteDTO.setPassword(map.get("password"));
        return quotationService.quote(chain, quoteDTO);
    }


    public Collector getCollector(String clazz) throws Exception {
        Class<?> clasz = Class.forName(clazz);
        return (Collector) SpringLiteContext.getBean(clasz);
    }
}

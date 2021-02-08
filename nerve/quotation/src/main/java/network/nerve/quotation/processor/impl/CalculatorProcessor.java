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

package network.nerve.quotation.processor.impl;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.quotation.constant.QuotationConstant;
import network.nerve.quotation.constant.QuotationContext;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.po.ConfirmFinalQuotationPO;
import network.nerve.quotation.model.po.NodeQuotationPO;
import network.nerve.quotation.model.po.NodeQuotationWrapperPO;
import network.nerve.quotation.processor.Calculator;
import network.nerve.quotation.storage.ConfirmFinalQuotationStorageService;
import network.nerve.quotation.storage.QuotationStorageService;
import network.nerve.quotation.util.CommonUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static network.nerve.quotation.constant.QuotationContext.INTRADAY_NEED_NOT_QUOTE_TOKENS;

/**
 * @author: Loki
 * @date: 2019/12/5
 */
@Component
public class CalculatorProcessor implements Calculator {

    @Autowired
    private QuotationStorageService quotationStorageService;

    @Autowired
    private ConfirmFinalQuotationStorageService cfrFinalQuotationStorageService;

    @Override
    public Double calcFinal(Chain chain, String token, String date) {
        try {
            chain.getLogger().info("[CalculatorProcessor] 开始统计({})网络最终报价", token);
            NulsLogger log = chain.getLogger();
            //获取各节点报价交易数据
            String dbKey = CommonUtil.assembleKey(date, token);
            NodeQuotationWrapperPO nodeQuotationWrapper = quotationStorageService.getNodeQuotationsBykey(chain, dbKey);
            if (null == nodeQuotationWrapper || null == nodeQuotationWrapper.getList() || nodeQuotationWrapper.getList().isEmpty()) {
                log.warn("There is no node quote yet, key:{}, 无任何节点报价 ", dbKey);
                return getLastFinalConfiremdPrice(chain, token);
            }

            List<NodeQuotationPO> list = nodeQuotationWrapper.getList();
            log.info("{}当前报价数:{}", dbKey, list.size());
            distinctNodeTx(chain, list);
            list = removeMinMax(chain, list);
            if (list.size() < QuotationContext.effectiveQuotation) {
                log.warn("The current quoted quantity is less than the minimum. current-effective:{}", list.size());
                //获取前一次报价, 作为当天的最终报价
                return getLastFinalConfiremdPrice(chain, token);
            }
            double finalPrice = avgCalc(list);
            log.info("{}当前节基于{}个节点的报价，最终报价计算结果:{}", dbKey, list.size(), (new BigDecimal(Double.toString(finalPrice))).toPlainString());
            return finalPrice;
        } catch (Throwable e) {
            chain.getLogger().error("统计最终报价异常.. {}, {}", token, date);
            chain.getLogger().error(e);
            return null;
        }
    }

    /**
     * 获取最近一次已确认报价
     * @param chain
     * @param token
     * @return
     */
    public Double getLastFinalConfiremdPrice(Chain chain, String token){
        ConfirmFinalQuotationPO cfrFinalQuotationPO = cfrFinalQuotationStorageService.getCfrFinalLastTimeQuotation(chain, token);
        if(null == cfrFinalQuotationPO){
            //记录该token当天无需再次计算
            INTRADAY_NEED_NOT_QUOTE_TOKENS.add(token);
            chain.getLogger().error("暂无该任何最终报价数据(包括历史报价), token:{}", token);
            return null;
        }
        chain.getLogger().warn("存(使用前一天报价): {} - {}", token, cfrFinalQuotationPO.getPrice());
        return cfrFinalQuotationPO.getPrice();
    }


    /**
     * 去除 有多笔报价交易的节点的所有报价数据
     *
     * @param list
     */
    private void distinctNodeTx(Chain chain, List<NodeQuotationPO> list) {
        Set<String> addressSet = new HashSet<>();
        Set<String> distinctAddress = new HashSet<>();
        list.forEach(v -> {
            if (!addressSet.add(v.getAddress())) {
                distinctAddress.add(v.getAddress());
            }
        });
        Iterator<NodeQuotationPO> it = list.iterator();
        while (it.hasNext()) {
            NodeQuotationPO nq = it.next();
            for (String address : distinctAddress) {
                if (address.equals(nq.getAddress())) {
                    chain.getLogger().debug("CalculatorProcessor, 节点重复报价 address:{}, key:{}", address, nq.getToken());
                    it.remove();
                }
            }
        }
    }

    /**
     * 去掉[两个]最大值和两个最小值的数据
     */
    private List<NodeQuotationPO> removeMinMax(Chain chain, List<NodeQuotationPO> list) {
        if (QuotationContext.removeMaxMinCount <= 0){
            return list;
        }
        if (list.size() <= QuotationContext.removeMaxMinCount * 2) {
            list.clear();
            return list;
        }
        //排序
        list.sort(new Comparator<NodeQuotationPO>() {
            @Override
            public int compare(NodeQuotationPO o1, NodeQuotationPO o2) {
                if (o1.getPrice() < o2.getPrice()) {
                    return -1;
                } else if (o1.getPrice() > o2.getPrice()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        List<NodeQuotationPO> rsList = new ArrayList<>();
        //去掉头尾各两个元素
        for (int i = QuotationContext.removeMaxMinCount; i < list.size() - QuotationContext.removeMaxMinCount; i++) {
            rsList.add(list.get(i));
        }
        return rsList;
    }

    /**
     * 计算平均数
     *
     * @param list
     * @return
     */
    private double avgCalc(List<NodeQuotationPO> list) {
        List<BigDecimal> prices = new ArrayList<>();
        list.forEach(v -> {
            BigDecimal price = new BigDecimal(String.valueOf(v.getPrice()));
            prices.add(price);
        });
        BigDecimal total = new BigDecimal("0");
        for (BigDecimal price : prices) {
            total = total.add(price);
        }
        BigDecimal count = new BigDecimal(String.valueOf(list.size()));
        BigDecimal avg = total.divide(count, QuotationConstant.SCALE, RoundingMode.HALF_DOWN);
        return avg.doubleValue();
    }

}

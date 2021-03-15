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

import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import network.nerve.quotation.constant.QuotationConstant;
import network.nerve.quotation.constant.QuotationContext;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.bo.QuerierCfg;
import network.nerve.quotation.processor.Collector;
import network.nerve.quotation.rpc.querier.Querier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author: Loki
 * @date: 2019/12/5
 */
@Component
public class CollectorProcessor implements Collector {

    private ExecutorService basicQuerierExecutor = ThreadUtils.createThreadPool(Runtime.getRuntime().availableProcessors(),
            50, new NulsThreadFactory(QuotationConstant.BASIC_QUERIER_THREAD));

    @Override
    public BigDecimal enquiry(Chain chain, String anchorToken) {
        /**
         * 根据token从各个查询器采集多个(交易所等)价格
         * 根据不同第三方机构,对价格计算加权平均值
         */
        long start;
        chain.getLogger().info("开始获({})第三方报价, 当前时间戳:{}", anchorToken, start = System.currentTimeMillis());
        class EnquiryResult {
            String name;
            BigDecimal price;
            String weight;

            public EnquiryResult(String name, BigDecimal price, String weight) {
                this.name = name;
                this.price = price;
                this.weight = weight;
            }
        }
        List<Future<EnquiryResult>> futures = new ArrayList<>();
        try {
            // 异步处理
            for (QuerierCfg cfg : chain.getCollectors()) {
                Future<EnquiryResult> res = basicQuerierExecutor.submit(() -> {
                    Querier querier = getQuerier(cfg.getCollector());
                    BigDecimal price = querier.tickerPrice(chain, cfg.getBaseurl(), anchorToken);
                    EnquiryResult rs = new EnquiryResult(cfg.getName(), price, cfg.getWeight());
                    return rs;
                });
                futures.add(res);
            }

            // 去掉没有获取到报价的 元素
            List<Future<EnquiryResult>> effectiveList = new ArrayList<>();
            for (Future<EnquiryResult> res : futures) {
                if(null != res.get().price){
                    effectiveList.add(res);
                }
            }
            // 如果报价数足够 则去掉一个最高价 去掉一个最低价
            List<Future<EnquiryResult>> rsList = new ArrayList<>();
            if (effectiveList.size() > QuotationContext.enquiryRemoveMaxMinCount * 2) {
                //排序
                effectiveList.sort(new Comparator<Future<EnquiryResult>>() {
                    @Override
                    public int compare(Future<EnquiryResult> o1, Future<EnquiryResult> o2) {
                        try {
                            return o1.get().price.compareTo(o2.get().price);
                        } catch (Exception e) {
                            chain.getLogger().error(e);
                            return 0;
                        }
                    }
                });
                //去掉头尾各两个元素
                for (int i = QuotationContext.enquiryRemoveMaxMinCount; i < effectiveList.size() - QuotationContext.enquiryRemoveMaxMinCount; i++) {
                    rsList.add(effectiveList.get(i));
                }
            }
            if (!rsList.isEmpty()) {
                effectiveList = rsList;
            }

            BigDecimal interim = new BigDecimal("0");
            BigDecimal weightTotal = new BigDecimal("0.0");
            for (Future<EnquiryResult> future : effectiveList) {
                try {
                    EnquiryResult enquiryResult = future.get();
                    BigDecimal querierPrice = enquiryResult.price;
                    if (null == querierPrice || querierPrice.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    BigDecimal querierWeight = new BigDecimal(enquiryResult.weight);
                    interim = interim.add(querierPrice.multiply(querierWeight));
                    weightTotal = weightTotal.add(querierWeight);
                    chain.getLogger().debug("{}, {}, {}", enquiryResult.name, querierPrice, querierWeight);
                } catch (InterruptedException e) {
                    continue;
                } catch (ExecutionException e) {
                    continue;
                } catch (Exception e) {
                    continue;
                }
            }
            if (weightTotal.doubleValue() == 0) {
                chain.getLogger().error("没有获取token任何第三方价格, anchorToken:{}", anchorToken);
                return null;
            }
            BigDecimal price = interim.divide(weightTotal, QuotationConstant.SCALE, RoundingMode.HALF_DOWN);
            chain.getLogger().debug("interim:{}, weightTotal:{}, price:{}, 总耗时:{}", interim, weightTotal, price, System.currentTimeMillis() - start);
            return price;
        } catch (Throwable e) {
            chain.getLogger().error("获取token第三方价格失败, anchorToken:{}", anchorToken);
            chain.getLogger().error(e);
            return null;
        }
    }

    public Querier getQuerier(String clazz) throws Exception {
        Class<?> clasz = Class.forName(clazz);
        return (Querier) SpringLiteContext.getBean(clasz);
    }

   /* @Override
    public BigDecimal enquiry(Chain chain, String anchorToken) {
        try {
            *//**
     * 根据token从各个查询器采集多个(交易所等)价格
     * 根据不同第三方机构,对价格计算加权平均值
     *//*
            chain.getLogger().info("开始获({})第三方报价", anchorToken);
            BigDecimal interim = new BigDecimal("0");
            BigDecimal weightTotal = new BigDecimal("0.0");
            for (QuerierCfg cfg : chain.getCollectors()) {
                try {
                    Querier querier = getQuerier(cfg.getCollector());
                    BigDecimal price = querier.tickerPrice(chain, cfg.getBaseurl(), anchorToken);
                    if (null == price || price.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    BigDecimal weight = new BigDecimal(cfg.getWeight());
                    interim = interim.add(price.multiply(weight));
                    weightTotal = weightTotal.add(weight);
                } catch (Exception e) {
                    chain.getLogger().error("获取价格异常, name:{}, anchorToken:{}", cfg.getName(), anchorToken);
                    chain.getLogger().error(e);
                    continue;
                }
            }
            if(weightTotal.doubleValue() == 0){
                chain.getLogger().error("没有获取token任何第三方价格, anchorToken:{}", anchorToken);
                return null;
            }
            return interim.divide(weightTotal, QuotationConstant.SCALE, RoundingMode.HALF_DOWN);
        } catch (Throwable e) {
            chain.getLogger().error("获取token第三方价格失败, anchorToken:{}", anchorToken);
            chain.getLogger().error(e);
            return null;
        }
    }*/
}

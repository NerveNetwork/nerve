/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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
package network.nerve.quotation;

import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.DateUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.model.Entry;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.quotation.constant.QuotationConstant;
import network.nerve.quotation.constant.QuotationContext;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.po.NodeQuotationPO;
import network.nerve.quotation.model.po.NodeQuotationWrapperPO;
import network.nerve.quotation.util.CommonUtil;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static network.nerve.quotation.util.LoggerUtil.LOG;

/**
 * @author: PierreLuo
 * @date: 2023/7/14
 */
public class DataTest {

    @Test
    public void testVirtualBankTableTx() throws Exception {
        //RocksDBService.init("/Users/pierreluo/Nuls/quotation");
        RocksDBService.init("/Users/pierreluo/IdeaProjects/nerve-network/data/quotation");
        byte[] bytes = RocksDBService.get("quotation_node_9", StringUtils.bytes("20230226-BTC-USDT"));
        NodeQuotationWrapperPO nodeQuotationWrapperPO = null;
        if(null != bytes){
            try {
                nodeQuotationWrapperPO = CommonUtil.getInstance(bytes, NodeQuotationWrapperPO.class);
            } catch (NulsException e) {
                LOG.error(e);
            }
            List<NodeQuotationPO> list = nodeQuotationWrapperPO.getList();
            /*list.sort(new Comparator<NodeQuotationPO>() {
                @Override
                public int compare(NodeQuotationPO o1, NodeQuotationPO o2) {
                    int compare = o1.getAddress().compareTo(o2.getAddress());
                    if (compare > 0) {
                        return 1;
                    } else if (compare < 0) {
                        return -1;
                    }
                    return 0;
                }
            });*/
            /*list.sort(new Comparator<NodeQuotationPO>() {
                @Override
                public int compare(NodeQuotationPO o1, NodeQuotationPO o2) {
                    if (o1.getBlockTime() > (o2.getBlockTime())) {
                        return 1;
                    } else if (o1.getBlockTime() < (o2.getBlockTime())) {
                        return -1;
                    }
                    return 0;
                }
            });*/
            list.forEach(n -> {
                System.out.println(String.format(
                        "date: %s, txHash: %s, token: %s, address: %s, price: %s",
                        DateUtils.timeStamp2DateStr(n.getBlockTime() * 1000),
                        n.getTxHash(), n.getToken(), n.getAddress(), n.getPrice()
                ));
            });
            System.out.println();
            System.out.println("================================");
            System.out.println();
            distinctNodeTx(list);
            list.forEach(n -> {
                System.out.println(String.format(
                        "date: %s, txHash: %s, token: %s, address: %s, price: %s",
                        DateUtils.timeStamp2DateStr(n.getBlockTime() * 1000),
                        n.getTxHash(), n.getToken(), n.getAddress(), n.getPrice()
                ));
            });
            System.out.println();
            System.out.println("================================");
            System.out.println();
            list = removeMinMax(list);
            list.forEach(n -> {
                System.out.println(String.format(
                        "date: %s, txHash: %s, token: %s, address: %s, price: %s",
                        DateUtils.timeStamp2DateStr(n.getBlockTime() * 1000),
                        n.getTxHash(), n.getToken(), n.getAddress(), n.getPrice()
                ));
            });
            double finalPrice = avgCalc(list);
            System.out.println(String.format("The current section is based on%sQuotation for nodes, final quotation calculation result:%s", list.size(), (new BigDecimal(Double.toString(finalPrice))).toPlainString()));

        }
    }

    private void distinctNodeTx(List<NodeQuotationPO> list) {
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
            if (nq.getBlockTime() >= 1677370518) {
                it.remove();
                continue;
            }
            for (String address : distinctAddress) {
                if (address.equals(nq.getAddress())) {
                    System.out.println(String.format("CalculatorProcessor, Node duplicate quotation address:%s, key:%s", address, nq.getToken()));
                    it.remove();
                }
            }
        }
    }

    private List<NodeQuotationPO> removeMinMax(List<NodeQuotationPO> list) {
        if (QuotationContext.removeMaxMinCount <= 0){
            return list;
        }
        if (list.size() <= QuotationContext.removeMaxMinCount * 2) {
            list.clear();
            return list;
        }
        //sort
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
        //Remove two elements at the beginning and two at the end
        for (int i = QuotationContext.removeMaxMinCount; i < list.size() - QuotationContext.removeMaxMinCount; i++) {
            rsList.add(list.get(i));
        }
        return rsList;
    }

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

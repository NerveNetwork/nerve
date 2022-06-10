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

package network.nerve.converter.utils;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousConfirmedVirtualBank;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.dto.HeterogeneousAddressDTO;
import network.nerve.converter.model.dto.SignAccountDTO;
import network.nerve.converter.model.dto.VirtualBankDirectorDTO;
import network.nerve.converter.rpc.call.ConsensusCall;
import network.nerve.converter.storage.VirtualBankAllHistoryStorageService;
import network.nerve.converter.storage.VirtualBankStorageService;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author: Loki
 * @date: 2020/4/14
 */
public class VirtualBankUtil {

    /**
     * 根据异构链chainId 获取当前节点(虚拟银行) 对应的异构链地址
     *
     * @param chain
     * @param heterogeneousChainId
     * @return
     * @throws NulsException
     */
    public static String getCurrentDirectorHaddress(Chain chain, int heterogeneousChainId) throws NulsException {
        SignAccountDTO signInfo = ConsensusCall.getPackerInfo(chain);
        if (null == signInfo) {
            return null;
        }
        VirtualBankAllHistoryStorageService sevice = SpringLiteContext.getBean(VirtualBankAllHistoryStorageService.class);
        VirtualBankDirector director = sevice.findBySignAddress(chain, signInfo.getAddress());
        if (null == director) {
            return null;
        }
        HeterogeneousAddress heterogeneousAddress = director.getHeterogeneousAddrMap().get(heterogeneousChainId);
        if (null == heterogeneousAddress) {
            return null;
        }
        return heterogeneousAddress.getAddress();
    }


    /**
     * 判断当前节点是否是虚拟银行节点
     * 是:返回签名(出块)地址信息, 不是:返回null
     *
     * @param chain
     * @return
     * @throws NulsException
     */
    public static SignAccountDTO getCurrentDirectorSignInfo(Chain chain) {
        if (!isCurrentDirector(chain)) {
            return null;
        }
        return ConsensusCall.getPackerInfo(chain);
    }

    /**
     * 判断当前节点是否是虚拟银行节点
     *
     * @param chain
     * @return
     * @throws NulsException
     */
    public static boolean isCurrentDirector(Chain chain) {
        return chain.getCurrentIsDirector().get();
    }


    /**
     * 根据虚拟银行成员数, 获取当前签名拜占庭数量
     */
    public static int getByzantineCount(Chain chain) {
        return getByzantineCount(chain, null);
    }

    public static int getByzantineCount(Chain chain, Integer virtualBankTotal) {
        int directorCount = null == virtualBankTotal ? chain.getMapVirtualBank().size() : virtualBankTotal;
        int ByzantineRateCount = directorCount * ConverterContext.BYZANTINERATIO;
        int minPassCount = ByzantineRateCount / ConverterConstant.MAGIC_NUM_100;
        if (ByzantineRateCount % ConverterConstant.MAGIC_NUM_100 > 0) {
            minPassCount++;
        }
        LoggerUtil.LOG.debug("当前共识节点数量为：{}, 拜占庭最少数量为:{}", directorCount, minPassCount);
        return minPassCount;
    }

    /**
     * 从签名列表中去除不匹配的签名, 并返回不匹配的签名列表
     *
     * @param chain
     * @param transactionSignature
     * @param addressSet           虚拟银行成员签名的列表
     * @return
     */
    public static List<P2PHKSignature> getMisMatchSigns(Chain chain, TransactionSignature transactionSignature, Set<String> addressSet) {
        List<P2PHKSignature> misMatchSignList = new ArrayList<>();
        transactionSignature.setP2PHKSignatures(transactionSignature.getP2PHKSignatures().parallelStream().distinct().collect(Collectors.toList()));
        Iterator<P2PHKSignature> iterator = transactionSignature.getP2PHKSignatures().iterator();
        while (iterator.hasNext()) {
            P2PHKSignature signature = iterator.next();
            boolean isMatchSign = false;
            for (String address : addressSet) {
                if (Arrays.equals(AddressTool.getAddress(signature.getPublicKey(), chain.getChainId()), AddressTool.getAddress(address))) {
                    isMatchSign = true;
                    break;
                }
            }
            if (!isMatchSign) {
                misMatchSignList.add(signature);
                iterator.remove();
            }
        }
        return misMatchSignList;
    }

    /**
     * 获取签名数(去重复,并且只统计虚拟银行成员的签名)
     *
     * @param chain
     * @param transactionSignature
     * @param addressSet           虚拟银行成员签名的列表
     * @return
     */
    public static int getSignCountWithoutMisMatchSigns(Chain chain, TransactionSignature transactionSignature, Set<String> addressSet) {
        transactionSignature.setP2PHKSignatures(transactionSignature.getP2PHKSignatures().parallelStream().distinct().collect(Collectors.toList()));
        Iterator<P2PHKSignature> iterator = transactionSignature.getP2PHKSignatures().iterator();
        int count = 0;
        while (iterator.hasNext()) {
            P2PHKSignature signature = iterator.next();
            String signAddress = AddressTool.getStringAddressByBytes(AddressTool.getAddress(signature.getPublicKey(), chain.getChainId()));
            if (addressSet.contains(signAddress)) {
                count++;
            }
        }
        return count;
    }

    public static void sortListByChainId(List<HeterogeneousConfirmedVirtualBank> hList) {
        Collections.sort(hList, new Comparator<HeterogeneousConfirmedVirtualBank>() {
            @Override
            public int compare(HeterogeneousConfirmedVirtualBank o1, HeterogeneousConfirmedVirtualBank o2) {
                if (o1.getHeterogeneousChainId() > o2.getHeterogeneousChainId()) {
                    return 1;
                } else if (o1.getHeterogeneousChainId() < o2.getHeterogeneousChainId()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
    }

    public static void virtualBankAdd(Chain chain, Map<String, VirtualBankDirector> virtualBankMap, List<VirtualBankDirector> directorList, VirtualBankStorageService virtualBankStorageService) {
        if (directorList == null || directorList.isEmpty()) {
            return;
        }
        int size = virtualBankMap.size();
        if (size > 0) {
            List<VirtualBankDirector> list = new ArrayList<>(virtualBankMap.values());
            Collections.sort(list, VirtualBankDirectorSort.getInstance());
            VirtualBankDirector lastDirector = list.get(list.size() - 1);
            VirtualBankDirector lastDirectorFromDB = virtualBankStorageService.findBySignAddress(chain, lastDirector.getSignAddress());
            int lastOrderFromDB = lastDirectorFromDB.getOrder();
            for (VirtualBankDirector director : directorList) {
                director.setOrder(++lastOrderFromDB);
                virtualBankStorageService.save(chain, director);
            }
        }
        for (VirtualBankDirector director : directorList) {
            director.setOrder(++size);
            virtualBankMap.put(director.getSignAddress(), director);
        }
    }

    public static void virtualBankRemove(Chain chain, Map<String, VirtualBankDirector> virtualBankMap, List<VirtualBankDirector> directorList, VirtualBankStorageService virtualBankStorageService) {
        if (directorList == null || directorList.isEmpty()) {
            return;
        }
        for (VirtualBankDirector director : directorList) {
            virtualBankMap.remove(director.getSignAddress());
            virtualBankStorageService.deleteBySignAddress(chain, director.getSignAddress());
        }
        sortDirectorMap(virtualBankMap);
    }

    public static void sortDirectorMap(Map<String, VirtualBankDirector> virtualBankMap) {
        List<VirtualBankDirector> list = new ArrayList<>(virtualBankMap.values());
        Collections.sort(list, VirtualBankDirectorSort.getInstance());
        int i = 1;
        for (VirtualBankDirector director : list) {
            virtualBankMap.get(director.getSignAddress()).setOrder(i++);
        }
    }

    public static void virtualBankDirectorBalance(List<VirtualBankDirectorDTO> list, Chain chain, HeterogeneousDockingManager heterogeneousDockingManager, int logPrint) throws Exception {
        ExecutorService threadPool = null;
        try {
            // 并行查询余额
            threadPool = Executors.newFixedThreadPool(5);
            int fixedCount = 5;
            int listSize = list.size();
            VirtualBankDirectorDTO directorDTO;
            CountDownLatch countDownLatch = null;
            for (int s = 0; s < listSize; s++) {
                directorDTO = list.get(s);
                if (s % fixedCount == 0) {
                    // 剩余的dto数量不小于fixedCount
                    if (listSize - s >= fixedCount) {
                        countDownLatch = new CountDownLatch(fixedCount);
                    } else {
                        // 剩余的dto数量不足fixedCount
                        countDownLatch = new CountDownLatch(listSize - s);
                    }
                }
                threadPool.submit(new GetBalance(chain.getLogger(), heterogeneousDockingManager, directorDTO, countDownLatch, logPrint));
                // 达到CountDown的最大任务数时，等待执行完成
                if ((s + 1) % fixedCount == 0 || (s + 1) == listSize) {
                    countDownLatch.await();
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (threadPool != null) {
                threadPool.shutdown();
            }
        }
    }

    static class GetBalance implements Runnable {
        private NulsLogger logger;
        private CountDownLatch countDownLatch;
        private HeterogeneousDockingManager heterogeneousDockingManager;
        private VirtualBankDirectorDTO directorDTO;
        private int logPrint;

        public GetBalance(NulsLogger logger, HeterogeneousDockingManager heterogeneousDockingManager, VirtualBankDirectorDTO directorDTO, CountDownLatch countDownLatch, int logPrint) {
            this.heterogeneousDockingManager = heterogeneousDockingManager;
            this.countDownLatch = countDownLatch;
            this.directorDTO = directorDTO;
            this.logger = logger;
            this.logPrint = logPrint;
        }

        @Override
        public void run() {
            try {
                for (HeterogeneousAddressDTO addr : directorDTO.getHeterogeneousAddresses()) {
                    IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(addr.getChainId());
                    BigDecimal balance = docking.getBalance(addr.getAddress()).stripTrailingZeros();
                    addr.setBalance(balance.toPlainString());
                    if (this.logPrint % 10 == 0) {
                        logger.info("[{}]成功查询[{}]余额: {}", addr.getAddress(), docking.getChainSymbol(), addr.getBalance());
                    } else {
                        logger.debug("[{}]成功查询[{}]余额: {}", addr.getAddress(), docking.getChainSymbol(), addr.getBalance());
                    }
                }
            } catch (Exception e) {
                logger.error("查询异构链账户余额异常", e);
            } finally {
                countDownLatch.countDown();
            }
        }
    }
}

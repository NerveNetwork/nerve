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
import network.nerve.converter.core.api.ConverterCoreApi;
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
     * Based on heterogeneous chainschainId Get the current node(Virtual banking) Corresponding heterogeneous chain address
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
     * Determine whether the current node is a virtual bank node
     * yes:Return signature(Chunking)Address information, No, it's not:returnnull
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
     * Determine whether the current node is a virtual bank node
     *
     * @param chain
     * @return
     * @throws NulsException
     */
    public static boolean isCurrentDirector(Chain chain) {
        return chain.getCurrentIsDirector().get();
    }


    /**
     * Based on the number of virtual bank members, Obtain the current number of Byzantine signatures
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
        LoggerUtil.LOG.debug("The current number of consensus nodes is：{}, The minimum number of Byzantiums is:{}", directorCount, minPassCount);
        return minPassCount;
    }

    /**
     * Remove mismatched signatures from the signature list, And return a list of mismatched signatures
     *
     * @param chain
     * @param transactionSignature
     * @param addressSet           List of virtual bank member signatures
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
     * Obtain the number of signatures(To repeat,And only count the signatures of virtual bank members)
     *
     * @param chain
     * @param transactionSignature
     * @param addressSet           List of virtual bank member signatures
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
            //try {
            //    chain.getLogger().warn("pierre test===chain info: {}, {}", chain.getCurrentHeterogeneousVersion(), Arrays.toString(ConverterContext.INIT_VIRTUAL_BANK_PUBKEY_LIST.toArray()));
            //    chain.getLogger().warn("pierre test===current virtualBankMap: {}", JSONUtils.obj2json(virtualBankMap));
            //    chain.getLogger().warn("pierre test===remove sign address: {}", director.getSignAddress());
            //} catch (Exception e) {
            //    chain.getLogger().warn("MapVirtualBank log print error ");
            //}

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

    public static void virtualBankDirectorBalance(List<VirtualBankDirectorDTO> list, Chain chain, HeterogeneousDockingManager heterogeneousDockingManager, int logPrint, ConverterCoreApi converterCoreApi) throws Exception {
        ExecutorService threadPool = null;
        try {
            // Parallel query of balance
            threadPool = Executors.newFixedThreadPool(5);
            int fixedCount = 5;
            int listSize = list.size();
            VirtualBankDirectorDTO directorDTO;
            CountDownLatch countDownLatch = null;
            for (int s = 0; s < listSize; s++) {
                directorDTO = list.get(s);
                if (s % fixedCount == 0) {
                    // RemainingdtoQuantity not less thanfixedCount
                    if (listSize - s >= fixedCount) {
                        countDownLatch = new CountDownLatch(fixedCount);
                    } else {
                        // RemainingdtoInsufficient quantityfixedCount
                        countDownLatch = new CountDownLatch(listSize - s);
                    }
                }
                threadPool.submit(new GetBalance(chain, heterogeneousDockingManager, directorDTO, countDownLatch, logPrint, converterCoreApi));
                // achieveCountDownWhen the maximum number of tasks is reached, wait for execution to complete
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
        private Chain chain;
        private NulsLogger logger;
        private CountDownLatch countDownLatch;
        private HeterogeneousDockingManager heterogeneousDockingManager;
        private VirtualBankDirectorDTO directorDTO;
        private int logPrint;
        private ConverterCoreApi converterCoreApi;

        public GetBalance(Chain chain, HeterogeneousDockingManager heterogeneousDockingManager, VirtualBankDirectorDTO directorDTO, CountDownLatch countDownLatch, int logPrint, ConverterCoreApi converterCoreApi) {
            this.chain = chain;
            this.heterogeneousDockingManager = heterogeneousDockingManager;
            this.countDownLatch = countDownLatch;
            this.directorDTO = directorDTO;
            this.logger = chain.getLogger();
            this.logPrint = logPrint;
            this.converterCoreApi = converterCoreApi;
        }

        @Override
        public void run() {
            try {
                for (HeterogeneousAddressDTO addr : directorDTO.getHeterogeneousAddresses()) {

                    if (!converterCoreApi.checkNetworkRunning(addr.getChainId())) {
                        addr.setBalance("0");
                    } else if (chain.getChainId() == 5 && addr.getChainId() == 101) {
                        addr.setBalance("0");
                    } else {
                        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(addr.getChainId());
                        try {
                            BigDecimal balance = docking.getBalance(addr.getAddress()).stripTrailingZeros();
                            addr.setBalance(balance.toPlainString());
                            if (this.logPrint % 10 == 0) {
                                logger.info("[{}] Successfully queried [{}] balance: {}", addr.getAddress(), docking.getChainSymbol(), addr.getBalance());
                            } else {
                                logger.debug("[{}] Successfully queried [{}] balance: {}", addr.getAddress(), docking.getChainSymbol(), addr.getBalance());
                            }
                        } catch (Exception e) {
                            logger.error(String.format("[%s] query [%s] Abnormal balance", addr.getAddress(), docking.getChainSymbol()), e);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Abnormal balance in querying heterogeneous chain accounts", e);
            } finally {
                countDownLatch.countDown();
            }
        }
    }
}

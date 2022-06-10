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

import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.HeterogeneousTxTypeEnum;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;

import java.util.List;

/**
 * @author: Loki
 * @date: 2020/5/18
 */
public class HeterogeneousUtil {

    /**
     * 获取异构链交易
     * @param chain
     * @param heterogeneousChainId
     * @param heterogeneousTxHash
     * @param type 充值/提现
     * @param manager
     * @return
     * @throws NulsException
     */
    public static HeterogeneousTransactionInfo getTxInfo(Chain chain,
                                                         int heterogeneousChainId,
                                                         String heterogeneousTxHash,
                                                         HeterogeneousTxTypeEnum type,
                                                         HeterogeneousDockingManager manager) throws NulsException {
        return getTxInfo(chain, heterogeneousChainId, heterogeneousTxHash, type, manager, null);
    }

    /**
     * 获取异构链交易
     * @param chain
     * @param heterogeneousChainId
     * @param heterogeneousTxHash
     * @param type 充值/提现
     * @param manager
     * @param heterogeneousInterface
     * @return
     * @throws NulsException
     */
    public static HeterogeneousTransactionInfo getTxInfo(Chain chain,
                                                         int heterogeneousChainId,
                                                         String heterogeneousTxHash,
                                                         HeterogeneousTxTypeEnum type,
                                                         HeterogeneousDockingManager manager,
                                                         IHeterogeneousChainDocking heterogeneousInterface) throws NulsException {
        if(null == heterogeneousInterface) {
            heterogeneousInterface = manager.getHeterogeneousDocking(heterogeneousChainId);
        }
        if (null == heterogeneousInterface) {
            chain.getLogger().error("异构链不存在 heterogeneousChainId:{}", heterogeneousChainId);
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
        }
        HeterogeneousTransactionInfo info = null;
        try {
            if(HeterogeneousTxTypeEnum.DEPOSIT == type) {
                info = heterogeneousInterface.getDepositTransaction(heterogeneousTxHash);
            }else if(HeterogeneousTxTypeEnum.WITHDRAWAL == type){
                info = heterogeneousInterface.getWithdrawTransaction(heterogeneousTxHash);
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_INVOK_ERROR);
        }
        return info;
    }


    /**
     * 比较两个异构链地址列表中地址是否相同
     * @param txSigners
     * @param heterogeneousSigners
     * @return
     */
    public static boolean listHeterogeneousAddressEquals(List<HeterogeneousAddress> txSigners, List<HeterogeneousAddress> heterogeneousSigners) {
        if (heterogeneousSigners.size() != txSigners.size()) {
            return false;
        }
        for (HeterogeneousAddress addressSigner : heterogeneousSigners) {
            boolean hit = false;
            for (HeterogeneousAddress address : txSigners) {
                if (address.equals(addressSigner)) {
                    hit = true;
                }
            }
            if (!hit) {
                return false;
            }
        }
        return true;
    }
}

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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

/**
 * @author: Loki
 * @date: 2020/5/18
 */
public class HeterogeneousUtil {

    /**
     * Obtain heterogeneous chain transactions
     * @param chain
     * @param heterogeneousChainId
     * @param heterogeneousTxHash
     * @param type Recharge/Withdrawal
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
     * Obtain heterogeneous chain transactions
     * @param chain
     * @param heterogeneousChainId
     * @param heterogeneousTxHash
     * @param type Recharge/Withdrawal
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
            chain.getLogger().error("Heterogeneous chain does not exist heterogeneousChainId:{}", heterogeneousChainId);
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
     * Compare whether the addresses in two heterogeneous chain address lists are the same
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

    // only (nuls & EVM:enuls) (eth & EVM:goerliETH)
    public static boolean checkHeterogeneousMainAssetBind(int chainId, int htgChainId, int nerveAssetChainId, int nerveAssetId) {
        if (chainId == 5) {
            if (htgChainId == 118) {
                if (nerveAssetChainId == 5 && nerveAssetId == 2) {
                    return true;
                }
            } else if (htgChainId == 119) {
                if (nerveAssetChainId == 2 && nerveAssetId == 1) {
                    return true;
                }
            }
        } else if (chainId == 9) {
            if (htgChainId == 119) {
                if (nerveAssetChainId == 1 && nerveAssetId == 1) {
                    return true;
                }
            }
        }
        return false;
    }

    // exclude (nuls & EVM:enuls) (eth & EVM:goerliETH)
    public static boolean checkHeterogeneousMainAssetReg(int htgChainId) {
        if (htgChainId == 118 || htgChainId == 119) {
            return false;
        }
        return true;
    }

    public static BigInteger getL1Fee(int htgChainId, BigInteger ethNetworkGasPrice) {
        switch (htgChainId) {
            case 130: return getL1FeeOnScroll(_l1GasUsedOnScroll, ethNetworkGasPrice);
            case 133: return getL1FeeOnManta(_l1GasUsedOnManta, ethNetworkGasPrice);
            case 136:
            case 115:
            case 129: return getL1FeeOnOptimismOrBase(_l1GasUsedOnOptimismOrBase, ethNetworkGasPrice);
            default: return BigInteger.ZERO;
        }
    }

    private static final BigInteger _l1GasUsedOnScroll = BigInteger.valueOf(21000L);
    private static final BigInteger _l1GasUsedOnOptimismOrBase = BigInteger.valueOf(18000L);
    private static final BigInteger _l1GasUsedOnManta = BigInteger.valueOf(18000L);
    private static final BigInteger scalarOnScroll = BigInteger.valueOf(1150000000L);
    private static final BigInteger precisionOnScroll = BigInteger.valueOf(1000000000L);
    private static final BigDecimal dynamicOverheadOnOptimismOrBase = new BigDecimal("0.684");
    // look formanta L1 feeof`L1 Fee Scalar`, tentatively set as1
    private static final BigDecimal dynamicOverheadOnManta = new BigDecimal("1");

    private static BigInteger getL1FeeOnScroll(BigInteger _l1GasUsed, BigInteger ethNetworkGasPrice) {
        return _l1GasUsed.multiply(ethNetworkGasPrice).multiply(scalarOnScroll).divide(precisionOnScroll);
    }

    private static BigInteger getL1FeeOnOptimismOrBase(BigInteger _l1GasUsed, BigInteger ethNetworkGasPrice) {
        return new BigDecimal(_l1GasUsed).multiply(dynamicOverheadOnOptimismOrBase).multiply(new BigDecimal(ethNetworkGasPrice)).toBigInteger();
    }

    private static BigInteger getL1FeeOnManta(BigInteger _l1GasUsed, BigInteger ethNetworkGasPrice) {
        return new BigDecimal(_l1GasUsed).multiply(dynamicOverheadOnManta).multiply(new BigDecimal(ethNetworkGasPrice)).toBigInteger();
    }

}

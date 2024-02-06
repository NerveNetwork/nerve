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
package network.nerve.converter.heterogeneouschain.fch.helper;

import apipClass.TxInfo;
import fchClass.CashMark;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.btc.model.BtcUnconfirmedTxPo;
import network.nerve.converter.btc.txdata.RechargeData;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.enums.HeterogeneousChainTxType;
import network.nerve.converter.heterogeneouschain.fch.context.FchContext;
import network.nerve.converter.heterogeneouschain.fch.core.FchWalletApi;
import network.nerve.converter.heterogeneouschain.lib.listener.HtgListener;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.utils.HtgUtil;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;

import java.math.BigInteger;
import java.util.ArrayList;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class FchParseTxHelper implements BeanInitial {

    private FchWalletApi walletApi;
    private HtgListener htgListener;
    private FchContext htgContext;

    private NulsLogger logger() {
        return htgContext.logger();
    }

    public HeterogeneousTransactionInfo parseDepositTransaction(TxInfo txInfo, boolean validate) throws Exception {
        if (txInfo == null) {
            logger().warn("Transaction does not exist");
            return null;
        }
        String htgTxHash = txInfo.getId();
        if (HtgUtil.isEmptyList(txInfo.getSpentCashes())) {
            return null;
        }
        ArrayList<CashMark> issuedCashes = txInfo.getIssuedCashes();
        if (HtgUtil.isEmptyList(issuedCashes)) {
            return null;
        }
        BigInteger value = BigInteger.ZERO;
        String opReturnInfo = null;
        String txTo = null;
        for (CashMark cash : issuedCashes) {
            if (htgListener.isListeningAddress(cash.getOwner())) {
                if (txTo == null) {
                    txTo = cash.getOwner();
                }
                value = value.add(BigInteger.valueOf(cash.getValue()));
            } else if (cash.getOwner().equals("OpReturn")) {
                opReturnInfo = walletApi.getOpReturnInfo(htgTxHash);
            }
        }
        if (value.compareTo(BigInteger.ZERO) == 0) {
            return null;
        }
        RechargeData rechargeData = null;
        boolean error = false;
        do {
            if (StringUtils.isBlank(opReturnInfo)) {
                logger().warn("Illegal recharge information[0] txHash:[{}], opReturnInfo: {}", htgTxHash, opReturnInfo);
                error = true;
                break;
            }
            try {
                rechargeData = new RechargeData();
                rechargeData.parse(HexUtil.decode(opReturnInfo), 0);
            } catch (Exception e) {
                logger().warn(String.format("Illegal recharge information[1] txHash:[%s], opReturnInfo: %s", htgTxHash, opReturnInfo), e);
                error = true;
                break;
            }
            byte[] rechargeDataTo = rechargeData.getTo();
            if (rechargeDataTo == null) {
                logger().warn("[Abnormal recharge address] txHash:[{}], [0]Recharge address is empty", htgTxHash);
                error = true;
                break;
            }
            if (!AddressTool.validAddress(htgContext.NERVE_CHAINID(), rechargeDataTo)) {
                logger().warn("[Abnormal recharge address] txHash:[{}], [0]Recharge address HexData: {}", htgTxHash, HexUtil.encode(rechargeDataTo));
                error = true;
                break;
            }
            boolean hasFeeTo = rechargeData.getFeeTo() != null;
            if (hasFeeTo && !AddressTool.validAddress(htgContext.NERVE_CHAINID(), rechargeData.getFeeTo())) {
                logger().warn("[FeeToAddress abnormality] txHash:[{}], [0]address HexData: {}", htgTxHash, HexUtil.encode(rechargeData.getFeeTo()));
                error = true;
                break;
            }
            BigInteger rechargeValue = BigInteger.valueOf(rechargeData.getValue());
            if (rechargeValue.compareTo(value) > 0) {
                logger().warn("[Abnormal recharge amount] txHash:[{}], [0]Registration amount: {}, Actual amount: {}", htgTxHash, rechargeData.getValue(), value);
                error = true;
                break;
            }
            // If the actual amount is greater than the specified recharge amount and no handling fee address is filled in, the excess amount will be transferred to the team address
            if (value.compareTo(rechargeValue) > 0 && !hasFeeTo) {
                rechargeData.setFeeTo(ConverterContext.AWARD_FEE_SYSTEM_ADDRESS_PROTOCOL_1_17_0);
            }
        } while (false);
        if (validate && error) {
            return null;
        }

        BtcUnconfirmedTxPo po = new BtcUnconfirmedTxPo();
        po.setFee(BigInteger.ZERO);
        po.setValue(value);
        po.setFrom(txInfo.getSpentCashes().get(0).getOwner());
        po.setTo(txTo);
        if (rechargeData != null) {
            po.setNerveAddress(rechargeData.getTo() != null ? AddressTool.getStringAddressByBytes(rechargeData.getTo()) : null);
            po.setValue(BigInteger.valueOf(rechargeData.getValue()));
            po.setFee(value.subtract(BigInteger.valueOf(rechargeData.getValue())));
            po.setNerveFeeTo(rechargeData.getFeeTo() != null ? AddressTool.getStringAddressByBytes(rechargeData.getFeeTo()) : null);
            po.setExtend0(rechargeData.getExtend0());
            po.setExtend1(rechargeData.getExtend1());
            po.setExtend2(rechargeData.getExtend2());
            po.setExtend3(rechargeData.getExtend3());
            po.setExtend4(rechargeData.getExtend4());
            po.setExtend5(rechargeData.getExtend5());
        }
        po.setTxType(HeterogeneousChainTxType.DEPOSIT);
        po.setTxHash(htgTxHash);
        po.setBlockHeight(txInfo.getHeight());
        po.setTxTime(txInfo.getBlockTime());
        po.setDecimals(htgContext.ASSET_NAME().decimals());
        po.setIfContractAsset(false);
        po.setAssetId(1);
        return po;
    }

    public HeterogeneousTransactionInfo parseDepositTransaction(String txHash, boolean validate) throws Exception {
        TxInfo txInfo = walletApi.getTransactionByHash(txHash);
        return this.parseDepositTransaction(txInfo, validate);
    }
}

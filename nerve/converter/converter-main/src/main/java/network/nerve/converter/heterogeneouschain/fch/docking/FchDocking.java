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
package network.nerve.converter.heterogeneouschain.fch.docking;

import apipClass.TxInfo;
import keyTools.KeyTools;
import network.nerve.converter.core.api.interfaces.IBitCoinApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.IBitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.docking.BitCoinLibDocking;
import network.nerve.converter.heterogeneouschain.fch.context.FchContext;
import network.nerve.converter.heterogeneouschain.fch.core.FchBitCoinApi;
import network.nerve.converter.heterogeneouschain.fch.core.FchWalletApi;
import network.nerve.converter.heterogeneouschain.fch.helper.FchAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.fch.utils.FchUtil;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgCommonHelper;
import network.nerve.converter.heterogeneouschain.lib.model.HtgAccount;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgUnconfirmedTxStorageService;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousConfirmedInfo;
import network.nerve.converter.model.bo.WithdrawalUTXO;
import txTools.FchTool;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class FchDocking extends BitCoinLibDocking {

    private FchContext context;
    private FchBitCoinApi fchBitCoinApi;
    private FchWalletApi walletApi;
    private HtgUnconfirmedTxStorageService htgUnconfirmedTxStorageService;
    private HtgCommonHelper htgCommonHelper;
    private FchAnalysisTxHelper htgAnalysisTxHelper;

    @Override
    protected HtgUnconfirmedTxStorageService getHtgUnconfirmedTxStorageService() {
        return htgUnconfirmedTxStorageService;
    }

    @Override
    public IBitCoinApi getBitCoinApi() {
        return fchBitCoinApi;
    }

    @Override
    protected HtgContext context() {
        return context;
    }

    @Override
    protected IBitCoinLibWalletApi walletApi() {
        return walletApi;
    }

    @Override
    protected HtgAccount _createAccount(String prikey, boolean mainnet) {
        return FchUtil.createAccount(prikey);
    }

    @Override
    protected HtgAccount _createAccountByPubkey(String pubkeyStr, boolean mainnet) {
        return FchUtil.createAccountByPubkey(pubkeyStr);
    }

    @Override
    public String generateAddressByCompressedPublicKey(String compressedPublicKey) {
        return KeyTools.pubKeyToFchAddr(compressedPublicKey);
    }

    @Override
    public boolean validateAddress(String address) {
        return KeyTools.isValidFchAddr(address);
    }

    @Override
    protected Object[] _makeChangeTxBaseInfo(HtgContext htgContext, WithdrawalUTXO withdrawalUTXO, List<String> multiSignAddressPubs) {
        return FchUtil.makeChangeTxBaseInfo(htgContext, withdrawalUTXO, multiSignAddressPubs);
    }

    @Override
    public String genMultiSignAddress(int threshold, List<byte[]> pubs, boolean mainnet) {
        return FchUtil.genMultiP2sh(pubs, threshold, true).getFid();
    }

    @Override
    public HeterogeneousConfirmedInfo getConfirmedTxInfo(String txHash) throws Exception {
        HeterogeneousConfirmedInfo info = new HeterogeneousConfirmedInfo();
        TxInfo tx = walletApi.getTransactionByHash(txHash);
        if (tx == null || tx.getHeight() <= 0) {
            return null;
        }
        String btcFeeReceiverPub = context.getConverterCoreApi().getBtcFeeReceiverPub();
        String btcFeeReceiver = FchTool.pubkeyToAddr(btcFeeReceiverPub);
        List<HeterogeneousAddress> signers = new ArrayList<>();
        signers.add(new HeterogeneousAddress(context.getConfig().getChainId(), btcFeeReceiver));
        Long txTime = tx.getBlockTime();

        info.setMultySignAddress(context.MULTY_SIGN_ADDRESS());
        info.setTxTime(txTime);
        info.setSigners(signers);
        return info;
    }

    @Override
    public Boolean reAnalysisDepositTx(String htgTxHash) throws Exception {
        if (htgCommonHelper.constainHash(htgTxHash)) {
            logger().info("Repeated collection of recharge transactions hash: {} No more repeated parsing[0]", htgTxHash);
            return true;
        }
        reAnalysisLock.lock();
        try {
            if (htgCommonHelper.constainHash(htgTxHash)) {
                logger().info("Repeated collection of recharge transactions hash: {} No more repeated parsing[1]", htgTxHash);
                return true;
            }
            logger().info("Re analyze recharge transactions: {}", htgTxHash);
            TxInfo tx = walletApi.getTransactionByHash(htgTxHash);
            long txTime = tx.getBlockTime();
            htgAnalysisTxHelper.analysisTx(tx, txTime, tx.getHeight(), tx.getBlockId());
            htgCommonHelper.addHash(htgTxHash);
            return true;
        } catch (Exception e) {
            throw e;
        } finally {
            reAnalysisLock.unlock();
        }
    }

    @Override
    public Boolean reAnalysisTx(String htgTxHash) throws Exception {
        if (htgCommonHelper.constainHash(htgTxHash)) {
            logger().info("Repeated collection of transactions hash: {} No more repeated parsing[0]", htgTxHash);
            return true;
        }
        reAnalysisLock.lock();
        try {
            if (htgCommonHelper.constainHash(htgTxHash)) {
                logger().info("Repeated collection of transactions hash: {} No more repeated parsing[1]", htgTxHash);
                return true;
            }
            logger().info("Re analyze transactions: {}", htgTxHash);
            TxInfo txInfo = walletApi.getTransactionByHash(htgTxHash);
            long txTime = txInfo.getBlockTime();
            htgUnconfirmedTxStorageService.deleteByTxHash(htgTxHash);
            htgAnalysisTxHelper.analysisTx(txInfo, txTime, txInfo.getHeight(), txInfo.getBlockId());
            htgCommonHelper.addHash(htgTxHash);
            return true;
        } catch (Exception e) {
            throw e;
        } finally {
            reAnalysisLock.unlock();
        }
    }

    @Override
    public String getAddressString(byte[] addressBytes) {
        return KeyTools.hash160ToFchAddr(addressBytes);
    }

    @Override
    public byte[] getAddressBytes(String addressString) {
        return KeyTools.addrToHash160(addressString);
    }

}

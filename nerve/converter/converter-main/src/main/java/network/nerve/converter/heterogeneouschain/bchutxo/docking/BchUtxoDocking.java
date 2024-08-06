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
package network.nerve.converter.heterogeneouschain.bchutxo.docking;

import com.neemre.btcdcli4j.core.domain.RawTransaction;
import network.nerve.converter.core.api.interfaces.IBitCoinApi;
import network.nerve.converter.heterogeneouschain.bchutxo.utils.BchUtxoUtil;
import network.nerve.converter.heterogeneouschain.bchutxo.utils.addr.CashAddress;
import network.nerve.converter.heterogeneouschain.bchutxo.utils.addr.CashAddressFactory;
import network.nerve.converter.heterogeneouschain.bchutxo.utils.addr.MainNetParamsForAddr;
import network.nerve.converter.heterogeneouschain.bchutxo.utils.addr.TestNet4ParamsForAddr;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.IBitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.docking.BitCoinLibDocking;
import network.nerve.converter.heterogeneouschain.bitcoinlib.helper.BitCoinLibAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.bitcoinlib.utils.BitCoinLibUtil;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgCommonHelper;
import network.nerve.converter.heterogeneouschain.lib.model.HtgAccount;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgUnconfirmedTxStorageService;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousConfirmedInfo;
import network.nerve.converter.model.bo.WithdrawalUTXO;
import org.bitcoinj.base.Base58;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class BchUtxoDocking extends BitCoinLibDocking {

    private HtgContext context;
    private IBitCoinApi bitCoinApi;
    private BitCoinLibWalletApi walletApi;
    private HtgUnconfirmedTxStorageService htgUnconfirmedTxStorageService;
    private HtgCommonHelper htgCommonHelper;
    private BitCoinLibAnalysisTxHelper htgAnalysisTxHelper;

    @Override
    protected HtgUnconfirmedTxStorageService getHtgUnconfirmedTxStorageService() {
        return htgUnconfirmedTxStorageService;
    }

    @Override
    public IBitCoinApi getBitCoinApi() {
        return bitCoinApi;
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
        return BitCoinLibUtil.createAccount(prikey, mainnet);
    }

    @Override
    protected HtgAccount _createAccountByPubkey(String pubkeyStr, boolean mainnet) {
        return BitCoinLibUtil.createAccountByPubkey(pubkeyStr, mainnet);
    }

    @Override
    public boolean validateAddress(String address) {
        return CashAddress.isValidCashAddr(context.getConverterCoreApi().isNerveMainnet() ? MainNetParamsForAddr.get() : TestNet4ParamsForAddr.get(), address);
    }

    @Override
    protected Object[] _makeChangeTxBaseInfo(HtgContext htgContext, WithdrawalUTXO withdrawalUTXO, List<String> multiSignAddressPubs) {
        return BchUtxoUtil.makeChangeTxBaseInfo(htgContext, withdrawalUTXO, multiSignAddressPubs);
    }

    @Override
    protected String _genMultiSignAddress(int threshold, List<byte[]> pubECKeys, boolean mainnet) {
        return BchUtxoUtil.multiAddr(pubECKeys, threshold, mainnet);
    }

    @Override
    public String generateAddressByCompressedPublicKey(String compressedPublicKey) {
        return BchUtxoUtil.getBchAddress(compressedPublicKey, context.getConverterCoreApi().isNerveMainnet());
    }

    @Override
    public HeterogeneousConfirmedInfo getConfirmedTxInfo(String txHash) throws Exception {
        HeterogeneousConfirmedInfo info = new HeterogeneousConfirmedInfo();
        RawTransaction tx = walletApi.getTransactionByHash(txHash);
        if (tx == null || tx.getConfirmations() == null || tx.getConfirmations().intValue() == 0) {
            return null;
        }
        String btcFeeReceiverPub = context.getConverterCoreApi().getBtcFeeReceiverPub();
        String btcFeeReceiver = BchUtxoUtil.getBchAddress(btcFeeReceiverPub, context.getConverterCoreApi().isNerveMainnet());

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
            logger().info("Repeated collection of recharge transactionshash: {}No more repeated parsing[0]", htgTxHash);
            return true;
        }
        reAnalysisLock.lock();
        try {
            if (htgCommonHelper.constainHash(htgTxHash)) {
                logger().info("Repeated collection of recharge transactionshash: {}No more repeated parsing[1]", htgTxHash);
                return true;
            }
            logger().info("Re analyze recharge transactions: {}", htgTxHash);
            RawTransaction tx = walletApi.getTransactionByHash(htgTxHash);
            Long txTime = tx.getBlockTime();
            Long height = Long.valueOf(walletApi.getBlockHeaderByHash(tx.getBlockHash()).getHeight());
            htgAnalysisTxHelper.analysisTx(tx, txTime, height, tx.getBlockHash());
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
            logger().info("Repeated collection of transactionshash: {}No more repeated parsing[0]", htgTxHash);
            return true;
        }
        reAnalysisLock.lock();
        try {
            if (htgCommonHelper.constainHash(htgTxHash)) {
                logger().info("Repeated collection of transactionshash: {}No more repeated parsing[1]", htgTxHash);
                return true;
            }
            logger().info("Re analyze transactions: {}", htgTxHash);
            RawTransaction txInfo = walletApi.getTransactionByHash(htgTxHash);
            Long txTime = txInfo.getBlockTime();
            htgUnconfirmedTxStorageService.deleteByTxHash(htgTxHash);
            Long height = Long.valueOf(walletApi.getBlockHeaderByHash(txInfo.getBlockHash()).getHeight());
            htgAnalysisTxHelper.analysisTx(txInfo, txTime, height, txInfo.getBlockHash());
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
        int version = context.getConverterCoreApi().isNerveMainnet() ? 0 : 111;
        String base58 = Base58.encodeChecked(version, addressBytes);
        return CashAddressFactory.create().getFromBase58(context.getConverterCoreApi().isNerveMainnet() ? MainNetParamsForAddr.get() : TestNet4ParamsForAddr.get(), base58).toString();
    }

    @Override
    public byte[] getAddressBytes(String addressString) {
        CashAddress cashAddress = CashAddressFactory.create().getFromFormattedAddress(context.getConverterCoreApi().isNerveMainnet() ? MainNetParamsForAddr.get() : TestNet4ParamsForAddr.get(), addressString);
        return cashAddress.getHash160();
    }
}

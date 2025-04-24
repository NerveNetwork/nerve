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
package network.nerve.converter.heterogeneouschain.tbc.docking;

import com.neemre.btcdcli4j.core.domain.RawTransaction;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.core.api.interfaces.IBitCoinApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.BitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.core.IBitCoinLibWalletApi;
import network.nerve.converter.heterogeneouschain.bitcoinlib.docking.BitCoinLibDocking;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgCommonHelper;
import network.nerve.converter.heterogeneouschain.lib.helper.HtgERC20Helper;
import network.nerve.converter.heterogeneouschain.lib.model.HtgAccount;
import network.nerve.converter.heterogeneouschain.lib.model.HtgERC20Po;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgUnconfirmedTxStorageService;
import network.nerve.converter.heterogeneouschain.tbc.context.TbcContext;
import network.nerve.converter.heterogeneouschain.tbc.core.TbcBitCoinApi;
import network.nerve.converter.heterogeneouschain.tbc.core.TbcWalletApi;
import network.nerve.converter.heterogeneouschain.tbc.helper.TbcAnalysisTxHelper;
import network.nerve.converter.heterogeneouschain.tbc.model.FtInfo;
import network.nerve.converter.heterogeneouschain.tbc.utils.TbcUtil;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.HeterogeneousConfirmedInfo;
import network.nerve.converter.model.bo.WithdrawalUTXO;
import org.bitcoinj.base.Base58;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2023/12/26
 */
public class TbcDocking extends BitCoinLibDocking {

    private TbcContext context;
    private TbcBitCoinApi bitCoinApi;
    private BitCoinLibWalletApi walletApi;
    protected HtgERC20Helper htgERC20Helper;
    private HtgUnconfirmedTxStorageService htgUnconfirmedTxStorageService;
    private HtgCommonHelper htgCommonHelper;
    private TbcAnalysisTxHelper tbcAnalysisTxHelper;

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
        return TbcUtil.createAccount(prikey);
    }

    @Override
    protected HtgAccount _createAccountByPubkey(String pubkeyStr, boolean mainnet) {
        return TbcUtil.createAccountByPubkey(pubkeyStr);
    }

    @Override
    public boolean validateAddress(String address) {
        return TbcUtil.validateAddress(address);
    }

    private TbcWalletApi tbcWalletApi() {
        return (TbcWalletApi) walletApi;
    }
    @Override
    public boolean validateHeterogeneousAssetInfoFromNet(String contractAddress, String symbol, int decimals) throws Exception {
        FtInfo tbc20Info = tbcWalletApi().getTbc20Info(contractAddress);
        if (tbc20Info == null) {
            return false;
        }
        if (!tbc20Info.getFtSymbol().equals(symbol)) {
            return false;
        }
        if (tbc20Info.getFtDecimal() != decimals) {
            return false;
        }
        return true;
    }

    @Override
    public HeterogeneousAssetInfo getAssetByContractAddress(String contractAddress) {
        if (StringUtils.isBlank(contractAddress)) {
            return getMainAsset();
        }
        HtgERC20Po erc20Po = htgERC20Helper.getERC20ByContractAddress(contractAddress);
        if (erc20Po == null) {
            return null;
        }
        HeterogeneousAssetInfo assetInfo = new HeterogeneousAssetInfo();
        assetInfo.setChainId(context.getConfig().getChainId());
        assetInfo.setAssetId(erc20Po.getAssetId());
        assetInfo.setDecimals((byte) erc20Po.getDecimals());
        assetInfo.setSymbol(erc20Po.getSymbol());
        assetInfo.setContractAddress(erc20Po.getAddress());
        return assetInfo;
    }

    @Override
    public HeterogeneousAssetInfo getAssetByAssetId(int assetId) {
        if (context.HTG_ASSET_ID() == assetId) {
            return this.getMainAsset();
        }
        HtgERC20Po erc20Po = htgERC20Helper.getERC20ByAssetId(assetId);
        if (erc20Po == null) {
            return null;
        }
        HeterogeneousAssetInfo assetInfo = new HeterogeneousAssetInfo();
        assetInfo.setChainId(context.getConfig().getChainId());
        assetInfo.setAssetId(erc20Po.getAssetId());
        assetInfo.setDecimals((byte) erc20Po.getDecimals());
        assetInfo.setSymbol(erc20Po.getSymbol());
        assetInfo.setContractAddress(erc20Po.getAddress());
        return assetInfo;
    }

    @Override
    public void saveHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception {
        if (assetInfos == null || assetInfos.isEmpty()) {
            return;
        }
        htgERC20Helper.saveHeterogeneousAssetInfos(assetInfos);
    }

    @Override
    public void rollbackHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception {
        if (assetInfos == null || assetInfos.isEmpty()) {
            return;
        }
        htgERC20Helper.rollbackHeterogeneousAssetInfos(assetInfos);
    }

    @Override
    protected Object[] _makeChangeTxBaseInfo(HtgContext htgContext, WithdrawalUTXO withdrawalUTXO, List<String> multiSignAddressPubs) {
        return new Object[0];
    }

    @Override
    public String genMultiSignAddress(int threshold, List<byte[]> pubECKeys, boolean mainnet) {
        return TbcUtil.genMultisigAddress(threshold, pubECKeys);
    }

    @Override
    public String generateAddressByCompressedPublicKey(String compressedPublicKey) {
        return TbcUtil.getBtcLegacyAddress(HexUtil.decode(compressedPublicKey));
    }

    @Override
    public HeterogeneousConfirmedInfo getConfirmedTxInfo(String txHash) throws Exception {
        HeterogeneousConfirmedInfo info = new HeterogeneousConfirmedInfo();
        RawTransaction tx = walletApi.getTransactionByHash(txHash);
        if (tx == null || tx.getConfirmations() == null || tx.getConfirmations().intValue() == 0) {
            return null;
        }
        String btcFeeReceiverPub = context.getConverterCoreApi().getBtcFeeReceiverPub();
        String btcFeeReceiver = TbcUtil.getBtcLegacyAddress(btcFeeReceiverPub);
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
            List list = tbcAnalysisTxHelper.fetchVinInfoOfMultiSign(List.of(tx));
            tbcAnalysisTxHelper.analysisTx(list.get(0), txTime, height, tx.getBlockHash());
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
            List list = tbcAnalysisTxHelper.fetchVinInfoOfMultiSign(List.of(txInfo));
            tbcAnalysisTxHelper.analysisTx(list.get(0), txTime, height, txInfo.getBlockHash());
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
        int version = 0;
        return Base58.encodeChecked(version, addressBytes);
    }

    @Override
    public byte[] getAddressBytes(String addressString) {
        byte[] addrBytes = Base58.decode(addressString);
        byte[] hash160Bytes = new byte[20];
        System.arraycopy(addrBytes, 1, hash160Bytes, 0, 20);
        return hash160Bytes;
    }

}

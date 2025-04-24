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
package network.nerve.converter.helper;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.btc.txdata.UTXOData;
import network.nerve.converter.btc.txdata.WithdrawalUTXORebuildPO;
import network.nerve.converter.btc.txdata.WithdrawalUTXOTxData;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.UTXONeed;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.po.WithdrawalAdditionalFeePO;
import network.nerve.converter.storage.HeterogeneousAssetConverterStorageService;
import network.nerve.converter.storage.HeterogeneousChainInfoStorageService;
import network.nerve.converter.storage.TxStorageService;
import network.nerve.converter.utils.ConverterUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: Mimi
 * @date: 2020-10-21
 */
@Component
public class HeterogeneousAssetHelper {

    @Autowired
    private HeterogeneousAssetConverterStorageService heterogeneousAssetConverterStorageService;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private HeterogeneousChainInfoStorageService heterogeneousChainInfoStorageService;
    @Autowired
    private AssembleTxService assembleTxService;
    @Autowired
    private TxStorageService txStorageService;
    @Autowired
    private ConverterCoreApi converterCoreApi;

    public List<HeterogeneousAssetInfo> getHeterogeneousAssetInfo(int nerveAssetChainId, int nerveAssetId) {
        return heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(nerveAssetChainId, nerveAssetId);
    }

    public HeterogeneousAssetInfo getHeterogeneousAssetInfo(int heterogeneousChainId, int nerveAssetChainId, int nerveAssetId) {
        if (heterogeneousChainId == 0) {
            heterogeneousChainId = ConverterConstant.FIRST_HETEROGENEOUS_ASSET_CHAIN_ID;
        }
        return heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(heterogeneousChainId, nerveAssetChainId, nerveAssetId);
    }

    public WithdrawalUTXOTxData makeWithdrawalUTXOsTxData(Chain chain, Transaction tx, int htgChainId, String currentMultiSignAddress, Map<String, VirtualBankDirector> mapVirtualBank) throws Exception {
        int currenVirtualBankTotal = mapVirtualBank.size();
        String txHash = tx.getHash().toHex();
        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
        HeterogeneousAssetInfo assetInfo = null;
        CoinTo withdrawCoinTo = null;
        byte[] withdrawalBlackhole = AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, chain.getChainId());
        for (CoinTo coinTo : coinData.getTo()) {
            if (Arrays.equals(withdrawalBlackhole, coinTo.getAddress())) {
                assetInfo = this.getHeterogeneousAssetInfo(htgChainId, coinTo.getAssetsChainId(), coinTo.getAssetsId());
                if (assetInfo != null) {
                    withdrawCoinTo = coinTo;
                    break;
                }
            }
        }
        if (null == assetInfo) {
            chain.getLogger().error("[Heterogeneous chain address signature message-withdraw] no withdrawCoinTo. hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
        int heterogeneousChainId = assetInfo.getChainId();
        BigInteger amount = withdrawCoinTo.getAmount();
        // docking get utxos
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
        UTXONeed needUTXO = docking.getBitCoinApi().getNeedUTXO(docking.getCurrentMultySignAddress(), amount, assetInfo.getAssetId());
        List<UTXOData> utxos = needUTXO.getUtxoDataList();
        chain.getLogger().info("hash: {}, addr: {}, utxo size: {}", txHash, docking.getCurrentMultySignAddress(), utxos.size());
        utxos.sort(ConverterUtil.BITCOIN_SYS_COMPARATOR);
        BigInteger feeRate = BigInteger.valueOf(docking.getBitCoinApi().getFeeRate());
        List<UTXOData> needUtxos;
        do {
            if (heterogeneousChainId == AssetName.TBC.chainId()) {
                // tbc not check, as check inner tbc codes: getNeedUTXO
                needUtxos = utxos;
                break;
            }
            needUtxos = new ArrayList<>();
            BigInteger spend = amount;
            BigInteger total = BigInteger.ZERO;

            for (UTXOData utxo : utxos) {
                chain.getLogger().info("utxo data: {}", utxo);
                // check utxo locked
                boolean lockedUTXO = docking.getBitCoinApi().isLockedUTXO(utxo.getTxid(), utxo.getVout());
                if (lockedUTXO) {
                    continue;
                }
                total = total.add(utxo.getAmount());
                needUtxos.add(utxo);
                spend = amount.add(BigInteger.valueOf(docking.getBitCoinApi().getWithdrawalFeeSize(total.longValue(), amount.longValue(), feeRate.longValue(), needUtxos.size())).multiply(feeRate));
                if (total.compareTo(spend) >= 0) {
                    break;
                }
            }
            if (total.compareTo(spend) < 0) {
                chain.getLogger().info("[{}] not enough utxos to withdraw", txHash);
                throw new NulsException(ConverterErrorCode.DATA_ERROR);
            }
        } while (false);

        List<byte[]> pubs = docking.getBitCoinApi().getMultiSignAddressPubs(docking.getCurrentMultySignAddress()).stream().map(d -> HexUtil.decode(d)).collect(Collectors.toList());
        pubs.sort(new Comparator<byte[]>() {
            @Override
            public int compare(byte[] o1, byte[] o2) {
                return Arrays.compare(o1, o2);
            }
        });
        WithdrawalUTXOTxData txData = new WithdrawalUTXOTxData();
        txData.setNerveTxHash(txHash);
        txData.setHtgChainId(htgChainId);
        txData.setCurrentMultiSignAddress(currentMultiSignAddress);
        txData.setCurrentVirtualBankTotal(currenVirtualBankTotal);
        txData.setFeeRate(docking.getBitCoinApi().getFeeRate());
        txData.setPubs(pubs);
        txData.setUtxoDataList(needUtxos);
        txData.setScript(needUTXO.getScript());
        txData.setFtAddress(needUTXO.getFtAddress());
        txData.setFtUtxoDataList(needUTXO.getFtUTXODataList());
        return txData;
    }

    public WithdrawalUTXOTxData rebuildWithdrawalUTXOsTxData(Chain chain, Transaction tx, int htgChainId) throws Exception {
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(htgChainId);
        String txHash = tx.getHash().toHex();
        WithdrawalUTXOTxData utxoTxData = docking.getBitCoinApi().takeWithdrawalUTXOs(txHash);
        if (utxoTxData == null) {
            throw new RuntimeException(String.format("Empty WithdrawalUTXOsTxData, hash: %s", txHash));
        }

        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
        HeterogeneousAssetInfo assetInfo = null;
        CoinTo withdrawCoinTo = null;
        CoinTo feeCoinTo = null;
        byte[] withdrawalFeeAddress = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
        byte[] withdrawalBlackhole = AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, chain.getChainId());
        for (CoinTo coinTo : coinData.getTo()) {
            if (Arrays.equals(withdrawalBlackhole, coinTo.getAddress())) {
                assetInfo = this.getHeterogeneousAssetInfo(htgChainId, coinTo.getAssetsChainId(), coinTo.getAssetsId());
                if (assetInfo != null) {
                    withdrawCoinTo = coinTo;
                }
            } else if (Arrays.equals(withdrawalFeeAddress, coinTo.getAddress())) {
                feeCoinTo = coinTo;
            }
        }
        if (null == assetInfo) {
            chain.getLogger().error("[Heterogeneous chain address signature message-withdraw] no withdrawCoinTo. hash:{}", txHash);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
        AssetName feeAssetName = converterCoreApi.getHtgMainAssetName(feeCoinTo.getAssetsChainId(), feeCoinTo.getAssetsId());
        BigInteger amount = withdrawCoinTo.getAmount();
        // total adding fee
        BigInteger totalAddingFee = BigInteger.ZERO;
        WithdrawalAdditionalFeePO po = txStorageService.getWithdrawalAdditionalFeePO(chain, txHash);
        if (null == po || null == po.getMapAdditionalFee()) {
            throw new RuntimeException(String.format("Empty WithdrawalAdditionalFeePO, hash: %s", txHash));
        }
        WithdrawalUTXORebuildPO rebuildPO = docking.getBitCoinApi().getWithdrawalUTXORebuildPO(txHash);
        if (null == rebuildPO || null == rebuildPO.getNerveTxHashSet()) {
            throw new RuntimeException(String.format("Empty WithdrawalUTXORebuildPO, hash: %s", txHash));
        }
        Map<String, BigInteger> mapAdditionalFee = po.getMapAdditionalFee();
        Set<String> addingFeeTxHashSet = rebuildPO.getNerveTxHashSet();
        for (String addingFeeTxHash : addingFeeTxHashSet) {
            BigInteger fee = mapAdditionalFee.get(addingFeeTxHash);
            if (fee == null) {
                throw new RuntimeException(String.format("Empty Fee, hash: %s, addingFeeTxHash: %s", txHash, addingFeeTxHash));
            }
            totalAddingFee = totalAddingFee.add(fee);
        }
        long mainFee = docking.getBitCoinApi().convertMainAssetByFee(feeAssetName, new BigDecimal(totalAddingFee));
        List<UTXOData> utxoDataList = utxoTxData.getUtxoDataList();
        BigInteger total = BigInteger.ZERO;
        for (UTXOData utxo : utxoDataList) {
            total = total.add(utxo.getAmount());
        }

        long txSize = docking.getBitCoinApi().getWithdrawalFeeSize(total.longValue(), amount.longValue(), rebuildPO.getBaseFeeRate(), utxoDataList.size());
        long addingFeeRate = mainFee / txSize;
        if (addingFeeRate == 0) {
            throw new RuntimeException(String.format("Not enough adding fee to rebuild utxo, hash: %s", txHash));
        }

        BigInteger feeRate = BigInteger.valueOf(rebuildPO.getBaseFeeRate() + addingFeeRate);
        BigInteger spend = amount.add(BigInteger.valueOf(txSize).multiply(feeRate));


        if (total.compareTo(spend) < 0) {
            // docking get utxos
            List<UTXOData> utxos = docking.getBitCoinApi().getUTXOs(utxoTxData.getCurrentMultiSignAddress());
            utxos.sort(ConverterUtil.BITCOIN_SYS_COMPARATOR);
            for (UTXOData utxo : utxos) {
                // check utxo locked
                boolean lockedUTXO = docking.getBitCoinApi().isLockedUTXO(utxo.getTxid(), utxo.getVout());
                if (lockedUTXO) {
                    continue;
                }
                total = total.add(utxo.getAmount());
                utxoDataList.add(utxo);
                spend = amount.add(BigInteger.valueOf(docking.getBitCoinApi().getWithdrawalFeeSize(total.longValue(), amount.longValue(), feeRate.longValue(), utxoDataList.size())).multiply(feeRate));
                if (total.compareTo(spend) >= 0) {
                    break;
                }
            }
            if (total.compareTo(spend) < 0) {
                chain.getLogger().info("[{}] not enough utxos to withdraw", txHash);
                throw new NulsException(ConverterErrorCode.DATA_ERROR);
            }
        }
        WithdrawalUTXOTxData txData = new WithdrawalUTXOTxData();
        txData.setNerveTxHash(txHash);
        txData.setHtgChainId(htgChainId);
        txData.setCurrentMultiSignAddress(utxoTxData.getCurrentMultiSignAddress());
        txData.setCurrentVirtualBankTotal(utxoTxData.getCurrentVirtualBankTotal());
        txData.setFeeRate(feeRate.longValue());
        txData.setPubs(utxoTxData.getPubs());
        txData.setUtxoDataList(utxoDataList);
        return txData;
    }

    public WithdrawalUTXOTxData makeManagerChangeUTXOTxData(Chain chain, Transaction tx, int htgChainId, String currentMultiSignAddress, Map<String, VirtualBankDirector> mapVirtualBank) throws Exception {
        int currenVirtualBankTotal = mapVirtualBank.size();
        String txHash = tx.getHash().toHex();
        // docking get utxos
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(htgChainId);
        List<UTXOData> utxos = docking.getBitCoinApi().getUTXOs(docking.getCurrentMultySignAddress());
        utxos.sort(ConverterUtil.BITCOIN_SYS_COMPARATOR);
        List<UTXOData> needUtxos = new ArrayList<>();
        for (UTXOData utxo : utxos) {
            // check utxo locked
            boolean lockedUTXO = docking.getBitCoinApi().isLockedUTXO(utxo.getTxid(), utxo.getVout());
            if (lockedUTXO) {
                continue;
            }
            needUtxos.add(utxo);
        }

        List<byte[]> pubs = chain.getMapVirtualBank().values().stream().map(d -> HexUtil.decode(d.getSignAddrPubKey())).sorted(new Comparator<byte[]>() {
            @Override
            public int compare(byte[] o1, byte[] o2) {
                return Arrays.compare(o1, o2);
            }
        }).collect(Collectors.toList());
        WithdrawalUTXOTxData txData = new WithdrawalUTXOTxData();
        txData.setNerveTxHash(txHash);
        txData.setHtgChainId(htgChainId);
        txData.setCurrentMultiSignAddress(currentMultiSignAddress);
        txData.setCurrentVirtualBankTotal(currenVirtualBankTotal);
        txData.setFeeRate(docking.getBitCoinApi().getFeeRate());
        txData.setPubs(pubs);
        txData.setUtxoDataList(needUtxos);
        return txData;
    }

    public WithdrawalUTXOTxData rebuildManagerChangeUTXOTxData(Chain chain, Transaction tx, int htgChainId, String currentMultiSignAddress, Map<String, VirtualBankDirector> mapVirtualBank) throws Exception {
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(htgChainId);
        String txHash = tx.getHash().toHex();
        WithdrawalUTXOTxData utxoTxData = docking.getBitCoinApi().takeWithdrawalUTXOs(txHash);
        if (utxoTxData == null) {
            throw new RuntimeException(String.format("Empty WithdrawalUTXOsTxData, hash: %s", txHash));
        }
        // total adding fee
        BigInteger totalAddingFee = BigInteger.ZERO;
        WithdrawalAdditionalFeePO po = txStorageService.getWithdrawalAdditionalFeePO(chain, txHash);
        if (null == po || null == po.getMapAdditionalFee()) {
            throw new RuntimeException(String.format("Empty WithdrawalAdditionalFeePO, hash: %s", txHash));
        }
        WithdrawalUTXORebuildPO rebuildPO = docking.getBitCoinApi().getWithdrawalUTXORebuildPO(txHash);
        if (null == rebuildPO || null == rebuildPO.getNerveTxHashSet()) {
            throw new RuntimeException(String.format("Empty WithdrawalUTXORebuildPO, hash: %s", txHash));
        }
        Map<String, BigInteger> mapAdditionalFee = po.getMapAdditionalFee();
        Set<String> addingFeeTxHashSet = rebuildPO.getNerveTxHashSet();
        for (String addingFeeTxHash : addingFeeTxHashSet) {
            BigInteger fee = mapAdditionalFee.get(addingFeeTxHash);
            if (fee == null) {
                throw new RuntimeException(String.format("Empty Fee, hash: %s, addingFeeTxHash: %s", txHash, addingFeeTxHash));
            }
            totalAddingFee = totalAddingFee.add(fee);
        }
        // docking get utxos
        List<UTXOData> needUtxos = utxoTxData.getUtxoDataList();
        BigInteger total = BigInteger.ZERO;
        for (UTXOData utxo : needUtxos) {
            total = total.add(utxo.getAmount());
        }
        List<UTXOData> utxos = docking.getBitCoinApi().getUTXOs(docking.getCurrentMultySignAddress());
        utxos.sort(ConverterUtil.BITCOIN_SYS_COMPARATOR);
        for (UTXOData utxo : utxos) {
            // check utxo locked
            boolean lockedUTXO = docking.getBitCoinApi().isLockedUTXO(utxo.getTxid(), utxo.getVout());
            if (lockedUTXO) {
                continue;
            }
            needUtxos.add(utxo);
            total = total.add(utxo.getAmount());
        }
        long mainFee = docking.getBitCoinApi().convertMainAssetByFee(AssetName.NVT, new BigDecimal(totalAddingFee));
        long txSize = docking.getBitCoinApi().getChangeFeeSize(needUtxos.size());
        long addingFeeRate = mainFee / txSize;
        if (addingFeeRate == 0) {
            throw new RuntimeException(String.format("Not enough adding fee to rebuild utxo, hash: %s", txHash));
        }
        BigInteger feeRate = BigInteger.valueOf(rebuildPO.getBaseFeeRate() + addingFeeRate);
        BigInteger fee = BigInteger.valueOf(txSize).multiply(feeRate);
        if (total.compareTo(fee) < 0) {
            throw new RuntimeException(String.format("Not enough utxos to change addr, hash: %s", txHash));
        }
        List<byte[]> pubs = chain.getMapVirtualBank().values().stream().map(d -> HexUtil.decode(d.getSignAddrPubKey())).sorted(new Comparator<byte[]>() {
            @Override
            public int compare(byte[] o1, byte[] o2) {
                return Arrays.compare(o1, o2);
            }
        }).collect(Collectors.toList());
        WithdrawalUTXOTxData txData = new WithdrawalUTXOTxData();
        txData.setNerveTxHash(txHash);
        txData.setHtgChainId(htgChainId);
        txData.setCurrentMultiSignAddress(utxoTxData.getCurrentMultiSignAddress());
        txData.setCurrentVirtualBankTotal(utxoTxData.getCurrentVirtualBankTotal());
        txData.setFeeRate(feeRate.longValue());
        txData.setPubs(pubs);
        txData.setUtxoDataList(needUtxos);
        return txData;
    }
}
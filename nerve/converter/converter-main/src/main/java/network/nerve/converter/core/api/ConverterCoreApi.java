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
package network.nerve.converter.core.api;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.btc.txdata.WithdrawalUTXOTxData;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.core.context.HeterogeneousChainManager;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.helper.LedgerAssetRegisterHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.message.ComponentSignMessage;
import network.nerve.converter.model.HeterogeneousSign;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.model.dto.HeterogeneousAddressDTO;
import network.nerve.converter.model.dto.VirtualBankDirectorDTO;
import network.nerve.converter.model.po.ComponentSignByzantinePO;
import network.nerve.converter.model.po.ConfirmWithdrawalPO;
import network.nerve.converter.model.txdata.WithdrawalTxData;
import network.nerve.converter.rpc.call.AccountCall;
import network.nerve.converter.rpc.call.LedgerCall;
import network.nerve.converter.rpc.call.QuotationCall;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.ComponentSignStorageService;
import network.nerve.converter.storage.ConfirmWithdrawalStorageService;
import network.nerve.converter.storage.HeterogeneousChainInfoStorageService;
import network.nerve.converter.storage.VirtualBankStorageService;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.HeterogeneousUtil;
import network.nerve.converter.utils.VirtualBankUtil;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: Mimi
 * @date: 2020-05-08
 */
@Component
public class ConverterCoreApi implements IConverterCoreApi {

    private Chain nerveChain;
    @Autowired
    private VirtualBankService virtualBankService;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousAssetHelper heterogeneousAssetHelper;
    @Autowired
    private ComponentSignStorageService componentSignStorageService;
    @Autowired
    private LedgerAssetRegisterHelper ledgerAssetRegisterHelper;
    @Autowired
    private ConfirmWithdrawalStorageService confirmWithdrawalStorageService;
    @Autowired
    private AssembleTxService assembleTxService;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private HeterogeneousChainManager heterogeneousChainManager;
    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private HeterogeneousChainInfoStorageService heterogeneousChainInfoStorageService;
    @Autowired
    private VirtualBankStorageService virtualBankStorageService;

    private Map<Integer, NerveAssetInfo> htgMainAssetMap = new HashMap<>(16);
    private Map<NerveAssetInfo, AssetName> htgMainAssetByNerveMap = new HashMap<>(16);
    private Set<String> skipTransactions = new HashSet<>();
    private Map<Integer, String> chainDBNameMap = new HashMap<>();
    private Map<Integer, Boolean> dbMergedChainMap = new HashMap<>();
    private volatile BigDecimal fchToDogePrice = new BigDecimal("0.05");
    private volatile List<Runnable> htgConfirmTxHandlers = new ArrayList<>();
    private volatile List<Runnable> htgRpcAvailableHandlers = new ArrayList<>();
    private volatile List<Runnable> htgWaitingTxInvokeDataHandlers = new ArrayList<>();

    Set<Integer> skippedTestNetworks;
    private Set<Integer> l1FeeChainSet = new HashSet<>();
    private Map<String, Integer> seedPacker = new HashMap<>();
    private String btcFeeReceiverPub;

    public ConverterCoreApi() {
        /*
            101 eth, 102 bsc, 103 heco, 104 oec, 105 Harmony(ONE), 106 Polygon(MATIC), 107 kcc(KCS),
            108 TRX, 109 CRO, 110 AVAX, 111 AETH, 112 FTM, 113 METIS, 114 IOTX, 115 OETH, 116 KLAY, 117 BCH,
            119 ENULS, 120 KAVA, 121 ETHW, 122 REI, 123 ZK, 124 EOS, 125 ZKpolygon, 126 Linea,
            127 Celo, 128 ETC, 129 Base, 130 Scroll, 131 Brise, 132 Janus, 133 Manta, 134 XLayer(OKB), 135 ZETA,
            138 Mode-ETH, 139 Blast-ETH, 140 Merlin-BTC, 141 PLS, 142 Mint-ETH
            201 BTC, 202 FCH
         */
        skippedTestNetworks = new HashSet<>();
        skippedTestNetworks.add(103);
        skippedTestNetworks.add(105);
        skippedTestNetworks.add(111);
        skippedTestNetworks.add(112);
        skippedTestNetworks.add(113);
        skippedTestNetworks.add(115);
        skippedTestNetworks.add(117);
        skippedTestNetworks.add(133);

        skippedTestNetworks.add(106);
        skippedTestNetworks.add(109);
        skippedTestNetworks.add(123);
        skippedTestNetworks.add(125);
        skippedTestNetworks.add(129);
        skippedTestNetworks.add(130);
        skippedTestNetworks.add(132);

        l1FeeChainSet.add(115);
        l1FeeChainSet.add(129);
        l1FeeChainSet.add(130);
        l1FeeChainSet.add(133);
        l1FeeChainSet.add(136);
        l1FeeChainSet.add(138);
        l1FeeChainSet.add(139);
        l1FeeChainSet.add(142);
    }

    private void initSeedPackerOrder(Chain chain) {
        List<String> seedPubkeyList = ConverterContext.INIT_VIRTUAL_BANK_PUBKEY_LIST;
        int i = 0;
        for (String pub : seedPubkeyList) {
            if (i == 0) {
                btcFeeReceiverPub = pub;
            }
            seedPacker.put(AddressTool.getStringAddressByBytes(AddressTool.getAddress(HexUtil.decode(pub), chain.getChainId())), i++);
        }
        //if (chain.getChainId() == 9) {
        //    seedPacker.put("NERVEepb69uqMbNRufoPz6QGerCMtDG4ybizAA", 0);
        //    seedPacker.put("NERVEepb649o7fSmXPBCM4F6cAJsfPQoQSbnBB", 1);
        //    seedPacker.put("NERVEepb6Cu6CC2uYpS2pAgmaReHjgPwtNGbCC", 2);
        //} else {
        //    seedPacker.put("TNVTdTSPLGfeN8cS9tLBnYnjYjk4MrMabDgcK", 0);
        //    seedPacker.put("TNVTdTSPRdfVQaiS6MzaY7M1bgfauCbEVZMhF", 1);
        //    seedPacker.put("TNVTdTSPQmKV5o9dhsN6TiXKot4mPLQXQLyHz", 2);
        //
        //    seedPacker.put("TNVTdTSPLbhQEw4hhLc2Enr5YtTheAjg8yDsV", 0);
        //    seedPacker.put("TNVTdTSPMGoSukZyzpg23r3A7AnaNyi3roSXT", 1);
        //    seedPacker.put("TNVTdTSPV7WotsBxPc4QjbL8VLLCoQfHPXWTq", 2);
        //}
    }

    public NulsLogger logger() {
        return nerveChain.getLogger();
    }

    public void setNerveChain(Chain nerveChain) {
        this.nerveChain = nerveChain;
        // Skip historical legacy problem transactions
        if (nerveChain.getChainId() == 9) {
            skipTransactions.add("b0a3f4e0f7f28b6d55ced8f333e63f0844c25f061b8a843f8c49c6a0612ccd8d");
            // add on 2024 05/14 for manager change
            skipTransactions.add("960ed1c4253e9be5077474aabe4cd58e7d51cfb496a395c4fd6c3136849be20c");
            skipTransactions.add("84a3120bce337cb0088b151c09ecca54f828c94b3eaed1d025a2a67132738fbd");
            skipTransactions.add("82153d88055b7053c22d0531141538ecb185d128b147dc4213fdc77cf02c0e10");
        }
        initSeedPackerOrder(nerveChain);
    }

    public void setFchToDogePrice(BigDecimal fchToDogePrice) {
        if (fchToDogePrice == null) {
            return;
        }
        this.fchToDogePrice = fchToDogePrice;
    }

    @Override
    public long getCurrentBlockHeightOnNerve() {
        return nerveChain.getLatestBasicBlock().getHeight();
    }

    @Override
    public boolean isVirtualBankByCurrentNode() {
        try {
            return virtualBankService.getCurrentDirector(nerveChain.getChainId()) != null;
        } catch (NulsException e) {
            logger().error("Failed to query node status", e);
            return false;
        }
    }

    @Override
    public boolean isSeedVirtualBankByCurrentNode() {
        try {
            VirtualBankDirector currentDirector = virtualBankService.getCurrentDirector(nerveChain.getChainId());
            if (null != currentDirector) {
                return currentDirector.getSeedNode();
            }
            return false;
        } catch (NulsException e) {
            logger().error("Failed to query node status", e);
            return false;
        }
    }

    @Override
    public int getVirtualBankOrder() {
        try {
            VirtualBankDirector director = virtualBankService.getCurrentDirector(nerveChain.getChainId());
            if (director == null) {
                return 0;
            }
            return director.getOrder();
        } catch (NulsException e) {
            logger().error("Failed to query node status", e);
            return 0;
        }
    }

    @Override
    public int getVirtualBankSize() {
        return nerveChain.getMapVirtualBank().size();
    }

    @Override
    public Transaction getNerveTx(String hash) {
        try {
            if (!ConverterUtil.isHexStr(hash)) {
                return null;
            }
            return TransactionCall.getConfirmedTx(nerveChain, NulsHash.fromHex(hash));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean isRunning() {
        return SyncStatusEnum.RUNNING == nerveChain.getLatestBasicBlock().getSyncStatusEnum();
    }

    @Override
    public HeterogeneousWithdrawTxInfo getWithdrawTxInfo(String nerveTxHash) throws NulsException {
        Transaction tx = TransactionCall.getConfirmedTx(nerveChain, NulsHash.fromHex(nerveTxHash));
        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);
        WithdrawalTxData txData = ConverterUtil.getInstance(tx.getTxData(), WithdrawalTxData.class);
        HeterogeneousAssetInfo heterogeneousAssetInfo = null;
        CoinTo withdrawCoinTo = null;
        byte[] withdrawalBlackhole = AddressTool.getAddress(ConverterContext.WITHDRAWAL_BLACKHOLE_PUBKEY, nerveChain.getChainId());
        for (CoinTo coinTo : coinData.getTo()) {
            if (Arrays.equals(withdrawalBlackhole, coinTo.getAddress())) {
                heterogeneousAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(txData.getHeterogeneousChainId(), coinTo.getAssetsChainId(), coinTo.getAssetsId());
                if (heterogeneousAssetInfo != null) {
                    withdrawCoinTo = coinTo;
                    break;
                }
            }
        }
        if (null == heterogeneousAssetInfo) {
            nerveChain.getLogger().error("Withdraw transaction cointo data error, no withdrawCoinTo. hash:{}", nerveTxHash);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }

        HeterogeneousWithdrawTxInfo info = new HeterogeneousWithdrawTxInfo();
        info.setHeterogeneousChainId(heterogeneousAssetInfo.getChainId());
        info.setAssetId(heterogeneousAssetInfo.getAssetId());
        info.setNerveTxHash(nerveTxHash);
        info.setToAddress(txData.getHeterogeneousAddress());
        info.setValue(withdrawCoinTo.getAmount());
        return info;
    }

    private BigDecimal minBalance = new BigDecimal("0.04");
    /**
     * Return the current list and order of virtual banks under the specified heterogeneous chain
     * When the balance of heterogeneous chains is lower than0.04Move the order to the end
     */
    @Override
    public Map<String, Integer> currentVirtualBanks(int hChainId) {
        Map<String, VirtualBankDirectorDTO> cacheMap = ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST.stream().collect(Collectors.toMap(VirtualBankDirectorDTO::getSignAddress, Function.identity(), (key1, key2) -> key2));
        Map<String, Integer> resultMap = new HashMap<>();
        Map<String, VirtualBankDirector> map = nerveChain.getMapVirtualBank();
        map.entrySet().stream().forEach(e -> {
            VirtualBankDirector director = e.getValue();
            HeterogeneousAddress address = director.getHeterogeneousAddrMap().get(hChainId);
            if (address != null) {
                int order = director.getOrder();
                VirtualBankDirectorDTO cacheDto = cacheMap.get(director.getSignAddress());
                // When the balance of heterogeneous chains is lower than0.04Move the order to the end
                if (cacheDto != null) {
                    List<HeterogeneousAddressDTO> cacheBalanceList = cacheDto.getHeterogeneousAddresses();
                    if (cacheBalanceList != null && !cacheBalanceList.isEmpty()) {
                        HeterogeneousAddressDTO cacheAddressDTO = null;
                        for (HeterogeneousAddressDTO addressDTO : cacheBalanceList) {
                            if (addressDTO.getChainId() == address.getChainId()) {
                                cacheAddressDTO = addressDTO;
                                break;
                            }
                        }
                        if (cacheAddressDTO != null) {
                            String balance = cacheAddressDTO.getBalance();
                            if (StringUtils.isNotBlank(balance)) {
                                BigDecimal realBalance = new BigDecimal(balance);
                                if (realBalance.compareTo(minBalance) < 0) {
                                    order = order + ConverterContext.VIRTUAL_BANK_AGENT_TOTAL * 2;
                                }
                            }
                        }
                    }
                }
                resultMap.put(address.getAddress(), order);
            }
        });
        nerveChain.getLogger().debug("Current Bank Order: {}", resultMap);
        return resultMap;
    }

    /**
     * Return the current virtual bank list and balance order under the specified heterogeneous chain（Reverse balance order）
     */
    @Override
    public Map<String, Integer> currentVirtualBanksBalanceOrder(int hChainId) {
        Map<String, VirtualBankDirectorDTO> cacheMap = ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST.stream().collect(Collectors.toMap(VirtualBankDirectorDTO::getSignAddress, Function.identity(), (key1, key2) -> key2));
        try {
            logger().info("ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST: {}", JSONUtils.obj2json(cacheMap));
        } catch (Exception e) {
            logger().error(e);
        }
        Map<String, BigDecimal> balanceMap = new HashMap<>();
        Map<String, Integer> resultMap = new HashMap<>();
        Map<String, VirtualBankDirector> map = nerveChain.getMapVirtualBank();
        map.entrySet().stream().forEach(e -> {
            VirtualBankDirector director = e.getValue();
            HeterogeneousAddress address = director.getHeterogeneousAddrMap().get(hChainId);
            if (address != null) {
                BigDecimal realBalance = BigDecimal.ZERO;
                VirtualBankDirectorDTO cacheDto = cacheMap.get(director.getSignAddress());
                // Obtain balance
                if (cacheDto != null) {
                    List<HeterogeneousAddressDTO> cacheBalanceList = cacheDto.getHeterogeneousAddresses();
                    if (cacheBalanceList != null && !cacheBalanceList.isEmpty()) {
                        HeterogeneousAddressDTO cacheAddressDTO = null;
                        for (HeterogeneousAddressDTO addressDTO : cacheBalanceList) {
                            if (addressDTO.getChainId() == address.getChainId()) {
                                cacheAddressDTO = addressDTO;
                                break;
                            }
                        }
                        if (cacheAddressDTO != null) {
                            String balance = cacheAddressDTO.getBalance();
                            if (StringUtils.isNotBlank(balance)) {
                                realBalance = new BigDecimal(balance);
                            }
                        }
                    }
                }
                balanceMap.put(address.getAddress(), realBalance);
            }
        });
        List<Map.Entry<String, BigDecimal>> list = new ArrayList<>(balanceMap.entrySet());
        list.sort(ConverterUtil.BALANCE_SORT);
        int i = 1;
        for (Map.Entry<String, BigDecimal> entry : list) {
            resultMap.put(entry.getKey(), i++);
        }
        nerveChain.getLogger().info("Current Bank Order: {}, balanceMap: {}", resultMap, balanceMap);
        return resultMap;
    }


    /**
     * Requesting a new Byzantine signature
     */
    @Override
    public List<HeterogeneousSign> regainSignatures(int nerveChainId, String nerveTxHash, int hChainId) {
        Chain chain = chainManager.getChain(nerveChainId);
        List<HeterogeneousSign> list = new ArrayList<>();
        ComponentSignByzantinePO compSignPO = componentSignStorageService.get(chain, nerveTxHash);
        if(null == compSignPO){
            return list;
        }
        if (null == compSignPO.getListMsg() || compSignPO.getListMsg().isEmpty()) {
            return list;
        }
        for (ComponentSignMessage msg : compSignPO.getListMsg()) {
            for (HeterogeneousSign sign : msg.getListSign()) {
                if(sign.getHeterogeneousAddress().getChainId() == hChainId){
                    list.add(sign);
                }
            }
        }
        return list;
    }

    @Override
    public boolean isBoundHeterogeneousAsset(int hChainId, int hAssetId) {
        /**
          Is the asset obtained from the new storage a bound heterogeneous chain asset, Otherwise, obtain asset types from the ledger for judgment
               greater than4And less than9When, it is to bind assets（5~8It is a pure bound type asset）
               When the asset type is9Due to the type of native asset4It is a heterogeneous registered asset that supports9Before type assets,4If there is no bound asset for the type asset, it indicates that the heterogeneous asset is not of a bound type（If the asset is bound, the new storage must have stored it as a bound asset）
               9Type assets, one network is of heterogeneous registration type, while the other networks are of binding type
         */
        try {
            Boolean isBound = ledgerAssetRegisterHelper.isBoundHeterogeneousAsset(hChainId, hAssetId);
            if (isBound) {
                return true;
            }
            NerveAssetInfo nerveAssetInfo = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, hAssetId);
            if (nerveAssetInfo == null) {
                return false;
            }
            Map<String, Object> asset = LedgerCall.getNerveAsset(nerveChain.getChainId(), nerveAssetInfo.getAssetChainId(), nerveAssetInfo.getAssetId());
            if (asset == null) {
                return false;
            }
            Integer assetType = Integer.parseInt(asset.get("assetType").toString());
            if (assetType > 4) {
                if (assetType == 9) {
                    return false;
                }
                return true;
            }
        } catch (NulsException e) {
            nerveChain.getLogger().warn("query bind nerve asset error, msg: {}", e.format());
            return false;
        } catch (Exception e) {
            nerveChain.getLogger().warn("query bind nerve asset error, msg: {}", e.getMessage());
            return false;
        }
        return false;
    }

    @Override
    public boolean isWithdrawalComfired(String nerveTxHash) {
        NulsHash hash = NulsHash.fromHex(nerveTxHash);
        ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(nerveChain, hash);
        return null != po;
    }

    @Override
    public WithdrawalTotalFeeInfo getFeeOfWithdrawTransaction(String nerveTxHash) throws NulsException {
        // according toNERVEWithdrawal transactionstxHashReturn the total transaction fee provided
        return assembleTxService.calculateWithdrawalTotalFee(nerveChain, this.getNerveTx(nerveTxHash));
    }

    @Override
    public BigDecimal getUsdtPriceByAsset(AssetName assetName) {
        String key = ConverterContext.priceKeyMap.get(assetName.name());
        if (assetName == AssetName.FCH) {
            return getFchUsdtPrice();
        }
        //TODO pierre test code
        //if (assetName == AssetName.ETH) {
        //    key = "ETH_PRICE";
        //}
        if (key == null) {
            return BigDecimal.ZERO;
        }
        // Obtain the specified asset'sUSDTprice
        return QuotationCall.getPriceByOracleKey(nerveChain, key);
    }

    private BigDecimal getFchUsdtPrice() {
        String key = ConverterContext.priceKeyMap.get(AssetName.FCH.name());
        BigDecimal price = QuotationCall.getPriceByOracleKey(nerveChain, key);
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            price = QuotationCall.getPriceByOracleKey(nerveChain, "DOGE_PRICE");
            price = price.multiply(fchToDogePrice);
        }
        return price;
    }

    @Override
    public boolean isSupportNewMechanismOfWithdrawalFee() {
        return nerveChain.getLatestBasicBlock().getHeight() >= ConverterContext.FEE_ADDITIONAL_HEIGHT;
    }

    @Override
    public boolean isSupportProtocol12ERC20OfTransferBurn() {
        return nerveChain.getLatestBasicBlock().getHeight() >= ConverterContext.PROTOCOL_12_ERC20_OF_TRANSFER_BURN_HEIGHT;
    }

    @Override
    public boolean validNerveAddress(String address) {
        boolean valid = AddressTool.validAddress(nerveChain.getChainId(), address);
        // v1.14.0 After the protocol upgrade, address verification adds verification logic and is replaced with a byte array, which is then regeneratedbase58After the address is provided, communicate with the userbase58Avoiding the problem of different address prefixes compared to addresses
        if (isProtocol14() && valid) {
            byte[] addressBytes = AddressTool.getAddress(address);
            String addressStr = AddressTool.getStringAddressByBytes(addressBytes);
            valid = address.equals(addressStr);
        }
        nerveChain.getLogger().debug("chainId: {}, address: {}, valid: {}", nerveChain.getChainId(), address, valid);
        return valid;
    }

    @Override
    public boolean isSupportProtocol13NewValidationOfERC20() {
        return nerveChain.getLatestBasicBlock().getHeight() >= ConverterContext.PROTOCOL_13_NEW_VALIDATION_OF_ERC20;
    }

    @Override
    public boolean isProtocol14() {
        return nerveChain.getLatestBasicBlock().getHeight() >= ConverterContext.PROTOCOL_1_14_0;
    }

    @Override
    public boolean isSupportProtocol15TrxCrossChain() {
        return nerveChain.getLatestBasicBlock().getHeight() >= ConverterContext.PROTOCOL_15_TRX_CROSS_CHAIN_HEIGHT;
    }

    @Override
    public boolean isProtocol16() {
        return nerveChain.getLatestBasicBlock().getHeight() >= ConverterContext.PROTOCOL_1_16_0;
    }

    @Override
    public boolean isProtocol21() {
        return nerveChain.getLatestBasicBlock().getHeight() >= ConverterContext.PROTOCOL_1_21_0;
    }

    @Override
    public boolean isProtocol22() {
        return nerveChain.getLatestBasicBlock().getHeight() >= ConverterContext.PROTOCOL_1_22_0;
    }

    @Override
    public boolean isProtocol23() {
        return nerveChain.getLatestBasicBlock().getHeight() >= ConverterContext.PROTOCOL_1_23_0;
    }

    @Override
    public boolean isProtocol24() {
        return nerveChain.getLatestBasicBlock().getHeight() >= ConverterContext.PROTOCOL_1_24_0;
    }

    @Override
    public boolean isProtocol26() {
        return nerveChain.getLatestBasicBlock().getHeight() >= ConverterContext.PROTOCOL_1_26_0;
    }

    @Override
    public boolean isProtocol27() {
        return nerveChain.getLatestBasicBlock().getHeight() >= ConverterContext.PROTOCOL_1_27_0;
    }

    @Override
    public boolean isProtocol31() {
        return nerveChain.getLatestBasicBlock().getHeight() >= ConverterContext.PROTOCOL_1_31_0;
    }
    @Override
    public boolean isProtocol33() {
        return nerveChain.getLatestBasicBlock().getHeight() >= ConverterContext.PROTOCOL_1_33_0;
    }

    @Override
    public boolean isProtocol34() {
        return nerveChain.getLatestBasicBlock().getHeight() >= ConverterContext.PROTOCOL_1_34_0;
    }

    @Override
    public boolean isProtocol35() {
        return nerveChain.getLatestBasicBlock().getHeight() >= ConverterContext.PROTOCOL_1_35_0;
    }

    private void loadHtgMainAsset() {
        if (heterogeneousDockingManager.getAllHeterogeneousDocking().size() == htgMainAssetMap.size()) return;
        AssetName[] values = AssetName.values();
        for (AssetName assetName : values) {
            int htgChainId = assetName.chainId();
            if (htgChainId < 101) continue;
            this.getHtgMainAsset(htgChainId);/**/
        }
    }
    /**
     * Obtain heterogeneous chain master assets(Register toNERVEAsset information on the internet)
     */
    public NerveAssetInfo getHtgMainAsset(int htgChainId) {
        NerveAssetInfo nerveAssetInfo = htgMainAssetMap.get(htgChainId);
        if (nerveAssetInfo == null) {
            nerveAssetInfo = ledgerAssetRegisterHelper.getNerveAssetInfo(htgChainId, 1);
            if (nerveAssetInfo == null || nerveAssetInfo.isEmpty()) {
                return NerveAssetInfo.emptyInstance();
            }
            htgMainAssetMap.put(htgChainId, nerveAssetInfo);
            htgMainAssetByNerveMap.put(nerveAssetInfo, AssetName.getEnum(htgChainId));
        }
        return nerveAssetInfo;
    }

    public void clearHtgMainAssetMap() {
        htgMainAssetMap.clear();
        htgMainAssetByNerveMap.clear();
    }

    public boolean isHtgMainAsset(Coin coin) {
        if (coin == null) return false;
        return isHtgMainAsset(coin.getAssetsChainId(), coin.getAssetsId());
    }

    private boolean isHtgMainAsset(int nerveAssetChainId, int nerveAssetId) {
        // Load cache
        this.loadHtgMainAsset();
        // Check if it is a heterogeneous chain master asset
        AssetName assetName = htgMainAssetByNerveMap.get(new NerveAssetInfo(nerveAssetChainId, nerveAssetId));
        if (assetName != null) {
            return true;
        }
        return false;
    }

    public AssetName getHtgMainAssetName(Coin coin) {
        if (coin == null) return null;
        return getHtgMainAssetName(coin.getAssetsChainId(), coin.getAssetsId());
    }

    public AssetName getHtgMainAssetName(int nerveAssetChainId, int nerveAssetId) {
        if (nerveAssetChainId == nerveChain.getChainId() && nerveAssetId == 1) {
            return AssetName.NVT;
        }
        // Load cache
        this.loadHtgMainAsset();
        return htgMainAssetByNerveMap.get(new NerveAssetInfo(nerveAssetChainId, nerveAssetId));
    }

    public List<Runnable> getHtgConfirmTxHandlers() {
        return htgConfirmTxHandlers;
    }

    public List<Runnable> getHtgRpcAvailableHandlers() {
        return htgRpcAvailableHandlers;
    }

    public List<Runnable> getHtgWaitingTxInvokeDataHandlers() {
        return htgWaitingTxInvokeDataHandlers;
    }

    public void addHtgConfirmTxHandler(Runnable runnable) {
        htgConfirmTxHandlers.add(runnable);
    }

    public void addHtgRpcAvailableHandler(Runnable runnable) {
        htgRpcAvailableHandlers.add(runnable);
    }

    public void addHtgWaitingTxInvokeDataHandler(Runnable runnable) {
        htgWaitingTxInvokeDataHandlers.add(runnable);
    }

    @Override
    public boolean skippedTransaction(String nerveTxHash) {
        return skipTransactions.contains(nerveTxHash);
    }

    @Override
    public ConverterConfig getConverterConfig() {
        return converterConfig;
    }

    @Override
    public boolean isPauseInHeterogeneousAsset(int hChainId, int hAssetId) throws Exception {
        return ledgerAssetRegisterHelper.isPauseInHeterogeneousAsset(hChainId, hAssetId);
    }

    @Override
    public boolean isPauseOutHeterogeneousAsset(int hChainId, int hAssetId) throws Exception {
        return ledgerAssetRegisterHelper.isPauseOutHeterogeneousAsset(hChainId, hAssetId);
    }

    @Override
    public Map<Long, Map> HTG_RPC_CHECK_MAP() {
        return ConverterContext.HTG_RPC_CHECK_MAP;
    }

    @Override
    public HeterogeneousAssetInfo getHeterogeneousAsset(int hChainId, int hAssetId) {
        NerveAssetInfo nerveAssetInfo = ledgerAssetRegisterHelper.getNerveAssetInfo(hChainId, hAssetId);
        if (nerveAssetInfo == null) {
            return null;
        }
        HeterogeneousAssetInfo assetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(hChainId, nerveAssetInfo.getAssetChainId(), nerveAssetInfo.getAssetId());
        return assetInfo;
    }

    @Override
    public BigInteger checkDecimalsSubtractedToNerveForWithdrawal(int htgChainId, int htgAssetId, BigInteger value) {
        // Cross chain asset accuracy varies, conversion accuracy
        HeterogeneousAssetInfo heterogeneousAsset = this.getHeterogeneousAsset(htgChainId, htgAssetId);
        return checkDecimalsSubtractedToNerveForWithdrawal(heterogeneousAsset, value);
    }

    @Override
    public BigInteger checkDecimalsSubtractedToNerveForWithdrawal(HeterogeneousAssetInfo heterogeneousAsset, BigInteger value) {
        if (heterogeneousAsset == null) {
            return value;
        }
        String decimalsSubtractedToNerve = heterogeneousAsset.getDecimalsSubtractedToNerve();
        if (StringUtils.isBlank(decimalsSubtractedToNerve)) {
            return value;
        }
        BigInteger decimalsSubtracted = new BigInteger(decimalsSubtractedToNerve);
        if (decimalsSubtracted.compareTo(BigInteger.ZERO) < 0) {
            value = new BigDecimal(value).movePointLeft(decimalsSubtracted.intValue()).toBigInteger();
        } else if (decimalsSubtracted.compareTo(BigInteger.ZERO) > 0) {
            value = new BigDecimal(value).movePointRight(decimalsSubtracted.intValue()).toBigInteger();
        }
        return value;
    }

    @Override
    public BigInteger checkDecimalsSubtractedToNerveForDeposit(int htgChainId, int nerveAssetChainId, int nerveAssetId, BigInteger value) {
        // Cross chain asset accuracy varies, conversion accuracy
        HeterogeneousAssetInfo heterogeneousAsset = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, nerveAssetChainId, nerveAssetId);
        return checkDecimalsSubtractedToNerveForDeposit(heterogeneousAsset, value);
    }

    @Override
    public BigInteger checkDecimalsSubtractedToNerveForDeposit(HeterogeneousAssetInfo heterogeneousAsset, BigInteger value) {
        if (heterogeneousAsset == null) {
            return value;
        }
        String decimalsSubtractedToNerve = heterogeneousAsset.getDecimalsSubtractedToNerve();
        if (StringUtils.isBlank(decimalsSubtractedToNerve)) {
            return value;
        }
        BigInteger decimalsSubtracted = new BigInteger(decimalsSubtractedToNerve);
        if (decimalsSubtracted.compareTo(BigInteger.ZERO) < 0) {
            value = new BigDecimal(value).movePointRight(decimalsSubtracted.intValue()).toBigInteger();
        } else if (decimalsSubtracted.compareTo(BigInteger.ZERO) > 0) {
            value = new BigDecimal(value).movePointLeft(decimalsSubtracted.intValue()).toBigInteger();
        }
        return value;
    }

    @Override
    public void setCurrentHeterogeneousVersionII() {
        nerveChain.setCurrentHeterogeneousVersion(2);
    }

    @Override
    public boolean checkNetworkRunning(int hChainId) {
        // 101 eth, 102 bsc, 103 heco, 104 oec, 105 Harmony(ONE), 106 Polygon(MATIC), 107 kcc(KCS),
        // 108 TRX, 109 CRO, 110 AVAX, 111 AETH, 112 FTM, 113 METIS, 114 IOTX, 115 OETH, 116 KLAY, 117 BCH, 118 GoerliETH
        // 119 ENULS, 120 KAVA, 121 ETHW, 122 REI, 123 ZK
        /*skippedNetworks.add(101);
        skippedNetworks.add(103);
        skippedNetworks.add(105);
        skippedNetworks.addAll(List.of(106, 107, 109, 110));
        skippedNetworks.add(108);
        skippedNetworks.add(111);
        skippedNetworks.add(112);
        skippedNetworks.add(113);
        skippedNetworks.add(114);
        skippedNetworks.add(115);
        skippedNetworks.add(116);
        skippedNetworks.add(117);
        skippedNetworks.addAll(List.of(119, 120, 121, 122, 123));*/
        if (nerveChain.getChainId() == 5 && skippedTestNetworks.contains(hChainId)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isLocalSign() {
        return ConverterContext.SIG_MODE == ConverterConstant.SIG_MODE_LOCAL;
    }

    @Override
    public String signWithdrawByMachine(long nativeId, String signerPubkey, String txKey, String toAddress, BigInteger value, Boolean isContractAsset, String erc20, byte version) throws NulsException {
        Map<String, Object> extend = new HashMap<>();
        extend.put("method", "cvSignWithdraw");
        extend.put("nativeId", nativeId);
        extend.put("txKey", txKey);
        extend.put("toAddress", toAddress);
        extend.put("value", value.toString());
        extend.put("isContractAsset", isContractAsset);
        extend.put("erc20", erc20);
        extend.put("version", version);
        String hex = AccountCall.signature(nerveChain.getChainId(), AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(Numeric.cleanHexPrefix(signerPubkey), nerveChain.getChainId())), "pwd", "00", extend);
        return Numeric.cleanHexPrefix(hex);
    }

    @Override
    public String signChangeByMachine(long nativeId, String signerPubkey, String txKey, String[] adds, int count, String[] removes, byte version) throws NulsException {
        Map<String, Object> extend = new HashMap<>();
        extend.put("method", "cvSignChange");
        extend.put("nativeId", nativeId);
        extend.put("txKey", txKey);
        extend.put("adds", adds);
        extend.put("count", count);
        extend.put("removes", removes);
        extend.put("version", version);
        String hex = AccountCall.signature(nerveChain.getChainId(), AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(Numeric.cleanHexPrefix(signerPubkey), nerveChain.getChainId())), "pwd", "00", extend);
        return Numeric.cleanHexPrefix(hex);
    }

    @Override
    public String signUpgradeByMachine(long nativeId, String signerPubkey, String txKey, String upgradeContract, byte version) throws NulsException {
        Map<String, Object> extend = new HashMap<>();
        extend.put("method", "cvSignUpgrade");
        extend.put("nativeId", nativeId);
        extend.put("txKey", txKey);
        extend.put("upgradeContract", upgradeContract);
        extend.put("version", version);
        String hex = AccountCall.signature(nerveChain.getChainId(), AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(Numeric.cleanHexPrefix(signerPubkey), nerveChain.getChainId())), "pwd", "00", extend);
        return Numeric.cleanHexPrefix(hex);
    }

    @Override
    public String signRawTransactionByMachine(long nativeId, String signerPubkey, String from, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String to, BigInteger value, String data) throws Exception {
        Map<String, Object> extend = new HashMap<>();
        extend.put("method", "cvSignRawTransaction");
        extend.put("nativeId", nativeId);
        extend.put("from", from);
        extend.put("nonce", nonce.toString());
        extend.put("gasPrice", gasPrice.toString());
        extend.put("gasLimit", gasLimit.toString());
        extend.put("to", to);
        extend.put("value", value.toString());
        extend.put("data", data);
        String txHex = AccountCall.signature(nerveChain.getChainId(), AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(Numeric.cleanHexPrefix(signerPubkey), nerveChain.getChainId())), "pwd", "00", extend);
        return Numeric.prependHexPrefix(txHex);
    }

    @Override
    public String signTronRawTransactionByMachine(String signerPubkey, String txStr) throws Exception {
        Map<String, Object> extend = new HashMap<>();
        extend.put("method", "cvSignTronRawTransaction");
        extend.put("tx", txStr);
        return AccountCall.signature(nerveChain.getChainId(), AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(Numeric.cleanHexPrefix(signerPubkey), nerveChain.getChainId())), "pwd", "00", extend);
    }

    @Override
    public void addChainDBName(int hChainId, String dbName) {
        chainDBNameMap.put(hChainId, dbName);
    }

    @Override
    public Map<Integer, String> chainDBNameMap() {
        return chainDBNameMap;
    }

    @Override
    public void setDbMergedStatus(int hChainid) {
        dbMergedChainMap.put(hChainid, true);
    }

    @Override
    public boolean isDbMerged(int hChainid) {
        Boolean merged = dbMergedChainMap.get(hChainid);
        if (merged == null) {
            return false;
        }
        return merged;
    }

    @Override
    public String mergedDBName() {
        return ConverterDBConstant.DB_HETEROGENEOUS_CHAIN;
    }

    @Override
    public BigInteger getL1Fee(int htgChainId) {
        if (!l1FeeChainSet.contains(htgChainId)) {
            return BigInteger.ZERO;
        }
        try {
            BigInteger ethGasPrice;
            if (nerveChain.getChainId() == 9) {
                ethGasPrice = heterogeneousDockingManager.getHeterogeneousDocking(101).currentGasPrice();
            } else {
                ethGasPrice = heterogeneousDockingManager.getHeterogeneousDocking(118).currentGasPrice();
                //TODO pierre test
                //ethGasPrice = new BigDecimal("0.127838726").movePointRight(9).toBigInteger();
            }
            return HeterogeneousUtil.getL1Fee(htgChainId, ethGasPrice);
        } catch (NulsException e) {
            return BigInteger.ZERO;
        }
    }

    @Override
    public boolean hasL1FeeOnChain(int htgChainId) {
        return l1FeeChainSet.contains(htgChainId);
    }

    public boolean isNerveMainnet() {
        return nerveChain.getChainId() == 9;
    }

    @Override
    public int getByzantineCount(Integer total) {
        return VirtualBankUtil.getByzantineCount(nerveChain, total);
    }

    @Override
    public Integer getSeedPackerOrder(String addr) {
        return seedPacker.get(addr);
    }

    @Override
    public Map<String, Integer> getSeedPacker() {
        return seedPacker;
    }

    @Override
    public Set<String> getAllPackers() {
        return nerveChain.getMapVirtualBank().keySet();
    }

    @Override
    public List<String> getAllPackerPubs() {
        return nerveChain.getMapVirtualBank().values().stream().map(v -> v.getSignAddrPubKey()).collect(Collectors.toList());
    }

    @Override
    public String getBtcFeeReceiverPub() {
        return btcFeeReceiverPub;
    }

    @Override
    public String getInitialBtcPubKeyList() {
        return converterConfig.getInitBtcPubKeyList();
    }

    @Override
    public String getInitialFchPubKeyList() {
        return converterConfig.getInitBtcPubKeyList();
    }

    @Override
    public HeterogeneousAssetInfo getHeterogeneousAssetByNerveAsset(int htgChainId, int nerveAssetChainId, int nerveAssetId) {
        HeterogeneousAssetInfo heterogeneousAsset = heterogeneousAssetHelper.getHeterogeneousAssetInfo(htgChainId, nerveAssetChainId, nerveAssetId);
        return heterogeneousAsset;
    }

    @Override
    public String signBtcWithdrawByMachine(long nativeId, int htgChainId, String signerPubkey, String txKey, String toAddress, long value, WithdrawalUTXO data) throws NulsException {
        WithdrawalUTXOTxData txData = new WithdrawalUTXOTxData(
                data.getNerveTxHash(),
                htgChainId,
                data.getCurrentMultiSignAddress(),
                data.getCurrenVirtualBankTotal(),
                data.getFeeRate(),
                data.getPubs(),
                data.getUtxoDataList());
        int n = data.getPubs().size();
        int m = this.getByzantineCount(n);
        Map<String, Object> extend = new HashMap<>();
        extend.put("method", "cvBitcoinSignWithdraw");
        extend.put("nativeId", nativeId);
        extend.put("signerPubkey", signerPubkey);
        extend.put("txKey", txKey);
        extend.put("toAddress", toAddress);
        extend.put("value", String.valueOf(value));
        extend.put("m", String.valueOf(m));
        extend.put("n", String.valueOf(n));
        extend.put("mainnet", this.isNerveMainnet());
        try {
            extend.put("txData", HexUtil.encode(txData.serialize()));
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.IO_ERROR, e);
        }
        String hex = AccountCall.signature(nerveChain.getChainId(), AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(Numeric.cleanHexPrefix(signerPubkey), nerveChain.getChainId())), "pwd", "00", extend);
        return Numeric.cleanHexPrefix(hex);
    }

    @Override
    public String signBtcChangeByMachine(long nativeId, int htgChainId, String signerPubkey, String txKey, String toAddress, long value, WithdrawalUTXO data) throws NulsException {
        WithdrawalUTXOTxData txData = new WithdrawalUTXOTxData(
                data.getNerveTxHash(),
                htgChainId,
                data.getCurrentMultiSignAddress(),
                data.getCurrenVirtualBankTotal(),
                data.getFeeRate(),
                data.getPubs(),
                data.getUtxoDataList());
        int n = data.getPubs().size();
        int m = this.getByzantineCount(n);
        Map<String, Object> extend = new HashMap<>();
        extend.put("method", "cvBitcoinSignChange");
        extend.put("nativeId", nativeId);
        extend.put("signerPubkey", signerPubkey);
        extend.put("txKey", txKey);
        extend.put("toAddress", toAddress);
        extend.put("value", String.valueOf(value));
        extend.put("m", String.valueOf(m));
        extend.put("n", String.valueOf(n));
        extend.put("mainnet", this.isNerveMainnet());
        try {
            extend.put("txData", HexUtil.encode(txData.serialize()));
        } catch (IOException e) {
            throw new NulsException(ConverterErrorCode.IO_ERROR, e);
        }
        String hex = AccountCall.signature(nerveChain.getChainId(), AddressTool.getStringAddressByBytes(AddressTool.getAddressByPubKeyStr(Numeric.cleanHexPrefix(signerPubkey), nerveChain.getChainId())), "pwd", "00", extend);
        return Numeric.cleanHexPrefix(hex);
    }

    @Override
    public void updateMultySignAddress(int heterogeneousChainId, String multySignAddress) throws NulsException {
        if (heterogeneousChainId < 200) {
            throw new RuntimeException("not support chain");
        }
        heterogeneousChainManager.updateMultySignAddress(heterogeneousChainId, multySignAddress);
    }

    @Override
    public boolean checkChangeP35(String nerveTxHash) {
        if (!isNerveMainnet()) {
            return false;
        }
        if (!isProtocol35()) {
            return false;
        }
        if ("6ef994439924c868bfd98f15f154c8280e7b25905510d80ae763c562d615e991".equals(nerveTxHash)) {
            return true;
        }
        return false;
    }

    @Override
    public String[] inChangeP35() {
        return new String[]{"0xa7ad2bc3ea1b68f1e372ae1d5e303c17d9ed9951", "0x48ee8759515e0c703691140e63725f39acd3f7d5"};
    }

    @Override
    public String[] outChangeP35() {
        return new String[]{"0x7c4b783a0101359590e6153df3b58c7fe24ea468", "0x5fbf7793196efbf7066d99fa29dc64dc23052451"};
    }

    @Override
    public void closeHtgChain(int htgChainId) throws Exception {
        heterogeneousChainManager.removeHeterogeneousChain(htgChainId);
        heterogeneousChainInfoStorageService.markChainClosed(htgChainId);
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.removeHeterogeneousDocking(htgChainId);
        if (docking != null) {
            docking.closeChainPending();
            docking.closeChainConfirm();
        }
        Map<String, VirtualBankDirector> mapVirtualBank = nerveChain.getMapVirtualBank();
        mapVirtualBank.entrySet().forEach(e -> {
            VirtualBankDirector director = e.getValue();
            director.getHeterogeneousAddrMap().remove(htgChainId);
            virtualBankStorageService.update(nerveChain, director);
        });

    }

}

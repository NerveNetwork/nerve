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
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.info.Constants;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.business.VirtualBankService;
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
import network.nerve.converter.rpc.call.LedgerCall;
import network.nerve.converter.rpc.call.QuotationCall;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.ComponentSignStorageService;
import network.nerve.converter.storage.ConfirmWithdrawalStorageService;
import network.nerve.converter.utils.ConverterUtil;

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
    private ConverterConfig converterConfig;

    private Map<Integer, NerveAssetInfo> htgMainAssetMap = new HashMap<>(16);
    private Map<NerveAssetInfo, AssetName> htgMainAssetByNerveMap = new HashMap<>(16);
    private Set<String> skipTransactions = new HashSet<>();
    private volatile List<Runnable> htgConfirmTxHandlers = new ArrayList<>();
    private volatile List<Runnable> htgRpcAvailableHandlers = new ArrayList<>();
    private volatile List<Runnable> htgWaitingTxInvokeDataHandlers = new ArrayList<>();

    Set<Integer> skippedNetworks;

    public ConverterCoreApi() {
        skippedNetworks = new HashSet<>();
        skippedNetworks.add(105);
        skippedNetworks.add(111);
        skippedNetworks.add(112);
        skippedNetworks.add(113);
        skippedNetworks.add(115);
        skippedNetworks.add(117);
    }

    public NulsLogger logger() {
        return nerveChain.getLogger();
    }

    public void setNerveChain(Chain nerveChain) {
        this.nerveChain = nerveChain;
        // 跳过历史遗留的问题交易
        if (nerveChain.getChainId() == 9) {
            skipTransactions.add("b0a3f4e0f7f28b6d55ced8f333e63f0844c25f061b8a843f8c49c6a0612ccd8d");
        }
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
            logger().error("查询节点状态失败", e);
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
            logger().error("查询节点状态失败", e);
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
            logger().error("查询节点状态失败", e);
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
     * 返回指定异构链下的当前虚拟银行列表和顺序
     * 当异构链余额低于0.04时，顺序挪到末尾
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
                // 当异构链余额低于0.04时，顺序挪到末尾
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
        nerveChain.getLogger().debug("当前银行顺序: {}", resultMap);
        return resultMap;
    }


    /**
     * 重新索要拜占庭签名
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
          从新存储中获取资产是否为绑定异构链资产, 否则从账本获取资产类型做判断
               大于4且小于9时，则是绑定资产（5~8是纯绑定类型资产）
               当资产类型为9时，由于原生资产类型4是异构注册资产，从支持9类型资产前，4类型资产不存在绑定资产，则说明该异构资产不是绑定类型（若是绑定资产，则新存储中一定存储了它是绑定资产）
               9类型资产，有一个网络是异构注册类型，其他网络是绑定类型
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
        // 根据NERVE提现交易txHash返回总提供的手续费
        return assembleTxService.calculateWithdrawalTotalFee(nerveChain, this.getNerveTx(nerveTxHash));
    }

    @Override
    public BigDecimal getUsdtPriceByAsset(AssetName assetName) {
        String key = ConverterContext.priceKeyMap.get(assetName.name());
        if (key == null) {
            return BigDecimal.ZERO;
        }
        // 获取指定资产的USDT价格
        return QuotationCall.getPriceByOracleKey(nerveChain, key);
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
        // v1.14.0 协议升级后，地址校验增加验证逻辑，换成字节数组后，重新生成base58地址后，再和用户提供的base58地址相比，避免地址前缀不同的问题
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

    private void loadHtgMainAsset() {
        if (heterogeneousDockingManager.getAllHeterogeneousDocking().size() == htgMainAssetMap.size()) return;
        AssetName[] values = AssetName.values();
        for (AssetName assetName : values) {
            int htgChainId = assetName.chainId();
            if (htgChainId < 101) continue;
            this.getHtgMainAsset(htgChainId);
        }
    }
    /**
     * 获取异构链主资产(注册到NERVE网络上的资产信息)
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
        // 加载缓存
        this.loadHtgMainAsset();
        // 检查是否为异构链主资产
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

    private AssetName getHtgMainAssetName(int nerveAssetChainId, int nerveAssetId) {
        // 加载缓存
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
        // 跨链资产精度不同，换算精度
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
        // 跨链资产精度不同，换算精度
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
        //skippedNetworks.add(103);
        //skippedNetworks.add(109);
        if (nerveChain.getChainId() == 5 && skippedNetworks.contains(hChainId)) {
            return false;
        }
        return true;
    }
}

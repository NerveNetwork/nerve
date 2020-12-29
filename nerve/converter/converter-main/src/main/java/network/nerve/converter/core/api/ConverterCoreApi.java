/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.interfaces.IConverterCoreApi;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.helper.LedgerAssetRegisterHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.message.ComponentSignMessage;
import network.nerve.converter.model.HeterogeneousSign;
import network.nerve.converter.model.bo.*;
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
import java.util.*;

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



    private NulsLogger logger() {
        return nerveChain.getLogger();
    }

    public void setNerveChain(Chain nerveChain) {
        this.nerveChain = nerveChain;
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

    /**
     * 返回指定异构链下的当前虚拟银行列表和顺序
     */
    @Override
    public Map<String, Integer> currentVirtualBanks(int hChainId) {
        Map<String, Integer> resultMap = new HashMap<>();
        Map<String, VirtualBankDirector> map = nerveChain.getMapVirtualBank();
        map.entrySet().stream().forEach(e -> {
            VirtualBankDirector director = e.getValue();
            HeterogeneousAddress address = director.getHeterogeneousAddrMap().get(hChainId);
            if (address != null) {
                resultMap.put(address.getAddress(), director.getOrder());
            }
        });
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
    public BigDecimal getFeeOfWithdrawTransaction(String nerveTxHash) throws NulsException {
        // 根据NERVE提现交易txHash返回总提供的NVT手续费
        return new BigDecimal(assembleTxService.calculateWithdrawalTotalFee(nerveChain, this.getNerveTx(nerveTxHash)));
    }

    @Override
    public BigDecimal getUsdtPriceByAsset(AssetName assetName) {
        // 获取指定资产的USDT价格
        switch (assetName) {
            case NVT: return QuotationCall.getPriceByOracleKey(nerveChain, ConverterConstant.ORACLE_KEY_NVT_PRICE);
            case ETH: return QuotationCall.getPriceByOracleKey(nerveChain, ConverterConstant.ORACLE_KEY_ETH_PRICE);
            case BNB: return QuotationCall.getPriceByOracleKey(nerveChain, ConverterConstant.ORACLE_KEY_BNB_PRICE);
            case HT: return QuotationCall.getPriceByOracleKey(nerveChain, ConverterConstant.ORACLE_KEY_HT_PRICE);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public boolean isSupportNewMechanismOfWithdrawalFee() {
        return nerveChain.getLatestBasicBlock().getHeight() >= ConverterContext.FEE_ADDITIONAL_HEIGHT;
    }
}

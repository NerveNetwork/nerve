package network.nerve.dex.tx;

import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.ModuleTxPackageProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.model.ModuleE;
import network.nerve.dex.context.DexErrorCode;
import network.nerve.dex.manager.DexService;
import network.nerve.dex.util.LoggerUtil;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.util.*;

@Component(ModuleE.Constant.DEX)
public class DexTxPackageProcessor implements ModuleTxPackageProcessor {

    @Autowired
    private DexService dexService;

    @Override
    public Map<String, List<String>> packProduce(int chainId, List<Transaction> txs, int process, long height, long blockTime) throws NulsException {

        try {
            if (process == 0) {
                Map<String, List<Transaction>> map = dexService.doPacking(txs, blockTime, height, false);
                List<Transaction> txList = map.get("dealTxList");
                List<String> newlyList = new ArrayList<>();
                List<String> rmHashList = new ArrayList<>();
                if (txList.size() > 0) {
                    for (int i = 0; i < txList.size(); i++) {
                        newlyList.add(Hex.toHexString(txList.get(i).serialize()));
                    }
                }

                txList = map.get("removeTxList");
                if (txList.size() > 0) {
                    for (int i = 0; i < txList.size(); i++) {
                        rmHashList.add(txList.get(i).getHash().toHex());
                    }
                }

                Map<String, List<String>> packMap = new HashMap<>();
                packMap.put("newlyList", newlyList);
                packMap.put("rmHashList", rmHashList);

                return packMap;
            } else {
                //批量验证本模块所有交易
                validateTxs(txs, blockTime, height);
                return null;
            }
        } catch (IOException e) {
            LoggerUtil.dexLog.error(e);
            throw new NulsException(DexErrorCode.FAILED);
        }
    }

    /**
     * 同步区块时，批量验证本模块所有交易
     * 这里的验证只针对由委托单生成的成交记录，如果验证生成的成交记录和
     * 验证逻辑：
     * 1.将区块打包的成交交易放入一个集合
     * 2.根据区块打包的挂单交易生成成交交易放入另外一个集合
     * 3.比对两个集合里的成交交易是否一致
     *
     * @param txs
     */
    private void validateTxs(List<Transaction> txs, long blockTime, long height) throws NulsException {
        List<Transaction> dealTxList1 = new ArrayList<>();
        List<Transaction> dealTxList2;
        //如果本次打包的txs长度为0，说明此次打包的块是空块
        //出现打包空块很有可能是因为打包时间不够而导致，所以不对空块做成交交易进行匹配验证
        boolean isEmpty = false;
        if (txs.isEmpty()) {
            isEmpty = true;
        }

        Transaction tx;
        for (int i = 0; i < txs.size(); i++) {
            tx = txs.get(i);
            if (tx.getType() == TxType.TRADING_DEAL) {
                dealTxList1.add(tx);
                txs.remove(i);
                i--;
            } else if (tx.getType() == TxType.ORDER_CANCEL_CONFIRM) {
                dealTxList1.add(tx);
                txs.remove(i);
                i--;
            }
        }
        //根据区块打包的挂单交易，生成成交交易

        Map<String, List<Transaction>> map = dexService.doPacking(txs, blockTime, height, true);

        List<Transaction> removeTxList = map.get("removeTxList");
        if (!removeTxList.isEmpty()) {
            LoggerUtil.dexLog.error("--------batch validate dexTxs fail, have removeTxList ------------");
            LoggerUtil.dexLog.error("-------- 区块高度：" + height);
            for (Transaction x : removeTxList) {
                LoggerUtil.dexLog.error("--------txHash:" + x.getHash().toHex() + ",type:" + x.getType());
            }
            throw new NulsException(DexErrorCode.SYNC_BATCH_VALIDATE_ERROR);
        }
        dealTxList2 = map.get("dealTxList");

        //比对成交交易是否一致,如果本次打包的是空块，则不做此验证
        if (dealTxList1.size() != dealTxList2.size() && !isEmpty) {
            LoggerUtil.dexLog.error("--------dealTxList validate fail 比对长度不一致 ------------");
            LoggerUtil.dexLog.error("-------- 区块高度：" + height);
            LoggerUtil.dexLog.error("--------dealTxList1.size:{}, dealTxList1.size:{}", dealTxList1.size(), dealTxList2.size());

            LoggerUtil.dexLog.error("--------dealTxList1 from ------------");
            for (Transaction tx1 : dealTxList1) {
                for (CoinFrom from : tx1.getCoinDataInstance().getFrom()) {
                    LoggerUtil.dexLog.error(from.toString());
                }
            }
            LoggerUtil.dexLog.error("--------dealTxList1 to ------------");
            for (Transaction tx1 : dealTxList1) {
                for (CoinTo to : tx1.getCoinDataInstance().getTo()) {
                    LoggerUtil.dexLog.error(to.toString());
                }
            }
            LoggerUtil.dexLog.error("---------------- ------------");
            LoggerUtil.dexLog.error("--------dealTxList2 from ------------");
            for (Transaction tx1 : dealTxList2) {
                for (CoinFrom from : tx1.getCoinDataInstance().getFrom()) {
                    LoggerUtil.dexLog.error(from.toString());
                }
            }
            LoggerUtil.dexLog.error("--------dealTxList2 to ------------");
            for (Transaction tx1 : dealTxList2) {
                for (CoinTo to : tx1.getCoinDataInstance().getTo()) {
                    LoggerUtil.dexLog.error(to.toString());
                }
            }
            throw new NulsException(DexErrorCode.SYNC_BATCH_VALIDATE_ERROR);
        }

        if (dealTxList1.size() == dealTxList2.size()) {
            for (int i = 0; i < dealTxList1.size(); i++) {
                if (!equals(dealTxList1.get(i), dealTxList2.get(i))) {
                    LoggerUtil.dexLog.error("--------dealTxList validate fail 金额不一致 ------------");
                    LoggerUtil.dexLog.error("-------- 区块高度：" + height);

                    LoggerUtil.dexLog.error("--------dealTxList1 from ------------");
                    for (Transaction tx1 : dealTxList1) {
                        for (CoinFrom from : tx1.getCoinDataInstance().getFrom()) {
                            LoggerUtil.dexLog.error(from.toString());
                        }
                    }

                    LoggerUtil.dexLog.error("--------dealTxList1 to ------------");
                    for (Transaction tx1 : dealTxList1) {
                        for (CoinTo to : tx1.getCoinDataInstance().getTo()) {
                            LoggerUtil.dexLog.error(to.toString());
                        }
                    }
                    LoggerUtil.dexLog.error("---------------- ------------");
                    LoggerUtil.dexLog.error("--------dealTxList2 from ------------");
                    for (Transaction tx1 : dealTxList2) {
                        for (CoinFrom from : tx1.getCoinDataInstance().getFrom()) {
                            LoggerUtil.dexLog.error(from.toString());
                        }
                    }
                    LoggerUtil.dexLog.error("--------dealTxList2 to ------------");
                    for (Transaction tx1 : dealTxList2) {
                        for (CoinTo to : tx1.getCoinDataInstance().getTo()) {
                            LoggerUtil.dexLog.error(to.toString());
                        }
                    }
                    throw new NulsException(DexErrorCode.SYNC_BATCH_VALIDATE_ERROR);
                }
            }
        }
    }

    /**
     * 验证两个成交交易是否一致
     *
     * @param tx1
     * @param tx2
     * @return
     */
    private boolean equals(Transaction tx1, Transaction tx2) {
        if (!Arrays.equals(tx1.getTxData(), tx2.getTxData())) {
            return false;
        }
        if (!Arrays.equals(tx1.getCoinData(), tx2.getCoinData())) {
            return false;
        }
        return true;
    }

    @Override
    public String getModuleCode() {
        return ModuleE.DX.abbr;
    }
}

package network.nerve.dex.tx.v1.process;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import network.nerve.dex.context.DexConfig;
import network.nerve.dex.manager.DexManager;
import network.nerve.dex.manager.TradingContainer;
import network.nerve.dex.model.po.CoinTradingEditInfoPo;
import network.nerve.dex.model.po.CoinTradingPo;
import network.nerve.dex.model.po.EditCoinTradingPo;
import network.nerve.dex.model.txData.EditCoinTrading;
import network.nerve.dex.storage.CoinTradingStorageService;
import network.nerve.dex.tx.v1.validate.EditCoinTradingValidator;
import network.nerve.dex.util.LoggerUtil;

import java.util.*;

@Component("editCoinTradingProcessorV1")
public class EditCoinTradingProcessor implements TransactionProcessor {
    @Autowired
    private DexConfig dexConfig;
    @Autowired
    private DexManager dexManager;
    @Autowired
    private EditCoinTradingValidator editCoinTradingValidator;
    @Autowired
    private CoinTradingStorageService coinTradingStorageService;

    @Override
    public int getType() {
        return TxType.EDIT_COIN_TRADING;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> map, BlockHeader blockHeader) {
        return editCoinTradingValidator.validateTxs(txs);
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return true;
    }

    public void editCoinTradingCommit(Transaction tx) {
        try {
            EditCoinTrading c = new EditCoinTrading();
            c.parse(new NulsByteBuffer(tx.getTxData()));
            TradingContainer container = dexManager.getTradingContainer(c.getTradingHash().toHex());
            CoinTradingPo tradingPo = container.getCoinTrading();

            tradingPo.setScaleQuoteDecimal(c.getScaleQuoteDecimal());
            tradingPo.setScaleBaseDecimal(c.getScaleBaseDecimal());
            tradingPo.setMinBaseAmount(c.getMinBaseAmount());
            tradingPo.setMinQuoteAmount(c.getMinQuoteAmount());

            //再添加一条修改记录，供回滚使用
            EditCoinTradingPo editCoinTradingPo = new EditCoinTradingPo();
            editCoinTradingPo.setTxHash(tx.getHash());
            editCoinTradingPo.setMinBaseAmount(c.getMinBaseAmount());
            editCoinTradingPo.setMinQuoteAmount(c.getMinQuoteAmount());
            editCoinTradingPo.setScaleBaseDecimal(c.getScaleBaseDecimal());
            editCoinTradingPo.setScaleQuoteDecimal(c.getScaleQuoteDecimal());

            CoinTradingEditInfoPo editInfoPo = coinTradingStorageService.queryEditInfoPo(tradingPo.getHash());
            editInfoPo.getEditCoinTradingList().addLast(editCoinTradingPo);

            coinTradingStorageService.save(tradingPo);
            coinTradingStorageService.saveEditInfo(tradingPo.getHash(), editInfoPo);

        } catch (NulsException e) {
            LoggerUtil.dexLog.error("Failure to CoinTrading commit, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.dexLog.error("Failure to CoinTrading commit, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e);
        }
    }

    public void editCoinTradingRollback(Transaction tx) {
        try {
            EditCoinTrading c = new EditCoinTrading();
            c.parse(new NulsByteBuffer(tx.getTxData()));

            TradingContainer container = dexManager.getTradingContainer(c.getTradingHash().toHex());
            CoinTradingPo tradingPo = container.getCoinTrading();

            //查询历史修改记录
            CoinTradingEditInfoPo editInfoPo = coinTradingStorageService.queryEditInfoPo(tradingPo.getHash());
            //取出最后一条记录
            EditCoinTradingPo editCoinTradingPo = editInfoPo.getEditCoinTradingList().getLast();
            if (editCoinTradingPo.getTxHash().equals(tx.getHash())) {
                //如果hash一致，删除这条记录
                editInfoPo.getEditCoinTradingList().removeLast();
                coinTradingStorageService.saveEditInfo(tradingPo.getHash(), editInfoPo);
            }
            //再次取出最后一条记录，还原CoinTradingPo
            editCoinTradingPo = editInfoPo.getEditCoinTradingList().getLast();
            tradingPo.setMinQuoteAmount(editCoinTradingPo.getMinQuoteAmount());
            tradingPo.setMinBaseAmount(editCoinTradingPo.getMinBaseAmount());
            tradingPo.setScaleBaseDecimal(editCoinTradingPo.getScaleBaseDecimal());
            tradingPo.setScaleQuoteDecimal(editCoinTradingPo.getScaleQuoteDecimal());

            coinTradingStorageService.save(tradingPo);
        } catch (NulsException e) {
            LoggerUtil.dexLog.error("Failure to CoinTrading commit, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e.getErrorCode());
        } catch (Exception e) {
            LoggerUtil.dexLog.error("Failure to CoinTrading commit, hash:" + tx.getHash().toHex());
            LoggerUtil.dexLog.error(e);
            throw new NulsRuntimeException(e);
        }

    }

    @Override
    public int getPriority() {
        return 5;
    }
}

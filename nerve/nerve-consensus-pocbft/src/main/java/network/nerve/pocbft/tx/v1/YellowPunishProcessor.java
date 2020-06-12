package network.nerve.pocbft.tx.v1;

import io.nuls.base.data.BlockExtendsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.data.YellowPunishData;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.po.PunishLogPo;
import network.nerve.pocbft.storage.PunishStorageService;
import network.nerve.pocbft.utils.LoggerUtil;
import network.nerve.pocbft.utils.enumeration.PunishType;
import network.nerve.pocbft.utils.manager.ChainManager;
import network.nerve.pocbft.utils.manager.PunishManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 黄牌交易处理器
 *
 * @author tag
 * @date 2019/6/1
 */
@Component("YellowPunishProcessorV1")
public class YellowPunishProcessor implements TransactionProcessor {
    @Autowired
    private PunishManager punishManager;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private PunishStorageService punishStorageService;

    @Override
    public int getType() {
        return TxType.YELLOW_PUNISH;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        return null;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            LoggerUtil.commonLog.error("Chains do not exist.");
            return false;
        }
        List<Transaction> commitSuccessList = new ArrayList<>();
        boolean commitResult = true;
        for (Transaction tx : txs) {
            if (yellowPunishCommit(tx, chain, blockHeader)) {
                commitSuccessList.add(tx);
            } else {
                commitResult = false;
                break;
            }
        }
        //回滚已提交成功的交易
        if (!commitResult) {
            for (Transaction rollbackTx : commitSuccessList) {
                yellowPunishRollback(rollbackTx, chain, blockHeader);
            }
        }
        return commitResult;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            LoggerUtil.commonLog.error("Chains do not exist.");
            return false;
        }
        List<Transaction> rollbackSuccessList = new ArrayList<>();
        boolean rollbackResult = true;
        for (Transaction tx : txs) {
            if (yellowPunishRollback(tx, chain, blockHeader)) {
                rollbackSuccessList.add(tx);
            } else {
                rollbackResult = false;
                break;
            }
        }
        //保存已回滚成功的交易
        if (!rollbackResult) {
            for (Transaction commitTx : rollbackSuccessList) {
                yellowPunishCommit(commitTx, chain, blockHeader);
            }
        }
        return rollbackResult;
    }

    private boolean yellowPunishCommit(Transaction tx, Chain chain, BlockHeader blockHeader) {
        YellowPunishData punishData = new YellowPunishData();
        try {
            punishData.parse(tx.getTxData(), 0);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }
        BlockExtendsData roundData = blockHeader.getExtendsData();
        List<PunishLogPo> savedList = new ArrayList<>();
        int index = 1;
        int chainId = chain.getConfig().getChainId();
        for (byte[] address : punishData.getAddressList()) {
            PunishLogPo po = new PunishLogPo();
            po.setAddress(address);
            po.setHeight(blockHeader.getHeight());
            po.setRoundIndex(roundData.getRoundIndex());
            po.setTime(tx.getTime());
            po.setIndex(index++);
            po.setType(PunishType.YELLOW.getCode());
            boolean result = punishStorageService.save(po, chainId);
            if (!result) {
                for (PunishLogPo punishLogPo : savedList) {
                    punishStorageService.delete(punishManager.getPoKey(punishLogPo.getAddress(), PunishType.YELLOW.getCode(), punishLogPo.getHeight(), punishLogPo.getIndex()), chainId);
                }
                savedList.clear();
                chain.getLogger().error("Data save error!");
                return false;
            } else {
                savedList.add(po);
            }
        }
        chain.getYellowPunishList().addAll(savedList);
        return true;
    }

    private boolean yellowPunishRollback(Transaction tx, Chain chain, BlockHeader blockHeader) {
        long blockHeight = blockHeader.getHeight();
        YellowPunishData punishData = new YellowPunishData();
        try {
            punishData.parse(tx.getTxData(), 0);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }
        List<PunishLogPo> deletedList = new ArrayList<>();
        BlockExtendsData roundData = blockHeader.getExtendsData();
        int deleteIndex = 1;
        int chainId = chain.getConfig().getChainId();
        for (byte[] address : punishData.getAddressList()) {
            boolean result = punishStorageService.delete(punishManager.getPoKey(address, PunishType.YELLOW.getCode(), blockHeight, deleteIndex), chainId);
            if (!result) {
                for (PunishLogPo po : deletedList) {
                    punishStorageService.save(po, chainId);
                }
                deletedList.clear();
                chain.getLogger().error("Data save error!");
                return false;
            } else {
                PunishLogPo po = new PunishLogPo();
                po.setAddress(address);
                po.setHeight(blockHeight);
                po.setRoundIndex(roundData.getRoundIndex());
                po.setTime(tx.getTime());
                po.setIndex(deleteIndex);
                po.setType(PunishType.YELLOW.getCode());
                deletedList.add(po);
            }
            deleteIndex++;
        }
        chain.getYellowPunishList().removeAll(deletedList);
        return true;
    }
}

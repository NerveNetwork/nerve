package io.nuls.consensus.tx.v1;

import io.nuls.base.data.*;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.common.ConfigBean;
import io.nuls.consensus.constant.ConsensusErrorCode;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.utils.LoggerUtil;
import io.nuls.consensus.utils.manager.ChainManager;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.ArraysTool;
import io.nuls.transaction.model.po.TransactionConfirmedPO;
import io.nuls.transaction.service.ConfirmedTxService;

import java.math.BigInteger;
import java.util.*;

import static io.nuls.consensus.constant.ConsensusConstant.CONSENSUS_LOCK_TIME;

/**
 * quitstakingTransaction processor
 *
 * @author tag
 * @date 2019/6/1
 */
@Component("UnlockTransferProcessorV1")
public class UnlockTransferProcessor implements TransactionProcessor {
    @Autowired
    private ChainManager chainManager;

    @Autowired
    private ConfirmedTxService txService;

    @Override
    public int getType() {
        return TxType.UNLOCK_TRANSFER;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        Chain chain = chainManager.getChainMap().get(chainId);
        Map<String, Object> result = new HashMap<>(2);
        if (chain == null) {
            LoggerUtil.commonLog.error("Chains do not exist.");
            result.put("txList", txs);
            result.put("errorCode", ConsensusErrorCode.CHAIN_NOT_EXIST.getCode());
            return result;
        }
        List<Transaction> invalidTxList = new ArrayList<>();
        String errorCode = null;
        Set<NulsHash> hashSet = new HashSet<>();
        Result rs;
        for (Transaction unlockTx : txs) {
            try {
                NulsHash lockHash = new NulsHash(unlockTx.getTxData());
                if (null == lockHash) {
                    invalidTxList.add(unlockTx);
                    chain.getLogger().error("Unlock tx data is wrong:{}", unlockTx.getHash().toHex());
                    errorCode = ConsensusErrorCode.UNLOCK_TX_NOT_FOUND.getCode();
                    continue;
                }
                io.nuls.transaction.model.bo.Chain txChain = new io.nuls.transaction.model.bo.Chain();
                txChain.setConfig(new ConfigBean());
                txChain.getConfig().setChainId(chain.getChainId());
                TransactionConfirmedPO txPo = this.txService.getConfirmedTransaction(txChain, lockHash);
                if (null == txPo || null == txPo.getTx() || TxType.TRANSFER != txPo.getTx().getType()) {
                    invalidTxList.add(unlockTx);
                    chain.getLogger().error("Unlock tx not found:{}", lockHash.toHex());
                    errorCode = ConsensusErrorCode.LOCKED_TX_NOT_FOUND.getCode();
                    continue;
                }

                CoinData lockCoinData = txPo.getTx().getCoinDataInstance();
                if (null == lockCoinData || lockCoinData.getTo() == null || lockCoinData.getTo().size() != 1) {
                    invalidTxList.add(unlockTx);
                    chain.getLogger().error("Unlock tx coin data is null:{}", lockHash.toHex());
                    errorCode = ConsensusErrorCode.LOCKED_TX_NOT_FOUND.getCode();
                    continue;
                }
                CoinTo lockTo = lockCoinData.getTo().get(0);
                if (null == lockTo || lockTo.getLockTime() != CONSENSUS_LOCK_TIME) {
                    invalidTxList.add(unlockTx);
                    chain.getLogger().error("Unlock tx coin data is null:{}", lockHash.toHex());
                    errorCode = ConsensusErrorCode.LOCKED_TX_NOT_FOUND.getCode();
                    continue;
                }
                int lockedAssetChainId = lockTo.getAssetsChainId();
                int lockedAssetId = lockTo.getAssetsId();
                BigInteger lockedAmount = lockTo.getAmount();
                byte[] lockedAddress = lockTo.getAddress();

                CoinData coinData = unlockTx.getCoinDataInstance();
                if (null == coinData || null == coinData.getTo() || null == coinData.getFrom() || coinData.getFrom().size() != 1 || coinData.getTo().size() != 1) {
                    invalidTxList.add(unlockTx);
                    chain.getLogger().error("Unlock tx coin data is wrong:{}", unlockTx.getHash().toHex());
                    errorCode = ConsensusErrorCode.UNLOCK_TX_NOT_FOUND.getCode();
                    continue;
                }

                CoinFrom from = coinData.getFrom().get(0);
                if (null == from || from.getAssetsChainId() != lockedAssetChainId || from.getAssetsId() != lockedAssetId || from.getAmount().compareTo(lockedAmount) != 0 || from.getLocked() != CONSENSUS_LOCK_TIME || !ArraysTool.arrayEquals(lockedAddress, from.getAddress())) {
                    invalidTxList.add(unlockTx);
                    chain.getLogger().error("Unlock tx coin data is wrong:{}", unlockTx.getHash().toHex());
                    errorCode = ConsensusErrorCode.UNLOCK_TX_NOT_FOUND.getCode();
                    continue;
                }

                CoinTo to = coinData.getTo().get(0);
                if (null == to || to.getAssetsChainId() != lockedAssetChainId || to.getAssetsId() != lockedAssetId || to.getAmount().compareTo(lockedAmount) != 0 || to.getLockTime() != 0 || !ArraysTool.arrayEquals(lockedAddress, to.getAddress())) {
                    invalidTxList.add(unlockTx);
                    chain.getLogger().error("Unlock tx coin data is wrong:{}", unlockTx.getHash().toHex());
                    errorCode = ConsensusErrorCode.UNLOCK_TX_NOT_FOUND.getCode();
                    continue;
                }

                /*
                 * Repeated exit node
                 * */
                if (!hashSet.add(lockHash)) {
                    invalidTxList.add(unlockTx);
                    chain.getLogger().info("Repeated transactions");
                    errorCode = ConsensusErrorCode.CONFLICT_ERROR.getCode();
                }
            } catch (NulsException e) {
                invalidTxList.add(unlockTx);
                chain.getLogger().error("Conflict calibration error");
                chain.getLogger().error(e);
                errorCode = e.getErrorCode().getCode();
            }
        }
        result.put("txList", invalidTxList);
        result.put("errorCode", errorCode);
        return result;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return true;
    }

}

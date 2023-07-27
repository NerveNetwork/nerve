package io.nuls.consensus.tx.v1;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.tx.txdata.BatchStakingMerge;
import io.nuls.consensus.model.bo.tx.txdata.Deposit;
import io.nuls.consensus.utils.LoggerUtil;
import io.nuls.consensus.utils.manager.ChainManager;
import io.nuls.consensus.utils.manager.DepositManager;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.constant.ConsensusErrorCode;
import io.nuls.consensus.utils.validator.BatchMergeValidator;

import java.util.*;

/**
 * 退出staking交易处理器
 *
 * @author tag
 * @date 2019/6/1
 */
@Component("BatchStakingMergeProcessorV1")
public class BatchMergeProcessor implements TransactionProcessor {
    @Autowired
    private DepositManager depositManager;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private BatchMergeValidator validator;

    @Override
    public int getType() {
        return TxType.BATCH_STAKING_MERGE;
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
        for (Transaction batchMergeTx : txs) {
            try {
                //交易时间验证
                long time = NulsDateUtils.getCurrentTimeSeconds();
                if (blockHeader != null) {
                    time = blockHeader.getTime();
                }
                if (batchMergeTx.getTime() < time - ConsensusConstant.UNLOCK_TIME_DIFFERENCE_LIMIT || batchMergeTx.getTime() > time + ConsensusConstant.UNLOCK_TIME_DIFFERENCE_LIMIT) {
                    invalidTxList.add(batchMergeTx);
                    chain.getLogger().error("Trading time error,txTime:{},time:{}", batchMergeTx.getTime(), time);
                    errorCode = ConsensusErrorCode.ERROR_UNLOCK_TIME.getCode();
                    continue;
                }
                rs = validator.validate(chain, batchMergeTx, blockHeader);
                if (rs.isFailed()) {
                    invalidTxList.add(batchMergeTx);
                    chain.getLogger().error("Intelligent contract withdrawal delegation transaction verification failed");
                    errorCode = rs.getErrorCode().getCode();
                    continue;
                }
                BatchStakingMerge batchWithdraw = new BatchStakingMerge();
                batchWithdraw.parse(batchMergeTx.getTxData(), 0);
                /*
                 * 重复退出staking
                 * */
                for (NulsHash hash : batchWithdraw.getJoinTxHashList()) {
                    if (!hashSet.add(hash)) {
                        invalidTxList.add(batchMergeTx);
                        chain.getLogger().info("Repeated transactions");
                        errorCode = ConsensusErrorCode.CONFLICT_ERROR.getCode();
                    }
                }
            } catch (NulsException e) {
                chain.getLogger().error(e);
                invalidTxList.add(batchMergeTx);
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
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            LoggerUtil.commonLog.error("Chains do not exist.");
            return false;
        }
        List<Transaction> commitSuccessList = new ArrayList<>();
        boolean commitResult = true;
        for (Transaction tx : txs) {
            if (mergeCommit(tx, blockHeader, chain)) {
                commitSuccessList.add(tx);
            } else {
                commitResult = false;
                break;
            }
        }
        //回滚已提交成功的交易
        if (!commitResult) {
            for (Transaction rollbackTx : commitSuccessList) {
                mergeRollBack(rollbackTx, chain, blockHeader);
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
            if (mergeRollBack(tx, chain, blockHeader)) {
                rollbackSuccessList.add(tx);
            } else {
                rollbackResult = false;
                break;
            }
        }
        //保存已回滚成功的交易
        if (!rollbackResult) {
            for (Transaction commitTx : rollbackSuccessList) {
                mergeCommit(commitTx, blockHeader, chain);
            }
        }
        return rollbackResult;
    }

    private boolean mergeCommit(Transaction transaction, BlockHeader header, Chain chain) {
        BatchStakingMerge batchStakingMerge = new BatchStakingMerge();
        try {
            batchStakingMerge.parse(transaction.getTxData(), 0);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }
        List<NulsHash> successList = new ArrayList<>();
        for (NulsHash joinTxHash : batchStakingMerge.getJoinTxHashList()) {
            //获取该笔交易对应的加入共识委托交易
            Deposit deposit = depositManager.getDeposit(chain, joinTxHash);
            //委托交易不存在
            if (deposit == null) {
                chain.getLogger().error("The exited deposit information does not exist");
                return false;
            }
            //委托交易已退出
            if (deposit.getDelHeight() > 0) {
                chain.getLogger().error("The exited deposit information has been withdrawn");
                return false;
            }
            //设置退出共识高度
            deposit.setDelHeight(header.getHeight());

            boolean result = depositManager.updateDeposit(chain, deposit);
            if (!result) {
                for (NulsHash rbHash : successList) {
                    deposit = depositManager.getDeposit(chain, rbHash);
                    deposit.setDelHeight(-1L);
                    depositManager.updateDeposit(chain, deposit);
                }
                return false;
            }
            successList.add(joinTxHash);
        }
        Deposit deposit = new Deposit();
        deposit.setAddress(batchStakingMerge.getAddress());
        deposit.setDeposit(batchStakingMerge.getDeposit());
        deposit.setAssetChainId(batchStakingMerge.getAssetChainId());
        deposit.setAssetId(batchStakingMerge.getAssetId());
        deposit.setDepositType(batchStakingMerge.getDepositType());
        deposit.setTimeType(batchStakingMerge.getTimeType());
        deposit.setTxHash(transaction.getHash());
        deposit.setTime(transaction.getTime());
        deposit.setBlockHeight(header.getHeight());
        return depositManager.addDeposit(chain, deposit);
    }

    private boolean mergeRollBack(Transaction transaction, Chain chain, BlockHeader header) {
        BatchStakingMerge batchStakingMerge = new BatchStakingMerge();
        try {
            batchStakingMerge.parse(transaction.getTxData(), 0);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }
        List<NulsHash> successList = new ArrayList<>();
        for (NulsHash joinTxHash : batchStakingMerge.getJoinTxHashList()) {
            //获取该笔交易对应的加入共识委托交易
            Deposit deposit = depositManager.getDeposit(chain, joinTxHash);
            //委托交易不存在
            if (deposit == null) {
                chain.getLogger().error("The deposit information does not exist");
                return false;
            }
            if (deposit.getDelHeight() != header.getHeight()) {
                chain.getLogger().error("Exit delegate height is different from rollback height");
                return false;
            }
            deposit.setDelHeight(-1L);
            boolean result = depositManager.updateDeposit(chain, deposit);
            if (!result) {
                for (NulsHash rbHash : successList) {
                    deposit = depositManager.getDeposit(chain, rbHash);
                    deposit.setDelHeight(header.getHeight());
                    depositManager.updateDeposit(chain, deposit);
                }
                return false;
            }
            successList.add(joinTxHash);
        }

        return depositManager.removeDeposit(chain, transaction.getHash());
    }
}

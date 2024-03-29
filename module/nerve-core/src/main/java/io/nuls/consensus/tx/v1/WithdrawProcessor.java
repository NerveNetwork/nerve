package io.nuls.consensus.tx.v1;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.tx.txdata.CancelDeposit;
import io.nuls.consensus.model.bo.tx.txdata.Deposit;
import io.nuls.consensus.service.StakingLimitService;
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
import io.nuls.consensus.utils.validator.WithdrawValidator;

import java.io.IOException;
import java.util.*;

/**
 * quitstakingTransaction processor
 *
 * @author tag
 * @date 2019/6/1
 */
@Component("WithdrawProcessorV1")
public class WithdrawProcessor implements TransactionProcessor {
    @Autowired
    private DepositManager depositManager;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private WithdrawValidator validator;

    @Autowired
    private StakingLimitService stakingLimitService;

    @Override
    public int getType() {
        return TxType.CANCEL_DEPOSIT;
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
        for (Transaction withdrawTx : txs) {
            try {
                //Transaction time verification
                long time = NulsDateUtils.getCurrentTimeSeconds();
                if (blockHeader != null) {
                    time = blockHeader.getTime();
                }
                if (withdrawTx.getTime() < time - ConsensusConstant.UNLOCK_TIME_DIFFERENCE_LIMIT || withdrawTx.getTime() > time + ConsensusConstant.UNLOCK_TIME_DIFFERENCE_LIMIT) {
                    invalidTxList.add(withdrawTx);
                    chain.getLogger().error("Trading time error,txTime:{},time:{}", withdrawTx.getTime(), time);
                    errorCode = ConsensusErrorCode.ERROR_UNLOCK_TIME.getCode();
                    continue;
                }
                rs = validator.validate(chain, withdrawTx, blockHeader);
                if (rs.isFailed()) {
                    invalidTxList.add(withdrawTx);
                    chain.getLogger().error("Intelligent contract withdrawal delegation transaction verification failed");
                    errorCode = rs.getErrorCode().getCode();
                    continue;
                }
                CancelDeposit cancelDeposit = new CancelDeposit();
                cancelDeposit.parse(withdrawTx.getTxData(), 0);
                /*
                 * Repeated exit node
                 * */
                if (!hashSet.add(cancelDeposit.getJoinTxHash())) {
                    invalidTxList.add(withdrawTx);
                    chain.getLogger().info("Repeated transactions");
                    errorCode = ConsensusErrorCode.CONFLICT_ERROR.getCode();
                }
            } catch (NulsException e) {
                invalidTxList.add(withdrawTx);
                chain.getLogger().error("Conflict calibration error");
                chain.getLogger().error(e);
                errorCode = e.getErrorCode().getCode();
            } catch (IOException io) {
                invalidTxList.add(withdrawTx);
                chain.getLogger().error("Conflict calibration error");
                chain.getLogger().error(io);
                errorCode = ConsensusErrorCode.SERIALIZE_ERROR.getCode();
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
            if (withdrawCommit(tx, blockHeader, chain)) {
                commitSuccessList.add(tx);
            } else {
                commitResult = false;
                break;
            }
        }
        //Roll back transactions that have been successfully submitted
        if (!commitResult) {
            for (Transaction rollbackTx : commitSuccessList) {
                withdrawRollBack(rollbackTx, chain, blockHeader);
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
            if (withdrawRollBack(tx, chain, blockHeader)) {
                rollbackSuccessList.add(tx);
            } else {
                rollbackResult = false;
                break;
            }
        }
        //Save successfully rolled back transactions
        if (!rollbackResult) {
            for (Transaction commitTx : rollbackSuccessList) {
                withdrawCommit(commitTx, blockHeader, chain);

            }
        }
        return rollbackResult;
    }

    private boolean withdrawCommit(Transaction transaction, BlockHeader header, Chain chain) {
        CancelDeposit cancelDeposit = new CancelDeposit();
        try {
            cancelDeposit.parse(transaction.getTxData(), 0);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }

        //Obtain the consensus delegation transaction corresponding to this transaction
        Deposit deposit = depositManager.getDeposit(chain, cancelDeposit.getJoinTxHash());
        //The entrusted transaction does not exist
        if (deposit == null) {
            chain.getLogger().error("The exited deposit information does not exist");
            return false;
        }
        //The entrusted transaction has been exited
        if (deposit.getDelHeight() > 0) {
            chain.getLogger().error("The exited deposit information has been withdrawn");
            return false;
        }
        //Set exit consensus height
        deposit.setDelHeight(header.getHeight());

        boolean result = depositManager.updateDeposit(chain, deposit);
        if (result) {
            result = this.stakingLimitService.sub(chain, chainManager.getAssetByAsset(deposit.getAssetChainId(), deposit.getAssetId()), deposit.getDeposit());
            if (!result) {
                deposit.setDelHeight(-1L);
                depositManager.updateDeposit(chain, deposit);
            }
        }
        return result;
    }

    private boolean withdrawRollBack(Transaction transaction, Chain chain, BlockHeader header) {
        CancelDeposit cancelDeposit = new CancelDeposit();
        try {
            cancelDeposit.parse(transaction.getTxData(), 0);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }
        //Obtain the consensus delegation transaction corresponding to this transaction
        Deposit deposit = depositManager.getDeposit(chain, cancelDeposit.getJoinTxHash());
        //The entrusted transaction does not exist
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
        if (result) {
            result = this.stakingLimitService.add(chain, chainManager.getAssetByAsset(deposit.getAssetChainId(), deposit.getAssetId()), deposit.getDeposit());
            if (!result) {
                deposit.setDelHeight(header.getHeight());
                depositManager.updateDeposit(chain, deposit);
            }
        }
        return result;
    }
}

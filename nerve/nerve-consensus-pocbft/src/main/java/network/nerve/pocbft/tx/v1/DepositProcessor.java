package network.nerve.pocbft.tx.v1;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.constant.ConsensusErrorCode;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.tx.txdata.Deposit;
import network.nerve.pocbft.service.StakingLimitService;
import network.nerve.pocbft.utils.LoggerUtil;
import network.nerve.pocbft.utils.manager.ChainManager;
import network.nerve.pocbft.utils.manager.DepositManager;
import network.nerve.pocbft.utils.validator.DepositValidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 委托交易处理器
 *
 * @author tag
 * @date 2019/6/1
 */
@Component("DepositProcessorV1")
public class DepositProcessor implements TransactionProcessor {
    @Autowired
    private DepositManager depositManager;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private DepositValidator validator;

    @Autowired
    private StakingLimitService stakingLimitService;

    @Override
    public int getType() {
        return TxType.DEPOSIT;
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
        Result rs;
        for (Transaction depositTx : txs) {
            try {
                //交易时间验证
                long time = NulsDateUtils.getCurrentTimeSeconds();
                if (blockHeader != null) {
                    time = blockHeader.getTime();
                }
                if (depositTx.getTime() < time - ConsensusConstant.UNLOCK_TIME_DIFFERENCE_LIMIT || depositTx.getTime() > time + ConsensusConstant.UNLOCK_TIME_DIFFERENCE_LIMIT) {
                    invalidTxList.add(depositTx);
                    chain.getLogger().error("Trading time error,txTime:{},time:{}", depositTx.getTime(), time);
                    errorCode = ConsensusErrorCode.ERROR_UNLOCK_TIME.getCode();
                    continue;
                }
                rs = validator.validate(chain, depositTx, blockHeader);
                if (rs.isFailed()) {
                    invalidTxList.add(depositTx);
                    chain.getLogger().info("Transaction basis validation error");
                    errorCode = rs.getErrorCode().getCode();
                }
            } catch (NulsException e) {
                invalidTxList.add(depositTx);
                chain.getLogger().error("Conflict calibration error");
                chain.getLogger().error(e);
                errorCode = e.getErrorCode().getCode();
            } catch (IOException io) {
                invalidTxList.add(depositTx);
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
            if (depositCommit(tx, blockHeader, chain)) {
                commitSuccessList.add(tx);
            } else {
                commitResult = false;
                break;
            }
        }
        //回滚已提交成功的交易
        if (!commitResult) {
            for (Transaction rollbackTx : commitSuccessList) {
                depositRollBack(rollbackTx, chain);

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
            if (depositRollBack(tx, chain)) {
                rollbackSuccessList.add(tx);
            } else {
                rollbackResult = false;
                break;
            }
        }
        //保存已回滚成功的交易
        if (!rollbackResult) {
            for (Transaction commitTx : rollbackSuccessList) {
                depositCommit(commitTx, blockHeader, chain);
            }
        }
        return rollbackResult;
    }

    private boolean depositCommit(Transaction transaction, BlockHeader blockHeader, Chain chain) {
        Deposit deposit = new Deposit();
        try {
            deposit.parse(transaction.getTxData(), 0);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }
        deposit.setTxHash(transaction.getHash());
        deposit.setTime(transaction.getTime());
        deposit.setBlockHeight(blockHeader.getHeight());
        boolean result = depositManager.addDeposit(chain, deposit);
        if (result) {
            result = stakingLimitService.add(chain, chainManager.getAssetByAsset(deposit.getAssetChainId(), deposit.getAssetId()), deposit.getDeposit());
            if (!result) {
                depositManager.removeDeposit(chain, transaction.getHash());
            }
        }
        return result;
    }

    private boolean depositRollBack(Transaction transaction, Chain chain) {
        Deposit deposit = depositManager.getDeposit(chain, transaction.getHash());
        boolean result = depositManager.removeDeposit(chain, transaction.getHash());
        if (result) {
            result = stakingLimitService.sub(chain, chainManager.getAssetByAsset(deposit.getAssetChainId(), deposit.getAssetId()), deposit.getDeposit());
            if (!result) {
                depositManager.addDeposit(chain, deposit);
            }
        }
        return result;
    }
}

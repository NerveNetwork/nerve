package io.nuls.consensus.utils.validator;
import io.nuls.base.data.BlockHeader;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.tx.txdata.ChangeAgentDepositData;
import io.nuls.consensus.model.po.AgentPo;
import io.nuls.consensus.utils.ConsensusNetUtil;
import io.nuls.consensus.utils.manager.AgentDepositNonceManager;
import io.nuls.consensus.utils.validator.base.BaseValidator;
import io.nuls.consensus.utils.manager.AgentManager;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.consensus.constant.ConsensusErrorCode;

import java.io.IOException;
import java.math.BigInteger;

/**
 * Reduce margin trading validators
 * @author  tag
 * */
@Component
public class ReduceAgentDepositValidator extends BaseValidator {
    @Autowired
    private AgentManager agentManager;

    @Override
    public Result validate(Chain chain, Transaction tx, BlockHeader blockHeader) throws NulsException, IOException {
        //txDatavalidate
        if (tx.getTxData() == null) {
            chain.getLogger().error("CreateAgent -- TxData is null");
            return Result.getFailed(ConsensusErrorCode.TX_DATA_VALIDATION_ERROR);
        }
        ChangeAgentDepositData txData = new ChangeAgentDepositData();
        txData.parse(tx.getTxData(),0);

        //Verify if the transaction initiator is the node creator, only the node creator can reduce the margin
        Result rs = agentManager.creatorValid(chain, txData.getAgentHash(), txData.getAddress());
        if(rs.isFailed()){
            return rs;
        }

        //Margin verification
        AgentPo po = (AgentPo) rs.getData();
        BigInteger amount = txData.getAmount();

        BigInteger minAppendAmount = chain.getConfig().getReduceAgentDepositMin();
        if (chain.getBestHeader().getHeight() > chain.getConfig().getV130Height()) {
            minAppendAmount = chain.getConfig().getMinAppendAndExitAmount();
        }
        //The amount is less than the minimum allowed amount
        if(amount.compareTo(minAppendAmount) < 0){
            chain.getLogger().error("The amount of exit margin is not within the allowed range");
            return Result.getFailed(ConsensusErrorCode.REDUCE_DEPOSIT_OUT_OF_RANGE);
        }
        BigInteger maxReduceAmount = po.getDeposit().subtract(chain.getConfig().getDepositMin());
        //The exit amount is greater than the current maximum allowed exit amount
        if(amount.compareTo(maxReduceAmount) > 0){
            chain.getLogger().error("Exit amount is greater than the current maximum amount allowed,deposit:{},maxReduceAmount:{},reduceAmount:{}",po.getDeposit(),maxReduceAmount,amount);
            return Result.getFailed(ConsensusErrorCode.REDUCE_DEPOSIT_OUT_OF_RANGE);
        }

        //coinDatavalidate
        CoinData coinData = new CoinData();
        coinData.parse(tx.getCoinData(),0);
        rs = reduceDepositCoinDataValid(chain, amount, coinData, txData.getAddress(),  tx.getTime() + chain.getConfig().getReducedDepositLockTime());
        if(rs.isFailed()){
            return rs;
        }
        //CoinData noncevalidate
        if(!AgentDepositNonceManager.coinDataNonceVerify(chain, coinData, po.getHash())){
            return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }

        //Verification fee
        rs = validFee(chain, coinData, tx);
        if(rs.isFailed()){
            return rs;
        }

        return ConsensusNetUtil.getSuccess();
    }
}

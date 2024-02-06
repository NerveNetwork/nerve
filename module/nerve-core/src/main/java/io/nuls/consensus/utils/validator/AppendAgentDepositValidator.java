package io.nuls.consensus.utils.validator;

import io.nuls.base.data.BlockHeader;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.tx.txdata.ChangeAgentDepositData;
import io.nuls.consensus.model.po.AgentPo;
import io.nuls.consensus.utils.ConsensusNetUtil;
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
 * Additional margin trading validator
 *
 * @author tag
 */
@Component
public class AppendAgentDepositValidator extends BaseValidator {
    @Autowired
    private AgentManager agentManager;

    public Result validate(Chain chain, Transaction tx, BlockHeader blockHeader) throws NulsException, IOException {
        //txDatavalidate
        if (tx.getTxData() == null) {
            chain.getLogger().error("CreateAgent -- TxData is null");
            return Result.getFailed(ConsensusErrorCode.TX_DATA_VALIDATION_ERROR);
        }
        ChangeAgentDepositData txData = new ChangeAgentDepositData();
        txData.parse(tx.getTxData(), 0);

        //Verify if the transaction initiator is the node creator, only the node creator can add margin
        Result rs = agentManager.creatorValid(chain, txData.getAgentHash(), txData.getAddress());
        if (rs.isFailed()) {
            return rs;
        }

        //Margin verification
        AgentPo po = (AgentPo) rs.getData();
        BigInteger amount = txData.getAmount();
        //Additional commission amount greater than or equal to the minimum additional amount
        BigInteger minAppendAmount = chain.getConfig().getAppendAgentDepositMin();
        if (chain.getBestHeader().getHeight() > chain.getConfig().getV130Height()) {
            minAppendAmount = chain.getConfig().getMinAppendAndExitAmount();
        }
        if (amount.compareTo(minAppendAmount) < 0) {
            chain.getLogger().error("The amount of additional margin is less than the minimum value");
            return Result.getFailed(ConsensusErrorCode.APPEND_DEPOSIT_OUT_OF_RANGE);
        }
        //The additional amount is less than the maximum margin amount of the node
        if (amount.compareTo(chain.getConfig().getDepositMax()) >= 0) {
            chain.getLogger().error("The amount of additional margin is less than the minimum value");
            return Result.getFailed(ConsensusErrorCode.APPEND_DEPOSIT_OUT_OF_RANGE);
        }
        BigInteger newDeposit = po.getDeposit().add(amount);
        //The additional amount plus the current margin of the node is less than or equal to the maximum margin of the node
        if (newDeposit.compareTo(chain.getConfig().getDepositMax()) > 0) {
            chain.getLogger().error("The amount of additional margin is less than the minimum value");
            return Result.getFailed(ConsensusErrorCode.DEPOSIT_OUT_OF_RANGE);
        }

        //coinDatavalidate
        CoinData coinData = new CoinData();
        coinData.parse(tx.getCoinData(), 0);
        rs = appendDepositCoinDataValid(chain, amount, coinData, txData.getAddress());
        if (rs.isFailed()) {
            return rs;
        }

        //Verify if the handling fee is sufficient
        rs = validFee(chain, coinData, tx);
        if (rs.isFailed()) {
            return rs;
        }

        return ConsensusNetUtil.getSuccess();
    }
}

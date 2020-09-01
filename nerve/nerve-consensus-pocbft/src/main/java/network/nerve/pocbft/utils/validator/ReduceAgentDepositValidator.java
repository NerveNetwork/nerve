package network.nerve.pocbft.utils.validator;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.tx.txdata.ChangeAgentDepositData;
import network.nerve.pocbft.model.po.AgentPo;
import network.nerve.pocbft.utils.ConsensusNetUtil;
import network.nerve.pocbft.utils.manager.AgentDepositNonceManager;
import network.nerve.pocbft.utils.manager.AgentManager;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.pocbft.constant.ConsensusErrorCode;
import network.nerve.pocbft.utils.validator.base.BaseValidator;

import java.io.IOException;
import java.math.BigInteger;

/**
 * 减少保证金交易验证器
 * @author  tag
 * */
@Component
public class ReduceAgentDepositValidator extends BaseValidator {
    @Autowired
    private AgentManager agentManager;

    @Override
    public Result validate(Chain chain, Transaction tx) throws NulsException, IOException {
        //txData验证
        if (tx.getTxData() == null) {
            chain.getLogger().error("CreateAgent -- TxData is null");
            return Result.getFailed(ConsensusErrorCode.TX_DATA_VALIDATION_ERROR);
        }
        ChangeAgentDepositData txData = new ChangeAgentDepositData();
        txData.parse(tx.getTxData(),0);

        //验证交易发起者是否为节点创建者，只有节点创建者才能减少保证金
        Result rs = agentManager.creatorValid(chain, txData.getAgentHash(), txData.getAddress());
        if(rs.isFailed()){
            return rs;
        }

        //保证金验证
        AgentPo po = (AgentPo) rs.getData();
        BigInteger amount = txData.getAmount();

        BigInteger minAppendAmount = chain.getConfig().getReduceAgentDepositMin();
        if (chain.getBestHeader().getHeight() > chain.getConfig().getV130Height()) {
            minAppendAmount = chain.getConfig().getMinAppendAndExitAmount();
        }
        //金额小于允许的最小金额
        if(amount.compareTo(minAppendAmount) < 0){
            chain.getLogger().error("The amount of exit margin is not within the allowed range");
            return Result.getFailed(ConsensusErrorCode.REDUCE_DEPOSIT_OUT_OF_RANGE);
        }
        BigInteger maxReduceAmount = po.getDeposit().subtract(chain.getConfig().getDepositMin());
        //退出金额大于当前允许退出的最大金额
        if(amount.compareTo(maxReduceAmount) > 0){
            chain.getLogger().error("Exit amount is greater than the current maximum amount allowed,deposit:{},maxReduceAmount:{},reduceAmount:{}",po.getDeposit(),maxReduceAmount,amount);
            return Result.getFailed(ConsensusErrorCode.REDUCE_DEPOSIT_OUT_OF_RANGE);
        }

        //coinData验证
        CoinData coinData = new CoinData();
        coinData.parse(tx.getCoinData(),0);
        rs = reduceDepositCoinDataValid(chain, amount, coinData, txData.getAddress(),  tx.getTime() + chain.getConfig().getReducedDepositLockTime());
        if(rs.isFailed()){
            return rs;
        }
        //CoinData nonce验证
        if(!AgentDepositNonceManager.coinDataNonceVerify(chain, coinData, po.getHash())){
            return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }

        //验证手续费
        rs = validFee(chain, coinData, tx);
        if(rs.isFailed()){
            return rs;
        }

        return ConsensusNetUtil.getSuccess();
    }
}

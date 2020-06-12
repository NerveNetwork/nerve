package network.nerve.pocbft.utils.validator;

import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.tx.txdata.ChangeAgentDepositData;
import network.nerve.pocbft.model.po.AgentPo;
import network.nerve.pocbft.utils.ConsensusNetUtil;
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
 * 追加保证金交易验证器
 * @author  tag
 * */
@Component
public class AppendAgentDepositValidator extends BaseValidator {
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

        //验证交易发起者是否为节点创建者，只有节点创建者才能追加保证金
        Result rs = agentManager.creatorValid(chain, txData.getAgentHash(), txData.getAddress());
        if(rs.isFailed()){
            return rs;
        }

        //保证金验证
        AgentPo po = (AgentPo) rs.getData();
        BigInteger amount = txData.getAmount();
        //追加委托金额大于等于最小追加金额
        if(amount.compareTo(chain.getConfig().getAppendAgentDepositMin()) < 0){
            chain.getLogger().error("The amount of additional margin is less than the minimum value");
            return Result.getFailed(ConsensusErrorCode.APPEND_DEPOSIT_OUT_OF_RANGE);
        }
        //追加金额小于节点最大保证金金额
        if(amount.compareTo(chain.getConfig().getDepositMax()) >= 0){
            chain.getLogger().error("The amount of additional margin is less than the minimum value");
            return Result.getFailed(ConsensusErrorCode.APPEND_DEPOSIT_OUT_OF_RANGE);
        }
        BigInteger newDeposit = po.getDeposit().add(amount);
        //追加金额加上节点当前保证金小于等于节点最大保证金金额
        if(newDeposit.compareTo(chain.getConfig().getDepositMax()) > 0){
            chain.getLogger().error("The amount of additional margin is less than the minimum value");
            return Result.getFailed(ConsensusErrorCode.DEPOSIT_OUT_OF_RANGE);
        }

        //coinData验证
        CoinData coinData = new CoinData();
        coinData.parse(tx.getCoinData(),0);
        rs = appendDepositCoinDataValid(chain, amount, coinData, txData.getAddress());
        if (rs.isFailed()) {
            return rs;
        }

        //验证手续费是否足够
        rs = validFee(chain, coinData, tx);
        if (rs.isFailed()) {
            return rs;
        }

        return ConsensusNetUtil.getSuccess();
    }
}

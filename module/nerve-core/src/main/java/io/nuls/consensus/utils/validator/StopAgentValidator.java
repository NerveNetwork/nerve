package io.nuls.consensus.utils.validator;
import io.nuls.base.data.BlockHeader;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.tx.txdata.StopAgent;
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

/**
 * 注销节点交易验证器
 * @author  tag
 * */
@Component
public class StopAgentValidator extends BaseValidator {
    @Autowired
    private AgentManager agentManager;
    @Override
    public Result validate(Chain chain, Transaction tx, BlockHeader blockHeader) throws NulsException, IOException {
        //txData验证
        if (tx.getTxData() == null) {
            chain.getLogger().error("CreateAgent -- TxData is null");
            return Result.getFailed(ConsensusErrorCode.TX_DATA_VALIDATION_ERROR);
        }
        StopAgent txData = new StopAgent();
        txData.parse(tx.getTxData(), 0);

        //验证交易发起者是否为节点创建者
        Result rs = agentManager.creatorValid(chain, txData.getCreateTxHash(), txData.getAddress());
        if(rs.isFailed()){
            return rs;
        }

        //coinData验证
        AgentPo po = (AgentPo) rs.getData();
        CoinData coinData = new CoinData();
        coinData.parse(tx.getCoinData(), 0);
        rs = reduceDepositCoinDataValid(chain, po.getDeposit(), coinData, po.getAgentAddress(), tx.getTime()+chain.getConfig().getStopAgentLockTime());
        if(rs.isFailed()){
            return rs;
        }
        //CoinData nonce验证
        if(!AgentDepositNonceManager.coinDataNonceVerify(chain, coinData, po.getHash())){
            return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }

        //验证手续费是否足够
        rs = validFee(chain, coinData, tx);
        if (rs.isFailed()) {
            return rs;
        }

        return ConsensusNetUtil.getSuccess();
    }
}
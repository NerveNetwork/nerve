package nerve.network.pocbft.utils.validator;

import io.nuls.base.data.CoinData;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import nerve.network.pocbft.constant.ConsensusErrorCode;
import nerve.network.pocbft.model.bo.Chain;
import nerve.network.pocbft.model.bo.tx.txdata.CancelDeposit;
import nerve.network.pocbft.model.po.DepositPo;
import nerve.network.pocbft.storage.DepositStorageService;
import nerve.network.pocbft.utils.enumeration.DepositTimeType;
import nerve.network.pocbft.utils.enumeration.DepositType;
import nerve.network.pocbft.utils.validator.base.BaseValidator;

import java.io.IOException;
import java.util.Arrays;

import static nerve.network.pocbft.utils.ConsensusNetUtil.getSuccess;

/**
 * 减少保证金交易验证器
 * @author  tag
 * */
@Component
public class WithdrawValidator extends BaseValidator {
    @Autowired
    private DepositStorageService depositStorageService;
    @Override
    public Result validate(Chain chain, Transaction tx) throws NulsException, IOException {
        CancelDeposit txData = new CancelDeposit();
        txData.parse(tx.getTxData(), 0);
        DepositPo depositPo = depositStorageService.get(txData.getJoinTxHash(), chain.getConfig().getChainId());
        if (depositPo == null || depositPo.getDelHeight() > 0) {
            chain.getLogger().error("Withdraw -- Deposit transaction does not exist");
            return Result.getFailed(ConsensusErrorCode.DATA_NOT_EXIST);
        }

        //交易发起者是否为委托者
        if(!Arrays.equals(depositPo.getAddress(), txData.getAddress())){
            chain.getLogger().error("Withdraw -- Account is not the agent Creator");
            return Result.getFailed(ConsensusErrorCode.ACCOUNT_IS_NOT_CREATOR);
        }

        //如果为定期委托则验证委托是否到期
        if(depositPo.getDepositType() == DepositType.REGULAR.getCode()){
            DepositTimeType depositTimeType = DepositTimeType.getValue(depositPo.getTimeType());
            if(depositTimeType == null){
                chain.getLogger().error("Recurring delegation type does not exist");
                return Result.getFailed(ConsensusErrorCode.REGULAR_DEPOSIT_TIME_TYPE_NOT_EXIST);
            }
            long periodicTime = depositPo.getTime() + depositTimeType.getTime();
            if(tx.getTime() < periodicTime){
                chain.getLogger().error("Term commission not due");
                return Result.getFailed(ConsensusErrorCode.DEPOSIT_NOT_DUE);
            }
        }

        //coinData验证
        CoinData coinData = new CoinData();
        coinData.parse(tx.getCoinData(),0);
        Result rs = reduceDepositCoinDataValid(chain, depositPo.getDeposit(), coinData, depositPo.getAddress(), 0,depositPo.getAssetChainId(),depositPo.getAssetId());
        if(rs.isFailed()){
            return rs;
        }

        //验证手续费
        rs = validFee(chain, coinData, tx);
        if(rs.isFailed()){
            return rs;
        }

        return getSuccess();
    }
}

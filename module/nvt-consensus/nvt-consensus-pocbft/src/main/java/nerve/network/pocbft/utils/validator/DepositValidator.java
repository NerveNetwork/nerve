package nerve.network.pocbft.utils.validator;

import io.nuls.base.data.CoinData;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import nerve.network.pocbft.constant.ConsensusErrorCode;
import nerve.network.pocbft.model.bo.Chain;
import nerve.network.pocbft.model.bo.tx.txdata.Deposit;
import nerve.network.pocbft.utils.ConsensusAwardUtil;
import nerve.network.pocbft.utils.enumeration.DepositTimeType;
import nerve.network.pocbft.utils.enumeration.DepositType;
import nerve.network.pocbft.utils.manager.ChainManager;
import nerve.network.pocbft.utils.validator.base.BaseValidator;

import java.io.IOException;
import java.math.BigInteger;

import static nerve.network.pocbft.utils.ConsensusNetUtil.getSuccess;

/**
 * 减少保证金交易验证器
 * @author  tag
 * */
@Component
public class DepositValidator extends BaseValidator {
    @Autowired
    private ChainManager chainManager;
    @Override
    public Result validate(Chain chain, Transaction tx) throws NulsException, IOException {
        if (null == tx || null == tx.getTxData() || tx.getCoinData() == null) {
            chain.getLogger().error("Deposit -- Transaction data error");
            return Result.getFailed(ConsensusErrorCode.TX_DATA_VALIDATION_ERROR);
        }
        Deposit deposit = new Deposit();
        deposit.parse(tx.getTxData(), 0);

        //验证资产是否可以参与stacking
        if(!chainManager.assetStackingVerify(deposit.getAssetChainId(), deposit.getAssetId())){
            chain.getLogger().error("The current asset does not support stacking");
            return Result.getFailed(ConsensusErrorCode.ASSET_NOT_SUPPORT_STACKING);
        }


        //如果为存定期则验证定期类型是否存在
        if(deposit.getDepositType() == DepositType.REGULAR.getCode()){
            DepositTimeType depositTimeType = DepositTimeType.getValue(deposit.getTimeType());
            if(depositTimeType == null){
                chain.getLogger().error("Recurring delegation type does not exist");
                return Result.getFailed(ConsensusErrorCode.REGULAR_DEPOSIT_TIME_TYPE_NOT_EXIST);
            }
        }

        //验证委托金额是否是否小于最小委托金额
        BigInteger realAmount = ConsensusAwardUtil.getRealAmount(deposit.getDeposit(), deposit.getAssetChainId(), deposit.getAssetId(),tx.getTime());
        if(realAmount.compareTo(chain.getConfig().getEntrustMin()) < 0){
            chain.getLogger().error("Deposit -- Less than the minimum entrusted amount");
            return Result.getFailed(ConsensusErrorCode.DEPOSIT_NOT_ENOUGH);
        }

        CoinData coinData = new CoinData();
        coinData.parse(tx.getCoinData(), 0);
        Result rs = appendDepositCoinDataValid(chain, deposit.getDeposit(), coinData, deposit.getAddress(), deposit.getAssetChainId(), deposit.getAssetId());
        if(rs.isFailed()){
            return rs;
        }

        //验证手续费是否足够
        rs = validFee(chain, coinData, tx);
        if (rs.isFailed()) {
            return rs;
        }

        return getSuccess();
    }
}

package io.nuls.consensus.utils.validator;

import io.nuls.base.data.BlockHeader;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.StackingAsset;
import io.nuls.consensus.model.bo.tx.txdata.Deposit;
import io.nuls.consensus.service.StakingLimitService;
import io.nuls.consensus.utils.ConsensusAwardUtil;
import io.nuls.consensus.utils.ConsensusNetUtil;
import io.nuls.consensus.utils.enumeration.DepositTimeType;
import io.nuls.consensus.utils.enumeration.DepositType;
import io.nuls.consensus.utils.manager.ChainManager;
import io.nuls.consensus.utils.validator.base.BaseValidator;
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
 * 减少保证金交易验证器
 *
 * @author tag
 */
@Component
public class DepositValidator extends BaseValidator {
    @Autowired
    private ChainManager chainManager;

    @Autowired
    private StakingLimitService stakingLimitService;

    @Override
    public Result validate(Chain chain, Transaction tx, BlockHeader blockHeader) throws NulsException, IOException {
        if (null == tx || null == tx.getTxData() || tx.getCoinData() == null) {
            chain.getLogger().error("Deposit -- Transaction data error");
            return Result.getFailed(ConsensusErrorCode.TX_DATA_VALIDATION_ERROR);
        }
        Deposit deposit = new Deposit();
        deposit.parse(tx.getTxData(), 0);

        //验证资产是否可以参与stacking
        StackingAsset stackingAsset = chainManager.assetStackingVerify(deposit.getAssetChainId(), deposit.getAssetId());
        if (null == stackingAsset) {
            chain.getLogger().error("The asset cannot participate in staking");
            return Result.getFailed(ConsensusErrorCode.ASSET_NOT_SUPPORT_STACKING);
        }
        if (stackingAsset.getStopHeight() > 0 && stackingAsset.getStopHeight() <= chain.getBestHeader().getHeight()) {
            chain.getLogger().error("The asset is stoped to staking");
            return Result.getFailed(ConsensusErrorCode.ASSET_NOT_SUPPORT_STACKING);
        }


        //如果为存定期则验证定期类型是否存在
        if (deposit.getDepositType() == DepositType.REGULAR.getCode()) {
            if (!stackingAsset.isCanBePeriodically()) {
                chain.getLogger().error("The asset cannot participate in periodic staking");
                return Result.getFailed(ConsensusErrorCode.ASSET_NOT_SUPPORT_REGULAR_STACKING);
            }

            DepositTimeType depositTimeType = DepositTimeType.getValue(deposit.getTimeType());
            if (depositTimeType == null) {
                chain.getLogger().error("Recurring delegation type does not exist");
                return Result.getFailed(ConsensusErrorCode.REGULAR_DEPOSIT_TIME_TYPE_NOT_EXIST);
            }
        }

        //验证委托金额是否是否小于最小委托金额
        BigInteger realAmount = ConsensusAwardUtil.getRealAmount(chain.getChainId(), deposit.getDeposit(), stackingAsset, tx.getTime() * 1000L);
        boolean pass = tx.getBlockHeight() > 0 && tx.getBlockHeight() < chain.getConfig().getDepositVerifyHeight();

        BigInteger minStakingAmount = chain.getConfig().getEntrustMin();
        if (chain.getBestHeader().getHeight() > chain.getConfig().getV130Height()) {
            minStakingAmount = chain.getConfig().getMinStakingAmount();
        }
        if (!pass && realAmount.compareTo(minStakingAmount) < 0) {
            chain.getLogger().error("Deposit -- Less than the minimum entrusted amount");
            return Result.getFailed(ConsensusErrorCode.DEPOSIT_NOT_ENOUGH);
        }

        boolean topValidateResult = stakingLimitService.validate(chain, stackingAsset, deposit.getDeposit());
        if (!topValidateResult) {
            chain.getLogger().error("Deposit -- More than the max entrusted amount");
            return Result.getFailed(ConsensusErrorCode.DEPOSIT_OVER_AMOUNT);
        }

        CoinData coinData = new CoinData();
        coinData.parse(tx.getCoinData(), 0);
        Result rs = appendDepositCoinDataValid(chain, deposit.getDeposit(), coinData, deposit.getAddress(), deposit.getAssetChainId(), deposit.getAssetId());
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

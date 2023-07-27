package io.nuls.consensus.utils.validator;

import io.nuls.base.data.BlockHeader;
import io.nuls.consensus.model.bo.Chain;
import io.nuls.consensus.model.bo.StackingAsset;
import io.nuls.consensus.model.bo.tx.txdata.CancelDeposit;
import io.nuls.consensus.rpc.call.CallMethodUtils;
import io.nuls.consensus.storage.DepositStorageService;
import io.nuls.consensus.utils.ConsensusNetUtil;
import io.nuls.consensus.utils.enumeration.DepositTimeType;
import io.nuls.consensus.utils.enumeration.DepositType;
import io.nuls.consensus.utils.manager.ChainManager;
import io.nuls.consensus.utils.validator.base.BaseValidator;
import io.nuls.consensus.model.po.DepositPo;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.ArraysTool;
import io.nuls.consensus.constant.ConsensusErrorCode;

import java.io.IOException;
import java.util.Arrays;

/**
 * 减少保证金交易验证器
 *
 * @author tag
 */
@Component
public class WithdrawValidator extends BaseValidator {
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private DepositStorageService depositStorageService;

    @Override
    public Result validate(Chain chain, Transaction tx, BlockHeader blockHeader) throws NulsException, IOException {
        CancelDeposit txData = new CancelDeposit();
        txData.parse(tx.getTxData(), 0);
        DepositPo depositPo = depositStorageService.get(txData.getJoinTxHash(), chain.getConfig().getChainId());
        if (depositPo == null || depositPo.getDelHeight() > 0) {
            chain.getLogger().error("Withdraw -- Deposit transaction does not exist");
            return Result.getFailed(ConsensusErrorCode.DATA_NOT_EXIST);
        }

        //交易发起者是否为委托者
        if (!Arrays.equals(depositPo.getAddress(), txData.getAddress())) {
            chain.getLogger().error("Withdraw -- Account is not the agent Creator");
            return Result.getFailed(ConsensusErrorCode.ACCOUNT_IS_NOT_CREATOR);
        }

        //如果为定期委托则验证委托是否到期
        StackingAsset stackingAsset = chainManager.assetStackingVerify(depositPo.getAssetChainId(), depositPo.getAssetId());
        if (null == stackingAsset) {
            chain.getLogger().error("The asset cannot participate in staking");
            return Result.getFailed(ConsensusErrorCode.ASSET_NOT_SUPPORT_STACKING);
        }
        boolean stoped = stackingAsset.getStopHeight() > 0 && stackingAsset.getStopHeight() <= chain.getBestHeader().getHeight();

        if (!stoped && depositPo.getDepositType() == DepositType.REGULAR.getCode()) {
            DepositTimeType depositTimeType = DepositTimeType.getValue(depositPo.getTimeType());
            if (depositTimeType == null) {
                chain.getLogger().error("Recurring delegation type does not exist");
                return Result.getFailed(ConsensusErrorCode.REGULAR_DEPOSIT_TIME_TYPE_NOT_EXIST);
            }
            long periodicTime = depositPo.getTime() + depositTimeType.getTime();
            if (tx.getTime() < periodicTime) {
                chain.getLogger().error("Term commission not due");
                return Result.getFailed(ConsensusErrorCode.DEPOSIT_NOT_DUE);
            }
        }

        //coinData验证
        CoinData coinData = new CoinData();
        coinData.parse(tx.getCoinData(), 0);
        long realLockTime = 0;
        if (chain.getChainId() == depositPo.getAssetChainId() && chain.getAssetId() == depositPo.getAssetId() && chain.getBestHeader().getHeight() > chain.getConfig().getV130Height()) {
            realLockTime = tx.getTime() + chain.getConfig().getExitStakingLockHours() * 3600;
        }
        Result rs = reduceDepositCoinDataValid(chain, depositPo.getDeposit(), coinData, depositPo.getAddress(), realLockTime, depositPo.getAssetChainId(), depositPo.getAssetId());
        if (rs.isFailed()) {
            return rs;
        }
        //验证nonce值
        if (!ArraysTool.arrayEquals(coinData.getFrom().get(0).getNonce(), CallMethodUtils.getNonce(txData.getJoinTxHash().getBytes()))) {
            chain.getLogger().error("nonce error");
            return Result.getFailed(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }
        //验证手续费
        rs = validFee(chain, coinData, tx);
        if (rs.isFailed()) {
            return rs;
        }

        return ConsensusNetUtil.getSuccess();
    }
}

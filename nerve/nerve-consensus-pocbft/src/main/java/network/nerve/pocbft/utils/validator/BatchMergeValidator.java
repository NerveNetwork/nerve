package network.nerve.pocbft.utils.validator;

import io.nuls.base.data.*;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.ArraysTool;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.constant.ConsensusErrorCode;
import network.nerve.pocbft.constant.PocbftConstant;
import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.model.bo.tx.txdata.BatchStakingMerge;
import network.nerve.pocbft.model.bo.tx.txdata.BatchWithdraw;
import network.nerve.pocbft.model.po.DepositPo;
import network.nerve.pocbft.rpc.call.CallMethodUtils;
import network.nerve.pocbft.storage.DepositStorageService;
import network.nerve.pocbft.utils.enumeration.DepositTimeType;
import network.nerve.pocbft.utils.enumeration.DepositType;
import network.nerve.pocbft.utils.validator.base.BaseValidator;

import java.math.BigInteger;
import java.util.*;

/**
 * 批量退出staking验证器
 */
@Component
public class BatchMergeValidator extends BaseValidator {
    @Autowired
    private DepositStorageService depositStorageService;

    @Override
    public Result validate(Chain chain, Transaction tx, BlockHeader blockHeader) {
        BatchStakingMerge txData = new BatchStakingMerge();
        try {
            txData.parse(tx.getTxData(), 0);
            List<ResultData> list = new ArrayList<>();
            Set<NulsHash> hashSet = new HashSet<>();
            boolean feeIncluded = false;
            for (NulsHash hash : txData.getJoinTxHashList()) {
                if (!hashSet.add(hash)) {
                    chain.getLogger().error("withdraw hash is duplicated");
                    return Result.getFailed(ConsensusErrorCode.DEPOSIT_WAS_CANCELED);
                }
                ResultData data = validateDeposit(chain, hash, txData.getAddress(), tx);
                list.add(data);
                if (data.getAssetChainId() == chain.getChainId() && data.getAssetId() == chain.getAssetId()) {
                    feeIncluded = true;
                }
            }
            validateCoinData(chain, tx, list, txData, feeIncluded, blockHeader);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        }
        return Result.getSuccess(null);

    }

    private void validateCoinData(Chain chain, Transaction tx, List<ResultData> list, BatchStakingMerge txData, boolean feeIncluded, BlockHeader blockHeader) throws NulsException {
        CoinData coinData = new CoinData();
        coinData.parse(tx.getCoinData(), 0);
        if (feeIncluded && coinData.getFrom().size() != list.size()) {
            throw new NulsException(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }
        if (!feeIncluded && coinData.getFrom().size() != list.size() + 1) {
            throw new NulsException(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }
        if (coinData.getTo().size() != 1) {
            throw new NulsException(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }
        Map<String, BigInteger> toMap = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            CoinFrom from = coinData.getFrom().get(i);
            ResultData data = list.get(i);
            if (from.getAmount().compareTo(data.getAmount()) != 0 || !ArraysTool.arrayEquals(from.getNonce(), data.getNonce()) || from.getAssetsChainId() != data.getAssetChainId()
                    || from.getAssetsId() != data.getAssetId() || !ArraysTool.arrayEquals(from.getAddress(), txData.getAddress()) || from.getLocked() == 0) {
                throw new NulsException(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
            }
            String key = from.getAssetsChainId() + "-" + from.getAssetsId();
            BigInteger amount = toMap.get(key);
            if (null == amount) {
                amount = from.getAmount();
            } else {
                amount = amount.add(from.getAmount());
            }
            toMap.put(key, amount);
        }
        if (null == blockHeader || PocbftConstant.VERSION_1_19_0_HEIGHT < blockHeader.getHeight()) {
            for (int i = list.size(); i < coinData.getFrom().size(); i++) {
                CoinFrom from = coinData.getFrom().get(i);
                if (from.getLocked() != 0) {
                    throw new NulsException(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
                }
            }
        }
        if (toMap.size() > 1) {
            throw new NulsException(ConsensusErrorCode.DEPOSIT_ASSET_ERROR);
        }

        CoinTo to = coinData.getTo().get(0);
        if (to.getAssetsChainId() != txData.getAssetChainId() || to.getAssetsId() != txData.getAssetId()) {
            throw new NulsException(ConsensusErrorCode.DEPOSIT_ERROR);
        }


        BigInteger amount = toMap.get(to.getAssetsChainId() + "-" + to.getAssetsId());
        BigInteger toAmount = to.getAmount();
        if (to.getAssetsId() == chain.getAssetId() && to.getAssetsChainId() == chain.getChainId()) {
            amount = amount.subtract(BigInteger.valueOf(chain.getConfig().getFeeUnit()));
        }
        if (null == amount || amount.compareTo(toAmount) < 0 || txData.getDeposit().compareTo(toAmount) != 0) {
            throw new NulsException(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }

        if (to.getLockTime() != ConsensusConstant.CONSENSUS_LOCK_TIME) {
            throw new NulsException(ConsensusErrorCode.COIN_DATA_VALID_ERROR);
        }

    }

    private ResultData validateDeposit(Chain chain, NulsHash hash, byte[] address, Transaction batchTx) throws NulsException {
        DepositPo depositPo = depositStorageService.get(hash, chain.getConfig().getChainId());
        if (depositPo == null || depositPo.getDelHeight() > 0) {
            chain.getLogger().error("Withdraw -- Deposit transaction does not exist");
            throw new NulsException(ConsensusErrorCode.DATA_NOT_EXIST);
        }
        //交易发起者是否为委托者
        if (!Arrays.equals(depositPo.getAddress(), address)) {
            chain.getLogger().error("Withdraw -- Account is not the agent Creator");
            throw new NulsException(ConsensusErrorCode.ACCOUNT_IS_NOT_CREATOR);
        }
        //如果为定期委托则验证委托是否到期
        if (depositPo.getDepositType() == DepositType.REGULAR.getCode()) {
            DepositTimeType depositTimeType = DepositTimeType.getValue(depositPo.getTimeType());
            if (depositTimeType == null) {
                chain.getLogger().error("Recurring delegation type does not exist");
                throw new NulsException(ConsensusErrorCode.REGULAR_DEPOSIT_TIME_TYPE_NOT_EXIST);
            }
            long periodicTime = depositPo.getTime() + depositTimeType.getTime();
            if (batchTx.getTime() < periodicTime) {
                chain.getLogger().error("Term commission not due");
                throw new NulsException(ConsensusErrorCode.DEPOSIT_NOT_DUE);
            }
        }
        ResultData data = new ResultData();
        data.setAmount(depositPo.getDeposit());
        data.setAssetChainId(depositPo.getAssetChainId());
        data.setAssetId(depositPo.getAssetId());
        data.setNonce(CallMethodUtils.getNonce(hash.getBytes()));
        return data;
    }


    static class ResultData {
        private byte[] nonce;

        private BigInteger amount;

        private int assetChainId;

        private int assetId;

        public int getAssetChainId() {
            return assetChainId;
        }

        public void setAssetChainId(int assetChainId) {
            this.assetChainId = assetChainId;
        }

        public int getAssetId() {
            return assetId;
        }

        public void setAssetId(int assetId) {
            this.assetId = assetId;
        }

        public byte[] getNonce() {
            return nonce;
        }

        public void setNonce(byte[] nonce) {
            this.nonce = nonce;
        }

        public BigInteger getAmount() {
            return amount;
        }

        public void setAmount(BigInteger amount) {
            this.amount = amount;
        }
    }
}

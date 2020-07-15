package network.nerve.utils.validator;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.*;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.constant.TxType;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.model.ArraysTool;
import io.nuls.core.model.ByteUtils;
import io.nuls.crosschain.base.model.bo.ChainInfo;
import network.nerve.constant.NulsCrossChainConfig;
import network.nerve.constant.NulsCrossChainErrorCode;
import network.nerve.model.bo.Chain;
import network.nerve.rpc.call.ChainManagerCall;
import network.nerve.srorage.ConvertHashService;
import network.nerve.srorage.ConvertCtxService;
import network.nerve.utils.CommonUtil;
import network.nerve.utils.TxUtil;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.BigIntegerUtils;
import network.nerve.utils.manager.ChainManager;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * 跨链交易验证工具类
 * Transaction Verification Tool Class
 *
 * @author tag
 * 2019/4/15
 */
@Component
public class CrossTxValidator {
    @Autowired
    private NulsCrossChainConfig config;

    @Autowired
    private ConvertHashService convertHashService;

    @Autowired
    private ConvertCtxService convertCtxService;

    @Autowired
    private ChainManager chainManager;

    /**
     * 验证交易
     * Verifying transactions
     *
     * @param chain       链ID/chain id
     * @param tx          交易/transaction info
     * @param blockHeader 区块头信息/block header info
     * @return boolean
     */
    @SuppressWarnings("unchecked")
    public boolean validateTx(Chain chain, Transaction tx, BlockHeader blockHeader) throws NulsException, IOException {
        //判断这笔跨链交易是否属于本链
        CoinData coinData = tx.getCoinDataInstance();
        //如果本链为发起链且本链不为主链,则需要生成主网协议的跨链交易验证并验证签名
        int fromChainId = AddressTool.getChainIdByAddress(coinData.getFrom().get(0).getAddress());
        int toChainId = AddressTool.getChainIdByAddress(coinData.getTo().get(0).getAddress());

        if (coinData.getTo().size() != 1) {
            throw new NulsException(NulsCrossChainErrorCode.TO_ADDRESS_ERROR);
        }
        byte[] fromAddress = null;
        for (CoinFrom from : coinData.getFrom()) {
            if (fromAddress == null) {
                fromAddress = from.getAddress();
                continue;
            }
            if (!ArraysTool.arrayEquals(fromAddress, from.getAddress())) {
                throw new NulsException(NulsCrossChainErrorCode.COINDATA_VERIFY_FAIL);
            }
        }

        if (toChainId == 0) {
            throw new NulsException(NulsCrossChainErrorCode.TO_ADDRESS_ERROR);
        }
        int txSize = tx.size();
        //本链协议跨链交易不需要签名拜占庭验证，只需验证交易签名
        if (chain.getChainId() == fromChainId) {
            if (tx.getType() == TxType.CROSS_CHAIN) {
                //验证From中地址是否都签了名
                Set<String> fromAddressSet = tx.getCoinDataInstance().getFromAddressList();
                TransactionSignature transactionSignature = new TransactionSignature();
                transactionSignature.parse(tx.getTransactionSignature(), 0);
                String signAddress;
                boolean verifyResult = false;
                byte[] txHashByte = tx.getHash().getBytes();
                for (P2PHKSignature signature : transactionSignature.getP2PHKSignatures()) {
                    if (!ECKey.verify(txHashByte, signature.getSignData().getSignBytes(), signature.getPublicKey())) {
                        chain.getLogger().error("Signature verification failed");
                        throw new NulsException(new Exception("Transaction signature error !"));
                    }
                    signAddress = AddressTool.getStringAddressByBytes(AddressTool.getAddress(signature.getPublicKey(), chain.getChainId()));
                    fromAddressSet.remove(signAddress);
                    if (fromAddressSet.isEmpty()) {
                        verifyResult = true;
                        break;
                    }
                }
                if (!verifyResult) {
                    throw new NulsException(NulsCrossChainErrorCode.SIGNATURE_ERROR);
                }
                //todo ? 此处乘以5的目的是什么
                txSize += P2PHKSignature.SERIALIZE_LENGTH * (chain.getVerifierList().size() * 5);
            }
        } else {
            Transaction realCtx = tx;
            List<String> verifierList;
            int minPassCount;
            int verifierChainId = fromChainId;
            ChainInfo chainInfo;
            //从主网转入平行链，切当前链不是主网的情况
            if (chain.getChainId() == toChainId && !config.isMainNet()) {
                verifierChainId = config.getMainChainId();
                int txType = TxType.CROSS_CHAIN;
                if (tx.getTxData() != null) {
                    //todo 这行代码的目的是什么
                    txType = ByteUtils.bytesToInt(tx.getTxData());
                }
                realCtx = TxUtil.friendConvertToMain(chain, tx, txType, true);
            }
            chainInfo = chainManager.getChainInfo(verifierChainId);
            if (chainInfo == null) {
                chain.getLogger().error("链未注册,chainId:{}", verifierChainId);
                throw new NulsException(NulsCrossChainErrorCode.CHAIN_UNREGISTERED);
            }
            verifierList = new ArrayList<>(chainInfo.getVerifierList());
            if (verifierList.isEmpty()) {
                chain.getLogger().error("链还未注册验证人,chainId:{}", verifierChainId);
                throw new NulsException(NulsCrossChainErrorCode.CHAIN_UNREGISTERED_VERIFIER);
            }
            minPassCount = chainInfo.getMinPassCount();

            if (!SignatureUtil.validateCtxSignture(realCtx)) {
                chain.getLogger().info("主网协议跨链交易签名验证失败！");
                throw new NulsException(NulsCrossChainErrorCode.SIGNATURE_ERROR);
            }

            if (!TxUtil.signByzantineVerify(chain, realCtx, verifierList, minPassCount, verifierChainId)) {
                chain.getLogger().info("签名拜占庭验证失败！");
                throw new NulsException(NulsCrossChainErrorCode.CTX_SIGN_BYZANTINE_FAIL);
            }
            //todo 当前只适用于主网与平行链之间相互跨链，如果存在平行链之间相互跨链，则需要按其他方式计算
            txSize -= tx.getTransactionSignature().length;
        }

        if (!coinDataValid(chain, coinData, txSize)) {
            throw new NulsException(NulsCrossChainErrorCode.COINDATA_VERIFY_FAIL);
        }

        if (config.isMainNet()) {
            if (!ChainManagerCall.verifyCtxAsset(fromChainId, tx)) {
                chain.getLogger().info("跨链资产验证失败！");
                throw new NulsException(NulsCrossChainErrorCode.CROSS_ASSERT_VALID_ERROR);
            }
        }
        return true;
    }


    public boolean coinDataValid(Chain chain, CoinData coinData, int txSize) throws NulsException {
        return coinDataValid(chain, coinData, txSize, true);
    }

    /**
     * CoinData基础验证
     * CoinData basic validate
     *
     * @param chain
     * @param coinData
     * @param txSize
     */
    public boolean coinDataValid(Chain chain, CoinData coinData, int txSize, boolean isLocalCtx) throws NulsException {
        List<CoinFrom> coinFromList = coinData.getFrom();
        List<CoinTo> coinToList = coinData.getTo();
        if (coinFromList == null || coinFromList.isEmpty()
                || coinToList == null || coinToList.isEmpty()) {
            chain.getLogger().error("转出方或转入方为空");
            throw new NulsException(NulsCrossChainErrorCode.COINFROM_NOT_FOUND);
        }
        int fromChainId = 0;
        int toChainId = 0;
        //跨链交易的from中地址必须是同一条链的地址，to中的地址必须是一条链地址
        for (CoinFrom coinFrom : coinFromList) {
            if (fromChainId == 0) {
                fromChainId = AddressTool.getChainIdByAddress(coinFrom.getAddress());
            }
            if (AddressTool.getChainIdByAddress(coinFrom.getAddress()) != fromChainId) {
                chain.getLogger().error("跨链交易转出方存在多条链账户");
                throw new NulsException(NulsCrossChainErrorCode.CROSS_TX_PAYER_CHAIN_NOT_SAME);
            }
        }
        for (CoinTo coinTo : coinToList) {
            if (toChainId == 0) {
                toChainId = AddressTool.getChainIdByAddress(coinTo.getAddress());
            }
            if (AddressTool.getChainIdByAddress(coinTo.getAddress()) != toChainId) {
                chain.getLogger().error("跨链交易转入方存在多条链账户");
                throw new NulsException(NulsCrossChainErrorCode.CROSS_TX_PAYEE_CHAIN_NOT_SAME);
            }
        }
        //from和to不能是同一个地址
        if (fromChainId == toChainId) {
            chain.getLogger().error("跨链交易转出方和转入方是同一条链账户");
            throw new NulsException(NulsCrossChainErrorCode.PAYEE_AND_PAYER_IS_THE_SAME_CHAIN);
        }
        //查询这条跨链交易是否与本链相关
        int chainId = chain.getChainId();
        if (fromChainId != chainId && toChainId != chainId && !config.isMainNet()) {
            chain.getLogger().error("该跨链交易不是本链跨链交易");
            throw new NulsException(NulsCrossChainErrorCode.NOT_BELONG_TO_CURRENT_CHAIN);
        }
        //如果本链不为发起链，验证CoinData中的主网主资产是否足够支付手续费
        if (chain.getChainId() != fromChainId || !isLocalCtx) {
            BigInteger feeTotalFrom = BigInteger.ZERO;
            for (CoinFrom coinFrom : coinFromList) {
                if (CommonUtil.isNulsAsset(coinFrom)) {
                    feeTotalFrom = feeTotalFrom.add(coinFrom.getAmount());
                }
            }
            BigInteger feeTotalTo = BigInteger.ZERO;
            for (CoinTo coinTo : coinToList) {
                if (CommonUtil.isNulsAsset(coinTo)) {
                    feeTotalTo = feeTotalTo.add(coinTo.getAmount());
                }
            }
            //本交易预计收取的手续费
            BigInteger targetFee = TransactionFeeCalculator.getCrossTxFee(txSize);
            //交易中已收取的手续费
            BigInteger actualFee = feeTotalFrom.subtract(feeTotalTo);
            if (BigIntegerUtils.isLessThan(actualFee, targetFee)) {
                chain.getLogger().error("手续费不足");
                throw new NulsException(NulsCrossChainErrorCode.INSUFFICIENT_FEE);
            }
        }
        return true;
    }
}

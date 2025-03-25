package io.nuls.crosschain.servive.impl;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.basic.Result;
import io.nuls.core.constant.TxStatusEnum;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import io.nuls.crosschain.base.model.dto.input.CoinDTO;
import io.nuls.crosschain.base.model.dto.input.CrossTxTransferDTO;
import io.nuls.crosschain.base.service.CrossChainService;
import io.nuls.common.NerveCoreConfig;
import io.nuls.crosschain.constant.NulsCrossChainConstant;
import io.nuls.crosschain.constant.NulsCrossChainErrorCode;
import io.nuls.crosschain.model.bo.BackOutAmount;
import io.nuls.crosschain.model.bo.Chain;
import io.nuls.crosschain.model.po.CtxStatusPO;
import io.nuls.crosschain.rpc.call.AccountCall;
import io.nuls.crosschain.rpc.call.ChainManagerCall;
import io.nuls.crosschain.rpc.call.TransactionCall;
import io.nuls.crosschain.srorage.*;
import io.nuls.crosschain.utils.CommonUtil;
import io.nuls.crosschain.utils.LoggerUtil;
import io.nuls.crosschain.utils.TxUtil;
import io.nuls.crosschain.utils.manager.ChainManager;
import io.nuls.crosschain.utils.manager.CoinDataManager;
import io.nuls.crosschain.utils.thread.CrossTxHandler;
import io.nuls.crosschain.utils.validator.CrossTxValidator;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import static io.nuls.crosschain.constant.NulsCrossChainConstant.CHAIN_ID_MIN;
import static io.nuls.crosschain.constant.NulsCrossChainErrorCode.*;
import static io.nuls.crosschain.constant.ParamConstant.*;

/**
 * Cross chain module default interface implementation class
 *
 * @author tag
 * @date 2019/4/9
 */
@Component
public class NulsCrossChainServiceImpl implements CrossChainService {
    @Autowired
    private ChainManager chainManager;

    @Autowired
    private NerveCoreConfig config;

    @Autowired
    private CoinDataManager coinDataManager;

    @Autowired
    private CrossTxValidator txValidator;

    @Autowired
    private SendHeightService sendHeightService;

    @Autowired
    private ConvertHashService convertHashService;

    @Autowired
    private CtxStateService ctxStateService;

    @Autowired
    private ConvertCtxService convertCtxService;

    @Autowired
    private CtxStatusService ctxStatusService;

    @Autowired
    private TotalOutAmountService totalOutAmountService;

    @Autowired
    private CommitedOtherCtxService otherCtxService;

    private Map<Integer, Set<NulsHash>> verifiedCtxMap = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public Result createCrossTx(Map<String, Object> params) {
        if (params == null) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        CrossTxTransferDTO crossTxTransferDTO = JSONUtils.map2pojo(params, CrossTxTransferDTO.class);
        int chainId = crossTxTransferDTO.getChainId();
        if (chainId <= CHAIN_ID_MIN) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(CHAIN_NOT_EXIST);
        }
        if (!chainManager.isCrossNetUseAble()) {
            chain.getLogger().info("Cross chain network networking exception！");
            return Result.getFailed(CROSS_CHAIN_NETWORK_UNAVAILABLE);
        }
        Transaction tx = new Transaction(config.getCrossCtxType());
        try {
            tx.setRemark(StringUtils.bytes(crossTxTransferDTO.getRemark()));
            tx.setTime(NulsDateUtils.getCurrentTimeSeconds());
            List<CoinFrom> coinFromList = coinDataManager.assemblyCoinFrom(chain, crossTxTransferDTO.getListFrom(), false);
            List<CoinTo> coinToList = coinDataManager.assemblyCoinTo(crossTxTransferDTO.getListTo(), chain);
            coinDataManager.verifyCoin(coinFromList, coinToList, chain);
            int txSize = tx.size();
            txSize += P2PHKSignature.SERIALIZE_LENGTH * (chain.getVerifierList().size());
            CoinData coinData = coinDataManager.getCrossCoinData(chain, coinFromList, coinToList, txSize, config.isMainNet());
            tx.setCoinData(coinData.serialize());
            tx.setHash(NulsHash.calcHash(tx.serializeForHash()));
            //autograph
            TransactionSignature transactionSignature = new TransactionSignature();
            List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
            List<String> signedAddressList = new ArrayList<>();
            for (CoinDTO coinDTO : crossTxTransferDTO.getListFrom()) {
                if (!signedAddressList.contains(coinDTO.getAddress())) {
                    P2PHKSignature p2PHKSignature = AccountCall.signDigest(coinDTO.getAddress(), coinDTO.getPassword(), tx.getHash().getBytes(),null);
                    p2PHKSignatures.add(p2PHKSignature);
                    signedAddressList.add(coinDTO.getAddress());
                }
            }
            if (!txValidator.coinDataValid(chain, coinData, tx.size())) {
                chain.getLogger().error("Cross chain transactionsCoinDataVerification failed！\n\n");
                return Result.getFailed(COINDATA_VERIFY_FAIL);
            }
            transactionSignature.setP2PHKSignatures(p2PHKSignatures);
            tx.setTransactionSignature(transactionSignature.serialize());

            if (!TransactionCall.sendTx(chain, RPCUtil.encode(tx.serialize()))) {
                chain.getLogger().error("Cross chain transaction sending transaction module failed\n\n");
                throw new NulsException(INTERFACE_CALL_FAILED);
            }
            Map<String, Object> result = new HashMap<>(2);
            result.put(TX_HASH, tx.getHash().toHex());
            return Result.getSuccess(SUCCESS).setData(result);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        } catch (IOException e) {
            Log.error(e);
            return Result.getFailed(SERIALIZE_ERROR);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result newApiModuleCrossTx(Map<String, Object> params) {
        if (params.get(CHAIN_ID) == null || params.get(TX) == null) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        int chainId = (Integer) params.get(CHAIN_ID);
        if (chainId <= 0) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(CHAIN_NOT_EXIST);
        }
        String txStr = (String) params.get(TX);
        try {
            Transaction tx = new Transaction();
            tx.parse(RPCUtil.decode(txStr), 0);
            CoinData coinData = tx.getCoinDataInstance();
            if (!txValidator.coinDataValid(chain, coinData, tx.size())) {
                chain.getLogger().error("Cross chain transactionsCoinDataVerification failed！\n\n");
                return Result.getFailed(COINDATA_VERIFY_FAIL);
            }

            if (!TransactionCall.sendTx(chain, RPCUtil.encode(tx.serialize()))) {
                chain.getLogger().error("Cross chain transaction sending transaction module failed\n\n");
                throw new NulsException(INTERFACE_CALL_FAILED);
            }

            Map<String, Object> result = new HashMap<>(2);
            result.put(TX_HASH, tx.getHash().toHex());
            result.put("success", true);
            return Result.getSuccess(SUCCESS).setData(result);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        } catch (IOException e) {
            Log.error(e);
            return Result.getFailed(SERIALIZE_ERROR);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result validCrossTx(Map<String, Object> params) {
        if (params.get(CHAIN_ID) == null || params.get(TX) == null) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        int chainId = (Integer) params.get(CHAIN_ID);
        if (chainId <= 0) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(CHAIN_NOT_EXIST);
        }
        String txStr = (String) params.get(TX);
        try {
            Transaction transaction = new Transaction();
            transaction.parse(RPCUtil.decode(txStr), 0);
            if (!txValidator.validateTx(chain, transaction, null)) {
                chain.getLogger().error("Cross chain transaction verification failed,Hash:{}\n", transaction.getHash().toHex());
                return Result.getFailed(TX_DATA_VALIDATION_ERROR);
            }
            Map<String, Object> validResult = new HashMap<>(2);
            validResult.put(VALUE, true);
            chain.getLogger().info("Cross chain transaction verification successful,Hash:{}\n", transaction.getHash().toHex());
            return Result.getSuccess(SUCCESS).setData(validResult);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return Result.getFailed(e.getErrorCode());
        } catch (IOException e) {
            Log.error(e);
            return Result.getFailed(SERIALIZE_ERROR);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean commitCrossTx(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return false;
        }
        try {
            List<NulsHash> convertHashList = new ArrayList<>();
            List<NulsHash> ctxStatusList = new ArrayList<>();
            List<NulsHash> otherCtxList = new ArrayList<>();
            for (Transaction ctx : txs) {
                NulsHash ctxHash = ctx.getHash();
                if (verifiedCtxMap.get(chainId) != null) {
                    verifiedCtxMap.get(chainId).remove(ctxHash);
                }
                CoinData coinData = ctx.getCoinDataInstance();
                int fromChainId = AddressTool.getChainIdByAddress(coinData.getFrom().get(0).getAddress());
                int toChainId = AddressTool.getChainIdByAddress(coinData.getTo().get(0).getAddress());
                if (chainId == toChainId) {
                    NulsHash convertHash = ctxHash;
                    if (!config.isMainNet()) {
                        convertHash = TxUtil.friendConvertToMain(chain, ctx, TxType.CROSS_CHAIN).getHash();
                    }
                    if (!convertHashService.save(convertHash, ctxHash, chainId)) {
                        rollbackCtx(convertHashList, ctxStatusList, otherCtxList, chainId);
                        return false;
                    }
                    convertHashList.add(convertHash);
                    if (!otherCtxService.save(convertHash, ctx, chainId)) {
                        rollbackCtx(convertHashList, ctxStatusList, otherCtxList, chainId);
                        return false;
                    }
                    otherCtxList.add(convertHash);
                } else {
                    if (!config.isMainNet()) {
                        NulsHash convertHash = TxUtil.friendConvertToMain(chain, ctx, TxType.CROSS_CHAIN).getHash();
                        if (!convertHashService.save(convertHash, ctxHash, chainId)) {
                            rollbackCtx(convertHashList, ctxStatusList, otherCtxList, chainId);
                            return false;
                        }
                        convertHashList.add(convertHash);
                    }
                    //If the current chain is not the initiating chain, then the main network intermediary chain needs to clear the signature before signing the transaction pair Byzantine, to avoid other chains from failing to obtain transactions from this chain
                    if (chainId != fromChainId) {
                        ctx.setTransactionSignature(null);
                        if (!otherCtxService.save(ctxHash, ctx, chainId)) {
                            rollbackCtx(convertHashList, ctxStatusList, otherCtxList, chainId);
                            return false;
                        }
                        otherCtxList.add(ctxHash);
                    }
                    //Roll back and repackage. If the current cross chain transaction has been processed, there is no need for duplicate processing
                    CtxStatusPO ctxStatusPO = ctxStatusService.get(ctxHash, chainId);
                    if (ctxStatusPO != null) {
                        if (ctxStatusPO.getStatus() == TxStatusEnum.CONFIRMED.getStatus()) {
                            chain.getLogger().info("The cross chain transfer transaction has been processed before and does not need to be processed again：{}", ctxHash.toHex());
                            continue;
                        }
                    }
                    ctxStatusList.add(ctxHash);
                    chain.getLogger().debug("Cross chain transaction submission completed, perform Byzantine verification on cross chain transfer transactions：{}", ctxHash.toHex());
                    //If this chain is the main network, notify the cross chain management module to initiate and receive chain asset changes
                    if (config.isMainNet()) {
                        List<String> txStrList = new ArrayList<>();
                        for (Transaction tx : txs) {
                            txStrList.add(RPCUtil.encode(tx.serialize()));
                        }
                        String headerStr = RPCUtil.encode(blockHeader.serialize());
                        ChainManagerCall.ctxAssetCirculateCommit(chainId, txStrList, headerStr);
                    }
                    //Initiate Byzantine verification
                    //Clear transaction original signature
                    ctx.setTransactionSignature(null);
                    chain.getCrossTxThreadPool().execute(new CrossTxHandler(chain, ctx,blockHeader, syncStatus));
                }
                //Maintain the total number of transfers handled here
                BackOutAmount backOutAmount = getBackAmount(ctx, chain);
                if (null == backOutAmount) {
                    continue;
                }
                if (backOutAmount.getBack().compareTo(BigInteger.ZERO) > 0) {
                    this.totalOutAmountService.addBackAmount(backOutAmount.getChainId(), backOutAmount.getAssetId(), backOutAmount.getBack());
                } else if (backOutAmount.getOut().compareTo(BigInteger.ZERO) > 0) {
                    this.totalOutAmountService.addOutAmount(backOutAmount.getChainId(), backOutAmount.getAssetId(), backOutAmount.getOut());
                }
            }
//            chain.getLogger().info("height：{} Cross chain transaction submission completed\n", blockHeader.getHeight());
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean rollbackCrossTx(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return false;
        }
        try {
            for (Transaction ctx : txs) {
                CoinData coinData = ctx.getCoinDataInstance();
                int fromChainId = AddressTool.getChainIdByAddress(coinData.getFrom().get(0).getAddress());
                int toChainId = AddressTool.getChainIdByAddress(coinData.getTo().get(0).getAddress());
                NulsHash ctxHash = ctx.getHash();
                if (chainId == fromChainId) {
                    CtxStatusPO ctxStatusPO = ctxStatusService.get(ctxHash, chainId);
                    if (ctxStatusPO != null) {
                        if (ctxStatusPO.getStatus() == TxStatusEnum.CONFIRMED.getStatus()) {
                            chain.getLogger().info("The cross chain transfer transaction has been processed and does not need to be rolled back：{}", ctxHash.toHex());
                            continue;
                        }
                    }
                    if (!ctxStatusService.delete(ctxHash, chainId) || !convertHashService.delete(ctxHash, chainId)) {
                        return false;
                    }
                } else if (chainId == toChainId) {
                    NulsHash convertHash = ctxHash;
                    if (!config.isMainNet() && ctx.getType() == config.getCrossCtxType()) {
                        convertHash = TxUtil.friendConvertToMain(chain, ctx, TxType.CROSS_CHAIN).getHash();
                    }
                    if (!convertHashService.delete(convertHash, chainId)) {
                        return false;
                    }
                } else {
                    CtxStatusPO ctxStatusPO = ctxStatusService.get(ctxHash, chainId);
                    if (ctxStatusPO != null) {
                        if (ctxStatusPO.getStatus() == TxStatusEnum.CONFIRMED.getStatus()) {
                            chain.getLogger().info("The cross chain transfer transaction has been processed and does not need to be rolled back：{}", ctxHash.toHex());
                            continue;
                        }
                    }
                    if (!ctxStatusService.delete(ctxHash, chainId) || !convertHashService.delete(ctxHash, chainId)) {
                        return false;
                    }
                }
            }
            //If the main network notifies the cross chain management module to initiate and receive chain asset changes
            if (config.isMainNet()) {
                List<String> txStrList = new ArrayList<>();
                for (Transaction tx : txs) {
                    txStrList.add(RPCUtil.encode(tx.serialize()));
                }
                String headerStr = RPCUtil.encode(blockHeader.serialize());
                ChainManagerCall.ctxAssetCirculateRollback(chainId, txStrList, headerStr);
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> crossTxBatchValid(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        Chain chain = chainManager.getChainMap().get(chainId);
        Map<String, Object> result = new HashMap<>(2);
        if (chain == null) {
            result.put("txList", txs);
            result.put("errorCode", NulsCrossChainErrorCode.CHAIN_NOT_EXIST.getCode());
            return result;
        }
        if (!verifiedCtxMap.keySet().contains(chainId)) {
            verifiedCtxMap.put(chainId, new HashSet<>());
        }
        Set<NulsHash> verifiedCtxSet = verifiedCtxMap.get(chainId);
        List<Transaction> invalidCtxList = new ArrayList<>();
        String errorCode = null;
        Map<String, BigInteger> totalBackAmountMap = new HashMap<>();
        for (Transaction ctx : txs) {
            NulsHash ctxHash = ctx.getHash();
            try {
                boolean ok = verifiedCtxSet.contains(ctxHash);
                if (!ok) {
                    ok = txValidator.validateTx(chain, ctx, blockHeader);
                }
                BackOutAmount amount = getBackAmount(ctx, chain);

                if (null != amount && ok && amount.getBack().compareTo(BigInteger.ZERO) > 0) {
                    String key = amount.getChainId() + "_" + amount.getAssetId();
                    BigInteger totalBackAmount = totalBackAmountMap.computeIfAbsent(key, val -> BigInteger.ZERO);
                    BigInteger tempTotalBackAmount = amount.getBack().add(totalBackAmount);
                    BigInteger totalOutAmount = this.totalOutAmountService.getOutTotalAmount(amount.getChainId(), amount.getAssetId());
                    if (tempTotalBackAmount.compareTo(totalOutAmount) > 0) {
                        //If it exceeds the total amount, it will not pass
                        ok = false;
                    } else {
                        //If it does not exceed, continue to accumulate
                        totalBackAmountMap.put(key, tempTotalBackAmount);
                    }
                }
                if (!ok) {
                    invalidCtxList.add(ctx);
                } else {
                    verifiedCtxSet.add(ctxHash);
                }

            } catch (NulsException e) {
                invalidCtxList.add(ctx);
                chain.getLogger().error("Cross-Chain Transaction Verification Failure");
                chain.getLogger().error(e);
                errorCode = e.getErrorCode().getCode();
            } catch (IOException io) {
                invalidCtxList.add(ctx);
                chain.getLogger().error("Cross-Chain Transaction Verification Failure");
                chain.getLogger().error(io);
                errorCode = NulsCrossChainErrorCode.SERIALIZE_ERROR.getCode();
            }
        }
        result.put("txList", invalidCtxList);
        result.put("errorCode", errorCode);
        return result;
    }

    private BackOutAmount getBackAmount(Transaction ctx, Chain chain) throws NulsException {
        BackOutAmount backOutAmount = null;
        CoinData coinData = new CoinData();
        coinData.parse(ctx.getCoinData(), 0);
        CoinTo coinTo = coinData.getTo().get(0);
        if (chain.getChainId() != coinTo.getAssetsChainId()) {
            //Only handle assets in this chain
            return null;
        }
        byte[] toAddress = coinTo.getAddress();
        int chainId = AddressTool.getChainIdByAddress(toAddress);
        backOutAmount = new BackOutAmount(coinTo.getAssetsChainId(), coinTo.getAssetsId());
        if (chain.getChainId() == chainId) {
            //Guess it's a reversal
            backOutAmount.addBack(coinTo.getAmount());
        } else {
            backOutAmount.addOut(coinTo.getAmount());
        }
        //It should be verifiedfromaddress
        CoinFrom from = coinData.getFrom().get(0);
        int fromChainId = AddressTool.getChainIdByAddress(from.getAddress());
        if (fromChainId == chainId) {
            throw new NulsException(TO_ADDRESS_ERROR);
        }
        return backOutAmount;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result getCrossTxState(Map<String, Object> params) {
        if (params.get(CHAIN_ID) == null || params.get(TX_HASH) == null) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        int chainId = (Integer) params.get(CHAIN_ID);
        if (chainId <= 0) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(CHAIN_NOT_EXIST);
        }
        String hashStr = (String) params.get(TX_HASH);
        Map<String, Object> result = new HashMap<>(2);
        NulsHash requestHash = NulsHash.fromHex(hashStr);
        byte statisticsResult = TxUtil.getCtxState(chain, requestHash);
        result.put(VALUE, statisticsResult);
        return Result.getSuccess(SUCCESS).setData(result);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result getRegisteredChainInfoList(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>(2);
        LoggerUtil.commonLog.info("------Obtain cross chain asset information---Total length：" + chainManager.getRegisteredCrossChainList().size());
        result.put(LIST, chainManager.getRegisteredCrossChainList());
        return Result.getSuccess(SUCCESS).setData(result);
    }

    @Override
    public int getCrossChainTxType() {
        return config.getCrossCtxType();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Result getByzantineCount(Map<String, Object> params) {
        if (params.get(CHAIN_ID) == null) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        int chainId = (Integer) params.get(CHAIN_ID);
        if (chainId <= 0) {
            return Result.getFailed(PARAMETER_ERROR);
        }
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return Result.getFailed(CHAIN_NOT_EXIST);
        }
        Map<String, Object> result = new HashMap<>(2);
        result.put(VALUE, config.getByzantineRatio() * CommonUtil.getCurrentPackAddressList(chain).size() / NulsCrossChainConstant.MAGIC_NUM_100);
        return Result.getSuccess(SUCCESS).setData(result);
    }

    private void rollbackCtx(List<NulsHash> convertHashList, List<NulsHash> ctxStatusList, List<NulsHash> otherCtxList, int chainId) {
        for (NulsHash convertHash : convertHashList) {
            convertHashService.delete(convertHash, chainId);
        }
        for (NulsHash otherHash : otherCtxList) {
            otherCtxService.delete(otherHash, chainId);
        }
        for (NulsHash ctxStatusHash : ctxStatusList) {
            CtxStatusPO ctxStatusPO = ctxStatusService.get(ctxStatusHash, chainId);
            ctxStatusPO.setStatus(TxStatusEnum.UNCONFIRM.getStatus());
            ctxStatusService.save(ctxStatusHash, ctxStatusPO, chainId);
        }
    }
}

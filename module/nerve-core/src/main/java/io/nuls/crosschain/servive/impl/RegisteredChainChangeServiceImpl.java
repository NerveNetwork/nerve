package io.nuls.crosschain.servive.impl;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.crosschain.base.model.bo.ChainInfo;
import io.nuls.crosschain.base.model.bo.txdata.RegisteredChainChangeData;
import io.nuls.crosschain.base.model.bo.txdata.RegisteredChainMessage;
import io.nuls.crosschain.base.service.RegisteredChainChangeService;
import io.nuls.crosschain.base.utils.enumeration.ChainInfoChangeType;
import io.nuls.common.NerveCoreConfig;
import io.nuls.crosschain.constant.NulsCrossChainConstant;
import io.nuls.crosschain.constant.NulsCrossChainErrorCode;
import io.nuls.crosschain.model.bo.Chain;
import io.nuls.crosschain.rpc.call.LedgerCall;
import io.nuls.crosschain.srorage.RegisteredCrossChainService;
import io.nuls.crosschain.utils.CommonUtil;
import io.nuls.crosschain.utils.TxUtil;
import io.nuls.crosschain.utils.manager.ChainManager;

import java.io.IOException;
import java.util.*;

@Component
public class RegisteredChainChangeServiceImpl implements RegisteredChainChangeService {
    @Autowired
    private ChainManager chainManager;

    @Autowired
    private NerveCoreConfig config;

    @Autowired
    private RegisteredCrossChainService registeredCrossChainService;
    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        List<Transaction> invalidTxList = new ArrayList<>();
        Map<String, Object> result = new HashMap<>(2);
        String errorCode = null;
        Chain chain = chainManager.getChainMap().get(chainId);
        if(config.isMainNet()){
            invalidTxList.addAll(txs);
            result.put("errorCode", NulsCrossChainErrorCode.DATA_ERROR.getCode());
            return result;
        }
        ChainInfo chainInfo = chainManager.getChainInfo(config.getMainChainId());
        if (chainInfo == null || chainInfo.getVerifierList().isEmpty()) {
            invalidTxList.addAll(txs);
            result.put("errorCode", NulsCrossChainErrorCode.CHAIN_UNREGISTERED_VERIFIER.getCode());
            chain.getLogger().error("The main network verifier information has not been initialized on this chain yet");
            return result;
        }
        int minPassCount = CommonUtil.getByzantineCount(chain, chainInfo.getVerifierList().size());
        for (Transaction verifierChangeTx : txs) {
            try {
                if (!SignatureUtil.validateCtxSignture(verifierChangeTx)) {
                    chain.getLogger().info("Main network protocol cross chain transaction signature verification failed！");
                    throw new NulsException(NulsCrossChainErrorCode.SIGNATURE_ERROR);
                }
                if (!TxUtil.signByzantineVerify(chain, verifierChangeTx, new ArrayList<>(chainInfo.getVerifierList()), minPassCount,config.getMainChainId())) {
                    chain.getLogger().info("Signature Byzantine verification failed！");
                    throw new NulsException(NulsCrossChainErrorCode.CTX_SIGN_BYZANTINE_FAIL);
                }
            } catch (NulsException e) {
                try {
                    chain.getLogger().error(HexUtil.encode(verifierChangeTx.serialize()));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                chain.getLogger().error(e);
                errorCode = e.getErrorCode().getCode();
                invalidTxList.add(verifierChangeTx);
            }
        }
        result.put("txList", invalidTxList);
        result.put("errorCode", errorCode);
        return result;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return false;
        }
        for (Transaction crossChainChangeTx : txs) {
            try {
                RegisteredChainChangeData txData = new RegisteredChainChangeData();
                txData.parse(crossChainChangeTx.getTxData(), 0);
                Set<String> verifierSet;
                int mainByzantineRatio;
                int maxSignatureCount;
                if(txData.getType() == ChainInfoChangeType.INIT_REGISTER_CHAIN.getType()){
                    if(chainManager.getRegisteredCrossChainList() != null && !chainManager.getRegisteredCrossChainList().isEmpty()){
                        ChainInfo chainInfo = chainManager.getChainInfo(config.getMainChainId());
                        verifierSet = chainInfo.getVerifierList();
                        mainByzantineRatio = chainInfo.getSignatureByzantineRatio();
                        maxSignatureCount = chainInfo.getMaxSignatureCount();
                    }else{
                        verifierSet = new HashSet<>(Arrays.asList(config.getVerifiers().split(NulsCrossChainConstant.VERIFIER_SPLIT)));
                        mainByzantineRatio = config.getMainByzantineRatio();
                        maxSignatureCount = config.getMaxSignatureCount();
                    }
                    for (ChainInfo chainInfo : txData.getChainInfoList()){
                        if(chainInfo.getChainId() == config.getMainChainId()){
                            chainInfo.setVerifierList(verifierSet);
                            chainInfo.setMaxSignatureCount(maxSignatureCount);
                            chainInfo.setSignatureByzantineRatio(mainByzantineRatio);
                        }
                    }
                    RegisteredChainMessage registeredChainMessage = new RegisteredChainMessage(txData.getChainInfoList());
                    registeredCrossChainService.save(registeredChainMessage);
                    chainManager.setRegisteredCrossChainList(registeredChainMessage.getChainInfoList());
                    chainManager.setCrossNetUseAble(true);
                    chain.getLogger().info("Registered cross chain chain information initialization completed");
                    LedgerCall.registerAsset(chain, registeredChainMessage.getChainInfoList(), blockHeader.getHeight());
                }else{
                    RegisteredChainMessage registeredChainMessage = registeredCrossChainService.get();
                    if(registeredChainMessage == null){
                        continue;
                    }
                    if(txData.getChainInfoList() == null || txData.getChainInfoList().isEmpty()){
                        registeredChainMessage.getChainInfoList().removeIf(chainInfo -> chainInfo.getChainId() == txData.getRegisterChainId());
                        registeredCrossChainService.save(registeredChainMessage);
                        chainManager.setRegisteredCrossChainList(registeredChainMessage.getChainInfoList());
                        chain.getLogger().info("Cross chain deregistration,chainId:{}",txData.getRegisterChainId());
                    }else{
                        for(ChainInfo chainInfo : txData.getChainInfoList()){
                            if(chainInfo.getChainId() == config.getMainChainId()){
                                ChainInfo oldChainInfo = chainManager.getChainInfo(config.getMainChainId());
                                chainInfo.setVerifierList(oldChainInfo.getVerifierList());
                                chainInfo.setSignatureByzantineRatio(oldChainInfo.getSignatureByzantineRatio());
                                chainInfo.setMaxSignatureCount(oldChainInfo.getMaxSignatureCount());
                            }
                            registeredChainMessage.addChainInfo(chainInfo);
                            chain.getLogger().info("Registered cross chain chain information has changed,chainId:{}" ,chainInfo.getChainId());
                        }
                        registeredCrossChainService.save(registeredChainMessage);
                        chainManager.setRegisteredCrossChainList(registeredChainMessage.getChainInfoList());
                        LedgerCall.registerAsset(chain, txData.getChainInfoList(), blockHeader.getHeight());
                    }
                }
            }catch (Exception e){
                chain.getLogger().error(e);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return true;
    }
}

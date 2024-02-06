package io.nuls.crosschain.servive.impl;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.crosschain.base.model.bo.ChainInfo;
import io.nuls.crosschain.base.model.bo.txdata.RegisteredChainMessage;
import io.nuls.crosschain.base.model.bo.txdata.ResetChainInfoData;
import io.nuls.crosschain.base.service.ResetChainService;
import io.nuls.crosschain.srorage.RegisteredCrossChainService;
import io.nuls.common.NerveCoreConfig;
import io.nuls.crosschain.constant.NulsCrossChainErrorCode;
import io.nuls.crosschain.model.bo.Chain;
import io.nuls.crosschain.utils.manager.ChainManager;

import java.util.*;

@Component
public class ResetChainServiceImpl implements ResetChainService {


    @Autowired
    private ChainManager chainManager;

    @Autowired
    private NerveCoreConfig config;
    @Autowired
    private RegisteredCrossChainService registeredCrossChainService;

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {

        //todo Verify transactions signed by seed nodes（Do we need multiple seed node signatures）
        List<Transaction> errorList = new ArrayList<>();
        Map<String, Object> rtData = new HashMap<>(2);
        rtData.put("errorCode", "");
        rtData.put("txList", errorList);

        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            errorList.addAll(txs);
            chain.getLogger().error("Chain id is invaild");
            rtData.put("errorCode", NulsCrossChainErrorCode.DATA_ERROR.getCode());
            return rtData;
        }

        try {
            boolean success = true;
            ErrorCode errorCode = NulsCrossChainErrorCode.DATA_ERROR;
            for (Transaction tx : txs) {
                String txHash = tx.getHash().toHex();
                //todo Verify transactions signed by seed nodes
                tx.getTransactionSignature();
                Set<String> set = SignatureUtil.getAddressFromTX(tx, chainId);
                for (String txAddress : set) {
                    if (!config.getSeedNodeSet().contains(txAddress)) {
                        success = false;
                        errorCode = NulsCrossChainErrorCode.MUST_SEED_ADDRESS_SIGN;
                        break;
                    }
                }
                ResetChainInfoData txData = new ResetChainInfoData();
                txData.parse(tx.getTxData(), 0);

                ChainInfo chainInfo = JSONUtils.json2pojo(txData.getJson(), ChainInfo.class);
                if (!validate(chainInfo)) {
                    success = false;
                    errorCode = NulsCrossChainErrorCode.DATA_ERROR;
                }

                if (success) {
                    chain.getLogger().debug("txHash = {},chainId={} reset validate success!", txHash, chainId);
                } else {
                    rtData.put("errorCode", errorCode);
                    chain.getLogger().error("txHash = {},chainId={} reset validate fail!", txHash, chainId);
                    errorList.add(tx);
                }
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            throw new RuntimeException(e);
        }
        return rtData;
    }

    private boolean validate(ChainInfo chainInfo) {
        if (null == chainInfo) {
            return false;
        }
        if (chainInfo.getChainId() < 0) {
            return false;
        }
        if (StringUtils.isBlank(chainInfo.getChainName())) {
            return false;
        }
        if (StringUtils.isBlank(chainInfo.getAddressPrefix())) {
            return false;
        }
        if (chainInfo.getAssetInfoList() == null || chainInfo.getAssetInfoList().isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        // analysischainInformation, if available, overwrite; if not available, store（Cache and Database）
        Chain chain = chainManager.getChainMap().get(chainId);
        if (chain == null) {
            return false;
        }
        for (Transaction resetTx : txs) {
            try {
                ResetChainInfoData txData = new ResetChainInfoData();
                txData.parse(resetTx.getTxData(), 0);
                ChainInfo chainInfo = JSONUtils.json2pojo(txData.getJson(), ChainInfo.class);

                RegisteredChainMessage registeredChainMessage = registeredCrossChainService.get();
                if (registeredChainMessage == null) {
                    registeredChainMessage = new RegisteredChainMessage(new ArrayList<>());
                }


                List<ChainInfo> list = registeredChainMessage.getChainInfoList();
                for (int i = 0; i < list.size(); i++) {
                    ChainInfo old = list.get(i);
                    if (old.getChainId() == chainInfo.getChainId()) {
                        list.remove(i);
                        break;
                    }
                }


                registeredChainMessage.addChainInfo(chainInfo);
                chain.getLogger().info("reset cross chain chain information has changed,chainId:{}", chainInfo.getChainId());


                registeredCrossChainService.save(registeredChainMessage);
                chainManager.setRegisteredCrossChainList(registeredChainMessage.getChainInfoList());

            } catch (Exception e) {
                chain.getLogger().error(e);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        //todo reverse
        return true;
    }
}

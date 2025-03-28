package io.nuls.crosschain.utils;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.BaseNulsData;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Coin;
import io.nuls.base.data.Transaction;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.model.DoubleUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.crosschain.base.model.bo.txdata.VerifierChangeData;
import io.nuls.common.NerveCoreConfig;
import io.nuls.crosschain.constant.NulsCrossChainConstant;
import io.nuls.crosschain.constant.NulsCrossChainErrorCode;
import io.nuls.crosschain.model.bo.Chain;
import io.nuls.crosschain.rpc.call.ConsensusCall;
import io.nuls.crosschain.utils.manager.ChainManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Cross chain module basic tool class
 *
 * @author: tag
 * @date: 2019/4/12
 */
@Component
public class CommonUtil {
    @Autowired
    private static NerveCoreConfig config;
    @Autowired
    private static ChainManager chainManager;

    /**
     * RPCUtil Deserialization
     *
     * @param data
     * @param clazz
     * @param <T>
     * @return
     * @throws NulsException
     */
    public static <T> T getInstanceRpcStr(String data, Class<? extends BaseNulsData> clazz) throws NulsException {
        if (StringUtils.isBlank(data)) {
            throw new NulsException(NulsCrossChainErrorCode.DATA_NOT_FOUND);
        }
        return getInstance(RPCUtil.decode(data), clazz);
    }

    public static <T> T getInstance(byte[] bytes, Class<? extends BaseNulsData> clazz) throws NulsException {
        if (null == bytes || bytes.length == 0) {
            throw new NulsException(NulsCrossChainErrorCode.DATA_NOT_FOUND);
        }
        try {
            BaseNulsData baseNulsData = clazz.getDeclaredConstructor().newInstance();
            baseNulsData.parse(new NulsByteBuffer(bytes));
            return (T) baseNulsData;
        } catch (NulsException e) {
            Log.error(e);
            throw new NulsException(NulsCrossChainErrorCode.DESERIALIZE_ERROR);
        } catch (Exception e) {
            Log.error(e);
            throw new NulsException(NulsCrossChainErrorCode.DESERIALIZE_ERROR);
        }
    }

    public static boolean isNulsAsset(Coin coin) {
        return isNulsAsset(coin.getAssetsChainId(), coin.getAssetsId());
    }

    public static boolean isNulsAsset(int chainId, int assetId) {
        if (chainId == config.getMainChainId()
                && assetId == config.getMainAssetId()) {
            return true;
        }
        return false;
    }

    public static boolean isLocalAsset(Coin coin) {
        return isLocalAsset(coin.getAssetsChainId(), coin.getAssetsId());
    }

    public static boolean isLocalAsset(int chainId, int assetId) {

        if (chainId == config.getChainId()
                && assetId == config.getAssetId()) {
            return true;
        }
        return false;
    }

    public static List<P2PHKSignature> getMisMatchSigns(Chain chain, TransactionSignature transactionSignature, List<String> addressList) {
        List<P2PHKSignature> misMatchSignList = new ArrayList<>();
        transactionSignature.setP2PHKSignatures(transactionSignature.getP2PHKSignatures().parallelStream().distinct().collect(Collectors.toList()));
        Iterator<P2PHKSignature> iterator = transactionSignature.getP2PHKSignatures().iterator();
        Set<String> signedList = new HashSet<>();
        String validAddress;
        P2PHKSignature signature;
        while (iterator.hasNext()) {
            signature = iterator.next();
            boolean isMatchSign = false;
            validAddress = AddressTool.getAddressString(signature.getPublicKey(), chain.getChainId());
            if (signedList.contains(validAddress)) {
                iterator.remove();
                break;
            }
            for (String address : addressList) {
                if (address.equals(validAddress)) {
                    signedList.add(address);
                    isMatchSign = true;
                    break;
                }
            }
            if (!isMatchSign) {
                misMatchSignList.add(signature);
                iterator.remove();
            }
        }
        chain.getLogger().info("Verification successful account list,signedList:{}", signedList);
        return misMatchSignList;
    }

    /**
     * Obtain the current number of Byzantine signatures
     */
    @SuppressWarnings("unchecked")
    public static int getByzantineCount(Chain chain, int agentCount) {
        int byzantineRatio = chain.getConfig().getByzantineRatio();
        int minPassCount = getRealMinCount(chain,agentCount,byzantineRatio);
        chain.getLogger().debug("The current number of consensus nodes is：{},The minimum number of signatures is:{}", agentCount, minPassCount);
        return minPassCount;
    }

    /**
     * Obtain the current number of Byzantine signatures
     */
    @SuppressWarnings("unchecked")
    public static int getByzantineCount(Transaction ctx, List<String> packAddressList, Chain chain) throws NulsException {
        int agentCount = packAddressList.size();
        int chainId = chain.getChainId();
        int byzantineRatio = chain.getConfig().getByzantineRatio();
        if (ctx.getType() == TxType.VERIFIER_CHANGE) {
            VerifierChangeData verifierChangeData = new VerifierChangeData();
            verifierChangeData.parse(ctx.getTxData(), 0);
            if (verifierChangeData.getCancelAgentList() != null) {
                agentCount += verifierChangeData.getCancelAgentList().size();
            }
            if (verifierChangeData.getRegisterAgentList() != null) {
                agentCount -= verifierChangeData.getRegisterAgentList().size();
            }
        } else if (ctx.getType() == config.getCrossCtxType()) {
            int fromChainId = AddressTool.getChainIdByAddress(ctx.getCoinDataInstance().getFrom().get(0).getAddress());
            int toChainId = AddressTool.getChainIdByAddress(ctx.getCoinDataInstance().getTo().get(0).getAddress());
            if (chainId == fromChainId || (chainId != toChainId && config.isMainNet())) {
                byzantineRatio += NulsCrossChainConstant.FAULT_TOLERANT_RATIO;
                if (byzantineRatio > NulsCrossChainConstant.MAGIC_NUM_100) {
                    byzantineRatio = NulsCrossChainConstant.MAGIC_NUM_100;
                }
            }
        }
        int minPassCount = getRealMinCount(chain,agentCount,byzantineRatio);
        chain.getLogger().debug("The current number of consensus nodes is：{},The minimum number of signatures is:{}", agentCount, minPassCount);
        return minPassCount;
    }

    /**
     * Obtain the current number of Byzantine signatures
     */
    @SuppressWarnings("unchecked")
    public static int getByzantineCount(List<String> packAddressList, Chain chain, boolean isFromChain) {
        int agentCount = packAddressList.size();
        int byzantineRatio = chain.getConfig().getByzantineRatio();
        if (isFromChain) {
            byzantineRatio += NulsCrossChainConstant.FAULT_TOLERANT_RATIO;
            if (byzantineRatio > NulsCrossChainConstant.MAGIC_NUM_100) {
                byzantineRatio = NulsCrossChainConstant.MAGIC_NUM_100;
            }
        }
        int minPassCount = getRealMinCount(chain,agentCount,byzantineRatio);
        chain.getLogger().debug("The current number of consensus nodes is：{},The minimum number of signatures is:{}", agentCount, minPassCount);
        return minPassCount;
    }

    /**
     * Obtain the current consensus address account
     */
    @SuppressWarnings("unchecked")
    public static List<String> getCurrentPackAddressList(Chain chain) {
        Map packerInfo = ConsensusCall.getPackerInfo(chain);
        return (List<String>) packerInfo.get("packAddressList");
    }

    private static int getRealMinCount(Chain chain, int agentCount, int byzantineRatio) {

        BlockHeader lastBlockHeader = chainManager.getChainHeaderMap().get(chain.getChainId());
        if(lastBlockHeader == null){
            Log.error("Data abnormality, unable to obtain the current height");
            return getOldMinCount( agentCount, byzantineRatio);
        }
        //version1.6.0This verification was not conducted before
        long bestHeight = lastBlockHeader.getHeight();

        if (0 == config.getVersion1_38_0_height() || bestHeight < config.getVersion1_38_0_height()) {
            Log.info("version is 1.37.0");
            return getOldMinCount( agentCount, byzantineRatio);
        }
        Log.info("version is 1.38.0");
        double val = DoubleUtils.div(agentCount * byzantineRatio, 100);
        int minPassCount = (int) Math.ceil(val);

        if (minPassCount == 0) {
            minPassCount = 1;
        }
        return minPassCount;
    }

    private static int getOldMinCount(int agentCount, int byzantineRatio) {
        int minPassCount = agentCount * byzantineRatio / NulsCrossChainConstant.MAGIC_NUM_100;
        if (minPassCount == 0) {
            minPassCount = 1;
        }
        return minPassCount;
    }
}

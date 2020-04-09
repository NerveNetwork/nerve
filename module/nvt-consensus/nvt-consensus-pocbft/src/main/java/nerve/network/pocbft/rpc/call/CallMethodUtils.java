package nerve.network.pocbft.rpc.call;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.*;
import io.nuls.base.signture.BlockSignature;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.MessageUtil;
import io.nuls.core.rpc.model.message.Request;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.core.rpc.util.NulsDateUtils;
import nerve.network.pocbft.constant.ConsensusConstant;
import nerve.network.pocbft.constant.ConsensusErrorCode;
import nerve.network.pocbft.constant.ParameterConstant;
import nerve.network.pocbft.model.bo.Chain;
import nerve.network.pocbft.utils.compare.BlockHeaderComparator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nerve.network.pocbft.constant.CommandConstant.*;
import static nerve.network.pocbft.constant.ParameterConstant.*;

/**
 * 公共远程方法调用工具类
 * Common Remote Method Call Tool Class
 *
 * @author: Jason
 * 2018/12/26
 */
public class CallMethodUtils {
    public static final long MIN_PACK_SURPLUS_TIME = 600;
    public static final long CALL_BACK_HANDLE_TIME = 200;

    /**
     * 账户验证
     * account validate
     *
     * @param chainId
     * @param address
     * @param password
     * @return validate result
     */
    public static HashMap accountValid(int chainId, String address, String password) throws NulsException {
        try {
            Map<String, Object> callParams = new HashMap<>(4);
            callParams.put(PARAM_CHAIN_ID, chainId);
            callParams.put(PARAM_ADDRESS, address);
            callParams.put(PARAM_PASSWORD, password);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, CALL_AC_GET_PRIKEY_BY_ADDRESS, callParams);
            if (!cmdResp.isSuccess()) {
                throw new NulsException(ConsensusErrorCode.ACCOUNT_VALID_ERROR);
            }
            HashMap callResult = (HashMap) ((HashMap) cmdResp.getResponseData()).get(CALL_AC_GET_PRIKEY_BY_ADDRESS);
            if (callResult == null || callResult.size() == 0) {
                throw new NulsException(ConsensusErrorCode.ACCOUNT_VALID_ERROR);
            }
            return callResult;
        } catch (NulsException e) {
            throw e;
        }catch (Exception e) {
            throw new NulsException(e);
        }
    }

    /**
     * 查询多签账户信息
     * Query for multi-signature account information
     *
     * @param chainId
     * @param address
     * @return validate result
     */
    public static MultiSigAccount getMultiSignAccount(int chainId, String address) throws NulsException {
        try {
            Map<String, Object> callParams = new HashMap<>(4);
            callParams.put(Constants.CHAIN_ID, chainId);
            callParams.put(PARAM_ADDRESS, address);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, CALL_AC_GET_MULTI_SIGN_ACCOUNT, callParams);
            if (!cmdResp.isSuccess()) {
                throw new NulsException(ConsensusErrorCode.ACCOUNT_VALID_ERROR);
            }
            HashMap callResult = (HashMap) ((HashMap) cmdResp.getResponseData()).get(CALL_AC_GET_MULTI_SIGN_ACCOUNT);
            if (callResult == null || callResult.size() == 0) {
                throw new NulsException(ConsensusErrorCode.ACCOUNT_VALID_ERROR);
            }
            MultiSigAccount multiSigAccount = new MultiSigAccount();
            multiSigAccount.parse(RPCUtil.decode((String) callResult.get(PARAM_RESULT_VALUE)),0);
            return multiSigAccount;
        } catch (NulsException e) {
            throw e;
        }catch (Exception e) {
            throw new NulsException(ConsensusErrorCode.INTERFACE_CALL_FAILED);
        }
    }


    /**
     * 交易签名
     * transaction signature
     *
     * @param chainId
     * @param address
     * @param password
     * @param priKey
     * @param tx
     */
    public static void transactionSignature(int chainId, String address, String password, String priKey, Transaction tx) throws NulsException {
        try {
            P2PHKSignature p2PHKSignature = new P2PHKSignature();
            if (!StringUtils.isBlank(priKey)) {
                p2PHKSignature = SignatureUtil.createSignatureByPriKey(tx, priKey);
            } else {
                Map<String, Object> callParams = new HashMap<>(4);
                callParams.put(Constants.CHAIN_ID, chainId);
                callParams.put(PARAM_ADDRESS, address);
                callParams.put(PARAM_PASSWORD, password);
                callParams.put(PARAM_DATA, RPCUtil.encode(tx.getHash().getBytes()));
                Response signResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, CALL_AC_GET_SIGN_DIGEST, callParams);
                if (!signResp.isSuccess()) {
                    throw new NulsException(ConsensusErrorCode.TX_SIGNTURE_ERROR);
                }
                HashMap signResult = (HashMap) ((HashMap) signResp.getResponseData()).get(CALL_AC_GET_SIGN_DIGEST);
                p2PHKSignature.parse(RPCUtil.decode((String) signResult.get(PARAM_SIGNATURE)), 0);
            }
            TransactionSignature signature = new TransactionSignature();
            List<P2PHKSignature> p2PHKSignatures = new ArrayList<>();
            p2PHKSignatures.add(p2PHKSignature);
            signature.setP2PHKSignatures(p2PHKSignatures);
            tx.setTransactionSignature(signature.serialize());
        } catch (NulsException e) {
            throw e;
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }

    /**
     * 区块签名
     * block signature
     *
     * @param chain
     * @param address
     * @param header
     */
    public static void blockSignature(Chain chain, String address, BlockHeader header) throws NulsException {
        try {
            Map<String, Object> callParams = new HashMap<>(4);
            callParams.put(Constants.CHAIN_ID, chain.getConfig().getChainId());
            callParams.put(PARAM_ADDRESS, address);
            callParams.put(PARAM_PASSWORD, chain.getConfig().getPassword());
            callParams.put(PARAM_DATA, RPCUtil.encode(header.getHash().getBytes()));
            Response signResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, CALL_AC_SIGN_BLOCK_DIGEST, callParams);
            if (!signResp.isSuccess()) {
                throw new NulsException(ConsensusErrorCode.TX_SIGNTURE_ERROR);
            }
            HashMap signResult = (HashMap) ((HashMap) signResp.getResponseData()).get(CALL_AC_SIGN_BLOCK_DIGEST);
            BlockSignature blockSignature = new BlockSignature();
            blockSignature.parse(RPCUtil.decode((String) signResult.get(PARAM_SIGNATURE)), 0);
            header.setBlockSignature(blockSignature);
        } catch (NulsException e) {
            throw e;
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }

    /**
     * 区块签名
     * block signature
     *
     * @param chain
     * @param address
     * @return
     */
    public static byte[] signature(Chain chain, String address, byte[] data) throws NulsException {
        try {
            Map<String, Object> callParams = new HashMap<>(4);
            callParams.put(Constants.CHAIN_ID, chain.getChainId());
            callParams.put(PARAM_ADDRESS, address);
            callParams.put(PARAM_PASSWORD, chain.getConfig().getPassword());
            callParams.put(PARAM_DATA, RPCUtil.encode(data));
            Response signResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, CALL_AC_SIGN_BLOCK_DIGEST, callParams);
            if (!signResp.isSuccess()) {
                throw new NulsException(ConsensusErrorCode.TX_SIGNTURE_ERROR);
            }
            HashMap signResult = (HashMap) ((HashMap) signResp.getResponseData()).get(CALL_AC_SIGN_BLOCK_DIGEST);
            return RPCUtil.decode((String) signResult.get(PARAM_SIGNATURE));
        } catch (NulsException e) {
            throw e;
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }

    /**
     * 获取可用余额和nonce
     * Get the available balance and nonce
     *
     * @param chain
     * @param address
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getBalanceAndNonce(Chain chain, String address, int assetChainId, int assetId) throws NulsException {
        Map<String, Object> params = new HashMap(4);
        params.put(Constants.CHAIN_ID, chain.getConfig().getChainId());
        params.put(PARAM_ASSET_CHAIN_ID, assetChainId);
        params.put(PARAM_ADDRESS, address);
        params.put(PARAM_ASSET_ID, assetId);
        try {
            Response callResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, CALL_LG_GET_BALANCE_NONCE, params);
            if (!callResp.isSuccess()) {
                return null;
            }
            return (HashMap) ((HashMap) callResp.getResponseData()).get(CALL_LG_GET_BALANCE_NONCE);
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }

    /**
     * 获取账户锁定金额和可用余额
     * Acquire account lock-in amount and available balance
     *
     * @param chain
     * @param address
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getBalance(Chain chain, String address, int assetChainId, int assetId) throws NulsException {
        Map<String, Object> params = new HashMap(4);
        params.put(Constants.CHAIN_ID, chain.getConfig().getChainId());
        params.put(PARAM_ASSET_CHAIN_ID, assetChainId);
        params.put(PARAM_ADDRESS, address);
        params.put(PARAM_ASSET_ID, assetId);
        try {
            Response callResp = ResponseMessageProcessor.requestAndResponse(ModuleE.LG.abbr, CALL_LG_GET_BALANCE, params);
            if (!callResp.isSuccess()) {
                return null;
            }
            return (HashMap) ((HashMap) callResp.getResponseData()).get(CALL_LG_GET_BALANCE);
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }

    /**
     * 获取打包交易
     * Getting Packaged Transactions
     *
     * @param chain chain info
     */
    @SuppressWarnings("unchecked")
    public static Map<String,Object> getPackingTxList(Chain chain, long blockTime) {
        try {
            long packEndTime = (blockTime + 1) * 1000;
            Map<String, Object> params = new HashMap(4);
            params.put(Constants.CHAIN_ID, chain.getConfig().getChainId());
            long currentTime = NulsDateUtils.getCurrentTimeMillis();
            long surplusTime = packEndTime - currentTime;
            if(surplusTime <= MIN_PACK_SURPLUS_TIME){
                chain.getLogger().warn("打包时间太短，出空块");
                return null;
            }
            params.put(PARAM_END_TIME_STAMP, packEndTime - CALL_BACK_HANDLE_TIME );
            params.put(PARAM_MAX_TX_SIZE, chain.getConfig().getBlockMaxSize());

            chain.getLogger().info("packEndTime:{},currentTime:{},打包时间为：{}",packEndTime,currentTime,surplusTime);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, CALL_TX_PACKABLE_TXS, params,surplusTime - CALL_BACK_HANDLE_TIME);
            if (!cmdResp.isSuccess()) {
                chain.getLogger().error("Packaging transaction acquisition failure!");
                return null;
            }
            return (HashMap) ((HashMap) cmdResp.getResponseData()).get(CALL_TX_PACKABLE_TXS);

        } catch (Exception e) {
            chain.getLogger().error(e);
            return null;
        }
    }

    /**
     * 获取指定交易
     * Acquisition of transactions based on transactions Hash
     *
     * @param chain  chain info
     * @param txHash transaction hash
     */
    @SuppressWarnings("unchecked")
    public static Transaction getTransaction(Chain chain, String txHash) {
        try {
            Map<String, Object> params = new HashMap(4);
            params.put(Constants.CHAIN_ID, chain.getConfig().getChainId());
            params.put(PARAM_TX_HASH, txHash);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, CALL_TX_CONFIRMED_TX, params);
            if (!cmdResp.isSuccess()) {
                chain.getLogger().error("Acquisition transaction failed！");
                return null;
            }
            Map responseData = (Map) cmdResp.getResponseData();
            Transaction tx = new Transaction();
            Map realData = (Map) responseData.get(CALL_TX_CONFIRMED_TX);
            String txHex = (String) realData.get(PARAM_TX);
            if (!StringUtils.isBlank(txHex)) {
                tx.parse(RPCUtil.decode(txHex), 0);
            }
            return tx;
        } catch (Exception e) {
            chain.getLogger().error(e);
            return null;
        }
    }


    /**
     * 将新创建的交易发送给交易管理模块
     * The newly created transaction is sent to the transaction management module
     *
     * @param chain chain info
     * @param tx transaction hex
     */
    @SuppressWarnings("unchecked")
    public static void sendTx(Chain chain, String tx) throws NulsException {
        Map<String, Object> params = new HashMap(4);
        params.put(Constants.CHAIN_ID, chain.getConfig().getChainId());
        Response cmdResp;
        params.put(PARAM_TX, tx);
        try {
            cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, CALL_TX_NEW_TX, params);
        }catch (Exception e){
            chain.getLogger().error(e);
            throw new NulsException(ConsensusErrorCode.INTERFACE_CALL_FAILED);
        }
        if (!cmdResp.isSuccess()) {
            chain.getLogger().error("Transaction failed to send!");
            throw new NulsException(ErrorCode.init(cmdResp.getResponseErrorCode()));
        }
    }

    /**
     * 共识状态修改通知交易模块
     * Consensus status modification notification transaction module
     *
     * @param chain   chain info
     * @param packing packing state
     */
    @SuppressWarnings("unchecked")
    public static void sendState(Chain chain, boolean packing) {
        try {
            Map<String, Object> params = new HashMap(4);
            params.put(Constants.CHAIN_ID, chain.getConfig().getChainId());
            params.put(PARAM_PACKAGING, packing);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, CALL_TX_CONSENSUS_STATE, params);
            if (!cmdResp.isSuccess()) {
                chain.getLogger().error("Packing state failed to send!");
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    /**
     * 批量验证交易
     *
     * @param chainId      链Id/chain id
     * @param transactions
     * @return
     */
    public static Response verify(int chainId, List<Transaction> transactions, BlockHeader header, BlockHeader lastHeader, NulsLogger logger) {
        try {
            Map<String, Object> params = new HashMap<>(2);
            params.put(Constants.CHAIN_ID, chainId);
            List<String> txList = new ArrayList<>();
            for (Transaction transaction : transactions) {
                txList.add(RPCUtil.encode(transaction.serialize()));
            }
            params.put(PARAM_TX_LIST, txList);
            BlockExtendsData lastData = lastHeader.getExtendsData();
            params.put(PARAM_PRE_STATE_ROOT, RPCUtil.encode(lastData.getStateRoot()));
            params.put(PARAM_BLOCK_HEADER, RPCUtil.encode(header.serialize()));
            return ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, CALL_TX_BATCH_VERIFY, params, 10 * 60 * 1000);
        } catch (Exception e) {
            logger.error("", e);
            return null;
        }
    }

    /**
     * 根据交易HASH获取NONCE（交易HASH后8位）
     * Obtain NONCE according to HASH (the last 8 digits of HASH)
     */
    public static byte[] getNonce(byte[] txHash) {
        byte[] targetArr = new byte[8];
        System.arraycopy(txHash, txHash.length - 8, targetArr, 0, 8);
        return targetArr;
    }

    /**
     * 查询本地加密账户
     * Search for Locally Encrypted Accounts
     */
    @SuppressWarnings("unchecked")
    public static List<byte[]> getEncryptedAddressList(Chain chain) {
        List<byte[]> packingAddressList = new ArrayList<>();
        try {
            Map<String, Object> params = new HashMap<>(2);
            params.put(ParameterConstant.PARAM_CHAIN_ID, chain.getConfig().getChainId());
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, CALL_AC_GET_ENCRYPTED_ADDRESS_LIST, params);
            List<String> accountAddressList = (List<String>) ((HashMap) ((HashMap) cmdResp.getResponseData()).get(CALL_AC_GET_ENCRYPTED_ADDRESS_LIST)).get(PARAM_LIST);
            if (accountAddressList != null && accountAddressList.size() > 0) {
                for (String address : accountAddressList) {
                    packingAddressList.add(AddressTool.getAddress(address));
                }
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
        return packingAddressList;
    }

    /**
     * 查询账户别名
     * Query account alias
     * */
    public static String getAlias(Chain chain, String address){
        String alias = null ;
        try {
            Map<String, Object> params = new HashMap<>(2);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chain.getConfig().getChainId());
            params.put(PARAM_ADDRESS, address);
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.AC.abbr, CALL_AC_GET_ALIAS_BY_ADDRESS, params);
            HashMap result = (HashMap) ((HashMap) cmdResp.getResponseData()).get(CALL_AC_GET_ALIAS_BY_ADDRESS);
            if(result.get(PARAM_ALIAS) != null){
                alias = (String) result.get(PARAM_ALIAS);
            }
        }catch (Exception e){
            chain.getLogger().error(e);
        }
        return alias;
    }

    /**
     * 初始化链区块头数据，缓存指定数量的区块头
     * Initialize chain block header entity to cache a specified number of block headers
     *
     * @param chain chain info
     */
    @SuppressWarnings("unchecked")
    public static void loadBlockHeader(Chain chain)throws Exception{
        Map params = new HashMap(ConsensusConstant.INIT_CAPACITY_2);
        params.put(Constants.CHAIN_ID, chain.getConfig().getChainId());
        params.put(PARAM_ROUND, ConsensusConstant.INIT_BLOCK_HEADER_COUNT);
        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.BL.abbr, CALL_BL_GET_LATEST_ROUND_BLOCK_HEADERS, params);
        Map<String, Object> responseData;
        List<String> blockHeaderHexs = new ArrayList<>();
        if (response.isSuccess()) {
            responseData = (Map<String, Object>) response.getResponseData();
            Map result = (Map) responseData.get(CALL_BL_GET_LATEST_ROUND_BLOCK_HEADERS);
            blockHeaderHexs = (List<String>) result.get(PARAM_RESULT_VALUE);
        }
        while (!response.isSuccess() && blockHeaderHexs.size() == 0) {
            response = ResponseMessageProcessor.requestAndResponse(ModuleE.BL.abbr, CALL_BL_GET_LATEST_ROUND_BLOCK_HEADERS, params);
            if (response.isSuccess()) {
                responseData = (Map<String, Object>) response.getResponseData();
                Map result = (Map) responseData.get(CALL_BL_GET_LATEST_ROUND_BLOCK_HEADERS);
                blockHeaderHexs = (List<String>) result.get(PARAM_RESULT_VALUE);
                break;
            }
            Log.debug("---------------------------区块加载失败！");
            Thread.sleep(1000);
        }
        List<BlockHeader> blockHeaders = new ArrayList<>();
        for (String blockHeaderHex : blockHeaderHexs) {
            BlockHeader blockHeader = new BlockHeader();
            blockHeader.parse(RPCUtil.decode(blockHeaderHex), 0);
            blockHeaders.add(blockHeader);
        }
        blockHeaders.sort(new BlockHeaderComparator());
        chain.setBlockHeaderList(blockHeaders);
        chain.setNewestHeader(blockHeaders.get(blockHeaders.size() - 1));
        Log.debug("---------------------------区块加载成功！");
    }


    /**
     * 初始化链区块头数据，缓存指定数量的区块头
     * Initialize chain block header entity to cache a specified number of block headers
     *
     * @param chain chain info
     */
    @SuppressWarnings("unchecked")
    public static void getRoundBlockHeaders(Chain chain, long roundCount, long startHeight)throws Exception{
        Map params = new HashMap(ConsensusConstant.INIT_CAPACITY_2);
        params.put(Constants.CHAIN_ID, chain.getConfig().getChainId());
        params.put(PARAM_ROUND, roundCount);
        params.put(PARAM_HEIGHT, startHeight);
        Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.BL.abbr, CALL_BL_GET_ROUND_BLOCK_HEADERS, params);
        Map<String, Object> responseData;
        List<String> blockHeaderHexs = new ArrayList<>();
        if (response.isSuccess()) {
            responseData = (Map<String, Object>) response.getResponseData();
            Map result = (Map) responseData.get(CALL_BL_GET_ROUND_BLOCK_HEADERS);
            blockHeaderHexs = (List<String>) result.get(PARAM_RESULT_VALUE);
        }
        int tryCount = 0;
        while (!response.isSuccess() && blockHeaderHexs.size() == 0 && tryCount < ConsensusConstant.RPC_CALL_TRY_COUNT) {
            response = ResponseMessageProcessor.requestAndResponse(ModuleE.BL.abbr, CALL_BL_GET_ROUND_BLOCK_HEADERS, params);
            if (response.isSuccess()) {
                responseData = (Map<String, Object>) response.getResponseData();
                Map result = (Map) responseData.get(CALL_BL_GET_ROUND_BLOCK_HEADERS);
                blockHeaderHexs = (List<String>) result.get(PARAM_RESULT_VALUE);
                break;
            }
            tryCount++;
            Log.debug("---------------------------回滚区块轮次变化从新加载区块失败！");
            Thread.sleep(1000);
        }
        List<BlockHeader> blockHeaders = new ArrayList<>();
        for (String blockHeaderHex : blockHeaderHexs) {
            BlockHeader blockHeader = new BlockHeader();
            blockHeader.parse(RPCUtil.decode(blockHeaderHex), 0);
            blockHeaders.add(blockHeader);
        }
        blockHeaders.sort(new BlockHeaderComparator());
        chain.getBlockHeaderList().addAll(0, blockHeaders);
        Log.debug("---------------------------回滚区块轮次变化从新加载区块成功！");
    }


    /**
     * 通知区块模块有区块拜占庭验证成功
     * Notification block module has block Byzantine verification succeeded
     * @param chain           链信息
     * @param bifurcate       是否分叉
     * @param height          区块高度
     * @param firstHash     第一个区块Hash
     * @param secondHash    如果分叉则为分叉块Hash否则为NULL
     * */
    @SuppressWarnings("unchecked")
    public static boolean noticeByzantineResult(Chain chain, long height, boolean bifurcate, NulsHash firstHash, NulsHash secondHash){
        Map params = new HashMap(ConsensusConstant.INIT_CAPACITY_8);
        params.put(Constants.CHAIN_ID, chain.getChainId());
        params.put(PARAM_HEIGHT, height);
        params.put(PARAM_BIFURCATE, bifurcate);
        params.put(PARAM_FIRST_HASH, firstHash.toHex());
        if(secondHash != null){
            params.put(PARAM_SECOND_HASH, secondHash.toHex());
        }else{
            params.put(PARAM_SECOND_HASH, null);
        }
        try {
            Request request = MessageUtil.newRequest(CALL_BL_PUT_BZFT_FLAG, params, Constants.BOOLEAN_FALSE, Constants.ZERO, Constants.ZERO);
            ResponseMessageProcessor.requestOnly(ModuleE.BL.abbr, request);
            return true;
        }catch (Exception e){
            chain.getLogger().error(e);
            return false;
        }
    }

    /**
     * 将打包的新区块发送给区块管理模块
     *
     * @param chainId chain ID
     * @param block   new block Info
     * @return Successful Sending
     */
    @SuppressWarnings("unchecked")
    public static void  receivePackingBlock(int chainId, String block) throws NulsException {
        Map<String, Object> params = new HashMap(4);
        params.put(Constants.CHAIN_ID, chainId);
        params.put(PARAM_BLOCK, block);
        try {
            Request request = MessageUtil.newRequest(CALL_BL_RECEIVE_PACKING_BLOCK, params, Constants.BOOLEAN_FALSE, Constants.ZERO, Constants.ZERO);
            ResponseMessageProcessor.requestOnly(ModuleE.BL.abbr, request);
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }

    /**
     * 获取主网节点版本
     * Acquire account lock-in amount and available balance
     *
     * @param chainId
     */
    @SuppressWarnings("unchecked")
    public static Map getVersion(int chainId) throws NulsException {
        Map<String, Object> params = new HashMap(4);
        params.put(Constants.CHAIN_ID, chainId);
        try {
            Response callResp = ResponseMessageProcessor.requestAndResponse(ModuleE.PU.abbr, CALL_PU_GET_VERSION, params);
            if (!callResp.isSuccess()) {
                return null;
            }
            return (Map) ((Map) callResp.getResponseData()).get(CALL_PU_GET_VERSION);
        } catch (Exception e) {
            throw new NulsException(e);
        }
    }
}

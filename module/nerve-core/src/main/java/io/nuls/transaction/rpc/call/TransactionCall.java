package io.nuls.transaction.rpc.call;

import io.nuls.core.constant.BaseConstant;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.exception.NulsException;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.common.NerveCoreResponseMessageProcessor;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.model.bo.Chain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nuls.transaction.utils.LoggerUtil.LOG;

/**
 * Calling other modules and transaction related interfaces
 *
 * @author: qinyifeng
 * @date: 2018/12/05
 */
public class TransactionCall {


    public static Object requestAndResponse(String moduleCode, String cmd, Map params) throws NulsException {
        return requestAndResponse(moduleCode, cmd, params, null);
    }

    /**
     * Call other module interfaces
     * Call other module interfaces
     */
    public static Object requestAndResponse(String moduleCode, String cmd, Map params, Long timeout) throws NulsException {
        try {
            params.put(Constants.VERSION_KEY_STR, TxConstant.RPC_VERSION);
            Response response = null;
            try {
                if (null == timeout) {
                    response = NerveCoreResponseMessageProcessor.requestAndResponse(moduleCode, cmd, params);
                } else {
                    response = NerveCoreResponseMessageProcessor.requestAndResponse(moduleCode, cmd, params, timeout);
                }
            } catch (Exception e) {
                LOG.error("Call interface [{}] error, moduleCode:{}, ", moduleCode, cmd);
                LOG.error(e);
                throw new NulsException(TxErrorCode.RPC_REQUEST_FAILD);
            }
            if (!response.isSuccess()) {
                String errorCode = response.getResponseErrorCode();
                LOG.error("Call interface [{}] error, moduleCode:{}, ErrorCode is {}, ResponseComment:{}", moduleCode, cmd, errorCode, response.getResponseComment());
                throw new NulsException(ErrorCode.init(errorCode));
            }
            Map data = (Map) response.getResponseData();
            return data.get(cmd);
        } catch (RuntimeException e) {
            LOG.error(e);
            throw new NulsException(TxErrorCode.RPC_REQUEST_FAILD);
        }
    }


    /**
     * Call transaction rollback
     *
     * @param chain       Chain information
     * @param cmd         Rollback interface command
     * @param moduleCode  modulecode
     * @param txList      List of transactions to be rolled back
     * @param blockHeader Block header information
     * @return
     */
    public static boolean txRollback(Chain chain, String cmd, String moduleCode, List<String> txList, String blockHeader) {
        //Calling a single transaction validator
        Map<String, Object> params = new HashMap(TxConstant.INIT_CAPACITY_8);
        params.put(Constants.CHAIN_ID, chain.getChainId());
        params.put("txList", txList);
        params.put("blockHeader", blockHeader);
        return txProcess(chain, cmd, moduleCode, params);
    }

    /**
     * Call transaction commit
     *
     * @param chain       Chain information
     * @param cmd         Submit interface commands
     * @param moduleCode  modulecode
     * @param txList      List of pending transactions to be submitted
     * @param blockHeader Block header information
     * @param syncStatus  0:synchronization 1:normal operation
     * @return
     */
    public static boolean txCommit(Chain chain, String cmd, String moduleCode, List<String> txList, String blockHeader, Integer syncStatus) {
        //Calling a single transaction validator
        Map<String, Object> params = new HashMap(TxConstant.INIT_CAPACITY_8);
        params.put(Constants.CHAIN_ID, chain.getChainId());
        params.put("txList", txList);
        params.put("blockHeader", blockHeader);
        params.put("syncStatus", syncStatus);
        return txProcess(chain, cmd, moduleCode, params);
    }

    /**
     * Package transaction processor
     *
     * @param chain      Chain information
     * @param cmd        Submit interface commands
     * @param moduleCode modulecode
     * @param txList     List of pending transactions to be submitted
     * @param process    0:Indicates packaging, 1:Represent verification
     * @return
     */
    public static Map<String, List<String>> packProduce(Chain chain, String cmd, String moduleCode, List<String> txList, long height, long blockTime, int process) throws NulsException {
        Map<String, Object> params = new HashMap(TxConstant.INIT_CAPACITY_8);
        params.put(Constants.CHAIN_ID, chain.getChainId());
        params.put("txList", txList);
        params.put("height", height);
        params.put("blockTime", blockTime);
        params.put("process", process);
        HashMap responseMap = (HashMap) TransactionCall.requestAndResponse(moduleCode, cmd, params);
        return responseMap;
    }

    /**
     * Call transaction commit perhaps rollback
     *
     * @param chain      Chain information
     * @param cmd        Interface commands
     * @param moduleCode modulecode
     * @return
     */
    public static boolean txProcess(Chain chain, String cmd, String moduleCode, Map<String, Object> params) {
        try {
            Map result = (Map) TransactionCall.requestAndResponse(moduleCode, cmd, params, TxConstant.REQUEST_TIME_OUT);
            Boolean value = (Boolean) result.get("value");
            if (null == value) {
                chain.getLogger().error("call module-{} {} response value is null, error:{}",
                        moduleCode, cmd, TxErrorCode.REMOTE_RESPONSE_DATA_NOT_FOUND.getCode());
                return false;
            }
            return value;
        } catch (Exception e) {
            chain.getLogger().error("call module-{} {} error, error:{}", moduleCode, "txProcess", e);
            return false;
        }
    }

    /**
     * Module Transaction Unified Verifier
     * Single module transaction integrate validator
     *
     * @return Return unverified transactionshash, If there is an exception, all transactions will be returned(Not passed) / return unverified transaction hash
     */
    public static Map<String, Object> txModuleValidator(Chain chain, String moduleCode, String tx) throws NulsException {
        List<String> txList = new ArrayList<>();
        txList.add(tx);
        return callTxModuleValidator(chain, moduleCode, txList, null);
    }

    /**
     * Module Transaction Unified Verifier
     * Single module transaction integrate validator
     *
     * @return Return unverified transactionshash, If there is an exception, all transactions will be returned(Not passed) / return unverified transaction hash
     */
    public static List<String> txModuleValidator(Chain chain, String moduleCode, List<String> txList) throws NulsException {
        return txModuleValidator(chain, moduleCode, txList, null);
    }

    /**
     * Module Transaction Unified Verifier
     * Single module transaction integrate validator
     *
     * @return Return unverified transactionshash, If there is an exception, all transactions will be returned(Not passed) / return unverified transaction hash
     */
    public static List<String> txModuleValidator(Chain chain, String moduleCode, List<String> txList, String blockHeaderStr) throws NulsException {
        //Call the transaction module's unified validator
        Map<String, Object> result = callTxModuleValidator(chain, moduleCode, txList, blockHeaderStr);
        return (List<String>) result.get("list");
    }

    private static Map<String, Object> callTxModuleValidator(Chain chain, String moduleCode, List<String> txList, String blockHeaderStr) throws NulsException {
        Map<String, Object> params = new HashMap(TxConstant.INIT_CAPACITY_8);
        params.put(Constants.CHAIN_ID, chain.getChainId());
        params.put("txList", txList);
        params.put("blockHeader", blockHeaderStr);
        Map responseMap = (Map) TransactionCall.requestAndResponse(moduleCode, BaseConstant.TX_VALIDATOR, params);

        List<String> list = (List<String>) responseMap.get("list");
        if (null == list) {
            chain.getLogger().error("call txModuleValidator-{} {} response value is null, error:{}",
                    moduleCode, BaseConstant.TX_VALIDATOR, TxErrorCode.REMOTE_RESPONSE_DATA_NOT_FOUND.getCode());
            throw new NulsException(TxErrorCode.REMOTE_RESPONSE_DATA_NOT_FOUND);
        }

        String errorCode = (String) responseMap.get("errorCode");
        Map<String, Object> result = new HashMap<>(TxConstant.INIT_CAPACITY_4);
        result.put("list", list);
        result.put("errorCode", errorCode);
        return result;
    }

}

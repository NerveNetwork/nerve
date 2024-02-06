package io.nuls.base.protocol;

import io.nuls.base.protocol.cmd.TransactionDispatcher;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.log.Log;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.channel.manager.ConnectManager;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;

import java.util.*;

/**
 * Help the module achieve automatic registration of transactions、Tool class for registering messages
 *
 * @author captain
 * @version 1.0
 * @date 2019/5/28 11:44
 */
public class RegisterHelper {

    @Autowired
    private static TransactionDispatcher transactionDispatcher;

    /**
     * Register transactions with the trading module
     * Register transactions with the transaction module
     */
    public static boolean registerTx(int chainId, Protocol protocol, String moduleCode) {
        try {
            List<TxRegisterDetail> txRegisterDetailList = new ArrayList<>();
            Set<TxDefine> allowTxs = protocol.getAllowTx();
            // Is there a need to process internally generated transactions during packaging
            boolean moduleHasPackProduceTx = false;
            for (TxDefine config : allowTxs) {
                TxRegisterDetail detail = new TxRegisterDetail();
                detail.setSystemTx(config.isSystemTx());
                detail.setTxType(config.getType());
                detail.setUnlockTx(config.isUnlockTx());
                detail.setVerifySignature(config.isVerifySignature());
                detail.setVerifyFee(config.getVerifyFee());
                detail.setPackProduce(config.getPackProduce());
                detail.setPackGenerate(config.getPackGenerate());
                if(config.getPackProduce()){
                    moduleHasPackProduceTx = true;
                }
                txRegisterDetailList.add(detail);
            }
            if (txRegisterDetailList.isEmpty()) {
                return true;
            }
            //Register transactions with the transaction management module
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            params.put("moduleCode", moduleCode);
            params.put("list", txRegisterDetailList);
            params.put("delList", protocol.getInvalidTxs());
            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_register", params);
            if (!cmdResp.isSuccess()) {
                Log.error("chain ：" + chainId + " Failure of transaction registration,errorMsg: " + cmdResp.getResponseComment());
                return false;
            }

            /**
             * add by Charlie 2020-04-03
             * There is a need to process internally generated transactions during packaging
             * Corresponding processor needs to be loaded, This processor is at the module level, Simultaneously processing multiple types of transactions in this module
             * The implementation of processor interfaces is loaded with module namesbean
             */
            if(moduleHasPackProduceTx) {
                // Register packaging processor interface
                if (transactionDispatcher == null) {
                    transactionDispatcher = SpringLiteContext.getBean(TransactionDispatcher.class);
                }
                ModuleTxPackageProcessor processor = SpringLiteContext.getBean(ModuleTxPackageProcessor.class, moduleCode);
                transactionDispatcher.setModuleTxPackageProcessor(processor);
            }
        } catch (Exception e) {
            Log.error("", e);
        }
        return true;
    }

    /**
     * Register transactions with the trading module
     * Register transactions with the transaction module
     */
    public static boolean registerTx(int chainId, Protocol protocol) {
        return registerTx(chainId, protocol, ConnectManager.LOCAL.getAbbreviation());
    }

    /**
     * Register messages with the network module
     *
     * @return
     */
    public static boolean registerMsg(Protocol protocol, String role) {
        try {
            Map<String, Object> map = new HashMap<>(2);
            List<String> cmds = new ArrayList<>();
            map.put("role", role);
            protocol.getAllowMsg().forEach(e -> cmds.addAll(Arrays.asList(e.getProtocolCmd().split(","))));
            if (cmds.isEmpty()) {
                return true;
            }
            map.put("protocolCmds", cmds);
            return ResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, "nw_protocolRegister", map).isSuccess();
        } catch (Exception e) {
            Log.error("registerMsg fail", e);
            return false;
        }
    }

    /**
     * Register messages with the network module
     *
     * @return
     */
    public static boolean registerMsg(Protocol protocol) {
        return registerMsg(protocol, ConnectManager.LOCAL.getAbbreviation());
    }

    /**
     * Register multi version protocol configuration with protocol upgrade module
     * Register transactions with the transaction module
     */
    public static boolean registerProtocol(int chainId) {
        if (!ModuleHelper.isSupportProtocolUpdate()) {
            return true;
        }
        try {
            Collection<Protocol> protocols = ProtocolGroupManager.getProtocols(chainId);
            Map<String, Object> params = new HashMap<>();
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            List<Protocol> list = new ArrayList<>(protocols);
            params.put("list", list);
            params.put("moduleCode", ConnectManager.LOCAL.getAbbreviation());

            Response cmdResp = ResponseMessageProcessor.requestAndResponse(ModuleE.PU.abbr, "registerProtocol", params);
            if (!cmdResp.isSuccess()) {
                Log.error("chain ：" + chainId + " Failure of transaction registration,errorMsg: " + cmdResp.getResponseComment());
                return false;
            }
        } catch (Exception e) {
            Log.error("", e);
        }
        return true;
    }

}

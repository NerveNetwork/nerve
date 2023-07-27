package io.nuls.transaction.rpc.cmd;

import com.fasterxml.jackson.databind.DeserializationFeature;
import io.nuls.core.rpc.model.NerveCoreCmd;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TxRegisterDetail;
import io.nuls.base.signture.MultiSignTxSignature;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.ObjectUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.transaction.cache.PackablePool;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.manager.ChainManager;
import io.nuls.transaction.manager.TxManager;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.bo.TxPackage;
import io.nuls.transaction.model.bo.VerifyLedgerResult;
import io.nuls.transaction.model.dto.ModuleTxRegisterDTO;
import io.nuls.transaction.model.po.TransactionConfirmedPO;
import io.nuls.transaction.service.ConfirmedTxService;
import io.nuls.transaction.service.TxPackageService;
import io.nuls.transaction.service.TxService;
import io.nuls.transaction.storage.LockedAddressStorageService;
import io.nuls.transaction.utils.TxUtil;

import java.util.*;

import static io.nuls.transaction.utils.LoggerUtil.LOG;

/**
 * @author: Charlie
 * @date: 2018/11/12
 */
@Component
@NerveCoreCmd(module = ModuleE.TX)
public class TxCmd extends BaseCmd {

    @Autowired
    private TxService txService;
    @Autowired
    private TxPackageService txPackageService;
    @Autowired
    private ConfirmedTxService confirmedTxService;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private PackablePool packablePool;
    @Autowired
    private LockedAddressStorageService lockedAddressStorageService;

    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_REGISTER, version = 1.0, description = "注册模块交易/Register module transactions")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "moduleCode", parameterType = "String", parameterDes = "注册交易的模块code"),
            @Parameter(parameterName = "list", requestType = @TypeDescriptor(value = List.class, collectionElement = TxRegisterDetail.class), parameterDes = "待注册交易的数据"),
            @Parameter(parameterName = "delList", requestType = @TypeDescriptor(value = List.class, collectionElement = Integer.class), parameterDes = "待移除已注册交易数据", canNull = true)
    })
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "是否注册成功")
    }))
    public Response register(Map params) {
        Map<String, Boolean> map = new HashMap<>(TxConstant.INIT_CAPACITY_2);
        boolean result = false;
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("moduleCode"), TxErrorCode.PARAMETER_ERROR.getMsg());

            JSONUtils.getInstance().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ModuleTxRegisterDTO moduleTxRegisterDto = JSONUtils.map2pojo(params, ModuleTxRegisterDTO.class);

            chain = chainManager.getChain(moduleTxRegisterDto.getChainId());
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            List<TxRegisterDetail> txRegisterList = moduleTxRegisterDto.getList();
            if (moduleTxRegisterDto == null || txRegisterList == null) {
                throw new NulsException(TxErrorCode.TX_NOT_EXIST);
            }
            result = txService.register(chain, moduleTxRegisterDto);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }

        map.put("value", result);
        return success(map);
    }

    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_NEWTX, version = 1.0, description = "接收本地新交易/receive a new transaction")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "tx", parameterType = "String", parameterDes = "交易序列化数据字符串")
    })
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "是否成功"),
            @Key(name = "hash", description = "交易hash")
    }))
    public Response newTx(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("tx"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            String txStr = (String) params.get("tx");
            //将txStr转换为Transaction对象
            Transaction transaction = TxUtil.getInstanceRpcStr(txStr, Transaction.class);
            //将交易放入待验证本地交易队列中
            txService.newTx(chain, transaction);
            Map<String, Object> map = new HashMap<>(TxConstant.INIT_CAPACITY_4);
            map.put("value", true);
            map.put("hash", transaction.getHash().toHex());
            return success(map);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }


    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_PACKABLETXS, version = 1.0, description = "获取可打包的交易集/returns a list of packaged transactions")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "endTimestamp", requestType = @TypeDescriptor(value = long.class), parameterDes = "截止时间"),
            @Parameter(parameterName = "blockTime", requestType = @TypeDescriptor(value = long.class), parameterDes = "本次出块区块时间"),
            @Parameter(parameterName = "maxTxDataSize", requestType = @TypeDescriptor(value = int.class), parameterDes = "交易集最大容量"),
            @Parameter(parameterName = "preStateRoot", parameterType = "String", parameterDes = "前一个区块的状态根")
    })
    @ResponseData(name = "返回值", description = "返回一个Map，包含一个key", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "list", valueType = List.class, valueElement = String.class, description = "可打包交易集")
    }))
    public Response packableTxs(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("endTimestamp"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("maxTxDataSize"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("blockTime"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            //结束打包的时间
            long endTimestamp = Long.parseLong(params.get("endTimestamp").toString());
            //交易数据最大容量值
            int maxTxDataSize = (int) params.get("maxTxDataSize");
            long blockTime = Long.parseLong(params.get("blockTime").toString());
            String preStateRoot = (String) params.get("preStateRoot");
//            chain.getLogger().info("总可用:{}", endTimestamp - NulsDateUtils.getCurrentTimeMillis());
            TxPackage txPackage = txPackageService.packageBasic(chain, endTimestamp, maxTxDataSize, blockTime, preStateRoot);
            List<String> list = txPackage.getList();
            Map<String, Object> map = new HashMap<>(TxConstant.INIT_CAPACITY_4);
            map.put("list", null == list ? new ArrayList<>() : list);
            map.put("stateRoot", txPackage.getStateRoot());
            return success(map);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_BACKPACKABLETXS, version = 1.0, description = "共识模块把不能打包的交易还回来，重新加入待打包列表/back packaged transactions")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txList", requestType = @TypeDescriptor(value = List.class, collectionElement = String.class), parameterDes = "交易序列化数据字符串集合")
    })
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "是否成功")
    }))
    public Response backPackableTxs(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txList"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            List<String> txStrList = (List<String>) params.get("txList");
            int count = txStrList.size() - 1;
            for (int i = count; i >= 0; i--) {
                Transaction tx = TxUtil.getInstanceRpcStr(txStrList.get(i), Transaction.class);
                packablePool.offerFirstOnlyHash(chain, tx);
            }
            Map<String, Object> map = new HashMap<>(TxConstant.INIT_CAPACITY_2);
            map.put("value", true);
            return success(map);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    /**
     * Save the transaction in the new block that was verified to the database
     * 保存新区块的交易
     *
     * @param params Map
     * @return Response
     */
    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_SAVE, priority = CmdPriority.HIGH, version = 1.0, description = "保存新区块的交易/Save the confirmed transaction")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txList", requestType = @TypeDescriptor(value = List.class, collectionElement = String.class), parameterDes = "待保存的交易集合"),
            @Parameter(parameterName = "blockHeader", parameterType = "String", parameterDes = "区块头"),
            @Parameter(parameterName = "syncStatus", requestType = @TypeDescriptor(value = int.class), parameterDes = "运行状态")
    })
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "是否成功")
    }))
    public Response txSave(Map params) {
        Map<String, Boolean> map = new HashMap<>(TxConstant.INIT_CAPACITY_16);
        boolean result = false;
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txList"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("blockHeader"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("syncStatus"), TxErrorCode.PARAMETER_ERROR.getMsg());

            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            List<String> txStrList = (List<String>) params.get("txList");
            if (null == txStrList) {
                throw new NulsException(TxErrorCode.PARAMETER_ERROR);
            }
            int syncStatus = (Integer) params.get("syncStatus");
            result = confirmedTxService.saveTxList(chain, txStrList, (String) params.get("blockHeader"), syncStatus);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        Map<String, Boolean> resultMap = new HashMap<>(TxConstant.INIT_CAPACITY_2);
        resultMap.put("value", result);
        return success(resultMap);
    }

    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_GENGSIS_SAVE, version = 1.0, description = "保存创世块的交易/Save the transactions of the Genesis block ")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txList", requestType = @TypeDescriptor(value = List.class, collectionElement = String.class), parameterDes = "待保存的交易集合"),
            @Parameter(parameterName = "blockHeader", parameterType = "String", parameterDes = "区块头"),
            @Parameter(parameterName = "syncStatus", requestType = @TypeDescriptor(value = int.class), parameterDes = "运行状态")
    })
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "是否成功")
    }))
    public Response txGengsisSave(Map params) {
        Map<String, Boolean> map = new HashMap<>(TxConstant.INIT_CAPACITY_16);
        boolean result = false;
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txList"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("blockHeader"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("syncStatus"), TxErrorCode.PARAMETER_ERROR.getMsg());

            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            List<String> txStrList = (List<String>) params.get("txList");
            int syncStatus = (Integer) params.get("syncStatus");
            result = confirmedTxService.saveGengsisTxList(chain, txStrList, (String) params.get("blockHeader"), syncStatus);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        Map<String, Boolean> resultMap = new HashMap<>(TxConstant.INIT_CAPACITY_2);
        resultMap.put("value", result);
        return success(resultMap);
    }

    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_ROLLBACK, priority = CmdPriority.HIGH, version = 1.0, description = "回滚区块的交易/transaction rollback")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txHashList", requestType = @TypeDescriptor(value = List.class, collectionElement = String.class), parameterDes = "待回滚交易集合"),
            @Parameter(parameterName = "blockHeader", parameterType = "String", parameterDes = "区块头")
    })
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "是否成功")
    }))
    public Response txRollback(Map params) {
        boolean result;
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txHashList"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("blockHeader"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            List<String> txHashStrList = (List<String>) params.get("txHashList");
            List<NulsHash> txHashList = new ArrayList<>();
            //将交易hashHex解码为交易hash字节数组
            for (String hashStr : txHashStrList) {
                txHashList.add(NulsHash.fromHex(hashStr));
            }
            //批量回滚已确认交易
            result = confirmedTxService.rollbackTxList(chain, txHashList, (String) params.get("blockHeader"));
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
        Map<String, Boolean> resultMap = new HashMap<>(TxConstant.INIT_CAPACITY_2);
        resultMap.put("value", result);
        return success(resultMap);
    }

    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_GET_SYSTEM_TYPES, version = 1.0, description = "获取所有系统交易类型/Get system transaction types")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    })
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "list", valueType = List.class, valueElement = Integer.class, description = "系统交易类型集合")
    }))
    public Response getSystemTypes(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            List<Integer> list = TxManager.getSysTypes(chain);
            Map<String, Object> resultMap = new HashMap<>(TxConstant.INIT_CAPACITY_2);
            resultMap.put("list", list);
            return success(resultMap);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_GETTX, version = 1.0, description = "根据hash获取交易, 先查未确认, 查不到再查已确认/Get transaction by tx hash")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txHash", parameterType = "String", parameterDes = "待查询交易hash")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "tx", description = "获取到的交易的序列化数据的字符串")
    }))
    public Response getTx(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txHash"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            String txHash = (String) params.get("txHash");
            if (!NulsHash.validHash(txHash)) {
                throw new NulsException(TxErrorCode.HASH_ERROR);
            }
            TransactionConfirmedPO tx = txService.getTransaction(chain, NulsHash.fromHex(txHash));
            Map<String, String> resultMap = new HashMap<>(TxConstant.INIT_CAPACITY_2);
            if (tx == null) {
//                LOG.debug("getTx - from all, fail! tx is null, txHash:{}", txHash);
                resultMap.put("tx", null);
            } else {
//                LOG.debug("getTx - from all, success txHash : " + tx.getTx().getHash().toHex());
                resultMap.put("tx", RPCUtil.encode(tx.getTx().serialize()));
            }
            return success(resultMap);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }


    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_GET_CONFIRMED_TX, version = 1.0, description = "根据hash获取已确认交易(只查已确认)/Get confirmed transaction by tx hash")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txHash", parameterType = "String", parameterDes = "待查询交易hash")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "tx", description = "获取到的交易的序列化数据的字符串")
    }))
    public Response getConfirmedTx(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txHash"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            String txHash = (String) params.get("txHash");
            if (!NulsHash.validHash(txHash)) {
                throw new NulsException(TxErrorCode.HASH_ERROR);
            }
            TransactionConfirmedPO tx = confirmedTxService.getConfirmedTransaction(chain, NulsHash.fromHex(txHash));
            Map<String, String> resultMap = new HashMap<>(TxConstant.INIT_CAPACITY_2);
            if (tx == null) {
//                LOG.debug("getConfirmedTransaction fail, tx is null. txHash:{}", txHash);
                resultMap.put("tx", null);
            } else {
//                LOG.debug("getConfirmedTransaction success. txHash:{}", txHash);
                resultMap.put("tx", RPCUtil.encode(tx.getTx().serialize()));
            }
            return success(resultMap);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }


    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_ISCONFIRMED, version = 1.0, description = "根据hash获取交易是否已确认(只查已确认)/Check tx is confirmed by tx hash")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txHash", parameterType = "String", parameterDes = "待查询交易hash")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "true: confirmed; false:unconfirmed")
    }))
    public Response isConfirmed(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txHash"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            String txHash = (String) params.get("txHash");
            if (!NulsHash.validHash(txHash)) {
                throw new NulsException(TxErrorCode.HASH_ERROR);
            }
            TransactionConfirmedPO txPO = confirmedTxService.getConfirmedTransaction(chain, NulsHash.fromHex(txHash));
            Map<String, Boolean> resultMap = new HashMap<>(TxConstant.INIT_CAPACITY_2);
            resultMap.put("value", false);
            if (txPO != null) {
                Transaction tx = txPO.getTx();
                if (null != tx && txHash.equals(tx.getHash().toHex())) {
                    resultMap.put("value", true);
                }
            }
            return success(resultMap);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_GET_BLOCK_TXS, version = 1.0,
            description = "获取区块的完整交易，如果没有查询到，或者查询到的不是区块完整的交易数据，则返回空集合/Get block transactions")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txHashList", requestType = @TypeDescriptor(value = List.class, collectionElement = String.class), parameterDes = "待查询交易hash集合")
    })
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "txList", valueType = List.class, valueElement = String.class, description = "返回交易序列化数据字符串集合")
    }))
    public Response getBlockTxs(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txHashList"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            List<String> txHashList = (List<String>) params.get("txHashList");
            List<String> txList = confirmedTxService.getTxList(chain, txHashList);
            Map<String, List<String>> resultMap = new HashMap<>(TxConstant.INIT_CAPACITY_2);
            resultMap.put("txList", txList);
            return success(resultMap);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_GET_BLOCK_TXS_EXTEND, version = 1.0, description = "根据hash列表，获取交易，已确认未确认都查/Get transactions by hashs")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txHashList", requestType = @TypeDescriptor(value = List.class, collectionElement = String.class), parameterDes = "待查询交易hash集合"),
            @Parameter(parameterName = "allHits", requestType = @TypeDescriptor(value = boolean.class), parameterDes = "true：必须全部查到才返回数据，否则返回空list； false：查到几个返回几个")
    })
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "txList", valueType = List.class, valueElement = String.class, description = "返回交易序列化数据字符串集合")
    }))
    public Response getBlockTxsExtend(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txHashList"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("allHits"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            List<String> txHashList = (List<String>) params.get("txHashList");
            boolean allHits = (boolean) params.get("allHits");
            List<String> txList = confirmedTxService.getTxListExtend(chain, txHashList, allHits);
            Map<String, List<String>> resultMap = new HashMap<>(TxConstant.INIT_CAPACITY_2);
            resultMap.put("txList", txList);
            return success(resultMap);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }


    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_GET_NONEXISTENT_UNCONFIRMED_HASHS, version = 1.0, description = "查询传入的交易hash中,不在未确认库中的交易hash/Get nonexistent unconfirmed transaction hashs")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txHashList", requestType = @TypeDescriptor(value = List.class, collectionElement = String.class), parameterDes = "待查询交易hash集合")
    })
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "txList", valueType = List.class, valueElement = String.class, description = "返回交易序列化数据字符串集合")
    }))
    public Response getNonexistentUnconfirmedHashs(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txHashList"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            List<String> txHashList = (List<String>) params.get("txHashList");
            List<String> hashList = confirmedTxService.getNonexistentUnconfirmedHashList(chain, txHashList);
            Map<String, List<String>> resultMap = new HashMap<>(TxConstant.INIT_CAPACITY_2);
            resultMap.put("txHashList", hashList);
            return success(resultMap);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_BATCHVERIFY, priority = CmdPriority.HIGH, version = 1.0, description = "验证区块所有交易/Verify all transactions in the block")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txList", requestType = @TypeDescriptor(value = List.class, collectionElement = String.class), parameterDes = "待验证交易序列化数据字符串集合"),
            @Parameter(parameterName = "blockHeader", parameterType = "String", parameterDes = "对应的区块头"),
            @Parameter(parameterName = "preStateRoot", parameterType = "String", parameterDes = "前一个区块状态根")
    })
    @ResponseData(name = "返回值", description = "返回一个Map，包含一个key", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class,  description = "是否验证成功")
    }))
    public Response batchVerify(Map params) {
        VerifyLedgerResult verifyLedgerResult = null;
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txList"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("blockHeader"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            List<String> txList = (List<String>) params.get("txList");
            String blockHeaderStr = (String) params.get("blockHeader");
            String preStateRoot = (String) params.get("preStateRoot");
            txPackageService.verifyBlockTransations(chain, txList, blockHeaderStr, preStateRoot);
            Map<String, Boolean> resultMap = new HashMap<>(TxConstant.INIT_CAPACITY_2);
            resultMap.put("value", true);
            return success(resultMap);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }

    }

    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_SET_MODULE_GENERATE_TX_TYPES, priority = CmdPriority.HIGH, version = 1.0, description = "设置模块生成的系统交易类型")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "moduleAbbr", requestType = @TypeDescriptor(value = String.class), parameterDes = "模块名称"),
            @Parameter(parameterName = "txTypeList", requestType = @TypeDescriptor(value = List.class, collectionElement = Integer.class), parameterDes = "设置模块生成的系统交易类型"),
    })
    @ResponseData(description = "无特定返回值，没有错误即设置成功")
    public Response setContractGenerateTxTypes(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("moduleAbbr"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txTypeList"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            List<Integer> txTypeList = (List<Integer>) params.get("txTypeList");
            if (txTypeList == null) {
                txTypeList = new ArrayList<>();
            }
            String moduleAbbr = (String) params.get("moduleAbbr");
            chain.getModuleGenerateTxTypesMap().put(moduleAbbr, new HashSet<>(txTypeList));
            chain.getLogger().info("设置模块{}生成交易类型: {}", moduleAbbr, Arrays.toString(txTypeList.toArray()));
            return success();
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }

    }

    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_CS_STATE, version = 1.0, description = "设置节点打包状态(由共识模块设置)/Set the node packaging state")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "packaging", requestType = @TypeDescriptor(value = boolean.class), parameterDes = "是否正在打包")
    })
    @ResponseData(description = "无特定返回值，没有错误即设置成功")
    public Response packaging(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            Boolean packaging = null == params.get("packaging") ? null : (Boolean) params.get("packaging");
            if (null == packaging) {
                throw new NulsException(TxErrorCode.PARAMETER_ERROR);
            }
            chain.getPackaging().set(packaging);
            chain.getLogger().debug("Task-Packaging 节点是否是打包节点,状态变更为: {}", chain.getPackaging().get());
            return success();
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }


    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_BL_STATE, version = 1.0, description = "设置节点区块同步状态(由区块模块设置)/Set the node block state")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "status", requestType = @TypeDescriptor(value = int.class), parameterDes = "是否进入等待, 不处理交易")
    })
    @ResponseData(description = "无特定返回值，没有错误即设置成功")
    public Response blockNotice(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            Integer status = (Integer) params.get("status");
            if (null == status) {
                throw new NulsException(TxErrorCode.PARAMETER_ERROR);
            }
            if (1 == status) {
                chain.getProcessTxStatus().set(true);
                chain.getLogger().info("节点区块同步状态变更为: true");
            } else {
                chain.getProcessTxStatus().set(false);
                chain.getLogger().info("节点区块同步状态变更为: false");
            }
            return success();
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    /**
     * 最新区块高度
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_BLOCK_HEIGHT, priority = CmdPriority.HIGH, version = 1.0, description = "接收最新区块高度/Receive the latest block height")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "height", requestType = @TypeDescriptor(value = long.class), parameterDes = "区块高度")
    })
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "是否成功")
    }))
    public Response height(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            Long height = Long.parseLong(params.get("height").toString());
            chain.setBestBlockHeight(height);
            chain.getLogger().debug("最新已确认区块高度更新为: [{}]" + TxUtil.nextLine() + TxUtil.nextLine(), height);
            Map<String, Object> resultMap = new HashMap<>(TxConstant.INIT_CAPACITY_2);
            resultMap.put("value", true);
            return success(resultMap);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }


    @CmdAnnotation(cmd = "tx_getTxSigners", version = 1.0, description = "获取交易合法签名的签名者列表/Gets the list of signers of the transaction's legal signature")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txHex", parameterType = "String", parameterDes = "交易字符串")
    })
    @ResponseData(name = "返回值", description = "返回一个Map", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = List.class, valueElement = String.class, description = "交易的合法签名账户"),

    }))
    public Response getTxSigners(Map params){
        Chain chain = null;
        try {
            // check parameters
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txHex"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            String txHex = (String)params.get("txHex");
            Transaction tx = TxUtil.getInstance(txHex, Transaction.class);
            TransactionSignature transactionSignature = null;
            if (tx.isMultiSignTx()) {
                transactionSignature = TxUtil.getInstance(tx.getTransactionSignature(), MultiSignTxSignature.class);
            } else {
                transactionSignature = TxUtil.getInstance(tx.getTransactionSignature(), TransactionSignature.class);
            }
            List<P2PHKSignature> p2PHKSignatureList = transactionSignature.getP2PHKSignatures();
            Set<String> signers = new HashSet<>();
            if(null != p2PHKSignatureList && !p2PHKSignatureList.isEmpty()){
                for (P2PHKSignature signature : p2PHKSignatureList) {
                    if (!ECKey.verify(tx.getHash().getBytes(), signature.getSignData().getSignBytes(), signature.getPublicKey())) {
                        throw new NulsException(new Exception("Transaction signature error !"));
                    }else{
                        signers.add(AddressTool.getStringAddressByBytes(AddressTool.getAddress(signature.getPublicKey(), chain.getChainId())));
                    }
                }
            }
            Map<String, Object> map = new HashMap<>(TxConstant.INIT_CAPACITY_2);
            map.put("list", signers);
            return success(map);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_LOCK, version = 1.0, description = "锁定账户")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "地址")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "是否成功")
    }))
    public Response lockAddress(Map params){
        Chain chain = null;
        try {
            // check parameters
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("address"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            String address = (String) params.get("address");
            Map<String, Object> map = new HashMap<>(TxConstant.INIT_CAPACITY_2);
            map.put("value", lockedAddressStorageService.save(chain.getChainId(), address));
            return success(map);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_UNLOCK, version = 1.0, description = "解锁账户")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "地址")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "是否成功")
    }))
    public Response unLockAddress(Map params){
        Chain chain = null;
        try {
            // check parameters
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("address"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            String address = (String) params.get("address");
            Map<String, Object> map = new HashMap<>(TxConstant.INIT_CAPACITY_2);
            map.put("value", lockedAddressStorageService.delete(chain.getChainId(), address));
            return success(map);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_ISLOCKED, version = 1.0, description = "返回地址是否已被锁定")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "待检查地址")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "true:已锁定, false:未锁定")
    }))
    public Response isLocked(Map params){
        Chain chain = null;
        try {
            // check parameters
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("address"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            String address = (String)params.get("address");
            String lockedAddress =  lockedAddressStorageService.find(chain.getChainId(), address);
            Map<String, Object> map = new HashMap<>(TxConstant.INIT_CAPACITY_2);
            // 返回true 表示被锁定
            map.put("value", StringUtils.isNotBlank(lockedAddress));
            return success(map);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = io.nuls.transaction.constant.TxCmd.TX_ALL_LOCKED_ADDRESS, version = 1.0, description = "获取所有已被锁定的地址列表")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = List.class, description = "地址列表")
    }))
    public Response getAllLocked(Map params){
        Chain chain = null;
        try {
            // check parameters
            ObjectUtils.canNotEmpty(params.get("chainId"), TxErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsException(TxErrorCode.CHAIN_NOT_FOUND);
            }
            Map<String, Object> map = new HashMap<>(TxConstant.INIT_CAPACITY_2);
            map.put("value", lockedAddressStorageService.findAll(chain.getChainId()));
            return success(map);
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }



    private void errorLogProcess(Chain chain, Exception e) {
        if (chain == null) {
            LOG.error(e);
        } else {
            chain.getLogger().error(e);
        }
    }

}
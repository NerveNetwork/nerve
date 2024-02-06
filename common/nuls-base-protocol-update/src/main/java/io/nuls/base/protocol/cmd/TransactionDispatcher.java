package io.nuls.base.protocol.cmd;

import io.nuls.base.RPCUtil;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.CommonAdvice;
import io.nuls.base.protocol.ModuleTxPackageProcessor;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.Log;
import io.nuls.core.model.ObjectUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.CmdAnnotation;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.NerveCoreCmd;
import io.nuls.core.rpc.model.Parameter;
import io.nuls.core.rpc.model.message.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Transaction distributor
 *
 * @author captain
 * @version 1.0
 * @date 2019/5/24 19:02
 */
@Component
@NerveCoreCmd(module = ModuleE.NC)
public final class TransactionDispatcher extends BaseCmd {

    private List<TransactionProcessor> processors;
    /**
     * When packaging transactions,Module Unified Transaction Internal Generation Processor
     */
    private ModuleTxPackageProcessor moduleTxPackageProcessor;

    public void setModuleTxPackageProcessor(ModuleTxPackageProcessor moduleTxPackageProcessor) {
        this.moduleTxPackageProcessor = moduleTxPackageProcessor;
    }

    public void setProcessors(List<TransactionProcessor> processors) {
//        processors.forEach(e -> Log.info("register TransactionProcessor-" + e.toString()));
        processors.sort(TransactionProcessor.COMPARATOR);
        this.processors = processors;
    }

    @Autowired("EmptyCommonAdvice")
    private CommonAdvice commitAdvice;
    @Autowired("EmptyCommonAdvice")
    private CommonAdvice rollbackAdvice;
    @Autowired("EmptyCommonAdvice")
    private CommonAdvice validatorAdvice;

    public void register(CommonAdvice commitAdvice, CommonAdvice rollbackAdvice) {
        if (commitAdvice != null) {
            this.commitAdvice = commitAdvice;
        }
        if (rollbackAdvice != null) {
            this.rollbackAdvice = rollbackAdvice;
        }
    }

    public void register(CommonAdvice commitAdvice, CommonAdvice rollbackAdvice, CommonAdvice validatorAdvice) {
        if (commitAdvice != null) {
            this.commitAdvice = commitAdvice;
        }
        if (rollbackAdvice != null) {
            this.rollbackAdvice = rollbackAdvice;
        }
        if (validatorAdvice != null) {
            this.validatorAdvice = validatorAdvice;
        }
    }

    /**
     * Internally generated transactions and validators
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = BaseConstant.TX_PACKPRODUCE, version = 1.0, description = "")
    @Parameter(parameterName = "chainId", parameterType = "int")
    @Parameter(parameterName = "txList", parameterType = "List")
    @Parameter(parameterName = "process", parameterType = "int")
    @Parameter(parameterName = "height", parameterType = "long")
    @Parameter(parameterName = "blockTime", parameterType = "long")
    public Response txPackProduce(Map params) {
        ObjectUtils.canNotEmpty(params.get(Constants.CHAIN_ID), CommonCodeConstanst.PARAMETER_ERROR.getMsg());
        ObjectUtils.canNotEmpty(params.get("process"), CommonCodeConstanst.PARAMETER_ERROR.getMsg());
        ObjectUtils.canNotEmpty(params.get("height"), CommonCodeConstanst.PARAMETER_ERROR.getMsg());
        ObjectUtils.canNotEmpty(params.get("blockTime"), CommonCodeConstanst.PARAMETER_ERROR.getMsg());
        try {
            int chainId = Integer.parseInt(params.get(Constants.CHAIN_ID).toString());
            int process = Integer.parseInt(params.get("process").toString());
            long blockTime = Long.parseLong(params.get("blockTime").toString());
            long height = Long.parseLong(params.get("height").toString());
            List<Transaction> txs = new ArrayList<>();
            List<String> txList = (List<String>) params.get("txList");
            if (null != txList) {
                for (String txStr : txList) {
                    Transaction tx = RPCUtil.getInstanceRpcStr(txStr, Transaction.class);
                    txs.add(tx);
                }
            }
            //1.newlyListNewly generated transactions, 2.rmHashListOriginal transactions that need to be deletedhash
            Map<String, List<String>> map = new HashMap<>();
            if (null != moduleTxPackageProcessor) {
                map = moduleTxPackageProcessor.packProduce(chainId, txs, process, height, blockTime);
            }
            return success(map);
        } catch (NulsException e) {
            Log.error(e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            Log.error(e);
            return failed(CommonCodeConstanst.SYS_UNKOWN_EXCEPTION);
        }

    }


    /**
     * Module validator for transactions
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = BaseConstant.TX_VALIDATOR, version = 1.0, description = "")
    @Parameter(parameterName = "chainId", parameterType = "int")
    @Parameter(parameterName = "txList", parameterType = "List")
    @Parameter(parameterName = "blockHeader", parameterType = "String")
    public Response txValidator(Map params) {
        try {
            ObjectUtils.canNotEmpty(params.get(Constants.CHAIN_ID), CommonCodeConstanst.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txList"), CommonCodeConstanst.PARAMETER_ERROR.getMsg());
            int chainId = Integer.parseInt(params.get(Constants.CHAIN_ID).toString());
            String blockHeaderStr = (String) params.get("blockHeader");
            BlockHeader blockHeader = null;
            if (StringUtils.isNotBlank(blockHeaderStr)) {
                blockHeader = RPCUtil.getInstanceRpcStr(blockHeaderStr, BlockHeader.class);
            }
            List<String> txList = (List<String>) params.get("txList");
            List<Transaction> txs = new ArrayList<>();
            List<Transaction> finalInvalidTxs = new ArrayList<>();
            for (String txStr : txList) {
                Transaction tx = RPCUtil.getInstanceRpcStr(txStr, Transaction.class);
                txs.add(tx);
            }
            validatorAdvice.begin(chainId, txs, blockHeader, 1);
            String errorCode = "";
            Map<String, Object> validateMap;
//        Map<String, Object> validateMap = validatorAdvice.validates(chainId, txs, blockHeader);
//        if (validateMap != null) {
//            List<Transaction> invalidTxs = (List<Transaction>) validateMap.get("txList");
//            if (invalidTxs != null && !invalidTxs.isEmpty()) {
//                errorCode = (String) validateMap.get("errorCode");
//                finalInvalidTxs.addAll(invalidTxs);
//            }
//        }

            Map<Integer, List<Transaction>> map = new HashMap<>();
            for (TransactionProcessor processor : processors) {
                for (Transaction tx : txs) {
                    List<Transaction> transactions = map.computeIfAbsent(processor.getType(), k -> new ArrayList<>());
                    if (tx.getType() == processor.getType()) {
                        if (null != blockHeader) {
                            tx.setBlockHeight(blockHeader.getHeight());
                        }
                        transactions.add(tx);
                    }
                }
            }

            for (TransactionProcessor processor : processors) {
                validateMap = processor.validate(chainId, map.get(processor.getType()), map, blockHeader);
                if (validateMap == null) {
                    continue;
                }
                List<Transaction> invalidTxs = (List<Transaction>) validateMap.get("txList");
                //List<Transaction> invalidTxs = processor.validate(chainId, map.get(processor.getType()), map, blockHeader);
                if (invalidTxs != null && !invalidTxs.isEmpty()) {
                    errorCode = (String) validateMap.get("errorCode");
                    finalInvalidTxs.addAll(invalidTxs);
                    invalidTxs.forEach(e -> map.get(e.getType()).remove(e));
                }
            }
            Map<String, Object> resultMap = new HashMap<>(2);
            List<String> list = finalInvalidTxs.stream().map(e -> e.getHash().toHex()).collect(Collectors.toList());
            resultMap.put("errorCode", errorCode);
            resultMap.put("list", list);
            validatorAdvice.end(chainId, txs, blockHeader);
            return success(resultMap);
        } catch (Exception e) {
            ErrorCode code = CommonCodeConstanst.DATA_ERROR;
            String msg = e.getMessage() == null ? code.getMsg() : e.getMessage();
            if (e instanceof NulsException) {
                NulsException e1 = (NulsException) e;
                code = e1.getErrorCode();
                msg = e1.format();
            } else if (e instanceof NulsRuntimeException) {
                NulsRuntimeException e1 = (NulsRuntimeException) e;
                code = e1.getErrorCode();
                msg = e1.format();
            }
            return failed(code, msg);
        }
    }

    /**
     * Transaction business submission
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = BaseConstant.TX_COMMIT, version = 1.0, description = "")
    @Parameter(parameterName = "chainId", parameterType = "int")
    @Parameter(parameterName = "txList", parameterType = "List")
    @Parameter(parameterName = "blockHeader", parameterType = "String")
    @Parameter(parameterName = "syncStatus", parameterType = "int")
    public Response txCommit(Map params) {
        ObjectUtils.canNotEmpty(params.get(Constants.CHAIN_ID), CommonCodeConstanst.PARAMETER_ERROR.getMsg());
        ObjectUtils.canNotEmpty(params.get("txList"), CommonCodeConstanst.PARAMETER_ERROR.getMsg());
        ObjectUtils.canNotEmpty(params.get("blockHeader"), CommonCodeConstanst.PARAMETER_ERROR.getMsg());
        ObjectUtils.canNotEmpty(params.get("syncStatus"), CommonCodeConstanst.PARAMETER_ERROR.getMsg());

        int chainId = Integer.parseInt(params.get(Constants.CHAIN_ID).toString());
        String blockHeaderStr = (String) params.get("blockHeader");
        BlockHeader blockHeader = RPCUtil.getInstanceRpcStr(blockHeaderStr, BlockHeader.class);
        List<String> txList = (List<String>) params.get("txList");
        List<Transaction> txs = new ArrayList<>();
        for (String txStr : txList) {
            Transaction tx = RPCUtil.getInstanceRpcStr(txStr, Transaction.class);
            txs.add(tx);
        }
        int syncStatus = (int) params.get("syncStatus");
        commitAdvice.begin(chainId, txs, blockHeader, syncStatus);
        Map<String, Boolean> resultMap = new HashMap<>(2);
        boolean handle = commitAdvice.handle(chainId, txs, blockHeader, syncStatus, resultMap, processors);
        if (!handle) {
            Map<Integer, List<Transaction>> map = new HashMap<>();
            for (TransactionProcessor processor : processors) {
                for (Transaction tx : txs) {
                    List<Transaction> transactions = map.computeIfAbsent(processor.getType(), k -> new ArrayList<>());
                    if (tx.getType() == processor.getType()) {
                        transactions.add(tx);
                    }
                }
            }
            List<TransactionProcessor> completedProcessors = new ArrayList<>();
            for (TransactionProcessor processor : processors) {
                //Log.info("[{}]type: {}, processor: {}", blockHeader.getHeight(), processor.getType(), processor.getClass().getSimpleName());
                boolean commit = processor.commit(chainId, map.get(processor.getType()), blockHeader, syncStatus);
                if (!commit) {
                    completedProcessors.forEach(e -> e.rollback(chainId, map.get(e.getType()), blockHeader));
                    resultMap.put("value", commit);
                    return success(resultMap);
                } else {
                    completedProcessors.add(processor);
                }
            }
            resultMap.put("value", true);
        }
        commitAdvice.end(chainId, txs, blockHeader);
        return success(resultMap);
    }

    /**
     * Transaction business rollback
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = BaseConstant.TX_ROLLBACK, version = 1.0, description = "")
    @Parameter(parameterName = "chainId", parameterType = "int")
    @Parameter(parameterName = "txList", parameterType = "List")
    @Parameter(parameterName = "blockHeader", parameterType = "String")
    public Response txRollback(Map params) {
        ObjectUtils.canNotEmpty(params.get(Constants.CHAIN_ID), CommonCodeConstanst.PARAMETER_ERROR.getMsg());
        ObjectUtils.canNotEmpty(params.get("txList"), CommonCodeConstanst.PARAMETER_ERROR.getMsg());
        ObjectUtils.canNotEmpty(params.get("blockHeader"), CommonCodeConstanst.PARAMETER_ERROR.getMsg());
        int chainId = Integer.parseInt(params.get(Constants.CHAIN_ID).toString());
        String blockHeaderStr = (String) params.get("blockHeader");
        BlockHeader blockHeader = RPCUtil.getInstanceRpcStr(blockHeaderStr, BlockHeader.class);
        List<String> txList = (List<String>) params.get("txList");
        List<Transaction> txs = new ArrayList<>();
        for (String txStr : txList) {
            Transaction tx = RPCUtil.getInstanceRpcStr(txStr, Transaction.class);
            txs.add(tx);
        }
        rollbackAdvice.begin(chainId, txs, blockHeader, 0);
        Map<Integer, List<Transaction>> map = new HashMap<>();
        for (TransactionProcessor processor : processors) {
            for (Transaction tx : txs) {
                List<Transaction> transactions = map.computeIfAbsent(processor.getType(), k -> new ArrayList<>());
                if (tx.getType() == processor.getType()) {
                    transactions.add(tx);
                }
            }
        }
        Map<String, Boolean> resultMap = new HashMap<>(2);
        List<TransactionProcessor> completedProcessors = new ArrayList<>();
        for (TransactionProcessor processor : processors) {
            boolean rollback = processor.rollback(chainId, map.get(processor.getType()), blockHeader);
            if (!rollback) {
                completedProcessors.forEach(e -> e.commit(chainId, map.get(e.getType()), blockHeader, 0));
                resultMap.put("value", rollback);
                return success(resultMap);
            } else {
                completedProcessors.add(processor);
            }
        }
        resultMap.put("value", true);
        rollbackAdvice.end(chainId, txs, blockHeader);
        return success(resultMap);
    }

}

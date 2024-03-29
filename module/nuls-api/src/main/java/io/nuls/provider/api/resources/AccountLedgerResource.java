/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2019 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.nuls.provider.api.resources;

import io.nuls.base.api.provider.crosschain.CrossChainProvider;
import io.nuls.base.api.provider.crosschain.facade.CreateCrossTxReq;
import io.nuls.provider.api.config.Config;
import io.nuls.base.RPCUtil;
import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.ledger.LedgerProvider;
import io.nuls.base.api.provider.transaction.TransferService;
import io.nuls.base.api.provider.transaction.facade.TransferReq;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.model.*;
import io.nuls.provider.model.ErrorData;
import io.nuls.provider.model.RpcClientResult;
import io.nuls.provider.model.dto.AccountBalanceDto;
import io.nuls.provider.model.form.BalanceForm;
import io.nuls.provider.model.form.TransferForm;
import io.nuls.provider.model.form.TxForm;
import io.nuls.provider.rpctools.ContractTools;
import io.nuls.provider.rpctools.LegderTools;
import io.nuls.provider.rpctools.TransactionTools;
import io.nuls.provider.rpctools.vo.AccountBalance;
import io.nuls.provider.utils.Log;
import io.nuls.provider.utils.ResultUtil;
import io.nuls.v2.model.annotation.Api;
import io.nuls.v2.model.annotation.ApiOperation;
import io.nuls.v2.model.dto.*;
import io.nuls.v2.txdata.CallContractData;
import io.nuls.v2.txdata.CreateContractData;
import io.nuls.v2.txdata.DeleteContractData;
import io.nuls.v2.util.CommonValidator;
import io.nuls.v2.util.NulsSDKTool;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static io.nuls.core.constant.TxType.*;
import static io.nuls.provider.utils.Utils.extractTxTypeFromTx;

/**
 * @author: PierreLuo
 * @date: 2019-06-27
 */
@Path("/api/accountledger")
@Component
@Api
public class AccountLedgerResource {

    @Autowired
    Config config;

    TransferService transferService = ServiceManager.get(TransferService.class);
    CrossChainProvider crossChainProvider = ServiceManager.get(CrossChainProvider.class);
    LedgerProvider ledgerProvider = ServiceManager.get(LedgerProvider.class);
    @Autowired
    TransactionTools transactionTools;
    @Autowired
    private ContractTools contractTools;
    @Autowired
    private LegderTools legderTools;

    @POST
    @Path("/balance/{address}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Query account balance", order = 109, detailDesc = "According to the asset chainIDAnd assetsID, query the balance of assets corresponding to this chain account andnoncevalue")
    @Parameters({
            @Parameter(parameterName = "balanceDto", parameterDes = "Account Balance Form", requestType = @TypeDescriptor(value = BalanceForm.class))
    })
    @ResponseData(name = "Return value", responseType = @TypeDescriptor(value = AccountBalanceDto.class))
    public RpcClientResult getBalance(@PathParam("address") String address, BalanceForm form) {
        if (!AddressTool.validAddress(config.getChainId(), address)) {
            return RpcClientResult.getFailed(new ErrorData(CommonCodeConstanst.PARAMETER_ERROR.getCode(), "address is invalid"));
        }
        if (form.getAssetChainId() < 1 || form.getAssetChainId() > 65535) {
            return RpcClientResult.getFailed(new ErrorData(CommonCodeConstanst.PARAMETER_ERROR.getCode(), "assetChainId is invalid"));
        }
        if (form.getAssetId() < 1 || form.getAssetId() > 65535) {
            return RpcClientResult.getFailed(new ErrorData(CommonCodeConstanst.PARAMETER_ERROR.getCode(), "assetId is invalid"));
        }
        Result<AccountBalance> balanceResult = legderTools.getBalanceAndNonce(config.getChainId(), form.getAssetChainId(), form.getAssetId(), address);
        RpcClientResult clientResult = ResultUtil.getRpcClientResult(balanceResult);
        if (clientResult.isSuccess()) {
            clientResult.setData(new AccountBalanceDto((AccountBalance) clientResult.getData()));
        }
        return clientResult;
    }

    @POST
    @Path("/transaction/validate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Verify transactions", order = 302, detailDesc = "Verify transactions for offline assembly,Successful verification returns transactionhashvalue,Failure returns error message")
    @Parameters({
            @Parameter(parameterName = "Verify if the transaction is correct", parameterDes = "Verify the correctness of the transaction form", requestType = @TypeDescriptor(value = TxForm.class))
    })
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
    }))
    public RpcClientResult validate(TxForm form) {
        if (form == null || StringUtils.isBlank(form.getTxHex())) {
            return RpcClientResult.getFailed(new ErrorData(CommonCodeConstanst.PARAMETER_ERROR.getCode(), "form is empty"));
        }
        Result result = transactionTools.validateTx(config.getChainId(), form.getTxHex());
        return ResultUtil.getRpcClientResult(result);
    }

    @POST
    @Path("/transaction/broadcast")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Broadcasting transactions", order = 303, detailDesc = "Broadcast offline assembly transactions,Successfully returnedtrue,Failure returns error message")
    @Parameters({
            @Parameter(parameterName = "Broadcasting transactions", parameterDes = "Broadcast transaction form", requestType = @TypeDescriptor(value = TxForm.class))
    })
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "Whether successful"),
            @Key(name = "hash", description = "transactionhash")
    }))
    public RpcClientResult broadcast(TxForm form) {
        if (form == null || StringUtils.isBlank(form.getTxHex())) {
            return RpcClientResult.getFailed(new ErrorData(CommonCodeConstanst.PARAMETER_ERROR.getCode(), "form is empty"));
        }
        try {
            String txHex = form.getTxHex();
            int type = extractTxTypeFromTx(txHex);
            //if (type == SWAP_TRADE_STABLE_COIN) {
            //    return RpcClientResult.getFailed(new ErrorData(CommonCodeConstanst.PARAMETER_ERROR.getCode(), "This transaction is suspended"));
            //}
            Result result = null;
            switch (type) {
                case CREATE_CONTRACT:
                    Transaction tx = new Transaction();
                    tx.parse(new NulsByteBuffer(RPCUtil.decode(txHex)));
                    CreateContractData create = new CreateContractData();
                    create.parse(new NulsByteBuffer(tx.getTxData()));
                    result = contractTools.validateContractCreate(config.getChainId(),
                            AddressTool.getStringAddressByBytes(create.getSender()),
                            create.getGasLimit(),
                            create.getPrice(),
                            RPCUtil.encode(create.getCode()),
                            create.getArgs());
                    break;
                case CALL_CONTRACT:
                    Transaction callTx = new Transaction();
                    callTx.parse(new NulsByteBuffer(RPCUtil.decode(txHex)));
                    CallContractData call = new CallContractData();
                    call.parse(new NulsByteBuffer(callTx.getTxData()));
                    result = contractTools.validateContractCall(config.getChainId(),
                            AddressTool.getStringAddressByBytes(call.getSender()),
                            call.getValue(),
                            call.getGasLimit(),
                            call.getPrice(),
                            AddressTool.getStringAddressByBytes(call.getContractAddress()),
                            call.getMethodName(),
                            call.getMethodDesc(),
                            call.getArgs());
                    break;
                case DELETE_CONTRACT:
                    Transaction deleteTx = new Transaction();
                    deleteTx.parse(new NulsByteBuffer(RPCUtil.decode(txHex)));
                    DeleteContractData delete = new DeleteContractData();
                    delete.parse(new NulsByteBuffer(deleteTx.getTxData()));
                    result = contractTools.validateContractDelete(config.getChainId(),
                            AddressTool.getStringAddressByBytes(delete.getSender()),
                            AddressTool.getStringAddressByBytes(delete.getContractAddress()));
                    break;
                default:
                    break;
            }
            if (result != null) {
                Map contractMap = (Map) result.getData();
                if (contractMap != null && Boolean.FALSE.equals(contractMap.get("success"))) {
                    return RpcClientResult.getFailed((String) contractMap.get("msg"));
                }
            }
            result = transactionTools.newTx(config.getChainId(), txHex);
            return ResultUtil.getRpcClientResult(result);
        } catch (Exception e) {
            Log.error(e);
            return RpcClientResult.getFailed(e.getMessage());
        }
    }

    @POST
    @Path("/transaction/broadcastWithNoContractValidation")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Broadcasting transactions(Not verifying contracts)", order = 304, detailDesc = "Broadcast offline assembly transactions(Not verifying contracts),Successfully returnedtrue,Failure returns error message")
    @Parameters({
            @Parameter(parameterName = "Broadcasting transactions(Not verifying contracts)", parameterDes = "Broadcasting transactions(Not verifying contracts)form", requestType = @TypeDescriptor(value = TxForm.class))
    })
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "Whether successful"),
            @Key(name = "hash", description = "transactionhash")
    }))
    public RpcClientResult broadcastWithNoContractValidation(TxForm form) {
        if (form == null || StringUtils.isBlank(form.getTxHex())) {
            return RpcClientResult.getFailed(new ErrorData(CommonCodeConstanst.PARAMETER_ERROR.getCode(), "form is empty"));
        }
        try {
            String txHex = form.getTxHex();
            //int type = extractTxTypeFromTx(txHex);
            //if (type == SWAP_TRADE_STABLE_COIN) {
            //    return RpcClientResult.getFailed(new ErrorData(CommonCodeConstanst.PARAMETER_ERROR.getCode(), "This transaction is suspended"));
            //}
            Result result = transactionTools.newTx(config.getChainId(), txHex);
            return ResultUtil.getRpcClientResult(result);
        } catch (Exception e) {
            Log.error(e);
            return RpcClientResult.getFailed(e.getMessage());
        }
    }

    @POST
    @Path("/transfer")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Single transfer", order = 305, detailDesc = "Initiate transfer transactions for a single account or asset")
    @Parameters({
            @Parameter(parameterName = "Single transfer", parameterDes = "Single transfer form", requestType = @TypeDescriptor(value = TransferForm.class))
    })
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
    }))
    public RpcClientResult transfer(TransferForm form) {
        if (form == null) {
            return RpcClientResult.getFailed(new ErrorData(CommonCodeConstanst.PARAMETER_ERROR.getCode(), "form is empty"));
        }
        if (form.getAssetChainId() == 0) {
            form.setAssetChainId(config.getChainId());
        }
        if (form.getAssetId() == 0) {
            form.setAssetId(config.getAssetsId());
        }
        TransferReq.TransferReqBuilder builder =
                new TransferReq.TransferReqBuilder(config.getChainId(), form.getAssetId())
                        .addForm(form.getAddress(), form.getPassword(), form.getAmount())
                        .addTo(form.getToAddress(), form.getAmount()).setRemark(form.getRemark());
        TransferReq req = builder.build(new TransferReq());
        req.getInputs().get(0).setAssetsChainId(form.getAssetChainId());
        req.getOutputs().get(0).setAssetsChainId(form.getAssetChainId());
        Result<String> result = transferService.transfer(req);
        RpcClientResult clientResult = ResultUtil.getRpcClientResult(result);
        if (clientResult.isSuccess()) {
            return clientResult.resultMap().map("value", clientResult.getData()).mapToData();
        }
        return clientResult;
    }

    @POST
    @Path("/crossTransfer")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Cross chain transfer", order = 306, detailDesc = "Initiate cross chain transfer transactions for single account and single asset transactions")
    @Parameters({
            @Parameter(parameterName = "Cross chain transfer", parameterDes = "Cross chain transfer form", requestType = @TypeDescriptor(value = CrossTransferForm.class))
    })
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
    }))
    public RpcClientResult crossTransfer(CrossTransferForm form) {
        if (form == null) {
            return RpcClientResult.getFailed(new ErrorData(CommonCodeConstanst.PARAMETER_ERROR.getCode(), "form is empty"));
        }
        CreateCrossTxReq.CreateCrossTxReqBuilder builder = new CreateCrossTxReq.CreateCrossTxReqBuilder(config.getChainId())
                .addForm(form.getAssetChainId(), form.getAssetId(), form.getAddress(), form.getPassword(), form.getAmount())
                .addTo(form.getAssetChainId(), form.getAssetId(), form.getToAddress(), form.getAmount())
                .setRemark(form.getRemark());
        Result<String> result = crossChainProvider.createCrossTx(builder.build());
        RpcClientResult clientResult = ResultUtil.getRpcClientResult(result);
        if (clientResult.isSuccess()) {
            return clientResult.resultMap().map("txHash", clientResult.getData()).mapToData();
        }
        return clientResult;
    }

    @POST
    @Path("/createTransferTxOffline")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Offline assembly transfer transaction", order = 350, detailDesc = "according toinputsandoutputsOffline assembly transfer transaction, used for single account or multi account transfer transactions." +
            "The transaction fee isinputsThe total amount of main assets in the Li Ben Chain, minusoutputsThe total amount of main assets in the Li Ben Chain")
    @Parameters({
            @Parameter(parameterName = "transferDto", parameterDes = "Transfer transaction form", requestType = @TypeDescriptor(value = TransferDto.class))
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "hash", description = "transactionhash"),
            @Key(name = "txHex", description = "Transaction serialization16Hexadecimal Strings")
    }))
    public RpcClientResult createTransferTxOffline(TransferDto transferDto) {
        try {
            CommonValidator.checkTransferDto(transferDto);
            io.nuls.core.basic.Result result = NulsSDKTool.createTransferTxOffline(transferDto);
            return ResultUtil.getRpcClientResult(result);
        } catch (NulsException e) {
            return RpcClientResult.getFailed(new ErrorData(e.getErrorCode().getCode(), e.getMessage()));
        }
    }

    @POST
    @Path("/createCrossTxOffline")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Offline assembly of cross chain transfer transactions", order = 350, detailDesc = "according toinputsandoutputsOffline assembly of cross chain transfer transactions, used for single account or multi account transfer transactions." +
            "The transaction fee isinputsThe total amount of main assets in the Li Ben Chain, minusoutputsThe total amount of main assets in the local chain, plus the cross chain transferNULSHandling fees")
    @Parameters({
            @Parameter(parameterName = "transferDto", parameterDes = "Cross chain transfer transaction form", requestType = @TypeDescriptor(value = TransferDto.class))
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "hash", description = "transactionhash"),
            @Key(name = "txHex", description = "Transaction serialization16Hexadecimal Strings")
    }))
    public RpcClientResult createCrossTxOffline(TransferDto transferDto) {
        try {
            CommonValidator.checkTransferDto(transferDto);
            io.nuls.core.basic.Result result = NulsSDKTool.createCrossTransferTxOffline(transferDto);
            return ResultUtil.getRpcClientResult(result);
        } catch (NulsException e) {
            return RpcClientResult.getFailed(new ErrorData(e.getErrorCode().getCode(), e.getMessage()));
        }
    }

    @POST
    @Path("/calcTransferTxFee")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Calculate the transaction fee required for offline creation of transfer transactions", order = 351)
    @Parameters({
            @Parameter(parameterName = "TransferTxFeeDto", parameterDes = "Transfer transaction fees", requestType = @TypeDescriptor(value = TransferTxFeeDto.class))
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "Transaction fees"),
    }))
    public RpcClientResult calcTransferTxFee(TransferTxFeeDto dto) {
        BigInteger fee = NulsSDKTool.calcTransferTxFee(dto);
        Map map = new HashMap();
        map.put("value", fee);
        RpcClientResult result = RpcClientResult.getSuccess(map);
        return result;
    }

    @POST
    @Path("/calcCrossTxFee")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Calculate the transaction fee required for offline creation of cross chain transfer transactions", order = 351)
    @Parameters({
            @Parameter(parameterName = "TransferTxFeeDto", parameterDes = "Transfer transaction fees", requestType = @TypeDescriptor(value = TransferTxFeeDto.class))
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "Transaction fees"),
    }))
    public RpcClientResult calcCrossTxFee(CrossTransferTxFeeDto dto) {
        Map<String, BigInteger> map = NulsSDKTool.calcCrossTransferTxFee(dto);
        RpcClientResult result = RpcClientResult.getSuccess(map);
        return result;
    }

    @POST
    @Path("/createMultiSignTransferTxOffline")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Multiple account offline assembly transfer transactions", order = 352, detailDesc = "according toinputsandoutputsOffline assembly transfer transaction, used for single account or multi account transfer transactions." +
            "The transaction fee isinputsThe total amount of main assets in the Li Ben Chain, minusoutputsThe total amount of main assets in the Li Ben Chain")
    @Parameters({
            @Parameter(parameterName = "transferDto", parameterDes = "Multi signature account transfer transaction form", requestType = @TypeDescriptor(value = MultiSignTransferDto.class))
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "hash", description = "transactionhash"),
            @Key(name = "txHex", description = "Transaction serialization16Hexadecimal Strings")
    }))
    public RpcClientResult createMultiTransferTxOffline(MultiSignTransferDto transferDto) {
        try {
            CommonValidator.checkMultiSignTransferDto(transferDto);
            io.nuls.core.basic.Result result = NulsSDKTool.createMultiSignTransferTxOffline(transferDto);
            return ResultUtil.getRpcClientResult(result);
        } catch (NulsException e) {
            return RpcClientResult.getFailed(new ErrorData(e.getErrorCode().getCode(), e.getMessage()));
        }
    }

    @POST
    @Path("/calcMultiSignTransferTxFee")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(description = "Calculate the transaction fee required for offline creation of multi signature account transfer transactions", order = 353)
    @Parameters({
            @Parameter(parameterName = "MultiSignTransferTxFeeDto", parameterDes = "Multiple account transfer transaction fee form", requestType = @TypeDescriptor(value = MultiSignTransferTxFeeDto.class))
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "Transaction fees"),
    }))
    public RpcClientResult calcMultiSignTransferTxFee(MultiSignTransferTxFeeDto dto) {
        BigInteger fee = NulsSDKTool.calcMultiSignTransferTxFee(dto);
        Map map = new HashMap();
        map.put("value", fee);
        RpcClientResult result = RpcClientResult.getSuccess(map);
        return result;
    }

}

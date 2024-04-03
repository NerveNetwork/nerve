/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.provider.api.jsonrpc.controller;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.account.AccountService;
import io.nuls.base.api.provider.account.facade.*;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Controller;
import io.nuls.core.core.annotation.RpcMethod;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.FormatValidUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.model.*;
import io.nuls.provider.api.config.Config;
import io.nuls.provider.api.config.Context;
import io.nuls.provider.model.dto.AccountBlockDTO;
import io.nuls.provider.model.dto.AccountKeyStoreDto;
import io.nuls.provider.model.dto.AccountWhitelistDTO;
import io.nuls.provider.model.form.PriKeyForm;
import io.nuls.provider.model.jsonrpc.RpcErrorCode;
import io.nuls.provider.model.jsonrpc.RpcResult;
import io.nuls.provider.model.jsonrpc.RpcResultError;
import io.nuls.provider.rpctools.AccountTools;
import io.nuls.provider.rpctools.LegderTools;
import io.nuls.provider.rpctools.vo.AccountBalance;
import io.nuls.provider.utils.ResultUtil;
import io.nuls.provider.utils.VerifyUtils;
import io.nuls.v2.error.AccountErrorCode;
import io.nuls.v2.model.annotation.Api;
import io.nuls.v2.model.annotation.ApiOperation;
import io.nuls.v2.model.annotation.ApiType;
import io.nuls.v2.model.dto.AccountDto;
import io.nuls.v2.model.dto.AliasDto;
import io.nuls.v2.model.dto.MultiSignAliasDto;
import io.nuls.v2.model.dto.SignDto;
import io.nuls.v2.util.NulsSDKTool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Eva
 */
@Controller
@Api(type = ApiType.JSONRPC)
public class AccountController {

    @Autowired
    private LegderTools legderTools;
    @Autowired
    private AccountTools accountTools;
    @Autowired
    private Config config;

    private long time;

    AccountService accountService = ServiceManager.get(AccountService.class);

    @RpcMethod("createAccount")
    @ApiOperation(description = "Batch Create Accounts", order = 101, detailDesc = "The created account exists in the local wallet")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "count", requestType = @TypeDescriptor(value = int.class), parameterDes = "Create quantity"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return account address set", responseType = @TypeDescriptor(value = List.class, collectionElement = String.class))
    public RpcResult createAccount(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int chainId, count;
        String password;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            count = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[count] is inValid");
        }
        try {
            password = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[password] is inValid");
        }
        if (!FormatValidUtils.validPassword(password)) {
            return RpcResult.paramError("[password] is inValid");
        }

        CreateAccountReq req = new CreateAccountReq(count, password);
        req.setChainId(chainId);
        Result<String> result = accountService.createAccount(req);
        RpcResult rpcResult = new RpcResult();
        if (result.isFailed()) {
            rpcResult.setError(new RpcResultError(result.getStatus(), result.getMessage(), null));
        } else {
            rpcResult.setResult(result.getList());
        }
        return rpcResult;
    }

    @RpcMethod("updatePassword")
    @ApiOperation(description = "Change account password", order = 102)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "Account address"),
            @Parameter(parameterName = "oldPassword", requestType = @TypeDescriptor(value = String.class), parameterDes = "Original password"),
            @Parameter(parameterName = "newPassword", requestType = @TypeDescriptor(value = String.class), parameterDes = "New password")
    })
    @ResponseData(name = "Return value", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = Boolean.class, description = "Is the modification successful")
    }))
    public RpcResult updatePassword(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        int chainId;
        String address, oldPassword, newPassword;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            address = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        try {
            oldPassword = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[oldPassword] is inValid");
        }
        try {
            newPassword = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[newPassword] is inValid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!FormatValidUtils.validPassword(oldPassword)) {
            return RpcResult.paramError("[oldPassword] is inValid");
        }
        if (!FormatValidUtils.validPassword(newPassword)) {
            return RpcResult.paramError("[newPassword] is inValid");
        }
        if (System.currentTimeMillis() - time < 3000) {
            return RpcResult.paramError("Access frequency limit");
        }
        time = System.currentTimeMillis();
        UpdatePasswordReq req = new UpdatePasswordReq(address, oldPassword, newPassword);
        req.setChainId(chainId);
        Result<Boolean> result = accountService.updatePassword(req);
        RpcResult rpcResult = new RpcResult();
        if (result.isSuccess()) {
            rpcResult.setResult(result.getData());
        } else {
            rpcResult.setError(new RpcResultError(result.getStatus(), result.getMessage(), null));
        }
        return rpcResult;
    }

    private long lastTime;

    @RpcMethod("getPriKey")
    @ApiOperation(description = "Export account private key", order = 103, detailDesc = "Only the private key of an existing account in the local wallet can be exported")
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "address", parameterDes = "Account address"),
            @Parameter(parameterName = "password", parameterDes = "password")
    })
    @ResponseData(name = "Return value", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "Private key")
    }))
    public RpcResult getPriKey(List<Object> params) {
        //Add a frequency limit
        if (System.currentTimeMillis() - lastTime < 2000) {
            return RpcResult.failed(RpcErrorCode.ExceedingFrequencyLimit);
        }
        int chainId;
        String address, password;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            address = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        try {
            password = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[password] is inValid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!FormatValidUtils.validPassword(password)) {
            return RpcResult.paramError("[password] is inValid");
        }

        if (System.currentTimeMillis() - time < 3000) {
            return RpcResult.paramError("Access frequency limit");
        }
        time = System.currentTimeMillis();
        GetAccountPrivateKeyByAddressReq req = new GetAccountPrivateKeyByAddressReq(password, address);
        req.setChainId(chainId);
        Result<String> result = accountService.getAccountPrivateKey(req);
        RpcResult rpcResult = new RpcResult();
        if (result.isSuccess()) {
            rpcResult.setResult(result.getData());
        } else {
            rpcResult.setError(new RpcResultError(result.getStatus(), result.getMessage(), null));
        }
        return rpcResult;
    }

    @RpcMethod("importPriKey")
    @ApiOperation(description = "Import account based on private key", order = 104, detailDesc = "When importing a private key, you need to enter a password to encrypt the plaintext private key")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "priKey", requestType = @TypeDescriptor(value = String.class), parameterDes = "Account plaintext private key"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "New password")
    })
    @ResponseData(name = "Return value", description = "Return account address", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "Account address")
    }))
    public RpcResult importPriKey(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int chainId;
        String priKey, password;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            priKey = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[priKey] is inValid");
        }
        try {
            password = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[password] is inValid");
        }
        if (StringUtils.isBlank(priKey)) {
            return RpcResult.paramError("[priKey] is inValid");
        }
        if (!FormatValidUtils.validPassword(password)) {
            return RpcResult.paramError("[password] is inValid");
        }

        ImportAccountByPrivateKeyReq req = new ImportAccountByPrivateKeyReq(password, priKey, true);
        req.setChainId(chainId);
        Result<String> result = accountService.importAccountByPrivateKey(req);
        RpcResult rpcResult = new RpcResult();
        if (result.isSuccess()) {
            rpcResult.setResult(result.getData());
        } else {
            rpcResult.setError(new RpcResultError(result.getStatus(), result.getMessage(), null));
        }
        return rpcResult;
    }

    @RpcMethod("importKeystore")
    @ApiOperation(description = "according tokeystoreImport account", order = 105)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "keyStoreJson", requestType = @TypeDescriptor(value = Map.class), parameterDes = "keyStoreJson"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "keystorepassword")
    })
    @ResponseData(name = "Return value", description = "Return account address", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "Account address")
    }))
    public RpcResult importKeystore(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int chainId;
        String password, keyStoreJson;
        Map keyStoreMap;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            keyStoreMap = (Map) params.get(1);
            keyStoreJson = JSONUtils.obj2json(keyStoreMap);
        } catch (Exception e) {
            return RpcResult.paramError("[keyStoreJson] is inValid");
        }
        try {
            password = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[password] is inValid");
        }
        if (!FormatValidUtils.validPassword(password)) {
            return RpcResult.paramError("[password] is inValid");
        }

        ImportAccountByKeyStoreReq req = new ImportAccountByKeyStoreReq(password, HexUtil.encode(keyStoreJson.getBytes()), true);
        req.setChainId(chainId);
        Result<String> result = accountService.importAccountByKeyStore(req);
        RpcResult rpcResult = new RpcResult();
        if (result.isSuccess()) {
            rpcResult.setResult(result.getData());
        } else {
            rpcResult.setError(new RpcResultError(result.getStatus(), result.getMessage(), null));
        }
        return rpcResult;
    }

    @RpcMethod("exportKeystore")
    @ApiOperation(description = "Account backup, exporting accountskeystoreinformation", order = 106)
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "Account address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "Account password")
    })
    @ResponseData(name = "Return value", description = "returnkeystorecharacter string", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "result", description = "keystore")
    }))
    public RpcResult exportKeystore(List<Object> params) {
        //Add a frequency limit
        if (System.currentTimeMillis() - lastTime < 2000) {
            return RpcResult.failed(RpcErrorCode.ExceedingFrequencyLimit);
        }

        VerifyUtils.verifyParams(params, 3);
        int chainId;
        String address, password;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            address = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        try {
            password = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[password] is inValid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!FormatValidUtils.validPassword(password)) {
            return RpcResult.paramError("[password] is inValid");
        }
        if (System.currentTimeMillis() - time < 3000) {
            return RpcResult.paramError("Access frequency limit");
        }
        time = System.currentTimeMillis();
        KeyStoreReq req = new KeyStoreReq(password, address);
        req.setChainId(chainId);
        Result<String> result = accountService.getAccountKeyStore(req);
        RpcResult rpcResult = new RpcResult();
        try {
            if (result.isSuccess()) {
                AccountKeyStoreDto keyStoreDto = JSONUtils.json2pojo(result.getData(), AccountKeyStoreDto.class);
                rpcResult.setResult(keyStoreDto);
            } else {
                rpcResult.setError(new RpcResultError(result.getStatus(), result.getMessage(), null));
            }
            return rpcResult;
        } catch (IOException e) {
            return RpcResult.failed(CommonCodeConstanst.DATA_PARSE_ERROR);
        }
    }

    @RpcMethod("getAccountBalance")
    @ApiOperation(description = "Query account balance", order = 107, detailDesc = "According to the asset chainIDAnd assetsID, query the balance of assets corresponding to this chain account andnoncevalue")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "assetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "The chain of assetsID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "assetID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "Account address")
    })
    @ResponseData(name = "Return value", responseType = @TypeDescriptor(value = AccountBalance.class))
    public RpcResult getAccountBalance(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        int chainId, assetChainId, assetId;
        String address;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            assetChainId = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[assetChainId] is inValid");
        }
        try {
            assetId = (int) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[assetId] is inValid");
        }
        try {
            address = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }

        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }
        RpcResult rpcResult = new RpcResult();
        Result<AccountBalance> balanceResult = legderTools.getBalanceAndNonce(chainId, assetChainId, assetId, address);
        if (balanceResult.isFailed()) {
            return rpcResult.setError(new RpcResultError(balanceResult.getStatus(), balanceResult.getMessage(), null));
        }
        return rpcResult.setResult(balanceResult.getData());
    }

    /**
     * Query total user assets
     * @param params
     * @return
     */
    @RpcMethod("getBalanceList")
    @ApiOperation(description = "Query account balance", order = 107, detailDesc = "According to the asset chainIDAnd assetsID, query the balance of assets corresponding to this chain account andnonceValue set")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "Account address"),
            @Parameter(parameterName = "assetIdList", requestType = @TypeDescriptor(value = List.class), parameterDes = "AssetsIDaggregate")
    })
    @ResponseData(name = "Return value", responseType = @TypeDescriptor(value = AccountBalance.class))
    public RpcResult getBalanceList(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        String address;
        int chainId;
        List<Map> coinDtoList;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            address = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        try {
            coinDtoList = (List<Map> ) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }

        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }
        RpcResult rpcResult = new RpcResult();
        Result<List<AccountBalance>> balanceResult = legderTools.getBalanceList(chainId, coinDtoList, address);
        if (balanceResult.isFailed()) {
            return rpcResult.setError(new RpcResultError(balanceResult.getStatus(), balanceResult.getMessage(), null));
        }
        return rpcResult.setResult(balanceResult.getData());
    }



    @RpcMethod("setAlias")
    @ApiOperation(description = "Set account alias", order = 108, detailDesc = "The alias format is1-20A combination of lowercase letters and numbers, setting an alias will destroy it1individualNULS")
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "Account address"),
            @Parameter(parameterName = "alias", requestType = @TypeDescriptor(value = String.class), parameterDes = "alias"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "Account password")
    })
    @ResponseData(name = "Return value", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "Setting up alias transactionshash")
    }))
    public RpcResult setAlias(List<Object> params) {
        int chainId;
        String address, alias, password;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            address = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        try {
            alias = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[alias] is inValid");
        }
        try {
            password = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[password] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.dataNotFound();
        }

        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!FormatValidUtils.validAlias(alias)) {
            return RpcResult.paramError("[alias] is inValid");
        }
        if (StringUtils.isBlank(password)) {
            return RpcResult.paramError("[password] is inValid");
        }
        SetAccountAliasReq aliasReq = new SetAccountAliasReq(password, address, alias);
        Result<String> result = accountService.setAccountAlias(aliasReq);
        RpcResult rpcResult = new RpcResult();
        if (result.isSuccess()) {
            rpcResult.setResult(result.getData());
        } else {
            rpcResult.setError(new RpcResultError(result.getStatus(), result.getMessage(), null));
        }
        return rpcResult;
    }

    @RpcMethod("validateAddress")
    @ApiOperation(description = "Verify if the address is correct", order = 109, detailDesc = "Verify if the address is correct")
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "Account address")
    })
    @ResponseData(name = "Return value", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "boolean")
    }))
    public RpcResult validateAddress(List<Object> params) {
        int chainId;
        String address;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            address = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        boolean b = AddressTool.validAddress(chainId, address);
        if (b) {
            return RpcResult.success(Map.of("value", true));
        } else {
            return RpcResult.failed(AccountErrorCode.ADDRESS_ERROR);
        }
    }

    @RpcMethod("createAccountOffline")
    @ApiOperation(description = "off-line - Batch Create Accounts", order = 151, detailDesc = "The created account will not be saved to the wallet,The interface directly returns the account'skeystoreinformation")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "count", requestType = @TypeDescriptor(value = int.class), parameterDes = "Create quantity"),
            @Parameter(parameterName = "prefix", requestType = @TypeDescriptor(value = String.class), parameterDes = "Address prefix", canNull = true),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return account information collection", responseType = @TypeDescriptor(value = List.class, collectionElement = AccountDto.class))
    public RpcResult createAccountOffline(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        int chainId, count;
        String prefix, password;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            count = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[count] is inValid");
        }
        try {
            prefix = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[prefix] is inValid");
        }
        try {
            password = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[password] is inValid");
        }
        if (!FormatValidUtils.validPassword(password)) {
            return RpcResult.paramError("[password] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.paramError(String.format("chainId [%s] is invalid", chainId));
        }
        io.nuls.core.basic.Result<List<AccountDto>> result;
        if (StringUtils.isBlank(prefix)) {
            result = NulsSDKTool.createOffLineAccount(count, password);
        } else {
            result = NulsSDKTool.createOffLineAccount(chainId, count, prefix, password);
        }
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getPriKeyOffline")
    @ApiOperation(description = "Offline acquisition of account plaintext private key", order = 152)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
            @Parameter(parameterName = "encryptedPrivateKey", parameterType = "String", parameterDes = "Account ciphertext private key"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "Clear text private key")
    }))
    public RpcResult getPriKeyOffline(List<Object> params) {
        int chainId;
        String address, encryptedPriKey, password;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            address = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        try {
            encryptedPriKey = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[encryptedPriKey] is inValid");
        }
        try {
            password = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[password] is inValid");
        }
        io.nuls.core.basic.Result result = NulsSDKTool.getPriKeyOffline(address, encryptedPriKey, password);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("resetPasswordOffline")
    @ApiOperation(description = "Offline account password modification", order = 153)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
            @Parameter(parameterName = "encryptedPrivateKey", parameterType = "String", parameterDes = "Account ciphertext private key"),
            @Parameter(parameterName = "oldPassword", parameterType = "String", parameterDes = "Original password"),
            @Parameter(parameterName = "newPassword", parameterType = "String", parameterDes = "New password")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "Encryption private key after resetting password")
    }))
    public RpcResult resetPasswordOffline(List<Object> params) {
        int chainId;
        String address, encryptedPriKey, oldPassword, newPassword;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            address = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        try {
            encryptedPriKey = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[encryptedPriKey] is inValid");
        }
        try {
            oldPassword = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[oldPassword] is inValid");
        }
        try {
            newPassword = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[newPassword] is inValid");
        }

        io.nuls.core.basic.Result result = NulsSDKTool.resetPasswordOffline(address, encryptedPriKey, oldPassword, newPassword);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("multiSign")
    @ApiOperation(description = "Multiple Account Summary Signature", order = 154, detailDesc = "Multi account transfer transaction for offline assembly of signatures,When calling the interface, parameters can be passed an address and private key, or an address and encryption private key and encryption password")
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "signDtoList", parameterDes = "Summary signature form", requestType = @TypeDescriptor(value = List.class, collectionElement = SignDto.class)),
            @Parameter(parameterName = "txHex", parameterType = "String", parameterDes = "Transaction serialization16Hexadecimal Strings")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "hash", description = "transactionhash"),
            @Key(name = "txHex", description = "Signed transaction16Hexadecimal Strings")
    }))
    public RpcResult multiSign(List<Object> params) {
        int chainId;
        String txHex;
        List<Map> signMap;
        List<SignDto> signDtoList = new ArrayList<>();
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.paramError(String.format("chainId [%s] is invalid", chainId));
        }

        try {
            signMap = (List<Map>) params.get(1);
            for (Map map : signMap) {
                SignDto signDto = JSONUtils.map2pojo(map, SignDto.class);
                signDtoList.add(signDto);
            }
        } catch (Exception e) {
            return RpcResult.paramError("[signDto] is inValid");
        }
        txHex = (String) params.get(2);

        io.nuls.core.basic.Result result = NulsSDKTool.sign(signDtoList, txHex);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("priKeySign")
    @ApiOperation(description = "Clear text private key digest signature", order = 155)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "txHex", parameterType = "String", parameterDes = "Transaction serialization16Hexadecimal Strings"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
            @Parameter(parameterName = "privateKey", parameterType = "String", parameterDes = "Account plaintext private key")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "hash", description = "transactionhash"),
            @Key(name = "txHex", description = "Signed transaction16Hexadecimal Strings")
    }))
    public RpcResult sign(List<Object> params) {
        int chainId;
        String txHex, address, priKey;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            txHex = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[txHex] is inValid");
        }
        try {
            address = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        try {
            priKey = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[priKey] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.paramError(String.format("chainId [%s] is invalid", chainId));
        }
        if (StringUtils.isBlank(txHex)) {
            return RpcResult.paramError("[txHex] is inValid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (StringUtils.isBlank(priKey)) {
            return RpcResult.paramError("[priKey] is inValid");
        }

        io.nuls.core.basic.Result result = NulsSDKTool.sign(txHex, address, priKey);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("encryptedPriKeySign")
    @ApiOperation(description = "Cryptography private key digest signature", order = 156)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "txHex", parameterType = "String", parameterDes = "Transaction serialization16Hexadecimal Strings"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
            @Parameter(parameterName = "encryptedPrivateKey", parameterType = "String", parameterDes = "Account ciphertext private key"),
            @Parameter(parameterName = "password", parameterType = "String", parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "hash", description = "transactionhash"),
            @Key(name = "txHex", description = "Signed transaction16Hexadecimal Strings")
    }))
    public RpcResult encryptedPriKeySign(List<Object> params) {
        int chainId;
        String txHex, address, encryptedPriKey, password;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            txHex = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[txHex] is inValid");
        }
        try {
            address = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        try {
            encryptedPriKey = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[encryptedPriKey] is inValid");
        }
        try {
            password = (String) params.get(4);
        } catch (Exception e) {
            return RpcResult.paramError("[password] is inValid");
        }
        if (StringUtils.isBlank(txHex)) {
            return RpcResult.paramError("[txHex] is inValid");
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (StringUtils.isBlank(encryptedPriKey)) {
            return RpcResult.paramError("[encryptedPriKey] is inValid");
        }
        io.nuls.core.basic.Result result = NulsSDKTool.sign(txHex, address, encryptedPriKey, password);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("createMultiSignAccount")
    @ApiOperation(description = "Create a multi signature account", order = 157, detailDesc = "Create multiple signed accounts based on the public keys of multiple accounts,minSignsThe minimum number of signatures required to create transactions for multi signature accounts")
    @Parameters(value = {
            @Parameter(parameterName = "pubKeys", requestType = @TypeDescriptor(value = List.class, collectionElement = String.class), parameterDes = "Account public key collection"),
            @Parameter(parameterName = "minSigns", requestType = @TypeDescriptor(value = int.class), parameterDes = "Minimum number of signatures")
    })
    @ResponseData(name = "Return value", description = "Return aMap", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "The address of the account")
    }))
    public RpcResult createMultiSignAccount(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int minSigns;
        List<String> pubKeys;

        try {
            pubKeys = (List<String>) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[pubKeys] is inValid");
        }
        try {
            minSigns = (int) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[minSigns] is inValid");
        }
        if (pubKeys.isEmpty()) {
            return RpcResult.paramError("[pubKeys] is empty");
        }
        if (minSigns < 1 || minSigns > pubKeys.size()) {
            return RpcResult.paramError("[minSigns] is inValid");
        }

        io.nuls.core.basic.Result result = NulsSDKTool.createMultiSignAccount(pubKeys, minSigns);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("createAliasTx")
    @ApiOperation(description = "Offline creation and setting of alias transactions", order = 158)
    @Parameters({
            @Parameter(parameterName = "Create an alias transaction", parameterDes = "Create an alias transaction form", requestType = @TypeDescriptor(value = AliasDto.class))
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "hash", description = "transactionhash"),
            @Key(name = "txHex", description = "Transaction serialization16Hexadecimal Strings")
    }))
    public RpcResult createAliasTx(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        String address, alias, nonce;
        try {
            address = (String) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        try {
            alias = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[alias] is inValid");
        }
        try {
            nonce = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[nonce] is inValid");
        }

        AliasDto dto = new AliasDto();
        dto.setAddress(address);
        dto.setAlias(alias);
        dto.setNonce(nonce);
        io.nuls.core.basic.Result result = NulsSDKTool.createAliasTxOffline(dto);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("createMultiSignAliasTx")
    @ApiOperation(description = "Offline creation and setting of alias transactions for multiple signed accounts", order = 159)
    @Parameters({
            @Parameter(parameterName = "Offline creation and setting of alias transactions for multiple signed accounts", parameterDes = "Create an alias transaction form", requestType = @TypeDescriptor(value = MultiSignAliasDto.class))
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "hash", description = "transactionhash"),
            @Key(name = "txHex", description = "Transaction serialization16Hexadecimal Strings")
    }))
    public RpcResult createMultiSignAliasTx(List<Object> params) {
        String address, alias, nonce, remark;
        List<String> pubKeys;
        int minSigns;
        try {
            address = (String) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        try {
            alias = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[alias] is inValid");
        }
        try {
            nonce = (String) params.get(2);
        } catch (Exception e) {
            return RpcResult.paramError("[nonce] is inValid");
        }
        try {
            remark = (String) params.get(3);
        } catch (Exception e) {
            return RpcResult.paramError("[remark] is inValid");
        }
        try {
            pubKeys = (List<String>) params.get(4);
        } catch (Exception e) {
            return RpcResult.paramError("[pubKeys] is inValid");
        }
        try {
            minSigns = (int) params.get(5);
        } catch (Exception e) {
            return RpcResult.paramError("[minSigns] is inValid");
        }
        MultiSignAliasDto dto = new MultiSignAliasDto();
        dto.setAddress(address);
        dto.setAlias(alias);
        dto.setNonce(nonce);
        dto.setPubKeys(pubKeys);
        dto.setMinSigns(minSigns);
        dto.setRemark(remark);
        io.nuls.core.basic.Result result = NulsSDKTool.createMultiSignAliasTxOffline(dto);
        return ResultUtil.getJsonRpcResult(result);
    }

    @RpcMethod("getAddressByPriKey")
    @ApiOperation(description = "Obtain account address format based on private key", order = 160)
    @Parameters({
            @Parameter(parameterName = "Original private key", parameterDes = "Private Key Form", requestType = @TypeDescriptor(value = PriKeyForm.class))
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "Account address")
    }))
    public RpcResult getAddressByPriKey(List<Object> params) {
        String priKey;
        try {
            priKey = (String) params.get(0);
            io.nuls.core.basic.Result result = NulsSDKTool.getAddressByPriKey(priKey);
            return ResultUtil.getJsonRpcResult(result);
        } catch (Exception e) {
            return RpcResult.paramError("[priKey] is inValid");
        }
    }

    @RpcMethod("isBlockAccount")
    @ApiOperation(description = "Is the account locked", order = 161)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "Is it locked"),
    }))
    public RpcResult isBlockAccount(List<Object> params) {
        int chainId;
        String address;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            address = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.paramError(String.format("chainId [%s] is invalid", chainId));
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }
        boolean blockAccount = accountTools.isBlockAccount(chainId, address);
        return RpcResult.success(Map.of("value", blockAccount));
    }

    @RpcMethod("getBlockAccountInfo")
    @ApiOperation(description = "Query locked account information", order = 162)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = AccountBlockDTO.class))
    public RpcResult getBlockAccountInfo(List<Object> params) {
        int chainId;
        String address;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            address = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.paramError(String.format("chainId [%s] is invalid", chainId));
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }
        AccountBlockDTO dto = accountTools.getBlockAccountInfo(chainId, address);
        if (dto == null) {
            return RpcResult.failed(AccountErrorCode.DATA_NOT_FOUND);
        }
        return RpcResult.success(dto);
    }

    @RpcMethod("getAccountWhitelistInfo")
    @ApiOperation(description = "getAccountWhitelistInfo", order = 163)
    @Parameters({
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainID"),
            @Parameter(parameterName = "address", parameterType = "String", parameterDes = "Account address"),
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = AccountWhitelistDTO.class))
    public RpcResult getAccountWhitelistInfo(List<Object> params) {
        int chainId;
        String address;
        try {
            chainId = (int) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            address = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[address] is inValid");
        }
        if (!Context.isChainExist(chainId)) {
            return RpcResult.paramError(String.format("chainId [%s] is invalid", chainId));
        }
        if (!AddressTool.validAddress(chainId, address)) {
            return RpcResult.paramError("[address] is inValid");
        }
        AccountWhitelistDTO dto = accountTools.getAccountWhitelistInfo(chainId, address);
        if (dto == null) {
            return RpcResult.failed(AccountErrorCode.DATA_NOT_FOUND);
        }
        return RpcResult.success(dto);
    }



}

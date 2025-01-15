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
import io.nuls.base.api.provider.block.BlockService;
import io.nuls.base.api.provider.block.facade.GetBlockHeaderByLastHeightReq;
import io.nuls.base.api.provider.crosschain.CrossChainProvider;
import io.nuls.base.api.provider.crosschain.facade.RehandleCtxReq;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinFrom;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Controller;
import io.nuls.core.core.annotation.RpcMethod;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.rpc.model.Parameter;
import io.nuls.core.rpc.model.Parameters;
import io.nuls.core.rpc.model.ResponseData;
import io.nuls.core.rpc.model.TypeDescriptor;
import io.nuls.provider.model.CrossTransferData;
import io.nuls.provider.model.jsonrpc.RpcErrorCode;
import io.nuls.provider.model.jsonrpc.RpcResult;
import io.nuls.provider.rpctools.TransactionTools;
import io.nuls.provider.utils.VerifyUtils;
import io.nuls.v2.model.annotation.Api;
import io.nuls.v2.model.annotation.ApiOperation;
import io.nuls.v2.model.annotation.ApiType;

import java.math.BigInteger;
import java.util.List;

/**
 * @author Niels
 */
@Controller
@Api(type = ApiType.JSONRPC)
public class CrossChainController {

    @Autowired
    private TransactionTools transactionTools;
    private BlockService blockService = ServiceManager.get(BlockService.class);

    private CrossChainProvider crossChainProvider = ServiceManager.get(CrossChainProvider.class);
    public CrossChainController(){
        System.out.println();
    }

    @RpcMethod("rehandlectx")
    @ApiOperation(description = "Resend transactions that have already been held by the cardholder", order = 901, detailDesc = "Resending only represents the attempt of this node and cannot guarantee the final result")
    @Parameters(value = {
            @Parameter(parameterName = "hash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Cross chain transactionshash")
    })
    @ResponseData(name = "Return value", description = "Whether successful", responseType = @TypeDescriptor(value = Boolean.class))
    public RpcResult createAccount(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        String hash;
        try {
            hash = (String) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[hash] is inValid");
        }

        long blockHeight = blockService.getBlockHeaderByLastHeight(new GetBlockHeaderByLastHeightReq()).getData().getHeight();
        Result<String> result = crossChainProvider.rehandleCtx(new RehandleCtxReq(hash, blockHeight));
        if (result.isFailed()) {
            return RpcResult.failed(RpcErrorCode.SYS_UNKNOWN_EXCEPTION);
        }
        return RpcResult.success(true);
    }

    @RpcMethod("calcNulsHash")
    @ApiOperation(description = "calcNulsHash", order = 902, detailDesc = "calcNulsHash")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "hash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Cross chain tx hash")
    })
    @ResponseData(name = "Return value", description = "Return a tx hash", responseType = @TypeDescriptor(value = String.class))
    public RpcResult calcNulsHash(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int chainId;
        String txHash;
        try {
            chainId = (Integer) params.get(0);
        } catch (Exception e) {
            return RpcResult.paramError("[chainId] is inValid");
        }
        try {
            txHash = (String) params.get(1);
        } catch (Exception e) {
            return RpcResult.paramError("[hash] is inValid");
        }
        Result<String> result = transactionTools.getTxSerialize(chainId, txHash);
        if (result.isFailed()) {
            return RpcResult.dataNotFound();
        }
        Transaction tx = this.getCrossTxData(chainId, result.getData());
        return RpcResult.success(tx.getHash().toHex());
    }

    private Transaction getCrossTxData(int chainId, String txHex) {
        try {
            Transaction tx = new Transaction();
            tx.parse(HexUtil.decode(txHex), 0);

            if (tx.getType() != 10) {
                throw new RuntimeException("its not a cross chain tx.");
            }

            Transaction newTx = new Transaction();
            newTx.setType(tx.getType());
            newTx.setTime(tx.getTime());
            newTx.setRemark(tx.getRemark());
            CrossTransferData txData = new CrossTransferData();
            txData.setSourceType(10);
            txData.setSourceHash(tx.getHash().getBytes());
            byte[] txDataBytes = txData.serialize();
            newTx.setTxData(txDataBytes);

            // Assembling CoinData for cross-chain transactions on the NULS network
            CoinData _coinData = tx.getCoinDataInstance();
            List<CoinTo> tos = _coinData.getTo();
            List<CoinFrom> froms = _coinData.getFrom();
            CoinData coinData = new CoinData();
            boolean isTransferNVT = false;
            BigInteger transferAmount = BigInteger.ZERO;
            for (CoinTo to : tos) {
                int assetsChainId = to.getAssetsChainId();
                int assetsId = to.getAssetsId();
                if (assetsChainId == chainId && assetsId == 1) {
                    isTransferNVT = true;
                }
                transferAmount = to.getAmount();
                CoinTo _to = new CoinTo();
                _to.setAddress(to.getAddress());
                _to.setAssetsChainId(assetsChainId);
                _to.setAssetsId(assetsId);
                _to.setAmount(transferAmount);
                _to.setLockTime(to.getLockTime());
                coinData.getTo().add(_to);
            }
            for (CoinFrom from : froms) {
                int assetsChainId = from.getAssetsChainId();
                int assetsId = from.getAssetsId();
                BigInteger amount = from.getAmount();
                // Excluding NVT fees of NERVE chain
                if (assetsChainId == chainId && assetsId == 1) {
                    if (!isTransferNVT) {
                        continue;
                    } else {
                        amount = transferAmount;
                    }
                }
                CoinFrom _from = new CoinFrom();
                _from.setAddress(from.getAddress());
                _from.setAssetsChainId(assetsChainId);
                _from.setAssetsId(assetsId);
                _from.setAmount(amount);
                _from.setNonce(from.getNonce());
                _from.setLocked(from.getLocked());
                coinData.getFrom().add(_from);
            }
            newTx.setCoinData(coinData.serialize());

            return newTx;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

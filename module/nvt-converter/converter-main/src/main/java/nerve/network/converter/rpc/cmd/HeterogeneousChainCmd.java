/*-
 * ⁣⁣
 * MIT License
 * ⁣⁣
 * Copyright (C) 2017 - 2018 nuls.io
 * ⁣⁣
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ⁣⁣
 */
package nerve.network.converter.rpc.cmd;

import io.nuls.base.RPCUtil;
import io.nuls.base.data.Transaction;
import nerve.network.converter.constant.ConverterCmdConstant;
import nerve.network.converter.constant.ConverterConstant;
import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.core.business.AssembleTxService;
import nerve.network.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import nerve.network.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import nerve.network.converter.manager.ChainManager;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.bo.HeterogeneousAssetInfo;
import nerve.network.converter.model.dto.HeterogeneousAssetCollectionDTO;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.model.ObjectUtils;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nerve.network.converter.utils.LoggerUtil.LOG;

/**
 * 异构链信息提供命令
 *
 * @author: Chino
 * @date: 2020-02-28
 */
@Component
public class HeterogeneousChainCmd extends BaseCmd {

    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private AssembleTxService assembleTxService;
    @Autowired
    private ChainManager chainManager;

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_HETEROGENEOUS_CHAIN_ASSET_INFO, version = 1.0, description = "异构链资产信息查询")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产链ID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产ID")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "contractAddress", description = "资产对应合约地址(若有)"),
            @Key(name = "symbol", description = "资产symbol"),
            @Key(name = "decimals", valueType = int.class, description = "资产小数位数")
    })
    )
    public Response getAssetInfoById(Map params) {
        Map<String, Object> rtMap = new HashMap<>(2);
        try {
            Integer chainId = Integer.parseInt(params.get("chainId").toString());
            Integer assetId = Integer.parseInt(params.get("assetId").toString());
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(chainId);
            if (docking == null) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            HeterogeneousAssetInfo assetInfo = docking.getAssetByAssetId(assetId);
            if (assetInfo == null) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid assetId");
            }
            rtMap.put("contractAddress", assetInfo.getContractAddress());
            rtMap.put("symbol", assetInfo.getSymbol());
            rtMap.put("decimals", assetInfo.getDecimals());
        } catch (Exception e) {
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_ALL_HETEROGENEOUS_CHAIN_ASSET_LIST, version = 1.0, description = "所有注册的异构链的所有初始资产")
    @ResponseData(name = "返回值", description = "返回一个集合", responseType = @TypeDescriptor(
            value = List.class, collectionElement = HeterogeneousAssetCollectionDTO.class)
    )
    public Response getAllHeterogeneousChainAssetList(Map params) {
        Map<String, Object> rtMap = new HashMap<>(2);
        try {
            List<HeterogeneousAssetCollectionDTO> result = new ArrayList<>();
            heterogeneousDockingManager.getAllHeterogeneousDocking().stream().forEach(docking -> {
                List<HeterogeneousAssetInfo> assetList = docking.getAllInitializedAssets();
                result.add(new HeterogeneousAssetCollectionDTO(docking.getChainId(), assetList));
            });
            rtMap.put("list", result);
        } catch (Exception e) {
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.CREATE_HETEROGENEOUS_CONTRACT_ASSET_REG_TX, version = 1.0, description = "创建异构链合约资产注册交易")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链chainId"),
            @Parameter(parameterName = "decimals", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产小数位数"),
            @Parameter(parameterName = "symbol", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产符号"),
            @Parameter(parameterName = "contractAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产对应合约地址"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "交易备注", canNull = true),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "支付/签名地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "密码")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "交易hash")
    })
    )
    public Response createHeterogeneousContractAssetRegTx(Map params) {
        Chain chain = null;
        try {
            // check parameters
            if (params == null) {
                LOG.warn("ac_transfer params is null");
                throw new NulsRuntimeException(ConverterErrorCode.NULL_PARAMETER);
            }
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("heterogeneousChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("decimals"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("symbol"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("contractAddress"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("address"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("password"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            // parse params
            Integer chainId = (Integer) params.get("chainId");
            Integer heterogeneousChainId = (Integer) params.get("heterogeneousChainId");
            Integer decimals = (Integer) params.get("decimals");
            String symbol = (String) params.get("symbol");
            String contractAddress = (String) params.get("contractAddress");
            String remark = (String) params.get("remark");
            String address = (String) params.get("address");
            String password = (String) params.get("password");


            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            Transaction tx = assembleTxService.createHeterogeneousContractAssetRegTx(chain, address, password,
                    heterogeneousChainId,
                    decimals,
                    symbol,
                    contractAddress,
                    remark);
            Map<String, String> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("value", tx.getHash().toHex());
            map.put("hex", RPCUtil.encode(tx.serialize()));
            return success(map);
        } catch (NulsRuntimeException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode(), e.getMessage());
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
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

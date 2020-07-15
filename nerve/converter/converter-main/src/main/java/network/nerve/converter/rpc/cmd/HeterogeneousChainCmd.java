/*-
 * ⁣⁣
 * MIT License
 * ⁣⁣
 * Copyright (C) 2019-2020 nerve.network
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
package network.nerve.converter.rpc.cmd;

import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.model.ObjectUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.constant.ConverterCmdConstant;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.helper.LedgerAssetRegisterHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.AgentBasic;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.LatestBasicBlock;
import network.nerve.converter.rpc.call.ConsensusCall;
import network.nerve.converter.utils.LoggerUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 异构链信息提供命令
 *
 * @author: Mimi
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
    @Autowired
    private LedgerAssetRegisterHelper ledgerAssetRegisterHelper;
    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private ConverterCoreApi converterCoreApi;

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_HETEROGENEOUS_CHAIN_ASSET_INFO, version = 1.0, description = "异构链资产信息查询")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产链ID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产ID")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "symbol", description = "资产symbol"),
            @Key(name = "decimals", valueType = int.class, description = "资产小数位数"),
            @Key(name = "contractAddress", description = "资产对应合约地址(若有)"),
            @Key(name = "isToken", description = "资产是否为TOKEN资产"),
            @Key(name = "heterogeneousChainId", valueType = int.class, description = "异构链ID"),
            @Key(name = "heterogeneousChainSymbol", description = "异构链Symbol"),
            @Key(name = "heterogeneousChainMultySignAddress", description = "异构链多签地址")
    })
    )
    public Response getAssetInfoById(Map params) {
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
        try {
            Integer chainId = Integer.parseInt(params.get("chainId").toString());
            if (chainId != converterConfig.getChainId()) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chain ID");
            }
            Integer assetId = Integer.parseInt(params.get("assetId").toString());
            HeterogeneousAssetInfo assetInfo = ledgerAssetRegisterHelper.getHeterogeneousAssetInfo(assetId);
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(assetInfo.getChainId());
            if (docking == null) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid heterogeneous asset");
            }
            rtMap.put("symbol", assetInfo.getSymbol());
            rtMap.put("decimals", assetInfo.getDecimals());
            rtMap.put("contractAddress", assetInfo.getContractAddress());
            rtMap.put("isToken", StringUtils.isNotBlank(assetInfo.getContractAddress()));
            rtMap.put("heterogeneousChainId", docking.getChainId());
            rtMap.put("heterogeneousChainSymbol", docking.getChainSymbol());
            rtMap.put("heterogeneousChainMultySignAddress", docking.getCurrentMultySignAddress());
        } catch (Exception e) {
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_HETEROGENEOUS_CHAIN_ASSET_INFO_BY_ADDRESS, version = 1.0, description = "异构链资产信息查询")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链ID"),
            @Parameter(parameterName = "contractAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产对应合约地址")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "chainId", valueType = int.class, description = "资产链ID"),
            @Key(name = "assetId", valueType = int.class, description = "资产ID"),
            @Key(name = "symbol", description = "资产symbol"),
            @Key(name = "decimals", valueType = int.class, description = "资产小数位数")
    })
    )
    public Response getAssetInfoByAddress(Map params) {
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
        try {
            Integer heterogeneousChainId = Integer.parseInt(params.get("heterogeneousChainId").toString());
            String contractAddress = params.get("contractAddress").toString().toLowerCase();
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            if (docking == null) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            HeterogeneousAssetInfo assetInfo = docking.getAssetByContractAddress(contractAddress);
            if (assetInfo == null) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid contractAddress");
            }
            int nerveAssetId = ledgerAssetRegisterHelper.getNerveAssetId(assetInfo.getChainId(), assetInfo.getAssetId());
            rtMap.put("chainId", converterConfig.getChainId());
            rtMap.put("assetId", nerveAssetId);
            rtMap.put("symbol", assetInfo.getSymbol());
            rtMap.put("decimals", assetInfo.getDecimals());
        } catch (Exception e) {
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.VALIDATE_HETEROGENEOUS_CONTRACT_ASSET_REG_TX, version = 1.0, description = "验证异构链合约资产注册交易")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链chainId"),
            @Parameter(parameterName = "decimals", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产小数位数"),
            @Parameter(parameterName = "symbol", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产符号"),
            @Parameter(parameterName = "contractAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产对应合约地址")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "true/false")
    })
    )
    public Response validateHeterogeneousContractAssetRegPendingTx(Map params) {
        Chain chain = null;
        try {
            if (!converterCoreApi.isVirtualBankByCurrentNode()) {
                throw new NulsRuntimeException(ConverterErrorCode.AGENT_IS_NOT_VIRTUAL_BANK);
            }
            // check parameters
            if (params == null) {
                LoggerUtil.LOG.warn("params is null");
                throw new NulsRuntimeException(ConverterErrorCode.NULL_PARAMETER);
            }
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("heterogeneousChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("decimals"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("symbol"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("contractAddress"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            // parse params
            Integer chainId = (Integer) params.get("chainId");
            Integer heterogeneousChainId = (Integer) params.get("heterogeneousChainId");
            Integer decimals = (Integer) params.get("decimals");
            String symbol = (String) params.get("symbol");
            String contractAddress = (String) params.get("contractAddress");
            contractAddress = contractAddress.trim().toLowerCase();

            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            if (docking == null) {
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
            }
            HeterogeneousAssetInfo assetInfo = docking.getAssetByContractAddress(contractAddress);
            // 资产已存在
            if (assetInfo != null) {
                LoggerUtil.LOG.error("资产已存在");
                throw new NulsException(ConverterErrorCode.ASSET_EXIST);
            }
            // 资产信息验证
            if (!docking.validateHeterogeneousAssetInfoFromNet(contractAddress, symbol, decimals)) {
                LoggerUtil.LOG.error("资产信息不匹配");
                throw new NulsException(ConverterErrorCode.REG_ASSET_INFO_INCONSISTENCY);
            }

            Map<String, Boolean> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("value", true);
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
    public Response createHeterogeneousContractAssetRegPendingTx(Map params) {
        Chain chain = null;
        try {
            if (!converterCoreApi.isVirtualBankByCurrentNode()) {
                throw new NulsRuntimeException(ConverterErrorCode.AGENT_IS_NOT_VIRTUAL_BANK);
            }
            // check parameters
            if (params == null) {
                LoggerUtil.LOG.warn("params is null");
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
            contractAddress = contractAddress.trim().toLowerCase();


            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }

            Transaction tx = assembleTxService.createHeterogeneousContractAssetRegPendingTx(chain, address, password,
                    heterogeneousChainId,
                    decimals,
                    symbol,
                    contractAddress,
                    remark);
            Map<String, String> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("value", tx.getHash().toHex());
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


    @CmdAnnotation(cmd = ConverterCmdConstant.GET_HETEROGENEOUS_ADDRESS, version = 1.0, description = "查询共识节点打包地址对应的异构链地址")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链ID"),
            @Parameter(parameterName = "packingAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "共识节点打包地址")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "heterogeneousAddress", description = "异构链地址"),
    })
    )
    public Response getHeterogeneousAddress(Map params) {
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
        try {
            Integer chainId = Integer.parseInt(params.get("chainId").toString());
            Integer heterogeneousChainId = Integer.parseInt(params.get("heterogeneousChainId").toString());
            String packingAddress = params.get("packingAddress").toString();
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            if (docking == null) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            Chain chain = chainManager.getChain(chainId);
            LatestBasicBlock latestBasicBlock = chain.getLatestBasicBlock();
            // 获取最新共识列表
            List<AgentBasic> listAgent = ConsensusCall.getAgentList(chain, latestBasicBlock.getHeight());
            if (null == listAgent) {
                chain.getLogger().error("向共识模块获取共识节点列表数据为null");
                return failed(ConverterErrorCode.DATA_ERROR, "empty agent list");
            }
            String pubKey = null;
            for (AgentBasic agentBasic : listAgent) {
                if (agentBasic.getPackingAddress().equals(packingAddress)) {
                    pubKey = agentBasic.getPubKey();
                    break;
                }
            }
            if (StringUtils.isBlank(pubKey)) {
                return failed(ConverterErrorCode.DATA_NOT_FOUND);
            }
            rtMap.put("heterogeneousAddress", docking.generateAddressByCompressedPublicKey(pubKey));
        } catch (Exception e) {
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    private void errorLogProcess(Chain chain, Exception e) {
        if (chain == null) {
            LoggerUtil.LOG.error(e);
        } else {
            chain.getLogger().error(e);
        }
    }

}

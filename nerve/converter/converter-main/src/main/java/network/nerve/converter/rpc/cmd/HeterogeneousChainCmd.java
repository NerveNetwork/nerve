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
import io.nuls.core.log.Log;
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
import network.nerve.converter.enums.BindHeterogeneousContractMode;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.helper.LedgerAssetRegisterHelper;
import network.nerve.converter.heterogeneouschain.cro.context.CroContext;
import network.nerve.converter.heterogeneouschain.lib.core.HtgWalletApi;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.rpc.call.ConsensusCall;
import network.nerve.converter.rpc.call.LedgerCall;
import network.nerve.converter.utils.LoggerUtil;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.http.HttpService;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.protostuff.ByteString.EMPTY_STRING;
import static network.nerve.converter.utils.ConverterUtil.addressToLowerCase;

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
    private HeterogeneousAssetHelper heterogeneousAssetHelper;
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
            Integer assetId = Integer.parseInt(params.get("assetId").toString());
            List<HeterogeneousAssetInfo> assetInfos = heterogeneousAssetHelper.getHeterogeneousAssetInfo(chainId, assetId);
            if (assetInfos == null || assetInfos.isEmpty()) {
                return failed(ConverterErrorCode.DATA_NOT_FOUND);
            }
            HeterogeneousAssetInfo assetInfo = assetInfos.get(0);
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

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_HETEROGENEOUS_CHAIN_ASSET_INFO_LIST, version = 1.0, description = "异构链资产信息列表查询")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产链ID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产ID")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = List.class, collectionElement = Map.class, mapKeys = {
            @Key(name = "symbol", description = "资产symbol"),
            @Key(name = "decimals", valueType = int.class, description = "资产小数位数"),
            @Key(name = "contractAddress", description = "资产对应合约地址(若有)"),
            @Key(name = "isToken", description = "资产是否为TOKEN资产"),
            @Key(name = "heterogeneousChainId", valueType = int.class, description = "异构链ID"),
            @Key(name = "heterogeneousChainSymbol", description = "异构链Symbol"),
            @Key(name = "heterogeneousChainMultySignAddress", description = "异构链多签地址")
    })
    )
    public Response getAssetInfoListById(Map params) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        try {
            Integer chainId = Integer.parseInt(params.get("chainId").toString());
            Integer assetId = Integer.parseInt(params.get("assetId").toString());
            List<HeterogeneousAssetInfo> assetInfos = heterogeneousAssetHelper.getHeterogeneousAssetInfo(chainId, assetId);
            if (assetInfos == null || assetInfos.isEmpty()) {
                return failed(ConverterErrorCode.DATA_NOT_FOUND);
            }
            for (HeterogeneousAssetInfo assetInfo : assetInfos) {
                Map<String, Object> rtMap = new HashMap<>(16);
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
                resultList.add(rtMap);
            }
        } catch (Exception e) {
            return failed(e.getMessage());
        }
        return success(resultList);
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
            String contractAddress = addressToLowerCase(params.get("contractAddress").toString());
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            if (docking == null) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            HeterogeneousAssetInfo assetInfo = docking.getAssetByContractAddress(contractAddress);
            if (assetInfo == null) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid contractAddress");
            }
            NerveAssetInfo nerveAssetInfo = ledgerAssetRegisterHelper.getNerveAssetInfo(assetInfo.getChainId(), assetInfo.getAssetId());
            rtMap.put("chainId", nerveAssetInfo.getAssetChainId());
            rtMap.put("assetId", nerveAssetInfo.getAssetId());
            rtMap.put("symbol", assetInfo.getSymbol());
            rtMap.put("decimals", assetInfo.getDecimals());
        } catch (Exception e) {
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_HETEROGENEOUS_CHAIN_ASSET_INFO_BY_ID, version = 1.0, description = "异构链资产信息查询")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链ID"),
            @Parameter(parameterName = "heterogeneousAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链资产ID")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "chainId", valueType = int.class, description = "资产链ID"),
            @Key(name = "assetId", valueType = int.class, description = "资产ID"),
            @Key(name = "symbol", description = "资产symbol"),
            @Key(name = "decimals", valueType = int.class, description = "资产小数位数")
    })
    )
    public Response getAssetInfoByHeterogeneousAssetId(Map params) {
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
        try {
            Integer heterogeneousChainId = Integer.parseInt(params.get("heterogeneousChainId").toString());
            Integer heterogeneousAssetId = Integer.parseInt(params.get("heterogeneousAssetId").toString());
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            if (docking == null) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            HeterogeneousAssetInfo assetInfo = docking.getAssetByAssetId(heterogeneousAssetId);
            NerveAssetInfo nerveAssetInfo = ledgerAssetRegisterHelper.getNerveAssetInfo(assetInfo.getChainId(), assetInfo.getAssetId());
            rtMap.put("chainId", nerveAssetInfo.getAssetChainId());
            rtMap.put("assetId", nerveAssetInfo.getAssetId());
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
            contractAddress = addressToLowerCase(contractAddress.trim());

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
            if (!converterCoreApi.isSeedVirtualBankByCurrentNode()) {
                throw new NulsRuntimeException(ConverterErrorCode.AGENT_IS_NOT_SEED_VIRTUAL_BANK);
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
            contractAddress = addressToLowerCase(contractAddress.trim());


            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isVirtualBankBySignAddr(address)) {
                throw new NulsRuntimeException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
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

    @CmdAnnotation(cmd = ConverterCmdConstant.VALIDATE_BIND_HETEROGENEOUS_CONTRACT_TOKEN_TO_NERVE_ASSET_REG_TX, version = 1.0, description = "验证Nerve资产绑定异构链合约资产")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链chainId"),
            @Parameter(parameterName = "decimals", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产小数位数"),
            @Parameter(parameterName = "symbol", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产符号"),
            @Parameter(parameterName = "contractAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产对应合约地址"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Nerve资产链ID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Nerve资产ID")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "true/false")
    })
    )
    public Response validateBindHeterogeneousContractTokenToNerveAssetRegPendingTx(Map params) {
        Chain chain = null;
        try {
            if (!converterCoreApi.isVirtualBankByCurrentNode()) {
                throw new NulsRuntimeException(ConverterErrorCode.AGENT_IS_NOT_VIRTUAL_BANK);
            }

            ObjectUtils.canNotEmpty(params.get("heterogeneousChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("nerveAssetChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("nerveAssetId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            // parse params
            Integer chainId = (Integer) params.get("chainId");
            Integer heterogeneousChainId = (Integer) params.get("heterogeneousChainId");
            Integer nerveAssetChainId = (Integer) params.get("nerveAssetChainId");
            Integer nerveAssetId = (Integer) params.get("nerveAssetId");
            Integer decimals = (Integer) params.get("decimals");
            Map<String, Object> nerveAsset = LedgerCall.getNerveAsset(chainId, nerveAssetChainId, nerveAssetId);
            boolean existNerveAsset = nerveAsset != null;
            if (!existNerveAsset) {
                throw new NulsRuntimeException(ConverterErrorCode.ASSET_ID_NOT_EXIST);
            }
            int nerveAssetDecimals = 0;
            if (nerveAsset.get("decimalPlace") != null) {
                nerveAssetDecimals = Integer.parseInt(nerveAsset.get("decimalPlace").toString());
            }
            if (nerveAssetDecimals != decimals) {
                throw new NulsRuntimeException(ConverterErrorCode.REG_ASSET_INFO_INCONSISTENCY);
            }
            HeterogeneousAssetInfo assetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(heterogeneousChainId, nerveAssetChainId, nerveAssetId);
            if (assetInfo != null && assetInfo.getChainId() == heterogeneousChainId) {
                throw new NulsRuntimeException(ConverterErrorCode.DUPLICATE_BIND);
            }

            Response response = this.validateHeterogeneousContractAssetRegPendingTx(params);
            if (!response.isSuccess()) {
                return response;
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

    @CmdAnnotation(cmd = ConverterCmdConstant.BIND_HETEROGENEOUS_CONTRACT_TOKEN_TO_NERVE_ASSET_REG_TX, version = 1.0, description = "Nerve资产绑定异构链合约资产[新绑定]")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链chainId"),
            @Parameter(parameterName = "decimals", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产小数位数"),
            @Parameter(parameterName = "symbol", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产符号"),
            @Parameter(parameterName = "contractAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产对应合约地址"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Nerve资产链ID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Nerve资产ID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "支付/签名地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "密码")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "交易hash")
    })
    )
    public Response bindHeterogeneousContractTokenToNerveAssetRegPendingTx(Map params) {
        Chain chain = null;
        try {
            if (!converterCoreApi.isSeedVirtualBankByCurrentNode()) {
                throw new NulsRuntimeException(ConverterErrorCode.AGENT_IS_NOT_SEED_VIRTUAL_BANK);
            }
            // check parameters
            if (params == null) {
                LoggerUtil.LOG.warn("params is null");
                throw new NulsRuntimeException(ConverterErrorCode.NULL_PARAMETER);
            }
            Response response = this.validateBindHeterogeneousContractTokenToNerveAssetRegPendingTx(params);
            if (!response.isSuccess()) {
                return response;
            }
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("heterogeneousChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("decimals"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("symbol"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("contractAddress"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("nerveAssetChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("nerveAssetId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("address"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("password"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            // parse params
            Integer chainId = (Integer) params.get("chainId");
            Integer heterogeneousChainId = (Integer) params.get("heterogeneousChainId");
            Integer decimals = (Integer) params.get("decimals");
            Integer nerveAssetChainId = (Integer) params.get("nerveAssetChainId");
            Integer nerveAssetId = (Integer) params.get("nerveAssetId");
            String symbol = (String) params.get("symbol");
            String contractAddress = (String) params.get("contractAddress");
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            contractAddress = addressToLowerCase(contractAddress.trim());


            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isVirtualBankBySignAddr(address)) {
                throw new NulsRuntimeException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
            }

            Transaction tx = assembleTxService.createHeterogeneousContractAssetRegPendingTx(chain, address, password,
                    heterogeneousChainId,
                    decimals,
                    symbol,
                    contractAddress,
                    String.valueOf(String.format("%s:%s-%s", BindHeterogeneousContractMode.NEW, nerveAssetChainId, nerveAssetId)));
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

    @CmdAnnotation(cmd = ConverterCmdConstant.OVERRIDE_BIND_HETEROGENEOUS_CONTRACT_TOKEN_TO_NERVE_ASSET_REG_TX, version = 1.0, description = "Nerve资产绑定异构链合约资产[覆盖绑定]")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链chainId"),
            @Parameter(parameterName = "contractAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "资产对应合约地址"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Nerve资产链ID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Nerve资产ID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "支付/签名地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "密码")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "交易hash")
    })
    )
    public Response overrideBindHeterogeneousContractTokenToNerveAssetRegPendingTx(Map params) {
        Chain chain = null;
        try {
            if (!converterCoreApi.isSeedVirtualBankByCurrentNode()) {
                throw new NulsRuntimeException(ConverterErrorCode.AGENT_IS_NOT_SEED_VIRTUAL_BANK);
            }
            // check parameters
            if (params == null) {
                LoggerUtil.LOG.warn("params is null");
                throw new NulsRuntimeException(ConverterErrorCode.NULL_PARAMETER);
            }
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("heterogeneousChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("contractAddress"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("nerveAssetChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("nerveAssetId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("address"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("password"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            // parse params
            Integer chainId = (Integer) params.get("chainId");
            Integer heterogeneousChainId = (Integer) params.get("heterogeneousChainId");
            Integer nerveAssetChainId = (Integer) params.get("nerveAssetChainId");
            Integer nerveAssetId = (Integer) params.get("nerveAssetId");
            String contractAddress = (String) params.get("contractAddress");
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            contractAddress = addressToLowerCase(contractAddress.trim());

            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isVirtualBankBySignAddr(address)) {
                throw new NulsRuntimeException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
            }
            Map<String, Object> nerveAsset = LedgerCall.getNerveAsset(chainId, nerveAssetChainId, nerveAssetId);
            boolean existNerveAsset = nerveAsset != null;
            if (!existNerveAsset) {
                throw new NulsRuntimeException(ConverterErrorCode.ASSET_ID_NOT_EXIST);
            }
            int nerveAssetDecimals = 0;
            if (nerveAsset.get("decimalPlace") != null) {
                nerveAssetDecimals = Integer.parseInt(nerveAsset.get("decimalPlace").toString());
            }
            HeterogeneousAssetInfo assetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(heterogeneousChainId, nerveAssetChainId, nerveAssetId);
            if (assetInfo != null) {
                throw new NulsRuntimeException(ConverterErrorCode.DUPLICATE_BIND);
            }
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            HeterogeneousAssetInfo hAssetInfo = docking.getAssetByContractAddress(contractAddress);
            if (hAssetInfo == null) {
                throw new NulsRuntimeException(ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND);
            }
            if (nerveAssetDecimals != hAssetInfo.getDecimals()) {
                throw new NulsRuntimeException(ConverterErrorCode.REG_ASSET_INFO_INCONSISTENCY);
            }
            NerveAssetInfo overrideNerveAssetInfo = ledgerAssetRegisterHelper.getNerveAssetInfo(hAssetInfo.getChainId(), hAssetInfo.getAssetId());
            if (overrideNerveAssetInfo == null) {
                throw new NulsRuntimeException(ConverterErrorCode.OVERRIDE_BIND_ASSET_NOT_FOUND);
            }
            Transaction tx = assembleTxService.createHeterogeneousContractAssetRegPendingTx(chain, address, password,
                    heterogeneousChainId,
                    0,
                    EMPTY_STRING,
                    contractAddress,
                    String.valueOf(String.format("%s:%s-%s:%s-%s", BindHeterogeneousContractMode.OVERRIDE, nerveAssetChainId, nerveAssetId, overrideNerveAssetInfo.getAssetChainId(), overrideNerveAssetInfo.getAssetId())));
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

    @CmdAnnotation(cmd = ConverterCmdConstant.UNBIND_HETEROGENEOUS_CONTRACT_TOKEN_TO_NERVE_ASSET_REG_TX, version = 1.0, description = "Nerve资产取消绑定异构链合约资产[取消绑定]")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链chainId"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Nerve资产链ID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Nerve资产ID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "支付/签名地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "密码")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "交易hash")
    })
    )
    public Response unbindHeterogeneousContractTokenToNerveAssetRegPendingTx(Map params) {
        Chain chain = null;
        try {
            if (!converterCoreApi.isSeedVirtualBankByCurrentNode()) {
                throw new NulsRuntimeException(ConverterErrorCode.AGENT_IS_NOT_SEED_VIRTUAL_BANK);
            }
            // check parameters
            if (params == null) {
                LoggerUtil.LOG.warn("params is null");
                throw new NulsRuntimeException(ConverterErrorCode.NULL_PARAMETER);
            }
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("heterogeneousChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("nerveAssetChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("nerveAssetId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("address"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("password"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            // parse params
            Integer chainId = (Integer) params.get("chainId");
            Integer heterogeneousChainId = (Integer) params.get("heterogeneousChainId");
            Integer nerveAssetChainId = (Integer) params.get("nerveAssetChainId");
            Integer nerveAssetId = (Integer) params.get("nerveAssetId");
            String address = (String) params.get("address");
            String password = (String) params.get("password");

            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isVirtualBankBySignAddr(address)) {
                throw new NulsRuntimeException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
            }
            Map<String, Object> nerveAsset = LedgerCall.getNerveAsset(chainId, nerveAssetChainId, nerveAssetId);
            boolean existNerveAsset = nerveAsset != null;
            if (!existNerveAsset) {
                throw new NulsRuntimeException(ConverterErrorCode.ASSET_ID_NOT_EXIST);
            }
            HeterogeneousAssetInfo assetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(heterogeneousChainId, nerveAssetChainId, nerveAssetId);
            if (assetInfo == null) {
                throw new NulsRuntimeException(ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND);
            }
            Integer assetType = Integer.parseInt(nerveAsset.get("assetType").toString());
            if (assetType <= 4) {
                throw new NulsRuntimeException(ConverterErrorCode.NOT_BIND_ASSET);
            }
            // 非Nerve链ID的资产，一定是异构链的绑定资产
            if (nerveAssetChainId == chainId) {
                Map checkMap = new HashMap();
                checkMap.put("chainId", nerveAssetChainId);
                checkMap.put("assetId", nerveAssetId);
                Response registerNetwork = this.getRegisterNetwork(checkMap);
                if (!registerNetwork.isSuccess()) {
                    throw new Exception(registerNetwork.getResponseComment());
                }
                Map checkResult = (Map) registerNetwork.getResponseData();
                int registerHeterogeneousChainId = Integer.parseInt(checkResult.get("heterogeneousChainId").toString());
                if (registerHeterogeneousChainId == heterogeneousChainId) {
                    throw new NulsRuntimeException(ConverterErrorCode.NOT_BIND_ASSET);
                }
            }

            Transaction tx = assembleTxService.createHeterogeneousContractAssetRegPendingTx(chain, address, password,
                    heterogeneousChainId,
                    assetInfo.getDecimals(),
                    assetInfo.getSymbol(),
                    assetInfo.getContractAddress(),
                    String.valueOf(String.format("%s:%s-%s", BindHeterogeneousContractMode.REMOVE, nerveAssetChainId, nerveAssetId)));
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

    @CmdAnnotation(cmd = ConverterCmdConstant.CREATE_HETEROGENEOUS_MAIN_ASSET_REG_TX, version = 1.0, description = "创建异构链主资产注册交易")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链chainId"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "交易备注", canNull = true),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "支付/签名地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "密码")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "交易hash")
    })
    )
    public Response createHeterogeneousMainAssetRegTx(Map params) {
        Chain chain = null;
        try {
            if (!converterCoreApi.isSeedVirtualBankByCurrentNode()) {
                throw new NulsRuntimeException(ConverterErrorCode.AGENT_IS_NOT_SEED_VIRTUAL_BANK);
            }
            // check parameters
            if (params == null) {
                LoggerUtil.LOG.warn("params is null");
                throw new NulsRuntimeException(ConverterErrorCode.NULL_PARAMETER);
            }
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("heterogeneousChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("address"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("password"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            // parse params
            Integer chainId = (Integer) params.get("chainId");
            Integer heterogeneousChainId = (Integer) params.get("heterogeneousChainId");
            String remark = (String) params.get("remark");
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isVirtualBankBySignAddr(address)) {
                throw new NulsRuntimeException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
            }
            Transaction tx = assembleTxService.createHeterogeneousMainAssetRegTx(chain, address, password, heterogeneousChainId, remark);
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

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_HETEROGENEOUS_REGISTER_NETWORK, version = 1.0, description = "查询资产的异构链注册网络")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产链ID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "资产ID")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "heterogeneousChainId", valueType = int.class, description = "异构链ID")
    })
    )
    public Response getRegisterNetwork(Map params) {
        Chain chain = null;
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
        try {
            chain = chainManager.getChain(converterConfig.getChainId());
            Integer chainId = Integer.parseInt(params.get("chainId").toString());
            Integer assetId = Integer.parseInt(params.get("assetId").toString());
            List<HeterogeneousAssetInfo> assetInfos = heterogeneousAssetHelper.getHeterogeneousAssetInfo(chainId, assetId);
            if (assetInfos == null || assetInfos.isEmpty()) {
                return failed(ConverterErrorCode.DATA_NOT_FOUND);
            }
            int resultChainId = 0;
            for (HeterogeneousAssetInfo assetInfo : assetInfos) {
                if (StringUtils.isBlank(assetInfo.getContractAddress())) {
                    resultChainId = assetInfo.getChainId();
                    break;
                }
            }
            if (resultChainId == 0) {
                for (HeterogeneousAssetInfo assetInfo : assetInfos) {
                    IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(assetInfo.getChainId());
                    if (docking == null) {
                        return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid heterogeneous asset");
                    }
                    try {
                        if (!docking.isMinterERC20(assetInfo.getContractAddress())) {
                            resultChainId = docking.getChainId();
                            break;
                        }
                    } catch (Exception e) {
                        //skip it
                    }
                }
            }
            if (resultChainId == 0) {
                return failed(ConverterErrorCode.DATA_NOT_FOUND);
            }
            rtMap.put("heterogeneousChainId", resultChainId);
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_HETEROGENEOUS_NETWORK_CHAIN_ID, version = 1.0, description = "查询资产的异构链注册网络")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链chainId")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "heterogeneousNetworkChainId", valueType = long.class, description = "异构链网络内部chainId")
    })
    )
    public Response getHtgNetworkChainId(Map params) {
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
        try {
            Integer heterogeneousChainId = Integer.parseInt(params.get("heterogeneousChainId").toString());
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            if (docking == null) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid heterogeneousChainId");
            }
            long resultChainId = docking.getHeterogeneousNetworkChainId();
            rtMap.put("heterogeneousNetworkChainId", resultChainId);

        } catch (Exception e) {
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.UNREGISTER_HETEROGENEOUS_CONTRACT_TOKEN_TO_NERVE_ASSET_REG_TX, version = 1.0, description = "Nerve资产取消注册异构链合约资产[取消注册]")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链chainId"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Nerve资产链ID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Nerve资产ID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "支付/签名地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "密码")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "交易hash")
    })
    )
    public Response unRegisterHeterogeneousContractTokenToNerveAssetRegPendingTx(Map params) {
        Chain chain = null;
        try {
            if (!converterCoreApi.isSeedVirtualBankByCurrentNode()) {
                throw new NulsRuntimeException(ConverterErrorCode.AGENT_IS_NOT_SEED_VIRTUAL_BANK);
            }
            // check parameters
            if (params == null) {
                LoggerUtil.LOG.warn("params is null");
                throw new NulsRuntimeException(ConverterErrorCode.NULL_PARAMETER);
            }
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("heterogeneousChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("nerveAssetChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("nerveAssetId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("address"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("password"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            // parse params
            Integer chainId = (Integer) params.get("chainId");
            Integer heterogeneousChainId = (Integer) params.get("heterogeneousChainId");
            Integer nerveAssetChainId = (Integer) params.get("nerveAssetChainId");
            Integer nerveAssetId = (Integer) params.get("nerveAssetId");
            String address = (String) params.get("address");
            String password = (String) params.get("password");

            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isVirtualBankBySignAddr(address)) {
                throw new NulsRuntimeException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
            }
            Map<String, Object> nerveAsset = LedgerCall.getNerveAsset(chainId, nerveAssetChainId, nerveAssetId);
            boolean existNerveAsset = nerveAsset != null;
            if (!existNerveAsset) {
                throw new NulsRuntimeException(ConverterErrorCode.ASSET_ID_NOT_EXIST);
            }
            HeterogeneousAssetInfo assetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(heterogeneousChainId, nerveAssetChainId, nerveAssetId);
            if (assetInfo == null) {
                throw new NulsRuntimeException(ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND);
            }
            Integer assetType = Integer.parseInt(nerveAsset.get("assetType").toString());
            if (assetType != 4) {
                throw new NulsRuntimeException(ConverterErrorCode.HETEROGENEOUS_INFO_NOT_MATCH);
            }
            Map checkMap = new HashMap();
            checkMap.put("chainId", nerveAssetChainId);
            checkMap.put("assetId", nerveAssetId);
            Response registerNetwork = this.getRegisterNetwork(checkMap);
            if (!registerNetwork.isSuccess()) {
                throw new Exception(registerNetwork.getResponseComment());
            }
            Map checkResult = (Map) registerNetwork.getResponseData();
            int registerHeterogeneousChainId = Integer.parseInt(checkResult.get("heterogeneousChainId").toString());
            if (registerHeterogeneousChainId != heterogeneousChainId) {
                throw new NulsRuntimeException(ConverterErrorCode.HETEROGENEOUS_INFO_NOT_MATCH);
            }

            Transaction tx = assembleTxService.createHeterogeneousContractAssetRegPendingTx(chain, address, password,
                    heterogeneousChainId,
                    assetInfo.getDecimals(),
                    assetInfo.getSymbol(),
                    assetInfo.getContractAddress(),
                    String.valueOf(String.format("%s:%s-%s", BindHeterogeneousContractMode.UNREGISTER, nerveAssetChainId, nerveAssetId)));
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

    /*@CmdAnnotation(cmd = "cv_test", version = 1.0, description = "测试")
    @Parameters(value = {
            @Parameter(parameterName = "params", requestType = @TypeDescriptor(value = String.class), parameterDes = "测试参数")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class))
    public Response test(Map params) {
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
        try {
            String ethRpcAddress = "https://evm-cronos.crypto.org";
            HtgWalletApi htgWalletApi = new HtgWalletApi();
            Web3j web3j = Web3j.build(new HttpService(ethRpcAddress));
            htgWalletApi.setWeb3j(web3j);
            htgWalletApi.setEthRpcAddress(ethRpcAddress);
            CroContext htgContext = new CroContext();
            htgContext.setLogger(Log.BASIC_LOGGER);
            HeterogeneousCfg cfg = new HeterogeneousCfg();
            cfg.setChainIdOnHtgNetwork(25);
            htgContext.setConfig(cfg);
            Field field = htgWalletApi.getClass().getDeclaredField("htgContext");
            field.setAccessible(true);
            field.set(htgWalletApi, htgContext);

            Chain chain = chainManager.getChain(9);
            if (null == chain) {
                chain = chainManager.getChain(5);
                if (null == chain) {
                    throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
                }
            }

            String param = params.get("params").toString();
            EthBlock.Block block = htgWalletApi.getBlockByHeight(Long.parseLong(param));

            chain.getLogger().info("cronos block[{}] hash: {}, tx count: {}", param, block.getHash(), block.getTransactions().size());
            rtMap.put("blockHeight", param);
            rtMap.put("blockHash", block.getHash());
            rtMap.put("txCount", block.getTransactions().size());
        } catch (Exception e) {
            e.printStackTrace();
            return failed(e.getMessage());
        }
        return success(rtMap);
    }*/

    private void errorLogProcess(Chain chain, Exception e) {
        if (chain == null) {
            LoggerUtil.LOG.error(e);
        } else {
            chain.getLogger().error(e);
        }
    }

}

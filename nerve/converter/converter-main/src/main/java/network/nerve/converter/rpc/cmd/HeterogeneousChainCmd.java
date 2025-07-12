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
import network.nerve.converter.btc.txdata.UTXOData;
import network.nerve.converter.btc.txdata.WithdrawalFeeLog;
import network.nerve.converter.btc.txdata.WithdrawalUTXOTxData;
import network.nerve.converter.config.AccountConfig;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterCmdConstant;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.AssetName;
import network.nerve.converter.enums.BindHeterogeneousContractMode;
import network.nerve.converter.helper.HeterogeneousAssetHelper;
import network.nerve.converter.helper.LedgerAssetRegisterHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.model.vo.NerveLockedUTXO;
import network.nerve.converter.model.vo.WithdrawalUTXOVO;
import network.nerve.converter.rpc.call.ConsensusCall;
import network.nerve.converter.rpc.call.LedgerCall;
import network.nerve.converter.rpc.call.SwapCall;
import network.nerve.converter.utils.HeterogeneousUtil;
import network.nerve.converter.utils.LoggerUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.protostuff.ByteString.EMPTY_STRING;
import static network.nerve.converter.utils.ConverterUtil.addressToLowerCase;

/**
 * Heterogeneous chain information provision command
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

    @Autowired
    private AccountConfig accountConfig;


    @CmdAnnotation(cmd = ConverterCmdConstant.GET_HETEROGENEOUS_CHAIN_ASSET_INFO, version = 1.0, description = "Heterogeneous Chain Asset Information Query")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Asset ChainID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "assetID")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "symbol", description = "assetsymbol"),
            @Key(name = "decimals", valueType = int.class, description = "Decimal places of assets"),
            @Key(name = "contractAddress", description = "Asset corresponding contract address(If there is any)"),
            @Key(name = "isToken", description = "Is the assetTOKENasset"),
            @Key(name = "heterogeneousChainId", valueType = int.class, description = "Heterogeneous chainID"),
            @Key(name = "heterogeneousChainSymbol", description = "Heterogeneous chainSymbol"),
            @Key(name = "heterogeneousChainMultySignAddress", description = "Heterogeneous chain with multiple signed addresses")
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
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(assetInfo.getChainId());
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

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_HETEROGENEOUS_CHAIN_ASSET_INFO_LIST, version = 1.0, description = "Query of heterogeneous chain asset information list")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Asset ChainID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "assetID")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = List.class, collectionElement = Map.class, mapKeys = {
            @Key(name = "symbol", description = "assetsymbol"),
            @Key(name = "decimals", valueType = int.class, description = "Decimal places of assets"),
            @Key(name = "contractAddress", description = "Asset corresponding contract address(If there is any)"),
            @Key(name = "isToken", description = "Is the assetTOKENasset"),
            @Key(name = "heterogeneousChainId", valueType = int.class, description = "Heterogeneous chainID"),
            @Key(name = "heterogeneousChainSymbol", description = "Heterogeneous chainSymbol"),
            @Key(name = "heterogeneousChainMultySignAddress", description = "Heterogeneous chain with multiple signed addresses")
    })
    )
    public Response getAssetInfoListById(Map params) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        int size = 0;
        try {
            Integer chainId = Integer.parseInt(params.get("chainId").toString());
            Integer assetId = Integer.parseInt(params.get("assetId").toString());
            List<HeterogeneousAssetInfo> assetInfos = heterogeneousAssetHelper.getHeterogeneousAssetInfo(chainId, assetId);
            if (assetInfos == null || assetInfos.isEmpty()) {
                return failed(ConverterErrorCode.DATA_NOT_FOUND);
            }
            size = null == assetInfos ? 0 : assetInfos.size();
            for (HeterogeneousAssetInfo assetInfo : assetInfos) {
                Map<String, Object> rtMap = new HashMap<>(16);
                if (assetInfo.getChainId() == 101) {
                    if (!heterogeneousDockingManager.existHeterogeneousDocking(assetInfo.getChainId())) {
                        continue;
                    }
                }
                IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(assetInfo.getChainId());
                if (docking == null) {
                    continue;
                }
                rtMap.put("symbol", assetInfo.getSymbol());
                rtMap.put("decimals", assetInfo.getDecimals());
                rtMap.put("contractAddress", assetInfo.getContractAddress());
                rtMap.put("isToken", StringUtils.isNotBlank(assetInfo.getContractAddress()));
                rtMap.put("heterogeneousChainId", docking.getChainId());
                rtMap.put("heterogeneousChainSymbol", docking.getChainSymbol());
                rtMap.put("heterogeneousChainMultySignAddress", docking.getCurrentMultySignAddress());
                rtMap.put("decimalsSubtractedToNerve", assetInfo.getDecimalsSubtractedToNerve());
                resultList.add(rtMap);
            }
        } catch (Exception e) {
            LoggerUtil.LOG.error("Size: " + size, e);
            return failed(e.getMessage());
        }
        return success(resultList);
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_HETEROGENEOUS_CHAIN_ASSET_INFO_BY_ADDRESS, version = 1.0, description = "Heterogeneous Chain Asset Information Query")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainID"),
            @Parameter(parameterName = "contractAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset corresponding contract address")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "chainId", valueType = int.class, description = "Asset ChainID"),
            @Key(name = "assetId", valueType = int.class, description = "assetID"),
            @Key(name = "symbol", description = "assetsymbol"),
            @Key(name = "decimals", valueType = int.class, description = "Decimal places of assets")
    })
    )
    public Response getAssetInfoByAddress(Map params) {
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
        try {
            Integer heterogeneousChainId = Integer.parseInt(params.get("heterogeneousChainId").toString());
            String contractAddress = addressToLowerCase(params.get("contractAddress").toString());
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(heterogeneousChainId);
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

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_HETEROGENEOUS_CHAIN_ASSET_INFO_BY_ID, version = 1.0, description = "Heterogeneous Chain Asset Information Query")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainID"),
            @Parameter(parameterName = "heterogeneousAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chain assetsID")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "chainId", valueType = int.class, description = "Asset ChainID"),
            @Key(name = "assetId", valueType = int.class, description = "assetID"),
            @Key(name = "symbol", description = "assetsymbol"),
            @Key(name = "decimals", valueType = int.class, description = "Decimal places of assets")
    })
    )
    public Response getAssetInfoByHeterogeneousAssetId(Map params) {
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
        try {
            Integer heterogeneousChainId = Integer.parseInt(params.get("heterogeneousChainId").toString());
            Integer heterogeneousAssetId = Integer.parseInt(params.get("heterogeneousAssetId").toString());
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(heterogeneousChainId);
            if (docking == null) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            HeterogeneousAssetInfo assetInfo = docking.getAssetByAssetId(heterogeneousAssetId);
            NerveAssetInfo nerveAssetInfo = ledgerAssetRegisterHelper.getNerveAssetInfo(assetInfo.getChainId(), assetInfo.getAssetId());
            rtMap.put("chainId", nerveAssetInfo.getAssetChainId());
            rtMap.put("assetId", nerveAssetInfo.getAssetId());
            rtMap.put("symbol", assetInfo.getSymbol());
            int chainId = nerveAssetInfo.getAssetChainId();
            int assetId = nerveAssetInfo.getAssetId();
            if ((chainId == 9 && assetId == 160) || (chainId == 5 && assetId == 34)) {
                rtMap.put("symbol", "POL");
            } else if ((chainId == 9 && assetId == 448) || (chainId == 5 && assetId == 118)) {
                rtMap.put("symbol", "KAIA");
            } else if ((chainId == 9 && assetId == 692) || (chainId == 5 && assetId == 148)) {
                rtMap.put("symbol", "A");
            }
            if (heterogeneousChainId == 119 && heterogeneousAssetId == 1) {
                rtMap.put("decimals", 8);
            } else {
                rtMap.put("decimals", assetInfo.getDecimals());
            }
        } catch (Exception e) {
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.VALIDATE_HETEROGENEOUS_CONTRACT_ASSET_REG_TX, version = 1.0, description = "Verify heterogeneous chain contract asset registration transactions")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainchainId"),
            @Parameter(parameterName = "decimals", requestType = @TypeDescriptor(value = int.class), parameterDes = "Decimal places of assets"),
            @Parameter(parameterName = "symbol", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset symbols"),
            @Parameter(parameterName = "contractAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset corresponding contract address")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
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
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(heterogeneousChainId);
            if (docking == null) {
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
            }
            HeterogeneousAssetInfo assetInfo = docking.getAssetByContractAddress(contractAddress);
            // Asset already exists
            if (assetInfo != null) {
                LoggerUtil.LOG.error("Asset already exists");
                throw new NulsException(ConverterErrorCode.ASSET_EXIST);
            }
            // Asset information verification
            if (!docking.validateHeterogeneousAssetInfoFromNet(contractAddress, symbol, decimals)) {
                LoggerUtil.LOG.error("Asset information mismatch");
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

    @CmdAnnotation(cmd = ConverterCmdConstant.CREATE_HETEROGENEOUS_CONTRACT_ASSET_REG_TX, version = 1.0, description = "Create heterogeneous chain contract asset registration transactions")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainchainId"),
            @Parameter(parameterName = "decimals", requestType = @TypeDescriptor(value = int.class), parameterDes = "Decimal places of assets"),
            @Parameter(parameterName = "symbol", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset symbols"),
            @Parameter(parameterName = "contractAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset corresponding contract address"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "Transaction notes", canNull = true),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
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

            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
            }

            contractAddress = addressToLowerCase(contractAddress.trim());

            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isSeedVirtualBankBySignAddr(address)) {
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


    @CmdAnnotation(cmd = ConverterCmdConstant.GET_HETEROGENEOUS_ADDRESS, version = 1.0, description = "Query heterogeneous chain addresses corresponding to consensus node packaging addresses")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainID"),
            @Parameter(parameterName = "packingAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "Consensus node packaging address")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "heterogeneousAddress", description = "Heterogeneous Chain Address"),
    })
    )
    public Response getHeterogeneousAddress(Map params) {
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
        try {
            Integer chainId = Integer.parseInt(params.get("chainId").toString());
            Integer heterogeneousChainId = Integer.parseInt(params.get("heterogeneousChainId").toString());
            String packingAddress = params.get("packingAddress").toString();
            if (chainId == 5 && heterogeneousChainId == 101) {
                heterogeneousChainId = 118;
            }
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(heterogeneousChainId);
            if (docking == null) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            Chain chain = chainManager.getChain(chainId);
            LatestBasicBlock latestBasicBlock = chain.getLatestBasicBlock();
            // Get the latest consensus list
            List<AgentBasic> listAgent = ConsensusCall.getAgentList(chain, latestBasicBlock.getHeight());
            if (null == listAgent) {
                chain.getLogger().error("Obtain consensus node list data from the consensus module asnull");
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

    @CmdAnnotation(cmd = ConverterCmdConstant.VALIDATE_BIND_HETEROGENEOUS_CONTRACT_TOKEN_TO_NERVE_ASSET_REG_TX, version = 1.0, description = "validateNerveAsset binding heterogeneous chain contract assets")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainchainId"),
            @Parameter(parameterName = "decimals", requestType = @TypeDescriptor(value = int.class), parameterDes = "Decimal places of assets"),
            @Parameter(parameterName = "symbol", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset symbols"),
            @Parameter(parameterName = "contractAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset corresponding contract address"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveAsset ChainID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveassetID")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
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
            if (heterogeneousChainId != AssetName.TBC.chainId() && nerveAssetDecimals != decimals) {
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

    @CmdAnnotation(cmd = ConverterCmdConstant.BIND_HETEROGENEOUS_CONTRACT_TOKEN_TO_NERVE_ASSET_REG_TX, version = 1.0, description = "NerveAsset binding heterogeneous chain contract assets[New binding]")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainchainId"),
            @Parameter(parameterName = "decimals", requestType = @TypeDescriptor(value = int.class), parameterDes = "Decimal places of assets"),
            @Parameter(parameterName = "symbol", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset symbols"),
            @Parameter(parameterName = "contractAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset corresponding contract address"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveAsset ChainID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveassetID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
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
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
            }
            contractAddress = addressToLowerCase(contractAddress.trim());


            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isSeedVirtualBankBySignAddr(address)) {
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

    @CmdAnnotation(cmd = ConverterCmdConstant.OVERRIDE_BIND_HETEROGENEOUS_CONTRACT_TOKEN_TO_NERVE_ASSET_REG_TX, version = 1.0, description = "NerveAsset binding heterogeneous chain contract assets[Overwrite binding]")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainchainId"),
            @Parameter(parameterName = "contractAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "Asset corresponding contract address"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveAsset ChainID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveassetID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
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
            //ObjectUtils.canNotEmpty(params.get("contractAddress"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
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
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
            }
            contractAddress = addressToLowerCase(contractAddress.trim());

            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isSeedVirtualBankBySignAddr(address)) {
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
            HeterogeneousAssetInfo hAssetInfo;
            if (StringUtils.isBlank(contractAddress)) {
                hAssetInfo = docking.getMainAsset();
            } else {
                hAssetInfo = docking.getAssetByContractAddress(contractAddress);
                if (hAssetInfo == null) {
                    throw new NulsRuntimeException(ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND);
                }
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

    @CmdAnnotation(cmd = ConverterCmdConstant.UNBIND_HETEROGENEOUS_CONTRACT_TOKEN_TO_NERVE_ASSET_REG_TX, version = 1.0, description = "NerveUnbind assets to heterogeneous chain contract assets[Unbind]")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainchainId"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveAsset ChainID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveassetID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
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
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
            }
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isSeedVirtualBankBySignAddr(address)) {
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
            // wrongNervechainIDThe assets must be bound assets of heterogeneous chains
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

    @CmdAnnotation(cmd = ConverterCmdConstant.CREATE_HETEROGENEOUS_MAIN_ASSET_REG_TX, version = 1.0, description = "Create heterogeneous chain master asset registration transaction")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainchainId"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "Transaction notes", canNull = true),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
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
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
            }
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isSeedVirtualBankBySignAddr(address)) {
                throw new NulsRuntimeException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
            }
            // exclude (nuls & EVM:enuls) (eth & EVM:goerliETH)
            if (!HeterogeneousUtil.checkHeterogeneousMainAssetReg(heterogeneousChainId)) {
                throw new NulsRuntimeException(ConverterErrorCode.NO_LONGER_SUPPORTED);
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

    @CmdAnnotation(cmd = ConverterCmdConstant.CREATE_HETEROGENEOUS_MAIN_ASSET_BIND_TX, version = 1.0, description = "Create heterogeneous chain master asset bindingNERVEAsset trading")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainchainId"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveAsset ChainID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveassetID"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "Transaction notes", canNull = true),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
    })
    )
    public Response createHeterogeneousMainAssetBindTx(Map params) {
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
            String remark = (String) params.get("remark");
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
            }
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isSeedVirtualBankBySignAddr(address)) {
                throw new NulsRuntimeException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
            }
            // only (nuls & EVM:enuls) (eth & EVM:goerliETH)
            if (!HeterogeneousUtil.checkHeterogeneousMainAssetBind(chainId, heterogeneousChainId, nerveAssetChainId, nerveAssetId)) {
                throw new NulsRuntimeException(ConverterErrorCode.NO_LONGER_SUPPORTED);
            }
            Transaction tx = assembleTxService.createHeterogeneousMainAssetBindTx(chain, address, password, heterogeneousChainId, nerveAssetChainId, nerveAssetId, remark);
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

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_HETEROGENEOUS_REGISTER_NETWORK, version = 1.0, description = "Heterogeneous chain registration network for querying assets")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Asset ChainID"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "assetID")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "heterogeneousChainId", valueType = int.class, description = "Heterogeneous chainID"),
            @Key(name = "contractAddress", valueType = String.class, description = "Asset corresponding contract address(If there is any)")
    })
    )
    public Response getRegisterNetwork(Map params) {
        Chain chain = null;
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
        try {
            chain = chainManager.getChain(converterConfig.getChainId());
            Integer chainId = Integer.parseInt(params.get("chainId").toString());
            Integer assetId = Integer.parseInt(params.get("assetId").toString());
            int registerNetwork;
            String contractAddress = null;
            HeterogeneousAssetInfo assetInfo = ConverterContext.assetRegisterNetwork.get(chainId + "_" + assetId);
            if (assetInfo == null) {
                registerNetwork = 0;
            } else {
                registerNetwork = assetInfo.getChainId();
                contractAddress = assetInfo.getContractAddress();
            }
            rtMap.put("heterogeneousChainId", registerNetwork);
            rtMap.put("contractAddress", contractAddress);
            /*List<HeterogeneousAssetInfo> assetInfos = heterogeneousAssetHelper.getHeterogeneousAssetInfo(chainId, assetId);
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
            rtMap.put("heterogeneousChainId", resultChainId);*/
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_HETEROGENEOUS_NETWORK_CHAIN_ID, version = 1.0, description = "Heterogeneous chain registration network for querying assets")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainchainId")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "heterogeneousNetworkChainId", valueType = long.class, description = "Within heterogeneous chain networkschainId")
    })
    )
    public Response getHtgNetworkChainId(Map params) {
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_8);
        try {
            Integer heterogeneousChainId = Integer.parseInt(params.get("heterogeneousChainId").toString());
            if (heterogeneousChainId == 101 && !heterogeneousDockingManager.existHeterogeneousDocking(heterogeneousChainId)) {
                heterogeneousChainId = 118;
            }
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(heterogeneousChainId);
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

    @CmdAnnotation(cmd = ConverterCmdConstant.UNREGISTER_HETEROGENEOUS_CONTRACT_TOKEN_TO_NERVE_ASSET_REG_TX, version = 1.0, description = "NerveAsset deregistration of heterogeneous chain contract assets[Cancel registration]")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainchainId"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveAsset ChainID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveassetID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
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
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
            }
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isSeedVirtualBankBySignAddr(address)) {
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

    @CmdAnnotation(cmd = ConverterCmdConstant.PAUSE_IN_HETEROGENEOUS_CONTRACT_TOKEN_TX, version = 1.0, description = "Suspend asset recharge for heterogeneous chain contracts")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainchainId"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveAsset ChainID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveassetID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
    })
    )
    public Response pauseInHeterogeneousContractTokenTx(Map params) {
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
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
            }
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isSeedVirtualBankBySignAddr(address)) {
                throw new NulsRuntimeException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
            }
            Map<String, Object> nerveAsset = LedgerCall.getNerveAsset(chainId, nerveAssetChainId, nerveAssetId);
            boolean existNerveAsset = nerveAsset != null;
            if (!existNerveAsset) {
                throw new NulsRuntimeException(ConverterErrorCode.ASSET_ID_NOT_EXIST);
            }
            Integer assetType = Integer.parseInt(nerveAsset.get("assetType").toString());
            if (assetType < 4 || assetType == 10) {
                throw new NulsRuntimeException(ConverterErrorCode.HETEROGENEOUS_INFO_NOT_MATCH);
            }
            HeterogeneousAssetInfo assetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(heterogeneousChainId, nerveAssetChainId, nerveAssetId);
            if (assetInfo == null) {
                throw new NulsRuntimeException(ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND);
            }

            Transaction tx = assembleTxService.createHeterogeneousContractAssetRegPendingTx(chain, address, password,
                    heterogeneousChainId,
                    assetInfo.getDecimals(),
                    assetInfo.getSymbol(),
                    assetInfo.getContractAddress(),
                    String.valueOf(String.format("%s:%s-%s:in", BindHeterogeneousContractMode.PAUSE, nerveAssetChainId, nerveAssetId)));
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

    @CmdAnnotation(cmd = ConverterCmdConstant.RESUME_IN_HETEROGENEOUS_CONTRACT_TOKEN_TX, version = 1.0, description = "Restore heterogeneous chain contract asset recharge")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainchainId"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveAsset ChainID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveassetID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
    })
    )
    public Response resumeInHeterogeneousContractTokenTx(Map params) {
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
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
            }
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isSeedVirtualBankBySignAddr(address)) {
                throw new NulsRuntimeException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
            }
            Map<String, Object> nerveAsset = LedgerCall.getNerveAsset(chainId, nerveAssetChainId, nerveAssetId);
            boolean existNerveAsset = nerveAsset != null;
            if (!existNerveAsset) {
                throw new NulsRuntimeException(ConverterErrorCode.ASSET_ID_NOT_EXIST);
            }
            Integer assetType = Integer.parseInt(nerveAsset.get("assetType").toString());
            if (assetType < 4 || assetType == 10) {
                throw new NulsRuntimeException(ConverterErrorCode.HETEROGENEOUS_INFO_NOT_MATCH);
            }
            HeterogeneousAssetInfo assetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(heterogeneousChainId, nerveAssetChainId, nerveAssetId);
            if (assetInfo == null) {
                throw new NulsRuntimeException(ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND);
            }

            Transaction tx = assembleTxService.createHeterogeneousContractAssetRegPendingTx(chain, address, password,
                    heterogeneousChainId,
                    assetInfo.getDecimals(),
                    assetInfo.getSymbol(),
                    assetInfo.getContractAddress(),
                    String.valueOf(String.format("%s:%s-%s:in", BindHeterogeneousContractMode.RESUME, nerveAssetChainId, nerveAssetId)));
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

    @CmdAnnotation(cmd = ConverterCmdConstant.PAUSE_OUT_HETEROGENEOUS_CONTRACT_TOKEN_TX, version = 1.0, description = "Suspend withdrawal of heterogeneous chain contract assets")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainchainId"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveAsset ChainID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveassetID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
    })
    )
    public Response pauseOutHeterogeneousContractTokenTx(Map params) {
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
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
            }
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isSeedVirtualBankBySignAddr(address)) {
                throw new NulsRuntimeException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
            }
            Map<String, Object> nerveAsset = LedgerCall.getNerveAsset(chainId, nerveAssetChainId, nerveAssetId);
            boolean existNerveAsset = nerveAsset != null;
            if (!existNerveAsset) {
                throw new NulsRuntimeException(ConverterErrorCode.ASSET_ID_NOT_EXIST);
            }
            Integer assetType = Integer.parseInt(nerveAsset.get("assetType").toString());
            if (assetType < 4 || assetType == 10) {
                throw new NulsRuntimeException(ConverterErrorCode.HETEROGENEOUS_INFO_NOT_MATCH);
            }
            HeterogeneousAssetInfo assetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(heterogeneousChainId, nerveAssetChainId, nerveAssetId);
            if (assetInfo == null) {
                throw new NulsRuntimeException(ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND);
            }

            Transaction tx = assembleTxService.createHeterogeneousContractAssetRegPendingTx(chain, address, password,
                    heterogeneousChainId,
                    assetInfo.getDecimals(),
                    assetInfo.getSymbol(),
                    assetInfo.getContractAddress(),
                    String.valueOf(String.format("%s:%s-%s:out", BindHeterogeneousContractMode.PAUSE, nerveAssetChainId, nerveAssetId)));
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

    @CmdAnnotation(cmd = ConverterCmdConstant.RESUME_OUT_HETEROGENEOUS_CONTRACT_TOKEN_TX, version = 1.0, description = "Recovery of heterogeneous chain contract asset withdrawals")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainchainId"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveAsset ChainID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveassetID"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
    })
    )
    public Response resumeOutHeterogeneousContractTokenTx(Map params) {
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
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
            }
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isSeedVirtualBankBySignAddr(address)) {
                throw new NulsRuntimeException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
            }
            Map<String, Object> nerveAsset = LedgerCall.getNerveAsset(chainId, nerveAssetChainId, nerveAssetId);
            boolean existNerveAsset = nerveAsset != null;
            if (!existNerveAsset) {
                throw new NulsRuntimeException(ConverterErrorCode.ASSET_ID_NOT_EXIST);
            }
            Integer assetType = Integer.parseInt(nerveAsset.get("assetType").toString());
            if (assetType < 4 || assetType == 10) {
                throw new NulsRuntimeException(ConverterErrorCode.HETEROGENEOUS_INFO_NOT_MATCH);
            }
            HeterogeneousAssetInfo assetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(heterogeneousChainId, nerveAssetChainId, nerveAssetId);
            if (assetInfo == null) {
                throw new NulsRuntimeException(ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND);
            }

            Transaction tx = assembleTxService.createHeterogeneousContractAssetRegPendingTx(chain, address, password,
                    heterogeneousChainId,
                    assetInfo.getDecimals(),
                    assetInfo.getSymbol(),
                    assetInfo.getContractAddress(),
                    String.valueOf(String.format("%s:%s-%s:out", BindHeterogeneousContractMode.RESUME, nerveAssetChainId, nerveAssetId)));
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

    @CmdAnnotation(cmd = ConverterCmdConstant.GAS_LIMIT_OF_HETEROGENEOUS_CHAINS, version = 1.0, description = "What heterogeneous chains requiregasLimit")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class))
    public Response gasLimitOfHeterogeneousChains(Map params) {
        Chain chain = null;
        try {
            // check parameters
            if (params == null) {
                LoggerUtil.LOG.warn("params is null");
                throw new NulsRuntimeException(ConverterErrorCode.NULL_PARAMETER);
            }
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            // parse params
            Integer chainId = (Integer) params.get("chainId");
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            Map<Integer, HeterogeneousChainGasInfo> result = heterogeneousDockingManager.getAllHeterogeneousDocking().stream()
                    .collect(Collectors.toMap(IHeterogeneousChainDocking::getChainId, IHeterogeneousChainDocking::getHeterogeneousChainGasInfo));
            return success(result);
        } catch (NulsRuntimeException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode(), e.getMessage());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }


    @CmdAnnotation(cmd = ConverterCmdConstant.PAUSE_COIN_FOR_STABLE_SWAP, version = 1.0, description = "Suspend currency trading in the Multi-Routing pool")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "stablePairAddress", parameterType = "String", parameterDes = "stable pair address"),
            @Parameter(parameterName = "assetChainId", parameterType = "int", parameterDes = "NerveAsset ChainID"),
            @Parameter(parameterName = "assetId", parameterType = "int", parameterDes = "NerveassetID"),
            @Parameter(parameterName = "status", parameterType = "String", parameterDes = "pause/resume"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
    })
    )
    public Response pauseCoinForStableSwap(Map params) {
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
            ObjectUtils.canNotEmpty(params.get("stablePairAddress"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("assetChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("assetId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("status"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("address"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("password"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            // parse params
            Integer chainId = (Integer) params.get("chainId");
            String stablePairAddress = (String) params.get("stablePairAddress");
            Integer nerveAssetChainId = Integer.parseInt(params.get("assetChainId").toString());
            Integer nerveAssetId = Integer.parseInt(params.get("assetId").toString());
            String status = (String) params.get("status");
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
            }
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isSeedVirtualBankBySignAddr(address)) {
                throw new NulsRuntimeException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
            }
            boolean legalCoinForStable = SwapCall.isLegalCoinForStable(chainId, stablePairAddress, nerveAssetChainId, nerveAssetId);
            if (!legalCoinForStable) {
                throw new NulsRuntimeException(ConverterErrorCode.NOT_BIND_ASSET);
            }
            HeterogeneousAssetInfo assetInfo = ConverterContext.assetRegisterNetwork.get(nerveAssetChainId + "_" + nerveAssetId);
            if (assetInfo == null) {
                throw new NulsRuntimeException(ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND);
            }

            Transaction tx = assembleTxService.createHeterogeneousContractAssetRegPendingTx(chain, address, password,
                    assetInfo.getChainId(),
                    assetInfo.getDecimals(),
                    assetInfo.getSymbol(),
                    assetInfo.getContractAddress(),
                    String.valueOf(String.format("%s:%s-%s-%s-%s", BindHeterogeneousContractMode.STABLE_SWAP_COIN_PAUSE, nerveAssetChainId, nerveAssetId, stablePairAddress, status)));
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

    @CmdAnnotation(cmd = ConverterCmdConstant.CHAIN_WITHDRAWAL_FEE, version = 1.0, description = "CHAIN_WITHDRAWAL_FEE")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainID")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "WITHDRAWAL_FEE"),
    }))
    public Response getChainWithdrawalFee(Map params) {
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
        try {
            Integer heterogeneousChainId = Integer.parseInt(params.get("heterogeneousChainId").toString());
            if (heterogeneousChainId < 201) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(heterogeneousChainId);
            if (docking == null) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            rtMap.put("value", docking.getBitCoinApi().getChainWithdrawalFee());
        } catch (Exception e) {
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.HAS_RECORD_FEE_PAYMENT, version = 1.0, description = "HAS_RECORD_FEE_PAYMENT")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainID"),
            @Parameter(parameterName = "htgTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Heterogeneous tx hash")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "true/false")
    }))
    public Response hasRecordFeePayment(Map params) {
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
        try {
            Integer heterogeneousChainId = Integer.parseInt(params.get("heterogeneousChainId").toString());
            if (heterogeneousChainId < 201) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            String htgTxHash = (String) params.get("htgTxHash");
            if (StringUtils.isBlank(htgTxHash)) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid htgTxHash");
            }
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(heterogeneousChainId);
            if (docking == null) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            rtMap.put("value", docking.getBitCoinApi().hasRecordFeePayment(htgTxHash));
        } catch (Exception e) {
            return failed(e.getMessage());
        }
        return success(rtMap);
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.WITHDRAWAL_FEE_LOG, version = 1.0, description = "WITHDRAWAL_FEE_LOG")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainID"),
            @Parameter(parameterName = "htgTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Heterogeneous tx hash")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = WithdrawalFeeLog.class))
    public Response getWithdrawalFeeLogFromDB(Map params) {
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
        try {
            Integer heterogeneousChainId = Integer.parseInt(params.get("heterogeneousChainId").toString());
            if (heterogeneousChainId < 201) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            String htgTxHash = (String) params.get("htgTxHash");
            if (StringUtils.isBlank(htgTxHash)) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid htgTxHash");
            }
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(heterogeneousChainId);
            if (docking == null) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            return success(docking.getBitCoinApi().getWithdrawalFeeLogFromDB(htgTxHash));
        } catch (Exception e) {
            return failed(e.getMessage());
        }

    }

    @CmdAnnotation(cmd = ConverterCmdConstant.MINIMUM_FEE_OF_WITHDRAWAL, version = 1.0, description = "MINIMUM_FEE_OF_WITHDRAWAL")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "heterogeneousChainId"),
            @Parameter(parameterName = "nerveTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "nerve tx hash")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "minimumFee", description = "minimumFee"),
            @Key(name = "utxoSize", description = "utxoSize"),
            @Key(name = "feeRate", description = "feeRate")
    }))
    public Response getMinimumFeeOfWithdrawal(Map params) {
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
        try {
            Integer heterogeneousChainId = Integer.parseInt(params.get("heterogeneousChainId").toString());
            if (heterogeneousChainId < 201) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            String nerveTxHash = (String) params.get("nerveTxHash");
            if (StringUtils.isBlank(nerveTxHash)) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid txHash");
            }
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(heterogeneousChainId);
            if (docking == null) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            return success(docking.getBitCoinApi().getMinimumFeeOfWithdrawal(nerveTxHash));
        } catch (Exception e) {
            return failed(e.getMessage());
        }

    }

    @CmdAnnotation(cmd = ConverterCmdConstant.UNLOCK_UTXO, version = 1.0, description = "unlock utxo")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "nerveTxHash", parameterType = "String", parameterDes = "nerveTxHash"),
            @Parameter(parameterName = "forceUnlock", parameterType = "int", parameterDes = "forceUnlock"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password"),
            @Parameter(parameterName = "htgChainId", parameterType = "int", parameterDes = "htgChainId"),
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "true/false")
    })
    )
    public Response unlockUtxo(Map params) {
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
            ObjectUtils.canNotEmpty(params.get("nerveTxHash"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("forceUnlock"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("address"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("password"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            // parse params
            Integer chainId = (Integer) params.get("chainId");
            String nerveTxHash = (String) params.get("nerveTxHash");
            int forceUnlock = Integer.parseInt(params.get("forceUnlock").toString());
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
            }
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isSeedVirtualBankBySignAddr(address)) {
                throw new NulsRuntimeException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
            }
            Object htgChainId = params.get("htgChainId");
            Transaction tx = assembleTxService.createUnlockUTXOTx(chain, address, password,
                    nerveTxHash,
                    forceUnlock, htgChainId == null ? null : Integer.parseInt(htgChainId.toString()));
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

    @CmdAnnotation(cmd = ConverterCmdConstant.SKIP_TX, version = 1.0, description = "unlock utxo")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "nerveTxHash", parameterType = "String", parameterDes = "nerveTxHash"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "true/false")
    })
    )
    public Response skipTx(Map params) {
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
            ObjectUtils.canNotEmpty(params.get("nerveTxHash"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("address"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("password"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            // parse params
            Integer chainId = (Integer) params.get("chainId");
            String nerveTxHash = (String) params.get("nerveTxHash");
            String address = (String) params.get("address");
            String password = (String) params.get("password");
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
            }
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (!chain.isSeedVirtualBankBySignAddr(address)) {
                throw new NulsRuntimeException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
            }
            Transaction tx = assembleTxService.createSkipTx(chain, address, password,
                    nerveTxHash);
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

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_SPLIT_GRANULARITY, version = 1.0, description = "GET_SPLIT_GRANULARITY")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "heterogeneousChainId")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "splitGranularity")
    }))
    public Response getSplitGranularity(Map params) {
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
        try {
            Integer heterogeneousChainId = Integer.parseInt(params.get("heterogeneousChainId").toString());
            if (heterogeneousChainId < 201) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            Map<String, Object> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("value", docking.getBitCoinApi().getSplitGranularity());
            return success(map);
        } catch (Exception e) {
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_UTXO_CHECKED_INFO, version = 1.0, description = "GET_UTXO_CHECKED_INFO")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "heterogeneousChainId"),
            @Parameter(parameterName = "utxoList", requestType = @TypeDescriptor(value = List.class), parameterDes = "utxoList")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "detail data")
    }))
    public Response getUtxoCheckedInfo(Map params) {
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
        try {
            Integer heterogeneousChainId = Integer.parseInt(params.get("heterogeneousChainId").toString());
            if (heterogeneousChainId < 201) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            List<Map> utxoList = (List<Map>) params.get("utxoList");
            List<UTXOData> utxoDataList = new ArrayList<>();
            for (Map map : utxoList) {
                Object amount = map.get("amount");
                if (amount == null) {
                    amount = map.get("value");
                }
                utxoDataList.add(new UTXOData(map.get("txid").toString(), Integer.parseInt(map.get("vout").toString()), amount == null ? null : new BigInteger(amount.toString())));
            }
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
            List<String> nerveHashOfLockedUTXOList = docking.getBitCoinApi().getNerveHashOfLockedUTXOList(utxoDataList);

            List<NerveLockedUTXO> resultList = new ArrayList<>();
            int i = 0;
            for (UTXOData data : utxoDataList) {
                String nerveHash = nerveHashOfLockedUTXOList.get(i++);
                boolean locked = StringUtils.isNotBlank(nerveHash);
                resultList.add(new NerveLockedUTXO(data.getTxid(), data.getVout(), data.getAmount() == null ? null : data.getAmount().longValue(), locked, nerveHash));
            }
            Map<String, Object> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("value", resultList);
            return success(map);
        } catch (Exception e) {
            LoggerUtil.LOG.error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_WITHDRAWAL_UTXO_INFO, version = 1.0, description = "GET_WITHDRAWAL_UTXO_INFO")
    @Parameters(value = {
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "heterogeneousChainId"),
            @Parameter(parameterName = "nerveTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "nerve tx hash")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "detail data")
    }))
    public Response getWithdrawalUTXOInfo(Map params) {
        Map<String, Object> rtMap = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
        try {
            Integer heterogeneousChainId = Integer.parseInt(params.get("heterogeneousChainId").toString());
            if (heterogeneousChainId < 201) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            String nerveTxHash = (String) params.get("nerveTxHash");
            if (StringUtils.isBlank(nerveTxHash)) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid txHash");
            }
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(heterogeneousChainId);
            if (docking == null) {
                return failed(ConverterErrorCode.PARAMETER_ERROR, "invalid chainId");
            }
            WithdrawalUTXOTxData utxoTxData = docking.getBitCoinApi().takeWithdrawalUTXOs(nerveTxHash);
            if (utxoTxData == null) {
                return failed(ConverterErrorCode.DATA_NOT_FOUND, "invalid nerveTxHash");
            }
            Map<String, Object> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("value", new WithdrawalUTXOVO(utxoTxData));
            return success(map);
        } catch (Exception e) {
            return failed(e.getMessage());
        }

    }

    @CmdAnnotation(cmd = ConverterCmdConstant.CHECK_PAUSE_OUT, version = 1.0, description = "CHECK_PAUSE_OUT")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "htgChainId", parameterType = "int", parameterDes = "htgChainId"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveAsset ChainID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveassetID")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "true/false")
    })
    )
    public Response checkPauseOut(Map params) {
        Chain chain = null;
        try {
            // check parameters
            if (params == null) {
                LoggerUtil.LOG.warn("params is null");
                throw new NulsRuntimeException(ConverterErrorCode.NULL_PARAMETER);
            }
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("htgChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("nerveAssetChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("nerveAssetId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            // parse params
            Integer chainId = (Integer) params.get("chainId");
            Integer htgChainId = (Integer) params.get("htgChainId");
            Integer nerveAssetChainId = (Integer) params.get("nerveAssetChainId");
            Integer nerveAssetId = (Integer) params.get("nerveAssetId");
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            HeterogeneousAssetInfo assetInfo = converterCoreApi.getHeterogeneousAssetByNerveAsset(htgChainId, nerveAssetChainId, nerveAssetId);
            boolean pause = converterCoreApi.isPauseOutHeterogeneousAsset(assetInfo.getChainId(), assetInfo.getAssetId());
            Map<String, Object> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("value", pause);
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

    @CmdAnnotation(cmd = ConverterCmdConstant.CHECK_PAUSE_IN, version = 1.0, description = "CHECK_PAUSE_IN")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "htgChainId", parameterType = "int", parameterDes = "htgChainId"),
            @Parameter(parameterName = "nerveAssetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveAsset ChainID"),
            @Parameter(parameterName = "nerveAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "NerveassetID")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "true/false")
    })
    )
    public Response checkPauseIn(Map params) {
        Chain chain = null;
        try {
            // check parameters
            if (params == null) {
                LoggerUtil.LOG.warn("params is null");
                throw new NulsRuntimeException(ConverterErrorCode.NULL_PARAMETER);
            }
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("htgChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("nerveAssetChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("nerveAssetId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            // parse params
            Integer chainId = (Integer) params.get("chainId");
            Integer htgChainId = (Integer) params.get("htgChainId");
            Integer nerveAssetChainId = (Integer) params.get("nerveAssetChainId");
            Integer nerveAssetId = (Integer) params.get("nerveAssetId");
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            HeterogeneousAssetInfo assetInfo = converterCoreApi.getHeterogeneousAssetByNerveAsset(htgChainId, nerveAssetChainId, nerveAssetId);
            boolean pause = converterCoreApi.isPauseInHeterogeneousAsset(assetInfo.getChainId(), assetInfo.getAssetId());
            Map<String, Object> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("value", pause);
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

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_CROSS_OUT_TX_FEE, version = 1.0, description = "GET_CROSS_OUT_TX_FEE")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", parameterType = "int", parameterDes = "chainid"),
            @Parameter(parameterName = "txHash", parameterType = "String", parameterDes = "tx hash")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value")
    })
    )
    public Response getCrossOutTxFee(Map params) {
        Chain chain = null;
        try {
            // check parameters
            if (params == null) {
                LoggerUtil.LOG.warn("params is null");
                throw new NulsRuntimeException(ConverterErrorCode.NULL_PARAMETER);
            }
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txHash"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            // parse params
            Integer chainId = (Integer) params.get("chainId");
            String txHash = (String) params.get("txHash");
            chain = chainManager.getChain(chainId);
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }

            BigInteger value = converterCoreApi.getCrossOutTxFee(txHash);
            Map<String, Object> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("value", value.toString());
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

    /*@CmdAnnotation(cmd = "cv_test", version = 1.0, description = "test")
    @Parameters(value = {
            @Parameter(parameterName = "params", requestType = @TypeDescriptor(value = String.class), parameterDes = "Test parameters")
    })
    @ResponseData(name = "Return value", description = "Return a Map object", responseType = @TypeDescriptor(value = Map.class))
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

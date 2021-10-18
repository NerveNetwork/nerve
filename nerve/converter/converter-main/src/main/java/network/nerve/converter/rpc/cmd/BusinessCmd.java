/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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

package network.nerve.converter.rpc.cmd;

import com.fasterxml.jackson.databind.DeserializationFeature;
import io.nuls.base.RPCUtil;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.model.ObjectUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterCmdConstant;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.business.HeterogeneousService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.dto.*;
import network.nerve.converter.model.po.ConfirmWithdrawalPO;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.po.TxSubsequentProcessPO;
import network.nerve.converter.model.txdata.ProposalTxData;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.*;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.LoggerUtil;
import network.nerve.converter.utils.VirtualBankUtil;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: Loki
 * @date: 2020/3/17
 */
@Component
public class BusinessCmd extends BaseCmd {

    @Autowired
    private AssembleTxService assembleTxService;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private DisqualificationStorageService disqualificationStorageService;
    @Autowired
    private HeterogeneousService heterogeneousService;
    @Autowired
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService;
    @Autowired
    private ProposalStorageService proposalStorageService;
    @Autowired
    private RechargeStorageService rechargeStorageService;
    @Autowired
    private ConfirmWithdrawalStorageService confirmWithdrawalStorageService;

    @CmdAnnotation(cmd = ConverterCmdConstant.WITHDRAWAL, version = 1.0, description = "提现")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "assetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "提现资产链id"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "提现资产id"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链id"),
            @Parameter(parameterName = "heterogeneousAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "提现到账地址"),
            @Parameter(parameterName = "amount", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "提现到账金额"),
            @Parameter(parameterName = "distributionFee", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "手续费"),
            @Parameter(parameterName = "feeChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "手续费链ID(5/9,101,102,103....)"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "交易备注"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "支付/签名地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "密码")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "交易hash")
    })
    )
    public Response withdrawal(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("assetChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("assetId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("heterogeneousAddress"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("distributionFee"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("amount"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("address"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("password"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (chain.getLatestBasicBlock().getSyncStatusEnum() == SyncStatusEnum.SYNC) {
                throw new NulsException(ConverterErrorCode.PAUSE_NEWTX);
            }
            Integer feeChainId = (Integer) params.get("feeChainId");
            if (feeChainId == null) {
                feeChainId = chain.getChainId();
            }
            SignAccountDTO signAccountDTO = new SignAccountDTO();
            signAccountDTO.setAddress((String) params.get("address"));
            signAccountDTO.setPassword((String) params.get("password"));
            // parse params
            JSONUtils.getInstance().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            WithdrawalTxDTO withdrawalTxDTO = JSONUtils.map2pojo(params, WithdrawalTxDTO.class);
            withdrawalTxDTO.setSignAccount(signAccountDTO);
            withdrawalTxDTO.setFeeChainId(feeChainId);

            Transaction tx = assembleTxService.createWithdrawalTx(chain, withdrawalTxDTO);
            Map<String, String> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("value", tx.getHash().toHex());
            map.put("hex", RPCUtil.encode(tx.serialize()));
            return success(map);
        } catch (NulsRuntimeException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }


    @CmdAnnotation(cmd = ConverterCmdConstant.WITHDRAWAL_ADDITIONAL_FEE, version = 1.0, description = "追加提现手续费/原路退回的提案")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "原始交易hash"),
            @Parameter(parameterName = "feeChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "追加的主资产的链ID(5/9,101,102,103....)"),
            @Parameter(parameterName = "amount", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "追加的手续费金额"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "交易备注"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "支付/签名地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "密码")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "交易hash")
    })
    )
    public Response withdrawalAdditionalFee(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txHash"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("amount"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("address"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("password"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (chain.getLatestBasicBlock().getSyncStatusEnum() == SyncStatusEnum.SYNC) {
                throw new NulsException(ConverterErrorCode.PAUSE_NEWTX);
            }
            Integer feeChainId = (Integer) params.get("feeChainId");
            if (feeChainId == null) {
                feeChainId = chain.getChainId();
            }
            SignAccountDTO signAccountDTO = new SignAccountDTO();
            signAccountDTO.setAddress((String) params.get("address"));
            signAccountDTO.setPassword((String) params.get("password"));
            // parse params
            JSONUtils.getInstance().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            WithdrawalAdditionalFeeTxDTO withdrawalAdditionalFeeTxDTO = JSONUtils.map2pojo(params, WithdrawalAdditionalFeeTxDTO.class);
            withdrawalAdditionalFeeTxDTO.setSignAccount(signAccountDTO);
            withdrawalAdditionalFeeTxDTO.setFeeChainId(feeChainId);

            Transaction tx = assembleTxService.withdrawalAdditionalFeeTx(chain, withdrawalAdditionalFeeTxDTO);
            Map<String, String> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("value", tx.getHash().toHex());
            map.put("hex", RPCUtil.encode(tx.serialize()));
            return success(map);
        } catch (NulsRuntimeException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }


    @CmdAnnotation(cmd = ConverterCmdConstant.VIRTUAL_BANK_INFO, version = 1.0, description = "获取当前所有虚拟银行成员信息")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "balance", requestType = @TypeDescriptor(value = boolean.class),
                    parameterDes = "是否需要获取各银行节点支付手续费异构链地址余额(可不传,默认false)")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "list", description = "List<VirtualBankDirectorDTO>", valueType = List.class, valueElement = VirtualBankDirectorDTO.class)
    })
    )
    public Response getVirtualBankDirector(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            boolean checkBalance = false;
            if (null != params.get("balance")) {
                checkBalance = (boolean) params.get("balance");
            }
            Map<String, VirtualBankDirector> mapVirtualBank = chain.getMapVirtualBank();
            List<VirtualBankDirectorDTO> list = new ArrayList<>();
            for (VirtualBankDirector director : mapVirtualBank.values()) {
                VirtualBankDirectorDTO directorDTO = new VirtualBankDirectorDTO(director);
                for (HeterogeneousAddressDTO addr : directorDTO.getHeterogeneousAddresses()) {
                    IHeterogeneousChainDocking heterogeneousDocking = heterogeneousDockingManager.getHeterogeneousDocking(addr.getChainId());
                    String chainSymbol = heterogeneousDocking.getChainSymbol();
                    addr.setSymbol(chainSymbol);
                }
                list.add(directorDTO);
            }
            Map<String, List<VirtualBankDirectorDTO>> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("list", list);
            if (!checkBalance) {
                return success(map);
            }
            if (ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST.isEmpty()) {
                chain.getLogger().debug("缓存未建立，直接查询异构链余额");
                // 并行查询异构链余额
                VirtualBankUtil.virtualBankDirectorBalance(list, chain, heterogeneousDockingManager);
            } else {
                chain.getLogger().debug("使用缓存的异构链余额");
                // 使用缓存的异构链余额
                Map<String, VirtualBankDirectorDTO> cacheMap = ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST.stream().collect(Collectors.toMap(VirtualBankDirectorDTO::getSignAddress, Function.identity(), (key1, key2) -> key2));
                for (VirtualBankDirectorDTO dto : list) {
                    VirtualBankDirectorDTO cacheDto = cacheMap.get(dto.getSignAddress());
                    if (cacheDto != null) {
                        dto.setHeterogeneousAddresses(cacheDto.getHeterogeneousAddresses());
                    }
                }
            }
            return success(map);
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.BROADCAST_PROPOSAL, version = 1.0, description = "处理新的提案交易(已组装好的交易)")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txHex", requestType = @TypeDescriptor(value = String.class), parameterDes = "完整交易hex")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "交易hash")
    })
    )
    public Response processProposal(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txHex"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (chain.getLatestBasicBlock().getSyncStatusEnum() == SyncStatusEnum.SYNC) {
                throw new NulsException(ConverterErrorCode.PAUSE_NEWTX);
            }
            Transaction tx = ConverterUtil.getInstance((String) params.get("txHex"), Transaction.class);
            tx = assembleTxService.processProposalTx(chain, tx);
            Map<String, String> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("value", tx.getHash().toHex());
            return success(map);
        } catch (NulsRuntimeException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.PROPOSAL, version = 1.0, description = "申请提案")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "type", requestType = @TypeDescriptor(value = byte.class), parameterDes = "提案类型"),
            @Parameter(parameterName = "content", requestType = @TypeDescriptor(value = String.class), parameterDes = "提案类容"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链chainId"),
            @Parameter(parameterName = "heterogeneousTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "异构链交易hash"),
            @Parameter(parameterName = "businessAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "地址（账户、节点地址等）"),
            @Parameter(parameterName = "hash", requestType = @TypeDescriptor(value = String.class), parameterDes = "链内交易hash"),
            @Parameter(parameterName = "voteRangeType", requestType = @TypeDescriptor(value = byte.class), parameterDes = "投票范围类型"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "备注"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "签名地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "密码")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "交易hash")
    })
    )
    public Response proposal(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("type"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("content"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (chain.getLatestBasicBlock().getSyncStatusEnum() == SyncStatusEnum.SYNC) {
                throw new NulsException(ConverterErrorCode.PAUSE_NEWTX);
            }
            // parse params
            JSONUtils.getInstance().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ProposalTxDTO proposalTxDTO = JSONUtils.map2pojo(params, ProposalTxDTO.class);


            SignAccountDTO signAccountDTO = new SignAccountDTO();
            signAccountDTO.setAddress((String) params.get("address"));
            signAccountDTO.setPassword((String) params.get("password"));
            proposalTxDTO.setSignAccountDTO(signAccountDTO);
            Transaction tx = assembleTxService.createProposalTx(chain, proposalTxDTO);
            Map<String, String> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("value", tx.getHash().toHex());
            map.put("hex", RPCUtil.encode(tx.serialize()));
            try {
                chain.getLogger().info("提案tx hex: {}", RPCUtil.encode(tx.serialize()));
            } catch (Exception e) {
                chain.getLogger().warn("日志调用失败[0]: {}", e.getMessage());
            }
            try {
                chain.getLogger().info("提案tx format: {}", tx.format(ProposalTxData.class));
            } catch (Exception e) {
                chain.getLogger().warn("日志调用失败[1]: {}", e.getMessage());
            }
            try {
                chain.getLogger().info("提案参数: {}", JSONUtils.obj2PrettyJson(proposalTxDTO));
            } catch (Exception e) {
                chain.getLogger().warn("日志调用失败[2]: {}", e.getMessage());
            }
            return success(map);
        } catch (NulsRuntimeException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.RESET_VIRTUAL_BANK, version = 1.0, description = "重置虚拟银行异构链(合约)")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链id"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "签名地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "密码")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "交易hash")
    })
    )
    public Response resetVirtualBank(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("heterogeneousChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("address"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("password"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (chain.getLatestBasicBlock().getSyncStatusEnum() == SyncStatusEnum.SYNC) {
                throw new NulsException(ConverterErrorCode.PAUSE_NEWTX);
            }
            int heterogeneousChainId = ((Integer) params.get("heterogeneousChainId")).byteValue();
            SignAccountDTO signAccountDTO = new SignAccountDTO();
            signAccountDTO.setAddress((String) params.get("address"));
            signAccountDTO.setPassword((String) params.get("password"));
            if (!chain.isVirtualBankBySignAddr(signAccountDTO.getAddress())) {
                throw new NulsRuntimeException(ConverterErrorCode.SIGNER_NOT_VIRTUAL_BANK_AGENT);
            }
            Transaction tx = assembleTxService.createResetVirtualBankTx(chain, heterogeneousChainId, signAccountDTO);
            Map<String, String> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("value", tx.getHash().toHex());
            map.put("hex", RPCUtil.encode(tx.serialize()));
            return success(map);
        } catch (NulsRuntimeException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }


    @CmdAnnotation(cmd = ConverterCmdConstant.VOTE_PROPOSAL, version = 1.0, description = "投票提案")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "proposalTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "提案交易hash"),
            @Parameter(parameterName = "choice", requestType = @TypeDescriptor(value = byte.class), parameterDes = "表决"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "备注"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "签名地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "密码")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "交易hash")
    })
    )
    public Response voteProposal(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("proposalTxHash"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("choice"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("address"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("password"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (chain.getLatestBasicBlock().getSyncStatusEnum() == SyncStatusEnum.SYNC) {
                throw new NulsException(ConverterErrorCode.PAUSE_NEWTX);
            }
            String proposalTxHash = (String) params.get("proposalTxHash");
            byte choice = ((Integer) params.get("choice")).byteValue();
            String remark = (String) params.get("remark");
            SignAccountDTO signAccountDTO = new SignAccountDTO();
            signAccountDTO.setAddress((String) params.get("address"));
            signAccountDTO.setPassword((String) params.get("password"));

            Transaction tx = assembleTxService.createVoteProposalTx(chain, NulsHash.fromHex(proposalTxHash), choice, remark, signAccountDTO);
            Map<String, String> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("value", tx.getHash().toHex());
            map.put("hex", RPCUtil.encode(tx.serialize()));
            return success(map);
        } catch (NulsRuntimeException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }


    @CmdAnnotation(cmd = ConverterCmdConstant.DISQUALIFICATION, version = 1.0, description = "获取已撤销虚拟银行资格节点地址列表")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "list", valueType = List.class, valueElement = String.class, description = "节点地址")
    })
    )
    public Response getDisqualification(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (chain.getLatestBasicBlock().getSyncStatusEnum() == SyncStatusEnum.SYNC) {
                throw new NulsException(ConverterErrorCode.PAUSE_NEWTX);
            }
            List<String> list = disqualificationStorageService.findAll(chain);
            Map<String, List<String>> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("list", list);
            return success(map);
        } catch (NulsRuntimeException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.CHECK_RETRY_PARSE, version = 1.0, description = "重新解析异构链交易")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链id"),
            @Parameter(parameterName = "heterogeneousTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "异构链交易hash")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "是否成功")
    })
    )
    public Response checkRetryParse(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("heterogeneousChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("heterogeneousTxHash"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            int heterogeneousChainId = (Integer) params.get("heterogeneousChainId");
            String heterogeneousTxHash = params.get("heterogeneousTxHash").toString();
            Map<String, Boolean> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);

            if (!VirtualBankUtil.isCurrentDirector(chain)) {
                chain.getLogger().error("当前非虚拟银行成员节点, 不处理checkRetryParse");
                map.put("value", false);
            } else {
                heterogeneousService.checkRetryParse(chain, heterogeneousChainId, heterogeneousTxHash);
                map.put("value", true);
            }
            return success(map);
        } catch (NulsRuntimeException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.RETRY_WITHDRAWAL, version = 1.0, description = "重新将异构链提现交易放入task, 重发消息")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "hash", requestType = @TypeDescriptor(value = int.class), parameterDes = "链内提现交易hash")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "是否成功")
    })
    )
    public Response retryWithdrawalMsg(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("hash"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            Map<String, Boolean> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            if (!VirtualBankUtil.isCurrentDirector(chain)) {
                chain.getLogger().error("当前非虚拟银行成员节点, 不处理retryWithdrawalMsg");
                map.put("value", false);
            } else {
                String txHash = params.get("hash").toString();
                Transaction tx = TransactionCall.getConfirmedTx(chain, txHash);
                if(null == tx) {
                    throw new NulsRuntimeException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
                }
                TxSubsequentProcessPO pendingPO = new TxSubsequentProcessPO();
                pendingPO.setRetry(true);
                pendingPO.setTx(tx);
                pendingPO.setCurrenVirtualBankTotal(chain.getMapVirtualBank().size());
                BlockHeader header = new BlockHeader();
                header.setHeight(chain.getLatestBasicBlock().getHeight());
                pendingPO.setBlockHeader(header);
                txSubsequentProcessStorageService.save(chain, pendingPO);
                map.put("value", chain.getPendingTxQueue().offer(pendingPO));
            }
            return success(map);
        } catch (NulsRuntimeException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_PROPOSAL_INFO, version = 1.0, description = "查询提案")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "proposalTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "提案交易hash")
    })
    @ResponseData(name = "返回值", description = "返回 network.nerve.converter.model.po.ProposalPO 对象的序列化字符串")
    public Response getProposalInfo(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("proposalTxHash"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            String proposalTxHash = (String) params.get("proposalTxHash");
            ProposalPO po = proposalStorageService.find(chain, NulsHash.fromHex(proposalTxHash));
            if (po == null) {
                return failed(ConverterErrorCode.DATA_NOT_FOUND);
            }
            return success(HexUtil.encode(po.serialize()));
        } catch (NulsRuntimeException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }
    @CmdAnnotation(cmd = ConverterCmdConstant.CANCEL_HTG_TX, version = 1.0, description = "取消虚拟银行发出的异构链网络交易")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "异构链id"),
            @Parameter(parameterName = "heterogeneousAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "虚拟银行管理员签名账户地址"),
            @Parameter(parameterName = "nonce", requestType = @TypeDescriptor(value = String.class), parameterDes = "账户nonce"),
            @Parameter(parameterName = "priceGwei", requestType = @TypeDescriptor(value = String.class), parameterDes = "异构链price(Gwei)")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "是否成功")
    })
    )
    public Response cancelHtgTx(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("heterogeneousChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("heterogeneousAddress"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("nonce"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("priceGwei"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            int heterogeneousChainId = (Integer) params.get("heterogeneousChainId");
            String heterogeneousAddress = params.get("heterogeneousAddress").toString();
            String nonce = params.get("nonce").toString();
            String priceGwei = params.get("priceGwei").toString();
            Map<String, Boolean> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);

            if (!VirtualBankUtil.isCurrentDirector(chain)) {
                chain.getLogger().error("当前非虚拟银行成员节点, 不处理cancelHtgTx");
                map.put("value", false);
            } else {
                heterogeneousService.cancelHtgTx(chain, heterogeneousChainId, heterogeneousAddress, nonce, priceGwei);
                map.put("value", true);
            }
            return success(map);
        } catch (NulsRuntimeException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_RECHARGE_NERVE_HASH, version = 1.0, description = "根据异构链跨链转入的交易hash查询NERVE的交易hash")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "heterogeneousTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "异构链跨链转入的交易hash")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "交易hash")
    }))
    public Response getRechargeNerveHash(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("heterogeneousTxHash"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            String heterogeneousTxHash = (String) params.get("heterogeneousTxHash");
            NulsHash nerveTxHash = rechargeStorageService.find(chain, heterogeneousTxHash);
            if (nerveTxHash == null) {
                return failed(ConverterErrorCode.DATA_NOT_FOUND);
            }
            Map<String, String> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("value", nerveTxHash.toHex());
            return success(map);
        } catch (NulsRuntimeException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @CmdAnnotation(cmd = ConverterCmdConstant.FIND_BY_WITHDRAWAL_TX_HASH, version = 1.0, description = "根据提现交易hash获取确认信息")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "txHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "提现交易hash")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "heterogeneousChainId", description = "异构链ID"),
            @Key(name = "heterogeneousHeight", description = "异构链交易区块高度"),
            @Key(name = "heterogeneousTxHash", description = "异构链交易hash"),
            @Key(name = "confirmWithdrawalTxHash", description = "NERVE确认交易hash")
    }))
    public Response findByWithdrawalTxHash(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("txHash"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            String txHash = (String) params.get("txHash");
            ConfirmWithdrawalPO po = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, NulsHash.fromHex(txHash));
            if (po == null) {
                return failed(ConverterErrorCode.DATA_NOT_FOUND);
            }
            Map<String, Object> map = new HashMap<>();
            map.put("heterogeneousChainId", po.getHeterogeneousChainId());
            map.put("heterogeneousHeight", po.getHeterogeneousHeight());
            map.put("heterogeneousTxHash", po.getHeterogeneousTxHash());
            map.put("confirmWithdrawalTxHash", po.getConfirmWithdrawalTxHash().toHex());
            return success(map);
        } catch (NulsRuntimeException e) {
            errorLogProcess(chain, e);
            return failed(e.getErrorCode());
        } catch (Exception e) {
            errorLogProcess(chain, e);
            return failed(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    private void errorLogProcess(Chain chain, Exception e) {
        if (chain == null) {
            LoggerUtil.LOG.error(e);
        } else {
            chain.getLogger().error(e);
        }
    }
}

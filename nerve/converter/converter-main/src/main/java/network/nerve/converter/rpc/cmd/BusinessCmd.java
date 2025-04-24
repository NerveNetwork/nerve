/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2022 nerve.network
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
import io.nuls.base.basic.AddressTool;
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
import network.nerve.converter.config.AccountConfig;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterCmdConstant;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.business.HeterogeneousService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.AgentBasic;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.dto.*;
import network.nerve.converter.model.po.ConfirmWithdrawalPO;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.po.TxSubsequentProcessPO;
import network.nerve.converter.model.txdata.ChangeVirtualBankTxData;
import network.nerve.converter.model.txdata.ProposalTxData;
import network.nerve.converter.rpc.call.BlockCall;
import network.nerve.converter.rpc.call.ConsensusCall;
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

import static network.nerve.converter.constant.ConverterConstant.MINUTES_5;

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
    @Autowired
    private ConverterCoreApi converterCoreApi;
    @Autowired
    private AccountConfig accountConfig;

    @CmdAnnotation(cmd = ConverterCmdConstant.WITHDRAWAL, version = 1.0, description = "Withdrawal")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "assetChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Withdrawal asset chainid"),
            @Parameter(parameterName = "assetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Withdrawal of assetsid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainid"),
            @Parameter(parameterName = "heterogeneousAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "Withdrawal to account address"),
            @Parameter(parameterName = "amount", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "Withdrawal to account amount"),
            @Parameter(parameterName = "distributionFee", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "Handling fees"),
            @Parameter(parameterName = "feeChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Service fee chainID(5/9,101,102,103....)"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "Transaction notes"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
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
            String password = (String) params.get("password");
            String address = (String) params.get("address");
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
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


    @CmdAnnotation(cmd = ConverterCmdConstant.WITHDRAWAL_ADDITIONAL_FEE, version = 1.0, description = "Additional withdrawal handling fee/Proposal for returning the original route")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "txHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Original transactionhash"),
            @Parameter(parameterName = "feeChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Chain of additional main assetsID(5/9,101,102,103....)"),
            @Parameter(parameterName = "amount", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "Additional handling fee amount"),
            @Parameter(parameterName = "rebuild", requestType = @TypeDescriptor(value = boolean.class), parameterDes = "rebuild for btc tx"),
            @Parameter(parameterName = "htgChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "htgChainId for btc'sys chain tx"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "Transaction notes"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "payment/Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
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
            String password = (String) params.get("password");
            String address = (String) params.get("address");
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
            }
            Integer feeChainId = (Integer) params.get("feeChainId");
            if (feeChainId == null) {
                feeChainId = chain.getChainId();
            }
            boolean rebuild = false;
            Object _rebuild = params.get("rebuild");
            if (_rebuild != null) {
                rebuild = Boolean.parseBoolean(_rebuild.toString());
            }
            Integer htgChainId = null;
            Object _htgChainId = params.get("htgChainId");
            if (_htgChainId != null) {
                htgChainId = Integer.parseInt(_htgChainId.toString());
            }
            SignAccountDTO signAccountDTO = new SignAccountDTO();
            signAccountDTO.setAddress((String) params.get("address"));
            signAccountDTO.setPassword((String) params.get("password"));
            // parse params
            JSONUtils.getInstance().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            WithdrawalAdditionalFeeTxDTO withdrawalAdditionalFeeTxDTO = JSONUtils.map2pojo(params, WithdrawalAdditionalFeeTxDTO.class);
            withdrawalAdditionalFeeTxDTO.setSignAccount(signAccountDTO);
            withdrawalAdditionalFeeTxDTO.setFeeChainId(feeChainId);
            withdrawalAdditionalFeeTxDTO.setRebuild(rebuild);
            withdrawalAdditionalFeeTxDTO.setHtgChainId(htgChainId);

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


    @CmdAnnotation(cmd = ConverterCmdConstant.VIRTUAL_BANK_INFO, version = 1.0, description = "Obtain information on all current virtual bank members")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "balance", requestType = @TypeDescriptor(value = boolean.class),
                    parameterDes = "Do you need to obtain the balance of payment fees for heterogeneous chain addresses at each bank node(No need to transmit it,defaultfalse)")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
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
                    if (addr.getChainId() == 101) {
                        addr.setSymbol("ETH");
                    } else if (addr.getChainId() == 106) {
                        addr.setSymbol("POL");
                    } else if (addr.getChainId() == 116) {
                        addr.setSymbol("KAIA");
                    } else {
                        IHeterogeneousChainDocking heterogeneousDocking = heterogeneousDockingManager.getHeterogeneousDockingSmoothly(addr.getChainId());
                        if (heterogeneousDocking != null) {
                            String chainSymbol = heterogeneousDocking.getChainSymbol();
                            addr.setSymbol(chainSymbol);
                        }
                    }
                }
                list.add(directorDTO);
            }
            Map<String, List<VirtualBankDirectorDTO>> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("list", list);
            if (!checkBalance) {
                return success(map);
            }
            long now = System.currentTimeMillis();
            long cacheRecordTime = ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST_FOR_CMD_RECORD_TIME;
            if (cacheRecordTime > 0 && (now - cacheRecordTime) >= MINUTES_5) {
                ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST_FOR_CMD.clear();
            }
            if (ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST_FOR_CMD.isEmpty()) {
                chain.getLogger().debug("Cache not established, query heterogeneous chain balance directly");
                ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST_FOR_CMD = list;
                ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST_FOR_CMD_RECORD_TIME = now;
                // Parallel query of heterogeneous chain balances
                VirtualBankUtil.virtualBankDirectorBalance(list, chain, heterogeneousDockingManager, 0, converterCoreApi);
            } else {
                chain.getLogger().debug("Use cached heterogeneous chain balance");
                // Use cached heterogeneous chain balance
                Map<String, VirtualBankDirectorDTO> cacheMap = ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST_FOR_CMD.stream().collect(Collectors.toMap(VirtualBankDirectorDTO::getSignAddress, Function.identity(), (key1, key2) -> key2));
                for (VirtualBankDirectorDTO dto : list) {
                    VirtualBankDirectorDTO cacheDto = cacheMap.get(dto.getSignAddress());
                    if (cacheDto != null) {
                        List<HeterogeneousAddressDTO> heterogeneousAddresses = cacheDto.getHeterogeneousAddresses();
                        for (HeterogeneousAddressDTO addressDTO : heterogeneousAddresses) {
                            if (addressDTO.getChainId() == 106) {
                                addressDTO.setSymbol("POL");
                            } else
                            if (addressDTO.getChainId() == 116) {
                                addressDTO.setSymbol("KAIA");
                            }
                        }
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

    @CmdAnnotation(cmd = ConverterCmdConstant.BROADCAST_PROPOSAL, version = 1.0, description = "Handling new proposal transactions(Assembled transactions)")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "txHex", requestType = @TypeDescriptor(value = String.class), parameterDes = "Complete transactionhex")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
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

    @CmdAnnotation(cmd = ConverterCmdConstant.PROPOSAL, version = 1.0, description = "Application proposal")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "type", requestType = @TypeDescriptor(value = byte.class), parameterDes = "Proposal type"),
            @Parameter(parameterName = "content", requestType = @TypeDescriptor(value = String.class), parameterDes = "Proposal content"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainchainId"),
            @Parameter(parameterName = "heterogeneousTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Heterogeneous Chain Tradinghash"),
            @Parameter(parameterName = "businessAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "address（account、Node address, etc）"),
            @Parameter(parameterName = "hash", requestType = @TypeDescriptor(value = String.class), parameterDes = "On chain transactionshash"),
            @Parameter(parameterName = "voteRangeType", requestType = @TypeDescriptor(value = byte.class), parameterDes = "Voting scope type"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "Remarks"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
    })
    )
    public Response proposal(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("type"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("content"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            String password = (String) params.get("password");
            String address = (String) params.get("address");
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
            }
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
            signAccountDTO.setPassword(password);
            proposalTxDTO.setSignAccountDTO(signAccountDTO);
            Transaction tx = assembleTxService.createProposalTx(chain, proposalTxDTO);
            Map<String, String> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("value", tx.getHash().toHex());
            map.put("hex", RPCUtil.encode(tx.serialize()));
            try {
                chain.getLogger().info("proposaltx hex: {}", RPCUtil.encode(tx.serialize()));
            } catch (Exception e) {
                chain.getLogger().warn("Log call failed[0]: {}", e.getMessage());
            }
            try {
                chain.getLogger().info("proposaltx format: {}", tx.format(ProposalTxData.class));
            } catch (Exception e) {
                chain.getLogger().warn("Log call failed[1]: {}", e.getMessage());
            }
            try {
                chain.getLogger().info("Proposal parameters: {}", JSONUtils.obj2PrettyJson(proposalTxDTO));
            } catch (Exception e) {
                chain.getLogger().warn("Log call failed[2]: {}", e.getMessage());
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

    @CmdAnnotation(cmd = ConverterCmdConstant.RESET_VIRTUAL_BANK, version = 1.0, description = "Reset Virtual Bank Heterogeneous Chain(contract)")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainid"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
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
            String password = (String) params.get("password");
            String address = (String) params.get("address");
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
            }
            int heterogeneousChainId = ((Integer) params.get("heterogeneousChainId")).byteValue();
            SignAccountDTO signAccountDTO = new SignAccountDTO();
            signAccountDTO.setAddress((String) params.get("address"));
            signAccountDTO.setPassword((String) params.get("password"));
            if (!chain.isSeedVirtualBankBySignAddr(signAccountDTO.getAddress())) {
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


    @CmdAnnotation(cmd = ConverterCmdConstant.VOTE_PROPOSAL, version = 1.0, description = "Voting proposal")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "proposalTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Proposal transactionhash"),
            @Parameter(parameterName = "choice", requestType = @TypeDescriptor(value = byte.class), parameterDes = "vote"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "Remarks"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "Signature address"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "password")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
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
            String password = (String) params.get("password");
            String address = (String) params.get("address");
            if (!this.accountConfig.vaildPassword(address, password)) {
                throw new NulsException(ConverterErrorCode.PASSWORD_IS_WRONG);
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


    @CmdAnnotation(cmd = ConverterCmdConstant.DISQUALIFICATION, version = 1.0, description = "Obtain a list of revoked virtual bank qualification node addresses")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "list", valueType = List.class, valueElement = String.class, description = "Node address")
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

    @CmdAnnotation(cmd = ConverterCmdConstant.CHECK_RETRY_PARSE, version = 1.0, description = "Re analyze heterogeneous chain transactions")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainid"),
            @Parameter(parameterName = "heterogeneousTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Heterogeneous Chain Tradinghash")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "Whether successful")
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
                chain.getLogger().error("Current non virtual bank member nodes, Not processedcheckRetryParse");
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

    @CmdAnnotation(cmd = ConverterCmdConstant.CHECK_RETRY_HTG_TX, version = 1.0, description = "Re analyze heterogeneous chain transactions")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainid"),
            @Parameter(parameterName = "heterogeneousTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Heterogeneous Chain Tradinghash")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "Whether successful")
    })
    )
    public Response checkRetryHtgTx(Map params) {
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
                chain.getLogger().error("Current non virtual bank member nodes, Not processedcheckRetryHtgTx");
                map.put("value", false);
            } else {
                heterogeneousService.checkRetryHtgTx(chain, heterogeneousChainId, heterogeneousTxHash);
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

    @CmdAnnotation(cmd = ConverterCmdConstant.RETRY_WITHDRAWAL, version = 1.0, description = "Repositioning heterogeneous chain withdrawal transactionstask, Resend message")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "hash", requestType = @TypeDescriptor(value = int.class), parameterDes = "On chain withdrawal transactionshash")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "Whether successful")
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
                chain.getLogger().error("Current non virtual bank member nodes, Not processedretryWithdrawalMsg");
                map.put("value", false);
            } else {
                String txHash = params.get("hash").toString();
                Transaction tx = TransactionCall.getConfirmedTx(chain, txHash);
                if (null == tx) {
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
                chain.getLogger().info("Repositioning heterogeneous chain withdrawal transactionstask, Resend message, txHash: {}", txHash);
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

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_PROPOSAL_INFO, version = 1.0, description = "Query proposal")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "proposalTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Proposal transactionhash")
    })
    @ResponseData(name = "Return value", description = "return network.nerve.converter.model.po.ProposalPO Serialized string of object")
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

    @CmdAnnotation(cmd = ConverterCmdConstant.CANCEL_HTG_TX, version = 1.0, description = "Cancel heterogeneous chain network transactions issued by virtual banks")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainid"),
            @Parameter(parameterName = "heterogeneousAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "Virtual bank administrator signature account address"),
            @Parameter(parameterName = "nonce", requestType = @TypeDescriptor(value = String.class), parameterDes = "accountnonce"),
            @Parameter(parameterName = "priceGwei", requestType = @TypeDescriptor(value = String.class), parameterDes = "Heterogeneous chainprice(Gwei)")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "Whether successful")
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
                chain.getLogger().error("Current non virtual bank member nodes, Not processedcancelHtgTx");
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

    @CmdAnnotation(cmd = ConverterCmdConstant.GET_RECHARGE_NERVE_HASH, version = 1.0, description = "Transactions transferred across heterogeneous chainshashqueryNERVETransactionhash")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Cross chain transfer transactions of heterogeneous chainshash")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", description = "transactionhash")
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

    @CmdAnnotation(cmd = ConverterCmdConstant.FIND_BY_WITHDRAWAL_TX_HASH, version = 1.0, description = "Based on withdrawal transactionshashObtain confirmation information")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "txHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Withdrawal transactionshash")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "heterogeneousChainId", description = "Heterogeneous chainID"),
            @Key(name = "heterogeneousHeight", description = "Heterogeneous chain transaction block height"),
            @Key(name = "heterogeneousTxHash", description = "Heterogeneous Chain Tradinghash"),
            @Key(name = "confirmWithdrawalTxHash", description = "NERVEConfirm transactionhash")
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

    @CmdAnnotation(cmd = ConverterCmdConstant.RETRY_VIRTUAL_BANK, version = 1.0, description = "Retrying Virtual Bank Heterogeneous Chain(contract)")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "hash", requestType = @TypeDescriptor(value = String.class), parameterDes = "Virtual Bank Change Transactionhash"),
            @Parameter(parameterName = "height", requestType = @TypeDescriptor(value = long.class), parameterDes = "Exchange at height"),
            @Parameter(parameterName = "prepare", requestType = @TypeDescriptor(value = int.class), parameterDes = "1 - Preparation phase,2 - Unprepared, execution phase"),

    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "Whether successful")
    })
    )
    public Response retryVirtualBank(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("hash"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("height"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("prepare"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            if (chain.getLatestBasicBlock().getSyncStatusEnum() == SyncStatusEnum.SYNC) {
                throw new NulsException(ConverterErrorCode.PAUSE_NEWTX);
            }
            Map<String, Boolean> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            if (!VirtualBankUtil.isCurrentDirector(chain)) {
                chain.getLogger().error("Current non virtual bank member nodes, Not processedretryVirtualBank");
                map.put("value", false);
                return success(map);
            }
            int prepare = ((Integer) params.get("prepare")).intValue();
            if (prepare != 1 && prepare != 2) {
                throw new NulsException(ConverterErrorCode.DATA_ERROR);
            }
            String hash = params.get("hash").toString();
            Transaction confirmedTx = TransactionCall.getConfirmedTx(chain, hash);
            long height = Long.parseLong(params.get("height").toString());
            BlockHeader blockHeader = BlockCall.getBlockHeader(chain, height);
            this.retryVirtualBankProcessor(chain, confirmedTx, blockHeader, prepare);
            map.put("value", true);
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

    @CmdAnnotation(cmd = ConverterCmdConstant.RECORD_FEE_PAYMENT_NERVE_INNER, version = 1.0, description = "record_fee_payment_nerve_inner")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "chainid"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "Heterogeneous chainid"),
            @Parameter(parameterName = "nerveTxHash", requestType = @TypeDescriptor(value = String.class), parameterDes = "nerve tx hash")
    })
    @ResponseData(name = "Return value", description = "Return aMapobject", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
            @Key(name = "value", valueType = boolean.class, description = "Whether successful")
    })
    )
    public Response recordFeePaymentNerveInner(Map params) {
        Chain chain = null;
        try {
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("heterogeneousChainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("nerveTxHash"), ConverterErrorCode.PARAMETER_ERROR.getMsg());

            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
            int heterogeneousChainId = (Integer) params.get("heterogeneousChainId");
            if (heterogeneousChainId < 200) {
                throw new NulsRuntimeException(ConverterErrorCode.HETEROGENEOUS_CHAINID_ERROR);
            }
            String nerveTxHash = params.get("nerveTxHash").toString();
            Map<String, Boolean> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);

            if (!VirtualBankUtil.isCurrentDirector(chain)) {
                chain.getLogger().error("Current non virtual bank member nodes, Not processed recordFeePaymentNerveInner");
                map.put("value", false);
            } else {
                heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId).getBitCoinApi().recordFeePaymentByNerveInner(nerveTxHash);
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

    private void errorLogProcess(Chain chain, Exception e) {
        if (chain == null) {
            LoggerUtil.LOG.error(e);
        } else {
            chain.getLogger().error(e);
        }
    }

    private void retryVirtualBankProcessor(Chain chain, Transaction confirmedTx, BlockHeader blockHeader, int prepare) throws NulsException {
        List<AgentBasic> listAgent = ConsensusCall.getAgentList(chain, blockHeader.getHeight());
        Transaction tx = confirmedTx;
        ChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ChangeVirtualBankTxData.class);
        List<byte[]> listOutAgents = txData.getOutAgents();
        List<VirtualBankDirector> listOutDirector = new ArrayList<>();
        if (listOutAgents != null && !listOutAgents.isEmpty()) {
            for (byte[] addressBytes : listOutAgents) {
                String agentAddress0 = AddressTool.getStringAddressByBytes(addressBytes);
                AgentBasic agentInfo0 = getAgentInfo0(listAgent, agentAddress0);
                if (null == agentInfo0) {
                    throw new NulsException(ConverterErrorCode.AGENT_INFO_NOT_FOUND);
                }
                VirtualBankDirector director = new VirtualBankDirector();
                director.setAgentHash(agentInfo0.getAgentHash());
                director.setAgentAddress(agentAddress0);
                director.setSignAddress(agentInfo0.getPackingAddress());
                director.setRewardAddress(agentInfo0.getRewardAddress());
                director.setSignAddrPubKey(agentInfo0.getPubKey());
                director.setSeedNode(false);
                director.setHeterogeneousAddrMap(new HashMap<>(ConverterConstant.INIT_CAPACITY_8));
                listOutDirector.add(director);
            }
        }

        List<byte[]> listInAgents = txData.getInAgents();
        List<VirtualBankDirector> listInDirector = new ArrayList<>();
        if (listInAgents != null && !listInAgents.isEmpty()) {
            for (byte[] addressBytes : listInAgents) {
                String agentAddress = AddressTool.getStringAddressByBytes(addressBytes);
                AgentBasic agentInfo = getAgentInfo0(listAgent, agentAddress);
                if (null == agentInfo) {
                    throw new NulsException(ConverterErrorCode.AGENT_INFO_NOT_FOUND);
                }
                VirtualBankDirector virtualBankDirector = new VirtualBankDirector();
                virtualBankDirector.setAgentHash(agentInfo.getAgentHash());
                virtualBankDirector.setAgentAddress(agentAddress);
                virtualBankDirector.setSignAddress(agentInfo.getPackingAddress());
                virtualBankDirector.setRewardAddress(agentInfo.getRewardAddress());
                virtualBankDirector.setSignAddrPubKey(agentInfo.getPubKey());
                virtualBankDirector.setSeedNode(false);
                virtualBankDirector.setHeterogeneousAddrMap(new HashMap<>(ConverterConstant.INIT_CAPACITY_8));
                listInDirector.add(virtualBankDirector);
            }
        }


        // Insert heterogeneous chain processing mechanism
        TxSubsequentProcessPO pendingPO = new TxSubsequentProcessPO();
        pendingPO.setTx(tx);
        pendingPO.setListInDirector(listInDirector);
        pendingPO.setListOutDirector(listOutDirector);
        pendingPO.setBlockHeader(blockHeader);
        pendingPO.setSyncStatusEnum(SyncStatusEnum.RUNNING);
        pendingPO.setCurrentJoin(false);
        pendingPO.setCurrentQuit(false);
        pendingPO.setCurrentQuitDirector(null);
        pendingPO.setCurrentDirector(true);
        pendingPO.setCurrenVirtualBankTotal(chain.getMapVirtualBank().size());
        pendingPO.setRetry(true);
        pendingPO.setPrepare(prepare);
        txSubsequentProcessStorageService.save(chain, pendingPO);
        chain.getPendingTxQueue().offer(pendingPO);
    }

    private AgentBasic getAgentInfo0(List<AgentBasic> listCurrentAgent, String agentAddress) {
        for (int i = 0; i < listCurrentAgent.size(); i++) {
            AgentBasic agentBasic = listCurrentAgent.get(i);
            if (agentBasic.getAgentAddress().equals(agentAddress)) {
                return agentBasic;
            }
        }
        return null;
    }
}

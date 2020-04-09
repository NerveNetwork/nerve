/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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

package nerve.network.converter.rpc.cmd;

import com.fasterxml.jackson.databind.DeserializationFeature;
import io.nuls.base.RPCUtil;
import io.nuls.base.data.Transaction;
import nerve.network.converter.constant.ConverterCmdConstant;
import nerve.network.converter.constant.ConverterConstant;
import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.core.business.AssembleTxService;
import nerve.network.converter.manager.ChainManager;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.bo.VirtualBankDirector;
import nerve.network.converter.model.dto.SignAccountDTO;
import nerve.network.converter.model.dto.VirtualBankDirectorDTO;
import nerve.network.converter.model.dto.WithdrawalTxDTO;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.model.ObjectUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.*;
import io.nuls.core.rpc.model.message.Response;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nerve.network.converter.utils.LoggerUtil.LOG;

/**
 * @author: Chino
 * @date: 2020/3/17
 */
@Component
public class BusinessCmd extends BaseCmd {

    @Autowired
    private AssembleTxService assembleTxService;
    @Autowired
    private ChainManager chainManager;

    @CmdAnnotation(cmd = ConverterCmdConstant.WITHDRAWAL, version = 1.0, description = "提现")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id"),
            @Parameter(parameterName = "heterogeneousChainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "提现异构链chainId"),
            @Parameter(parameterName = "heterogeneousAssetId", requestType = @TypeDescriptor(value = int.class), parameterDes = "提现异构链资产id"),
            @Parameter(parameterName = "heterogeneousAddress", requestType = @TypeDescriptor(value = String.class), parameterDes = "提现到账地址"),
            @Parameter(parameterName = "amount", requestType = @TypeDescriptor(value = BigInteger.class), parameterDes = "提现到账地址"),
            @Parameter(parameterName = "remark", requestType = @TypeDescriptor(value = String.class), parameterDes = "交易备注"),
            @Parameter(parameterName = "address", requestType = @TypeDescriptor(value = String.class), parameterDes = "支付/签名地址"),
            @Parameter(parameterName = "password", requestType = @TypeDescriptor(value = String.class), parameterDes = "密码")
//            @Parameter(parameterName = "signAccount", requestType = @TypeDescriptor(value = SignAccountDTO.class), parameterDes = "交易支付方数据")
    })
    @ResponseData(name = "返回值", description = "返回一个Map对象", responseType = @TypeDescriptor(value = Map.class, mapKeys = {
                @Key(name = "value", description = "交易hash")
        })
    )
    public Response withdrawal(Map params) {
        Chain chain = null;
        try {
            // check parameters
            if (params == null) {
                LOG.warn("ac_transfer params is null");
                throw new NulsRuntimeException(ConverterErrorCode.NULL_PARAMETER);
            }
            ObjectUtils.canNotEmpty(params.get("chainId"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("address"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            ObjectUtils.canNotEmpty(params.get("password"), ConverterErrorCode.PARAMETER_ERROR.getMsg());
            SignAccountDTO signAccountDTO = new SignAccountDTO();
            signAccountDTO.setAddress((String) params.get("address"));
            signAccountDTO.setPassword((String) params.get("password"));
            // parse params
            JSONUtils.getInstance().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            WithdrawalTxDTO withdrawalTxDTO = JSONUtils.map2pojo(params, WithdrawalTxDTO.class);
            withdrawalTxDTO.setSignAccount(signAccountDTO);
            chain = chainManager.getChain((Integer) params.get("chainId"));
            if (null == chain) {
                throw new NulsRuntimeException(ConverterErrorCode.CHAIN_NOT_EXIST);
            }
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

    @CmdAnnotation(cmd = ConverterCmdConstant.VIRTUAL_BANK_INFO, version = 1.0, description = "获取当前所有虚拟银行成员信息")
    @Parameters(value = {
            @Parameter(parameterName = "chainId", requestType = @TypeDescriptor(value = int.class), parameterDes = "链id")
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
            Map<String, VirtualBankDirector> mapVirtualBank = chain.getMapVirtualBank();
            List<VirtualBankDirectorDTO> list = new ArrayList<>();
            for(VirtualBankDirector director : mapVirtualBank.values()){
                VirtualBankDirectorDTO directorDTO = new VirtualBankDirectorDTO(director);
                list.add(directorDTO);
            }
            Map<String, List<VirtualBankDirectorDTO>> map = new HashMap<>(ConverterConstant.INIT_CAPACITY_2);
            map.put("list", list);
            return success(map);
        }catch (Exception e) {
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

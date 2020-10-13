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

package network.nerve.converter.core.validator;

import io.nuls.base.data.Transaction;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.log.logback.NulsLogger;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.BindHeterogeneousContractMode;
import network.nerve.converter.helper.LedgerAssetRegisterHelper;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.txdata.HeterogeneousContractAssetRegCompleteTxData;
import network.nerve.converter.rpc.call.LedgerCall;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author: Mimi
 * @date: 2020-03-23
 */
@Component
public class HeterogeneousContractAssetRegCompleteVerifier {
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private LedgerAssetRegisterHelper ledgerAssetRegisterHelper;

    public void validate(int chainId, Transaction tx) throws NulsException {
        if (tx == null || tx.getTxData() == null) {
            throw new NulsException(ConverterErrorCode.NULL_PARAMETER);
        }
        Chain chain = chainManager.getChain(chainId);
        NulsLogger logger = chain.getLogger();
        try {
            long s = System.currentTimeMillis();
            // 异构合约资产注册 OR NERVE资产绑定异构合约资产: 新绑定 / 覆盖绑定
            HeterogeneousContractAssetRegCompleteTxData txData = new HeterogeneousContractAssetRegCompleteTxData();
            txData.parse(tx.getTxData(), 0);
            String contractAddress = txData.getContractAddress().toLowerCase();
            ErrorCode bindError = null;
            boolean isBindNew = false, isBingOverride = false;
            try {
                do {
                    byte[] remark = tx.getRemark();
                    if (remark == null) {
                        break;
                    }
                    String strRemark = new String(remark, StandardCharsets.UTF_8);
                    if (!strRemark.contains(":")) {
                        break;
                    }
                    String[] modeSplit = strRemark.split(":");
                    String mode = modeSplit[0];
                    String assetContactInfo = modeSplit[1];
                    if (!assetContactInfo.contains("-")) {
                        break;
                    }
                    String[] asset = assetContactInfo.split("-");
                    int assetChainId = Integer.parseInt(asset[0]);
                    int assetId = Integer.parseInt(asset[1]);
                    Map<String, Object> nerveAsset = LedgerCall.getNerveAsset(chainId, assetChainId, assetId);
                    boolean existNerveAsset = nerveAsset != null;
                    if (!existNerveAsset) {
                        bindError = ConverterErrorCode.ASSET_ID_NOT_EXIST;
                        break;
                    }
                    int nerveAssetDecimals = 0;
                    if (nerveAsset.get("decimalPlace") != null) {
                        nerveAssetDecimals = Integer.parseInt(nerveAsset.get("decimalPlace").toString());
                    }

                    HeterogeneousAssetInfo hAssetInfo = ledgerAssetRegisterHelper.getHeterogeneousAssetInfo(assetChainId, assetId);
                    if (hAssetInfo != null) {
                        bindError = ConverterErrorCode.DUPLICATE_BIND;
                        break;
                    }
                    if (BindHeterogeneousContractMode.NEW.toString().equals(mode)) {
                        if (nerveAssetDecimals != txData.getDecimals()) {
                            bindError = ConverterErrorCode.REG_ASSET_INFO_INCONSISTENCY;
                            break;
                        }
                        isBindNew = true;
                    } else if (BindHeterogeneousContractMode.OVERRIDE.toString().equals(mode)) {
                        if (modeSplit.length != 3) {
                            break;
                        }
                        String oldAssetContactInfo = modeSplit[2];
                        if (!oldAssetContactInfo.contains("-")) {
                            break;
                        }
                        String[] oldAsset = oldAssetContactInfo.split("-");
                        int oldAssetChainId = Integer.parseInt(oldAsset[0]);
                        int oldAssetId = Integer.parseInt(oldAsset[1]);
                        HeterogeneousAssetInfo _hAssetInfo = ledgerAssetRegisterHelper.getHeterogeneousAssetInfo(oldAssetChainId, oldAssetId);
                        if (_hAssetInfo == null) {
                            bindError = ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND;
                            break;
                        }
                        if (!contractAddress.equals(_hAssetInfo.getContractAddress())) {
                            bindError = ConverterErrorCode.HETEROGENEOUS_INFO_NOT_MATCH;
                            break;
                        }
                        if (nerveAssetDecimals != _hAssetInfo.getDecimals()) {
                            bindError = ConverterErrorCode.REG_ASSET_INFO_INCONSISTENCY;
                            break;
                        }
                        isBingOverride = true;
                    }
                } while (false);
            } catch (Exception e) {
                logger.warn("检查绑定信息异常, msg: {}", e.getMessage());
                bindError = ConverterErrorCode.DATA_ERROR;
            }
            if (bindError != null) {
                logger.error(bindError.getMsg());
                throw new NulsException(bindError);
            }
            IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(txData.getChainId());
            HeterogeneousAssetInfo assetInfo = docking.getAssetByContractAddress(contractAddress);
            // 绑定覆盖资产时，合约资产必须存在
            if (isBingOverride) {
                if (assetInfo == null) {
                    logger.error("合约资产不存在");
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND);
                }
                return;
            }

            // 新注册、新绑定，合约资产不能存在
            if(assetInfo != null) {
                logger.error("资产已存在");
                throw new NulsException(ConverterErrorCode.ASSET_EXIST);
            }
            // 资产信息验证
            if (!docking.validateHeterogeneousAssetInfoFromNet(contractAddress, txData.getSymbol(), txData.getDecimals())) {
                logger.error("资产信息不匹配");
                throw new NulsException(ConverterErrorCode.REG_ASSET_INFO_INCONSISTENCY);
            }
            if(logger.isDebugEnabled()) {
                long e = System.currentTimeMillis();
                logger.debug("[异构链合约资产注册信息-validate], 调用异构链[validateHeterogeneousAssetInfoFromNet]时间:{}", e - s);
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            if(e instanceof NulsException) {
                throw (NulsException) e;
            }
            throw new NulsException(ConverterErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

}

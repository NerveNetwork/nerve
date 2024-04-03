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
package network.nerve.converter.helper;

import io.nuls.base.data.Transaction;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.BindHeterogeneousContractMode;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.NerveAssetInfo;
import network.nerve.converter.model.dto.HtgAssetBindDTO;
import network.nerve.converter.rpc.call.LedgerCall;
import network.nerve.converter.storage.HeterogeneousAssetConverterStorageService;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.protostuff.ByteString.EMPTY_STRING;
import static network.nerve.converter.constant.ConverterConstant.IN;
import static network.nerve.converter.constant.ConverterConstant.OUT;

/**
 * @author: mimi
 * @date: 2020-05-29
 */
@Component
public class LedgerAssetRegisterHelper {

    @Autowired
    private ConverterConfig converterConfig;
    @Autowired
    private HeterogeneousAssetConverterStorageService heterogeneousAssetConverterStorageService;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private HeterogeneousAssetHelper heterogeneousAssetHelper;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private ConverterCoreApi converterCoreApi;

    public Boolean crossChainAssetReg(int chainId, int heterogeneousAssetChainId, int heterogeneousAssetId, String assetName, int decimalPlace, String assetSymbol, String assetAddress) throws Exception {
        Integer nerveAssetId = LedgerCall.crossChainAssetReg(chainId, assetName, decimalPlace, assetSymbol, assetAddress);
        HeterogeneousAssetInfo info = new HeterogeneousAssetInfo();
        info.setChainId(heterogeneousAssetChainId);
        info.setAssetId(heterogeneousAssetId);
        info.setSymbol(assetSymbol);
        info.setDecimals(decimalPlace);
        info.setContractAddress(assetAddress);
        heterogeneousAssetConverterStorageService.saveAssetInfo(chainId, nerveAssetId, info);
        return true;
    }

    public Boolean deleteCrossChainAsset(int heterogeneousAssetChainId, int heterogeneousAssetId) throws Exception {
        NerveAssetInfo nerveAssetInfo = heterogeneousAssetConverterStorageService.getNerveAssetInfo(heterogeneousAssetChainId, heterogeneousAssetId);
        LedgerCall.crossChainAssetDelete(nerveAssetInfo.getAssetId());
        heterogeneousAssetConverterStorageService.deleteAssetInfo(heterogeneousAssetChainId, heterogeneousAssetId);
        return true;
    }

    public Boolean htgMainAssetBind(int nerveAssetChainId, int nerveAssetId, int heterogeneousAssetChainId, int heterogeneousAssetId, String assetName, int decimalPlace, String assetSymbol, String assetAddress) throws Exception {
        if (converterCoreApi.isProtocol23()) {
            return this.htgMainAssetBindP23(nerveAssetChainId, nerveAssetId, heterogeneousAssetChainId, heterogeneousAssetId, assetName, decimalPlace, assetSymbol, assetAddress);
        } else {
            return this._htgMainAssetBind(nerveAssetChainId, nerveAssetId, heterogeneousAssetChainId, heterogeneousAssetId, assetName, decimalPlace, assetSymbol, assetAddress);
        }
    }

    public Boolean crossChainAssetRegByExistNerveAsset(int nerveAssetChainId, int nerveAssetId, int heterogeneousAssetChainId, int heterogeneousAssetId, String assetName, int decimalPlace, String assetSymbol, String assetAddress) throws Exception {
        HtgAssetBindDTO bindDTO = LedgerCall.bindHeterogeneousAssetReg(converterConfig.getChainId(), nerveAssetChainId, nerveAssetId);
        HeterogeneousAssetInfo info = new HeterogeneousAssetInfo();
        info.setChainId(heterogeneousAssetChainId);
        info.setAssetId(heterogeneousAssetId);
        info.setSymbol(assetSymbol);
        info.setDecimals(decimalPlace);
        info.setContractAddress(assetAddress);
        // Precision difference
        BigInteger decimalsFromHtg = BigInteger.valueOf(decimalPlace);
        BigInteger decimalsFromNerve = BigInteger.valueOf(bindDTO.getAssetDecimals());
        info.setDecimalsSubtractedToNerve(decimalsFromHtg.subtract(decimalsFromNerve).toString());
        heterogeneousAssetConverterStorageService.saveBindAssetInfo(nerveAssetChainId, nerveAssetId, info);
        return true;
    }



    public Boolean deleteCrossChainAssetByExistNerveAsset(int heterogeneousAssetChainId, int heterogeneousAssetId) throws Exception {
        NerveAssetInfo nerveAssetInfo = heterogeneousAssetConverterStorageService.getNerveAssetInfo(heterogeneousAssetChainId, heterogeneousAssetId);
        // inspectNERVEHow many heterogeneous chains are bound to assets
        List<HeterogeneousAssetInfo> assetInfoList = heterogeneousAssetHelper.getHeterogeneousAssetInfo(nerveAssetInfo.getAssetChainId(), nerveAssetInfo.getAssetId());
        // WhenNERVEAsset bound3Multiple or more heterogeneous chains, without changing the asset type of the ledger, because the current heterogeneous chain has been unboundNERVEAssets are still bound2Multiple or more heterogeneous chains
        if (assetInfoList.size() < 3) {
            LedgerCall.unbindHeterogeneousAssetReg(converterConfig.getChainId(), nerveAssetInfo.getAssetChainId(), nerveAssetInfo.getAssetId());
        }
        heterogeneousAssetConverterStorageService.deleteBindAssetInfo(heterogeneousAssetChainId, heterogeneousAssetId);
        return true;
    }

    public Boolean isBoundHeterogeneousAsset(int heterogeneousAssetChainId, int heterogeneousAssetId) throws Exception {
        return heterogeneousAssetConverterStorageService.isBoundHeterogeneousAsset(heterogeneousAssetChainId, heterogeneousAssetId);
    }

    public void checkMainAssetBind() throws NulsException {
        int chainId = converterConfig.getChainId();
        int assetId = converterConfig.getAssetId();
        List<HeterogeneousAssetInfo> assetInfos = heterogeneousAssetHelper.getHeterogeneousAssetInfo(chainId, assetId);
        if (assetInfos != null && !assetInfos.isEmpty()) {
            for (int i = 0, size = assetInfos.size(); i < size; i++) {
                LedgerCall.bindHeterogeneousAssetReg(chainId, chainId, assetId);
            }
        }
    }

    public NerveAssetInfo getNerveAssetInfo(int heterogeneousChainId, int heterogeneousAssetId) {
        NerveAssetInfo nerveAssetInfo = heterogeneousAssetConverterStorageService.getNerveAssetInfo(heterogeneousChainId, heterogeneousAssetId);
        return nerveAssetInfo;
    }

    public String checkHeterogeneousContractAssetReg(Chain chain, Transaction tx,
                                                     String contractAddress,
                                                     byte decimals,
                                                     String symbol,
                                                     int heterogeneousChainId,
                                                     Set<String> contractAssetRegSet,
                                                     Set<String> bindNewSet,
                                                     Set<String> bindRemoveSet,
                                                     Set<String> bindOverrideSet,
                                                     Set<String> unregisterSet,
                                                     Set<String> pauseSet,
                                                     Set<String> resumeSet,
                                                     Set<String> stableSwapCoinSet,
                                                     boolean validateHeterogeneousAssetInfoFromNet) throws Exception {
        String errorCode;
        NulsLogger logger = chain.getLogger();
        int chainId = chain.getChainId();
        ErrorCode bindError = null;
        boolean isBindNew = false, isBindRemove = false, isBindOverride = false, isUnregister = false, isPause = false, isResume = false, isStablePause = false;
        String bindNewKey, bindRemoveKey, bindOverrideKey, unregisterKey, pauseKey, resumeKey;
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
                HeterogeneousAssetInfo hAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(heterogeneousChainId, assetChainId, assetId);
                if (BindHeterogeneousContractMode.NEW.toString().equals(mode)) {
                    if (hAssetInfo != null && hAssetInfo.getChainId() == heterogeneousChainId) {
                        bindError = ConverterErrorCode.DUPLICATE_BIND;
                        break;
                    }
                    if (nerveAssetDecimals != decimals) {
                        bindError = ConverterErrorCode.REG_ASSET_INFO_INCONSISTENCY;
                        break;
                    }
                    isBindNew = true;
                    bindNewKey = new StringBuilder(heterogeneousChainId).append("_")
                            .append(contractAddress).append("_")
                            .append(assetChainId).append("_")
                            .append(assetId).toString();
                    boolean notExist = bindNewSet.add(bindNewKey);
                    if (!notExist) {
                        logger.error("[Conflict detection for duplicate transactions]Contract asset binding");
                        bindError = ConverterErrorCode.DUPLICATE_REGISTER;
                        break;
                    }
                } else if (BindHeterogeneousContractMode.REMOVE.toString().equals(mode)) {
                    if (hAssetInfo == null) {
                        bindError = ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND;
                        break;
                    }
                    Integer assetType = Integer.parseInt(nerveAsset.get("assetType").toString());
                    if (assetType <= 4) {
                        bindError = ConverterErrorCode.NOT_BIND_ASSET;
                        break;
                    }
                    isBindRemove = true;
                    bindRemoveKey = new StringBuilder(heterogeneousChainId).append("_")
                            .append(assetChainId).append("_")
                            .append(assetId).toString();
                    boolean notExist = bindRemoveSet.add(bindRemoveKey);
                    if (!notExist) {
                        logger.error("[Conflict detection for duplicate transactions]Unbind contract assets");
                        bindError = ConverterErrorCode.DUPLICATE_REGISTER;
                        break;
                    }
                } else if (BindHeterogeneousContractMode.OVERRIDE.toString().equals(mode)) {
                    if (hAssetInfo != null) {
                        bindError = ConverterErrorCode.DUPLICATE_BIND;
                        break;
                    }
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
                    HeterogeneousAssetInfo _hAssetInfo = heterogeneousAssetHelper.getHeterogeneousAssetInfo(heterogeneousChainId, oldAssetChainId, oldAssetId);
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
                    isBindOverride = true;
                    bindOverrideKey = new StringBuilder(heterogeneousChainId).append("_")
                            .append(contractAddress).append("_")
                            .append(assetChainId).append("_")
                            .append(assetId).toString();
                    boolean notExist = bindOverrideSet.add(bindOverrideKey);
                    if (!notExist) {
                        logger.error("[Conflict detection for duplicate transactions]Contract asset coverage binding");
                        bindError = ConverterErrorCode.DUPLICATE_REGISTER;
                        break;
                    }
                } else if (BindHeterogeneousContractMode.UNREGISTER.toString().equals(mode)) {
                    if (hAssetInfo == null) {
                        bindError = ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND;
                        break;
                    }
                    Integer assetType = Integer.parseInt(nerveAsset.get("assetType").toString());
                    if (assetType != 4) {
                        bindError = ConverterErrorCode.HETEROGENEOUS_INFO_NOT_MATCH;
                        break;
                    }
                    isUnregister = true;
                    unregisterKey = new StringBuilder(heterogeneousChainId).append("_")
                            .append(assetChainId).append("_")
                            .append(assetId).toString();
                    boolean notExist = unregisterSet.add(unregisterKey);
                    if (!notExist) {
                        logger.error("[Conflict detection for duplicate transactions]Contract asset deregistration");
                        bindError = ConverterErrorCode.DUPLICATE_REGISTER;
                        break;
                    }
                } else if (BindHeterogeneousContractMode.PAUSE.toString().equals(mode)) {
                    if (hAssetInfo == null) {
                        bindError = ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND;
                        break;
                    }
                    if (modeSplit.length != 3) {
                        bindError = ConverterErrorCode.DATA_PARSE_ERROR;
                        break;
                    }
                    String operationType = modeSplit[2];
                    boolean isIn = IN.equalsIgnoreCase(operationType);
                    if (!isIn) {
                        if (!OUT.equalsIgnoreCase(operationType)) {
                            bindError = ConverterErrorCode.DATA_PARSE_ERROR;
                            break;
                        }
                    }
                    Integer assetType = Integer.parseInt(nerveAsset.get("assetType").toString());
                    if (assetType < 4 || assetType == 10) {
                        bindError = ConverterErrorCode.HETEROGENEOUS_INFO_NOT_MATCH;
                        break;
                    }
                    isPause = true;
                    pauseKey = new StringBuilder(heterogeneousChainId).append("_")
                            .append(assetChainId).append("_")
                            .append(assetId).append("_")
                            .append(operationType).toString();
                    boolean notExist = pauseSet.add(pauseKey);
                    if (!notExist) {
                        logger.error("[Conflict detection for duplicate transactions]Contract asset recharge suspended");
                        bindError = ConverterErrorCode.DUPLICATE_REGISTER;
                        break;
                    }
                } else if (BindHeterogeneousContractMode.RESUME.toString().equals(mode)) {
                    if (hAssetInfo == null) {
                        bindError = ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND;
                        break;
                    }
                    if (modeSplit.length != 3) {
                        bindError = ConverterErrorCode.DATA_PARSE_ERROR;
                        break;
                    }
                    String operationType = modeSplit[2];
                    boolean isIn = IN.equalsIgnoreCase(operationType);
                    if (!isIn) {
                        if (!OUT.equalsIgnoreCase(operationType)) {
                            bindError = ConverterErrorCode.DATA_PARSE_ERROR;
                            break;
                        }
                    }
                    Integer assetType = Integer.parseInt(nerveAsset.get("assetType").toString());
                    if (assetType < 4 || assetType == 10) {
                        bindError = ConverterErrorCode.HETEROGENEOUS_INFO_NOT_MATCH;
                        break;
                    }
                    isResume = true;
                    resumeKey = new StringBuilder(heterogeneousChainId).append("_")
                            .append(assetChainId).append("_")
                            .append(assetId).append("_")
                            .append(operationType).toString();
                    boolean notExist = resumeSet.add(resumeKey);
                    if (!notExist) {
                        logger.error("[Conflict detection for duplicate transactions]Contract asset recharge recovery");
                        bindError = ConverterErrorCode.DUPLICATE_REGISTER;
                        break;
                    }
                } else if (BindHeterogeneousContractMode.STABLE_SWAP_COIN_PAUSE.toString().equals(mode)) {
                    if (hAssetInfo == null) {
                        bindError = ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND;
                        break;
                    }
                    if (asset.length != 4) {
                        bindError = ConverterErrorCode.DATA_PARSE_ERROR;
                        break;
                    }
                    isStablePause = true;
                    String stableAddress = asset[2];
                    String status = asset[3];
                    if (converterCoreApi.isProtocol34()) {
                        if (!"pause".equalsIgnoreCase(status) && !"resume".equalsIgnoreCase(status)) {
                            bindError = ConverterErrorCode.DATA_PARSE_ERROR;
                            break;
                        }
                    } else {
                        if (!"pause".equalsIgnoreCase(status) && "resume".equalsIgnoreCase(status)) {
                            bindError = ConverterErrorCode.DATA_PARSE_ERROR;
                            break;
                        }
                    }
                    pauseKey = new StringBuilder(heterogeneousChainId).append("_")
                            .append(assetChainId).append("_")
                            .append(assetId).append("_")
                            .append(stableAddress).append("_")
                            .append(status).toString();
                    boolean notExist = stableSwapCoinSet.add(pauseKey);
                    if (!notExist) {
                        logger.error("[Conflict Detection Duplicate Transaction] Multi-link by asset transaction suspended");
                        bindError = ConverterErrorCode.DUPLICATE_REGISTER;
                        break;
                    }
                }
            } while (false);
        } catch (Exception e) {
            logger.warn("Check for abnormal binding information, msg: {}", e.getMessage());
            bindError = ConverterErrorCode.DATA_ERROR;
        }
        if (bindError != null) {
            errorCode = bindError.getCode();
            logger.error(bindError.getMsg());
            return errorCode;
        }
        if (isBindRemove) {
            return EMPTY_STRING;
        }
        IHeterogeneousChainDocking docking = heterogeneousDockingManager.getHeterogeneousDocking(heterogeneousChainId);
        HeterogeneousAssetInfo assetInfo;
        if (StringUtils.isNotBlank(contractAddress)) {
            assetInfo = docking.getAssetByContractAddress(contractAddress);
        } else {
            assetInfo = docking.getMainAsset();
        }
        // Bind coverage assets OR Unregistration of heterogeneous chain assets, contract assets must exist
        if (isBindOverride || isUnregister || isPause || isResume || isStablePause) {
            if (assetInfo == null) {
                logger.error("Contract assets do not exist");
                ErrorCode error = ConverterErrorCode.HETEROGENEOUS_ASSET_NOT_FOUND;
                errorCode = error.getCode();
                logger.error(error.getMsg());
                return errorCode;
            }
            return EMPTY_STRING;
        }
        // New registration、New binding, contract assets cannot exist
        if(assetInfo != null) {
            logger.error("Contract asset already exists, details: {}", JSONUtils.obj2json(assetInfo));
            ErrorCode error = ConverterErrorCode.ASSET_EXIST;
            errorCode = error.getCode();
            logger.error(error.getMsg());
            return errorCode;
        }
        // Heterogeneous Contract Asset Registration
        if (!isBindNew && !isBindRemove && !isBindOverride && !isUnregister && !isPause && !isResume && !isStablePause) {
            String key = heterogeneousChainId + "_" + contractAddress;
            boolean notExist = contractAssetRegSet.add(key);
            if (!notExist) {
                logger.error("[Conflict detection for duplicate transactions]Contract asset registration, details: {} - {} - {} - {}, hash: {}", heterogeneousChainId, contractAddress, decimals, symbol, tx.getHash().toHex());
                ErrorCode error = ConverterErrorCode.DUPLICATE_REGISTER;
                errorCode = error.getCode();
                logger.error(error.getMsg());
                return errorCode;
            }
        }
        // Asset information verification
        if (validateHeterogeneousAssetInfoFromNet && !docking.validateHeterogeneousAssetInfoFromNet(contractAddress, symbol, decimals)) {
            logger.error("Asset information mismatch");
            ErrorCode error = ConverterErrorCode.REG_ASSET_INFO_INCONSISTENCY;
            errorCode = error.getCode();
            logger.error(error.getMsg());
            return errorCode;
        }
        return EMPTY_STRING;
    }


    public void pauseIn(Integer hChainId, int assetId) throws Exception {
        heterogeneousAssetConverterStorageService.pauseInAssetInfo(hChainId, assetId);
    }

    public void resumeIn(Integer hChainId, int assetId) throws Exception {
        heterogeneousAssetConverterStorageService.resumeInAssetInfo(hChainId, assetId);
    }

    public boolean isPauseInHeterogeneousAsset(Integer hChainId, int assetId) throws Exception {
        return heterogeneousAssetConverterStorageService.isPauseInHeterogeneousAsset(hChainId, assetId);
    }

    public void pauseOut(Integer hChainId, int assetId) throws Exception {
        heterogeneousAssetConverterStorageService.pauseOutAssetInfo(hChainId, assetId);
    }

    public void resumeOut(Integer hChainId, int assetId) throws Exception {
        heterogeneousAssetConverterStorageService.resumeOutAssetInfo(hChainId, assetId);
    }

    public boolean isPauseOutHeterogeneousAsset(Integer hChainId, int assetId) throws Exception {
        return heterogeneousAssetConverterStorageService.isPauseOutHeterogeneousAsset(hChainId, assetId);
    }

    private Boolean _htgMainAssetBind(int nerveAssetChainId, int nerveAssetId, int heterogeneousAssetChainId, int heterogeneousAssetId, String assetName, int decimalPlace, String assetSymbol, String assetAddress) throws Exception {
        HtgAssetBindDTO bindDTO = LedgerCall.bindHeterogeneousAssetReg(converterConfig.getChainId(), nerveAssetChainId, nerveAssetId);
        HeterogeneousAssetInfo info = new HeterogeneousAssetInfo();
        info.setChainId(heterogeneousAssetChainId);
        info.setAssetId(heterogeneousAssetId);
        info.setSymbol(assetSymbol);
        info.setDecimals(decimalPlace);
        info.setContractAddress(assetAddress);
        // Precision difference
        BigInteger decimalsFromHtg = BigInteger.valueOf(decimalPlace);
        BigInteger decimalsFromNerve = BigInteger.valueOf(bindDTO.getAssetDecimals());
        info.setDecimalsSubtractedToNerve(decimalsFromHtg.subtract(decimalsFromNerve).toString());
        heterogeneousAssetConverterStorageService.saveBindAssetInfo(nerveAssetChainId, nerveAssetId, info);
        heterogeneousAssetConverterStorageService.saveAssetInfo(converterConfig.getChainId(), nerveAssetId, info);
        checkLogger(heterogeneousAssetChainId, heterogeneousAssetId);
        return true;
    }

    private Boolean htgMainAssetBindP23(int nerveAssetChainId, int nerveAssetId, int heterogeneousAssetChainId, int heterogeneousAssetId, String assetName, int decimalPlace, String assetSymbol, String assetAddress) throws Exception {
        HtgAssetBindDTO bindDTO = LedgerCall.bindHeterogeneousAssetReg(converterConfig.getChainId(), nerveAssetChainId, nerveAssetId);
        HeterogeneousAssetInfo info = new HeterogeneousAssetInfo();
        info.setChainId(heterogeneousAssetChainId);
        info.setAssetId(heterogeneousAssetId);
        info.setSymbol(assetSymbol);
        info.setDecimals(decimalPlace);
        info.setContractAddress(assetAddress);
        // Precision difference
        BigInteger decimalsFromHtg = BigInteger.valueOf(decimalPlace);
        BigInteger decimalsFromNerve = BigInteger.valueOf(bindDTO.getAssetDecimals());
        info.setDecimalsSubtractedToNerve(decimalsFromHtg.subtract(decimalsFromNerve).toString());
        if (heterogeneousAssetChainId == 119 && heterogeneousAssetId == 1 && converterConfig.getChainId() == 9) {
            NerveAssetInfo assetInfo = converterCoreApi.getHtgMainAsset(119);
            if (assetInfo != null && !assetInfo.isEmpty() && (assetInfo.getAssetChainId() != 1 || assetInfo.getAssetId() != 1)) {
                heterogeneousAssetConverterStorageService.deleteAssetInfo(119, 1);
                converterCoreApi.clearHtgMainAssetMap();
            }
        }
        heterogeneousAssetConverterStorageService.saveBindAssetInfo(nerveAssetChainId, nerveAssetId, info);
        checkLogger(heterogeneousAssetChainId, heterogeneousAssetId);
        return true;
    }

    private void checkLogger(int heterogeneousAssetChainId, int heterogeneousAssetId) {
        if (heterogeneousAssetChainId == 119 && heterogeneousAssetId == 1) {
            Chain chain = chainManager.getChain(converterConfig.getChainId());
            NulsLogger logger = chain.getLogger();
            logger.info("EnulsAsset registration inspection-->");
            HeterogeneousAssetInfo assetInfo = heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(119, 9, 1);
            logger.info("119,9,1 assetInfo not exist--> {}", assetInfo == null);
            List<HeterogeneousAssetInfo> infoList = heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(9, 1);
            logger.info("9,1 bind enuls assetInfo not exist--> {}", infoList.stream().filter(i -> i.getChainId() == 119).collect(Collectors.toList()).size() == 0);

            HeterogeneousAssetInfo assetInfo1 = heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(119, 1, 1);
            logger.info("119,1,1 assetInfo exist--> {}", assetInfo1 != null);
            List<HeterogeneousAssetInfo> infoList1 = heterogeneousAssetConverterStorageService.getHeterogeneousAssetInfo(1, 1);
            logger.info("1,1 bind enuls assetInfo exist--> {}", infoList1.stream().filter(i -> i.getChainId() == 119).collect(Collectors.toList()).size() != 0);
            NerveAssetInfo nerveAssetInfo = heterogeneousAssetConverterStorageService.getNerveAssetInfo(119, 1);
            logger.info("119,1 enuls assetInfo is --> {}-{}", nerveAssetInfo.getAssetChainId(), nerveAssetInfo.getAssetId());
            NerveAssetInfo cacheAssetInfo = converterCoreApi.getHtgMainAsset(119);
            logger.info("119 enuls cache assetInfo is --> {}-{}", cacheAssetInfo.getAssetChainId(), cacheAssetInfo.getAssetId());
        }
    }

}

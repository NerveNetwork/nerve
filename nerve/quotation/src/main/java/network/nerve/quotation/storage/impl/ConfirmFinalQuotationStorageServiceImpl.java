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

package network.nerve.quotation.storage.impl;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.quotation.constant.QuotationConstant;
import network.nerve.quotation.constant.QuotationErrorCode;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.po.ConfirmFinalQuotationPO;
import network.nerve.quotation.storage.ConfirmFinalQuotationStorageService;
import network.nerve.quotation.util.CommonUtil;

import java.io.IOException;

import static network.nerve.quotation.util.LoggerUtil.LOG;

/**
 * @author: Loki
 * @date: 2020-02-19
 */
@Component
public class ConfirmFinalQuotationStorageServiceImpl implements ConfirmFinalQuotationStorageService {
    @Override
    public boolean saveCfrFinalQuotation(Chain chain, String key, ConfirmFinalQuotationPO cfrFinalQuotationPO) {
        if (StringUtils.isBlank(key) || null == cfrFinalQuotationPO) {
            throw new NulsRuntimeException(QuotationErrorCode.PARAMETER_ERROR);
        }
        try {
            return RocksDBService.put(QuotationConstant.DB_CONFIRM_FINAL_QUOTATION_PREFIX + chain.getChainId(), StringUtils.bytes(key), cfrFinalQuotationPO.serialize());
        } catch (IOException e){
            LOG.error(e);
            throw new NulsRuntimeException(QuotationErrorCode.DESERIALIZE_ERROR);
        } catch (Exception e) {
            LOG.error(e);
            throw new NulsRuntimeException(QuotationErrorCode.DB_SAVE_BATCH_ERROR);
        }
    }

    @Override
    public ConfirmFinalQuotationPO getCfrFinalQuotation(Chain chain, String key) {
        if (StringUtils.isBlank(key)) {
            throw new NulsRuntimeException(QuotationErrorCode.PARAMETER_ERROR);
        }
        byte[] bytes = RocksDBService.get(QuotationConstant.DB_CONFIRM_FINAL_QUOTATION_PREFIX + chain.getChainId(), StringUtils.bytes(key));
        ConfirmFinalQuotationPO cfrFinalQuotationPO = null;
        if(null != bytes){
            try {
                cfrFinalQuotationPO = CommonUtil.getInstance(bytes, ConfirmFinalQuotationPO.class);
            } catch (NulsException e) {
                LOG.error(e);
                return null;
            }
        }
        return cfrFinalQuotationPO;
    }

    @Override
    public boolean deleteCfrFinalQuotationByKey(Chain chain, String key) {
        if (StringUtils.isBlank(key)) {
            throw new NulsRuntimeException(QuotationErrorCode.PARAMETER_ERROR);
        }
        boolean result = false;
        try {
            result = RocksDBService.delete(QuotationConstant.DB_CONFIRM_FINAL_QUOTATION_PREFIX + chain.getChainId(), StringUtils.bytes(key));
        } catch (Exception e) {
            LOG.error(e);
        }
        return result;
    }


    @Override
    public boolean saveCfrFinalLastQuotation(Chain chain, String key, ConfirmFinalQuotationPO cfrFinalQuotationPO) {
        if (StringUtils.isBlank(key) || null == cfrFinalQuotationPO) {
            throw new NulsRuntimeException(QuotationErrorCode.PARAMETER_ERROR);
        }
        try {
            return RocksDBService.put(QuotationConstant.DB_CONFIRM_LAST_FINAL_QUOTATION_PREFIX + chain.getChainId(), StringUtils.bytes(key), cfrFinalQuotationPO.serialize());
        } catch (IOException e){
            LOG.error(e);
            throw new NulsRuntimeException(QuotationErrorCode.DESERIALIZE_ERROR);
        } catch (Exception e) {
            LOG.error(e);
            throw new NulsRuntimeException(QuotationErrorCode.DB_SAVE_BATCH_ERROR);
        }
    }

    @Override
    public ConfirmFinalQuotationPO getCfrFinalLastTimeQuotation(Chain chain, String key) {
        if (StringUtils.isBlank(key)) {
            throw new NulsRuntimeException(QuotationErrorCode.PARAMETER_ERROR);
        }
        byte[] bytes = RocksDBService.get(QuotationConstant.DB_CONFIRM_LAST_FINAL_QUOTATION_PREFIX + chain.getChainId(), StringUtils.bytes(key));
        ConfirmFinalQuotationPO cfrFinalQuotationPO = null;
        if(null != bytes){
            try {
                cfrFinalQuotationPO = CommonUtil.getInstance(bytes, ConfirmFinalQuotationPO.class);
            } catch (NulsException e) {
                LOG.error(e);
                return null;
            }
        }
        return cfrFinalQuotationPO;
    }

    @Override
    public boolean deleteCfrFinalLastTimeQuotationByKey(Chain chain, String key) {
        if (StringUtils.isBlank(key)) {
            throw new NulsRuntimeException(QuotationErrorCode.PARAMETER_ERROR);
        }
        boolean result = false;
        try {
            result = RocksDBService.delete(QuotationConstant.DB_CONFIRM_LAST_FINAL_QUOTATION_PREFIX + chain.getChainId(), StringUtils.bytes(key));
        } catch (Exception e) {
            LOG.error(e);
        }
        return result;
    }
}

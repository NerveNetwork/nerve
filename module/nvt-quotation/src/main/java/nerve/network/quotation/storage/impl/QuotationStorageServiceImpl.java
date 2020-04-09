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

package nerve.network.quotation.storage.impl;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import nerve.network.quotation.constant.QuotationConstant;
import nerve.network.quotation.constant.QuotationErrorCode;
import nerve.network.quotation.model.bo.Chain;
import nerve.network.quotation.model.po.FinalQuotationPO;
import nerve.network.quotation.model.po.NodeQuotationWrapperPO;
import nerve.network.quotation.storage.QuotationStorageService;
import nerve.network.quotation.util.CommonUtil;

import java.io.IOException;

import static nerve.network.quotation.util.LoggerUtil.LOG;

/**
 * @author: Chino
 * @date: 2020/03/28
 */
@Component
public class QuotationStorageServiceImpl implements QuotationStorageService {

    @Override
    public boolean saveNodeQuotation(Chain chain, String key, NodeQuotationWrapperPO nodeQuotationWrapperPO) {
        if (null == nodeQuotationWrapperPO || StringUtils.isBlank(key)) {
            throw new NulsRuntimeException(QuotationErrorCode.PARAMETER_ERROR);
        }
        try {
            return RocksDBService.put(QuotationConstant.DB_QUOTATION_NODE_PREFIX + chain.getChainId(), StringUtils.bytes(key), nodeQuotationWrapperPO.serialize());
        } catch (IOException e){
            LOG.error(e);
            throw new NulsRuntimeException(QuotationErrorCode.DESERIALIZE_ERROR);
        } catch (Exception e) {
            LOG.error(e);
            throw new NulsRuntimeException(QuotationErrorCode.DB_SAVE_BATCH_ERROR);
        }
    }

    @Override
    public NodeQuotationWrapperPO getNodeQuotationsBykey(Chain chain, String key) {
        if (StringUtils.isBlank(key)) {
            throw new NulsRuntimeException(QuotationErrorCode.PARAMETER_ERROR);
        }
        byte[] bytes = RocksDBService.get(QuotationConstant.DB_QUOTATION_NODE_PREFIX + chain.getChainId(), StringUtils.bytes(key));
        NodeQuotationWrapperPO nodeQuotationWrapperPO = null;
        if(null != bytes){
            try {
                nodeQuotationWrapperPO = CommonUtil.getInstance(bytes, NodeQuotationWrapperPO.class);
            } catch (NulsException e) {
                LOG.error(e);
                return null;
            }
        }
        return nodeQuotationWrapperPO;
    }

    @Override
    public boolean saveFinalQuotation(Chain chain, String key, FinalQuotationPO finalQuotationPO) {
        if (StringUtils.isBlank(key) || null == finalQuotationPO) {
            throw new NulsRuntimeException(QuotationErrorCode.PARAMETER_ERROR);
        }
        try {
            return RocksDBService.put(QuotationConstant.DB_QUOTATION_FINAL_PREFIX + chain.getChainId(), StringUtils.bytes(key), finalQuotationPO.serialize());
        } catch (IOException e){
            LOG.error(e);
            throw new NulsRuntimeException(QuotationErrorCode.DESERIALIZE_ERROR);
        } catch (Exception e) {
            LOG.error(e);
            throw new NulsRuntimeException(QuotationErrorCode.DB_SAVE_BATCH_ERROR);
        }
    }

    @Override
    public FinalQuotationPO getFinalQuotation(Chain chain, String key) {
        if (StringUtils.isBlank(key)) {
            throw new NulsRuntimeException(QuotationErrorCode.PARAMETER_ERROR);
        }
        byte[] bytes = RocksDBService.get(QuotationConstant.DB_QUOTATION_FINAL_PREFIX + chain.getChainId(), StringUtils.bytes(key));
        FinalQuotationPO finalQuotationPO = null;
        if(null != bytes){
            try {
                finalQuotationPO = CommonUtil.getInstance(bytes, FinalQuotationPO.class);
            } catch (NulsException e) {
                LOG.error(e);
                return null;
            }
        }
        return finalQuotationPO;
    }



    @Override
    public boolean saveFinalLastQuotation(Chain chain, String key, FinalQuotationPO finalQuotationPO) {
        if (StringUtils.isBlank(key) || null == finalQuotationPO) {
            throw new NulsRuntimeException(QuotationErrorCode.PARAMETER_ERROR);
        }
        try {
            return RocksDBService.put(QuotationConstant.DB_LAST_QUOTATION_PREFIX + chain.getChainId(), StringUtils.bytes(key), finalQuotationPO.serialize());
        } catch (IOException e){
            LOG.error(e);
            throw new NulsRuntimeException(QuotationErrorCode.DESERIALIZE_ERROR);
        } catch (Exception e) {
            LOG.error(e);
            throw new NulsRuntimeException(QuotationErrorCode.DB_SAVE_BATCH_ERROR);
        }
    }

    @Override
    public FinalQuotationPO getFinalLastTimeQuotation(Chain chain, String key) {
        if (StringUtils.isBlank(key)) {
            throw new NulsRuntimeException(QuotationErrorCode.PARAMETER_ERROR);
        }
        byte[] bytes = RocksDBService.get(QuotationConstant.DB_LAST_QUOTATION_PREFIX + chain.getChainId(), StringUtils.bytes(key));
        FinalQuotationPO finalQuotationPO = null;
        if(null != bytes){
            try {
                finalQuotationPO = CommonUtil.getInstance(bytes, FinalQuotationPO.class);
            } catch (NulsException e) {
                LOG.error(e);
                return null;
            }
        }
        return finalQuotationPO;
    }
}

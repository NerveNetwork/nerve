/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.quotation.constant.QuotationConstant;
import network.nerve.quotation.constant.QuotationErrorCode;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.storage.QuotationIntradayStorageService;

import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static network.nerve.quotation.util.LoggerUtil.LOG;

/**
 * @author: Loki
 * @date: 2020/6/17
 */
@Component
public class QuotationIntradayStorageServiceImpl implements QuotationIntradayStorageService {

    @Override
    public boolean save(Chain chain, String key) {
        if (StringUtils.isBlank(key)) {
            throw new NulsRuntimeException(QuotationErrorCode.PARAMETER_ERROR);
        }
        try {
            return RocksDBService.put(QuotationConstant.DB_INTRADAY_QUOTATION_NODE_PREFIX + chain.getChainId(), StringUtils.bytes(key), StringUtils.bytes(key));
        }  catch (Exception e) {
            LOG.error(e);
            throw new NulsRuntimeException(QuotationErrorCode.DB_SAVE_BATCH_ERROR);
        }
    }

    @Override
    public String get(Chain chain, String key) {
        if (StringUtils.isBlank(key)) {
            throw new NulsRuntimeException(QuotationErrorCode.PARAMETER_ERROR);
        }
        try {
            byte[] bytes = RocksDBService.get(QuotationConstant.DB_INTRADAY_QUOTATION_NODE_PREFIX + chain.getChainId(), StringUtils.bytes(key));
            return null == bytes ? null : key;
        }  catch (Exception e) {
            LOG.error(e);
            throw new NulsRuntimeException(QuotationErrorCode.DB_SAVE_BATCH_ERROR);
        }
    }

    @Override
    public boolean delete(Chain chain, String key) {
        if (StringUtils.isBlank(key)) {
            throw new NulsRuntimeException(QuotationErrorCode.PARAMETER_ERROR);
        }
        try {
            return RocksDBService.delete(QuotationConstant.DB_INTRADAY_QUOTATION_NODE_PREFIX + chain.getChainId(), StringUtils.bytes(key));
        }  catch (Exception e) {
            LOG.error(e);
            throw new NulsRuntimeException(QuotationErrorCode.DB_SAVE_BATCH_ERROR);
        }
    }

    @Override
    public List<String> getAll(Chain chain) {
        try {
            List<byte[]> bytesList = RocksDBService.keyList(QuotationConstant.DB_INTRADAY_QUOTATION_NODE_PREFIX + chain.getChainId());
            List<String> list = new ArrayList<>();
            if(null == bytesList){
                return list;
            }
            for(byte[] token : bytesList){
                list.add(new String(token, UTF_8));
            }
            return list;
        }  catch (Exception e) {
            LOG.error(e);
            throw new NulsRuntimeException(QuotationErrorCode.DB_SAVE_BATCH_ERROR);
        }
    }

    @Override
    public boolean removeAll(Chain chain) {
        List<byte[]> bytesList = RocksDBService.keyList(QuotationConstant.DB_INTRADAY_QUOTATION_NODE_PREFIX + chain.getChainId());
        if(null == bytesList || bytesList.size() == 0){
            return true;
        }
        try {
           return RocksDBService.deleteKeys(QuotationConstant.DB_INTRADAY_QUOTATION_NODE_PREFIX + chain.getChainId(), bytesList);
        }  catch (Exception e) {
            LOG.error(e);
            throw new NulsRuntimeException(QuotationErrorCode.DB_DELETE_ERROR);
        }
    }
}

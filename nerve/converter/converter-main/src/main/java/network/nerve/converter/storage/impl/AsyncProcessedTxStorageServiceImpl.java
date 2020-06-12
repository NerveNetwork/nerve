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

package network.nerve.converter.storage.impl;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.storage.AsyncProcessedTxStorageService;
import network.nerve.converter.utils.ConverterDBUtil;
import org.ethereum.util.ByteUtil;

/**
 * @author: Loki
 * @date: 2020/6/2
 */
@Component
public class AsyncProcessedTxStorageServiceImpl implements AsyncProcessedTxStorageService {

    private static final String PROPOSAL_EXE_PREFIX = "PROPOSAL_EXE_PREFIX_";
    private static final String COMPONENT_CALL_PREFIX = "COMPONENT_CALL_PREFIX_";

    @Override
    public boolean saveProposalExe(Chain chain, String hash, long height) {
        if(StringUtils.isBlank(hash)){
            return false;
        }
        try {
            return RocksDBService.put(ConverterDBConstant.DB_ASYNC_PROCESSED_PREFIX + chain.getChainId(),
                    ConverterDBUtil.stringToBytes(PROPOSAL_EXE_PREFIX + hash), ByteUtil.longToBytes(height));
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public boolean saveComponentCall(Chain chain, String hash, long height) {
        if(StringUtils.isBlank(hash)){
            return false;
        }
        try {
            return RocksDBService.put(ConverterDBConstant.DB_ASYNC_PROCESSED_PREFIX + chain.getChainId(),
                    ConverterDBUtil.stringToBytes(COMPONENT_CALL_PREFIX + hash), ByteUtil.longToBytes(height));
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }


    @Override
    public Long getProposalExe(Chain chain, String hash) {
        byte[] bytes = RocksDBService.get(ConverterDBConstant.DB_ASYNC_PROCESSED_PREFIX + chain.getChainId(), ConverterDBUtil.stringToBytes(PROPOSAL_EXE_PREFIX + hash));
        return null == bytes ? null : ByteUtil.byteArrayToLong(bytes);
    }

    @Override
    public Long getComponentCall(Chain chain, String hash) {
        byte[] bytes = RocksDBService.get(ConverterDBConstant.DB_ASYNC_PROCESSED_PREFIX + chain.getChainId(), ConverterDBUtil.stringToBytes(COMPONENT_CALL_PREFIX + hash));
        return null == bytes ? null : ByteUtil.byteArrayToLong(bytes);
    }

}

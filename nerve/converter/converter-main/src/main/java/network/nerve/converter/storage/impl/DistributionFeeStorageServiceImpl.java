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

import io.nuls.base.data.NulsHash;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.DistributionFeePO;
import network.nerve.converter.storage.DistributionFeeStorageService;
import network.nerve.converter.utils.ConverterDBUtil;

import static network.nerve.converter.constant.ConverterDBConstant.DB_DISTRIBUTION_FEE_PREFIX;
import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Loki
 * @date: 2020/3/20
 */
@Component
public class DistributionFeeStorageServiceImpl implements DistributionFeeStorageService {

    @Override
    public boolean save(Chain chain, DistributionFeePO po) {
        if(null == po){
            return false;
        }
        try {
            return ConverterDBUtil.putModel(DB_DISTRIBUTION_FEE_PREFIX + chain.getChainId(), stringToBytes(po.getBasisTxHash().toHex()), po);
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public DistributionFeePO findByBasisTxHash(Chain chain, NulsHash hash) {
        return ConverterDBUtil.getModel(DB_DISTRIBUTION_FEE_PREFIX + chain.getChainId(),
                stringToBytes(hash.toHex()), DistributionFeePO.class);
    }

    @Override
    public boolean deleteByBasisTxHash(Chain chain, NulsHash hash) {
        if(null == hash || StringUtils.isBlank(hash.toHex())){
            chain.getLogger().error("deleteByBasisTxHash key is null");
            return false;
        }
        try {
            return RocksDBService.delete(DB_DISTRIBUTION_FEE_PREFIX + chain.getChainId(), stringToBytes(hash.toHex()));
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }
}

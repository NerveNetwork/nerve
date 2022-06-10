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

package network.nerve.converter.storage.impl;

import io.nuls.base.data.NulsHash;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.ConfirmWithdrawalPO;
import network.nerve.converter.storage.ConfirmWithdrawalStorageService;
import network.nerve.converter.utils.ConverterDBUtil;

import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Loki
 * @date: 2020-03-06
 */
@Component
public class ConfirmWithdrawalStorageServiceImpl implements ConfirmWithdrawalStorageService {

    @Override
    public boolean save(Chain chain, ConfirmWithdrawalPO po) {
        if(null == po){
            return false;
        }
        try {
            return ConverterDBUtil.putModel(ConverterDBConstant.DB_CONFIRM_WITHDRAWAL_PREFIX + chain.getChainId(), stringToBytes(po.getWithdrawalTxHash().toHex()), po);
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public ConfirmWithdrawalPO findByWithdrawalTxHash(Chain chain, NulsHash hash) {
        return ConverterDBUtil.getModel(ConverterDBConstant.DB_CONFIRM_WITHDRAWAL_PREFIX + chain.getChainId(),
                stringToBytes(hash.toHex()), ConfirmWithdrawalPO.class);
    }

    @Override
    public boolean deleteByWithdrawalTxHash(Chain chain, NulsHash hash) {
        if(null == hash || StringUtils.isBlank(hash.toHex())){
            chain.getLogger().error("deleteWithdrawalTxHash key is null");
            return false;
        }
        try {
            return RocksDBService.delete(ConverterDBConstant.DB_CONFIRM_WITHDRAWAL_PREFIX + chain.getChainId(), stringToBytes(hash.toHex()));
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }
}

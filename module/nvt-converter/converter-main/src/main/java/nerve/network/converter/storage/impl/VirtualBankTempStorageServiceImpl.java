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

package nerve.network.converter.storage.impl;

import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.po.VirtualBankTemporaryChangePO;
import nerve.network.converter.storage.VirtualBankTempStorageService;
import nerve.network.converter.utils.ConverterDBUtil;

import static nerve.network.converter.constant.ConverterDBConstant.*;
import static nerve.network.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Chino
 * @date: 2020-03-13
 */
public class VirtualBankTempStorageServiceImpl implements VirtualBankTempStorageService {

    private final String KEY = "VIRTUAL_BANK_CHANGE_TEMP";
    @Override
    public boolean save(Chain chain, VirtualBankTemporaryChangePO po) {
        if(null == po){
            return false;
        }
        try {
            return ConverterDBUtil.putModel(DB_VIRTUAL_BANK_PREFIX + chain.getChainId(),
                    stringToBytes(KEY), po);
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public VirtualBankTemporaryChangePO get(Chain chain) {
        return ConverterDBUtil.getModel(DB_VIRTUAL_BANK_PREFIX + chain.getChainId(),
                stringToBytes(KEY), VirtualBankTemporaryChangePO.class);
    }

}

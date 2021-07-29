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
package network.nerve.swap.storage.impl;

import io.nuls.base.basic.AddressTool;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.swap.constant.SwapDBConstant;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.po.SwapPairPO;
import network.nerve.swap.storage.SwapPairStorageService;
import network.nerve.swap.utils.SwapDBUtil;

/**
 * @author: PierreLuo
 * @date: 2021/5/7
 */
@Component
public class SwapPairStorageServiceImpl implements SwapPairStorageService {

    private final String baseArea = SwapDBConstant.DB_NAME_SWAP;
    private final String KEY_PREFIX = "PAIR-";
    private final String KEY_PREFIX_LP = "PAIRLP-";

    @Override
    public boolean savePair(byte[] address, SwapPairPO po) throws Exception {
        if (address == null) {
            return false;
        }
        String addressStr = AddressTool.getStringAddressByBytes(address);
        return this.savePair(addressStr, po);
    }

    @Override
    public boolean savePair(String address, SwapPairPO po) throws Exception {
        if (StringUtils.isBlank(address)) {
            return false;
        }

        int chainId = AddressTool.getChainIdByAddress(address);
        NerveToken tokenLP = po.getTokenLP();
        RocksDBService.put(baseArea + chainId, SwapDBUtil.stringToBytes(KEY_PREFIX_LP + tokenLP.str()), AddressTool.getAddress(address));
        return SwapDBUtil.putModel(baseArea + chainId, SwapDBUtil.stringToBytes(KEY_PREFIX + address), po);
    }

    @Override
    public SwapPairPO getPair(byte[] address) {
        if (address == null) {
            return null;
        }
        String addressStr = AddressTool.getStringAddressByBytes(address);
        return this.getPair(addressStr);
    }

    @Override
    public SwapPairPO getPair(String address) {
        if (StringUtils.isBlank(address)) {
            return null;
        }
        int chainId = AddressTool.getChainIdByAddress(address);
        return SwapDBUtil.getModel(baseArea + chainId, SwapDBUtil.stringToBytes(KEY_PREFIX + address), SwapPairPO.class);
    }

    @Override
    public String getPairAddressByTokenLP(int chainId, NerveToken tokenLP) {
        if (tokenLP == null) {
            return null;
        }
        byte[] bytes = RocksDBService.get(baseArea + chainId, SwapDBUtil.stringToBytes(KEY_PREFIX_LP + tokenLP.str()));
        if (bytes == null) {
            return null;
        }
        return AddressTool.getStringAddressByBytes(bytes);
    }

    @Override
    public boolean delelePair(byte[] address) throws Exception {
        if (address == null) {
            return false;
        }
        String addressStr = AddressTool.getStringAddressByBytes(address);
        return this.delelePair(addressStr);
    }

    @Override
    public boolean delelePair(String address) throws Exception {
        if (StringUtils.isBlank(address)) {
            return false;
        }
        int chainId = AddressTool.getChainIdByAddress(address);
        return RocksDBService.delete(baseArea + chainId, SwapDBUtil.stringToBytes(KEY_PREFIX + address));
    }

}

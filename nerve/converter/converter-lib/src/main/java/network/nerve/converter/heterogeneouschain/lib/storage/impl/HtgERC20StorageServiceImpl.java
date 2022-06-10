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

package network.nerve.converter.heterogeneouschain.lib.storage.impl;

import io.nuls.core.exception.NulsException;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.model.HtgERC20Po;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgERC20StorageService;
import network.nerve.converter.model.po.StringSetPo;
import network.nerve.converter.utils.ConverterDBUtil;

import java.util.*;

/**
 * @author: Mimi
 * @date: 2020-02-20
 */
public class HtgERC20StorageServiceImpl implements HtgERC20StorageService {

    private final String baseArea;
    private final String KEY_PREFIX = "ERC20-";
    private final String KEY_ASSETID_PREFIX = "ERC20_ASSETID-";
    private final String KEY_SYMBOL_PREFIX = "ERC20_SYMBOL-";
    private final byte[] MAX_ASSETID_KEY = ConverterDBUtil.stringToBytes("ERC20-MAX_ASSETID");

    private final HtgContext htgContext;
    public HtgERC20StorageServiceImpl(HtgContext htgContext, String baseArea) {
        this.htgContext = htgContext;
        this.baseArea = baseArea;
    }

    @Override
    public int save(HtgERC20Po po) throws Exception {
        if (po == null) {
            return 0;
        }
        if (isExistsByAssetId(po.getAssetId())) {
            htgContext.logger().error("资产ID已存在[{}], 存在的资产详情: {}", po.getAssetId(), this.findByAssetId(po.getAssetId()));
            throw new NulsException(ConverterErrorCode.ASSET_ID_EXIST);
        }
        Map<byte[], byte[]> values = new HashMap<>(8);
        String address = po.getAddress();
        byte[] addressBytes = ConverterDBUtil.stringToBytes(address);
        values.put(ConverterDBUtil.stringToBytes(KEY_PREFIX + address), ConverterDBUtil.getModelSerialize(po));
        values.put(ConverterDBUtil.stringToBytes(KEY_ASSETID_PREFIX + po.getAssetId()), addressBytes);
        StringSetPo setPo = ConverterDBUtil.getModel(baseArea, ConverterDBUtil.stringToBytes(KEY_SYMBOL_PREFIX + po.getSymbol()), StringSetPo.class);
        if (setPo == null) {
            setPo = new StringSetPo();
            Set<String> set = new HashSet<>();
            set.add(address);
            setPo.setCollection(set);
            values.put(ConverterDBUtil.stringToBytes(KEY_SYMBOL_PREFIX + po.getSymbol()), ConverterDBUtil.getModelSerialize(setPo));
        } else {
            Set<String> set = setPo.getCollection();
            if (set.add(address)) {
                values.put(ConverterDBUtil.stringToBytes(KEY_SYMBOL_PREFIX + po.getSymbol()), ConverterDBUtil.getModelSerialize(setPo));
            }
        }
        RocksDBService.batchPut(baseArea, values);
        //ConverterDBUtil.putModel(baseArea, stringToBytes(KEY_PREFIX + po.getAddress()), po);
        //RocksDBService.put(baseArea, stringToBytes(KEY_ASSETID_PREFIX + po.getAssetId()), stringToBytes(po.getAddress()));
        //RocksDBService.put(baseArea, stringToBytes(KEY_SYMBOL_PREFIX + po.getSymbol()), stringToBytes(po.getAddress()));
        return 1;
    }

    @Override
    public HtgERC20Po findByAddress(String address) {
        HtgERC20Po po = ConverterDBUtil.getModel(baseArea, ConverterDBUtil.stringToBytes(KEY_PREFIX + address), HtgERC20Po.class);
        if (po == null) {
            return null;
        }
        po.setAddress(address);
        return po;
    }

    @Override
    public void deleteByAddress(String address) throws Exception {
        HtgERC20Po po = this.findByAddress(address);
        if (po == null) {
            return;
        }
        this.deleteAddressByAssetId(po.getAssetId());
        this.deleteAddressBySymbol(po.getSymbol(), address);
        RocksDBService.delete(baseArea, ConverterDBUtil.stringToBytes(KEY_PREFIX + address));
    }

    @Override
    public boolean isExistsByAddress(String address) {
        byte[] bytes = RocksDBService.get(baseArea, ConverterDBUtil.stringToBytes(KEY_PREFIX + address));
        if (bytes == null) {
            return false;
        }
        return true;
    }

    @Override
    public HtgERC20Po findByAssetId(int assetId) {
        String address = this.findAddressByAssetId(assetId);
        if (StringUtils.isBlank(address)) {
            return null;
        }
        return this.findByAddress(address);
    }

    @Override
    public void deleteByAssetId(int assetId) throws Exception {
        String address = this.findAddressByAssetId(assetId);
        if (StringUtils.isBlank(address)) {
            return;
        }
        this.deleteByAddress(address);
    }

    @Override
    public String findAddressByAssetId(int assetId) {
        byte[] bytes = RocksDBService.get(baseArea, ConverterDBUtil.stringToBytes(KEY_ASSETID_PREFIX + assetId));
        if (bytes == null) {
            return null;
        }
        return ConverterDBUtil.bytesToString(bytes);
    }

    private void deleteAddressByAssetId(int assetId) throws Exception {
        RocksDBService.delete(baseArea, ConverterDBUtil.stringToBytes(KEY_ASSETID_PREFIX + assetId));
    }

    @Override
    public boolean isExistsByAssetId(int assetId) {
        byte[] bytes = RocksDBService.get(baseArea, ConverterDBUtil.stringToBytes(KEY_ASSETID_PREFIX + assetId));
        if (bytes == null) {
            return false;
        }
        return true;
    }

    @Override
    public List<HtgERC20Po> findBySymbol(String symbol) {
        Set<String> addressSet = this.findAddressBySymbol(symbol);
        if (addressSet == null || addressSet.isEmpty()) {
            return null;
        }
        List<HtgERC20Po> erc20PoList = new ArrayList<>(addressSet.size());
        for (String address : addressSet) {
            erc20PoList.add(this.findByAddress(address));
        }
        return erc20PoList;
    }

    private void deleteAddressBySymbol(String symbol, String address) throws Exception {
        StringSetPo setPo = ConverterDBUtil.getModel(baseArea, ConverterDBUtil.stringToBytes(KEY_SYMBOL_PREFIX + symbol), StringSetPo.class);
        if (setPo != null) {
            setPo.getCollection().remove(address);
            ConverterDBUtil.putModel(baseArea, ConverterDBUtil.stringToBytes(KEY_SYMBOL_PREFIX + symbol), setPo);
        }
    }

    @Override
    public Set<String> findAddressBySymbol(String symbol) {
        StringSetPo setPo = ConverterDBUtil.getModel(baseArea, ConverterDBUtil.stringToBytes(KEY_SYMBOL_PREFIX + symbol), StringSetPo.class);
        if (setPo == null) {
            return null;
        }
        return setPo.getCollection();
    }

    @Override
    public boolean isExistsBySymbol(String symbol) {
        byte[] bytes = RocksDBService.get(baseArea, ConverterDBUtil.stringToBytes(KEY_SYMBOL_PREFIX + symbol));
        if (bytes == null) {
            return false;
        }
        return true;
    }

    @Override
    public void saveMaxAssetId(int maxAssetId) throws Exception {
        RocksDBService.put(baseArea, MAX_ASSETID_KEY, ByteUtils.intToBytes(maxAssetId));
    }

    @Override
    public int getMaxAssetId() throws Exception {
        byte[] bytes = RocksDBService.get(baseArea, MAX_ASSETID_KEY);
        if(bytes == null) {
            int maxAssetId = 1;
            saveMaxAssetId(maxAssetId);
            return maxAssetId;
        }
        return ByteUtils.bytesToInt(bytes);
    }

    @Override
    public void increaseAssetId() throws Exception {
        int maxAssetId = this.getMaxAssetId();
        maxAssetId++;
        this.saveMaxAssetId(maxAssetId);
    }
}

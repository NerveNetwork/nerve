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


import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.config.ConverterConfig;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.NerveAssetInfo;
import network.nerve.converter.model.po.IntegerSetPo;
import network.nerve.converter.model.po.StringListPo;
import network.nerve.converter.storage.HeterogeneousAssetConverterStorageService;
import network.nerve.converter.utils.ConverterDBUtil;

import java.util.*;

import static network.nerve.converter.constant.ConverterConstant.FIRST_HETEROGENEOUS_ASSET_CHAIN_ID;
import static network.nerve.converter.constant.ConverterConstant.ZERO_BYTES;
import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;


/**
 * @desription:
 * @author: Mimi
 * @date: 2018/5/24
 */
@Component
public class HeterogeneousAssetConverterStorageServiceImpl implements HeterogeneousAssetConverterStorageService {

    @Autowired
    private ConverterConfig converterConfig;

    private final String baseArea = ConverterDBConstant.DB_HETEROGENEOUS_CHAIN_INFO;
    private final String KEY_PREFIX_NERVE = "HETEROGENEOUS_ASSET_NERVE-";
    private final String KEY_PREFIX_H = "HETEROGENEOUS_ASSET_H-";
    private final String KEY_PREFIX_NERVE_MORE = "HTG_ASSET_NERVE_MORE-";
    private final String KEY_PREFIX_HTG_MORE = "HTG_ASSET_HTG_MORE_";
    private final String KEY_PREFIX_NERVE_HTG_KEY_SET = "HTG_ASSET_NERVE_HTG_KEY_SET-";
    private final String KEY_PREFIX_HTG_BIND = "HTG_ASSET_BIND_";
    private final String CONTACT_LINE = "-";

    @Override
    public int saveAssetInfo(int nerveAssetChainId, int nerveAssetId, HeterogeneousAssetInfo info) throws Exception {
        if (info == null) {
            return 0;
        }
        if (info.getChainId() == FIRST_HETEROGENEOUS_ASSET_CHAIN_ID) {
            this.saveFirstAssetInfo(nerveAssetChainId, nerveAssetId, info);
        } else {
            this.saveMoreAssetInfo(nerveAssetChainId, nerveAssetId, info);
        }
        return 1;
    }

    private int saveFirstAssetInfo(int nerveAssetChainId, int nerveAssetId, HeterogeneousAssetInfo info) throws Exception {
        if (info == null || info.getChainId() != FIRST_HETEROGENEOUS_ASSET_CHAIN_ID) {
            return 0;
        }
        if (converterConfig.getChainId() == nerveAssetChainId) {
            ConverterDBUtil.putModel(baseArea, stringToBytes(KEY_PREFIX_NERVE + nerveAssetId), info);
            RocksDBService.put(baseArea, stringToBytes(KEY_PREFIX_H + info.getChainId() + CONTACT_LINE + info.getAssetId()), ByteUtils.intToBytes(nerveAssetId));
        } else {
            ConverterDBUtil.putModel(baseArea, stringToBytes(KEY_PREFIX_NERVE + nerveAssetChainId + CONTACT_LINE + nerveAssetId), info);
            ConverterDBUtil.putModel(baseArea, stringToBytes(KEY_PREFIX_H + info.getChainId() + CONTACT_LINE + info.getAssetId()), new NerveAssetInfo(nerveAssetChainId, nerveAssetId));
        }
        return 1;
    }

    private int saveMoreAssetInfo(int nerveAssetChainId, int nerveAssetId, HeterogeneousAssetInfo info) throws Exception {
        if (info == null || info.getChainId() == FIRST_HETEROGENEOUS_ASSET_CHAIN_ID) {
            return 0;
        }
        int htgChainId = info.getChainId();
        ConverterDBUtil.putModel(baseArea, stringToBytes(KEY_PREFIX_NERVE_MORE + htgChainId + CONTACT_LINE + nerveAssetChainId + CONTACT_LINE + nerveAssetId), info);
        ConverterDBUtil.putModel(baseArea, stringToBytes(KEY_PREFIX_HTG_MORE + htgChainId + CONTACT_LINE + info.getAssetId()), new NerveAssetInfo(nerveAssetChainId, nerveAssetId));
        // 保存nerve资产的多异构链关系
        byte[] moreKey = stringToBytes(KEY_PREFIX_NERVE_HTG_KEY_SET + nerveAssetChainId + CONTACT_LINE + nerveAssetId);
        IntegerSetPo setPo = ConverterDBUtil.getModel(baseArea, moreKey, IntegerSetPo.class);
        if(setPo == null) {
            setPo = new IntegerSetPo();
            Set<Integer> set = new HashSet<>();
            set.add(htgChainId);
            setPo.setCollection(set);
            ConverterDBUtil.putModel(baseArea, moreKey, setPo);
        } else {
            Set<Integer> set = setPo.getCollection();
            if(!set.contains(htgChainId)) {
                set.add(htgChainId);
                ConverterDBUtil.putModel(baseArea, moreKey, setPo);
            }
        }
        return 1;
    }

    @Override
    public int deleteAssetInfo(int heterogeneousChainId, int heterogeneousAssetId) throws Exception {
        if (heterogeneousChainId == FIRST_HETEROGENEOUS_ASSET_CHAIN_ID) {
            return this.deleteFirstAssetInfo(heterogeneousChainId, heterogeneousAssetId);
        } else {
            return this.deleteMoreAssetInfo(heterogeneousChainId, heterogeneousAssetId);
        }
    }

    public int deleteFirstAssetInfo(int heterogeneousChainId, int heterogeneousAssetId) throws Exception {
        if (heterogeneousChainId != FIRST_HETEROGENEOUS_ASSET_CHAIN_ID) {
            return 0;
        }
        NerveAssetInfo info = this.getNerveAssetInfo(heterogeneousChainId, heterogeneousAssetId);
        if (converterConfig.getChainId() == info.getAssetChainId()) {
            RocksDBService.delete(baseArea, stringToBytes(KEY_PREFIX_NERVE + info.getAssetId()));
        } else {
            RocksDBService.delete(baseArea, stringToBytes(KEY_PREFIX_NERVE + info.getAssetChainId() + CONTACT_LINE + info.getAssetId()));
        }
        RocksDBService.delete(baseArea, stringToBytes(KEY_PREFIX_H + heterogeneousChainId + CONTACT_LINE + heterogeneousAssetId));
        return 1;
    }

    public int deleteMoreAssetInfo(int heterogeneousChainId, int heterogeneousAssetId) throws Exception {
        if (heterogeneousChainId == FIRST_HETEROGENEOUS_ASSET_CHAIN_ID) {
            return 0;
        }
        NerveAssetInfo info = this.getNerveAssetInfo(heterogeneousChainId, heterogeneousAssetId);
        RocksDBService.delete(baseArea, stringToBytes(KEY_PREFIX_NERVE_MORE + heterogeneousChainId + CONTACT_LINE + info.getAssetChainId() + CONTACT_LINE + info.getAssetId()));
        RocksDBService.delete(baseArea, stringToBytes(KEY_PREFIX_HTG_MORE + heterogeneousChainId + CONTACT_LINE + heterogeneousAssetId));
        byte[] moreKey = stringToBytes(KEY_PREFIX_NERVE_HTG_KEY_SET + info.getAssetChainId() + CONTACT_LINE + info.getAssetId());
        IntegerSetPo setPo = ConverterDBUtil.getModel(baseArea, moreKey, IntegerSetPo.class);
        setPo.getCollection().remove(heterogeneousChainId);
        ConverterDBUtil.putModel(baseArea, moreKey, setPo);
        return 1;
    }

    @Override
    public int saveBindAssetInfo(int nerveAssetChainId, int nerveAssetId, HeterogeneousAssetInfo info) throws Exception {
        this.saveAssetInfo(nerveAssetChainId, nerveAssetId, info);
        RocksDBService.put(baseArea, stringToBytes(KEY_PREFIX_HTG_BIND + info.getChainId() + CONTACT_LINE + info.getAssetId()), ZERO_BYTES);
        return 1;
    }

    @Override
    public int deleteBindAssetInfo(int heterogeneousChainId, int heterogeneousAssetId) throws Exception {
        this.deleteAssetInfo(heterogeneousChainId, heterogeneousAssetId);
        RocksDBService.delete(baseArea, stringToBytes(KEY_PREFIX_HTG_BIND + heterogeneousChainId + CONTACT_LINE + heterogeneousAssetId));
        return 1;
    }

    @Override
    public boolean isBoundHeterogeneousAsset(int heterogeneousChainId, int heterogeneousAssetId) throws Exception {
        byte[] bytes = RocksDBService.get(baseArea, stringToBytes(KEY_PREFIX_HTG_BIND + heterogeneousChainId + CONTACT_LINE + heterogeneousAssetId));
        if (bytes == null) {
            return false;
        }
        return true;
    }

    @Override
    public List<HeterogeneousAssetInfo> getHeterogeneousAssetInfo(int nerveAssetChainId, int nerveAssetId) {
        HeterogeneousAssetInfo firstInfo = this.getFirstHeterogeneousAssetInfo(nerveAssetChainId, nerveAssetId);
        List<HeterogeneousAssetInfo> moreInfoList = this.getMoreHeterogeneousAssetInfo(nerveAssetChainId, nerveAssetId);
        if (firstInfo != null) {
            moreInfoList.add(0, firstInfo);
        }
        return moreInfoList;
    }

    private HeterogeneousAssetInfo getFirstHeterogeneousAssetInfo(int nerveAssetChainId, int nerveAssetId) {
        HeterogeneousAssetInfo info;
        if (converterConfig.getChainId() == nerveAssetChainId) {
            info = ConverterDBUtil.getModel(baseArea, stringToBytes(KEY_PREFIX_NERVE + nerveAssetId), HeterogeneousAssetInfo.class);
        } else {
            info = ConverterDBUtil.getModel(baseArea, stringToBytes(KEY_PREFIX_NERVE + nerveAssetChainId + CONTACT_LINE + nerveAssetId), HeterogeneousAssetInfo.class);
        }
        return info;
    }

    private List<HeterogeneousAssetInfo> getMoreHeterogeneousAssetInfo(int nerveAssetChainId, int nerveAssetId) {
        List<HeterogeneousAssetInfo> list = new ArrayList<>();
        byte[] moreKey = stringToBytes(KEY_PREFIX_NERVE_HTG_KEY_SET + nerveAssetChainId + CONTACT_LINE + nerveAssetId);
        IntegerSetPo setPo = ConverterDBUtil.getModel(baseArea, moreKey, IntegerSetPo.class);
        if (setPo == null) {
            return list;
        }
        Set<Integer> set = setPo.getCollection();
        for (Integer htgChainId : set) {
            HeterogeneousAssetInfo htgAssetInfo = ConverterDBUtil.getModel(baseArea, stringToBytes(KEY_PREFIX_NERVE_MORE + htgChainId + CONTACT_LINE + nerveAssetChainId + CONTACT_LINE + nerveAssetId), HeterogeneousAssetInfo.class);
            if (htgAssetInfo != null) {
                list.add(htgAssetInfo);
            }
        }
        return list;
    }

    @Override
    public HeterogeneousAssetInfo getHeterogeneousAssetInfo(int heterogeneousChainId, int nerveAssetChainId, int nerveAssetId) {
        if (heterogeneousChainId == FIRST_HETEROGENEOUS_ASSET_CHAIN_ID) {
            return this.getFirstHeterogeneousAssetInfo(nerveAssetChainId, nerveAssetId);
        } else {
            return ConverterDBUtil.getModel(baseArea, stringToBytes(KEY_PREFIX_NERVE_MORE + heterogeneousChainId + CONTACT_LINE + nerveAssetChainId + CONTACT_LINE + nerveAssetId), HeterogeneousAssetInfo.class);
        }
    }

    @Override
    public NerveAssetInfo getNerveAssetInfo(int heterogeneousChainId, int heterogeneousAssetId) {
        if (heterogeneousChainId == FIRST_HETEROGENEOUS_ASSET_CHAIN_ID) {
            return this.getFirstNerveAssetInfo(heterogeneousChainId, heterogeneousAssetId);
        } else {
            return ConverterDBUtil.getModel(baseArea, stringToBytes(KEY_PREFIX_HTG_MORE + heterogeneousChainId + CONTACT_LINE + heterogeneousAssetId), NerveAssetInfo.class);
        }
    }

    private NerveAssetInfo getFirstNerveAssetInfo(int heterogeneousChainId, int heterogeneousAssetId) {
        if (heterogeneousChainId != FIRST_HETEROGENEOUS_ASSET_CHAIN_ID) {
            return NerveAssetInfo.emptyInstance();
        }
        byte[] bytes = RocksDBService.get(baseArea, stringToBytes(KEY_PREFIX_H + heterogeneousChainId + CONTACT_LINE + heterogeneousAssetId));
        if (bytes == null) {
            return NerveAssetInfo.emptyInstance();
        }
        NerveAssetInfo info;
        if (bytes.length > 4) {
            info = ConverterDBUtil.getModel(bytes, NerveAssetInfo.class);
        } else {
            int assetId = ByteUtils.bytesToInt(bytes);
            info = new NerveAssetInfo(converterConfig.getChainId(), assetId);
        }
        return info;
    }

}

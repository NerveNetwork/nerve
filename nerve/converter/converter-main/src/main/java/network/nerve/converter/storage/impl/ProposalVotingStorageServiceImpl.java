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
import io.nuls.core.exception.NulsException;
import io.nuls.core.rockdb.model.Entry;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.storage.ProposalVotingStorageService;
import network.nerve.converter.utils.ConverterUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Loki
 * @date: 2020/5/13
 */
@Component
public class ProposalVotingStorageServiceImpl implements ProposalVotingStorageService {
    @Override
    public boolean save(Chain chain, ProposalPO po) {
        if (null == po) {
            return false;
        }
        try {
            return RocksDBService.put(ConverterDBConstant.DB_PROPOSAL_VOTING_PREFIX + chain.getChainId(), po.getHash().getBytes(), po.serialize());

        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public ProposalPO find(Chain chain, NulsHash hash) {
        byte[] bytes = RocksDBService.get(ConverterDBConstant.DB_PROPOSAL_VOTING_PREFIX + chain.getChainId(),
                hash.getBytes());
        if (null == bytes || bytes.length == 0) {
            return null;
        }
        ProposalPO po = new ProposalPO();
        try {
            po.parse(bytes, 0);
            po.setHash(hash);
            return po;
        } catch (NulsException e) {
            chain.getLogger().error(e);
        }
        return null;

    }

    @Override
    public boolean delete(Chain chain, NulsHash hash) {
        if (null == hash || hash.isBlank()) {
            chain.getLogger().error("proposalTxHash key is null");
            return false;
        }
        try {
            return RocksDBService.delete(ConverterDBConstant.DB_PROPOSAL_VOTING_PREFIX + chain.getChainId(), stringToBytes(hash.toHex()));
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public Map<NulsHash, ProposalPO> findAll(Chain chain) {
        List<Entry<byte[], byte[]>> listEntry = RocksDBService.entryList(ConverterDBConstant.DB_PROPOSAL_VOTING_PREFIX + chain.getChainId());
        if(null == listEntry){
            return null;
        }
        Map<NulsHash, ProposalPO> map = new HashMap<>();
        try {
            for(Entry<byte[], byte[]> entry : listEntry){
                ProposalPO vbd = ConverterUtil.getInstance(entry.getValue(), ProposalPO.class);
                map.put(vbd.getHash(), vbd);
            }
        } catch (NulsException e) {
            chain.getLogger().error(e);
        }
        return map;
    }
}

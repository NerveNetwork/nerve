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

import io.nuls.base.data.NulsHash;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.po.VoteProposalPo;
import nerve.network.converter.storage.VoteProposalStorageService;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.ArraysTool;
import io.nuls.core.rockdb.service.RocksDBService;

import static nerve.network.converter.constant.ConverterDBConstant.*;

/**
 * @author: Niels
 * @date: 2020-03-06
 */
@Component
public class VoteProposalStorageServiceImpl implements VoteProposalStorageService {

    @Override
    public boolean save(Chain chain, VoteProposalPo po) {
        if (null == po) {
            return false;
        }
        try {
            byte[] key = ArraysTool.concatenate(po.getProposalTxHash().getBytes(), po.getAddress());
            return RocksDBService.put(DB_PROPOSAL_VOTE_PREFIX + chain.getChainId(), key, po.serialize());

        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public VoteProposalPo find(Chain chain, NulsHash proposalTxHash, byte[] address) {
        byte[] bytes = RocksDBService.get(DB_PROPOSAL_VOTE_PREFIX + chain.getChainId(),
                ArraysTool.concatenate(proposalTxHash.getBytes(), address));
        if (null == bytes || bytes.length == 0) {
            return null;
        }
        VoteProposalPo po = new VoteProposalPo();
        try {
            po.parse(bytes, 0);
            po.setProposalTxHash(proposalTxHash);
            po.setAddress(address);
            return po;
        } catch (NulsException e) {
            chain.getLogger().error(e);
        }
        return null;

    }

    @Override
    public boolean delete(Chain chain, NulsHash proposalTxHash, byte[] address) {
        if (null == proposalTxHash || proposalTxHash.isBlank()) {
            chain.getLogger().error("proposalTxHash key is null");
            return false;
        }
        try {
            return RocksDBService.delete(DB_PROPOSAL_VOTE_PREFIX + chain.getChainId(), ArraysTool.concatenate(proposalTxHash.getBytes(), address));
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }
}

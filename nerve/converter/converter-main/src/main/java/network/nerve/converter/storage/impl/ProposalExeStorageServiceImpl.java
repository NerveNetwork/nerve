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

import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.storage.ProposalExeStorageService;

import static network.nerve.converter.utils.ConverterDBUtil.bytesToString;
import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Loki
 * @date: 2020/5/25
 */
@Component
public class ProposalExeStorageServiceImpl implements ProposalExeStorageService {

    @Override
    public boolean save(Chain chain, String proposalHash, String confirmProposalHash) {
        if(StringUtils.isBlank(proposalHash)){
            return false;
        }
        try {
            return RocksDBService.put(ConverterDBConstant.DB_PROPOSAL_EXE + chain.getChainId(), stringToBytes(proposalHash), stringToBytes(confirmProposalHash));
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
        return false;
    }

    @Override
    public String find(Chain chain, String proposalHash) {
        if(StringUtils.isBlank(proposalHash)){
            return null;
        }
        byte[] txHash = RocksDBService.get(ConverterDBConstant.DB_PROPOSAL_EXE + chain.getChainId(), stringToBytes(proposalHash));
        if (null == txHash || txHash.length == 0) {
            return null;
        }
        return bytesToString(txHash);
    }

    @Override
    public boolean delete(Chain chain, String proposalHash) {
        try {
            return RocksDBService.delete(ConverterDBConstant.DB_PROPOSAL_EXE + chain.getChainId(), stringToBytes(proposalHash));
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
        return false;
    }
}

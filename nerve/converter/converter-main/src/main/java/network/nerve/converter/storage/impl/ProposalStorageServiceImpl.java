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
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.storage.ProposalStorageService;

import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Niels
 * @date: 2020-03-06
 */
@Component
public class ProposalStorageServiceImpl implements ProposalStorageService {
    /**
     * Proposal execution businesshash Trading with proposalshashThe relationship between prefix
     */
    private final String EXE_BUSINESS_PREFIX = "exeBusinessPrefix_";



    @Override
    public boolean save(Chain chain, ProposalPO po) {
        if (null == po) {
            return false;
        }
        try {
            return RocksDBService.put(ConverterDBConstant.DB_PROPOSAL_PREFIX + chain.getChainId(), po.getHash().getBytes(), po.serialize());

        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public ProposalPO find(Chain chain, NulsHash hash) {
        byte[] bytes = RocksDBService.get(ConverterDBConstant.DB_PROPOSAL_PREFIX + chain.getChainId(),
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
            return RocksDBService.delete(ConverterDBConstant.DB_PROPOSAL_PREFIX + chain.getChainId(), stringToBytes(hash.toHex()));
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public boolean saveExeBusiness(Chain chain, String exeHash, NulsHash proposalHash){
        if (StringUtils.isBlank(exeHash) || null == proposalHash) {
            return false;
        }
        try {
            return RocksDBService.put(ConverterDBConstant.DB_PROPOSAL_PREFIX + chain.getChainId(),stringToBytes(EXE_BUSINESS_PREFIX + exeHash), proposalHash.getBytes());
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public NulsHash getExeBusiness(Chain chain, String exeHash) {
        byte[] bytes = RocksDBService.get(ConverterDBConstant.DB_PROPOSAL_PREFIX + chain.getChainId(),
                stringToBytes(EXE_BUSINESS_PREFIX + exeHash));
        if(null == bytes || bytes.length == 0){
            return null;
        }
        return new NulsHash(bytes);
    }

    @Override
    public boolean deleteExeBusiness(Chain chain, String exeHash) {
        if (StringUtils.isBlank(exeHash)) {
            chain.getLogger().error("proposalTxHash key is null");
            return false;
        }
        try {
            return RocksDBService.delete(ConverterDBConstant.DB_PROPOSAL_PREFIX + chain.getChainId(), stringToBytes(EXE_BUSINESS_PREFIX + exeHash));
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }
}

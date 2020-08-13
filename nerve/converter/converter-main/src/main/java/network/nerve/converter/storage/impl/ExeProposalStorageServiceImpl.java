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

import io.nuls.core.core.annotation.Component;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.ExeProposalPO;
import network.nerve.converter.model.po.ExeProposalPOKeyListPO;
import network.nerve.converter.storage.ExeProposalStorageService;
import network.nerve.converter.utils.ConverterDBUtil;

import java.util.ArrayList;
import java.util.List;

import static network.nerve.converter.constant.ConverterDBConstant.DB_EXE_PROPOSAL_PENDING_PREFIX;
import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Loki
 * @date: 2020/5/15
 */
@Component
public class ExeProposalStorageServiceImpl implements ExeProposalStorageService {

    private final byte[] EXEPROPOSAL_TX_ALL_KEY = stringToBytes("EXEPROPOSAL_TX_ALL");
    @Override
    public boolean save(Chain chain, ExeProposalPO po) {
        if (po == null) {
            return false;
        }
        boolean result;
        int chainId = chain.getChainId();
        try {
            String txHash = po.getProposalTxHash().toHex();
            result = ConverterDBUtil.putModel(DB_EXE_PROPOSAL_PENDING_PREFIX + chainId, stringToBytes(txHash), po);
            if (result) {
                ExeProposalPOKeyListPO listPO = ConverterDBUtil.getModel(DB_EXE_PROPOSAL_PENDING_PREFIX + chainId,
                        EXEPROPOSAL_TX_ALL_KEY, ExeProposalPOKeyListPO.class);
                if (listPO == null) {
                    listPO = new ExeProposalPOKeyListPO();
                    List<String> list = new ArrayList<>();
                    list.add(txHash);
                    listPO.setListTxHash(list);
                } else {
                    listPO.getListTxHash().add(txHash);
                }
                result = ConverterDBUtil.putModel(DB_EXE_PROPOSAL_PENDING_PREFIX + chainId, EXEPROPOSAL_TX_ALL_KEY, listPO);
            }
            return result;
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }


    @Override
    public ExeProposalPO get(Chain chain, String txHash) {
        return ConverterDBUtil.getModel(DB_EXE_PROPOSAL_PENDING_PREFIX + chain.getChainId(), stringToBytes(txHash), ExeProposalPO.class);
    }

    @Override
    public void delete(Chain chain, String txHash) {
        try {
            int chainId = chain.getChainId();
            RocksDBService.delete(DB_EXE_PROPOSAL_PENDING_PREFIX + chainId, stringToBytes(txHash));
            ExeProposalPOKeyListPO listPO = ConverterDBUtil.getModel(DB_EXE_PROPOSAL_PENDING_PREFIX + chainId,
                    EXEPROPOSAL_TX_ALL_KEY, ExeProposalPOKeyListPO.class);
            listPO.getListTxHash().remove(txHash);

            ConverterDBUtil.putModel(DB_EXE_PROPOSAL_PENDING_PREFIX + chainId, EXEPROPOSAL_TX_ALL_KEY, listPO);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

    @Override
    public List<ExeProposalPO> findAll(Chain chain) {
        ExeProposalPOKeyListPO listPO = ConverterDBUtil.getModel(DB_EXE_PROPOSAL_PENDING_PREFIX + chain.getChainId(),
                EXEPROPOSAL_TX_ALL_KEY, ExeProposalPOKeyListPO.class);
        List<ExeProposalPO> list = new ArrayList<>();
        if(null == listPO || null == listPO.getListTxHash()){
            return list;
        }
        for (String txHash : listPO.getListTxHash()) {
            list.add(this.get(chain, txHash));
        }
        return list;
    }
}

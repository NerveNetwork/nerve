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

package network.nerve.converter.core.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.HeterogeneousConfirmedInfo;
import network.nerve.converter.model.bo.HeterogeneousConfirmedVirtualBank;
import network.nerve.converter.model.po.ConfirmedChangeVirtualBankPO;
import network.nerve.converter.model.po.MergedComponentCallPO;
import network.nerve.converter.model.txdata.ChangeVirtualBankTxData;
import network.nerve.converter.model.txdata.ConfirmedChangeVirtualBankTxData;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.CfmChangeBankStorageService;
import network.nerve.converter.storage.HeterogeneousConfirmedChangeVBStorageService;
import network.nerve.converter.storage.MergeComponentStorageService;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.HeterogeneousUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 确认虚拟银行变更交易业务验证器
 * (创建交易后)
 * @author: Loki
 * @date: 2020/4/15
 */
@Component
public class ConfirmedChangeVirtualBankVerifier {
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private CfmChangeBankStorageService cfmChangeBankStorageService;
    @Autowired
    private HeterogeneousConfirmedChangeVBStorageService heterogeneousConfirmedChangeVBStorageService;
    @Autowired
    private VirtualBankService virtualBankService;
    @Autowired
    private MergeComponentStorageService mergeComponentStorageService;


    public void validate(Chain chain, Transaction tx) throws NulsException {
        byte[] coinData = tx.getCoinData();
        if(coinData != null && coinData.length > 0){
            // coindata存在数据(coinData应该没有数据)
            throw new NulsException(ConverterErrorCode.COINDATA_CANNOT_EXIST);
        }
        ConfirmedChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmedChangeVirtualBankTxData.class);
        String originalHash = txData.getChangeVirtualBankTxHash().toHex();
        ConfirmedChangeVirtualBankPO po = cfmChangeBankStorageService.find(chain, originalHash);
        if (null != po) {
            // 说明确认交易业务重复,该原始交易已经有一个确认交易 已确认
            throw new NulsException(ConverterErrorCode.CFM_IS_DUPLICATION);
        }

        //获取银行变更交易
        Transaction changeVirtualBankTx = TransactionCall.getConfirmedTx(chain, txData.getChangeVirtualBankTxHash());
        if (null == changeVirtualBankTx) {
            // 交易不存在
            throw new NulsException(ConverterErrorCode.CHANGE_VIRTUAL_BANK_TX_NOT_EXIST);
        }

        List<byte[]> agents = txData.getListAgents();
        if (agents.size() != chain.getMapVirtualBank().values().size()) {
            // 虚拟银行列表不一致
            throw new NulsException(ConverterErrorCode.VIRTUAL_BANK_MISMATCH);
        }
        //判断所有地址是否是当前虚拟银行节点
        for (byte[] addrByte : agents) {
            if (!chain.isVirtualBankByAgentAddr(AddressTool.getStringAddressByBytes(addrByte))) {
                // 虚拟银行列表不一致
                throw new NulsException(ConverterErrorCode.AGENT_IS_NOT_VIRTUAL_BANK);
            }
        }

        List<HeterogeneousConfirmedVirtualBank> listConfirmed = txData.getListConfirmed();

        for (HeterogeneousConfirmedVirtualBank confirmed : listConfirmed) {
            if(StringUtils.isBlank(confirmed.getHeterogeneousTxHash())){
                // 说明合并交易后(加入和退出银行出现抵消的情况)没有实际执行异构链的需要, 直接发出确认交易
                // 找出合并的交易, 验证合并是否是抵消情况
                String cvTxhash = changeVirtualBankTx.getHash().toHex();
                String mergedTxKey = mergeComponentStorageService.getMergedTxKeyByMember(chain, cvTxhash);
                if(StringUtils.isBlank(mergedTxKey)){
                    // 不存在合并key
                    chain.getLogger().error("变更交易不存在合并key, 当前hash:{}, 变更交易hash:{}", tx.getHash().toHex(),  cvTxhash);
                    throw new NulsException(ConverterErrorCode.DATA_NOT_FOUND);
                }
                MergedComponentCallPO mergedTx = mergeComponentStorageService.findMergedTx(chain, mergedTxKey);
                List<Transaction> list = new ArrayList<>();
                for(String hash : mergedTx.getListTxHash()){
                    if(hash.equals(cvTxhash)){
                        list.add(changeVirtualBankTx);
                        continue;
                    }
                    Transaction cvBankTx = TransactionCall.getConfirmedTx(chain, hash);
                    if(null == cvBankTx){
                        chain.getLogger().error("银行变更交易合并成员交易不存在, hash:{}, 变更交易hash:{}",tx.getHash().toHex(), hash);
                        throw new NulsException(ConverterErrorCode.DATA_NOT_FOUND);
                    }
                    list.add(cvBankTx);
                }
                boolean rs = validMergedQuits(chain, list);
                if(!rs){
                    chain.getLogger().error("合并没有出现抵消情况, hash:{}",tx.getHash().toHex());
                    throw new NulsException(ConverterErrorCode.DATA_ERROR);
                }

            }else {
                // 正常验证
                IHeterogeneousChainDocking HeterogeneousInterface =
                        heterogeneousDockingManager.getHeterogeneousDocking(confirmed.getHeterogeneousChainId());
                if (null == HeterogeneousInterface) {
                    // 异构链不存在
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST);
                }
                long startTimeHeterogeneousCall = NulsDateUtils.getCurrentTimeMillis();
                HeterogeneousConfirmedInfo info = null;
                try {
                    info = HeterogeneousInterface.getConfirmedTxInfo(confirmed.getHeterogeneousTxHash());
                } catch (Exception e) {
                    chain.getLogger().error(e);
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_INVOK_ERROR);
                }
                chain.getLogger().debug("[validate]虚拟银行变更确认交易, 调用异构链[getChangeVirtualBankConfirmedTxInfo]时间:{}, txhash:{}",
                        NulsDateUtils.getCurrentTimeMillis() - startTimeHeterogeneousCall,
                        tx.getHash().toHex());
                if (null == info) {

                    // 变更交易不存在
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
                }

                if (confirmed.getEffectiveTime() != info.getTxTime()) {
                    // 异构交易生效时间不匹配
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_TIME_MISMATCH);
                }
                if (!confirmed.getHeterogeneousAddress().equals(info.getMultySignAddress())) {
                    // 变更交易数据不匹配
                    throw new NulsException(ConverterErrorCode.VIRTUAL_BANK_MULTIADDRESS_MISMATCH);
                }

                // 验证 异构链签名列表
                List<HeterogeneousAddress> signedList = confirmed.getSignedHeterogeneousAddress();
                if (null == signedList || signedList.isEmpty()) {
                    // 异构链签名列表是空的
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_SIGN_ADDRESS_LIST_EMPTY);
                }
                List<HeterogeneousAddress> heterogeneousSigners = info.getSigners();
                if (!HeterogeneousUtil.listHeterogeneousAddressEquals(signedList, heterogeneousSigners)) {
                    // 异构链签名列表不匹配
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_SIGNER_LIST_MISMATCH);
                }
            }
        }

    }

    /**
     * 验证原始变更交易合并是否会抵消, 从而无需调用异构链交易
     * @param list
     */
    private boolean validMergedQuits(Chain chain, List<Transaction> list) throws NulsException{
        Map<String, Integer> mapAllDirector = new HashMap<>(list.size());
        for(Transaction tx : list) {
            ChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getCoinData(), ChangeVirtualBankTxData.class);
            for(byte[] addressBytes : txData.getInAgents()) {
                String address = AddressTool.getStringAddressByBytes(addressBytes);
                mapAllDirector.compute(address, (k, v) -> {
                    if (null == v) {
                        return 1;
                    } else {
                        return v + 1;
                    }
                });
            }
            for(byte[] addressBytes : txData.getOutAgents()) {
                String address = AddressTool.getStringAddressByBytes(addressBytes);
                mapAllDirector.compute(address, (k, v) -> {
                    if (null == v) {
                        return -1;
                    } else {
                        return v - 1;
                    }
                });
            }
        }
        int inCount = 0;
        int outCount = 0;
        for (Integer count : mapAllDirector.values()) {
            if (count < 0) {
                outCount++;
            } else if (count > 0) {
                inCount++;
            }
        }
        if(inCount == 0 && outCount == 0){
            return true;
        }
        try {
            chain.getLogger().error("[ConfirmedChangeVirtualBankVerifier]合并没有抵消, inSize:{}, outSize:{}, mapAllDirector:{}", inCount, outCount, JSONUtils.obj2json(mapAllDirector));
        } catch (JsonProcessingException e) {
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
        return false;
    }

}

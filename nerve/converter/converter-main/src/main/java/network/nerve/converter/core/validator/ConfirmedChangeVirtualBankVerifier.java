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

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
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
import network.nerve.converter.model.txdata.ConfirmedChangeVirtualBankTxData;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.CfmChangeBankStorageService;
import network.nerve.converter.storage.HeterogeneousConfirmedChangeVBStorageService;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.HeterogeneousUtil;

import java.util.List;

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
            IHeterogeneousChainDocking HeterogeneousInterface =
                    heterogeneousDockingManager.getHeterogeneousDocking(confirmed.getHeterogeneousChainId());
            if (null == HeterogeneousInterface) {
                // 异构链不存在
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST);
            }
            long startTimeHeterogeneousCall = NulsDateUtils.getCurrentTimeMillis();
            HeterogeneousConfirmedInfo info = null;
            try {
                info = HeterogeneousInterface.getChangeVirtualBankConfirmedTxInfo(confirmed.getHeterogeneousTxHash());
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

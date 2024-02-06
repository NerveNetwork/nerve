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
 * Confirm virtual bank change transaction business validator
 * (After creating the transaction)
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
            // coindataExisting data(coinDataThere should be no data available)
            throw new NulsException(ConverterErrorCode.COINDATA_CANNOT_EXIST);
        }
        ConfirmedChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmedChangeVirtualBankTxData.class);
        String originalHash = txData.getChangeVirtualBankTxHash().toHex();
        ConfirmedChangeVirtualBankPO po = cfmChangeBankStorageService.find(chain, originalHash);
        if (null != po) {
            // Explanation: Confirmation of duplicate transaction business,The original transaction already has a confirmed transaction Confirmed
            throw new NulsException(ConverterErrorCode.CFM_IS_DUPLICATION);
        }

        //Obtain bank change transactions
        Transaction changeVirtualBankTx = TransactionCall.getConfirmedTx(chain, txData.getChangeVirtualBankTxHash());
        if (null == changeVirtualBankTx) {
            // Transaction does not exist
            throw new NulsException(ConverterErrorCode.CHANGE_VIRTUAL_BANK_TX_NOT_EXIST);
        }

        List<byte[]> agents = txData.getListAgents();
        if (agents.size() != chain.getMapVirtualBank().values().size()) {
            // Inconsistent virtual bank list
            throw new NulsException(ConverterErrorCode.VIRTUAL_BANK_MISMATCH);
        }
        //Determine if all addresses are the current virtual bank node
        for (byte[] addrByte : agents) {
            if (!chain.isVirtualBankByAgentAddr(AddressTool.getStringAddressByBytes(addrByte))) {
                // Inconsistent virtual bank list
                throw new NulsException(ConverterErrorCode.AGENT_IS_NOT_VIRTUAL_BANK);
            }
        }

        List<HeterogeneousConfirmedVirtualBank> listConfirmed = txData.getListConfirmed();

        for (HeterogeneousConfirmedVirtualBank confirmed : listConfirmed) {
            if(StringUtils.isBlank(confirmed.getHeterogeneousTxHash())){
                // Explanation: After the merger transaction(Offset situation between joining and exiting the bank)There is no actual need to execute heterogeneous chains, Directly send confirmation transaction
                // Identify the merged transactions, Verify if the merger is an offsetting situation
                String cvTxhash = changeVirtualBankTx.getHash().toHex();
                String mergedTxKey = mergeComponentStorageService.getMergedTxKeyByMember(chain, cvTxhash);
                if(StringUtils.isBlank(mergedTxKey)){
                    // No merge existskey
                    chain.getLogger().error("There is no merger in the change transactionkey, currenthash:{}, Change transactionhash:{}", tx.getHash().toHex(),  cvTxhash);
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
                        chain.getLogger().error("Bank change transaction merged member transaction does not exist, hash:{}, Change transactionhash:{}",tx.getHash().toHex(), hash);
                        throw new NulsException(ConverterErrorCode.DATA_NOT_FOUND);
                    }
                    list.add(cvBankTx);
                }
                boolean rs = validMergedQuits(chain, list);
                if(!rs){
                    chain.getLogger().error("There was no offsetting situation in the merger, hash:{}",tx.getHash().toHex());
                    throw new NulsException(ConverterErrorCode.DATA_ERROR);
                }

            }else {
                // Normal verification
                IHeterogeneousChainDocking HeterogeneousInterface =
                        heterogeneousDockingManager.getHeterogeneousDocking(confirmed.getHeterogeneousChainId());
                if (null == HeterogeneousInterface) {
                    // Heterogeneous chain does not exist
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
                chain.getLogger().debug("[validate]Virtual Bank Change Confirmation Transaction, Calling heterogeneous chains[getChangeVirtualBankConfirmedTxInfo]time:{}, txhash:{}",
                        NulsDateUtils.getCurrentTimeMillis() - startTimeHeterogeneousCall,
                        tx.getHash().toHex());
                if (null == info) {

                    // Change transaction does not exist
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
                }

                if (confirmed.getEffectiveTime() != info.getTxTime()) {
                    // Heterogeneous transaction effective time mismatch
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_TIME_MISMATCH);
                }
                if (!confirmed.getHeterogeneousAddress().equals(info.getMultySignAddress())) {
                    // Change transaction data mismatch
                    throw new NulsException(ConverterErrorCode.VIRTUAL_BANK_MULTIADDRESS_MISMATCH);
                }

                // validate Heterogeneous Chain Signature List
                List<HeterogeneousAddress> signedList = confirmed.getSignedHeterogeneousAddress();
                if (null == signedList || signedList.isEmpty()) {
                    // The heterogeneous chain signature list is empty
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_SIGN_ADDRESS_LIST_EMPTY);
                }
                List<HeterogeneousAddress> heterogeneousSigners = info.getSigners();
                if (!HeterogeneousUtil.listHeterogeneousAddressEquals(signedList, heterogeneousSigners)) {
                    // Heterogeneous chain signature list mismatch
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_SIGNER_LIST_MISMATCH);
                }
            }
        }

    }

    /**
     * Verify if the original change transaction merger will offset, So there is no need to call heterogeneous chain transactions
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
            chain.getLogger().error("[ConfirmedChangeVirtualBankVerifier]Merge without offsetting, inSize:{}, outSize:{}, mapAllDirector:{}", inCount, outCount, JSONUtils.obj2json(mapAllDirector));
        } catch (JsonProcessingException e) {
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
        return false;
    }

}

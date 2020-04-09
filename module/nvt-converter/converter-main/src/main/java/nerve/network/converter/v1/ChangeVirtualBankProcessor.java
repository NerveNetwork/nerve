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

package nerve.network.converter.v1;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import nerve.network.converter.config.ConverterContext;
import nerve.network.converter.constant.ConverterConstant;
import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.core.business.VirtualBankService;
import nerve.network.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import nerve.network.converter.manager.ChainManager;
import nerve.network.converter.model.bo.AgentBasic;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.bo.VirtualBankDirector;
import nerve.network.converter.model.dto.SignAccountDTO;
import nerve.network.converter.model.po.TxSubsequentProcessPO;
import nerve.network.converter.model.txdata.ChangeVirtualBankTxData;
import nerve.network.converter.rpc.call.ConsensusCall;
import nerve.network.converter.storage.TxSubsequentProcessStorageService;
import nerve.network.converter.storage.VirtualBankStorageService;
import nerve.network.converter.utils.ConverterSignValidUtil;
import nerve.network.converter.utils.ConverterUtil;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;

import java.util.*;

/**
 * @author: Chino
 * @date: 2020-02-28
 */
@Component("ChangeVirtualBankV1")
public class ChangeVirtualBankProcessor implements TransactionProcessor {

    @Override
    public int getType() {
        return TxType.CHANGE_VIRTUAL_BANK;
    }
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private VirtualBankStorageService virtualBankStorageService;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService;
    @Autowired
    private VirtualBankService virtualBankService;


    /**
     * 新增节点
     * 1.是否是共识节点
     * 2.是否保证金排行在前10
     * 3.不是当前虚拟银行成员
     * <p>
     * 退出节点
     * 1.不是共识节点, 并且是当前虚拟银行成员
     * 2.是共识节点
     * 2-1.判断保证金排不在普通共识节点前10
     * 2-2.是当前虚拟银行成员
     *
     * @param chainId     链Id
     * @param txs         类型为{@link #getType()}的所有交易集合
     * @param txMap       不同交易类型与其对应交易列表键值对
     * @param blockHeader 区块头
     * @return
     */
    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        Chain chain = null;
        Map<String, Object> result = null;
        try {
            chain = chainManager.getChain(chainId);
            NulsLogger log = chain.getLogger();
            String errorCode = null;
            result = new HashMap<>(ConverterConstant.INIT_CAPACITY_4);
            List<Transaction> failsList = new ArrayList<>();
            List<AgentBasic> listAgent = ConsensusCall.getAgentInfo(chain);

            //区块内业务重复交易检查
            Set<String> setDuplicate = new HashSet<>();
            outer:
            for (Transaction tx : txs) {
                // 判断区块内重复
                String txDataHex = HexUtil.encode(tx.getTxData());
                if(setDuplicate.contains(txDataHex)){
                    // 区块内业务重复交易
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.BLOCK_TX_DUPLICATION.getCode();
                    log.error(ConverterErrorCode.BLOCK_TX_DUPLICATION.getMsg());
                    continue;
                }
                ChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ChangeVirtualBankTxData.class);
                // 验证退出虚拟银行业务
                List<byte[]> listOutAgents = txData.getOutAgents();
                int listOutAgentsSize = 0;
                if (null != listOutAgents && !listOutAgents.isEmpty()) {
                    listOutAgentsSize = listOutAgents.size();
                    for (byte[] addressBytes : listOutAgents) {
                        String agentAddress = AddressTool.getStringAddressByBytes(addressBytes);
                        // 判断不是虚拟银行成员
                        if (!chain.isVirtualBankByAgentAddr(agentAddress)) {
                            failsList.add(tx);
                            // 当前不是虚拟银行节点, 不存在退出的情况
                            errorCode = ConverterErrorCode.AGENT_IS_NOT_VIRTUAL_BANK.getCode();
                            log.error(ConverterErrorCode.AGENT_IS_NOT_VIRTUAL_BANK.getMsg());
                            continue outer;
                        }

                        boolean rs = isQuitVirtualBank(chain, listAgent, agentAddress);
                        if (rs) {
                            failsList.add(tx);
                            // 不满足退出虚拟银行节点条件
                            errorCode = ConverterErrorCode.CAN_NOT_QUIT_VIRTUAL_BANK.getCode();
                            log.error(ConverterErrorCode.CAN_NOT_QUIT_VIRTUAL_BANK.getMsg());
                            continue outer;
                        }
                    }
                }

                // 验证加入虚拟银行节点业务
                List<byte[]> listInAgents = txData.getInAgents();
                if (null != listInAgents && !listInAgents.isEmpty()) {
                    //  判断加入后是否导致虚拟银行成员超出
                    int countVirtualBank = chain.getVirtualBankCountWithoutSeedNode();
                    if(countVirtualBank - listOutAgentsSize + listInAgents.size() > ConverterContext.VIRTUAL_BANK_AGENT_NUMBER){
                        failsList.add(tx);
                        // 当前已是造成虚拟银行非种子节点数量超出最大值
                        errorCode = ConverterErrorCode.VIRTUAL_BANK_OVER_MAXIMUM.getCode();
                        log.error(ConverterErrorCode.VIRTUAL_BANK_OVER_MAXIMUM.getMsg());
                        continue outer;
                    }
                    for (byte[] addressBytes : listInAgents) {
                        String agentAddress = AddressTool.getStringAddressByBytes(addressBytes);
                        //判断是虚拟银行成员
                        if (chain.isVirtualBankByAgentAddr(agentAddress)) {
                            failsList.add(tx);
                            // 当前已是虚拟银行节点, 不存在加入的情况
                            errorCode = ConverterErrorCode.AGENT_IS_VIRTUAL_BANK.getCode();
                            log.error(ConverterErrorCode.AGENT_IS_VIRTUAL_BANK.getMsg());
                            continue outer;
                        }
                        boolean rs = isJoinVirtualBank(chain, listAgent, agentAddress);
                        if (!rs) {
                            failsList.add(tx);
                            // 未达到进入虚拟银行的条件
                            errorCode = ConverterErrorCode.CAN_NOT_JOIN_VIRTUAL_BANK.getCode();
                            log.error(ConverterErrorCode.CAN_NOT_JOIN_VIRTUAL_BANK.getMsg());
                            continue outer;
                        }
                    }
                }

                // 验签名
                try {
                    ConverterSignValidUtil.validateSign(chain, tx);
                } catch (NulsException e) {
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    log.error(e.getErrorCode().getMsg());
                    continue outer;
                }
                setDuplicate.add(txDataHex);
            }
            result.put("txList", failsList);
            result.put("errorCode", errorCode);
        } catch (Exception e) {
            chain.getLogger().error(e);
            result.put("txList", txs);
            result.put("errorCode", ConverterErrorCode.SYS_UNKOWN_EXCEPTION.getCode());
        }
        return result;
    }

    /**
     * 判断节点是否应该进入虚拟银行
     *
     * @param listAgent
     * @param agentAddress
     * @return
     */
    private boolean isJoinVirtualBank(Chain chain, List<AgentBasic> listAgent, String agentAddress) {
        for (int i = 0; i < listAgent.size(); i++) {
            AgentBasic agentBasic = listAgent.get(i);
            //判断是否是非种子的出块共识节点
            if (!agentBasic.getSeedNode()
                    && agentBasic.getAgentAddress().equals(agentAddress)) {
                if (StringUtils.isBlank(agentBasic.getPubKey())) {
                    chain.getLogger().error("节点公钥为空, 无出块记录不能加入虚拟银行， agentAddress:{}", agentAddress);
                    return false;
                }
                byte[] pubKeyAddrByte = AddressTool.getAddressByPubKeyStr(agentBasic.getPackingAddress(), chain.getChainId());
                String pubKeyAddr = AddressTool.getStringAddressByBytes(pubKeyAddrByte);
                if (!pubKeyAddr.equals(agentBasic.getPackingAddress())) {
                    //公钥生成的地址与出块地址不一致，表示无效
                    chain.getLogger().error("节点公钥错误，与出块地址不匹配，不能加入虚拟银行， agentAddress:{}", agentAddress);
                    return false;
                }
                //判断排位在虚拟银行规定的排位内
                int currentPosition = i + 1;
                if (currentPosition <= ConverterContext.VIRTUAL_BANK_AGENT_NUMBER) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 退出银行的节点
     * 1.不是共识节点, 并且是当前虚拟银行成员
     * 2.是共识节点
     * 2-1.判断保证金排不在普通共识节点前10
     * 2-2.是当前虚拟银行成员
     *
     * @param listAgent
     * @param agentAddress
     * @return
     */
    private boolean isQuitVirtualBank(Chain chain, List<AgentBasic> listAgent, String agentAddress) {
        for (int i = 0; i < listAgent.size(); i++) {
            AgentBasic agentBasic = listAgent.get(i);
            //判断是出块共识节点
            if (agentBasic.getAgentAddress().equals(agentAddress)) {
                // 公钥为空说明没出块, 应该退出虚拟银行
                if (StringUtils.isBlank(agentBasic.getPubKey())) {
                    return true;
                }
                byte[] pubKeyAddrByte = AddressTool.getAddressByPubKeyStr(agentBasic.getPackingAddress(), chain.getChainId());
                String pubKeyAddr = AddressTool.getStringAddressByBytes(pubKeyAddrByte);
                if (!pubKeyAddr.equals(agentBasic.getPackingAddress())) {
                    //公钥生成的地址与出块地址不一致，表示无效
                    return true;
                }
                if (agentBasic.getSeedNode()) {
                    chain.getLogger().error("种子节点不能退出虚拟银行， agentAddress:{}", agentAddress);
                    return false;
                }
                int currentPosition = i + 0;
                if (currentPosition >= ConverterContext.VIRTUAL_BANK_AGENT_NUMBER) {
                    //排位在虚拟银行规定的排位外,表示应该退出银行
                    return true;
                } else {
                    chain.getLogger().error("当前不能退出虚拟银行， 排行:{}", currentPosition);
                    return false;
                }
            }
        }
        //不是共识节点
        return true;
    }

    private AgentBasic getAgentInfo(List<AgentBasic> listAgent, String agentAddress) {
        for (int i = 0; i < listAgent.size(); i++) {
            AgentBasic agentBasic = listAgent.get(i);
            if (!agentBasic.getAgentAddress().equals(agentAddress)) {
                return agentBasic;
            }
        }
        return null;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        return commit(chainId, txs, blockHeader, syncStatus, true);
    }

    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus, boolean failRollback) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        try {
            long height = failRollback ? blockHeader.getHeight() : 0L;
            List<AgentBasic> listAgent = ConsensusCall.getAgentInfo(chain, height);
            for (Transaction tx : txs) {
                //维护虚拟银行
                ChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ChangeVirtualBankTxData.class);
                List<byte[]> listOutAgents = txData.getOutAgents();
                List<VirtualBankDirector> listOutDirector =
                        processRemoveVirtualBank(chain, listOutAgents, listAgent);
                List<byte[]> listInAgents = txData.getInAgents();
                List<VirtualBankDirector> listInDirector =
                        processSaveVirtualBank(chain, tx, listInAgents, listAgent);

                SignAccountDTO signAccountDTO = virtualBankService.isCurrentDirector(chain);
                if (null != signAccountDTO) {
                    // 放入类似队列处理机制 加回来的节点需要重新添加异构链地址信息
                    TxSubsequentProcessPO pendingPO = new TxSubsequentProcessPO();
                    pendingPO.setTx(tx);
                    pendingPO.setListInDirector(listInDirector);
                    pendingPO.setListOutDirector(listOutDirector);
                    pendingPO.setBlockHeader(blockHeader);
                    pendingPO.setSyncStatusEnum(SyncStatusEnum.getEnum(syncStatus));
                    txSubsequentProcessStorageService.save(chain, pendingPO);
                    chain.getPendingTxQueue().offer(pendingPO);
                    // 异构链组件注册当前节点签名地址
                    virtualBankService.initLocalSignPriKeyToHeterogeneous(chain, signAccountDTO);
                }
                chain.getLogger().debug("[commit]虚拟银行变更交易 hash:{}", tx.getHash().toHex());
            }
            return true;

        } catch (NulsException e) {
            chain.getLogger().error(e);
            if (failRollback) {
                rollback(chainId, txs, blockHeader, false);
            }
            return false;
        }
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return rollback(chainId, txs, blockHeader, true);
    }

    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader, boolean failCommit) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = chainManager.getChain(chainId);
        try {
            long height = blockHeader.getHeight() - 1;
            List<AgentBasic> listAgent = ConsensusCall.getAgentInfo(chain, height);
            for (Transaction tx : txs) {
                //维护虚拟银行
                ChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ChangeVirtualBankTxData.class);
                List<byte[]> listOutAgents = txData.getOutAgents();
                //把退出的虚拟银行的节点加回去
                processSaveVirtualBank(chain, tx, listOutAgents, listAgent);
                //把加入的虚拟银行移除
                List<byte[]> listInAgents = txData.getInAgents();
                processRemoveVirtualBank(chain, listInAgents, listAgent);
                chain.getLogger().debug("[rollback]虚拟银行变更交易 hash:{}", tx.getHash().toHex());
            }
            return true;
        } catch (NulsException e) {
            chain.getLogger().error(e);
            if (failCommit) {
                commit(chainId, txs, blockHeader, 0, false);
            }
            return false;
        }
    }


    /**
     * 加入虚拟银行节点
     * @param chain
     * @param tx
     * @param listAgents
     * @param listCurrentAgent
     * @throws NulsException
     */
    private List<VirtualBankDirector> processSaveVirtualBank(Chain chain, Transaction tx, List<byte[]> listAgents, List<AgentBasic> listCurrentAgent) throws NulsException {
        if (null == listAgents || listAgents.isEmpty()) {
            return null;
        }
        List<VirtualBankDirector> listInDirector = new ArrayList<>();
        for (byte[] addressBytes : listAgents) {
            String agentAddress = AddressTool.getStringAddressByBytes(addressBytes);
            AgentBasic agentInfo = getAgentInfo(listCurrentAgent, agentAddress);
            if (null == agentInfo) {
                throw new NulsException(ConverterErrorCode.AGENT_INFO_NOT_FOUND);
            }
            String packingAddr = agentInfo.getPackingAddress();
            VirtualBankDirector virtualBankDirector = new VirtualBankDirector();
            virtualBankDirector.setAgentAddress(agentAddress);
            virtualBankDirector.setSignAddress(packingAddr);
            virtualBankDirector.setSignAddrPubKey(agentInfo.getPubKey());
            virtualBankDirector.setSeedNode(false);
            virtualBankDirector.setHeterogeneousAddrMap(new HashMap<>(ConverterConstant.INIT_CAPACITY_8));
            chain.getMapVirtualBank().put(packingAddr, virtualBankDirector);
            virtualBankStorageService.save(chain, virtualBankDirector);
            listInDirector.add(virtualBankDirector);
        }
        return listInDirector;
    }

    /**
     * 移除虚拟银行节点
     * @param chain
     * @param listAgents
     * @param listCurrentAgent
     * @throws NulsException
     */
    private List<VirtualBankDirector> processRemoveVirtualBank(Chain chain, List<byte[]> listAgents, List<AgentBasic> listCurrentAgent) throws NulsException {
        if (null == listAgents || listAgents.isEmpty()) {
            return null;
        }
        List<VirtualBankDirector> listOutDirector = new ArrayList<>();
        for (byte[] addressBytes : listAgents) {
            String agentAddress = AddressTool.getStringAddressByBytes(addressBytes);
            AgentBasic agentInfo = getAgentInfo(listCurrentAgent, agentAddress);
            if (null == agentInfo) {
                throw new NulsException(ConverterErrorCode.AGENT_INFO_NOT_FOUND);
            }
            String signAddress = agentInfo.getPackingAddress();
            VirtualBankDirector director = chain.getMapVirtualBank().get(signAddress);
            listOutDirector.add(director);
            //移除
            chain.getMapVirtualBank().remove(signAddress);
            virtualBankStorageService.deleteByAgentAddress(chain, signAddress);

        }
        return listOutDirector;
    }
}

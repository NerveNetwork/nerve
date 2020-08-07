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

package network.nerve.converter.tx.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.HeterogeneousService;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.AgentBasic;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousAddress;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.dto.SignAccountDTO;
import network.nerve.converter.model.po.TxSubsequentProcessPO;
import network.nerve.converter.model.txdata.ChangeVirtualBankTxData;
import network.nerve.converter.rpc.call.ConsensusCall;
import network.nerve.converter.storage.DisqualificationStorageService;
import network.nerve.converter.storage.TxSubsequentProcessStorageService;
import network.nerve.converter.storage.VirtualBankAllHistoryStorageService;
import network.nerve.converter.storage.VirtualBankStorageService;
import network.nerve.converter.utils.ConverterSignValidUtil;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.VirtualBankUtil;

import java.util.*;

/**
 * @author: Loki
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
    private VirtualBankAllHistoryStorageService virtualBankAllHistoryStorageService;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private TxSubsequentProcessStorageService txSubsequentProcessStorageService;
    @Autowired
    private VirtualBankService virtualBankService;
    @Autowired
    private DisqualificationStorageService disqualificationStorageService;
    @Autowired
    private HeterogeneousService heterogeneousService;


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
            List<AgentBasic> listAgent;
            if(null != blockHeader){
                listAgent = ConsensusCall.getAgentList(chain, blockHeader.getHeight());
            } else {
                listAgent = ConsensusCall.getAgentList(chain);
            }
            // 当前最新有资格成为虚拟银行的(非种子)节点
            List<AgentBasic> listNewestVirtualBank = virtualBankService.calcNewestVirtualBank(chain, listAgent);
            // 区块内业务重复交易检查
            Set<String> setDuplicate = new HashSet<>();
            // 地址重复检查, 同一个区块的所有变更交易的in和out里面不能出现相同的地址
            Set<String> addressDuplicate = new HashSet<>();
            outer:
            for (Transaction tx : txs) {
                byte[] coinData = tx.getCoinData();
                if(coinData != null && coinData.length > 0){
                    // coindata存在数据(coinData应该没有数据)
                    throw new NulsException(ConverterErrorCode.COINDATA_CANNOT_EXIST);
                }
                // 判断区块内重复
                String txDataHex = HexUtil.encode(tx.getTxData());
                if (setDuplicate.contains(txDataHex)) {
                    // 区块内业务重复交易
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                    log.error(ConverterErrorCode.TX_DUPLICATION.getMsg());
                    continue;
                }
                ChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ChangeVirtualBankTxData.class);
                // 地址重复检查
                if(null != txData.getInAgents()){
                    for(byte[] addressBytes : txData.getInAgents()){
                        if(addressDuplicate.contains(AddressTool.getStringAddressByBytes(addressBytes))){
                            // 区块内业务重复交易
                            failsList.add(tx);
                            errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                            log.error("[ChangeVirtualBank] txData has duplication address. hash:{}", tx.getHash().toHex());
                            continue;
                        }
                    }
                }
                if(null != txData.getOutAgents()){
                    for(byte[] addressBytes : txData.getOutAgents()){
                        if(addressDuplicate.contains(AddressTool.getStringAddressByBytes(addressBytes))){
                            // 区块内业务重复交易
                            failsList.add(tx);
                            errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                            log.error("[ChangeVirtualBank] txData has duplication address. hash:{}", tx.getHash().toHex());
                            continue;
                        }
                    }
                }


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
                        // 判断不是种子节点
                        VirtualBankDirector director = chain.getDirectorByAgent(agentAddress);
                        if(director.getSeedNode()){
                            failsList.add(tx);
                            // 种子节点不能退出虚拟银行
                            errorCode = ConverterErrorCode.CAN_NOT_QUIT_VIRTUAL_BANK.getCode();
                            chain.getLogger().error("种子节点不能退出虚拟银行, agentAddress:{}", agentAddress);
                            continue outer;
                        }
                        if(StringUtils.isNotBlank(disqualificationStorageService.find(chain, addressBytes))){
                            // 撤销银行资格列表里面存在该地址, 验证通过直接撤销.
                            continue;
                        }
                        if (!isQuitVirtualBank(chain, listAgent, listNewestVirtualBank, agentAddress)) {
                            failsList.add(tx);
                            long h = null == blockHeader ? -1 : blockHeader.getHeight();
                            log.error(h + " - orginalListAgent: " + JSONUtils.obj2json(listAgent) + ", listNewestVirtualBank: " + JSONUtils.obj2json(listNewestVirtualBank));
                            log.error(h + "- MapVirtualBank: " + JSONUtils.obj2json(chain.getMapVirtualBank()));
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
                    if (countVirtualBank - listOutAgentsSize + listInAgents.size() > ConverterContext.VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED) {
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
                        boolean rs = isJoinVirtualBank(chain, listAgent, listNewestVirtualBank, agentAddress);
                        if (!rs) {
                            failsList.add(tx);
                            long h = null == blockHeader ? -1 : blockHeader.getHeight();
                            log.error(h + " - orginalListAgent: " + JSONUtils.obj2json(listAgent) + ", listNewestVirtualBank: " + JSONUtils.obj2json(listNewestVirtualBank));
                            log.error(h + "- MapVirtualBank: " + JSONUtils.obj2json(chain.getMapVirtualBank()));
                            // 未达到进入虚拟银行的条件
                            errorCode = ConverterErrorCode.CAN_NOT_JOIN_VIRTUAL_BANK.getCode();
                            log.error(ConverterErrorCode.CAN_NOT_JOIN_VIRTUAL_BANK.getMsg());
                            continue outer;
                        }
                    }
                }

                // 验签名
                try {
                    ConverterSignValidUtil.validateVirtualBankSign(chain, tx);
                } catch (NulsException e) {
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    log.error(e.getErrorCode().getMsg());
                    continue outer;
                }
                // 将所有地址 放入重复检查集合
                if(null != txData.getInAgents()){
                    for(byte[] addressBytes : txData.getInAgents()){
                        addressDuplicate.add(AddressTool.getStringAddressByBytes(addressBytes));
                    }
                }
                if(null != txData.getOutAgents()){
                    for(byte[] addressBytes : txData.getOutAgents()){
                        addressDuplicate.add(AddressTool.getStringAddressByBytes(addressBytes));
                    }
                }
                // 将txData hex 放入重复检查集合
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
    private boolean isJoinVirtualBank(Chain chain, List<AgentBasic> listAgent, List<AgentBasic> listNewestVirtualBank, String agentAddress) {
        for (int i = 0; i < listAgent.size(); i++) {
            AgentBasic agentBasic = listAgent.get(i);
            //判断是否是非种子的出块共识节点
            if (!agentBasic.getSeedNode()
                    && agentBasic.getAgentAddress().equals(agentAddress)) {
                if (StringUtils.isBlank(agentBasic.getPubKey())) {
                    chain.getLogger().error("节点公钥为空, 无出块记录不能加入虚拟银行， agentAddress:{}", agentAddress);
                    return false;
                }
                byte[] pubKeyAddrByte = AddressTool.getAddressByPubKeyStr(agentBasic.getPubKey(), chain.getChainId());
                String pubKeyAddr = AddressTool.getStringAddressByBytes(pubKeyAddrByte);
                if (!pubKeyAddr.equals(agentBasic.getPackingAddress())) {
                    //公钥生成的地址与出块地址不一致，表示无效
                    chain.getLogger().error("节点公钥错误，与出块地址不匹配，不能加入虚拟银行， agentAddress:{}", agentAddress);
                    return false;
                }
                //判断排位在虚拟银行规定的排位内
            }
        }

        for (int i = 0; i < listNewestVirtualBank.size(); i++) {
            AgentBasic agentBasic = listNewestVirtualBank.get(i);
            if(agentAddress.equals(agentBasic.getAgentAddress())){
                return true;
            }
        }
        chain.getLogger().error("[ChangeVirtualBankProcessor-validate-isJoinVirtualBank] 当前没有有虚拟银行资格, 不能加入虚拟银行. agentAddress:{}", agentAddress);
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
    private boolean isQuitVirtualBank(Chain chain, List<AgentBasic> listAgent, List<AgentBasic> listNewestVirtualBank, String agentAddress) {
        for (int i = 0; i < listAgent.size(); i++) {
            AgentBasic agentBasic = listAgent.get(i);
            //判断是出块共识节点
            if (agentBasic.getAgentAddress().equals(agentAddress)) {
                // 公钥为空说明没出块, 应该退出虚拟银行
                if (StringUtils.isBlank(agentBasic.getPubKey())) {
                    return true;
                }
                byte[] pubKeyAddrByte = AddressTool.getAddressByPubKeyStr(agentBasic.getPubKey(), chain.getChainId());
                String pubKeyAddr = AddressTool.getStringAddressByBytes(pubKeyAddrByte);
                if (!pubKeyAddr.equals(agentBasic.getPackingAddress())) {
                    //公钥生成的地址与出块地址不一致，表示无效 应该退出
                    return true;
                }
            }
        }
        for (int i = 0; i < listNewestVirtualBank.size(); i++) {
            AgentBasic agentBasic = listNewestVirtualBank.get(i);
            if(agentAddress.equals(agentBasic.getAgentAddress())){
                chain.getLogger().error("当前仍有虚拟银行资格,不能退出虚拟银行, 保证金:{}, 排行:{}", agentBasic.getDeposit(), ConverterContext.INITIAL_VIRTUAL_BANK_COUNT + i + 1);
                return false;
            }
        }

        // 应该退出
        return true;
    }

    private AgentBasic getAgentInfo(List<AgentBasic> listCurrentAgent, String agentAddress) {
        for (int i = 0; i < listCurrentAgent.size(); i++) {
            AgentBasic agentBasic = listCurrentAgent.get(i);
            if (agentBasic.getAgentAddress().equals(agentAddress)) {
                return agentBasic;
            }
        }
        return null;
    }


    /**
     * 加入虚拟银行节点
     *
     * @param chain
     * @param listInAgents
     * @param listCurrentAgent
     * @throws NulsException
     */
    private List<VirtualBankDirector> processSaveVirtualBank(Chain chain, List<byte[]> listInAgents, List<AgentBasic> listCurrentAgent) throws NulsException {
        List<VirtualBankDirector> listInDirector = new ArrayList<>();
        if (null == listInAgents || listInAgents.isEmpty()) {
            chain.getLogger().info("[commit] 没有节点加入虚拟银行");
            return listInDirector;
        }
        for (byte[] addressBytes : listInAgents) {
            String agentAddress = AddressTool.getStringAddressByBytes(addressBytes);
            AgentBasic agentInfo = getAgentInfo(listCurrentAgent, agentAddress);
            if (null == agentInfo) {
                throw new NulsException(ConverterErrorCode.AGENT_INFO_NOT_FOUND);
            }
            String packingAddr = agentInfo.getPackingAddress();
            VirtualBankDirector virtualBankDirector = new VirtualBankDirector();
            virtualBankDirector.setAgentHash(agentInfo.getAgentHash());
            virtualBankDirector.setAgentAddress(agentAddress);
            virtualBankDirector.setSignAddress(packingAddr);
            virtualBankDirector.setRewardAddress(agentInfo.getRewardAddress());
            virtualBankDirector.setSignAddrPubKey(agentInfo.getPubKey());
            virtualBankDirector.setSeedNode(false);
            virtualBankDirector.setHeterogeneousAddrMap(new HashMap<>(ConverterConstant.INIT_CAPACITY_8));
            chain.getLogger().info("[commit] 节点加入虚拟银行, packing:{}, agent:{}", packingAddr, agentAddress);
            listInDirector.add(virtualBankDirector);
        }
        // add by Mimi at 2020-05-06 加入虚拟银行时更新[virtualBankDirector]在DB存储以及内存中的顺序
        VirtualBankUtil.virtualBankAdd(chain, chain.getMapVirtualBank(), listInDirector, virtualBankStorageService);
        // end code by Mimi
        return listInDirector;
    }

    /**
     * 移除虚拟银行节点
     *
     * @param chain
     * @param listAgents
     * @throws NulsException
     */
    private List<VirtualBankDirector> processRemoveVirtualBank(Chain chain, List<byte[]> listAgents) throws NulsException {
        List<VirtualBankDirector> listOutDirector = new ArrayList<>();
        if (null == listAgents || listAgents.isEmpty()) {
            chain.getLogger().info("[commit] 没有节点退出虚拟银行");
            return listOutDirector;
        }
        for (byte[] addressBytes : listAgents) {
            String agentAddress = AddressTool.getStringAddressByBytes(addressBytes);
            VirtualBankDirector director = chain.getDirectorByAgent(agentAddress);
            listOutDirector.add(director);
            String directorSignAddress = director.getSignAddress();
            try {
                chain.getLogger().debug("[退出银行节点信息]:{}", JSONUtils.obj2PrettyJson(director));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            chain.getLogger().info("[commit] 节点退出虚拟银行, packing:{}, agent:{}", directorSignAddress, agentAddress);
        }
        // add by Mimi at 2020-05-06 移除时更新顺序
        VirtualBankUtil.virtualBankRemove(chain, chain.getMapVirtualBank(), listOutDirector, virtualBankStorageService);
        // end code by Mimi
        return listOutDirector;
    }


    /**
     * 检查所有虚拟银行成员创建多签/合约地址, 补充创建地址等信息
     **/
    private void createNewHeterogeneousAddress(Chain chain, List<IHeterogeneousChainDocking> hInterfaces) {
        Collection<VirtualBankDirector> allDirectors = chain.getMapVirtualBank().values();
        if (null != allDirectors && !allDirectors.isEmpty()) {
            for (VirtualBankDirector director : allDirectors) {
                for (IHeterogeneousChainDocking hInterface : hInterfaces) {
                    if (director.getHeterogeneousAddrMap().containsKey(hInterface.getChainId())) {
                        // 存在异构地址就不创建
                        continue;
                    }
                    // 为新成员创建异构链多签地址
                    String heterogeneousAddress = hInterface.generateAddressByCompressedPublicKey(director.getSignAddrPubKey());
                    director.getHeterogeneousAddrMap().put(hInterface.getChainId(),
                            new HeterogeneousAddress(hInterface.getChainId(), heterogeneousAddress));
                    virtualBankStorageService.update(chain, director);
                    virtualBankAllHistoryStorageService.save(chain, director);
                    chain.getLogger().debug("[为新加入节点创建异构链地址] 节点地址:{}, 异构id:{}, 异构地址:{}",
                            director.getAgentAddress(), hInterface.getChainId(), director.getHeterogeneousAddrMap().get(hInterface.getChainId()));
                }
            }
        }

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
            List<AgentBasic> listAgent = ConsensusCall.getAgentList(chain, height);
            SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);

            for (Transaction tx : txs) {
                //维护虚拟银行
                ChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ChangeVirtualBankTxData.class);
                List<byte[]> listOutAgents = txData.getOutAgents();
                List<VirtualBankDirector> listOutDirector =
                        processRemoveVirtualBank(chain, listOutAgents);
                List<byte[]> listInAgents = txData.getInAgents();
                List<VirtualBankDirector> listInDirector =
                        processSaveVirtualBank(chain, listInAgents, listAgent);

                boolean currentJoin = false;
                // 当前节点是虚拟银行成员
                boolean currentDirector = VirtualBankUtil.isCurrentDirector(chain);
                if (currentDirector) {
                    // 如果当前节点是虚拟银行, 则判断当前节点是否是退出虚拟银行节点
                    for (VirtualBankDirector director : listOutDirector) {
                        if (director.getSignAddress().equals(signAccountDTO.getAddress())) {
                            // 当前节点退出成虚拟银行成员
                            currentDirector = false;
                            chain.getCurrentIsDirector().set(false);
                            chain.getLogger().info("[虚拟银行] 当前节点退出虚拟银行,标识变更为: false");
                            break;
                        }
                    }
                } else {
                    if (null != signAccountDTO) {
                        // 如果当前节点不是虚拟银行, 是共识节点, 则判断当前节点是否是新加入的虚拟银行节点
                        for (VirtualBankDirector director : listInDirector) {
                            if (director.getSignAddress().equals(signAccountDTO.getAddress())) {
                                // 当前节点加入成虚拟银行成员
                                chain.getCurrentIsDirector().set(true);
                                currentJoin = true;
                                currentDirector = true;
                                // 异构链组件注册当前节点签名地址
                                virtualBankService.initLocalSignPriKeyToHeterogeneous(chain, signAccountDTO);
                                /**
                                 * 当前是新加入的虚拟银行成员, 默认把异构链调用标志设为true, 在确认变更交易的时候会自动复原.
                                 * 防止本节点, 发出后续异构交易导致合约与链内数据不一致
                                 */
                                heterogeneousService.saveExeHeterogeneousChangeBankStatus(chain, true);
                                chain.getLogger().info("[虚拟银行] 当前节点加入虚拟银行,标识变更为: true");
                                break;
                            }
                        }
                    }
                }
                List<IHeterogeneousChainDocking> hInterfaces = new ArrayList<>(heterogeneousDockingManager.getAllHeterogeneousDocking());
                if (null == hInterfaces || hInterfaces.isEmpty()) {
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST);
                }
                // 添加异构链地址信息
                createNewHeterogeneousAddress(chain, hInterfaces);

                // 同步模式不需要执行虚拟银行多签/合约成员变更
                if (SyncStatusEnum.getEnum(syncStatus).equals(SyncStatusEnum.RUNNING) && currentDirector && !currentJoin) {
                    // 放入异构链处理机制
                    TxSubsequentProcessPO pendingPO = new TxSubsequentProcessPO();
                    pendingPO.setTx(tx);
                    pendingPO.setListInDirector(listInDirector);
                    pendingPO.setListOutDirector(listOutDirector);
                    pendingPO.setBlockHeader(blockHeader);
                    pendingPO.setSyncStatusEnum(SyncStatusEnum.getEnum(syncStatus));
                    pendingPO.setCurrentJoin(currentJoin);
                    pendingPO.setCurrentDirector(currentDirector);
                    txSubsequentProcessStorageService.save(chain, pendingPO);
                    chain.getPendingTxQueue().offer(pendingPO);
                }

                ConsensusCall.sendVirtualBank(chain, height);

                chain.getLogger().info("[commit]虚拟银行变更交易 hash:{}", tx.getHash().toHex());
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
            SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
            for (Transaction tx : txs) {
                ChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ChangeVirtualBankTxData.class);
                long requestHeight = txData.getOutHeight() - 1;
                if(requestHeight < 0L){
                    requestHeight = 0L;
                }
                List<AgentBasic> listAgent = ConsensusCall.getAgentList(chain, requestHeight);
                //维护虚拟银行
                List<byte[]> listOutAgents = txData.getOutAgents();
                //把退出的虚拟银行的节点加回去
                List<VirtualBankDirector> listInDirector = processSaveVirtualBank(chain, listOutAgents, listAgent);
                //把加入的虚拟银行移除
                List<byte[]> listInAgents = txData.getInAgents();
                List<VirtualBankDirector> listOutDirector = processRemoveVirtualBank(chain, listInAgents);

                // 当前节点是虚拟银行成员
                if (VirtualBankUtil.isCurrentDirector(chain)) {
                    // 如果当前节点是虚拟银行, 移除的节点中包含当前节点, 则设为退出状态
                    for (VirtualBankDirector director : listOutDirector) {
                        if (director.getSignAddress().equals(signAccountDTO.getAddress())) {
                            // 当前节点退出成虚拟银行成员
                            chain.getCurrentIsDirector().set(false);
                            chain.setInitLocalSignPriKeyToHeterogeneous(false);
                            chain.getLogger().info("[虚拟银行] 当前节点退出虚拟银行,标识变更为: false");
                            break;
                        }
                    }
                } else {
                    if (null != signAccountDTO) {
                        // 如果当前节点不是虚拟银行, 加入的节点中包含当前节点, 则设为加入状态
                        for (VirtualBankDirector director : listInDirector) {
                            if (director.getSignAddress().equals(signAccountDTO.getAddress())) {
                                // 当前节点加入成虚拟银行成员
                                chain.getCurrentIsDirector().set(true);
                                chain.getLogger().info("[虚拟银行] 当前节点计入虚拟银行,标识变更为: true");
                                break;
                            }
                        }
                    }
                }
                ConsensusCall.sendVirtualBank(chain, blockHeader.getHeight());
                chain.getLogger().info("[rollback]虚拟银行变更交易 hash:{}", tx.getHash().toHex());
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

}

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

package network.nerve.converter.core.business.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.core.constant.SyncStatusEnum;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.business.VirtualBankService;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.ProposalVoteStatusEnum;
import network.nerve.converter.manager.ChainManager;
import network.nerve.converter.model.bo.*;
import network.nerve.converter.model.dto.SignAccountDTO;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.po.VirtualBankTemporaryChangePO;
import network.nerve.converter.rpc.call.AccountCall;
import network.nerve.converter.rpc.call.BlockCall;
import network.nerve.converter.rpc.call.ConsensusCall;
import network.nerve.converter.storage.*;
import network.nerve.converter.utils.VirtualBankUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static network.nerve.converter.config.ConverterContext.*;
import static network.nerve.converter.constant.ConverterConstant.HETEROGENEOUS_VERSION_1;
import static network.nerve.converter.constant.ConverterConstant.HETEROGENEOUS_VERSION_2;

/**
 * @author: Loki
 * @date: 2020-03-13
 */
@Component
public class VirtualBankServiceImpl implements VirtualBankService {

    @Autowired
    private AssembleTxService assembleTxService;
    @Autowired
    private VirtualBankStorageService virtualBankStorageService;
    @Autowired
    private VirtualBankAllHistoryStorageService virtualBankAllHistoryStorageService;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private ProposalVotingStorageService proposalVotingStorageService;
    @Autowired
    private ProposalStorageService proposalStorageService;
    @Autowired
    private DisqualificationStorageService disqualificationStorageService;

    @Override
    public void recordVirtualBankChanges(Chain chain) {
        LatestBasicBlock latestBasicBlock = chain.getLatestBasicBlock();
        long height = latestBasicBlock.getHeight();
        // 获取最新共识列表
        List<AgentBasic> listAgent = ConsensusCall.getAgentList(chain, latestBasicBlock.getHeight());
        if (null == listAgent) {
            chain.getLogger().error("检查虚拟银行变更时, 向共识模块获取共识节点列表数据为null");
            return;
        }
        // 初始化虚拟银行
        initBank(chain, listAgent, height);
        if (latestBasicBlock.getSyncStatusEnum().equals(SyncStatusEnum.RUNNING)) {
            // 判断当前是否是虚拟银行成员
            SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
            if (null == signAccountDTO) {
                chain.getCurrentIsDirector().set(false);
                chain.setInitLocalSignPriKeyToHeterogeneous(false);
                return;
            }
            if (null != signAccountDTO && chain.isVirtualBankBySignAddr(signAccountDTO.getAddress())) {
                chain.getCurrentIsDirector().set(true);
            }
            if (!VirtualBankUtil.isCurrentDirector(chain)) {
                return;
            }
            //虚拟银行检查
            VirtualBankTemporaryChangePO virtualBankTemporaryChange = baseCheckVirtualBank(chain, listAgent, height, signAccountDTO);
            if (null == virtualBankTemporaryChange) {
                return;
            }
            chain.getLogger().info("[虚拟银行变更] 检测到需要执行创建变更交易.. 加入数:{}, 退出数:{}",
                    virtualBankTemporaryChange.getListInAgents().size(),
                    virtualBankTemporaryChange.getListOutAgents().size());
            // 如果有stopAgent节点 则用停止节点区块头的时间,作为变更交易时间
            long txTime = virtualBankTemporaryChange.getOutTxBlockTime() > 0L ? virtualBankTemporaryChange.getOutTxBlockTime() : latestBasicBlock.getTime();
            effectVirtualBankChangeTx(chain, virtualBankTemporaryChange, txTime);
        }
        // 检查提案是否到期
        checkIfVotingProposalClosed(chain, latestBasicBlock);

    }

    /**
     * 检查投票中的提案是否结束
     *
     * @param chain
     * @param latestBasicBlock
     */
    private void checkIfVotingProposalClosed(Chain chain, LatestBasicBlock latestBasicBlock) {
        // 可投票的提案放入缓存map
        Set<ProposalPO> removeSet = new HashSet<>();
        List<ProposalPO> list = new ArrayList<>(chain.getVotingProposalMap().values());
        for (ProposalPO po : list) {
            if (latestBasicBlock.getHeight() >= po.getVoteEndHeight()) {
                // 提案投票结束 修改状态 从投票中列表中移除, 更新提案数据库
                if (po.getStatus() == ProposalVoteStatusEnum.VOTING.value()) {
                    // 提案还在投票中说明还没通过, 则设置为被否决.
                    po.setStatus(ProposalVoteStatusEnum.REJECTED.value());
                }
                this.proposalStorageService.save(chain, po);
                this.proposalVotingStorageService.delete(chain, po.getHash());
                removeSet.add(po);
                chain.getLogger().info("提案已截止投票, hash:{}, type:{}, endHeight:{}, status:{}",
                        po.getHash().toHex(),
                        po.getType(),
                        po.getVoteEndHeight(),
                        ProposalVoteStatusEnum.getEnum(po.getStatus()));
            }
        }
        // 清理
        for (ProposalPO proposalPO : removeSet) {
            chain.getVotingProposalMap().remove(proposalPO.getHash());
        }
    }


    @Override
    public VirtualBankDirector getCurrentDirector(int chainId) throws NulsException {
        Chain chain = chainManager.getChain(chainId);
        if (null == chain) {
            throw new NulsException(ConverterErrorCode.CHAIN_NOT_EXIST);
        }
        if (!VirtualBankUtil.isCurrentDirector(chain)) {
            return null;
        }
        SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        return chain.getMapVirtualBank().get(signAccountDTO.getAddress());
    }

    @Override
    public void initLocalSignPriKeyToHeterogeneous(Chain chain, SignAccountDTO signAccountDTO) throws NulsException {
        // 如果本节点是共识节点, 并且是虚拟银行成员则执行注册
        if (null != signAccountDTO) {
            String priKey = AccountCall.getPriKey(signAccountDTO.getAddress(), signAccountDTO.getPassword());
            List<IHeterogeneousChainDocking> hInterfaces = new ArrayList<>(heterogeneousDockingManager.getAllHeterogeneousDocking());
            if (null == hInterfaces || hInterfaces.isEmpty()) {
                chain.getLogger().error("异构链组件为空");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST);
            }
            for (IHeterogeneousChainDocking dock : hInterfaces) {
                dock.importAccountByPriKey(priKey, signAccountDTO.getPassword());
                chain.getLogger().info("[初始化]本节点是虚拟银行节点,向异构链组件注册签名账户信息..");
            }
        }
    }


    private void initBank(Chain chain, List<AgentBasic> listAgent, long height) {
        int bankNumber = chain.getMapVirtualBank().size();
        /**
         * 判断需要进行初始化
         */
        if (INIT_VIRTUAL_BANK_HEIGHT <= height && bankNumber == 0) {
            try {
                //初始化种子节点为初始虚拟银行
                initVirtualBank(chain, listAgent, height);
            } catch (Exception e) {
                chain.getLogger().error(e);
            }
        }
        /**
         * 节点数超过阈值, 有虚拟银行在运行
         */
        if (!ENABLED_VIRTUAL_BANK_CHANGES_SERVICE) {
//            if (ConverterContext.INITIAL_VIRTUAL_BANK_COUNT <= listAgent.size() && bankNumber > 0) {
            if (ConverterContext.INITIAL_VIRTUAL_BANK_SEED_COUNT <= chain.getMapVirtualBank().size()) {
                ENABLED_VIRTUAL_BANK_CHANGES_SERVICE = true;
            }
        }
    }

    /**
     * 统计出立即执行的最新待加入和待退出的虚拟银行节点列表
     * 1.检查是否有成员退出, 有则执行变更.
     * 2.检查达到周期执行高度, 达到则执行变更.
     * 返回null(不执行变更)的情况包括
     * 1.没有成员退出 或没有达到周期执行条件
     * 2.虚拟银行成员无变化
     *
     * @param chain
     * @throws NulsException
     */
    private VirtualBankTemporaryChangePO baseCheckVirtualBank(Chain chain, List<AgentBasic> listAgent, long height, SignAccountDTO signAccountDTO) {
        //达到检查周期
        boolean immediateEffectHeight = height % EXECUTE_CHANGE_VIRTUAL_BANK_PERIODIC_HEIGHT == 0;
        if (immediateEffectHeight) {
            //记录当前检查高度
            chain.getLogger().info("达到周期性检查执行高度:{}", height);
        }
        if (INIT_VIRTUAL_BANK_HEIGHT <= height && !chain.getInitLocalSignPriKeyToHeterogeneous()) {
            try {
                // 向异构链组件,注册地址签名信息
                initLocalSignPriKeyToHeterogeneous(chain, signAccountDTO);
                chain.setInitLocalSignPriKeyToHeterogeneous(true);
            } catch (NulsException e) {
                if (immediateEffectHeight) {
                    chain.getLogger().info("达到周期性检查执行高度:{}, - 退出检查(向异构链组件注册地址签名信息异常)", height);
                }
                chain.getLogger().error(e);
            }
        }

        if (!ENABLED_VIRTUAL_BANK_CHANGES_SERVICE) {
            // 没有达到开启条件 不能触发变更服务 直接返回
            if (immediateEffectHeight) {
                chain.getLogger().info("达到周期性检查执行高度:{}, - 退出检查(变更服务未开启)", height);
            }
            return null;
        }

        if (chain.getResetVirtualBank().get() || chain.getExeDisqualifyBankProposal().get()) {
            // 当正在执行重置虚拟银行合约 或执行踢出虚拟银行提案时, 不进行银行变更检查
            return null;
        }
        // 根据最新共识列表,计算出最新的有虚拟银行资格的成员(非当前实际生效的虚拟应银行成员)
        List<AgentBasic> listVirtualBank = calcNewestVirtualBank(chain, listAgent);
        // 当前已生效的
        Map<String, VirtualBankDirector> mapCurrentVirtualBank = chain.getMapVirtualBank();
        // 记录周期内模块本地银行变化情况
        VirtualBankTemporaryChangePO virtualBankTemporaryChange = new VirtualBankTemporaryChangePO();
        boolean isImmediateEffect = false;

        // 统计退出的节点 (只要不在最新计算的虚拟银行列表中, 则表示退出立即执行(包括保证金排名和直接停止共识节点))
        for (VirtualBankDirector director : mapCurrentVirtualBank.values()) {
            if (director.getSeedNode()) {
                continue;
            }
            boolean rs = true;
            for (AgentBasic agentBasic : listVirtualBank) {
                // 判断当前银行成员也存在于计算出的最新有虚拟银行资格的列表中, 或成员是种子节点 都不用放入退出列表
                if (director.getAgentAddress().equals(agentBasic.getAgentAddress())) {
                    // 不需要执行退出的节点
                    rs = false;
                }
            }
            // 查询是否在撤销资格列表中
            String addr = disqualificationStorageService.find(chain, AddressTool.getAddress(director.getAgentAddress()));
            if (rs || StringUtils.isNotBlank(addr)) {
                //表示已经不是虚拟银行节点, 需要立即发布虚拟银行变更交易
                virtualBankTemporaryChange.getListOutAgents().add(AddressTool.getAddress(director.getAgentAddress()));

                /**
                 * 需要根据退出的节点地址, 去获取节点hash, 再获取节点信息
                 * 如果有节点退出高度就说明节点退出共识,直接取退出节点的高度; 否则用当前高度
                 * 节点hash需要存在虚拟银行列表中, 需要通过 ConsensusCall.getAgentInfo(chain, latestBasicBlock.getHeight());获取得到
                 */
                String agentHash = director.getAgentHash();
                long delHeight = ConsensusCall.agentDelHeight(chain, agentHash);
                virtualBankTemporaryChange.setOutHeight(delHeight);
                if (delHeight > 0L) {
                    // 说明有stopagent节点
                    BlockHeader blockHeader = BlockCall.getBlockHeader(chain, delHeight);
                    if (null == blockHeader) {
                        chain.getLogger().error("检查虚拟银行变更, 根据stopAgent高度获取区块头异常, height:{},", delHeight);
                        return null;
                    }
                    virtualBankTemporaryChange.setOutTxBlockTime(blockHeader.getTime());
                }
                try {
                    chain.getLogger().debug("OutAgent:{}", JSONUtils.obj2json(director));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                isImmediateEffect = true;
            }
        }

        if (!isImmediateEffect && !immediateEffectHeight) {
            // 如果没有退出的节点, 并且当前没达到周期性的触发高度
            return null;
        }
        // 统计加入的节点
        for (AgentBasic agentBasic : listVirtualBank) {
            // 判断当前生效的虚拟银行中不存在该节点, 表示该节点是新增节点
            if (!mapCurrentVirtualBank.containsKey(agentBasic.getPackingAddress())) {
                virtualBankTemporaryChange.getListInAgents().add(AddressTool.getAddress(agentBasic.getAgentAddress()));
                try {
                    chain.getLogger().debug("InAgent:{}", JSONUtils.obj2json(agentBasic));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
        if (virtualBankTemporaryChange.isBlank()) {
            // 表示虚拟银行成员没有变化
            chain.getLogger().info("[Check] 虚拟银行变更检查完成, 虚拟银行成员无变化!");
            return null;
        }
        return virtualBankTemporaryChange;
    }

    /**
     * 判断节点出块地址对应的异构地址余额是否满足条件
     *
     * @param agentBasic
     * @return 满足:true, 不满足:false
     */
    private boolean checkHeterogeneousAddressBalance(Chain chain, AgentBasic agentBasic) {
        List<IHeterogeneousChainDocking> hInterfaces = new ArrayList<>(heterogeneousDockingManager.getAllHeterogeneousDocking());
        for (IHeterogeneousChainDocking hInterface : hInterfaces) {
            String pubKey = agentBasic.getPubKey();
            if (StringUtils.isBlank(pubKey)) {
                return false;
            }
            String hAddress = hInterface.generateAddressByCompressedPublicKey(agentBasic.getPubKey());
            BigDecimal balance = hInterface.getBalance(hAddress).stripTrailingZeros();
            boolean rs = false;
            for (HeterogeneousCfg cfg : chain.getListHeterogeneous()) {
                if (cfg.getType() != 1) {
                    // 非主资产, 无需验证
                    continue;
                }
                if (cfg.getChainId() == hInterface.getChainId() && cfg.getInitialBalance().compareTo(balance) < 0) {
                    rs = true;
                }
            }
            // 所有异构链地址都需要满足条件
            if (!rs) {
                chain.getLogger().warn("The agent heterogeneous address insufficient balance, cannot join virtual bank. agentAddress:{}, packingAddress:{}",
                        agentBasic.getAgentAddress(), agentBasic.getPackingAddress());
                return false;
            }
        }
        return true;
    }

    /**
     * 根据最新的排好序的共识列表 统计出最新的有虚拟银行资格的成员
     *
     * @param listAgent
     * @return
     */
    @Override
    public List<AgentBasic> calcNewestVirtualBank(Chain chain, List<AgentBasic> listAgent) {
        List<AgentBasic> listVirtualBank = new ArrayList<>();
        // 排除种子节点
        for (AgentBasic agentBasic : listAgent) {
            // 排除已被撤销虚拟银行资格的节点地址
            String disqualificationAddress = disqualificationStorageService.find(chain, AddressTool.getAddress(agentBasic.getAgentAddress()));

            /* // 排除节点出块地址对应的异构地址余额不满足初始值的节点
            boolean join = checkHeterogeneousAddressBalance(chain, agentBasic);*/
            boolean packaged = StringUtils.isNotBlank(agentBasic.getPubKey());
            if (!agentBasic.getSeedNode() && StringUtils.isBlank(disqualificationAddress) && packaged) {
                listVirtualBank.add(agentBasic);
            }
        }
        if (listVirtualBank.size() <= VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED) {
            return listVirtualBank;
        }
        /**
         * 同时有多个节点保证金都相同,且满足加入虚拟银行条件,
         * 如都加入会超出银行总数阈值 需要进行排序来确定最终银行成员
         * 取出排在虚拟银行总数阈值位置的节点, 再取出列表中与之保证金相同的节点
         * 通过当前银行的顺序,当前已排序节点列表来确定最终顺序
         */
        AgentBasic thresholdAgentBasic = listVirtualBank.get(VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED - 1);
        BigInteger thresholdDeposit = thresholdAgentBasic.getDeposit();

        // 阈值后一个节点
        AgentBasic afterThresholdAgentBasic = listVirtualBank.get(VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED);
        BigInteger afterThresholdDeposit = afterThresholdAgentBasic.getDeposit();

        if (thresholdDeposit.compareTo(afterThresholdDeposit) != 0) {
            // 处于阈值位置节点的保证金与后一个节点保证金不同, 直接取前 VIRTUAL_BANK_AGENT_NUMBER 个节点成员新的虚拟银行资格成员列表
            return listVirtualBank.subList(0, VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED);
        }

        Map<String, VirtualBankDirector> mapCurrentVirtualBank = chain.getMapVirtualBank();
        List<AgentBasic> sameDeposit = new ArrayList<>();
        List<AgentBasic> listFinalVirtualBank = new ArrayList<>();
        for (int i = 0; i < listVirtualBank.size(); i++) {
            AgentBasic agentBasic = listVirtualBank.get(i);
            if (thresholdDeposit.compareTo(agentBasic.getDeposit()) == 0) {
                VirtualBankDirector director = mapCurrentVirtualBank.get(agentBasic.getPackingAddress());
                if (null == director) {
                    // 如果不是虚拟银行成员则index为银行总数+当前索引 保证当前顺序, 但排名在当前虚拟银行节点之后
                    agentBasic.setIndex(VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED + INITIAL_VIRTUAL_BANK_SEED_COUNT + i + 1);
                } else {
                    // 如果是虚拟银行则 用虚拟银行的排序值
                    agentBasic.setIndex(director.getOrder());
                }
                sameDeposit.add(agentBasic);
            }
            if (thresholdDeposit.compareTo(agentBasic.getDeposit()) < 0) {
                listFinalVirtualBank.add(agentBasic);
            }
        }
        // 排序
        sameDeposit.sort((Comparator.comparingInt(AgentBasic::getIndex)));
        int gap = VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED - listFinalVirtualBank.size();
        listFinalVirtualBank.addAll(sameDeposit.subList(0, gap));
        return listFinalVirtualBank;
    }

    /**
     * 添加所有种子节点为虚拟银行成员
     * @param chain
     * @param listAgent
     */
    private void addBankDirector(Chain chain, List<AgentBasic> listAgent){
        SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        List<VirtualBankDirector> listInDirector = new ArrayList<>();
        for (AgentBasic agentBasic : listAgent) {
            if (agentBasic.getSeedNode()) {
                VirtualBankDirector virtualBankDirector = new VirtualBankDirector();
                // 种子节点打包地址,节点地址 奖励地址 设为一致
                virtualBankDirector.setAgentHash(agentBasic.getAgentHash());
                virtualBankDirector.setAgentAddress(agentBasic.getPackingAddress());
                virtualBankDirector.setSignAddress(agentBasic.getPackingAddress());
                virtualBankDirector.setRewardAddress(agentBasic.getPackingAddress());
                virtualBankDirector.setSignAddrPubKey(agentBasic.getPubKey());
                virtualBankDirector.setSeedNode(agentBasic.getSeedNode());
                virtualBankDirector.setHeterogeneousAddrMap(new HashMap<>(ConverterConstant.INIT_CAPACITY_8));
                listInDirector.add(virtualBankDirector);
                // 如果当前是共识节点, 判断当前是否是虚拟银行成员
                if (null != signAccountDTO && agentBasic.getPackingAddress().equals(signAccountDTO.getAddress())) {
                    chain.getCurrentIsDirector().set(true);
                    chain.getLogger().info("[虚拟银行] 当前节点介入虚拟银行,标识变更为: true");
                }
            }
        }
        // add by Mimi at 2020-05-06 加入虚拟银行时更新[virtualBankDirector]在DB存储以及内存中的顺序
        VirtualBankUtil.virtualBankAdd(chain, chain.getMapVirtualBank(), listInDirector, virtualBankStorageService);
        // end code by Mimi
    }

    /**
     * 添加配置的种子节点为虚拟银行成员
     * @param chain
     * @param listAgent
     */
    private void addBankDirectorBySetting(Chain chain, List<AgentBasic> listAgent){
        SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        List<VirtualBankDirector> listInDirector = new ArrayList<>();
        for (AgentBasic agentBasic : listAgent) {
            if (!agentBasic.getSeedNode()) {
                continue;
            }
            for(String pubkey : INIT_VIRTUAL_BANK_PUBKEY_LIST){
                if(pubkey.equals(agentBasic.getPubKey())){
                    VirtualBankDirector virtualBankDirector = new VirtualBankDirector();
                    // 种子节点打包地址,节点地址 奖励地址 设为一致
                    virtualBankDirector.setAgentHash(agentBasic.getAgentHash());
                    virtualBankDirector.setAgentAddress(agentBasic.getPackingAddress());
                    virtualBankDirector.setSignAddress(agentBasic.getPackingAddress());
                    virtualBankDirector.setRewardAddress(agentBasic.getPackingAddress());
                    virtualBankDirector.setSignAddrPubKey(agentBasic.getPubKey());
                    virtualBankDirector.setSeedNode(agentBasic.getSeedNode());
                    virtualBankDirector.setHeterogeneousAddrMap(new HashMap<>(ConverterConstant.INIT_CAPACITY_8));
                    listInDirector.add(virtualBankDirector);
                    // 如果当前是共识节点, 判断当前是否是虚拟银行成员
                    if (null != signAccountDTO && agentBasic.getPackingAddress().equals(signAccountDTO.getAddress())) {
                        chain.getCurrentIsDirector().set(true);
                        chain.getLogger().info("[虚拟银行] 当前节点介入虚拟银行,标识变更为: true");
                    }
                }
            }
        }
        // add by Mimi at 2020-05-06 加入虚拟银行时更新[virtualBankDirector]在DB存储以及内存中的顺序
        VirtualBankUtil.virtualBankAdd(chain, chain.getMapVirtualBank(), listInDirector, virtualBankStorageService);
        // end code by Mimi
    }

    /**
     * 初始化所有种子节点为虚拟银行
     *
     * @param chain
     * @param listAgent
     */
    private void initVirtualBank(Chain chain, List<AgentBasic> listAgent, long height) throws NulsException {

        // 如果虚拟银行为空 将种子节点初始化为 虚拟银行节成员
        if (chain.getMapVirtualBank().isEmpty()) {
            if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_1) {
                addBankDirector(chain,  listAgent);
            } else if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_2) {
                addBankDirectorBySetting( chain, listAgent);
            }
        }
        // 如果虚拟银行成员 初始化异构链地址
        List<IHeterogeneousChainDocking> hInterfaces = new ArrayList<>(heterogeneousDockingManager.getAllHeterogeneousDocking());
        if (null == hInterfaces || hInterfaces.isEmpty()) {
            chain.getLogger().error("异构链组件为空");
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST);
        }
        // 调异构链组件,进行初始化
        for (VirtualBankDirector director : chain.getMapVirtualBank().values()) {
            if (!director.getSeedNode()) {
                // 非种子节点 没有初始化, 无法获取异构链地址
                continue;
            }
            for (IHeterogeneousChainDocking hInterface : hInterfaces) {
                if (!director.getHeterogeneousAddrMap().containsKey(hInterface.getChainId())) {
                    // 为新成员创建新的异构链多签地址
                    String heterogeneousAddress = hInterface.generateAddressByCompressedPublicKey(director.getSignAddrPubKey());
                    director.getHeterogeneousAddrMap().put(hInterface.getChainId(),
                            new HeterogeneousAddress(hInterface.getChainId(), heterogeneousAddress));
                    virtualBankStorageService.save(chain, director);
                    virtualBankAllHistoryStorageService.save(chain, director);
                }
            }
        }
        // 发送给共识
        ConsensusCall.sendVirtualBank(chain, height);
        try {
            chain.getLogger().info("[初始化]虚拟银行及异构链初始化完成 - MapVirtualBank : {}", JSONUtils.obj2json(chain.getMapVirtualBank()));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }

    /**
     * 执行创建并发布虚拟银行变更交易
     *
     * @param chain
     * @param vbankChange
     * @param txTime      当前高度的区块时间作为虚拟银行变更的交易时间
     */
    private void effectVirtualBankChangeTx(Chain chain, VirtualBankTemporaryChangePO vbankChange, long txTime) {
        List<byte[]> inAgentList = vbankChange.getListInAgents();
        List<byte[]> outAgentList = vbankChange.getListOutAgents();
        try {
            assembleTxService.createChangeVirtualBankTx(chain, inAgentList, outAgentList, vbankChange.getOutHeight(), txTime);
        } catch (NulsException e) {
            chain.getLogger().error(e);
        }
    }


}

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

    private boolean huobiCrossChainAvailable = false;

    @Override
    public void recordVirtualBankChanges(Chain chain) {
        LatestBasicBlock latestBasicBlock = chain.getLatestBasicBlock();
        long height = latestBasicBlock.getHeight();
        // Get the latest consensus list
        List<AgentBasic> listAgent = ConsensusCall.getAgentList(chain, latestBasicBlock.getHeight());
        if (null == listAgent) {
            chain.getLogger().error("When checking for changes in virtual banking, Obtain consensus node list data from the consensus module asnull");
            return;
        }
        // Initialize virtual bank
        initBank(chain, listAgent, height);
        if (latestBasicBlock.getSyncStatusEnum().equals(SyncStatusEnum.RUNNING)) {
            // Determine whether the current member is a virtual bank
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
            //Virtual Bank Check
            VirtualBankTemporaryChangePO virtualBankTemporaryChange = baseCheckVirtualBank(chain, listAgent, height, signAccountDTO);
            if (null == virtualBankTemporaryChange) {
                return;
            }
            chain.getLogger().info("[Virtual Bank Change] Detected the need to create a change transaction.. Number of additions:{}, Number of exits:{}",
                    virtualBankTemporaryChange.getListInAgents().size(),
                    virtualBankTemporaryChange.getListOutAgents().size());
            // If there is anystopAgentnode Then use the time to stop the node block header,As a change in transaction time
            long txTime = virtualBankTemporaryChange.getOutTxBlockTime() > 0L ? virtualBankTemporaryChange.getOutTxBlockTime() : latestBasicBlock.getTime();
            effectVirtualBankChangeTx(chain, virtualBankTemporaryChange, txTime);
        }
        // Check if the proposal has expired
        checkIfVotingProposalClosed(chain, latestBasicBlock);

    }

    /**
     * Check if the proposal in the vote has ended
     *
     * @param chain
     * @param latestBasicBlock
     */
    private void checkIfVotingProposalClosed(Chain chain, LatestBasicBlock latestBasicBlock) {
        // Votable proposals placed in cachemap
        Set<ProposalPO> removeSet = new HashSet<>();
        List<ProposalPO> list = new ArrayList<>(chain.getVotingProposalMap().values());
        for (ProposalPO po : list) {
            if (latestBasicBlock.getHeight() >= po.getVoteEndHeight()) {
                // Proposal voting ended modify state Remove from voting list, Update proposal database
                if (po.getStatus() == ProposalVoteStatusEnum.VOTING.value()) {
                    // The proposal is still in the voting process, indicating that it has not been approved yet, Set as rejected.
                    po.setStatus(ProposalVoteStatusEnum.REJECTED.value());
                }
                this.proposalStorageService.save(chain, po);
                this.proposalVotingStorageService.delete(chain, po.getHash());
                removeSet.add(po);
                chain.getLogger().info("The proposal has been closed for voting, hash:{}, type:{}, endHeight:{}, status:{}",
                        po.getHash().toHex(),
                        po.getType(),
                        po.getVoteEndHeight(),
                        ProposalVoteStatusEnum.getEnum(po.getStatus()));
            }
        }
        // clean up
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
        // If this node is a consensus node, And if it is a virtual bank member, registration will be executed
        if (null != signAccountDTO) {
            String priKey = AccountCall.getPriKey(signAccountDTO.getAddress(), signAccountDTO.getPassword());
            List<IHeterogeneousChainDocking> hInterfaces = new ArrayList<>(heterogeneousDockingManager.getAllHeterogeneousDocking());
            if (null == hInterfaces || hInterfaces.isEmpty()) {
                chain.getLogger().error("Heterogeneous chain component is empty");
                throw new NulsException(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST);
            }
            for (IHeterogeneousChainDocking dock : hInterfaces) {
                dock.importAccountByPriKey(priKey, signAccountDTO.getPassword());
                chain.getLogger().info("[initialization]This node is a virtual banking node,Register signing account information with heterogeneous chain components..");
            }
        }
    }


    private synchronized void initBank(Chain chain, List<AgentBasic> listAgent, long height) {
        int bankNumber = chain.getMapVirtualBank().size();
        /* 2020/11/19 If the number of heterogeneous chains is different from that of virtual banking nodes（seed）The number of heterogeneous chain addresses maintained is inconsistent, Then it is necessary to initialize the virtual bank**/
        int heterogeneousSize = 0;
        for (VirtualBankDirector director : chain.getMapVirtualBank().values()) {
            if (director.getSeedNode()) {
                heterogeneousSize = director.getHeterogeneousAddrMap().size();
                if (chain.getChainId() == 5) {
                    // add by pierre at 2023/2/6 Test network deleted 101 allocation
                    if (director.getHeterogeneousAddrMap().containsKey(101)) {
                        heterogeneousSize--;
                    }
                    if (director.getHeterogeneousAddrMap().containsKey(137)) {
                        heterogeneousSize--;
                    }
                    if (director.getHeterogeneousAddrMap().containsKey(142)) {
                        heterogeneousSize--;
                    }
                }
                /*try {
                    chain.getLogger().info("heterogeneousSize: {}", JSONUtils.obj2json(director.getHeterogeneousAddrMap()));
                } catch (Exception e) {
                    chain.getLogger().error(e);
                }*/
                break;
            }
        }
        int hSize = heterogeneousDockingManager.getAllHeterogeneousDocking().size();
        /*try {
            Collection<IHeterogeneousChainDocking> dockings = heterogeneousDockingManager.getAllHeterogeneousDocking();
            StringBuilder sb = new StringBuilder();
            for (IHeterogeneousChainDocking docking : dockings) {
                sb.append(docking.getChainId()).append(",");
            }
            chain.getLogger().info("hSize: {}", sb.toString());
        } catch (Exception e) {
            chain.getLogger().error(e);
        }*/
        chain.getLogger().info("heterogeneousSize: {}, hSize: {}, bankNumber: {}", heterogeneousSize, hSize, bankNumber);
        /**
         * Determine the need for initialization
         */
        if (heterogeneousSize != hSize || (INIT_VIRTUAL_BANK_HEIGHT <= height && bankNumber == 0)) {
            chain.getLogger().debug("INIT_VIRTUAL_BANK_HEIGHT: {}, height: {}", INIT_VIRTUAL_BANK_HEIGHT, height);
            try {
                //Initialize the seed node as the initial virtual bank
                initVirtualBank(chain, listAgent, height);
            } catch (Exception e) {
                chain.getLogger().error(e);
            }
        }
        /**
         * The number of nodes exceeds the threshold, There is a virtual bank running
         */
        if (!ENABLED_VIRTUAL_BANK_CHANGES_SERVICE) {
//            if (ConverterContext.INITIAL_VIRTUAL_BANK_COUNT <= listAgent.size() && bankNumber > 0) {
            if (ConverterContext.INITIAL_VIRTUAL_BANK_SEED_COUNT <= chain.getMapVirtualBank().size()) {
                ENABLED_VIRTUAL_BANK_CHANGES_SERVICE = true;
            }
        }
    }

    /**
     * Count the latest list of virtual bank nodes to be added and exited for immediate execution
     * 1.Check if any members have exited, If there is, execute the change.
     * 2.Check that the cycle execution height has been reached, If achieved, execute the change.
     * returnnull(Do not execute changes)The situation includes
     * 1.No members left Or did not meet the cycle execution conditions
     * 2.Virtual bank members remain unchanged
     *
     * @param chain
     * @throws NulsException
     */
    private VirtualBankTemporaryChangePO baseCheckVirtualBank(Chain chain, List<AgentBasic> listAgent, long height, SignAccountDTO signAccountDTO) {
        //Reaching the inspection cycle
        boolean immediateEffectHeight = height % EXECUTE_CHANGE_VIRTUAL_BANK_PERIODIC_HEIGHT == 0;
        if (immediateEffectHeight) {
            //Record the current inspection height
            chain.getLogger().info("Reaching the height of periodic inspection execution:{}", height);
        }
        // initialization
        if (INIT_VIRTUAL_BANK_HEIGHT <= height && !chain.getInitLocalSignPriKeyToHeterogeneous()) {
            try {
                // To heterogeneous chain components,Registration address signature information
                initLocalSignPriKeyToHeterogeneous(chain, signAccountDTO);
                chain.setInitLocalSignPriKeyToHeterogeneous(true);
            } catch (NulsException e) {
                if (immediateEffectHeight) {
                    chain.getLogger().info("Reaching the height of periodic inspection execution:{}, - Exit Check(Abnormal address signature information registered with heterogeneous chain components)", height);
                }
                chain.getLogger().error(e);
            }
        }
        // Newly added heterogeneous cross chain components should be checked based on their effectiveness level to determine if they are being integrated into heterogeneous chain components,Registration address signature information
        heterogeneousDockingManager.checkAccountImportedInDocking(chain, signAccountDTO);

        if (!ENABLED_VIRTUAL_BANK_CHANGES_SERVICE) {
            // Not meeting the opening conditions Cannot trigger change service Directly return
            if (immediateEffectHeight) {
                chain.getLogger().info("Reaching the height of periodic inspection execution:{}, - Exit Check(Change service not enabled)", height);
            }
            return null;
        }

        if (chain.getResetVirtualBank().get() || chain.getExeDisqualifyBankProposal().get()) {
            // When resetting virtual banking contracts Or when executing the proposal to kick out the virtual bank, Not conducting bank change checks
            return null;
        }
        // According to the latest consensus list,Calculate the latest members with virtual banking qualifications(Non current effective virtual bank members)
        List<AgentBasic> listVirtualBank = calcNewestVirtualBank(chain, listAgent);
        // Currently in effect
        Map<String, VirtualBankDirector> mapCurrentVirtualBank = chain.getMapVirtualBank();
        // Record the changes in the local bank of the module during the cycle
        VirtualBankTemporaryChangePO virtualBankTemporaryChange = new VirtualBankTemporaryChangePO();
        boolean isImmediateEffect = false;

        // Count the nodes that have exited (As long as it is not in the latest calculated virtual bank list, Means to exit and execute immediately(Including margin ranking and direct stop consensus nodes))
        for (VirtualBankDirector director : mapCurrentVirtualBank.values()) {
            if (director.getSeedNode()) {
                continue;
            }
            boolean rs = true;
            for (AgentBasic agentBasic : listVirtualBank) {
                // Determine if the current bank members also exist in the latest calculated list of virtual bank qualifications, Or member is a seed node No need to put it in the exit list
                if (director.getAgentAddress().equals(agentBasic.getAgentAddress())) {
                    // Nodes that do not require exit execution
                    rs = false;
                }
            }
            // Check if it is in the disqualification list
            String addr = disqualificationStorageService.find(chain, AddressTool.getAddress(director.getAgentAddress()));
            if (rs || StringUtils.isNotBlank(addr)) {
                //Indicates that it is no longer a virtual bank node, Need to immediately publish virtual bank change transactions
                virtualBankTemporaryChange.getListOutAgents().add(AddressTool.getAddress(director.getAgentAddress()));

                /**
                 * It needs to be determined based on the address of the exiting node, Go get the nodehash, Retrieve node information again
                 * If there is a node exit height, it indicates that the node has exited consensus,Directly take the height of the exit node; Otherwise, use the current height
                 * nodehashMust exist in the virtual bank list, Need to pass through ConsensusCall.getAgentInfo(chain, latestBasicBlock.getHeight());Obtain
                 */
                String agentHash = director.getAgentHash();
                long delHeight = ConsensusCall.agentDelHeight(chain, agentHash);
                virtualBankTemporaryChange.setOutHeight(delHeight);
                if (delHeight > 0L) {
                    // Explanation: Yesstopagentnode
                    BlockHeader blockHeader = BlockCall.getBlockHeader(chain, delHeight);
                    if (null == blockHeader) {
                        chain.getLogger().error("Check virtual bank changes, according tostopAgentAbnormal height acquisition block header, height:{},", delHeight);
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
            // If there are no nodes that have exited, And currently, it has not reached the periodic triggering height
            return null;
        }
        // Statistics of added nodes
        for (AgentBasic agentBasic : listVirtualBank) {
            // Determine if the node does not exist in the currently effective virtual bank, Indicates that the node is a newly added node
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
            // Indicates that the virtual bank members have not changed
            chain.getLogger().info("[Check] Virtual bank change check completed, Virtual bank members remain unchanged!");
            return null;
        }
        return virtualBankTemporaryChange;
    }

    /**
     * Determine whether the balance of heterogeneous addresses corresponding to node block addresses meets the conditions
     *
     * @param agentBasic
     * @return satisfy:true, Not satisfied:false
     */
    @Deprecated
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
                    // Non main assets, No verification required
                    continue;
                }
                if (cfg.getChainId() == hInterface.getChainId() && cfg.getInitialBalance().compareTo(balance) < 0) {
                    rs = true;
                }
            }
            // All heterogeneous chain addresses need to meet the conditions
            if (!rs) {
                chain.getLogger().warn("The agent heterogeneous address insufficient balance, cannot join virtual bank. agentAddress:{}, packingAddress:{}",
                        agentBasic.getAgentAddress(), agentBasic.getPackingAddress());
                return false;
            }
        }
        return true;
    }

    /**
     * Based on the latest sorted consensus list Count the latest members with virtual banking qualifications
     *
     * @param listAgent
     * @return
     */
    @Override
    public List<AgentBasic> calcNewestVirtualBank(Chain chain, List<AgentBasic> listAgent) {
        List<AgentBasic> listVirtualBank = new ArrayList<>();
        // Exclude seed nodes
        for (AgentBasic agentBasic : listAgent) {
            // Exclude node addresses that have been disqualified from virtual banking
            String disqualificationAddress = disqualificationStorageService.find(chain, AddressTool.getAddress(agentBasic.getAgentAddress()));

            /* // Exclude nodes with heterogeneous address balances corresponding to block addresses that do not meet the initial value
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
         * There are multiple nodes with the same margin at the same time,And meet the conditions for joining virtual banking,
         * If all are added, it will exceed the threshold for the total number of banks Sorting is required to determine the final bank members
         * Retrieve the nodes ranked at the threshold of the total number of virtual banks, Retrieve nodes from the list that have the same margin as them
         * By the order of the current bank,The current sorted node list is used to determine the final order
         */
        AgentBasic thresholdAgentBasic = listVirtualBank.get(VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED - 1);
        BigInteger thresholdDeposit = thresholdAgentBasic.getDeposit();

        // Node after threshold
        AgentBasic afterThresholdAgentBasic = listVirtualBank.get(VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED);
        BigInteger afterThresholdDeposit = afterThresholdAgentBasic.getDeposit();

        if (thresholdDeposit.compareTo(afterThresholdDeposit) != 0) {
            // The margin of the node at the threshold position is different from the margin of the subsequent node, Take directly before VIRTUAL_BANK_AGENT_NUMBER New virtual bank qualification member list for node members
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
                    // If not a member of a virtual bank, thenindexFor the total number of banks+Current index Ensure current order, But ranking behind the current virtual banking node
                    agentBasic.setIndex(VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED + INITIAL_VIRTUAL_BANK_SEED_COUNT + i + 1);
                } else {
                    // If it is a virtual bank, then Using sorting values from virtual banks
                    agentBasic.setIndex(director.getOrder());
                }
                sameDeposit.add(agentBasic);
            }
            if (thresholdDeposit.compareTo(agentBasic.getDeposit()) < 0) {
                listFinalVirtualBank.add(agentBasic);
            }
        }
        // sort
        sameDeposit.sort((Comparator.comparingInt(AgentBasic::getIndex)));
        int gap = VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED - listFinalVirtualBank.size();
        listFinalVirtualBank.addAll(sameDeposit.subList(0, gap));
        return listFinalVirtualBank;
    }

    /**
     * Add all seed nodes as virtual bank members
     * @param chain
     * @param listAgent
     */
    private void addBankDirector(Chain chain, List<AgentBasic> listAgent){
        SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        List<VirtualBankDirector> listInDirector = new ArrayList<>();
        for (AgentBasic agentBasic : listAgent) {
            if (agentBasic.getSeedNode()) {
                VirtualBankDirector virtualBankDirector = new VirtualBankDirector();
                // Seed node packaging address,Node address Reward Address Set as consistent
                virtualBankDirector.setAgentHash(agentBasic.getAgentHash());
                virtualBankDirector.setAgentAddress(agentBasic.getPackingAddress());
                virtualBankDirector.setSignAddress(agentBasic.getPackingAddress());
                virtualBankDirector.setRewardAddress(agentBasic.getPackingAddress());
                virtualBankDirector.setSignAddrPubKey(agentBasic.getPubKey());
                virtualBankDirector.setSeedNode(agentBasic.getSeedNode());
                virtualBankDirector.setHeterogeneousAddrMap(new HashMap<>(ConverterConstant.INIT_CAPACITY_8));
                listInDirector.add(virtualBankDirector);
                // If it is currently a consensus node, Determine whether the current member is a virtual bank
                if (null != signAccountDTO && agentBasic.getPackingAddress().equals(signAccountDTO.getAddress())) {
                    chain.getCurrentIsDirector().set(true);
                    chain.getLogger().info("[Virtual banking] The current node is involved in virtual banking,Identification changed to: true");
                }
            }
        }
        // add by Mimi at 2020-05-06 Update when joining virtual banking[virtualBankDirector]stayDBStorage and order in memory
        VirtualBankUtil.virtualBankAdd(chain, chain.getMapVirtualBank(), listInDirector, virtualBankStorageService);
        // end code by Mimi
    }

    /**
     * Add the configured seed node as a virtual bank member
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
                    //try {
                    //    chain.getLogger().warn("pierre test===2 chain info: {}, {}", chain.getCurrentHeterogeneousVersion(), Arrays.toString(ConverterContext.INIT_VIRTUAL_BANK_PUBKEY_LIST.toArray()));
                    //    chain.getLogger().warn("pierre test===2 current virtualBankMap: {}", JSONUtils.obj2json(chain.getMapVirtualBank()));
                    //    chain.getLogger().warn("pierre test===2 remove sign address: {}", agentBasic.getPackingAddress());
                    //} catch (Exception e) {
                    //    chain.getLogger().warn("MapVirtualBank log print error ");
                    //}
                    VirtualBankDirector virtualBankDirector = new VirtualBankDirector();
                    // Seed node packaging address,Node address Reward Address Set as consistent
                    virtualBankDirector.setAgentHash(agentBasic.getAgentHash());
                    virtualBankDirector.setAgentAddress(agentBasic.getPackingAddress());
                    virtualBankDirector.setSignAddress(agentBasic.getPackingAddress());
                    virtualBankDirector.setRewardAddress(agentBasic.getPackingAddress());
                    virtualBankDirector.setSignAddrPubKey(agentBasic.getPubKey());
                    virtualBankDirector.setSeedNode(agentBasic.getSeedNode());
                    virtualBankDirector.setHeterogeneousAddrMap(new HashMap<>(ConverterConstant.INIT_CAPACITY_8));
                    listInDirector.add(virtualBankDirector);
                    // If it is currently a consensus node, Determine whether the current member is a virtual bank
                    if (null != signAccountDTO && agentBasic.getPackingAddress().equals(signAccountDTO.getAddress())) {
                        chain.getCurrentIsDirector().set(true);
                        chain.getLogger().info("[Virtual banking] The current node is involved in virtual banking,Identification changed to: true");
                    }
                }
            }
        }
        // add by Mimi at 2020-05-06 Update when joining virtual banking[virtualBankDirector]stayDBStorage and order in memory
        VirtualBankUtil.virtualBankAdd(chain, chain.getMapVirtualBank(), listInDirector, virtualBankStorageService);
        // end code by Mimi
    }

    /**
     * Initialize all seed nodes as virtual banks
     *
     * @param chain
     * @param listAgent
     */
    private void initVirtualBank(Chain chain, List<AgentBasic> listAgent, long height) throws NulsException {

        // If the virtual bank is empty Initialize the seed node as Virtual Bank Festival Members
        if (chain.getMapVirtualBank().isEmpty()) {
            if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_1) {
                addBankDirector(chain,  listAgent);
            } else if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_2) {
                addBankDirectorBySetting( chain, listAgent);
            }
        }
        // If virtual bank members Initialize heterogeneous chain addresses
        List<IHeterogeneousChainDocking> hInterfaces = new ArrayList<>(heterogeneousDockingManager.getAllHeterogeneousDocking());
        if (null == hInterfaces || hInterfaces.isEmpty()) {
            chain.getLogger().error("Heterogeneous chain component is empty");
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST);
        }
        // Adjusting heterogeneous chain components,Perform initialization
        for (VirtualBankDirector director : chain.getMapVirtualBank().values()) {
           /* 2020/11/19
           if (!director.getSeedNode()) {
                // Non seed nodes Not initialized, Unable to obtain heterogeneous chain address
                continue;
            }*/
            boolean save = false;
            for (IHeterogeneousChainDocking hInterface : hInterfaces) {
                if (!director.getHeterogeneousAddrMap().containsKey(hInterface.getChainId())) {
                    // Create new heterogeneous chain multi sign addresses for new members
                    String heterogeneousAddress = hInterface.generateAddressByCompressedPublicKey(director.getSignAddrPubKey());
                    director.getHeterogeneousAddrMap().put(hInterface.getChainId(),
                            new HeterogeneousAddress(hInterface.getChainId(), heterogeneousAddress));
                    save = true;
                    chain.getLogger().info("[Initialize heterogeneous chain multi signature addresses] Node address:{}, isomerismid:{}, Heterogeneous addresses:{}",
                            director.getAgentAddress(), hInterface.getChainId(), heterogeneousAddress);
                }
            }
            if (save) {
                virtualBankStorageService.save(chain, director);
                virtualBankAllHistoryStorageService.save(chain, director);
            }
        }
        // Send to consensus
        ConsensusCall.sendVirtualBank(chain, height);
        try {
            chain.getLogger().info("[initialization]Virtual banking and heterogeneous chain initialization completed - MapVirtualBank : {}", JSONUtils.obj2json(chain.getMapVirtualBank()));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

    }

    /**
     * Execute the creation and publication of virtual bank change transactions
     *
     * @param chain
     * @param vbankChange
     * @param txTime      The current block time at high altitude serves as the transaction time for virtual bank changes
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

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

import static network.nerve.converter.constant.ConverterConstant.HETEROGENEOUS_VERSION_1;
import static network.nerve.converter.constant.ConverterConstant.HETEROGENEOUS_VERSION_2;

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
     * Add nodes
     * 1.Is it a consensus node
     * 2.Is the margin ranking among the top10
     * 3.Not the current virtual bank member
     * <p>
     * Exit node
     * 1.Not a consensus node, And it is currently a member of the virtual bank
     * 2.It is a consensus node
     * 2-1.Judging whether the margin is placed before the common consensus node10
     * 2-2.Is the current virtual bank member
     *
     * @param chainId     chainId
     * @param txs         Type is{@link #getType()}All transaction sets for
     * @param txMap       Different transaction types and their corresponding transaction list key value pairs
     * @param blockHeader Block head
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
            if (null != blockHeader) {
                listAgent = ConsensusCall.getAgentList(chain, blockHeader.getHeight());
            } else {
                listAgent = ConsensusCall.getAgentList(chain);
            }
            // The latest qualified individuals to become virtual banks(Non seed)node
            List<AgentBasic> listNewestVirtualBank = virtualBankService.calcNewestVirtualBank(chain, listAgent);
            // Check for duplicate transactions within the block business
            Set<String> setDuplicate = new HashSet<>();
            // Address duplicate check, All change transactions within the same blockinandoutThe same address cannot appear inside
            Set<String> addressDuplicate = new HashSet<>();
            outer:
            for (Transaction tx : txs) {
                byte[] coinData = tx.getCoinData();
                if (coinData != null && coinData.length > 0) {
                    // coindataExisting data(coinDataThere should be no data available)
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.COINDATA_CANNOT_EXIST.getCode();
                    log.error(ConverterErrorCode.COINDATA_CANNOT_EXIST.getMsg());
                    continue;
                }
                // Determine duplicate within the block
                String txDataHex = HexUtil.encode(tx.getTxData());
                if (setDuplicate.contains(txDataHex)) {
                    // Repeated transactions within the block
                    failsList.add(tx);
                    errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                    log.error(ConverterErrorCode.TX_DUPLICATION.getMsg());
                    continue;
                }
                ChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ChangeVirtualBankTxData.class);
                // Address duplicate check
                if (null != txData.getInAgents()) {
                    for (byte[] addressBytes : txData.getInAgents()) {
                        if (addressDuplicate.contains(AddressTool.getStringAddressByBytes(addressBytes))) {
                            // Repeated transactions within the block
                            failsList.add(tx);
                            errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                            log.error("[ChangeVirtualBank] txData has duplication address. hash:{}", tx.getHash().toHex());
                            continue;
                        }
                    }
                }
                if (null != txData.getOutAgents()) {
                    for (byte[] addressBytes : txData.getOutAgents()) {
                        if (addressDuplicate.contains(AddressTool.getStringAddressByBytes(addressBytes))) {
                            // Repeated transactions within the block
                            failsList.add(tx);
                            errorCode = ConverterErrorCode.TX_DUPLICATION.getCode();
                            log.error("[ChangeVirtualBank] txData has duplication address. hash:{}", tx.getHash().toHex());
                            continue;
                        }
                    }
                }


                // Verify exiting virtual banking business
                List<byte[]> listOutAgents = txData.getOutAgents();
                int listOutAgentsSize = 0;
                if (null != listOutAgents && !listOutAgents.isEmpty()) {
                    listOutAgentsSize = listOutAgents.size();
                    for (byte[] addressBytes : listOutAgents) {
                        String agentAddress = AddressTool.getStringAddressByBytes(addressBytes);
                        // Judging that it is not a member of a virtual bank
                        if (!chain.isVirtualBankByAgentAddr(agentAddress)) {
                            failsList.add(tx);
                            // Currently not a virtual bank node, There is no exit situation
                            errorCode = ConverterErrorCode.AGENT_IS_NOT_VIRTUAL_BANK.getCode();
                            log.error(ConverterErrorCode.AGENT_IS_NOT_VIRTUAL_BANK.getMsg());
                            chain.getLogger().error("The node itself is not a member of a virtual bank, There is no exit situation, agentAddress:{}", agentAddress);
                            continue outer;
                        }
                        // Determine if it is not a seed node
                        VirtualBankDirector director = chain.getDirectorByAgent(agentAddress);
                        if (director.getSeedNode()) {
                            failsList.add(tx);
                            // Seed node cannot exit virtual bank
                            errorCode = ConverterErrorCode.CAN_NOT_QUIT_VIRTUAL_BANK.getCode();
                            chain.getLogger().error("Seed node cannot exit virtual bank, agentAddress:{}", agentAddress);
                            continue outer;
                        }
                        if (StringUtils.isNotBlank(disqualificationStorageService.find(chain, addressBytes))) {
                            // The address exists in the revocation bank qualification list, Verified through direct revocation.
                            continue;
                        }
                        if (!isQuitVirtualBank(chain, listAgent, listNewestVirtualBank, agentAddress)) {
                            failsList.add(tx);
                            long h = null == blockHeader ? -1 : blockHeader.getHeight();
                            log.error(h + " - orginalListAgent: " + JSONUtils.obj2json(listAgent) + ", listNewestVirtualBank: " + JSONUtils.obj2json(listNewestVirtualBank));
                            log.error(h + "- MapVirtualBank: " + JSONUtils.obj2json(chain.getMapVirtualBank()));
                            // Not meeting the conditions for exiting the virtual bank node
                            errorCode = ConverterErrorCode.CAN_NOT_QUIT_VIRTUAL_BANK.getCode();
                            log.error(ConverterErrorCode.CAN_NOT_QUIT_VIRTUAL_BANK.getMsg());
                            continue outer;
                        }
                    }
                }

                // Verify joining virtual banking node business
                List<byte[]> listInAgents = txData.getInAgents();
                if (null != listInAgents && !listInAgents.isEmpty()) {
                    //  Determine if joining causes virtual bank members to exceed the limit
                    int countVirtualBank = chain.getVirtualBankCountWithoutSeedNode();
                    if (countVirtualBank - listOutAgentsSize + listInAgents.size() > ConverterContext.VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED) {
                        failsList.add(tx);
                        // The current situation has caused the number of non seed nodes in the virtual bank to exceed the maximum value
                        errorCode = ConverterErrorCode.VIRTUAL_BANK_OVER_MAXIMUM.getCode();
                        log.error(ConverterErrorCode.VIRTUAL_BANK_OVER_MAXIMUM.getMsg());
                        continue outer;
                    }
                    for (byte[] addressBytes : listInAgents) {
                        String agentAddress = AddressTool.getStringAddressByBytes(addressBytes);
                        //Judging as a virtual bank member
                        if (chain.isVirtualBankByAgentAddr(agentAddress)) {
                            failsList.add(tx);
                            // Currently a virtual bank node, There is no situation of joining
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
                            // Not meeting the conditions to enter virtual banking
                            errorCode = ConverterErrorCode.CAN_NOT_JOIN_VIRTUAL_BANK.getCode();
                            log.error(ConverterErrorCode.CAN_NOT_JOIN_VIRTUAL_BANK.getMsg());
                            continue outer;
                        }
                    }
                }

                // Verification signature
                try {
                    ConverterSignValidUtil.validateVirtualBankSign(chain, tx);
                } catch (NulsException e) {
                    failsList.add(tx);
                    errorCode = e.getErrorCode().getCode();
                    log.error(e.getErrorCode().getMsg());
                    continue outer;
                }
                // Include all addresses Place duplicate check set
                if (null != txData.getInAgents()) {
                    for (byte[] addressBytes : txData.getInAgents()) {
                        addressDuplicate.add(AddressTool.getStringAddressByBytes(addressBytes));
                    }
                }
                if (null != txData.getOutAgents()) {
                    for (byte[] addressBytes : txData.getOutAgents()) {
                        addressDuplicate.add(AddressTool.getStringAddressByBytes(addressBytes));
                    }
                }
                // taketxData hex Place duplicate check set
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
     * Determine whether the node should enter the virtual bank
     *
     * @param listAgent
     * @param agentAddress
     * @return
     */
    private boolean isJoinVirtualBank(Chain chain, List<AgentBasic> listAgent, List<AgentBasic> listNewestVirtualBank, String agentAddress) {
        for (int i = 0; i < listAgent.size(); i++) {
            AgentBasic agentBasic = listAgent.get(i);
            //Determine if it is a non seed block consensus node
            if (!agentBasic.getSeedNode()
                    && agentBasic.getAgentAddress().equals(agentAddress)) {
                if (StringUtils.isBlank(agentBasic.getPubKey())) {
                    chain.getLogger().error("Node public key is empty, Cannot join virtual bank without block output record, agentAddress:{}", agentAddress);
                    return false;
                }
                byte[] pubKeyAddrByte = AddressTool.getAddressByPubKeyStr(agentBasic.getPubKey(), chain.getChainId());
                String pubKeyAddr = AddressTool.getStringAddressByBytes(pubKeyAddrByte);
                if (!pubKeyAddr.equals(agentBasic.getPackingAddress())) {
                    //The address generated by the public key does not match the outgoing address, indicating that it is invalid
                    chain.getLogger().error("Node public key error, does not match block address, cannot join virtual bank, agentAddress:{}", agentAddress);
                    return false;
                }
                //Determine if the ranking is within the designated ranking of the virtual bank
            }
        }

        for (int i = 0; i < listNewestVirtualBank.size(); i++) {
            AgentBasic agentBasic = listNewestVirtualBank.get(i);
            if (agentAddress.equals(agentBasic.getAgentAddress())) {
                return true;
            }
        }
        chain.getLogger().error("[ChangeVirtualBankProcessor-validate-isJoinVirtualBank] Currently not eligible for virtual banking, Cannot join virtual bank. agentAddress:{}", agentAddress);
        return false;
    }

    /**
     * Node for exiting the bank
     * 1.Not a consensus node, And it is currently a member of the virtual bank
     * 2.It is a consensus node
     * 2-1.Judging whether the margin is placed before the common consensus node10
     * 2-2.Is the current virtual bank member
     *
     * @param listAgent
     * @param agentAddress
     * @return
     */
    private boolean isQuitVirtualBank(Chain chain, List<AgentBasic> listAgent, List<AgentBasic> listNewestVirtualBank, String agentAddress) {
        for (int i = 0; i < listAgent.size(); i++) {
            AgentBasic agentBasic = listAgent.get(i);
            //Determine whether it is a block consensus node
            if (agentBasic.getAgentAddress().equals(agentAddress)) {
                // If the public key is empty, it means that no block has been generated, Should exit virtual banking
                if (StringUtils.isBlank(agentBasic.getPubKey())) {
                    return true;
                }
                byte[] pubKeyAddrByte = AddressTool.getAddressByPubKeyStr(agentBasic.getPubKey(), chain.getChainId());
                String pubKeyAddr = AddressTool.getStringAddressByBytes(pubKeyAddrByte);
                if (!pubKeyAddr.equals(agentBasic.getPackingAddress())) {
                    //The address generated by the public key does not match the outgoing address, indicating that it is invalid Should exit
                    return true;
                }
            }
        }
        for (int i = 0; i < listNewestVirtualBank.size(); i++) {
            AgentBasic agentBasic = listNewestVirtualBank.get(i);
            if (agentAddress.equals(agentBasic.getAgentAddress())) {
                chain.getLogger().error("Currently still eligible for virtual banking,Cannot exit virtual bank, Margin:{}, Ranking:{}", agentBasic.getDeposit(), ConverterContext.INITIAL_VIRTUAL_BANK_SEED_COUNT + i + 1);
                return false;
            }
        }

        // Should exit
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
     * Join virtual banking node
     *
     * @param chain
     * @param listInAgents
     * @param listCurrentAgent
     * @throws NulsException
     */
    private List<VirtualBankDirector> processSaveVirtualBank(Chain chain, List<byte[]> listInAgents, List<AgentBasic> listCurrentAgent) throws NulsException {
        List<VirtualBankDirector> listInDirector = new ArrayList<>();
        if (null == listInAgents || listInAgents.isEmpty()) {
            chain.getLogger().info("[commit] No nodes joined the virtual bank");
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
            chain.getLogger().info("[commit] Node Joins Virtual Bank, packing:{}, agent:{}", packingAddr, agentAddress);
            listInDirector.add(virtualBankDirector);
        }
        // add by Mimi at 2020-05-06 Update when joining virtual banking[virtualBankDirector]stayDBStorage and order in memory
        VirtualBankUtil.virtualBankAdd(chain, chain.getMapVirtualBank(), listInDirector, virtualBankStorageService);
        // end code by Mimi
        return listInDirector;
    }

    /**
     * Remove virtual bank node
     *
     * @param chain
     * @param listAgents
     * @throws NulsException
     */
    private List<VirtualBankDirector> processRemoveVirtualBank(Chain chain, List<byte[]> listAgents) throws NulsException {
        List<VirtualBankDirector> listOutDirector = new ArrayList<>();
        if (null == listAgents || listAgents.isEmpty()) {
            chain.getLogger().info("[commit] No node exits virtual bank");
            return listOutDirector;
        }
        for (byte[] addressBytes : listAgents) {
            String agentAddress = AddressTool.getStringAddressByBytes(addressBytes);
            VirtualBankDirector director = chain.getDirectorByAgent(agentAddress);
            listOutDirector.add(director);
            String directorSignAddress = director.getSignAddress();
            try {
                chain.getLogger().debug("[Exit Bank Node Information]:{}", JSONUtils.obj2PrettyJson(director));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            chain.getLogger().info("[commit] Node exits virtual bank, packing:{}, agent:{}", directorSignAddress, agentAddress);
        }
        // add by Mimi at 2020-05-06 Update order when removing
        VirtualBankUtil.virtualBankRemove(chain, chain.getMapVirtualBank(), listOutDirector, virtualBankStorageService);
        // end code by Mimi
        return listOutDirector;
    }


    /**
     * Check all virtual bank members to create multiple signatures/Contract address, Supplement information such as creating an address
     **/
    private void createNewHeterogeneousAddress(Chain chain, List<IHeterogeneousChainDocking> hInterfaces) {
        Collection<VirtualBankDirector> allDirectors = chain.getMapVirtualBank().values();
        if (null != allDirectors && !allDirectors.isEmpty()) {
            for (VirtualBankDirector director : allDirectors) {
                boolean save = false;
                for (IHeterogeneousChainDocking hInterface : hInterfaces) {
                    if (director.getHeterogeneousAddrMap().containsKey(hInterface.getChainId())) {
                        // Do not create heterogeneous addresses if they exist
                        continue;
                    }
                    // Create heterogeneous chain multi sign addresses for new members
                    String heterogeneousAddress = hInterface.generateAddressByCompressedPublicKey(director.getSignAddrPubKey());
                    director.getHeterogeneousAddrMap().put(hInterface.getChainId(),
                            new HeterogeneousAddress(hInterface.getChainId(), heterogeneousAddress));
                    save = true;
                    chain.getLogger().info("[Create heterogeneous chain addresses for newly added nodes] Node address:{}, isomerismid:{}, Heterogeneous addresses:{}",
                            director.getAgentAddress(), hInterface.getChainId(), director.getHeterogeneousAddrMap().get(hInterface.getChainId()));
                }
                if (save) {
                    virtualBankStorageService.save(chain, director);
                    virtualBankAllHistoryStorageService.save(chain, director);
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
                //Maintain virtual banking
                ChangeVirtualBankTxData txData = ConverterUtil.getInstance(tx.getTxData(), ChangeVirtualBankTxData.class);
                List<byte[]> listOutAgents = txData.getOutAgents();
                List<VirtualBankDirector> listOutDirector =
                        processRemoveVirtualBank(chain, listOutAgents);
                List<byte[]> listInAgents = txData.getInAgents();
                List<VirtualBankDirector> listInDirector =
                        processSaveVirtualBank(chain, listInAgents, listAgent);

                boolean currentJoin = false;
                boolean currentQuit = false;
                // The current node is a virtual bank member identifier
                boolean currentDirector = VirtualBankUtil.isCurrentDirector(chain);
                VirtualBankDirector currentQuitDirector = null;
                if (currentDirector) {
                    // If the current node is identified as a virtual bank, Then determine whether the current node is an exit virtual bank node
                    for (VirtualBankDirector director : listOutDirector) {
                        if (director.getSignAddress().equals(signAccountDTO.getAddress())) {
                            // Current node exits as a virtual bank member
                            currentDirector = false;
                            currentQuit = true;
                            currentQuitDirector = director;
                            chain.getCurrentIsDirector().set(false);
                            chain.getLogger().info("[Virtual banking] Current node exits virtual bank,Identification changed to: false");
                            break;
                        }
                    }
                } else {
                    if (null != signAccountDTO) {
                        // If the current node is not a virtual bank, It is a consensus node, Then determine whether the current node is a newly added virtual bank node
                        for (VirtualBankDirector director : listInDirector) {
                            if (director.getSignAddress().equals(signAccountDTO.getAddress())) {
                                // Current node joined as a virtual bank member
                                chain.getCurrentIsDirector().set(true);
                                currentJoin = true;
                                currentDirector = true;
                                // Heterogeneous chain component registration of current node signature address
                                virtualBankService.initLocalSignPriKeyToHeterogeneous(chain, signAccountDTO);
                                /**
                                 * Currently a newly joined virtual bank member, By default, set the heterogeneous chain call flag totrue, Automatically restore when confirming change transactions.
                                 * Prevent this node, Subsequent heterogeneous transactions result in inconsistency between contracts and in chain data
                                 */
                                heterogeneousService.saveExeHeterogeneousChangeBankStatus(chain, true);
                                chain.getLogger().info("[Virtual banking] Current node joining virtual bank,Identification changed to: true");
                                break;
                            }
                        }
                    }
                }
                List<IHeterogeneousChainDocking> hInterfaces = new ArrayList<>(heterogeneousDockingManager.getAllHeterogeneousDocking());
                if (null == hInterfaces || hInterfaces.isEmpty()) {
                    throw new NulsException(ConverterErrorCode.HETEROGENEOUS_COMPONENT_NOT_EXIST);
                }
                // Add heterogeneous chain address information
                createNewHeterogeneousAddress(chain, hInterfaces);

                // Record current transaction Total Byzantine Heterogeneous Chains (Not including current joining, To calculate the current exit)
                int inTotal = null == txData.getInAgents() ? 0 : txData.getInAgents().size();
                int outTotal = null == txData.getOutAgents() ? 0 : txData.getOutAgents().size();
                int currenVirtualBankTotal = chain.getMapVirtualBank().size() - inTotal + outTotal;
                if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_1) {
                    // Synchronous mode does not require virtual bank multi signature execution/Change of Contract Members, Only those who are currently administrators and not joined by the current transaction
                    if (SyncStatusEnum.getEnum(syncStatus).equals(SyncStatusEnum.RUNNING) && currentDirector && !currentJoin) {
                        // Insert heterogeneous chain processing mechanism
                        TxSubsequentProcessPO pendingPO = new TxSubsequentProcessPO();
                        pendingPO.setTx(tx);
                        pendingPO.setListInDirector(listInDirector);
                        pendingPO.setListOutDirector(listOutDirector);
                        pendingPO.setBlockHeader(blockHeader);
                        pendingPO.setSyncStatusEnum(SyncStatusEnum.getEnum(syncStatus));
                        pendingPO.setCurrentJoin(currentJoin);
                        pendingPO.setCurrentDirector(currentDirector);
                        pendingPO.setCurrenVirtualBankTotal(currenVirtualBankTotal);
                        txSubsequentProcessStorageService.save(chain, pendingPO);
                        chain.getPendingTxQueue().offer(pendingPO);
                    }
                } else if (chain.getCurrentHeterogeneousVersion() == HETEROGENEOUS_VERSION_2) {
                    // Synchronous mode does not require virtual bank multi signature execution/Change of Contract Members, Currently an administrator(Can be the currently joined) Or currently exiting
                    if (SyncStatusEnum.getEnum(syncStatus).equals(SyncStatusEnum.RUNNING) && (currentDirector || currentQuit)) {
                        // Insert heterogeneous chain processing mechanism
                        TxSubsequentProcessPO pendingPO = new TxSubsequentProcessPO();
                        pendingPO.setTx(tx);
                        pendingPO.setListInDirector(listInDirector);
                        pendingPO.setListOutDirector(listOutDirector);
                        pendingPO.setBlockHeader(blockHeader);
                        pendingPO.setSyncStatusEnum(SyncStatusEnum.getEnum(syncStatus));
                        pendingPO.setCurrentJoin(currentJoin);
                        pendingPO.setCurrentQuit(currentQuit);
                        pendingPO.setCurrentQuitDirector(currentQuitDirector);
                        pendingPO.setCurrentDirector(currentDirector);
                        pendingPO.setCurrenVirtualBankTotal(currenVirtualBankTotal);
                        txSubsequentProcessStorageService.save(chain, pendingPO);
                        chain.getPendingTxQueue().offer(pendingPO);
                    }
                }
                ConsensusCall.sendVirtualBank(chain, height);
                chain.getLogger().info("[commit]Virtual Bank Change Transaction hash:{}", tx.getHash().toHex());
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
                if (requestHeight < 0L) {
                    requestHeight = 0L;
                }
                List<AgentBasic> listAgent = ConsensusCall.getAgentList(chain, requestHeight);
                //Maintain virtual banking
                List<byte[]> listOutAgents = txData.getOutAgents();
                //Add back the nodes of the exited virtual bank
                List<VirtualBankDirector> listInDirector = processSaveVirtualBank(chain, listOutAgents, listAgent);
                //Remove the added virtual bank
                List<byte[]> listInAgents = txData.getInAgents();
                List<VirtualBankDirector> listOutDirector = processRemoveVirtualBank(chain, listInAgents);

                // The current node is a virtual bank member
                if (VirtualBankUtil.isCurrentDirector(chain)) {
                    // If the current node is a virtual bank, The removed node contains the current node, Set it to exit status
                    for (VirtualBankDirector director : listOutDirector) {
                        if (director.getSignAddress().equals(signAccountDTO.getAddress())) {
                            // Current node exits as a virtual bank member
                            chain.getCurrentIsDirector().set(false);
                            chain.setInitLocalSignPriKeyToHeterogeneous(false);
                            chain.getLogger().info("[Virtual banking] Current node exits virtual bank,Identification changed to: false");
                            break;
                        }
                    }
                } else {
                    if (null != signAccountDTO) {
                        // If the current node is not a virtual bank, The added nodes include the current node, Set it to join status
                        for (VirtualBankDirector director : listInDirector) {
                            if (director.getSignAddress().equals(signAccountDTO.getAddress())) {
                                // Current node joined as a virtual bank member
                                chain.getCurrentIsDirector().set(true);
                                chain.getLogger().info("[Virtual banking] Current node credited to virtual bank,Identification changed to: true");
                                break;
                            }
                        }
                    }
                }
                ConsensusCall.sendVirtualBank(chain, blockHeader.getHeight());
                chain.getLogger().info("[rollback]Virtual Bank Change Transaction hash:{}", tx.getHash().toHex());
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

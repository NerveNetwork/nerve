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

package nerve.network.converter.core.business.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.base.basic.AddressTool;
import nerve.network.converter.config.ConverterContext;
import nerve.network.converter.constant.ConverterConstant;
import nerve.network.converter.constant.ConverterErrorCode;
import nerve.network.converter.core.business.AssembleTxService;
import nerve.network.converter.core.business.VirtualBankService;
import nerve.network.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import nerve.network.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import nerve.network.converter.manager.ChainManager;
import nerve.network.converter.model.bo.*;
import nerve.network.converter.model.dto.SignAccountDTO;
import nerve.network.converter.model.po.VirtualBankTemporaryChangePO;
import nerve.network.converter.rpc.call.AccountCall;
import nerve.network.converter.rpc.call.ConsensusCall;
import nerve.network.converter.storage.VirtualBankStorageService;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.parse.JSONUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Chino
 * @date: 2020-03-13
 */
@Component
public class VirtualBankServiceImpl implements VirtualBankService {

    @Autowired
    private AssembleTxService assembleTxService;
    @Autowired
    private VirtualBankStorageService virtualBankStorageService;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private ChainManager chainManager;

    @Override
    public void recordVirtualBankChanges(Chain chain) {
        LatestBasicBlock latestBasicBlock = chain.getLatestBasicBlock();
        VirtualBankTemporaryChangePO virtualBankTemporaryChange = baseCheckVirtualBank(chain, latestBasicBlock.getHeight());
        if (null == virtualBankTemporaryChange) {
            return;
        }
        chain.getLogger().info("[虚拟银行变更] 检测到需要执行创建变更交易.. 加入数:{}, 退出数:{}",
                virtualBankTemporaryChange.getListInAgents().size(),
                virtualBankTemporaryChange.getListOutAgents().size());
        effectVirtualBankChangeTx(chain, virtualBankTemporaryChange, latestBasicBlock.getTime());
    }


    @Override
    public VirtualBankDirector getCurrentDirector(int chainId) throws NulsException {
        Chain chain = chainManager.getChain(chainId);
        if (null == chain) {
            throw new NulsException(ConverterErrorCode.CHAIN_NOT_EXIST);
        }
        SignAccountDTO signAccountDTO = ConsensusCall.getPackerInfo(chain);
        if (null == isCurrentDirector(chain, signAccountDTO)) {
            return null;
        }
        return chain.getMapVirtualBank().get(signAccountDTO.getAddress());
    }

    @Override
    public SignAccountDTO isCurrentDirector(Chain chain) throws NulsException {
        return isCurrentDirector(chain, null);
    }
    @Override
    public SignAccountDTO isCurrentDirector(Chain chain, SignAccountDTO signAccountDTO) throws NulsException {
        if(null == signAccountDTO) {
            signAccountDTO = ConsensusCall.getPackerInfo(chain);
        }
        // 如果本节点是共识节点, 并且是虚拟银行成员
        if (null != signAccountDTO && chain.isVirtualBankBySignAddr(signAccountDTO.getAddress())) {
            return signAccountDTO;
        }
        return null;
    }

    @Override
    public void initLocalSignPriKeyToHeterogeneous(Chain chain) throws NulsException {
        initLocalSignPriKeyToHeterogeneous(chain, isCurrentDirector(chain));
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
    private VirtualBankTemporaryChangePO baseCheckVirtualBank(Chain chain, long height) {
        // 最新共识列表
        List<AgentBasic> listAgent = ConsensusCall.getAgentInfo(chain, height);
        if (null == listAgent) {
            chain.getLogger().error("检查虚拟银行变更时, 向共识模块获取共识节点列表数据为null");
            return null;
        }
        int bankNumber = chain.getMapVirtualBank().size();
        /**
         * 判断需要进行初始化
         */
        if (ConverterContext.INIT_VIRTUAL_BANK_HEIGHT >= height && bankNumber == 0) {
            try {
                //初始化种子节点为初始虚拟银行
                initVirtualBank(chain, listAgent);
            } catch (Exception e) {
                // TODO: 2020/3/24  test
                ConverterContext.INIT_VIRTUAL_BANK_HEIGHT += 50;
                chain.getLogger().error(e);
            }
        }

        if(ConverterContext.INIT_VIRTUAL_BANK_HEIGHT >= height && !chain.getInitLocalSignPriKeyToHeterogeneous()){
            try {
                // 向异构链组件,注册地址签名信息
                initLocalSignPriKeyToHeterogeneous(chain);
                chain.setInitLocalSignPriKeyToHeterogeneous(true);
            } catch (NulsException e) {
                chain.getLogger().error(e);
            }
        }

        /**
         * 如果没有开启虚拟银行变更服务就检查是否达到开启条件
         * 达到条件则开启服务
         */
        if (!ConverterContext.ENABLED_VIRTUAL_BANK_CHANGES_SERVICE) {
            if (ConverterContext.AGENT_COUNT_OF_ENABLE_VIRTUAL_BANK_CHANGES <= listAgent.size() && bankNumber > 0) {
                ConverterContext.ENABLED_VIRTUAL_BANK_CHANGES_SERVICE = true;
            }else{
                // 没有达到开启条件 不能触发变更服务 直接返回
                return null;
            }
        }

        // 根据最新共识列表,计算出当前虚拟银行成员(非当前实际生效的虚拟应银行成员)
        List<AgentBasic> listVirtualBank = calcNewestVirtualBank(listAgent);
        //当前已生效的
        Map<String, VirtualBankDirector> mapCurrentVirtualBank = chain.getMapVirtualBank();
        // 记录周期内模块本地银行变化情况
        VirtualBankTemporaryChangePO virtualBankTemporaryChange = new VirtualBankTemporaryChangePO();
        boolean isImmediateEffect = false;
        // 统计退出的节点
        for (VirtualBankDirector director : mapCurrentVirtualBank.values()) {
            if (!listVirtualBank.contains(director) && !director.getSeedNode()) {
                //表示已经不是虚拟银行节点, 需要立即发布虚拟银行变更交易
                virtualBankTemporaryChange.getListOutAgents().add(AddressTool.getAddress(director.getAgentAddress()));
                isImmediateEffect = true;
            }
        }
        boolean immediateEffectHeight = (ConverterContext.LATEST_EXECUTE_CHANGE_VIRTUAL_BANK_HEIGHT +
                ConverterContext.EXECUTE_CHANGE_VIRTUAL_BANK_PERIODIC_HEIGHT) == height;
        if (!isImmediateEffect && !immediateEffectHeight) {
            // 如果没有退出的节点, 并且当前没达到周期性的触发高度
            return null;
        }
        // 统计加入的节点
        for (AgentBasic agentBasic : listVirtualBank) {
            // 判断当前生效的虚拟银行中不存在该节点, 表示该节点是新增节点
            if (!mapCurrentVirtualBank.containsKey(agentBasic.getPackingAddress())) {
                virtualBankTemporaryChange.getListInAgents().add(AddressTool.getAddress(agentBasic.getAgentAddress()));
            }
        }
        if (immediateEffectHeight) {
            //记录当前检查高度
            ConverterContext.LATEST_EXECUTE_CHANGE_VIRTUAL_BANK_HEIGHT = height;
        }
        if (virtualBankTemporaryChange.isBlank()) {
            // 表示虚拟银行成员没有变化
            return null;
        }
        return virtualBankTemporaryChange;
    }

    /**
     * 根据最新的共识列表 统计出最新的有虚拟银行资格的成员
     *
     * @param listAgent
     * @return
     */
    private List<AgentBasic> calcNewestVirtualBank(List<AgentBasic> listAgent) {
        List<AgentBasic> listVirtualBank = new ArrayList<>();
        for (int i = 0; i < listAgent.size(); i++) {
            AgentBasic agentBasic = listAgent.get(i);
            if (!agentBasic.getSeedNode() && listVirtualBank.size() < ConverterContext.VIRTUAL_BANK_AGENT_NUMBER) {
                listVirtualBank.add(agentBasic);
            }
        }
        return listVirtualBank;
    }

    /**
     * 初始化所有种子节点为虚拟银行
     *
     * @param chain
     * @param listAgent
     */
    private void initVirtualBank(Chain chain, List<AgentBasic> listAgent) throws NulsException {
        // 如果虚拟银行为空 将种子节点初始化为 虚拟银行节成员
        if (chain.getMapVirtualBank().isEmpty()) {
            for (AgentBasic agentBasic : listAgent) {
                if (agentBasic.getSeedNode()) {
                    VirtualBankDirector virtualBankDirector = new VirtualBankDirector();
                    // 种子节点打包地址,节点地址 奖励地址 设为一致
                    virtualBankDirector.setAgentAddress(agentBasic.getPackingAddress());
                    virtualBankDirector.setSignAddress(agentBasic.getPackingAddress());
                    virtualBankDirector.setRewardAddress(agentBasic.getPackingAddress());
                    virtualBankDirector.setSignAddrPubKey(agentBasic.getPubKey());
                    virtualBankDirector.setSeedNode(agentBasic.getSeedNode());
                    virtualBankDirector.setHeterogeneousAddrMap(new HashMap<>(ConverterConstant.INIT_CAPACITY_8));
                    chain.getMapVirtualBank().put(agentBasic.getPackingAddress(), virtualBankDirector);
                    virtualBankStorageService.save(chain, virtualBankDirector);
                }
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
                }
            }
        }
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
            assembleTxService.createChangeVirtualBankTx(chain, inAgentList, outAgentList, txTime);
        } catch (NulsException e) {
            chain.getLogger().error(e);
        }
    }


}

/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
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
package network.nerve.converter.core.heterogeneous.callback;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.core.parse.JSONUtils;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.core.heterogeneous.callback.interfaces.IHeterogeneousUpgrade;
import network.nerve.converter.core.heterogeneous.callback.management.CallBackBeanManager;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.dto.SignAccountDTO;
import network.nerve.converter.storage.VirtualBankStorageService;
import network.nerve.converter.utils.VirtualBankUtil;

import java.util.ArrayList;
import java.util.List;

import static network.nerve.converter.config.ConverterContext.INIT_VIRTUAL_BANK_PUBKEY_LIST;

/**
 * After contract upgrade, heterogeneous chain components callCORE, switchdockingexample
 * @author: Mimi
 * @date: 2020-08-31
 */
public class HeterogeneousUpgradeImpl implements IHeterogeneousUpgrade {
    private Chain nerveChain;
    /**
     * Heterogeneous chainchainId
     */
    private int hChainId;
    private HeterogeneousDockingManager heterogeneousDockingManager;
    private VirtualBankStorageService virtualBankStorageService;

    public HeterogeneousUpgradeImpl(Chain nerveChain, int hChainId, CallBackBeanManager callBackBeanManager) {
        this.nerveChain = nerveChain;
        this.hChainId = hChainId;
        this.heterogeneousDockingManager = callBackBeanManager.getHeterogeneousDockingManager();
        this.virtualBankStorageService = callBackBeanManager.getVirtualBankStorageService();
    }

    @Override
    public void switchDocking(IHeterogeneousChainDocking newDocking) {
        nerveChain.getLogger().info("Contract upgrade, call process switching");
        this.heterogeneousDockingManager.registerHeterogeneousDocking(hChainId, newDocking);
        virtualBankUpgradeProcess();
    }

    private void virtualBankUpgradeProcess(){
        nerveChain.setCurrentHeterogeneousVersion(2);
        ConverterContext.INITIAL_VIRTUAL_BANK_SEED_COUNT = INIT_VIRTUAL_BANK_PUBKEY_LIST.size();
        ConverterContext.VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED =
                ConverterContext.VIRTUAL_BANK_AGENT_TOTAL - ConverterContext.INITIAL_VIRTUAL_BANK_SEED_COUNT;

        // Version upgrade Remove non configured seed node members from virtual banks
        //try {
        //    nerveChain.getLogger().warn("pierre test===3 chain info: {}, {}", nerveChain.getCurrentHeterogeneousVersion(), Arrays.toString(ConverterContext.INIT_VIRTUAL_BANK_PUBKEY_LIST.toArray()));
        //    nerveChain.getLogger().warn("pierre test===3 current virtualBankMap: {}", JSONUtils.obj2json(nerveChain.getMapVirtualBank()));
        //} catch (Exception e) {
        //    nerveChain.getLogger().warn("MapVirtualBank log print error ");
        //}
        List<VirtualBankDirector> listOutDirector = new ArrayList<>();
        for(VirtualBankDirector director : nerveChain.getMapVirtualBank().values()) {
            if(!director.getSeedNode()){
                continue;
            }
            boolean rs = false;
            for(String pubkey : INIT_VIRTUAL_BANK_PUBKEY_LIST){
                if(pubkey.equals(director.getSignAddrPubKey())){
                    rs = true;
                }
            }
            if(!rs){
                listOutDirector.add(director);
            }
        }
        for(VirtualBankDirector outDirector : listOutDirector){
            // If the kicked out bank node is the current node, Then modify the status
           if(VirtualBankUtil.isCurrentDirector(nerveChain)){
               SignAccountDTO info = VirtualBankUtil.getCurrentDirectorSignInfo(nerveChain);
               if(outDirector.getSignAddress().equals(info.getAddress())){
                   nerveChain.getCurrentIsDirector().set(false);
               }
           }
        }
        // Update order when removing
        VirtualBankUtil.virtualBankRemove(nerveChain, nerveChain.getMapVirtualBank(), listOutDirector, virtualBankStorageService);
        try {
            nerveChain.getLogger().info("Heterogeneous chain component version switch completed, Current heterogeneous chain version:{}, Current virtual bank members:{}",
                    2, JSONUtils.obj2json(nerveChain.getMapVirtualBank()));
        } catch (JsonProcessingException e) {
            nerveChain.getLogger().warn("MapVirtualBank log print error ");
        }
    }
}

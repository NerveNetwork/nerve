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

package network.nerve.converter.core.thread.task;

import io.nuls.core.core.ioc.SpringLiteContext;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.core.api.ConverterCoreApi;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.LatestBasicBlock;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.dto.HeterogeneousAddressDTO;
import network.nerve.converter.model.dto.VirtualBankDirectorDTO;
import network.nerve.converter.utils.VirtualBankUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author: Mimi
 * @date: 2021-01-12
 */
public class VirtualBankDirectorBalanceTask implements Runnable {

    private Chain chain;
    private HeterogeneousDockingManager heterogeneousDockingManager;
    private ConverterCoreApi converterCoreApi;
    private long flag;

    public VirtualBankDirectorBalanceTask(Chain chain) {
        this.chain = chain;
        this.heterogeneousDockingManager = SpringLiteContext.getBean(HeterogeneousDockingManager.class);
        this.converterCoreApi = SpringLiteContext.getBean(ConverterCoreApi.class);
        this.flag = -1;
    }

    @Override
    public void run() {
        try {
            if (!converterCoreApi.isRunning()) {
                chain.getLogger().debug("忽略同步区块模式");
                return;
            }
            if (!converterCoreApi.isVirtualBankByCurrentNode()) {
                chain.getLogger().debug("非虚拟银行成员，跳过异构链余额缓存的任务");
                return;
            }
            // 每隔150个区块更新一次，计算是否达到更新条件
            LatestBasicBlock latestBasicBlock = chain.getLatestBasicBlock();
            long latestHeight = latestBasicBlock.getHeight();
            long currentFlag = latestHeight / 150;
            do {
                if (flag == -1) {
                    flag = currentFlag;
                    break;
                }
                if (currentFlag > flag) {
                    flag = currentFlag;
                    break;
                }
                return;
            } while (false);
            chain.getLogger().info("按照高度来更新缓存的虚拟银行异构链网络余额, 每隔150个区块统计一次, 当前网络高度: {}", latestHeight);
            Map<String, VirtualBankDirector> mapVirtualBank = chain.getMapVirtualBank();
            List<VirtualBankDirectorDTO> list = new ArrayList<>();
            for (VirtualBankDirector director : mapVirtualBank.values()) {
                VirtualBankDirectorDTO directorDTO = new VirtualBankDirectorDTO(director);
                for (HeterogeneousAddressDTO addr : directorDTO.getHeterogeneousAddresses()) {
                    IHeterogeneousChainDocking heterogeneousDocking = heterogeneousDockingManager.getHeterogeneousDocking(addr.getChainId());
                    String chainSymbol = heterogeneousDocking.getChainSymbol();
                    addr.setSymbol(chainSymbol);
                }
                list.add(directorDTO);
            }
            ConverterContext.VIRTUAL_BANK_DIRECTOR_LIST = list;
            // 并行查询异构链余额
            VirtualBankUtil.virtualBankDirectorBalance(list, chain, heterogeneousDockingManager);
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
    }

}

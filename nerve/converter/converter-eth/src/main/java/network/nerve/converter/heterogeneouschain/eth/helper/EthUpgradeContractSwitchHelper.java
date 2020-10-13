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
package network.nerve.converter.heterogeneouschain.eth.helper;

import io.nuls.core.core.annotation.Component;
import network.nerve.converter.heterogeneouschain.eth.helper.interfaces.IEthUpgrade;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 当动态升级合约交易确认后，会调用注册的升级函数
 * @author: Mimi
 * @date: 2020-08-28
 */
@Component
public class EthUpgradeContractSwitchHelper {

    private List<IEthUpgrade> ethUpgradeList;

    public EthUpgradeContractSwitchHelper() {
        this.ethUpgradeList = new ArrayList<>();
    }

    public void registerUpgrade(IEthUpgrade upgrade) {
        if (upgrade == null) {
            return;
        }
        ethUpgradeList.add(upgrade);
    }

    public void switchProcessor(String newContract) throws Exception {
        if (!ethUpgradeList.isEmpty()) {
            Set<Integer> check = new HashSet<>();
            for (IEthUpgrade upgrade : ethUpgradeList) {
                if (check.add(upgrade.version())) {
                    upgrade.newSwitch(newContract);
                }
            }
        }
    }

}

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
package network.nerve.converter.heterogeneouschain.lib.helper;

import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.helper.interfaces.IHtgUpgrade;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * After confirming the dynamic upgrade contract transaction, the registered upgrade function will be called
 * @author: Mimi
 * @date: 2020-08-28
 */
public class HtgUpgradeContractSwitchHelper implements BeanInitial {

    private List<IHtgUpgrade> upgradeList;

    private HtgContext htgContext;

    //public HtgUpgradeContractSwitchHelper(BeanMap beanMap) {
    //    this.upgradeList = new ArrayList<>();
    //    this.htgContext = (HtgContext) beanMap.get("htgContext");
    //}


    public HtgUpgradeContractSwitchHelper() {
        this.upgradeList = new ArrayList<>();
    }

    public void registerUpgrade(IHtgUpgrade upgrade) {
        if (upgrade == null) {
            return;
        }
        upgradeList.add(upgrade);
    }

    public void switchProcessor(String newContract) throws Exception {
        if (!upgradeList.isEmpty()) {
            Set<Integer> check = new HashSet<>();
            for (IHtgUpgrade upgrade : upgradeList) {
                if (check.add(upgrade.version())) {
                    upgrade.newSwitch(newContract);
                }
            }
        }
    }

}

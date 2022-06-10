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
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Mimi
 * @date: 2020-03-20
 */
public class HtgCommonHelper implements BeanInitial {

    private HtgContext htgContext;

    //public HtgCommonHelper(BeanMap beanMap) {
    //    this.htgContext = (HtgContext) beanMap.get("htgContext");
    //}

    private Set<String> analysisTxHashSet = ConcurrentHashMap.newKeySet();

    public boolean constainHash(String hash) {
        return analysisTxHashSet.contains(hash);
    }

    public boolean addHash(String hash) {
        return analysisTxHashSet.add(hash);
    }

    public void clearHash() {
        analysisTxHashSet.clear();
    }

}

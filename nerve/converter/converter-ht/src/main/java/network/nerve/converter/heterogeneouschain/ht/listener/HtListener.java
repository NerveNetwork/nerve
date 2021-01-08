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
package network.nerve.converter.heterogeneouschain.ht.listener;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: Mimi
 * @date: 2020-02-20
 */
@Component
public class HtListener {
    /**
     * 监听收款交易的指定地址的集合
     */
    private Set<String> listeningAddressSet = ConcurrentHashMap.newKeySet();
    /**
     * 监听指定交易的集合
     */
    private Set<String> listeningTxHashSet = ConcurrentHashMap.newKeySet();

    /**
     * 是否为监听地址
     */
    public boolean isListeningAddress(String address) {
        if (StringUtils.isBlank(address)) {
            return false;
        }
        return listeningAddressSet.contains(address);
    }

    /**
     * 是否为监听交易
     */
    public boolean isListeningTx(String txHash) {
        if (StringUtils.isBlank(txHash)) {
            return false;
        }
        return listeningTxHashSet.contains(txHash);
    }

    /**
     * 增加监听地址
     */
    public void addListeningAddress(String address) {
        listeningAddressSet.add(address);
    }

    /**
     * 移除监听地址
     */
    public void removeListeningAddress(String address) {
        listeningAddressSet.remove(address);
    }

    /**
     * 增加监听交易
     */
    public void addListeningTx(String txHash) {
        listeningTxHashSet.add(txHash);
    }

    /**
     * 移除监听交易
     */
    public void removeListeningTx(String txHash) {
        listeningTxHashSet.remove(txHash);
    }
}

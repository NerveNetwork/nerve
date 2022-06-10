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

package network.nerve.converter.storage;

import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.ComponentCalledPO;

/**
 * 异构链组件/提案执行
 * 执行过的交易, 防止二次执行
 * @author: Loki
 * @date: 2020/6/2
 */
public interface AsyncProcessedTxStorageService {

    boolean saveProposalExe(Chain chain, String hash);

    boolean saveComponentCall(Chain chain, ComponentCalledPO componentCalledPO, boolean currentOut);

    boolean removeComponentCall(Chain chain, String hash);

    String getProposalExe(Chain chain, String hash);

    String getComponentCall(Chain chain, String hash);

    String getCurrentOutHash(Chain chain, String hash);

    ComponentCalledPO getComponentCalledPO(Chain chain, String hash);

}

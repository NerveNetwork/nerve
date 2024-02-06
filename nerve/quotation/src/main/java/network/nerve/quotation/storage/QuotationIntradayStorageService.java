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

package network.nerve.quotation.storage;

import network.nerve.quotation.model.bo.Chain;

import java.util.List;

/**
 * Nodes that have already been priced on the same daytoken, Prevent duplicate quotations
 * Every time the final quotation calculation is executed, Clean up all quotationstoken
 * @author: Loki
 * @date: 2020/6/17
 */
public interface QuotationIntradayStorageService {

    /**
     * Persistent save quotedtoken
     * @param chain
     * @param key
     * @return
     */
    boolean save(Chain chain, String key);

    String get(Chain chain, String key);

    boolean delete(Chain chain, String key);

    List<String> getAll(Chain chain);

    /**
     * Delete all previously quoted pricestoken
     * Generally, the day after deletion is the day before deletion
     * @param chain
     */
    boolean removeAll(Chain chain);
}

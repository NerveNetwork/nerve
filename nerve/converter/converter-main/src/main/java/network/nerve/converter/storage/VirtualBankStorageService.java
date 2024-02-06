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
import network.nerve.converter.model.bo.VirtualBankDirector;

import java.util.Map;

/**
 * @author: Loki
 * @date: 2020-03-03
 */
public interface VirtualBankStorageService {

    /**
     * To save an existing object, Direct useupdatemethod, Otherwise, it would beorderGenerate erroneous data
     *
     * @param chain
     * @param virtualBankDirector
     * @return
     */
    boolean save(Chain chain, VirtualBankDirector virtualBankDirector);

    /**
     * Update ExceptorderAll data except for, If updating a non-existent object, Then directlysaveThis object
     * @param chain
     * @param virtualBankDirector
     * @return
     */
    boolean update(Chain chain, VirtualBankDirector virtualBankDirector);



    VirtualBankDirector findBySignAddress(Chain chain, String address);

    boolean deleteBySignAddress(Chain chain, String address);

    Map<String, VirtualBankDirector> findAll(Chain chain);
}

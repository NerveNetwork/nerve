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

package network.nerve.quotation.processor;

import io.nuls.core.exception.NulsException;
import network.nerve.quotation.model.bo.Chain;

/**
 * Final quotation calculator
 *
 * @author: Loki
 * @date: 2019/12/5
 */
public interface Calculator {

    /**
     * Save the final quotation statistics
     *
     * @param chain
     * @param token
     * @return Whether the bid was successful or not
     * @throws NulsException
     */
    @Deprecated
//    boolean calc(Chain chain, String token);

    /**
     * Statistical final quotation results
     *
     * @param chain
     * @param token
     * @param date Date string format yyyyMMdd
     * @return double price
     * @throws NulsException
     */
    Double calcFinal(Chain chain, String token, String date);
}

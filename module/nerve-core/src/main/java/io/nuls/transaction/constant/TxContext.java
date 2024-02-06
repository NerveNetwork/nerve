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
package io.nuls.transaction.constant;

import java.util.HashSet;
import java.util.Set;

/**
 * @author: Charlie
 * @date: 2020/06/25
 */
public class TxContext {

    /** Maximum transaction data value default300K**/
    public static long TX_MAX_SIZE = 307200L;

    /** Unconfirmed transaction expiration time in seconds */
    public static long UNCONFIRMED_TX_EXPIRE_SEC = 20L;

    /** Orphan Trading Lifetime,Exceeding will be cleared**/
    public static long ORPHAN_LIFE_TIME_SEC = 300L;

    /**The default range value of the block time where the transaction time is located(At block time±Within the range of this value)*/
    public static long BLOCK_TX_TIME_RANGE_SEC = 600L;

    /** coinTo Amount equal to is not supported0 The effective height of the agreement*/
    public static long COINTO_PTL_HEIGHT_FIRST = 0L;

    /** coinTo Support amount equal to0, Only prohibit amounts of0Locked The effective height of the agreement*/
    public static long COINTO_PTL_HEIGHT_SECOND = 0L;

    public static long PROTOCOL_1_18_0 = 0L;
    public static long PROTOCOL_1_19_0 = 0L;
    public static long PROTOCOL_1_20_0 = 0L;
    public static Set<String> ACCOUNT_BLOCK_MANAGER_ADDRESS_SET = new HashSet<>();
    // Minimum number of signatures
    public static int ACCOUNT_BLOCK_MIN_SIGN_COUNT = 0;

}

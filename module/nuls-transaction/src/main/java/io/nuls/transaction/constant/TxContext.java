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

/**
 * @author: Charlie
 * @date: 2020/06/25
 */
public class TxContext {

    /** 交易数据最大值 默认300K**/
    public static long TX_MAX_SIZE = 307200L;

    /** 未确认交易过期时间秒 */
    public static long UNCONFIRMED_TX_EXPIRE_SEC = 20L;

    /** 孤儿交易生命时间,超过会被清理**/
    public static long ORPHAN_LIFE_TIME_SEC = 300L;

    /**交易时间所在区块时间的默认范围值(在区块时间±本值范围内)*/
    public static long BLOCK_TX_TIME_RANGE_SEC = 600L;

    /** coinTo 不支持金额等于0 的协议生效高度*/
    public static long COINTO_PTL_HEIGHT_FIRST = 0L;

    /** coinTo 支持金额等于0, 只禁止金额为0的锁定 的协议生效高度*/
    public static long COINTO_PTL_HEIGHT_SECOND = 0L;

}

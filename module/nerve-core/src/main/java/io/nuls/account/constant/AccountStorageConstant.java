/*
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.nuls.account.constant;

/**
 * @author: qinyifeng
 */
public interface AccountStorageConstant {

    /**
     * The name of the account table
     * The name of the account table
     */
    String DB_NAME_ACCOUNT = "account";
    String DB_NAME_MULTI_SIG_ACCOUNT = "multi_account";

    /**
     * Using alias askeyThe alias table name for
     * The name of the alias table which key is alias
     */
    String DB_NAME_ACCOUNT_ALIAS_KEY_ALIAS = "account_alias_key_alias";

    /**
     * Based on addresskeyThe alias table name for
     * The name of the alias table which key is address
     */
    String DB_NAME_ACCOUNT_ALIAS_KEY_ADDRESS = "account_alias_key_address";

    /**
     * Account Module Configuration Information Table Name
     * Account Module Configuration Information Table Name
     */
    String DB_NAME_ACCOUNT_CONGIF = "config";
    String DB_NAME_ACCOUNT_BLOCK = "account_block";
    String DB_NAME_ACCOUNT_WHITELIST = "account_whitelist";

}

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

package io.nuls.account.storage;

import io.nuls.account.model.po.MultiSigAccountPO;
import io.nuls.base.data.Address;

import java.util.List;

/**
 * Account Data Storage Service Interface
 * Account data storage service interface
 *
 * @author: EdwardChan
 */
public interface MultiSigAccountStorageService {

    /**
     * Create an account
     * save account
     *
     * @return
     */
    boolean saveAccount(MultiSigAccountPO multiSigAccountPo);

    /**
     * Delete account
     * Delete account
     *
     * @param address Account address to be deleted
     * @return the result of the opration
     */
    boolean removeAccount(Address address);

    /**
     * Get all accounts
     *
     * @return the result of the opration and Result<List<Account>>
     */
    List<MultiSigAccountPO> getAccountList();

    /**
     * Obtain account information based on the account
     * According to the account to obtain account information
     *
     * @param address
     * @return the result of the opration
     */
    MultiSigAccountPO getAccount(byte[] address);

}

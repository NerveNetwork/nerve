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
package io.nuls.ledger.model;

import io.nuls.ledger.model.po.AccountState;
import io.nuls.ledger.model.po.sub.AccountStateSnapshot;

/**
 * @author lan
 * @description
 * @date 2019/01/07
 **/
public class AccountBalance {
    private AccountState nowAccountState;
    private AccountStateSnapshot preAccountState;


    public AccountState getNowAccountState() {
        return nowAccountState;
    }

    public void setNowAccountState(AccountState nowAccountState) {
        this.nowAccountState = nowAccountState;
    }

    public AccountStateSnapshot getPreAccountState() {
        return preAccountState;
    }

    public void setPreAccountState(AccountStateSnapshot preAccountState) {
        this.preAccountState = preAccountState;
    }

    public AccountBalance(AccountState nowAccountState, AccountStateSnapshot preAccountState) {
        this.nowAccountState = nowAccountState;
        this.preAccountState = preAccountState;
    }
}

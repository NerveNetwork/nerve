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


import com.google.common.primitives.UnsignedBytes;
import io.nuls.core.crypto.HexUtil;

import java.math.BigInteger;
import java.util.Comparator;

/**
 * @author: qinyifeng
 * @description: Configure Constants
 */
public interface AccountConstant {

    int SIG_MODE_LOCAL = 0;
    int SIG_MODE_MACHINE = 1;

    /**
     * The cost of setting an alias(Destruction)
     * The cost of setting an alias
     */
    BigInteger ALIAS_FEE = BigInteger.valueOf(100000000);

    /**
     * exportaccountkeystoreThe suffix name of the file
     * The suffix of the accountkeystore file
     */
    String ACCOUNTKEYSTORE_FILE_SUFFIX = ".keystore";

    /**
     * SUCCESS_CODE
     */
    String SUCCESS_CODE = "1";

    /**
     * Theme for creating account events
     * topic of account create events
     */
    String EVENT_TOPIC_CREATE_ACCOUNT = "evt_ac_createAccount";

    /**
     * Remove the theme of account events
     * topic of account remove events
     */
    String EVENT_TOPIC_REMOVE_ACCOUNT = "evt_ac_removeAccount";

    /**
     * The theme of the account password modification event
     * topic of update account password events
     */
    String EVENT_TOPIC_UPDATE_PASSWORD = "evt_ac_updatePassword";

    /**
     * MapInitial value
     */
    int INIT_CAPACITY_16 = 16;
    int INIT_CAPACITY_8 = 8;
    int INIT_CAPACITY_4 = 4;
    int INIT_CAPACITY_2 = 2;


    /**
     * Ordinary transactions are non unlocked transactions：0Unlock amount transaction（Exit consensus, exit delegation）：-1
     */
    byte NORMAL_TX_LOCKED = 0;

    Comparator<String> PUBKEY_COMPARATOR = new Comparator<String>() {
        private Comparator<byte[]> COMPARATOR = UnsignedBytes.lexicographicalComparator();

        @Override
        public int compare(String k1, String k2) {
            return COMPARATOR.compare(HexUtil.decode(k1), HexUtil.decode(k2));
        }
    };
    /**
     * Operating System Name
     */
    String OS_NAME = "os.name";
    /**
     * WINDOWSsystem
     */
    String OS_WINDOWS = "WINDOWS";
    /**
     * Path slash
     */
    String SLASH = "/";

}

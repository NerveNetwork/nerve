/*-
 * ⁣⁣
 * MIT License
 * ⁣⁣
 * Copyright (C) 2017 - 2018 nuls.io
 * ⁣⁣
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ⁣⁣
 */
package io.nuls.ledger.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ljs on 2018/11/19.
 *
 * @author lanjinsheng
 */
public class LedgerConstant {
    /**
     * Basic type and contract type
     */
    public static final short COMMON_ASSET_TYPE = 1;
    public static final short CONTRACT_ASSET_TYPE = 2;
    public static final short CROSS_CHAIN_ASSET_TYPE = 3;
    public static final short HETEROGENEOUS_CROSS_CHAIN_ASSET_TYPE = 4;
    public static final short BIND_COMMON_ASSET_TO_HETEROGENEOUS_CROSS_CHAIN_ASSET = 5;
    public static final short BIND_CROSS_CHAIN_ASSET_TO_HETEROGENEOUS_CROSS_CHAIN_ASSET = 6;
    public static final short BIND_COMMON_ASSET_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET = 7;
    public static final short BIND_CROSS_CHAIN_ASSET_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET = 8;
    public static final short BIND_HETEROGENEOUS_CROSS_CHAIN_ASSET_TYPE_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET = 9;
    public static final short SWAP_LIQUIDITY_POOL_CROSS_CHAIN_ASSET_TYPE = 10;
    public static final short BIND_SWAP_ASSET_TO_HETEROGENEOUS_CROSS_CHAIN_ASSET = 11;
    public static final short BIND_SWAP_ASSET_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET = 12;

    /**
     * Decimal Division of Assets
     */
    public static final int DECIMAL_PLACES_MIN = 0;
    public static final int DECIMAL_PLACES_MAX = 18;

    public static int UNCONFIRMED_NONCE = 0;
    public static int CONFIRMED_NONCE = 1;


    /**
     * The threshold for height unlocking, greater than which is the time lock
     */
    public static final int MAX_HEIGHT_VALUE = 1000000000;
    public static final long LOCKED_ML_TIME_VALUE = 1000000000000L;
    /**
     * Recalculate the locked time 1s
     */
    public static final int TIME_RECALCULATE_FREEZE = 1;
    /**
     * FROM locked Unlocking Constants 0 Ordinary transactions,-1 Time unlocking,1 Height unlocking
     */
    public static final int UNLOCKED_COMMON = 0;
    public static final int UNLOCKED_TIME = -1;
    public static final int UNLOCKED_HEIGHT = 1;
    /**
     * To Permanent locklockTimevalue 0 Not locked -1 Normal permanent lock,-2 dexPermanent lock,x Lock time(sorms)
     */
    public static final int PERMANENT_LOCK_COMMON = -1;
    public static final int PERMANENT_LOCK_DEX = -2;


    public static byte[] blackHolePublicKey = null;

    /**
     * Number of cached account blocks
     */
    public static final int CACHE_ACCOUNT_BLOCK = 1000;
    /**
     * Block information of cache synchronization statistics data
     */
    public static final int CACHE_NONCE_INFO_BLOCK = 100;

    /**
     * Cache account initializationnonce
     */

    public static byte[] getInitNonceByte() {
        return new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
    }

    public static final int NONCE_LENGHT = 8;
    public static String DEFAULT_ENCODING = "UTF-8";
    /**
     * Expiration time of unconfirmed transactions-sConfiguration loading will reset this value
     */
    public static int UNCONFIRM_NONCE_EXPIRED_TIME = 100;

    public static final String COMMA = ",";
    public static final String COLON = ":";
    public static final String DOWN_LINE = "_";

    public static final String HEX_PREFIX = "0x";

    public static final Map<Short, Short> CORRESPONDENCE_ASSET_NERVE_HETEROGENEOUS = new HashMap<>();
    public static final Map<Short, Short> CORRESPONDENCE_ASSET_HETEROGENEOUS_NERVE = new HashMap<>();
    static {
        // 1-On chain ordinary assets ==> 5-On chain ordinary assets bound to heterogeneous chain assets
        CORRESPONDENCE_ASSET_NERVE_HETEROGENEOUS.put(COMMON_ASSET_TYPE, BIND_COMMON_ASSET_TO_HETEROGENEOUS_CROSS_CHAIN_ASSET);
        // 3-Parallel chain assets ==> 6-Parallel chain assets bound to heterogeneous chain assets
        CORRESPONDENCE_ASSET_NERVE_HETEROGENEOUS.put(CROSS_CHAIN_ASSET_TYPE, BIND_CROSS_CHAIN_ASSET_TO_HETEROGENEOUS_CROSS_CHAIN_ASSET);
        // 4-Heterogeneous chain assets ==> 9-Binding heterogeneous chain assets to multiple heterogeneous chain assets
        CORRESPONDENCE_ASSET_NERVE_HETEROGENEOUS.put(HETEROGENEOUS_CROSS_CHAIN_ASSET_TYPE, BIND_HETEROGENEOUS_CROSS_CHAIN_ASSET_TYPE_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET);
        // 10-In chainSWAPasset ==> 11-In chainSWAPAsset binding heterogeneous chain assets
        CORRESPONDENCE_ASSET_NERVE_HETEROGENEOUS.put(SWAP_LIQUIDITY_POOL_CROSS_CHAIN_ASSET_TYPE, BIND_SWAP_ASSET_TO_HETEROGENEOUS_CROSS_CHAIN_ASSET);
        // 5-On chain ordinary assets bound to heterogeneous chain assets ==> 7-Binding ordinary assets within the chain to multiple heterogeneous chain assets
        CORRESPONDENCE_ASSET_NERVE_HETEROGENEOUS.put(BIND_COMMON_ASSET_TO_HETEROGENEOUS_CROSS_CHAIN_ASSET, BIND_COMMON_ASSET_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET);
        // 6-Parallel chain assets bound to heterogeneous chain assets ==> 8-Binding Parallel Chain Assets to Multiple Heterogeneous Chain Assets
        CORRESPONDENCE_ASSET_NERVE_HETEROGENEOUS.put(BIND_CROSS_CHAIN_ASSET_TO_HETEROGENEOUS_CROSS_CHAIN_ASSET, BIND_CROSS_CHAIN_ASSET_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET);
        // 11-In chainSWAPAsset binding heterogeneous chain assets ==> 12-In chainSWAPAsset binding with multiple heterogeneous chain assets
        CORRESPONDENCE_ASSET_NERVE_HETEROGENEOUS.put(BIND_SWAP_ASSET_TO_HETEROGENEOUS_CROSS_CHAIN_ASSET, BIND_SWAP_ASSET_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET);
        /** After binding more than two assets, the asset type no longer changes **/
        // 7-Binding ordinary assets within the chain to multiple heterogeneous chain assets ==> 7-Binding ordinary assets within the chain to multiple heterogeneous chain assets
        CORRESPONDENCE_ASSET_NERVE_HETEROGENEOUS.put(BIND_COMMON_ASSET_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET, BIND_COMMON_ASSET_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET);
        // 8-Binding Parallel Chain Assets to Multiple Heterogeneous Chain Assets ==> 8-Binding Parallel Chain Assets to Multiple Heterogeneous Chain Assets
        CORRESPONDENCE_ASSET_NERVE_HETEROGENEOUS.put(BIND_CROSS_CHAIN_ASSET_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET, BIND_CROSS_CHAIN_ASSET_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET);
        // 9-Binding heterogeneous chain assets to multiple heterogeneous chain assets ==> 9-Binding heterogeneous chain assets to multiple heterogeneous chain assets
        CORRESPONDENCE_ASSET_NERVE_HETEROGENEOUS.put(BIND_HETEROGENEOUS_CROSS_CHAIN_ASSET_TYPE_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET, BIND_HETEROGENEOUS_CROSS_CHAIN_ASSET_TYPE_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET);
        // 12-In chainSWAPAsset binding with multiple heterogeneous chain assets ==> 12-In chainSWAPAsset binding with multiple heterogeneous chain assets
        CORRESPONDENCE_ASSET_NERVE_HETEROGENEOUS.put(BIND_SWAP_ASSET_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET, BIND_SWAP_ASSET_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET);


        // 5-On chain ordinary assets bound to heterogeneous chain assets ==> 1-On chain ordinary assets
        CORRESPONDENCE_ASSET_HETEROGENEOUS_NERVE.put(BIND_COMMON_ASSET_TO_HETEROGENEOUS_CROSS_CHAIN_ASSET, COMMON_ASSET_TYPE);
        // 6-Parallel chain assets bound to heterogeneous chain assets ==> 3-Parallel chain assets
        CORRESPONDENCE_ASSET_HETEROGENEOUS_NERVE.put(BIND_CROSS_CHAIN_ASSET_TO_HETEROGENEOUS_CROSS_CHAIN_ASSET, CROSS_CHAIN_ASSET_TYPE);
        // 7-Binding ordinary assets within the chain to multiple heterogeneous chain assets ==> 5-On chain ordinary assets bound to heterogeneous chain assets
        CORRESPONDENCE_ASSET_HETEROGENEOUS_NERVE.put(BIND_COMMON_ASSET_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET, BIND_COMMON_ASSET_TO_HETEROGENEOUS_CROSS_CHAIN_ASSET);
        // 11-In chainSWAPAsset binding heterogeneous chain assets ==> 10-In chainSWAPasset
        CORRESPONDENCE_ASSET_HETEROGENEOUS_NERVE.put(BIND_SWAP_ASSET_TO_HETEROGENEOUS_CROSS_CHAIN_ASSET, SWAP_LIQUIDITY_POOL_CROSS_CHAIN_ASSET_TYPE);
        // 8-Binding Parallel Chain Assets to Multiple Heterogeneous Chain Assets ==> 6-Parallel chain assets bound to heterogeneous chain assets
        CORRESPONDENCE_ASSET_HETEROGENEOUS_NERVE.put(BIND_CROSS_CHAIN_ASSET_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET, BIND_CROSS_CHAIN_ASSET_TO_HETEROGENEOUS_CROSS_CHAIN_ASSET);
        // 9-Binding heterogeneous chain assets to multiple heterogeneous chain assets ==> 4-Heterogeneous chain assets
        CORRESPONDENCE_ASSET_HETEROGENEOUS_NERVE.put(BIND_HETEROGENEOUS_CROSS_CHAIN_ASSET_TYPE_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET, HETEROGENEOUS_CROSS_CHAIN_ASSET_TYPE);
        // 12-In chainSWAPAsset binding with multiple heterogeneous chain assets ==> 11-In chainSWAPAsset binding heterogeneous chain assets
        CORRESPONDENCE_ASSET_HETEROGENEOUS_NERVE.put(BIND_SWAP_ASSET_TO_MULTY_HETEROGENEOUS_CROSS_CHAIN_ASSET, BIND_SWAP_ASSET_TO_HETEROGENEOUS_CROSS_CHAIN_ASSET);
    };

    /**
     * v1.17.0 Protocol upgrade height
     */
    public static long PROTOCOL_1_17_0 = 0L;
    public static long PROTOCOL_1_32_0 = 0L;
}

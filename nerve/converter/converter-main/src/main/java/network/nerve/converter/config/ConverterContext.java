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

package network.nerve.converter.config;

import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.dto.VirtualBankDirectorDTO;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default configuration items
 * @author: Loki
 * @date: 2020-03-02
 */
public class ConverterContext {

    public static long LATEST_BLOCK_HEIGHT = 0L;
    /**
     * Trigger the high cycle of executing virtual bank change transactions allocation
     * according to2One block per second about1Number of blocks produced per day
     */
    public static long EXECUTE_CHANGE_VIRTUAL_BANK_PERIODIC_HEIGHT = ConverterConstant.DAY_BLOCKS;

    /**
     * Collection and distribution of public keys for handling fees
     */
    public static byte[] FEE_PUBKEY = null;

    /**
     * Withdrawal of black hole public key
     */
    public static byte[] WITHDRAWAL_BLACKHOLE_PUBKEY = null;

    /**
     * Trigger the block height for initializing virtual banks
     */
    public static long INIT_VIRTUAL_BANK_HEIGHT = 10;
    /**
     * Number of consensus nodes in virtual banking（Non seed nodes）
     */
    public static int VIRTUAL_BANK_AGENT_COUNT_WITHOUT_SEED = 10;

    /**
     * Total number of virtual bank member seats（Include seed node members）
     */
    public static int VIRTUAL_BANK_AGENT_TOTAL = 15;

    /**
     * Number of virtual bank seed node members
     * The threshold for the total number of nodes that enable node change services for the first time in the network
     */
    public static int INITIAL_VIRTUAL_BANK_SEED_COUNT = 5;

    /**
     * Has the node change service been enabled
     */
    public static boolean ENABLED_VIRTUAL_BANK_CHANGES_SERVICE = false;

    /**
     * Proposal fee
     */
    public static BigInteger PROPOSAL_PRICE = new BigInteger("1000000000");
    /**
     * The block height corresponding to the duration of proposal voting
     * 2One piece per second The number of blocks produced per day is 43,200
     * Default voting duration is approximately10day, The number of blocks produced is: 43200 * 10
     */
    public static long PROPOSAL_VOTE_TIME_BLOCKS = ConverterConstant.DAY_BLOCKS * 10L;


    /**
     * Signature Byzantine Ratio
     */
    public static int BYZANTINERATIO = 66;

    /**
     * The height of the first protocol upgrade Withdrawal fees100
     */
    public static long FEE_EFFECTIVE_HEIGHT_FIRST = 0L;
    /**
     * Second protocol upgrade height Withdrawal fees10
     */
    public static long FEE_EFFECTIVE_HEIGHT_SECOND = 0L;

    /**
     * Third protocol upgrade height Withdrawal of heterogeneous chain handling fees changed to(custom(Not less than the minimum value) + Additional methods)
     */
    public static long FEE_ADDITIONAL_HEIGHT = 0L;
    /**
     * Protocol upgrade height Modify withdrawal and recharge transaction agreements,Add heterogeneous chainsid
     */
    public static long WITHDRAWAL_RECHARGE_CHAIN_HEIGHT = 0L;
    /**
     * v1.8.0 Protocol upgrade height Support cross chain of Huobi ecological chain
     */
    public static long PROTOCOL_8_HUOBI_CROSS_CHAIN_HEIGHT = 0L;
    /**
     * v1.11.0 Protocol upgrade height Supporting the cross chain of the Euclidean ecosystem
     */
    public static long PROTOCOL_11_OKT_CROSS_CHAIN_HEIGHT = 0L;

    /**
     * v1.12.0 Protocol upgrade height Support transfer and partial destructionERC20
     */
    public static long PROTOCOL_12_ERC20_OF_TRANSFER_BURN_HEIGHT = 0L;

    /**
     * v1.13.0 Protocol upgrade height Support heterogeneous chainsERC20A new verification method for recharging, supportingHarmony,Polygon,KucoinCross chain ecological chain
     */
    public static long PROTOCOL_13_NEW_VALIDATION_OF_ERC20 = 0L;
    public static long PROTOCOL_13_ONE_CROSS_CHAIN_HEIGHT = 0L;
    public static long PROTOCOL_13_POLYGON_CROSS_CHAIN_HEIGHT = 0L;
    public static long PROTOCOL_13_KUCOIN_CROSS_CHAIN_HEIGHT = 0L;

    /**
     * v1.14.0 Protocol upgrade height
     */
    public static long PROTOCOL_1_14_0 = 0L;
    /**
     * v1.15.0 Protocol upgrade height Support cross chain of wave field ecosystem
     */
    public static long PROTOCOL_15_TRX_CROSS_CHAIN_HEIGHT = 0L;
    /**
     * v1.16.0 Protocol upgrade height
     */
    public static long PROTOCOL_1_16_0 = 0L;
    /**
     * v1.21.0 Protocol upgrade height
     */
    public static long PROTOCOL_1_21_0 = 0L;
    /**
     * v1.22.0 Protocol upgrade height
     */
    public static long PROTOCOL_1_22_0 = 0L;
    /**
     * v1.23.0 Protocol upgrade height
     */
    public static long PROTOCOL_1_23_0 = 0L;
    /**
     * v1.24.0 Protocol upgrade height
     */
    public static long PROTOCOL_1_24_0 = 0L;
    /**
     * v1.26.0 Protocol upgrade height
     */
    public static long PROTOCOL_1_26_0 = 0L;
    /**
     * v1.27.0 Protocol upgrade height
     */
    public static long PROTOCOL_1_27_0 = 0L;
    /**
     * v1.29.0 Protocol upgrade height
     */
    public static long PROTOCOL_1_29_0 = 0L;
    /**
     * v1.30.0 Protocol upgrade height
     */
    public static long PROTOCOL_1_30_0 = 0L;
    /**
     * v1.31.0 Protocol upgrade height
     */
    public static long PROTOCOL_1_31_0 = 0L;
    public static long PROTOCOL_1_33_0 = 0L;
    public static long PROTOCOL_1_34_0 = 0L;
    public static long PROTOCOL_1_35_0 = 0L;
    /**
     * 0=local,1=signatrue-machine
     */
    public static long SIG_MODE = 0L;

    /**
     * The height corresponding to protocol upgrade
     */
    public static Map<Integer, Long> protocolHeightMap = new HashMap<>();
    /**
     * pricekey
     */
    public static Map<String, String> priceKeyMap = new HashMap<>();

    /**
     * Initialize virtual bank public key
     */
    public static List<String> INIT_VIRTUAL_BANK_PUBKEY_LIST = new ArrayList<>();
    /**
     * Virtual Bank Administrator Details List(Including balances corresponding to different heterogeneous chains)
     */
    public static List<VirtualBankDirectorDTO> VIRTUAL_BANK_DIRECTOR_LIST = new ArrayList<>();
    /**
     * Virtual Bank Administrator Details List(Including balances corresponding to different heterogeneous chains)(Used for querying interfaces)
     */
    public static List<VirtualBankDirectorDTO> VIRTUAL_BANK_DIRECTOR_LIST_FOR_CMD = new ArrayList<>();
    /**
     * Cache record time, used for replacing expired cache
     */
    public static long VIRTUAL_BANK_DIRECTOR_LIST_FOR_CMD_RECORD_TIME = 0L;
    public static Map<String, HeterogeneousAssetInfo> assetRegisterNetwork = new HashMap<>();

    public static Map<Long, Map> HTG_RPC_CHECK_MAP = new HashMap<>();

    public static byte[] AWARD_FEE_SYSTEM_ADDRESS_PROTOCOL_1_17_0;

    public static String BITCOIN_SYS_WITHDRAWAL_FEE_ADDRESS = null;
}

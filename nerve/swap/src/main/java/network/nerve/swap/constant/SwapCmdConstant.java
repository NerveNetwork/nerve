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
package network.nerve.swap.constant;

/**
 * @author: PierreLuo
 * @date: 2021/4/15
 */
public interface SwapCmdConstant {
    String BATCH_BEGIN = "sw_batch_begin";
    String INVOKE = "sw_invoke";
    String BATCH_END = "sw_batch_end";
    String SWAP_RESULT_INFO = "sw_swap_result_info";

    String IS_LEGAL_COIN_FOR_ADD_STABLE = "sw_is_legal_coin_for_add_stable";
    String ADD_COIN_FOR_STABLE = "sw_add_coin_for_stable";
    String BEST_TRADE_EXACT_IN = "sw_best_trade_exact_in";
    String SWAP_CREATE_PAIR = "sw_swap_create_pair";
    String SWAP_ADD_LIQUIDITY = "sw_swap_add_liquidity";
    String SWAP_REMOVE_LIQUIDITY = "sw_swap_remove_liquidity";
    String SWAP_TOKEN_TRADE = "sw_swap_token_trade";
    String SWAP_MIN_AMOUNT_ADD_LIQUIDITY = "sw_swap_min_amount_add_liquidity";
    String SWAP_MIN_AMOUNT_REMOVE_LIQUIDITY = "sw_swap_min_amount_remove_liquidity";
    String SWAP_MIN_AMOUNT_TOKEN_TRADE = "sw_swap_min_amount_token_trade";
    String SWAP_PAIR_INFO = "sw_swap_pair_info";
    String SWAP_PAIR_INFO_BY_ADDRESS = "sw_swap_pair_info_by_address";
    String SWAP_PAIR_BY_LP = "sw_swap_pair_by_lp";
    String SWAP_PAIR_INFO_BY_LP = "sw_swap_pair_info_by_lp";

    String STABLE_SWAP_CREATE_PAIR = "sw_stable_swap_create_pair";
    String STABLE_SWAP_ADD_LIQUIDITY = "sw_stable_swap_add_liquidity";
    String STABLE_SWAP_REMOVE_LIQUIDITY = "sw_stable_swap_remove_liquidity";
    String STABLE_SWAP_TOKEN_TRADE = "sw_stable_swap_token_trade";
    String STABLE_SWAP_PAIR_INFO = "sw_stable_swap_pair_info";
    String STABLE_SWAP_PAIR_BY_LP = "sw_stable_swap_pair_by_lp";

    String CREATE_FARM = "sw_createFarm";
    String FARM_STAKE = "sw_farmstake";
    String FARM_WAITHDRAW = "sw_farmwithdraw";
    String FARM_INFO = "sw_getfarm";
    String FARM_INFO_DETAIL = "sw_farmInfo";
    String FARM_LIST = "sw_farmlist";
    String FARM_USER_INFO = "sw_getstakeinfo";
    String FARM_USER_DETAIL = "sw_userstakeinfo";
}

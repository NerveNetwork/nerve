/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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

package nerve.network.converter.constant;

/**
 * @author: Chino
 * @date: 2020-02-27
 */
public interface ConverterCmdConstant {

    String NEW_HASH_SIGN_MESSAGE = "newHashSign";

    String GET_TX_MESSAGE = "getTx";

    String NEW_TX_MESSAGE = "newTx";

    String GET_HETEROGENEOUS_CHAIN_ASSET_INFO = "cv_get_heterogeneous_chain_asset_info";
    String GET_ALL_HETEROGENEOUS_CHAIN_ASSET_LIST = "cv_get_all_heterogeneous_chain_asset_list";
    String CREATE_HETEROGENEOUS_CONTRACT_ASSET_REG_TX = "cv_create_heterogeneous_contract_asset_reg_tx";
    /** 提现 */
    String WITHDRAWAL = "cv_withdrawal";
    String VIRTUAL_BANK_INFO = "cv_virtualBankInfo";

}

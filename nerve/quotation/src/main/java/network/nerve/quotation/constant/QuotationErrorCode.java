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

package network.nerve.quotation.constant;

import io.nuls.core.constant.CommonCodeConstanst;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.rpc.model.ModuleE;

/**
 * @author: Loki
 * @date: 2019/11/26
 */
public interface QuotationErrorCode extends CommonCodeConstanst {
    ErrorCode TXDATA_EMPTY = ErrorCode.init(ModuleE.QU.getPrefix() + "_0001");
    ErrorCode TXDATA_ERROR = ErrorCode.init(ModuleE.QU.getPrefix() + "_0002");
    ErrorCode REMOTE_RESPONSE_DATA_NOT_FOUND = ErrorCode.init(ModuleE.QU.getPrefix() + "_0003");
    ErrorCode TX_UNSIGNED = ErrorCode.init(ModuleE.QU.getPrefix() + "_0004");
    ErrorCode NO_AUTHORITY_TO_SIGN = ErrorCode.init(ModuleE.QU.getPrefix() + "_0005");
    ErrorCode CHAIN_NOT_FOUND = ErrorCode.init(ModuleE.QU.getPrefix() + "_0006");
    ErrorCode PACKER_ADDRESS_NOT_FOUND  = ErrorCode.init(ModuleE.QU.getPrefix() + "_0007");
    ErrorCode PACKER_PASSWORD_NOT_FOUND  = ErrorCode.init(ModuleE.QU.getPrefix() + "_0008");
    ErrorCode QUOTATION_KEY_NOT_EXIST  = ErrorCode.init(ModuleE.QU.getPrefix() + "_0009");
    ErrorCode QUOTATION_COINDATA_NOT_EMPTY = ErrorCode.init(ModuleE.QU.getPrefix() + "_0010");
    ErrorCode FINAL_QUOTATION_SIGN_NOT_EMPTY  = ErrorCode.init(ModuleE.QU.getPrefix() + "_0011");
    ErrorCode FINAL_QUOTATION_CONFIRMED  = ErrorCode.init(ModuleE.QU.getPrefix() + "_0012");
    ErrorCode FINAL_QUOTATION_CALC_NOT_SAME  = ErrorCode.init(ModuleE.QU.getPrefix() + "_0013");
}

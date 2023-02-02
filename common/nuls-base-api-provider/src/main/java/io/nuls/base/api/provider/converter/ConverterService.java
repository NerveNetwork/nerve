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

package io.nuls.base.api.provider.converter;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.converter.facade.*;

import java.util.List;

/**
 * @author: Charlie
 * @date: 2020/4/28
 */
public interface ConverterService {
    /**
     *  异构链资产提现
     *  withdrawal
     * @param req
     * @return
     */
    Result<String> withdrawal(WithdrawalReq req);

    /**
     * 升级合约提案
     * @param req
     * @return
     */
    Result<String> UpgradeContract(UpgradeContractReq req);

    /**
     * 提案投票
     * @param req
     * @return
     */
    Result<String> vote(VoteReq req);

    /**
     * 重置虚拟银行异构链
     * @param req
     * @return
     */
    Result<String> resetBank(ResetBankReq req);

    /**
     * 检查并重发异构链充值交易
     * @param req
     * @return
     */
    Result<Boolean> checkRetryParse(CheckRetryParseReq req);

    /**
     * 取消虚拟银行发送到异构链网络的交易
     * @param req
     * @return
     */
    Result<Boolean> cancelHtgTx(CancelHtgTxReq req);

    Result<String> registerHeterogeneousAsset(RegisterHeterogeneousAssetReq req);

    Result<String> registerHeterogeneousMainAsset(RegisterHeterogeneousMainAssetReq req);

    Result<String> bindHeterogeneousMainAsset(BindHeterogeneousMainAssetReq req);

    Result<String> bind(BindReq req);

    Result<String> bindOverride(BindOverrideReq req);

    Result<String> unbind(UnbindReq req);

    Result<String> unregister(UnbindReq req);
    Result<String> pauseIn(UnbindReq req);
    Result<String> resumeIn(UnbindReq req);
    Result<String> pauseOut(UnbindReq req);
    Result<String> resumeOut(UnbindReq req);

    Result<Boolean> retryWithdrawal(RetryWithdrawalReq req);

    Result<Boolean> validateRegisterHeterogeneousAsset(RegisterHeterogeneousAssetReq req);

    /**
     * 获取异构链资产的异构链信息
     * @param req
     * @return
     */
    Result<HeterogeneousAssetInfo> getHeterogeneousAssetInfo(GetHeterogeneousAssetInfoReq req);

    public Result<HeterogeneousAssetInfo> getHeterogeneousAssetInfoList(GetHeterogeneousAssetInfoReq req);

    /**
     * 获取虚拟银行信息
     * @param req
     * @return
     */
    Result<VirtualBankDirectorDTO> getVirtualBankInfo(GetVirtualBankInfoReq req);

    /**
     * 通过打包地址获取节点的异构链地址
     * @param req
     * @return
     */
    Result<String> getHeterogeneousAddress(GetHeterogeneousAddressReq req);

}

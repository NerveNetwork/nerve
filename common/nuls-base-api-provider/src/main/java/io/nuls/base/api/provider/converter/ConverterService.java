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

/**
 * @author: Charlie
 * @date: 2020/4/28
 */
public interface ConverterService {
    /**
     *  Withdrawal of heterogeneous chain assets
     *  withdrawal
     * @param req
     * @return
     */
    Result<String> withdrawal(WithdrawalReq req);

    /**
     * Upgrade contract proposal
     * @param req
     * @return
     */
    Result<String> UpgradeContract(UpgradeContractReq req);

    /**
     * Proposal voting
     * @param req
     * @return
     */
    Result<String> vote(VoteReq req);

    /**
     * Reset Virtual Bank Heterogeneous Chain
     * @param req
     * @return
     */
    Result<String> resetBank(ResetBankReq req);

    /**
     * Check and resend heterogeneous chain recharge transactions
     * @param req
     * @return
     */
    Result<Boolean> checkRetryParse(CheckRetryParseReq req);

    /**
     * Cancel transactions sent by virtual banks to heterogeneous chain networks
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
    Result<String> stableCoinPause(StableCoinPauseReq req);

    Result<Boolean> retryWithdrawal(RetryWithdrawalReq req);

    Result<Boolean> validateRegisterHeterogeneousAsset(RegisterHeterogeneousAssetReq req);

    /**
     * Obtain heterogeneous chain information of heterogeneous chain assets
     * @param req
     * @return
     */
    Result<HeterogeneousAssetInfo> getHeterogeneousAssetInfo(GetHeterogeneousAssetInfoReq req);

    public Result<HeterogeneousAssetInfo> getHeterogeneousAssetInfoList(GetHeterogeneousAssetInfoReq req);

    /**
     * Obtain virtual banking information
     * @param req
     * @return
     */
    Result<VirtualBankDirectorDTO> getVirtualBankInfo(GetVirtualBankInfoReq req);

    /**
     * Obtaining heterogeneous chain addresses of nodes through packaging addresses
     * @param req
     * @return
     */
    Result<String> getHeterogeneousAddress(GetHeterogeneousAddressReq req);

}

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

package network.nerve.converter.core.business;

import io.nuls.core.exception.NulsException;
import network.nerve.converter.model.bo.Chain;

/**
 * @author: Loki
 * @date: 2020/3/18
 */
public interface HeterogeneousService {

    /**
     * Determine whether it is necessary to assemble the main assets of the current network to subsidize heterogeneous chain transaction fees
     * Heterogeneous chains are contract types,And the withdrawal asset is not a heterogeneous chain main asset,Only then will the current network's main assets be collected as a subsidy for handling fees
     * @param heterogeneousChainId
     * @param heterogeneousAssetId
     * @return true Assembly subsidy handling fee required
     */
    boolean isAssembleCurrentAssetFee(int heterogeneousChainId, int heterogeneousAssetId) throws NulsException;

    /**
     * Caching and Persisting Heterogeneous Chains Executing Virtual Bank Change Transactions state
     * @param chain
     * @param status
     * @return
     */
    boolean saveExeHeterogeneousChangeBankStatus(Chain chain, Boolean status);

    /**
     * Persistence is implementing a proposal to revoke node banking eligibility state
     * @param chain
     * @param status
     * @return
     */
    boolean saveExeDisqualifyBankProposalStatus(Chain chain, Boolean status);

    /**
     * Are you resetting heterogeneous chains(contract) state
     * @param chain
     * @param status
     * @return
     */
    boolean saveResetVirtualBankStatus(Chain chain, Boolean status);


    void checkRetryParse(Chain chain, int heterogeneousChainId, String heterogeneousTxHash) throws NulsException;
    void checkRetryHtgTx(Chain chain, int heterogeneousChainId, String heterogeneousTxHash) throws NulsException;
    void cancelHtgTx(Chain chain, int heterogeneousChainId, String heterogeneousAddress, String nonce, String priceGwei) throws NulsException;
}

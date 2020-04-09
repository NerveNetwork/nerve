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

package nerve.network.converter.core.business;

import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import nerve.network.converter.model.bo.Chain;
import nerve.network.converter.model.bo.HeterogeneousConfirmedVirtualBank;
import nerve.network.converter.model.dto.ProposalTxDTO;
import nerve.network.converter.model.dto.RechargeTxDTO;
import nerve.network.converter.model.dto.WithdrawalTxDTO;
import nerve.network.converter.model.txdata.ConfirmWithdrawalTxData;
import io.nuls.core.exception.NulsException;

import java.util.List;

/**
 * 组装交易
 *
 * @author: Chino
 * @date: 2020-02-28
 */
public interface AssembleTxService {

    /**
     * 组装并发布 虚拟银行节点变更交易
     *
     * @param chain        链信息
     * @param inAgentList  加入的节点信息
     * @param outAgentList 移除的节点信息
     * @param txTime       交易时间
     * @return
     * @throws NulsException
     */
    Transaction createChangeVirtualBankTx(Chain chain,
                                          List<byte[]> inAgentList,
                                          List<byte[]> outAgentList,
                                          long txTime) throws NulsException;

    /**
     * 确认 虚拟银行节点变更交易
     *
     * @param chain                   链信息
     * @param changeVirtualBankTxHash 虚拟银行节点变更交易hash
     * @param listConfirmed           异构链变更确认信息
     * @param txTime                  交易时间
     * @return
     * @throws NulsException
     */
    Transaction createConfirmedChangeVirtualBankTx(Chain chain,
                                                   NulsHash changeVirtualBankTxHash,
                                                   List<HeterogeneousConfirmedVirtualBank> listConfirmed,
                                                   long txTime) throws NulsException;

    /**
     * 初始化异构链交易
     * PS:当节点区块处于正在同步模式时,不触发该笔交易的创建
     * @param chain
     * @param heterogeneousChainId
     * @param txTime
     * @return
     * @throws NulsException
     */
    Transaction createInitializeHeterogeneousTx(Chain chain,
                                                int heterogeneousChainId,
                                                /*List<byte[]> listDirector,*/
                                                long txTime) throws NulsException;
    /**
     * 组装并发布 充值交易
     *
     * @param chain         链信息
     * @param rechargeTxDTO 充值数据
     * @return
     * @throws NulsException
     */
    Transaction createRechargeTx(Chain chain, RechargeTxDTO rechargeTxDTO) throws NulsException;

    /**
     * 组装并发布 提现交易
     *
     * @param chain           链信息
     * @param withdrawalTxDTO 提现数据
     * @return
     * @throws NulsException
     */
    Transaction createWithdrawalTx(Chain chain, WithdrawalTxDTO withdrawalTxDTO) throws NulsException;

    /**
     * 组装并发布 确认提现交易
     *
     * @param chain                   链信息
     * @param confirmWithdrawalTxData 交易信息
     * @param txTime                  交易时间
     * @return
     * @throws NulsException
     */
    Transaction createConfirmWithdrawalTx(Chain chain,
                                          ConfirmWithdrawalTxData confirmWithdrawalTxData,
                                          long txTime) throws NulsException;

    /**
     * 组装并发布 发起提案交易
     *
     * @param chain         链信息
     * @param proposalTxDTO 提案信息
     * @return
     * @throws NulsException
     */
    Transaction createProposalTx(Chain chain, ProposalTxDTO proposalTxDTO) throws NulsException;

    /**
     * 组装并发布 对提案表决交易
     *
     * @param chain          链信息
     * @param proposalTxHash 提案交易hash
     * @param choice         表决
     * @param remark         备注
     * @return
     * @throws NulsException
     */
    Transaction createVoteProposalTx(Chain chain,
                                     NulsHash proposalTxHash,
                                     byte choice,
                                     String remark) throws NulsException;

    /**
     * 组装并发布 补贴手续费交易
     * @param chain
     * @param basisTxHash
     * @param listRewardAddress
     * @param txTime
     * @return
     * @throws NulsException
     */
    Transaction createDistributionFeeTx(Chain chain,
                                        NulsHash basisTxHash,
                                        List<byte[]> listRewardAddress,
                                        long txTime) throws NulsException;

    /**
     * 组装并发布注册异构链合约资产交易
     * @param chain
     * @param from
     * @param password
     * @param heterogeneousChainId
     * @param decimals
     * @param symbol
     * @param contractAddress
     * @param remark
     * @return
     * @throws NulsException
     */
    Transaction createHeterogeneousContractAssetRegTx(Chain chain,
                                                      String from,
                                                      String password,
                                                      int heterogeneousChainId,
                                                      int decimals,
                                                      String symbol,
                                                      String contractAddress, String remark) throws NulsException;

}

/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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

package network.nerve.converter.core.validator;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.context.HeterogeneousChainManager;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.HeterogeneousTxTypeEnum;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.enums.ProposalVoteRangeTypeEnum;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.txdata.ProposalTxData;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.RechargeStorageService;
import network.nerve.converter.utils.ConverterUtil;
import network.nerve.converter.utils.HeterogeneousUtil;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * @author: Loki
 * @date: 2020/5/11
 */
@Component
public class ProposalVerifier {

    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private HeterogeneousChainManager heterogeneousChainManager;
    @Autowired
    private RechargeStorageService rechargeStorageService;

    public void validate(Chain chain, Transaction tx) throws NulsException {
        ProposalTxData txData = ConverterUtil.getInstance(tx.getTxData(), ProposalTxData.class);
        ProposalVoteRangeTypeEnum rangeType = ProposalVoteRangeTypeEnum.getEnum(txData.getVoteRangeType());
        if (null == rangeType) {
            // 提案投票范围类型类型不存在
            throw new NulsException(ConverterErrorCode.PROPOSAL_VOTE_RANGE_ERROR);
        }
        ProposalTypeEnum proposalType = ProposalTypeEnum.getEnum(txData.getType());
        if (null == proposalType) {
            // 提案类型不存在
            throw new NulsException(ConverterErrorCode.PROPOSAL_TYPE_ERROR);
        }
        if (StringUtils.isBlank(txData.getContent())) {
            // 提案没有内容
            throw new NulsException(ConverterErrorCode.PROPOSAL_CONTENT_EMPTY);
        }

        // 验证提案费用
        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);

        boolean rs = false;
        for (CoinTo coinTo : coinData.getTo()) {
            // 手续费
            if (coinTo.getAssetsChainId() == chain.getConfig().getChainId()
                    && coinTo.getAssetsId() == chain.getConfig().getAssetId()) {
                // 验证to补贴手续费地址是补贴手续费地址公钥生成的地址
                byte[] feeBlackhole = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
                if (Arrays.equals(feeBlackhole, coinTo.getAddress())) {
                    if (BigIntegerUtils.isLessThan(coinTo.getAmount(), ConverterContext.PROPOSAL_PRICE)) {
                        chain.getLogger().error("提案交易费用不足. txHash:{}, proposal_price:{}, coinTo_amount:",
                                tx.getHash().toHex(), ConverterContext.PROPOSAL_PRICE, coinTo.getAmount());
                        throw new NulsException(ConverterErrorCode.TX_INSUFFICIENT_SUBSIDY_FEE);
                    }
                    rs = true;
                }
            }
        }
        if(!rs){
            chain.getLogger().error("提案交易费用到账地址错误. txHash:{}", tx.getHash().toHex());
            throw new NulsException(ConverterErrorCode.TX_SUBSIDY_FEE_ADDRESS_ERROR);
        }

        switch (proposalType) {
            case REFUND:
                validBankVoteRange(chain, rangeType);
                validHeterogeneousTx(chain, txData);
                break;
            case TRANSFER:
                validBankVoteRange(chain, rangeType);
                validTransfer(chain, txData, rangeType);
                break;
            case LOCK:
                validBankVoteRange(chain, rangeType);
                validTxDataAddress(chain, txData.getAddress());
                validLocked(chain, txData.getAddress());
                break;
            case UNLOCK:
                validBankVoteRange(chain, rangeType);
                validTxDataAddress(chain, txData.getAddress());
                validUnLocked(chain, txData.getAddress());
                break;
            case EXPELLED:
                validBankVoteRange(chain, rangeType);
                validTxDataAddress(chain, txData.getAddress());
                validVirtualBankDirector(chain, txData.getAddress());
                break;
            case UPGRADE:
                validBankVoteRange(chain, rangeType);
                break;
            default:
                break;
        }
    }

    /**
     * 验证提案类型为 转到其他账户 的提案业务数据
     * @param chain
     * @param txData
     * @param rangeType
     * @throws NulsException
     */
    private void validTransfer(Chain chain, ProposalTxData txData, ProposalVoteRangeTypeEnum rangeType) throws NulsException {
        validBankVoteRange(chain, rangeType);
        validTxDataAddress(chain, txData.getAddress());
        if (null != rechargeStorageService.find(chain, txData.getHeterogeneousTxHash())) {
            ConverterErrorCode.TX_DUPLICATION.getCode();
            chain.getLogger().error("The originalTxHash already confirmed (Repeat business)");
            throw new NulsException(ConverterErrorCode.TX_DUPLICATION);
        }
        validHeterogeneousTx(chain, txData);
    }

    private void validLocked(Chain chain, byte[] address) throws NulsException {
        String addr = AddressTool.getStringAddressByBytes(address);
        if (TransactionCall.isLocked(chain, addr)) {
            chain.getLogger().error("该地址已处于锁定状态 address:{}", addr);
            throw new NulsException(ConverterErrorCode.ADDRESS_LOCKED);
        }
    }

    private void validUnLocked(Chain chain, byte[] address) throws NulsException {
        String addr = AddressTool.getStringAddressByBytes(address);
        if (!TransactionCall.isLocked(chain, addr)) {
            chain.getLogger().error("该地址已处于未锁定状态 address:{}", addr);
            throw new NulsException(ConverterErrorCode.ADDRESS_UNLOCKED);
        }
    }

    private void validHeterogeneousTx(Chain chain, ProposalTxData txData) throws NulsException {
        HeterogeneousTransactionInfo txInfo = HeterogeneousUtil.getTxInfo(chain,
                txData.getHeterogeneousChainId(),
                txData.getHeterogeneousTxHash(),
                HeterogeneousTxTypeEnum.DEPOSIT,
                this.heterogeneousDockingManager);
        if (null == txInfo) {
            chain.getLogger().error("未查询到异构链交易 heterogeneousTxHash:{}", txData.getHeterogeneousTxHash());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }
        HeterogeneousChainInfo chainInfo = heterogeneousChainManager.getHeterogeneousChainByChainId(txData.getHeterogeneousChainId());
        if (!txInfo.getTo().equals(chainInfo.getMultySignAddress())) {
            // 异构交易to地址, 不是虚拟银行管理的异构链地址
            chain.getLogger().error("提案异构链交易不匹配 Proposal-heterogeneousTx-toAddress:{},  heterogeneous multySign address : {}",
                    txInfo.getTo(), chainInfo.getMultySignAddress());
            throw new NulsException(ConverterErrorCode.PROPOSAL_HETEROGENEOUS_TX_MISMATCH);
        }
        if (txInfo.getValue().compareTo(BigInteger.ZERO) <= 0) {
            // 充值交易金额错误
            chain.getLogger().error("提案异构交易充值交易金额错误 value:{}", txInfo.getValue());
            throw new NulsException(ConverterErrorCode.PROPOSAL_HETEROGENEOUS_TX_AMOUNT_ERROR);
        }
    }

    private void validTxDataAddress(Chain chain, byte[] bytes) throws NulsException {
        boolean rs = AddressTool.validNormalAddress(bytes, chain.getChainId());
        if (!rs) {
            // 地址错误
            chain.getLogger().error("Proposal tx data address is wrong");
            throw new NulsException(ConverterErrorCode.ADDRESS_ERROR);
        }
    }

    private void validBankVoteRange(Chain chain, ProposalVoteRangeTypeEnum rangeType) throws NulsException {
        if (!rangeType.equals(ProposalVoteRangeTypeEnum.BANK)) {
            // 投票人范围不是虚拟银行
            chain.getLogger().error("Proposal vote range is not bank");
            throw new NulsException(ConverterErrorCode.PROPOSAL_VOTE_RANGE_ERROR);
        }

    }

    private void validVirtualBankDirector(Chain chain, byte[] bytes) throws NulsException {
        String address = AddressTool.getStringAddressByBytes(bytes);
        VirtualBankDirector director = chain.getDirectorByAgent(address);
        if (null == director) {
            // 地址节点不是虚拟银行
            chain.getLogger().error("Proposal - the expelled agent is not a virtual_bank_director. agentAddress:{}", address);
            throw new NulsException(ConverterErrorCode.AGENT_IS_NOT_VIRTUAL_BANK);
        }
        if(director.getSeedNode()){
            // 种子节点不能退出虚拟银行
            chain.getLogger().error("种子节点不能退出虚拟银行, agentAddress:{}", address);
            throw new NulsException(ConverterErrorCode.CAN_NOT_QUIT_VIRTUAL_BANK);
        }
    }

}

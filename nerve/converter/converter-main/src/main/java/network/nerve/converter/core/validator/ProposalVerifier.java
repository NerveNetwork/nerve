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

package network.nerve.converter.core.validator;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.CoinTo;
import io.nuls.base.data.NulsHash;
import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.config.ConverterContext;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.core.context.HeterogeneousChainManager;
import network.nerve.converter.core.heterogeneous.docking.interfaces.IHeterogeneousChainDocking;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.enums.HeterogeneousTxTypeEnum;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.enums.ProposalVoteRangeTypeEnum;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.bo.HeterogeneousChainInfo;
import network.nerve.converter.model.bo.HeterogeneousTransactionInfo;
import network.nerve.converter.model.bo.VirtualBankDirector;
import network.nerve.converter.model.po.ConfirmWithdrawalPO;
import network.nerve.converter.model.txdata.ProposalTxData;
import network.nerve.converter.rpc.call.SwapCall;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.ConfirmWithdrawalStorageService;
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
    @Autowired
    private ConfirmWithdrawalStorageService confirmWithdrawalStorageService;

    public void validate(Chain chain, Transaction tx) throws NulsException {
        ProposalTxData txData = ConverterUtil.getInstance(tx.getTxData(), ProposalTxData.class);
        ProposalVoteRangeTypeEnum rangeType = ProposalVoteRangeTypeEnum.getEnum(txData.getVoteRangeType());
        if (null == rangeType) {
            // Proposal voting scope type does not exist
            throw new NulsException(ConverterErrorCode.PROPOSAL_VOTE_RANGE_ERROR);
        }
        ProposalTypeEnum proposalType = ProposalTypeEnum.getEnum(txData.getType());
        if (null == proposalType) {
            // Proposal type does not exist
            throw new NulsException(ConverterErrorCode.PROPOSAL_TYPE_ERROR);
        }
        if (StringUtils.isBlank(txData.getContent())) {
            // The proposal has no content
            throw new NulsException(ConverterErrorCode.PROPOSAL_CONTENT_EMPTY);
        }

        // Verification proposal fee
        CoinData coinData = ConverterUtil.getInstance(tx.getCoinData(), CoinData.class);

        boolean rs = false;
        for (CoinTo coinTo : coinData.getTo()) {
            // Handling fees
            if (coinTo.getAssetsChainId() == chain.getConfig().getChainId()
                    && coinTo.getAssetsId() == chain.getConfig().getAssetId()) {
                // validatetoThe subsidy fee address is the address generated by the public key of the subsidy fee address
                byte[] feeBlackhole = AddressTool.getAddress(ConverterContext.FEE_PUBKEY, chain.getChainId());
                if (Arrays.equals(feeBlackhole, coinTo.getAddress())) {
                    if (BigIntegerUtils.isLessThan(coinTo.getAmount(), ConverterContext.PROPOSAL_PRICE)) {
                        chain.getLogger().error("Insufficient transaction fees for proposals. txHash:{}, proposal_price:{}, coinTo_amount:",
                                tx.getHash().toHex(), ConverterContext.PROPOSAL_PRICE, coinTo.getAmount());
                        throw new NulsException(ConverterErrorCode.TX_INSUFFICIENT_SUBSIDY_FEE);
                    }
                    rs = true;
                }
            }
        }
        if(!rs){
            chain.getLogger().error("Proposal transaction fee payment address error. txHash:{}", tx.getHash().toHex());
            throw new NulsException(ConverterErrorCode.TX_SUBSIDY_FEE_ADDRESS_ERROR);
        }

        switch (proposalType) {
            case REFUND:
                validBankVoteRange(chain, rangeType);
                validHeterogeneousTx(chain, txData);
                validRefund(chain, txData);
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
            case WITHDRAW:
                validBankVoteRange(chain, rangeType);
                validWithdrawTx(chain, txData.getHash());
                break;
            case ADDCOIN:
                validBankVoteRange(chain, rangeType);
                validCoinForSwap(chain, txData.getContent(), txData.getAddress());
                break;
            case REMOVECOIN:
                validBankVoteRange(chain, rangeType);
                validRemoveCoinForSwap(chain, txData.getContent(), txData.getAddress());
                break;
            case MANAGE_STABLE_PAIR_FOR_SWAP_TRADE:
                validBankVoteRange(chain, rangeType);
                validStableSwap(chain, txData.getAddress());
                break;
            default:
                break;
        }
    }

    private void validCoinForSwap(Chain chain, String content, byte[] stablePairAddressBytes) throws NulsException {
        boolean success = false;
        int assetChainId = 0, assetId = 0;
        do {
            if (StringUtils.isBlank(content)) {
                break;
            }
            try {
                String[] split = content.split("-");
                if (split.length != 2) {
                    break;
                }
                assetChainId = Integer.parseInt(split[0].trim());
                assetId = Integer.parseInt(split[1].trim());
            } catch (Exception e) {
                chain.getLogger().error(e);
                break;
            }
            if (assetChainId == 0 || assetId == 0) {
                break;
            }
            success = true;
        } while (false);
        if (!success) {
            chain.getLogger().error("[Proposal to add currency] Currency information missing. content:{}", content);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
        String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);
        boolean legalCoin = SwapCall.isLegalCoinForAddStable(chain.getChainId(), stablePairAddress, assetChainId, assetId);
        if (!legalCoin) {
            chain.getLogger().error("[Proposal to add currency] Currency is illegal. stablePairAddress: {}, asset:{}-{}", stablePairAddress, assetChainId, assetId);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
    }

    private void validRemoveCoinForSwap(Chain chain, String content, byte[] stablePairAddressBytes) throws NulsException {
        boolean success = false;
        int assetChainId = 0, assetId = 0;
        do {
            if (StringUtils.isBlank(content)) {
                break;
            }
            try {
                String[] split = content.split("-");
                if (split.length < 2) {
                    break;
                }
                assetChainId = Integer.parseInt(split[0].trim());
                assetId = Integer.parseInt(split[1].trim());
            } catch (Exception e) {
                chain.getLogger().error(e);
                break;
            }
            if (assetChainId == 0 || assetId == 0) {
                break;
            }
            success = true;
        } while (false);
        if (!success) {
            chain.getLogger().error("[Proposal to remove currency] Currency information missing. content:{}", content);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
        String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);
        boolean legalCoin = SwapCall.isLegalCoinForRemoveStable(chain.getChainId(), stablePairAddress, assetChainId, assetId);
        if (!legalCoin) {
            chain.getLogger().error("[Proposal to remove currency] Currency is illegal. stablePairAddress: {}, asset:{}-{}", stablePairAddress, assetChainId, assetId);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
    }

    private void validStableSwap(Chain chain, byte[] stablePairAddressBytes) throws NulsException {
        String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);
        boolean legalStable = SwapCall.isLegalStable(chain.getChainId(), stablePairAddress);
        if (!legalStable) {
            chain.getLogger().error("[Proposal management for stablecoin trading] Transaction is illegal. stablePairAddress: {}", stablePairAddress);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
    }

    private void validRefund(Chain chain, ProposalTxData txData) throws NulsException {
        String heterogeneousTxHash = txData.getHeterogeneousTxHash();
        if(null != rechargeStorageService.find(chain, heterogeneousTxHash)){
            chain.getLogger().error("[Proposal returned by the original route] The heterogeneous chain transaction in the proposal has been successfully executed and completed, Cannot proceed with the original return route. heterogeneousTxHash:{}", heterogeneousTxHash);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
    }

    /**
     * Proposal for verifying withdrawal types
     * @param chain
     * @param hashByte
     * @throws NulsException
     */
    private void validWithdrawTx(Chain chain, byte[] hashByte) throws NulsException {
        if(null == hashByte || hashByte.length == 0){
            chain.getLogger().error("[Proposal-withdraw] The nerve hash not exist.");
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
        NulsHash hash = new NulsHash(hashByte);
        Transaction withdrawTx = TransactionCall.getConfirmedTx(chain, hash);
        if(null == withdrawTx) {
            chain.getLogger().error("[Proposal-withdraw] The withdraw tx not exist.");
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_TX_NOT_EXIST);
        }
        ConfirmWithdrawalPO cfmWithdrawalTx = confirmWithdrawalStorageService.findByWithdrawalTxHash(chain, hash);
        if(null != cfmWithdrawalTx) {
            chain.getLogger().error("[Proposal-withdraw] The confirmWithdraw tx is confirmed.");
            throw new NulsException(ConverterErrorCode.WITHDRAWAL_CONFIRMED);
        }
    }

    /**
     * The verification proposal type is Transfer to another account Proposal Business Data
     * @param chain
     * @param txData
     * @param rangeType
     * @throws NulsException
     */
    private void validTransfer(Chain chain, ProposalTxData txData, ProposalVoteRangeTypeEnum rangeType) throws NulsException {
        validBankVoteRange(chain, rangeType);
        validTxDataAddress(chain, txData.getAddress());
        NulsHash hash = rechargeStorageService.find(chain, txData.getHeterogeneousTxHash());
        if (null != hash) {
            chain.getLogger().error("[Proposal-transfer]The originalTxHash already confirmed (Repeat business). nerveHash:{}, HeterogeneousTxHash:{}",
                    hash.toHex(), txData.getHeterogeneousTxHash());
            throw new NulsException(ConverterErrorCode.TX_DUPLICATION);
        }
        validHeterogeneousTx(chain, txData);
    }

    private void validLocked(Chain chain, byte[] address) throws NulsException {
        String addr = AddressTool.getStringAddressByBytes(address);
        if (TransactionCall.isLocked(chain, addr)) {
            chain.getLogger().error("The address is already in a locked state address:{}", addr);
            throw new NulsException(ConverterErrorCode.ADDRESS_LOCKED);
        }
    }

    private void validUnLocked(Chain chain, byte[] address) throws NulsException {
        String addr = AddressTool.getStringAddressByBytes(address);
        if (!TransactionCall.isLocked(chain, addr)) {
            chain.getLogger().error("The address is already in an unlocked state address:{}", addr);
            throw new NulsException(ConverterErrorCode.ADDRESS_UNLOCKED);
        }
    }

    /**
     * Verify the correctness of heterogeneous chain recharge transactions
     * @param chain
     * @param txData
     * @throws NulsException
     */
    private void validHeterogeneousTx(Chain chain, ProposalTxData txData) throws NulsException {
        IHeterogeneousChainDocking docking = this.heterogeneousDockingManager.getHeterogeneousDocking(txData.getHeterogeneousChainId());
        HeterogeneousTransactionInfo txInfo;
        try {
            txInfo = docking.getUnverifiedDepositTransaction(txData.getHeterogeneousTxHash());
        } catch (Exception e) {
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_INVOK_ERROR);
        }
        if (null == txInfo) {
            chain.getLogger().error("No heterogeneous chain transactions found heterogeneousTxHash:{}", txData.getHeterogeneousTxHash());
            throw new NulsException(ConverterErrorCode.HETEROGENEOUS_TX_NOT_EXIST);
        }
        HeterogeneousChainInfo chainInfo = heterogeneousChainManager.getHeterogeneousChainByChainId(txData.getHeterogeneousChainId());
        if (!txInfo.getTo().equals(chainInfo.getMultySignAddress())) {
            // Heterogeneous transactionstoaddress, Heterogeneous chain addresses not managed by virtual banks
            chain.getLogger().error("Proposal heterogeneous chain transaction mismatch Proposal-heterogeneousTx-toAddress:{},  heterogeneous multySign address : {}",
                    txInfo.getTo(), chainInfo.getMultySignAddress());
            throw new NulsException(ConverterErrorCode.PROPOSAL_HETEROGENEOUS_TX_MISMATCH);
        }
        if (txInfo.getValue().compareTo(BigInteger.ZERO) <= 0) {
            // Recharge transaction amount error
            chain.getLogger().error("Proposal heterogeneous transaction recharge transaction amount error value:{}", txInfo.getValue());
            throw new NulsException(ConverterErrorCode.PROPOSAL_HETEROGENEOUS_TX_AMOUNT_ERROR);
        }
    }

    private void validTxDataAddress(Chain chain, byte[] bytes) throws NulsException {
        boolean rs = AddressTool.validNormalAddress(bytes, chain.getChainId());
        if (!rs) {
            // Address error
            chain.getLogger().error("Proposal tx data address is wrong");
            throw new NulsException(ConverterErrorCode.ADDRESS_ERROR);
        }
    }

    private void validBankVoteRange(Chain chain, ProposalVoteRangeTypeEnum rangeType) throws NulsException {
        if (!rangeType.equals(ProposalVoteRangeTypeEnum.BANK)) {
            // Voter scope is not virtual bank
            chain.getLogger().error("Proposal vote range is not bank");
            throw new NulsException(ConverterErrorCode.PROPOSAL_VOTE_RANGE_ERROR);
        }

    }

    private void validVirtualBankDirector(Chain chain, byte[] bytes) throws NulsException {
        String address = AddressTool.getStringAddressByBytes(bytes);
        VirtualBankDirector director = chain.getDirectorByAgent(address);
        if (null == director) {
            // The address node is not a virtual bank
            chain.getLogger().error("Proposal - the expelled agent is not a virtual_bank_director. agentAddress:{}", address);
            throw new NulsException(ConverterErrorCode.AGENT_IS_NOT_VIRTUAL_BANK);
        }
        if(director.getSeedNode()){
            // Seed node cannot exit virtual bank
            chain.getLogger().error("Seed node cannot exit virtual bank, agentAddress:{}", address);
            throw new NulsException(ConverterErrorCode.CAN_NOT_QUIT_VIRTUAL_BANK);
        }
    }

}

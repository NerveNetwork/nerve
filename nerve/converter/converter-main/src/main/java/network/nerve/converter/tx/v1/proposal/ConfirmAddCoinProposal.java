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

package network.nerve.converter.tx.v1.proposal;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.Transaction;
import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.StringUtils;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.helper.ConfirmProposalHelper;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.ProposalPO;
import network.nerve.converter.model.txdata.ConfirmProposalTxData;
import network.nerve.converter.model.txdata.ProposalExeBusinessData;
import network.nerve.converter.rpc.call.SwapCall;
import network.nerve.converter.rpc.call.TransactionCall;
import network.nerve.converter.storage.ProposalStorageService;
import network.nerve.converter.tx.v1.proposal.interfaces.IConfirmProposal;
import network.nerve.converter.utils.ConverterUtil;

/**
 * @author: Loki
 * @date: 2020/5/21
 */
@Component("ConfirmAddCoinProposal")
public class ConfirmAddCoinProposal implements IConfirmProposal, InitializingBean {

    @Autowired
    private ConfirmProposalHelper confirmProposalHelper;
    @Autowired
    private ProposalStorageService proposalStorageService;

    @Override
    public void afterPropertiesSet() throws NulsException {
        confirmProposalHelper.register(proposalType(), this);
    }

    @Override
    public Byte proposalType() {
        return ProposalTypeEnum.ADDCOIN.value();
    }

    @Override
    public void validate(Chain chain, Transaction tx, ConfirmProposalTxData txData) throws NulsException {
        ProposalExeBusinessData businessData = ConverterUtil.getInstance(txData.getBusinessData(), ProposalExeBusinessData.class);
        ProposalPO proposalPO = proposalStorageService.find(chain, businessData.getProposalTxHash());
        if (null == proposalPO) {
            chain.getLogger().error("[ConfirmAddCoinProposal] 提案不存在 proposalHash:{}", businessData.getProposalTxHash().toHex());
            throw new NulsException(ConverterErrorCode.PROPOSAL_NOT_EXIST);
        }

        String content = proposalPO.getContent();
        byte[] stablePairAddressBytes = proposalPO.getAddress();
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
            chain.getLogger().error("[提案添加币种] 币种信息缺失. content:{}", content);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
        String stablePairAddress = AddressTool.getStringAddressByBytes(stablePairAddressBytes);
        boolean legalCoin = SwapCall.isLegalCoinForAddStable(chain.getChainId(), stablePairAddress, assetChainId, assetId);
        if (!legalCoin) {
            chain.getLogger().error("[提案添加币种] 币种不合法. stablePairAddress: {}, asset:{}-{}", stablePairAddress, assetChainId, assetId);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
    }

}

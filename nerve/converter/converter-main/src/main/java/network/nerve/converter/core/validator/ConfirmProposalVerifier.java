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

import io.nuls.base.data.Transaction;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsException;
import network.nerve.converter.constant.ConverterErrorCode;
import network.nerve.converter.enums.ProposalTypeEnum;
import network.nerve.converter.helper.ConfirmProposalHelper;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.txdata.ConfirmProposalTxData;
import network.nerve.converter.utils.ConverterUtil;

/**
 * @author: Loki
 * @date: 2020/5/21
 */
@Component
public class ConfirmProposalVerifier {

    @Autowired
    private ConfirmProposalHelper confirmProposalHelper;

    public void validate(Chain chain, Transaction tx) throws NulsException {
        ConfirmProposalTxData txData = ConverterUtil.getInstance(tx.getTxData(), ConfirmProposalTxData.class);
        ProposalTypeEnum proposalType = ProposalTypeEnum.getEnum(txData.getType());
        if (null == proposalType) {
            // 提案类型不存在
            throw new NulsException(ConverterErrorCode.PROPOSAL_TYPE_ERROR);
        }
        // 业务验证
        confirmProposalHelper.getConfirmProposal(txData.getType()).validate(chain, tx, txData);
    }
}

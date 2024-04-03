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
import network.nerve.converter.storage.ProposalStorageService;
import network.nerve.converter.tx.v1.proposal.interfaces.IConfirmProposal;
import network.nerve.converter.utils.ConverterUtil;

/**
 * @author: Loki
 * @date: 2020/5/21
 */
@Component("ConfirmTxWhitelistProposal")
public class ConfirmTxWhitelistProposal implements IConfirmProposal, InitializingBean {

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
        return ProposalTypeEnum.TRANSACTION_WHITELIST.value();
    }

    @Override
    public void validate(Chain chain, Transaction tx, ConfirmProposalTxData txData) throws NulsException {
        ProposalExeBusinessData businessData = ConverterUtil.getInstance(txData.getBusinessData(), ProposalExeBusinessData.class);
        ProposalPO proposalPO = proposalStorageService.find(chain, businessData.getProposalTxHash());
        if (null == proposalPO) {
            chain.getLogger().error("[ConfirmRemoveCoinProposal] Proposal does not exist proposalHash:{}", businessData.getProposalTxHash().toHex());
            throw new NulsException(ConverterErrorCode.PROPOSAL_NOT_EXIST);
        }

        boolean success = false;
        String dataStr = proposalPO.getContent();
        OUT:
        do {
            if (StringUtils.isBlank(dataStr)) {
                break;
            }
            try {
                String[] addressInfos = dataStr.split(",");
                for (String addressInfoStr : addressInfos) {
                    String[] addressInfo = addressInfoStr.split("-");
                    String addr = addressInfo[0];
                    boolean valid = AddressTool.validAddress(chain.getChainId(), addr);
                    // address verification adds verification logic and is replaced with a byte array, which is then regeneratedbase58After the address is provided, communicate with the userbase58Avoiding the problem of different address prefixes compared to addresses
                    if (!valid) {
                        break OUT;
                    }
                    byte[] addressBytes = AddressTool.getAddress(addr);
                    String addressStr = AddressTool.getStringAddressByBytes(addressBytes);
                    valid = addr.equals(addressStr);
                    if (!valid) {
                        break OUT;
                    }
                    int length = addressInfo.length;
                    for (int i = 1; i < length; i++) {
                        int type = Integer.parseInt(addressInfo[i]);
                        if (type > 300 || type < 0) {
                            break OUT;
                        }
                    }
                }

            } catch (Exception e) {
                chain.getLogger().error(e);
                break;
            }
            success = true;
        } while (false);
        if (!success) {
            chain.getLogger().error("[Proposal to save account whitelist] Account whitelist information missing. content:{}", dataStr);
            throw new NulsException(ConverterErrorCode.DATA_ERROR);
        }
    }

}

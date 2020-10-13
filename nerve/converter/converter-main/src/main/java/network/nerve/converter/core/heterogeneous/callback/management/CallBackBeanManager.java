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
package network.nerve.converter.core.heterogeneous.callback.management;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.core.business.AssembleTxService;
import network.nerve.converter.core.heterogeneous.docking.management.HeterogeneousDockingManager;
import network.nerve.converter.helper.LedgerAssetRegisterHelper;
import network.nerve.converter.storage.*;

/**
 * @author: Mimi
 * @date: 2020-08-03
 */
@Component
public class CallBackBeanManager {
    @Autowired
    private AssembleTxService assembleTxService;
    @Autowired
    private HeterogeneousDockingManager heterogeneousDockingManager;
    @Autowired
    private HeterogeneousConfirmedChangeVBStorageService heterogeneousConfirmedChangeVBStorageService;
    @Autowired
    private ProposalStorageService proposalStorageService;
    @Autowired
    private ProposalExeStorageService proposalExeStorageService;
    @Autowired
    private MergeComponentStorageService mergeComponentStorageService;
    @Autowired
    private CfmChangeBankStorageService cfmChangeBankStorageService;
    @Autowired
    private LedgerAssetRegisterHelper ledgerAssetRegisterHelper;
    @Autowired
    private VirtualBankStorageService virtualBankStorageService;

    public AssembleTxService getAssembleTxService() {
        return assembleTxService;
    }

    public HeterogeneousDockingManager getHeterogeneousDockingManager() {
        return heterogeneousDockingManager;
    }

    public HeterogeneousConfirmedChangeVBStorageService getHeterogeneousConfirmedChangeVBStorageService() {
        return heterogeneousConfirmedChangeVBStorageService;
    }

    public ProposalStorageService getProposalStorageService() {
        return proposalStorageService;
    }

    public ProposalExeStorageService getProposalExeStorageService() {
        return proposalExeStorageService;
    }

    public MergeComponentStorageService getMergeComponentStorageService() {
        return mergeComponentStorageService;
    }

    public CfmChangeBankStorageService getCfmChangeBankStorageService() {
        return cfmChangeBankStorageService;
    }

    public LedgerAssetRegisterHelper getLedgerAssetRegisterHelper() {
        return ledgerAssetRegisterHelper;
    }

    public VirtualBankStorageService getVirtualBankStorageService() {
        return virtualBankStorageService;
    }
}

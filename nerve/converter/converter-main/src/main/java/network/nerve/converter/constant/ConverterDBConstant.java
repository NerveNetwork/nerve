/*
 * MIT License
 *
 * Copyright (c) 2019-2022 nerve.network
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package network.nerve.converter.constant;

/**
 * Transaction data storage constant
 * Transaction entity storage constants
 *
 * @author: qinyifeng
 */
public interface ConverterDBConstant {

    /** Persistent heterogeneous chain is executing virtual bank change transactions statekey*/
    String EXE_HETEROGENEOUS_CHANGE_BANK_KEY = "exeHeterogeneousChangeBankKey";
    /** Persistence is implementing a proposal to revoke node banking eligibility statekey*/
    String EXE_DISQUALIFY_BANK_PROPOSAL_KEY = "exeDisqualifyBankProposalKey";
    /** Are you resetting heterogeneous chains(contract) statekey*/
    String RESET_VIRTUALBANK_KEY = "resetVirtualBankKey";
    /**
     * Configuration Information Table Name
     * chain configuration table name
     */
    String DB_MODULE_CONGIF = "config";

    /**
     * Basic Information Table of Heterogeneous Chain
     */
    String DB_HETEROGENEOUS_CHAIN_INFO = "cv_table_heterogeneous_chain_info";
    /**
     * Heterogeneous Chain Data Table
     */
    String DB_HETEROGENEOUS_CHAIN = "cv_table_heterogeneous_chain";

    /**
     * Transaction storage table name prefix
     */
    String DB_TX_PREFIX = "cv_table_tx_";

    /**
     * Virtual Bank Information Persistence Table
     */
    String DB_VIRTUAL_BANK_PREFIX = "cv_table_virtual_bank_";

    /**
     * Persistent table of all virtual banking information in history(Only increase without decrease)
     */
    String DB_ALL_HISTORY_VIRTUAL_BANK_PREFIX = "cv_table_all_history_virtual_bank_";

    /**
     * Confirm virtual bank change transaction Business Storage Table
     */
    String DB_CFM_VIRTUAL_BANK_PREFIX = "cv_confirm_virtual_bank_";


    /**
     * Confirmation of withdrawal transaction status business data table
     */
    String DB_CONFIRM_WITHDRAWAL_PREFIX = "cv_confirm_withdrawal_";

    /**
     * Heterogeneous Chain Components/Proposal execution Executed transactions, Prevent secondary execution
     */
    String DB_ASYNC_PROCESSED_PREFIX = "cv_async_processed_";

    /**
     * Reset Virtual Bank Heterogeneous Chain
     */
    String DB_RESET_BANK_PREFIX = "cv_reset_bank_";

    /**
     * Waiting for the data table of the calling component
     */
    String DB_PENDING_PREFIX = "cv_pending_";

    /**
     * Virtual bank calls heterogeneous chains, When merging transactions, keyCorrespondence with each transaction
     */
    String DB_MERGE_COMPONENT_PREFIX = "cv_merge_component_";

    /**
     * Proposal awaiting execution
     */
    String DB_EXE_PROPOSAL_PENDING_PREFIX = "cv_exe_proposal_pending_";

    /**
     * Confirm subsidy handling fee transaction business data sheet
     */
    String DB_DISTRIBUTION_FEE_PREFIX = "cv_distribution_fee_";


    /**
     * Proposal Storage Table
     */
    String DB_PROPOSAL_PREFIX = "cv_proposal_";

    /**
     * Proposal storage table in voting
     */
    String DB_PROPOSAL_VOTING_PREFIX = "cv_proposal_voting_";


    /**
     * Proposal Function Voting Information Table
     */
    String DB_VOTE_PREFIX = "cv_vote_";

    /**
     * Cancelled Bank Qualification Address Table
     */
    String DB_DISQUALIFICATION_PREFIX = "cv_disqualification_";

    String DB_RECHARGE_PREFIX = "cv_recharge_";

    /**
     * Transaction for executing proposalshash and Correspondence of proposals
     */
    String DB_PROPOSAL_EXE = "cv_proposal_exe_";

    /**
     * Heterogeneous Chain Address Signature Message Storage Table
     */
    String DB_COMPONENT_SIGN = "cv_component_sign_";

}

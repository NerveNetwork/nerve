/*
 * MIT License
 *
 * Copyright (c) 2019-2020 nerve.network
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
 * 交易数据存储常量
 * Transaction entity storage constants
 *
 * @author: qinyifeng
 */
public interface ConverterDBConstant {

    /** 持久化异构链正在执行虚拟银行变更交易 状态key*/
    String EXE_HETEROGENEOUS_CHANGE_BANK_KEY = "exeHeterogeneousChangeBankKey";
    /** 持久化正在执行取消节点银行资格的提案 状态key*/
    String EXE_DISQUALIFY_BANK_PROPOSAL_KEY = "exeDisqualifyBankProposalKey";
    /** 是否正在重置异构链(合约) 状态key*/
    String RESET_VIRTUALBANK_KEY = "resetVirtualBankKey";
    /**
     * 配置信息表名
     * chain configuration table name
     */
    String DB_MODULE_CONGIF = "config";

    /**
     * 异构链基本信息表
     */
    String DB_HETEROGENEOUS_CHAIN_INFO = "cv_table_heterogeneous_chain_info";

    /**
     * 交易存储表名 前缀
     */
    String DB_TX_PREFIX = "cv_table_tx_";

    /**
     * 虚拟银行信息持久化表
     */
    String DB_VIRTUAL_BANK_PREFIX = "cv_table_virtual_bank_";

    /**
     * 历史所有虚拟银行信息持久化表(只增不减)
     */
    String DB_ALL_HISTORY_VIRTUAL_BANK_PREFIX = "cv_table_all_history_virtual_bank_";

    /**
     * 确认虚拟银行变更交易 业务存储表
     */
    String DB_CFM_VIRTUAL_BANK_PREFIX = "cv_confirm_virtual_bank_";


    /**
     * 确认提现交易状态业务数据表
     */
    String DB_CONFIRM_WITHDRAWAL_PREFIX = "cv_confirm_withdrawal_";

    /**
     * 异构链组件/提案执行 执行过的交易, 防止二次执行
     */
    String DB_ASYNC_PROCESSED_PREFIX = "cv_async_processed_";

    /**
     * 重置虚拟银行异构链
     */
    String DB_RESET_BANK_PREFIX = "cv_reset_bank_";

    /**
     * 等待调用组件的数据表
     */
    String DB_PENDING_PREFIX = "cv_pending_";

    /**
     * 等待执行的提案
     */
    String DB_EXE_PROPOSAL_PENDING_PREFIX = "cv_exe_proposal_pending_";

    /**
     * 确认补贴手续费交易业务 数据表
     */
    String DB_DISTRIBUTION_FEE_PREFIX = "cv_distribution_fee_";


    /**
     * 提案存储表
     */
    String DB_PROPOSAL_PREFIX = "cv_proposal_";

    /**
     * 投票中的提案存储表
     */
    String DB_PROPOSAL_VOTING_PREFIX = "cv_proposal_voting_";


    /**
     * 提案功能投票信息表
     */
    String DB_VOTE_PREFIX = "cv_vote_";

    /**
     * 被取消银行资格地址表
     */
    String DB_DISQUALIFICATION_PREFIX = "cv_disqualification_";

    String DB_RECHARGE_PREFIX = "cv_recharge_";

    /**
     * 执行提案的交易hash 和 提案的对应关系
     */
    String DB_PROPOSAL_EXE = "cv_proposal_exe_";

}

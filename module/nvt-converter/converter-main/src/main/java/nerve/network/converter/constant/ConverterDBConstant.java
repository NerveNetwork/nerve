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

package nerve.network.converter.constant;

/**
 * 交易数据存储常量
 * Transaction entity storage constants
 *
 * @author: qinyifeng
 */
public interface ConverterDBConstant {
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
     * 确认虚拟银行变更交易 业务存储表
     */
    String DB_CFM_VIRTUAL_BANK_PREFIX = "cv_confirm_virtual_bank_";


    /**
     * 确认提现交易状态业务数据表
     */
    String DB_CONFIRM_WITHDRAWAL_PREFIX = "cv_confirm_withdrawal_";

    /**
     * 已调用成功异构链组件的交易 持久化表 防止2次调用
     */
    String DB_TX_SUBSEQUENT_PROCESS_PREFIX = "cv_tx_subsequent_process_";

    /**
     * 等待调用组件的数据表
     */
    String DB_PENDING_PREFIX = "cv_pending_";

    /**
     * 确认补贴手续费交易业务 数据表
     */
    String DB_DISTRIBUTION_FEE_PREFIX = "cv_distribution_fee_";


    /**
     * 提案存储表
     */
    String DB_PROPOSAL_PREFIX = "cv_proposal_";

    /**
     * 提案功能投票信息表
     */
    String DB_PROPOSAL_VOTE_PREFIX = "cv_proposal_vote_";

}

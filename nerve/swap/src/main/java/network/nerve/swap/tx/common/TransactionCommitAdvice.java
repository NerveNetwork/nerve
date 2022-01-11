/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2019 nuls.io
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
package network.nerve.swap.tx.common;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.CommonAdvice;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.swap.context.SwapContext;
import network.nerve.swap.enums.BlockType;
import network.nerve.swap.manager.ChainManager;
import network.nerve.swap.model.Chain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: PierreLuo
 * @date: 2019-05-27
 */
@Component
public class TransactionCommitAdvice implements CommonAdvice {

    @Autowired
    private ChainManager chainManager;

    @Override
    public void begin(int chainId, List<Transaction> txList, BlockHeader blockHeader, int syncStatus) {
        try {
            ChainManager.chainHandle(chainId, BlockType.VERIFY_BLOCK.type());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean handle(int chainId, List<Transaction> txList, BlockHeader blockHeader, int syncStatus, Map<String, Boolean> resultMap, List<TransactionProcessor> processors) {
        if (SwapContext.PROTOCOL_1_17_0 > blockHeader.getHeight()) {
            // 协议17之前 使用旧流程处理交易commit
            return false;
        }
        Map<Integer, TransactionProcessor> processorMap = processors.stream().collect(Collectors.toMap(TransactionProcessor::getType, Function.identity()));
        List<Transaction> completedTxs = new ArrayList<>();
        for (Transaction tx : txList) {
            SwapContext.logger.info("type: {}, hash: {}", tx.getType(), tx.getHash().toHex());
            TransactionProcessor processor = processorMap.get(tx.getType());
            if (processor == null) {
                continue;
            }
            boolean commit = processor.commit(chainId, List.of(tx), blockHeader, syncStatus);
            if (!commit) {
                Collections.reverse(completedTxs);
                completedTxs.forEach(_tx -> processorMap.get(_tx.getType()).rollback(chainId, List.of(_tx), blockHeader));
                resultMap.put("value", commit);
                return true;
            } else {
                completedTxs.add(tx);
            }
        }
        resultMap.put("value", true);
        return true;
    }

    @Override
    public void end(int chainId, List<Transaction> txList, BlockHeader blockHeader) {
        // 移除临时余额, 临时区块头等当前批次执行数据
        Chain chain = chainManager.getChain(chainId);
        chain.setBatchInfo(null);
    }
}

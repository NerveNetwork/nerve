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

package network.nerve.swap.rpc.call;

import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import network.nerve.swap.constant.SwapConstant;
import network.nerve.swap.model.Chain;
import network.nerve.swap.rpc.callback.NewBlockHeightInvoke;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: Loki
 * @date: 2019/11/26
 */
public class BlockCall {


    /**
     * 区块最新高度
     * */
    public static boolean subscriptionNewBlockHeight(Chain chain) {
        try {
            Map<String, Object> params = new HashMap<>(SwapConstant.INIT_CAPACITY_4);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chain.getChainId());
            String messageId = ResponseMessageProcessor.requestAndInvoke(ModuleE.BL.abbr, "latestHeight",
                    params, "0", "1", new NewBlockHeightInvoke(chain));
            if(null != messageId){
                return true;
            }
            return false;
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }


}

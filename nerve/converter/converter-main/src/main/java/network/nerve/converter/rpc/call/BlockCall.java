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

package network.nerve.converter.rpc.call;

import io.nuls.base.data.BlockHeader;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import network.nerve.converter.constant.ConverterConstant;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.rpc.callback.NewBlockHeightInvoke;
import network.nerve.converter.utils.ConverterUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: Loki
 * @date: 2020-03-13
 */
public class BlockCall {
    /**
     * 区块最新高度
     * */
    public static boolean subscriptionNewBlockHeight(Chain chain) {
        try {
            Map<String, Object> params = new HashMap<>(ConverterConstant.INIT_CAPACITY_4);
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

    public static BlockHeader getBlockHeader(Chain chain, long height) {
        try {
            Map<String, Object> params = new HashMap<>(ConverterConstant.INIT_CAPACITY_4);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("height", height);
            HashMap map = (HashMap) BaseCall.requestAndResponse(ModuleE.BL.abbr, "getBlockHeaderByHeight", params);
            String headerStr = map.get("value").toString();
            if(StringUtils.isBlank(headerStr)){
                return null;
            }
            BlockHeader blockHeader = ConverterUtil.getInstance(headerStr, BlockHeader.class);
            return blockHeader;
        } catch (Exception e) {
            chain.getLogger().error(e);
            return null;
        }
    }
}

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

package network.nerve.swap.tx.v1.vals;

import network.nerve.swap.JunitCase;
import network.nerve.swap.JunitExecuter;
import network.nerve.swap.JunitUtils;
import network.nerve.swap.cache.SwapPairCache;
import network.nerve.swap.cache.impl.SwapPairCacheImpl;
import network.nerve.swap.constant.SwapErrorCode;
import network.nerve.swap.model.NerveToken;
import network.nerve.swap.model.ValidaterResult;
import network.nerve.swap.model.dto.SwapPairDTO;
import network.nerve.swap.utils.NerveCallback;
import network.nerve.swap.utils.SwapUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Niels
 */
public class CreatePairTxValidaterTest {

    @Test
    public void isPairExist() {
        SwapPairDTO dto = new SwapPairDTO();
        String address = SwapUtils.getStringPairAddress(9,new NerveToken(1,1),new NerveToken(9,1));
        String address1 = SwapUtils.getStringPairAddress(2,new NerveToken(1,1),new NerveToken(9,1));
        CreatePairTxValidater validater = new CreatePairTxValidater();
        SwapPairCache cache = new SwapPairCacheImpl();
        cache.put(address,dto);
        validater.setCacher(cache);

        List<JunitCase> items = new ArrayList<>();
        items.add(new JunitCase("case0",validater, new Object[]{address}, ValidaterResult.getFailed(SwapErrorCode.PAIR_ALREADY_EXISTS), false, null, NerveCallback.NULL_CALLBACK));
        items.add(new JunitCase("case1",validater, new Object[]{address1}, ValidaterResult.getSuccess(), false, null, NerveCallback.NULL_CALLBACK));
        JunitExecuter<CreatePairTxValidater> executer = new JunitExecuter<>() {
            @Override
            public Object execute(JunitCase<CreatePairTxValidater> junitCase) {
                return junitCase.getObj().isPairNotExist((String) junitCase.getParams()[0]);
            }
        };
        JunitUtils.execute(items, executer);
    }
}
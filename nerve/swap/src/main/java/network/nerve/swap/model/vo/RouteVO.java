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
package network.nerve.swap.model.vo;

import network.nerve.swap.model.TokenAmount;

import java.util.Arrays;
import java.util.List;

/**
 * @author: PierreLuo
 * @date: 2021/5/20
 */
public class RouteVO {

    private List<SwapPairVO> path;
    private TokenAmount tokenAmountIn;
    private TokenAmount tokenAmountOut;

    public RouteVO(List<SwapPairVO> path, TokenAmount tokenAmountIn, TokenAmount tokenAmountOut) {
        this.path = path;
        this.tokenAmountIn = tokenAmountIn;
        this.tokenAmountOut = tokenAmountOut;
    }

    public List<SwapPairVO> getPath() {
        return path;
    }

    public void setPath(List<SwapPairVO> path) {
        this.path = path;
    }

    public TokenAmount getTokenAmountIn() {
        return tokenAmountIn;
    }

    public void setTokenAmountIn(TokenAmount tokenAmountIn) {
        this.tokenAmountIn = tokenAmountIn;
    }

    public TokenAmount getTokenAmountOut() {
        return tokenAmountOut;
    }

    public void setTokenAmountOut(TokenAmount tokenAmountOut) {
        this.tokenAmountOut = tokenAmountOut;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"path\":")
                .append(Arrays.deepToString(path.toArray()));
        sb.append(",\"tokenAmountIn\":")
                .append(tokenAmountIn.toString());
        sb.append(",\"tokenAmountOut\":")
                .append(tokenAmountOut.toString());
        sb.append('}');
        return sb.toString();
    }
}

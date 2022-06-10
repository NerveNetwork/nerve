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

import java.math.BigDecimal;

/**
 * @author: PierreLuo
 * @date: 2021/5/20
 */
public class RouteOfPriceImpactVO {
    private RouteVO routeDTO;
    private BigDecimal impact;// 价格影响

    public RouteOfPriceImpactVO(RouteVO routeDTO, BigDecimal impact) {
        this.routeDTO = routeDTO;
        this.impact = impact;
    }

    public RouteVO getRouteDTO() {
        return routeDTO;
    }

    public void setRouteDTO(RouteVO routeDTO) {
        this.routeDTO = routeDTO;
    }

    public BigDecimal getImpact() {
        return impact;
    }

    public void setImpact(BigDecimal impact) {
        this.impact = impact;
    }
}
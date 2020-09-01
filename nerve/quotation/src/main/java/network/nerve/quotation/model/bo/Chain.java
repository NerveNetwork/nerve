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

package network.nerve.quotation.model.bo;

import io.nuls.core.log.logback.NulsLogger;

import java.util.List;

/**
 * @author: Loki
 * @date: 2019/11/26
 */
public class Chain {

    private ConfigBean configBean;

    private NulsLogger logger;

    private List<QuotationActuator> quote;

    private List<QuerierCfg> collectors;

    private LatestBasicBlock latestBasicBlock = new LatestBasicBlock();

    public int getChainId(){
        return configBean.getChainId();
    }

    public ConfigBean getConfigBean() {
        return configBean;
    }

    public void setConfigBean(ConfigBean configBean) {
        this.configBean = configBean;
    }

    public NulsLogger getLogger() {
        return logger;
    }

    public void setLogger(NulsLogger logger) {
        this.logger = logger;
    }

    public List<QuotationActuator> getQuote() {
        return quote;
    }

    public void setQuote(List<QuotationActuator> quote) {
        this.quote = quote;
    }

    public List<QuerierCfg> getCollectors() {
        return collectors;
    }

    public void setCollectors(List<QuerierCfg> collectors) {
        this.collectors = collectors;
    }

    public LatestBasicBlock getLatestBasicBlock() {
        return latestBasicBlock;
    }
}

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

import io.nuls.core.basic.ModuleConfig;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.core.annotation.Configuration;
import io.nuls.core.rpc.model.ModuleE;

import java.io.File;

/**
 * @author: Loki
 * @date: 2020/3/16
 */
@Component
@Configuration(domain = ModuleE.Constant.QUOTATION)
public class QuConfig extends ConfigBean implements ModuleConfig {

    private String dataPath;
    private String language;
    /** 主链链ID*/
    private int mainChainId;
    /** 主链主资产ID*/
    private int mainAssetId;
    private int effectiveQuotation;
    /**
     * 去掉最高和最低 数据的数量
     * 如果为1 则去掉1个最高和1个最低，共计2条价格数据
     * */
    private int removeMaxMinCount;
    private String quoteStartHm;
    private String quoteEndHm;
    /** USDT DAI USDC PAX 协议升级配置的高度*/
    private long usdtDaiUsdcPaxKeyHeight;
    /** BNB 协议升级配置高度 */
    private long bnbKeyHeight;
    /** HT OKB 协议升级配置高度 */
    private long htOkbKeyHeight;
    /** OKT 协议升级配置高度 */
    private long oktKeyHeight;

    public String getDataRoot() {
        return dataPath + File.separator + ModuleE.QU.name;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getMainChainId() {
        return mainChainId;
    }

    public void setMainChainId(int mainChainId) {
        this.mainChainId = mainChainId;
    }

    public int getMainAssetId() {
        return mainAssetId;
    }

    public void setMainAssetId(int mainAssetId) {
        this.mainAssetId = mainAssetId;
    }

    public int getEffectiveQuotation() {
        return effectiveQuotation;
    }

    public void setEffectiveQuotation(int effectiveQuotation) {
        this.effectiveQuotation = effectiveQuotation;
    }

    public String getQuoteStartHm() {
        return quoteStartHm;
    }

    public void setQuoteStartHm(String quoteStartHm) {
        this.quoteStartHm = quoteStartHm;
    }

    public String getQuoteEndHm() {
        return quoteEndHm;
    }

    public void setQuoteEndHm(String quoteEndHm) {
        this.quoteEndHm = quoteEndHm;
    }

    public long getUsdtDaiUsdcPaxKeyHeight() {
        return usdtDaiUsdcPaxKeyHeight;
    }

    public void setUsdtDaiUsdcPaxKeyHeight(long usdtDaiUsdcPaxKeyHeight) {
        this.usdtDaiUsdcPaxKeyHeight = usdtDaiUsdcPaxKeyHeight;
    }

    public long getBnbKeyHeight() {
        return bnbKeyHeight;
    }

    public void setBnbKeyHeight(long bnbKeyHeight) {
        this.bnbKeyHeight = bnbKeyHeight;
    }

    public long getHtOkbKeyHeight() {
        return htOkbKeyHeight;
    }

    public void setHtOkbKeyHeight(long htOkbKeyHeight) {
        this.htOkbKeyHeight = htOkbKeyHeight;
    }

    public int getRemoveMaxMinCount() {
        return removeMaxMinCount;
    }

    public void setRemoveMaxMinCount(int removeMaxMinCount) {
        this.removeMaxMinCount = removeMaxMinCount;
    }

    public long getOktKeyHeight() {
        return oktKeyHeight;
    }

    public void setOktKeyHeight(long oktKeyHeight) {
        this.oktKeyHeight = oktKeyHeight;
    }
}

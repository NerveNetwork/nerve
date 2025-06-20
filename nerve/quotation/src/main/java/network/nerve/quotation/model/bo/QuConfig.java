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
    /** Main chainID*/
    private int mainChainId;
    /** Main asset of the main chainID*/
    private int mainAssetId;
    private int effectiveQuotation;
    /**
     * Remove the highest and lowest values The quantity of data
     * If it is1 Then remove it1Maximum and1Lowest, total2Price data
     * */
    private int removeMaxMinCount;
    private String quoteStartHm;
    private String quoteEndHm;
    /** USDT DAI USDC PAX The height of protocol upgrade configuration*/
    private long usdtDaiUsdcPaxKeyHeight;
    /** BNB Protocol upgrade configuration height */
    private long bnbKeyHeight;
    /** HT OKB Protocol upgrade configuration height */
    private long htOkbKeyHeight;
    /** OKT Protocol upgrade configuration height */
    private long oktKeyHeight;
    /** ONE MATIC KCS Protocol upgrade configuration height */
    private long oneMaticKcsHeight;
    /** TRX Protocol upgrade configuration height */
    private long trxKeyHeight;
    /** p16 Protocol upgrade configuration height */
    private long protocol16Height;
    /** p21 Protocol upgrade configuration height */
    private long protocol21Height;
    /** p22 Protocol upgrade configuration height */
    private long protocol22Height;
    /** p24 Protocol upgrade configuration height */
    private long protocol24Height;
    /** p26 Protocol upgrade configuration height */
    private long protocol26Height;
    /** p27 Protocol upgrade configuration height */
    private long protocol27Height;
    /** p29 Protocol upgrade configuration height */
    private long protocol29Height;
    /** p30 Protocol upgrade configuration height */
    private long protocol30Height;
    private long protocol31Height;
    private long protocol34Height;
    private long protocol40Height;

    public long getProtocol40Height() {
        return protocol40Height;
    }

    public void setProtocol40Height(long protocol40Height) {
        this.protocol40Height = protocol40Height;
    }

    public long getProtocol34Height() {
        return protocol34Height;
    }

    public void setProtocol34Height(long protocol34Height) {
        this.protocol34Height = protocol34Height;
    }

    public long getProtocol31Height() {
        return protocol31Height;
    }

    public void setProtocol31Height(long protocol31Height) {
        this.protocol31Height = protocol31Height;
    }

    public long getProtocol30Height() {
        return protocol30Height;
    }

    public void setProtocol30Height(long protocol30Height) {
        this.protocol30Height = protocol30Height;
    }

    public long getProtocol29Height() {
        return protocol29Height;
    }

    public void setProtocol29Height(long protocol29Height) {
        this.protocol29Height = protocol29Height;
    }

    public long getProtocol27Height() {
        return protocol27Height;
    }

    public void setProtocol27Height(long protocol27Height) {
        this.protocol27Height = protocol27Height;
    }

    public long getProtocol26Height() {
        return protocol26Height;
    }

    public void setProtocol26Height(long protocol26Height) {
        this.protocol26Height = protocol26Height;
    }

    public long getProtocol24Height() {
        return protocol24Height;
    }

    public void setProtocol24Height(long protocol24Height) {
        this.protocol24Height = protocol24Height;
    }

    public long getProtocol22Height() {
        return protocol22Height;
    }

    public void setProtocol22Height(long protocol22Height) {
        this.protocol22Height = protocol22Height;
    }

    public long getProtocol21Height() {
        return protocol21Height;
    }

    public void setProtocol21Height(long protocol21Height) {
        this.protocol21Height = protocol21Height;
    }

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

    public long getOneMaticKcsHeight() {
        return oneMaticKcsHeight;
    }

    public void setOneMaticKcsHeight(long oneMaticKcsHeight) {
        this.oneMaticKcsHeight = oneMaticKcsHeight;
    }

    public long getTrxKeyHeight() {
        return trxKeyHeight;
    }

    public void setTrxKeyHeight(long trxKeyHeight) {
        this.trxKeyHeight = trxKeyHeight;
    }

    public long getProtocol16Height() {
        return protocol16Height;
    }

    public void setProtocol16Height(long protocol16Height) {
        this.protocol16Height = protocol16Height;
    }
}

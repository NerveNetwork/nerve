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
package network.nerve.converter.heterogeneouschain.tbc.model;

/**
 * @author: PierreLuo
 * @date: 2024/12/10
 */
public class FtInfo {
      private String ftContractId;
      private String ftCodeScript;
      private String ftTapeScript;
      private long ftSupply;
      private int ftDecimal;
      private String ftName;
      private String ftSymbol;
      private String ftDescription;
      private String ftOriginUtxo;
      private String ftCreatorCombineScript;
      private int ftHoldersCount;
      private String ftIconUrl;
      private long ftCreateTimestamp;
      private String ftTokenPrice;

      public String getFtContractId() {
            return ftContractId;
      }

      public void setFtContractId(String ftContractId) {
            this.ftContractId = ftContractId;
      }

      public String getFtCodeScript() {
            return ftCodeScript;
      }

      public void setFtCodeScript(String ftCodeScript) {
            this.ftCodeScript = ftCodeScript;
      }

      public String getFtTapeScript() {
            return ftTapeScript;
      }

      public void setFtTapeScript(String ftTapeScript) {
            this.ftTapeScript = ftTapeScript;
      }

      public long getFtSupply() {
            return ftSupply;
      }

      public void setFtSupply(long ftSupply) {
            this.ftSupply = ftSupply;
      }

      public int getFtDecimal() {
            return ftDecimal;
      }

      public void setFtDecimal(int ftDecimal) {
            this.ftDecimal = ftDecimal;
      }

      public String getFtName() {
            return ftName;
      }

      public void setFtName(String ftName) {
            this.ftName = ftName;
      }

      public String getFtSymbol() {
            return ftSymbol;
      }

      public void setFtSymbol(String ftSymbol) {
            this.ftSymbol = ftSymbol;
      }

      public String getFtDescription() {
            return ftDescription;
      }

      public void setFtDescription(String ftDescription) {
            this.ftDescription = ftDescription;
      }

      public String getFtOriginUtxo() {
            return ftOriginUtxo;
      }

      public void setFtOriginUtxo(String ftOriginUtxo) {
            this.ftOriginUtxo = ftOriginUtxo;
      }

      public String getFtCreatorCombineScript() {
            return ftCreatorCombineScript;
      }

      public void setFtCreatorCombineScript(String ftCreatorCombineScript) {
            this.ftCreatorCombineScript = ftCreatorCombineScript;
      }

      public int getFtHoldersCount() {
            return ftHoldersCount;
      }

      public void setFtHoldersCount(int ftHoldersCount) {
            this.ftHoldersCount = ftHoldersCount;
      }

      public String getFtIconUrl() {
            return ftIconUrl;
      }

      public void setFtIconUrl(String ftIconUrl) {
            this.ftIconUrl = ftIconUrl;
      }

      public long getFtCreateTimestamp() {
            return ftCreateTimestamp;
      }

      public void setFtCreateTimestamp(long ftCreateTimestamp) {
            this.ftCreateTimestamp = ftCreateTimestamp;
      }

      public String getFtTokenPrice() {
            return ftTokenPrice;
      }

      public void setFtTokenPrice(String ftTokenPrice) {
            this.ftTokenPrice = ftTokenPrice;
      }
}

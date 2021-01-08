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

package network.nerve.converter.heterogeneouschain.ht.storage;


import network.nerve.converter.heterogeneouschain.ht.model.HtERC20Po;

import java.util.List;
import java.util.Set;

/**
 * @author: Mimi
 * @date: 2020-02-26
 */
public interface HtERC20StorageService {

    int save(HtERC20Po po) throws Exception;

    HtERC20Po findByAddress(String address);

    void deleteByAddress(String address) throws Exception;

    boolean isExistsByAddress(String address);

    HtERC20Po findByAssetId(int assetId);

    void deleteByAssetId(int assetId) throws Exception;

    String findAddressByAssetId(int assetId);

    boolean isExistsByAssetId(int assetId);

    List<HtERC20Po> findBySymbol(String symbol);

    Set<String> findAddressBySymbol(String symbol);

    boolean isExistsBySymbol(String symbol);

    boolean hadInitDB();

    void initDBCompleted(int maxAssetId) throws Exception;

    void saveMaxAssetId(int maxAssetId) throws Exception;

    int getMaxAssetId() throws Exception;

    int getMaxInitializedAssetId() throws Exception;

    List<HtERC20Po> getAllInitializedERC20() throws Exception;

    void increaseAssetId() throws Exception;

}

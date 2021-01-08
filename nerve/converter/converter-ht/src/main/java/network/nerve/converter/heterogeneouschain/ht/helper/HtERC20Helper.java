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
package network.nerve.converter.heterogeneouschain.ht.helper;

import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import network.nerve.converter.heterogeneouschain.ht.constant.HtConstant;
import network.nerve.converter.heterogeneouschain.ht.context.HtContext;
import network.nerve.converter.heterogeneouschain.ht.model.HtERC20Po;
import network.nerve.converter.heterogeneouschain.ht.storage.HtERC20StorageService;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.HeterogeneousTransactionBaseInfo;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author: Mimi
 * @date: 2020-03-10
 */
@Component
public class HtERC20Helper {

    @Autowired
    private HtERC20StorageService htERC20StorageService;

    public boolean isERC20(String address, HeterogeneousTransactionBaseInfo po) {
        HtERC20Po erc20Po = this.loadERC20(address, po);
        if (erc20Po == null) {
            return false;
        }
        return true;
    }

    public HtERC20Po loadERC20(String address, HeterogeneousTransactionBaseInfo po) {
        HtERC20Po erc20Po = htERC20StorageService.findByAddress(address);
        if (erc20Po == null) {
            return erc20Po;
        }
        po.setDecimals(erc20Po.getDecimals());
        po.setAssetId(erc20Po.getAssetId());
        po.setIfContractAsset(true);
        po.setContractAddress(address);
        return erc20Po;
    }

    public boolean hasERC20WithListeningAddress(TransactionReceipt txReceipt, HeterogeneousTransactionBaseInfo po, Predicate<String> isListening) {
        if (txReceipt == null || !txReceipt.isStatusOK()) {
            return false;
        }
        List<Log> logs = txReceipt.getLogs();
        if (logs != null && logs.size() > 0) {
            for(Log log : logs) {
                List<String> topics = log.getTopics();
                // ERC20 topics 解析事件名
                if (topics.get(0).equals(HtConstant.EVENT_HASH_ERC20_TRANSFER)) {
                    // 为转账
                    String toAddress = "0x" + topics.get(2).substring(26, topics.get(1).length()).toString();
                    toAddress = toAddress.toLowerCase();
                    // 接收地址不是监听的多签地址
                    if (!isListening.test(toAddress)) {
                        return false;
                    }
                    String data;
                    if (topics.size() == 3) {
                        data = logs.get(0).getData();
                    } else {
                        data = topics.get(3);
                    }
                    String[] v = data.split("x");
                    // 转账金额
                    BigInteger amount = new BigInteger(v[1], 16);
                    if (amount.compareTo(BigInteger.ZERO) > 0) {
                        po.setTo(toAddress);
                        po.setValue(amount);
                        return true;
                    }
                    return false;
                }
            }

        }
        return false;
    }

    public String getContractAddressByAssetId(int assetId) {
        return htERC20StorageService.findAddressByAssetId(assetId);
    }

    public HtERC20Po getERC20ByContractAddress(String contractAddress) {
        return htERC20StorageService.findByAddress(contractAddress);
    }

    public HtERC20Po getERC20ByAssetId(int assetId) {
        return htERC20StorageService.findByAssetId(assetId);
    }

    public int getLatestMaxAssetId() throws Exception {
        return htERC20StorageService.getMaxAssetId();
    }

    public void saveHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception {
        int maxAssetId = this.getLatestMaxAssetId();
        while(htERC20StorageService.isExistsByAssetId(maxAssetId + 1)) {
            maxAssetId++;
        }
        List<HtERC20Po> successList = new ArrayList<>();
        try {
            for (HeterogeneousAssetInfo info : assetInfos) {
                HtERC20Po po = new HtERC20Po();
                po.setAddress(info.getContractAddress());
                po.setSymbol(info.getSymbol());
                po.setDecimals(info.getDecimals());
                po.setAssetId(++maxAssetId);
                info.setAssetId(po.getAssetId());
                htERC20StorageService.save(po);
                successList.add(po);
            }
        } catch (Exception e) {
            if(!successList.isEmpty()) {
                successList.stream().forEach(po -> {
                    try {
                        htERC20StorageService.deleteByAddress(po.getAddress());
                    } catch (Exception ex) {
                        HtContext.logger().error(ex);
                    }
                });
            }
            throw new Exception(e);
        }
        htERC20StorageService.saveMaxAssetId(maxAssetId);
    }

    public void rollbackHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception {
        List<HtERC20Po> successList = new ArrayList<>();
        try {
            for (HeterogeneousAssetInfo info : assetInfos) {
                HtERC20Po erc20Po = htERC20StorageService.findByAddress(info.getContractAddress());
                info.setAssetId(erc20Po.getAssetId());
                htERC20StorageService.deleteByAddress(erc20Po.getAddress());
                successList.add(erc20Po);
            }
        } catch (Exception e) {
            if(!successList.isEmpty()) {
                successList.stream().forEach(po -> {
                    try {
                        htERC20StorageService.save(po);
                    } catch (Exception ex) {
                        HtContext.logger().error(ex);
                    }
                });
            }
            throw new Exception(e);
        }
    }

    public List<HtERC20Po> getAllInitializedERC20() throws Exception {
        return htERC20StorageService.getAllInitializedERC20();
    }
}
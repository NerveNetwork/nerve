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
package network.nerve.converter.heterogeneouschain.trx.helper;

import io.nuls.core.model.StringUtils;
import network.nerve.converter.heterogeneouschain.lib.context.HtgContext;
import network.nerve.converter.heterogeneouschain.lib.management.BeanInitial;
import network.nerve.converter.heterogeneouschain.lib.model.HtgERC20Po;
import network.nerve.converter.heterogeneouschain.lib.storage.HtgERC20StorageService;
import network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant;
import network.nerve.converter.heterogeneouschain.trx.model.TRC20TransferEvent;
import network.nerve.converter.heterogeneouschain.trx.utils.TrxUtil;
import network.nerve.converter.model.bo.HeterogeneousAssetInfo;
import network.nerve.converter.model.bo.HeterogeneousTransactionBaseInfo;
import org.tron.trident.proto.Response;
import org.tron.trident.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static network.nerve.converter.heterogeneouschain.trx.constant.TrxConstant.HEX_PREFIX;

/**
 * @author: Mimi
 * @date: 2020-03-10
 */
public class TrxERC20Helper implements BeanInitial {

    private HtgERC20StorageService htgERC20StorageService;
    private HtgContext htgContext;

    public boolean isERC20(String address, HeterogeneousTransactionBaseInfo po) {
        HtgERC20Po erc20Po = this.loadERC20(address, po);
        if (erc20Po == null) {
            return false;
        }
        return true;
    }

    public HtgERC20Po loadERC20(String address, HeterogeneousTransactionBaseInfo po) {
        HtgERC20Po erc20Po = htgERC20StorageService.findByAddress(address);
        if (erc20Po == null) {
            return erc20Po;
        }
        po.setDecimals(erc20Po.getDecimals());
        po.setAssetId(erc20Po.getAssetId());
        po.setIfContractAsset(true);
        po.setContractAddress(address);
        return erc20Po;
    }

    public boolean hasERC20WithListeningAddress(Response.TransactionInfo txReceipt, HeterogeneousTransactionBaseInfo po, Predicate<String> isListening) {
        if (!TrxUtil.checkTransactionSuccess(txReceipt)) {
            return false;
        }
        List<Response.TransactionInfo.Log> logs = txReceipt.getLogList();
        if (logs != null && logs.size() > 0) {
            for(Response.TransactionInfo.Log log : logs) {
                String eventHash = Numeric.toHexString(log.getTopics(0).toByteArray());
                if (eventHash.equals(TrxConstant.EVENT_HASH_ERC20_TRANSFER)) {
                    TRC20TransferEvent trc20Event = TrxUtil.parseTRC20Event(log);
                    String toAddress = trc20Event.getTo();
                    // Transfer amount
                    BigInteger amount = trc20Event.getValue();
                    // The receiving address is not a listening multi signature address
                    if (!isListening.test(toAddress)) {
                        continue;
                    }
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

    public boolean hasERC20WithListeningAddress(String input, Predicate<String> isListening) {
        if(StringUtils.isBlank(input)) {
            return false;
        }
        input = Numeric.cleanHexPrefix(input);
        if (input.length() < 8) {
            return false;
        }
        String methodHash;
        if ((methodHash = HEX_PREFIX + input.substring(0, 8)).equals(TrxConstant.METHOD_HASH_TRANSFER)) {
            List<Object> objects = TrxUtil.parseTRC20TransferInput(input);
            if (objects.isEmpty() || objects.size() != 2)
                return false;
            String toAddress = objects.get(0).toString();
            // The receiving address is not a listening multi signature address
            if (!isListening.test(toAddress)) {
                return false;
            }
            return true;
        }
        if (methodHash.equals(TrxConstant.METHOD_HASH_TRANSFER_FROM)) {
            List<Object> objects = TrxUtil.parseTRC20TransferFromInput(input);
            if (objects.isEmpty() || objects.size() != 3)
                return false;
            String toAddress = objects.get(1).toString();
            // The receiving address is not a listening multi signature address
            if (!isListening.test(toAddress)) {
                return false;
            }
            return true;
        }
        return false;
    }

    public String getContractAddressByAssetId(int assetId) {
        return htgERC20StorageService.findAddressByAssetId(assetId);
    }

    public HtgERC20Po getERC20ByContractAddress(String contractAddress) {
        return htgERC20StorageService.findByAddress(contractAddress);
    }

    public HtgERC20Po getERC20ByAssetId(int assetId) {
        return htgERC20StorageService.findByAssetId(assetId);
    }

    public int getLatestMaxAssetId() throws Exception {
        return htgERC20StorageService.getMaxAssetId();
    }

    public void saveHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception {
        int maxAssetId = this.getLatestMaxAssetId();
        while(htgERC20StorageService.isExistsByAssetId(maxAssetId + 1)) {
            maxAssetId++;
        }
        List<HtgERC20Po> successList = new ArrayList<>();
        try {
            for (HeterogeneousAssetInfo info : assetInfos) {
                HtgERC20Po po = new HtgERC20Po();
                po.setAddress(info.getContractAddress());
                po.setSymbol(info.getSymbol());
                po.setDecimals(info.getDecimals());
                po.setAssetId(++maxAssetId);
                info.setAssetId(po.getAssetId());
                htgERC20StorageService.save(po);
                successList.add(po);
            }
        } catch (Exception e) {
            if(!successList.isEmpty()) {
                successList.stream().forEach(po -> {
                    try {
                        htgERC20StorageService.deleteByAddress(po.getAddress());
                    } catch (Exception ex) {
                        htgContext.logger().error(ex);
                    }
                });
            }
            throw new Exception(e);
        }
        htgERC20StorageService.saveMaxAssetId(maxAssetId);
    }

    public void rollbackHeterogeneousAssetInfos(List<HeterogeneousAssetInfo> assetInfos) throws Exception {
        List<HtgERC20Po> successList = new ArrayList<>();
        try {
            for (HeterogeneousAssetInfo info : assetInfos) {
                HtgERC20Po erc20Po = htgERC20StorageService.findByAddress(info.getContractAddress());
                info.setAssetId(erc20Po.getAssetId());
                htgERC20StorageService.deleteByAddress(erc20Po.getAddress());
                successList.add(erc20Po);
            }
        } catch (Exception e) {
            if(!successList.isEmpty()) {
                successList.stream().forEach(po -> {
                    try {
                        htgERC20StorageService.save(po);
                    } catch (Exception ex) {
                        htgContext.logger().error(ex);
                    }
                });
            }
            throw new Exception(e);
        }
    }

}

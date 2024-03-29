/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2019 nuls.io
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

package io.nuls.transaction.utils;

import io.nuls.common.NerveCoreConfig;
import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.*;
import io.nuls.core.core.ioc.SpringLiteContext;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxContext;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.manager.TxManager;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.bo.TxRegister;
import io.nuls.transaction.rpc.call.AccountCall;

import java.util.*;

import static io.nuls.transaction.utils.LoggerUtil.LOG;

/**
 * @author: Charlie
 * @date: 2018-12-05
 */
public class TxUtil {

    public static byte[] blackHolePublicKey = null;

    public static CoinData getCoinData(Transaction tx) throws NulsException {
        if (null == tx) {
            throw new NulsException(TxErrorCode.TX_NOT_EXIST);
        }
        try {
            return tx.getCoinDataInstance();
        } catch (NulsException e) {
            LOG.error(e);
            throw new NulsException(TxErrorCode.DESERIALIZE_COINDATA_ERROR);
        }
    }

    public static Transaction getTransaction(byte[] txBytes) throws NulsException {
        if (null == txBytes || txBytes.length == 0) {
            throw new NulsException(TxErrorCode.DATA_NOT_FOUND);
        }
        try {
            return Transaction.getInstance(txBytes);
        } catch (NulsException e) {
            LOG.error(e);
            throw new NulsException(TxErrorCode.DESERIALIZE_TX_ERROR);
        }
    }

    public static <T> T getInstance(byte[] bytes, Class<? extends BaseNulsData> clazz) throws NulsException {
        if (null == bytes || bytes.length == 0) {
            throw new NulsException(TxErrorCode.DATA_NOT_FOUND);
        }
        try {
            BaseNulsData baseNulsData = clazz.getDeclaredConstructor().newInstance();
            baseNulsData.parse(new NulsByteBuffer(bytes));
            return (T) baseNulsData;
        } catch (NulsException e) {
            LOG.error(e);
            throw new NulsException(TxErrorCode.DESERIALIZE_ERROR);
        } catch (Exception e) {
            LOG.error(e);
            throw new NulsException(TxErrorCode.DESERIALIZE_ERROR);
        }
    }

    /**
     * RPCUtil Deserialization
     *
     * @param data
     * @param clazz
     * @param <T>
     * @return
     * @throws NulsException
     */
    public static <T> T getInstanceRpcStr(String data, Class<? extends BaseNulsData> clazz) throws NulsException {
        return getInstance(RPCUtil.decode(data), clazz);
    }

    /**
     * HEXDeserialization
     *
     * @param hex
     * @param clazz
     * @param <T>
     * @return
     * @throws NulsException
     */
    public static <T> T getInstance(String hex, Class<? extends BaseNulsData> clazz) throws NulsException {
        return getInstance(HexUtil.decode(hex), clazz);
    }


    public static boolean isNulsAsset(Coin coin) {
        return isNulsAsset(coin.getAssetsChainId(), coin.getAssetsId());
    }

    public static boolean isNulsAsset(int chainId, int assetId) {
        NerveCoreConfig txConfig = SpringLiteContext.getBean(NerveCoreConfig.class);
        return chainId == txConfig.getMainChainId()
                && assetId == txConfig.getMainAssetId();
    }

    public static boolean isChainAssetExist(Chain chain, Coin coin) {
        return chain.getConfig().getChainId() == coin.getAssetsChainId() &&
                chain.getConfig().getAssetId() == coin.getAssetsId();
    }

    /**
     * From smart contractsTxDataObtain address from
     *
     * @param txData
     * @return
     */
    public static String extractContractAddress(byte[] txData) {
        if (txData == null) {
            return null;
        }
        int length = txData.length;
        if (length < Address.ADDRESS_LENGTH * 2) {
            return null;
        }
        byte[] contractAddress = new byte[Address.ADDRESS_LENGTH];
        System.arraycopy(txData, Address.ADDRESS_LENGTH, contractAddress, 0, Address.ADDRESS_LENGTH);
        return AddressTool.getStringAddressByBytes(contractAddress);
    }

    /**
     * Obtain cross chain transactionstxinfromsThe chain of addresses insideid
     *
     * @param tx
     * @return
     */
    public static int getCrossTxFromsOriginChainId(Transaction tx) throws NulsException {
        CoinData coinData = TxUtil.getCoinData(tx);
        if (null == coinData.getFrom() || coinData.getFrom().size() == 0) {
            throw new NulsException(TxErrorCode.COINFROM_NOT_FOUND);
        }
        return AddressTool.getChainIdByAddress(coinData.getFrom().get(0).getAddress());

    }

    /**
     * Obtain cross chain transactionstxintosThe chain of addresses insideid
     *
     * @param tx
     * @return
     */
    public static int getCrossTxTosOriginChainId(Transaction tx) throws NulsException {
        CoinData coinData = TxUtil.getCoinData(tx);
        if (null == coinData.getTo() || coinData.getTo().size() == 0) {
            throw new NulsException(TxErrorCode.COINFROM_NOT_FOUND);
        }
        return AddressTool.getChainIdByAddress(coinData.getTo().get(0).getAddress());

    }

    public static boolean isLegalContractAddress(byte[] addressBytes, Chain chain) {
        if (addressBytes == null) {
            return false;
        }
        return AddressTool.validContractAddress(addressBytes, chain.getChainId());
    }

    /**
     * Grouping transactions into modules
     *
     * @param chain
     * @param moduleVerifyMap
     * @param tx
     * @throws NulsException
     */
    public static void moduleGroups(Chain chain, Map<String, List<String>> moduleVerifyMap, Transaction tx) throws NulsException {
        //According to the unified validator name of the module, group all transactions and prepare for unified verification of each module
        String txStr;
        try {
            txStr = RPCUtil.encode(tx.serialize());
        } catch (Exception e) {
            throw new NulsException(e);
        }
        moduleGroups(chain, moduleVerifyMap, tx.getType(), txStr);
    }

    /**
     * Grouping transactions into modules
     *
     * @param chain
     * @param moduleVerifyMap
     * @param txType
     * @param txStr
     * @throws NulsException
     */
    public static void moduleGroups(Chain chain, Map<String, List<String>> moduleVerifyMap, int txType, String txStr) {
        //According to the unified validator name of the module, group all transactions and prepare for unified verification of each module
        TxRegister txRegister = TxManager.getTxRegister(chain, txType);
        moduleGroups(moduleVerifyMap, txRegister, txStr);
    }

    public static void moduleGroups(Map<String, List<String>> moduleVerifyMap, TxRegister txRegister, String txStr) {
        //According to the unified validator name of the module, group all transactions and prepare for unified verification of each module
        String moduleCode = txRegister.getModuleCode();
        List<String> txStrList = moduleVerifyMap.computeIfAbsent(moduleCode, k -> new ArrayList<>());
        txStrList.add(txStr);
        /*if (moduleVerifyMap.containsKey(moduleCode)) {
            moduleVerifyMap.get(moduleCode).add(txStr);
        } else {
            List<String> txStrList = new ArrayList<>();
            txStrList.add(txStr);
            moduleVerifyMap.put(moduleCode, txStrList);
        }*/
    }


    public static byte[] getNonce(byte[] preHash) {
        byte[] nonce = new byte[8];
        int copyEnd = preHash.length;
        System.arraycopy(preHash, (copyEnd - 8), nonce, 0, 8);
        return nonce;
    }


    /**
     * Parsing transaction types through transaction strings
     *
     * @param txString
     * @return
     * @throws NulsException
     */
    public static int extractTxTypeFromTx(String txString) throws NulsException {
        String txTypeHexString = txString.substring(0, 4);
        NulsByteBuffer byteBuffer = new NulsByteBuffer(RPCUtil.decode(txTypeHexString));
        return byteBuffer.readUint16();
    }


    /**
     * Store transactions based on the queue to be packagedmapThe total amount of transaction data, To calculate whether to abandon the current transaction
     *
     * @return
     */
    public static boolean discardTx(Chain chain, int packableTxMapDataSize, Transaction tx) {
        Random random = new Random();
        //random0~9
        int number = random.nextInt(10);
        if (packableTxMapDataSize >= TxConstant.PACKABLE_TX_MAP_MAX_DATA_SIZE) {
            //throw100%
            chain.getLogger().debug("Packable pool tx data size reach the 100% discard transaction threshold, hash:{}", tx.getHash().toHex());
            return true;
        } else if (packableTxMapDataSize >= TxConstant.PACKABLE_TX_MAP_HEAVY_DATA_SIZE) {
            //throw80%
            if (number < 8) {
                chain.getLogger().debug("Packable pool tx data size reach the 80% discard transaction threshold, hash:{}", tx.getHash().toHex());
                return true;
            }
        } else if (packableTxMapDataSize >= TxConstant.PACKABLE_TX_MAP_STRESS_DATA_SIZE) {
            //throw50%
            if (number < 5) {
                chain.getLogger().debug("Packable pool tx data size reach the 50% discard transaction threshold, hash:{}", tx.getHash().toHex());
                return true;
            }
        }
        return false;
    }



    /**
     * Get different elements from two sets
     *
     * @param collectionMax
     * @param collectionMin
     * @return
     */
    public static Collection getDiffent(Collection collectionMax, Collection collectionMin) {
        //applyLinkeListPrevent excessive differences,Element copying
        Collection collection = new LinkedList();
        Collection max = collectionMax;
        Collection min = collectionMin;
        //Compare sizes first,This will reduce the subsequent costsmapofifNumber of judgments
        if (collectionMax.size() < collectionMin.size()) {
            max = collectionMin;
            min = collectionMax;
        }
        Map<Object, Integer> map = new HashMap<>(max.size() * 2);
        for (Object object : max) {
            map.put(object, 1);
        }
        for (Object object : min) {
            if (map.get(object) == null) {
                collection.add(object);
            } else {
                map.put(object, 2);
            }
        }
        for (Map.Entry<Object, Integer> entry : map.entrySet()) {
            if (entry.getValue() == 1) {
                collection.add(entry.getKey());
            }
        }
        return collection;
    }

    /**
     * Get different elements from two sets,Remove duplicates
     *
     * @param collmax
     * @param collmin
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Collection getDiffentNoDuplicate(Collection collmax, Collection collmin) {
        return new HashSet(getDiffent(collmax, collmin));
    }


    /**
     * Output three line breaks, For logging purposes
     * @return
     */
    public static String nextLine(){
        String lineSeparator = System.getProperty("line.separator");
        return lineSeparator + lineSeparator;
    }

    public static boolean isBlackHoleAddress(byte[] address) {
        if(address == null) {
            return false;
        }
        int chainIdByAddress = AddressTool.getChainIdByAddress(address);
        if(chainIdByAddress != 1) {
            return false;
        }
        return AddressTool.BLOCK_HOLE_ADDRESS_SET.contains(AddressTool.getStringAddressByBytes(address));
    }


    public static boolean isBlockAddress(Chain chain, byte[] address) {
        // add by pierre at 2022-01-28 Protocol upgrade lock address
        if(address == null) {
            return false;
        }
        if (chain.getBestBlockHeight() < TxContext.PROTOCOL_1_18_0) {
            return false;
        }
        String addressStr = AddressTool.getStringAddressByBytes(address);
        return AccountCall.isBlockAccount(chain.getChainId(), addressStr);
        // end code by pierre
    }

}

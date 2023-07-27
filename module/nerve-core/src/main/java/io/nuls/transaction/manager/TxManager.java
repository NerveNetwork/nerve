/*
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.transaction.manager;

import io.nuls.base.data.Transaction;
import io.nuls.core.constant.TxType;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.bo.TxRegister;

import java.util.*;

/**
 * 交易管理类，存储管理交易注册的基本信息
 *
 * @author: Charlie
 * @date: 2018/11/22
 */
public class TxManager {

    public static String getModuleCode(Chain chain, int type){
       TxRegister txRegister = chain.getTxRegisterMap().get(type);
       if(null != txRegister){
           return txRegister.getModuleCode();
       }
        return null;
    }

    /**
     * 获取交易的注册对象
     *
     * @param type
     * @return
     */
    public static TxRegister getTxRegister(Chain chain, int type) {
        return chain.getTxRegisterMap().get(type);
    }

    /**
     * 获取所有注册过交易的模块code列表
     */
    public static Set<String> getAllRegisteredModuleCode(Chain chain){
        Set<String> set = new HashSet<>();
        for(TxRegister txRegister : chain.getTxRegisterMap().values()){
            set.add(txRegister.getModuleCode());
        }
        return set;
    }



    /**
     * 创建分组packProduce, 所有模块都会调用
     *
     * @param chain
     * @return
     */
    public static Map<String, List> getGroup(Chain chain) {
        Set<String> moduleCodes = TxManager.getAllRegisteredModuleCode(chain);
        Map<String, List> mapGroup = new HashMap<>();
        for (String code : moduleCodes) {
            mapGroup.put(code, new ArrayList<>());
        }
        return mapGroup;
    }

    /**
     * 根据交易类型返回交易类型是否存在
     *
     * @param type
     * @return
     */
    public static boolean contains(Chain chain, int type) {
        return chain.getTxRegisterMap().containsKey(type);
    }

    /**
     * 返回系统交易类型
     */
    public static List<Integer> getSysTypes(Chain chain) {
        List<Integer> list = new ArrayList<>();
        for (Map.Entry<Integer, TxRegister> map : chain.getTxRegisterMap().entrySet()) {
            if (map.getValue().getSystemTx()) {
                list.add(map.getKey());
            }
        }
        return list;
    }

    /**
     * 判断交易是系统交易
     *
     * @param tx
     * @return
     */
    public static boolean isSystemTx(Chain chain, Transaction tx) {
        TxRegister txRegister = chain.getTxRegisterMap().get(tx.getType());
        return txRegister.getSystemTx();
    }

    /**
     * 判断是否是打包时产生的交易
     * 例如 共识奖励 红黄牌 等
     * @param chain
     * @param tx
     * @return
     */
    public static boolean isPackGenerate(Chain chain, Transaction tx) {
        TxRegister txRegister = chain.getTxRegisterMap().get(tx.getType());
        return txRegister.getPackGenerate();
    }

    /**
     * 是否是智能合约交易
     * @param txType
     * @return
     */
    public static boolean isSmartContract(Chain chain, int txType){
        if(ModuleE.SC.abbr.equals(getModuleCode(chain, txType))){
            return true;
        }
        return false;
    }

    /**
     * 是否是智能合约非系统交易
     * @param txType
     * @return
     */
    public static boolean isUnSystemSmartContract(Chain chain, int txType){
        TxRegister txRegister = getTxRegister(chain, txType);
        if(ModuleE.SC.abbr.equals(txRegister.getModuleCode()) && !txRegister.getSystemTx()){
            return true;
        }
        return false;
    }

    public static boolean isUnSystemSmartContract(TxRegister txRegister){
        if (txRegister == null) {
            return false;
        }
        if(ModuleE.SC.abbr.equals(txRegister.getModuleCode()) && !txRegister.getSystemTx()){
            return true;
        }
        return false;
    }

    /**
     * 是否是智能合约系统交易
     * @param txType
     * @return
     */
    public static boolean isSystemSmartContract(Chain chain, int txType){
        TxRegister txRegister = getTxRegister(chain, txType);
        if (txRegister == null) {
            return false;
        }
        return ModuleE.SC.abbr.equals(txRegister.getModuleCode()) && txRegister.getSystemTx();
    }

    public static boolean isSystemSmartContract(TxRegister txRegister){
        if (txRegister == null) {
            return false;
        }
        return ModuleE.SC.abbr.equals(txRegister.getModuleCode()) && txRegister.getSystemTx();
    }

    public static boolean isCrossTx(int txType) {
        return txType == TxType.CROSS_CHAIN || txType == TxType.CONTRACT_TOKEN_CROSS_TRANSFER;
    }

    public static boolean isUnSystemSwap(TxRegister txRegister){
        if (txRegister == null) {
            return false;
        }
        if(ModuleE.SW.abbr.equals(txRegister.getModuleCode()) && !txRegister.getSystemTx()){
            return true;
        }
        return false;
    }
    public static boolean isSystemSwap(Chain chain, int txType){
        TxRegister txRegister = getTxRegister(chain, txType);
        if (txRegister == null) {
            return false;
        }
        return ModuleE.SW.abbr.equals(txRegister.getModuleCode()) && txRegister.getSystemTx();
    }
    public static boolean isSystemSwap(TxRegister txRegister){
        if (txRegister == null) {
            return false;
        }
        return ModuleE.SW.abbr.equals(txRegister.getModuleCode()) && txRegister.getSystemTx();
    }


}

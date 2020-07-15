/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.api.utils;

import io.nuls.api.model.po.AgentInfo;
import io.nuls.core.crypto.HexUtil;
import org.apache.commons.codec.binary.Hex;

import java.util.Arrays;
import java.util.Comparator;

/**
 * @author Eva
 */
public class AgentComparator implements Comparator<AgentInfo> {

    private static final AgentComparator INSTANCE = new AgentComparator();

    private AgentComparator() {
    }

    public static AgentComparator getInstance() {
        return INSTANCE;
    }

    @Override
    public int compare(AgentInfo o1, AgentInfo o2) {
        //保证金从大到小排序
        int result = o2.getDeposit().compareTo(o1.getDeposit());
        if(result != 0){
            return result;
        }
        if (o1.getBlockHeight() != o2.getBlockHeight()) {
            return (int) (o1.getBlockHeight() - o2.getBlockHeight());
        }
        result =  (int) (o1.getCreateTime() - o2.getCreateTime());
        if(result!=0){
            return result;
        }
        return Arrays.compare(HexUtil.decode(o1.getTxHash()), HexUtil.decode(o2.getTxHash()));

//        int cp = o1.getDeposit().compareTo(o2.getDeposit());
//        if(cp == 0){
//            if(o1.getCreateTime() == o2.getCreateTime()){
//                return o1.getAgentAddress().compareTo(o2.getAgentAddress());
//            }
//            return o1.getCreateTime() < o2.getCreateTime() ? -1 : 1;
//        }else{
//            return -cp;
//        }
//        if (o1.getStatus() > o2.getStatus()) {
//            return -1;
//        } else if (o1.getStatus() < o2.getStatus()) {
//            return 1;
//        }
//        if (o1.getCreditValue() > o2.getCreditValue()) {
//            return -1;
//        } else if (o1.getCreditValue() < o2.getCreditValue()) {
//            return 1;
//        }
////        if (o1.getCommissionRate() < o2.getCommissionRate()) {
////            return -1;
////        } else if (o1.getCommissionRate() > o2.getCommissionRate()) {
////            return 1;
////        }
////        if (o1.getTotalDeposit().compareTo(o2.getTotalDeposit()) < 0) {
////            return -1;
////        } else if (o1.getTotalDeposit().compareTo(o2.getTotalDeposit()) > 0) {
////            return 1;
////        }
//        if (o1.getDeposit().compareTo(o2.getDeposit()) > 0) {
//            return -1;
//        } else if (o1.getDeposit().compareTo(o2.getDeposit()) < 0) {
//            return 1;
//        } else {
//            if(o1.isBankNode()){
//                return -1;
//            }
//        }

//        return 0;
    }
}

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
package io.nuls.network.manager;

import io.nuls.network.constant.NetworkConstant;
import io.nuls.network.utils.LoggerUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * （业务连接节点集合）管理
 * node group  manager
 *
 * @author lan
 * @date 2019/11/01
 **/
public class BusinessGroupManager {

    public static BusinessGroupManager getInstance() {
        return businessGroupManager;
    }


    private static BusinessGroupManager businessGroupManager = new BusinessGroupManager();

    /**
     * key:业务自定义key-chainId+"_"+模块名称+"_"+flag
     * value:nodeId map
     */
    private Map<String, Map<String, String>> businessGroupMap = new ConcurrentHashMap<>();


    private BusinessGroupManager() {

    }

    public void addNodes(int chainId, String moduleName, String flag, List<String> ips) {
        String key = chainId + NetworkConstant.DOWN_LINE + moduleName + NetworkConstant.DOWN_LINE + flag;
        Map<String, String> map = businessGroupMap.get(key);
        if (null == map) {
            map = new ConcurrentHashMap();
            businessGroupMap.put(key, map);
        }
        for (String node : ips) {
            map.put(node, node);
        }
    }

    public void printGroupsInfo(int chainId, String moduleName, String flag) {
        String key = chainId + NetworkConstant.DOWN_LINE + moduleName + NetworkConstant.DOWN_LINE + flag;
        Map<String, String> map = businessGroupMap.get(key);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            LoggerUtil.logger(chainId).info("{} ip={}", key, entry.getKey());
        }
    }

    public void removeNodes(int chainId, String moduleName, String flag, List<String> ips) {
        String key = chainId + NetworkConstant.DOWN_LINE + moduleName + NetworkConstant.DOWN_LINE + flag;
        Map<String, String> map = businessGroupMap.get(key);
        if (null == map) {
            return;
        }
        for (String node : ips) {
            map.remove(node);
        }
    }

    public void removeNode(String ip) {
        LoggerUtil.COMMON_LOG.debug("==========remove ip={}",ip);
        for(Map.Entry<String,Map<String,String>> entry:businessGroupMap.entrySet()){
            entry.getValue().remove(ip);
        }

    }

    public List<String> getIps(int chainId, String moduleName, String flag) {
        String key = chainId + NetworkConstant.DOWN_LINE + moduleName + NetworkConstant.DOWN_LINE + flag;
        Map<String, String> map = businessGroupMap.get(key);
        List<String> ips = new ArrayList();
        if (null == map) {

        } else {
            ips.addAll(map.values());
        }
        return ips;
    }

    public Map<String, String> getIpsMap(int chainId, String moduleName, String flag) {
        String key = chainId + NetworkConstant.DOWN_LINE + moduleName + NetworkConstant.DOWN_LINE + flag;
        Map<String, String> map = businessGroupMap.get(key);
        return map;
    }
}

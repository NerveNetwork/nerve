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

package network.nerve.converter.storage.impl;

import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rockdb.service.RocksDBService;
import network.nerve.converter.constant.ConverterDBConstant;
import network.nerve.converter.message.ComponentSignMessage;
import network.nerve.converter.model.bo.Chain;
import network.nerve.converter.model.po.ComponentSignByzantinePO;
import network.nerve.converter.storage.ComponentSignStorageService;
import network.nerve.converter.utils.ConverterDBUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static network.nerve.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Loki
 * @date: 2020/9/1
 */
@Component
public class ComponentSignStorageServiceImpl implements ComponentSignStorageService {

    @Override
    public synchronized boolean save(Chain chain, ComponentSignByzantinePO po) {
        if (null == po) {
            return false;
        }
        try {
            ComponentSignByzantinePO saved = ConverterDBUtil.getModel(ConverterDBConstant.DB_COMPONENT_SIGN + chain.getChainId(),
                    stringToBytes(po.getHash().toHex()),
                    ComponentSignByzantinePO.class);
            if (null == saved) {
                return ConverterDBUtil.putModel(ConverterDBConstant.DB_COMPONENT_SIGN + chain.getChainId(),
                        stringToBytes(po.getHash().toHex()), po);
            }
            if (saved.getListMsg() == null) {
                saved.setListMsg(new ArrayList<>());
            }
            Set<ComponentSignMessage> msgAll = new HashSet<>(saved.getListMsg());
            msgAll.addAll(po.getListMsg());
            saved.setListMsg(new ArrayList<>(msgAll));

            if (!saved.getCompleted()) {
                saved.setCompleted(po.getCompleted());
            }
            if (!saved.getByzantinePass()) {
                saved.setByzantinePass(po.getByzantinePass());
            }
            if (!saved.getCurrentSigned()) {
                saved.setCurrentSigned(po.getCurrentSigned());
            }
            if (null != po.getCallParms()) {
                saved.setCallParms(po.getCallParms());
            }
            return ConverterDBUtil.putModel(ConverterDBConstant.DB_COMPONENT_SIGN + chain.getChainId(),
                    stringToBytes(saved.getHash().toHex()), saved);
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public ComponentSignByzantinePO get(Chain chain, String hash) {
        return ConverterDBUtil.getModel(ConverterDBConstant.DB_COMPONENT_SIGN + chain.getChainId(), stringToBytes(hash), ComponentSignByzantinePO.class);
    }

    @Override
    public boolean delete(Chain chain, String hash) {
        if (StringUtils.isBlank(hash)) {
            chain.getLogger().error("delete key is null");
            return false;
        }
        try {
            return RocksDBService.delete(ConverterDBConstant.DB_COMPONENT_SIGN + chain.getChainId(), stringToBytes(hash));
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }
}

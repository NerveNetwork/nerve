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

package network.nerve.converter.model.po;

import io.nuls.base.data.NulsHash;
import network.nerve.converter.message.ComponentSignMessage;

import java.io.Serializable;
import java.util.List;

/**
 * @author: Loki
 * @date: 2020/9/1
 */
public class ComponentSignByzantinePO implements Serializable {
    /**
     * nerve On chain transactionshash
     */
    private NulsHash hash;

    /**
     * One signed message per node
     */
    private List<ComponentSignMessage> listMsg;

    private boolean currentSigned;

    private boolean byzantinePass;

    /**
     * Has it been completed(Reached the number of Byzantine signatures, And it was successfully sent)
     */
    private boolean completed;

    private List<ComponentCallParm> callParms;

    public ComponentSignByzantinePO() {
    }

    public ComponentSignByzantinePO(NulsHash hash, List<ComponentSignMessage> listMsg, boolean currentSigned, boolean completed) {
        this.hash = hash;
        this.listMsg = listMsg;
        this.currentSigned = currentSigned;
        this.completed = completed;
    }

    public NulsHash getHash() {
        return hash;
    }

    public void setHash(NulsHash hash) {
        this.hash = hash;
    }

    public List<ComponentSignMessage> getListMsg() {
        return listMsg;
    }

    public void setListMsg(List<ComponentSignMessage> listMsg) {
        this.listMsg = listMsg;
    }

    public boolean getCurrentSigned() {
        return currentSigned;
    }

    public void setCurrentSigned(boolean currentSigned) {
        this.currentSigned = currentSigned;
    }

    public boolean getCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public List<ComponentCallParm> getCallParms() {
        return callParms;
    }

    public void setCallParms(List<ComponentCallParm> callParms) {
        this.callParms = callParms;
    }

    public boolean getByzantinePass() {
        return byzantinePass;
    }

    public void setByzantinePass(boolean byzantinePass) {
        this.byzantinePass = byzantinePass;
    }
}

/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
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

package nerve.network.converter.model.po;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 统计周期内，需要加入和退出虚拟银行的节点列表
 * @author: Chino
 * @date: 2020-03-13
 */
public class VirtualBankTemporaryChangePO implements Serializable {

    /**
     * 待加入的虚拟银行节点地址列表
     */
    private List<byte[]> listInAgents;

    /**
     * 待退出的虚拟银行节点列表
     */
    private List<byte[]> listOutAgents;

    public VirtualBankTemporaryChangePO() {
        listInAgents = new ArrayList<>();
        listOutAgents = new ArrayList<>();
    }

    public VirtualBankTemporaryChangePO(List<byte[]> listInAgents, List<byte[]> listOutAgents) {
        this.listInAgents = listInAgents;
        this.listOutAgents = listOutAgents;
    }

    public List<byte[]> getListInAgents() {
        return listInAgents;
    }

    public void setListInAgents(List<byte[]> listInAgents) {
        this.listInAgents = listInAgents;
    }

    public List<byte[]> getListOutAgents() {
        return listOutAgents;
    }

    public void setListOutAgents(List<byte[]> listOutAgents) {
        this.listOutAgents = listOutAgents;
    }

    public void clear() {
        if(null == listInAgents){
            listInAgents = new ArrayList<>();
        }else {
            listInAgents.clear();
        }
       if(null == listOutAgents){
           listOutAgents = new ArrayList<>();
       }else {
           listOutAgents.clear();
       }
   }

   public boolean isBlank(){
        boolean inBlank = null == listInAgents || (null != listInAgents && listInAgents.isEmpty());
        boolean outBlank = null == listOutAgents ||(null != listOutAgents && listOutAgents.isEmpty());
        if(inBlank && outBlank){
            return true;
        }
        return false;
   }
}

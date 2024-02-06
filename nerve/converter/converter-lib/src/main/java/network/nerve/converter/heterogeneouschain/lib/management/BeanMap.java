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
package network.nerve.converter.heterogeneouschain.lib.management;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: PierreLuo
 * @date: 2021-03-22
 */
public class BeanMap {
    public Map<String, Object> beanMap = new HashMap<>();

    public void add(String name, Object obj) throws Exception {
        beanMap.put(name, obj);
    }

    public void add(Class clazz) throws Exception {
        Object obj = clazz.getDeclaredConstructor().newInstance();
        Class temp = clazz;
        while (true) {
            if (temp == Object.class || temp == null) {
                break;
            }
            beanMap.put(temp.getName(), obj);
            temp = temp.getSuperclass();
        }
    }

    public void add(Class clazz, Object obj) throws Exception {
        Class temp = clazz;
        while (true) {
            if (temp == Object.class || temp == null) {
                break;
            }
            beanMap.put(temp.getName(), obj);
            temp = temp.getSuperclass();
        }
    }

    public Object get(Class clazz) {
        Class temp = clazz;
        while (true) {
            if (temp == Object.class || temp == null) {
                break;
            }
            Object obj = beanMap.get(temp.getName());
            if (obj != null) {
                return obj;
            }
            temp = temp.getSuperclass();
        }
        return null;
    }

    public Collection<Object> values() {
        return beanMap.values();
    }
}

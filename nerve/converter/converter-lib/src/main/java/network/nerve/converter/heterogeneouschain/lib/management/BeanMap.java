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
        beanMap.put(clazz.getName(), clazz.getDeclaredConstructor().newInstance());
    }

    public void add(Class clazz, Object obj) throws Exception {
        beanMap.put(clazz.getName(), obj);
    }

    public Object get(Class clazz) {
        return beanMap.get(clazz.getName());
    }

    public Collection<Object> values() {
        return beanMap.values();
    }
}

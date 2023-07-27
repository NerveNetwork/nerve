package io.nuls.transaction.utils;/*
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

import java.util.HashSet;
import java.util.Set;

/**
 * @author: Eva Wang
 * @date: 2018/7/9
 */
public class HashSetDuplicateProcessor<T> {

    private Set<T> set1 = new HashSet<>();
    private Set<T> set2 = new HashSet<>();
    private final int maxSize;
    private final int percent90;

    public HashSetDuplicateProcessor(int maxSize) {
        this.maxSize = maxSize;
        this.percent90 = maxSize * 9 / 10;
    }

    /**
     * 插入一个元素，并检查是否存在
     *
     * @param t
     * @return 存在：false，不存在：true
     */
    public boolean insertAndCheck(T t) {
        boolean result = set1.add(t);
        if (!result) {
            return result;
        }
        int size = set1.size();
        if (size >= maxSize) {
            set1.clear();
            set1.addAll(set2);
            set2.clear();
            set1.add(t);
        } else if (size >= percent90) {
            set2.add(t);
        }
        return result;
    }

    public boolean check(T t) {
        return !set1.contains(t);
    }
    public boolean contains(T t) {
        return set1.contains(t);
    }

    public void remove(T hash) {
        set1.remove(hash);
        set2.remove(hash);
    }

    public void clear() {
        set1.clear();
        set2.clear();
    }
}

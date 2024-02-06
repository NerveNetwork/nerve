/*
 * MIT License
 *
 * Copyright (c) 2019-2022 nerve.network
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
package network.nerve.converter.storage;


import network.nerve.converter.model.bo.ConfigBean;

import java.util.Map;

/**
 * Configuration Information Storage Management Class
 * Configuration Information Storage Management Class
 *
 * @author qinyifeng
 * @date 2018/12/11
 */
public interface ConfigStorageService {
    /**
     * Save configuration information for the specified chain
     * Save configuration information for the specified chain
     *
     * @param bean    Configuration class/config bean
     * @param chainID chainID/chain id
     * @return Whether the save was successful/Is preservation successful?
     * @throws
     */
    boolean save(ConfigBean bean, int chainID) throws Exception;

    /**
     * Query the configuration information of a certain chain
     * Query the configuration information of a chain
     *
     * @param chainID chainID/chain id
     * @return Configuration Information Class/config bean
     */
    ConfigBean get(int chainID);

    /**
     * Delete configuration information for a certain chain
     * Delete configuration information for a chain
     *
     * @param chainID chainID/chain id
     * @return Whether the deletion was successful/Delete success
     */
    boolean delete(int chainID);

    /**
     * Obtain all chain information of the current node
     * Get all the chain information of the current node
     *
     * @return Node Information List/Node information list
     */
    Map<Integer, ConfigBean> getList();
}

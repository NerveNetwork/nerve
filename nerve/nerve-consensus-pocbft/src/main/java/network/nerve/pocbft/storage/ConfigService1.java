package network.nerve.pocbft.storage;

import network.nerve.pocbft.model.bo.config.ChainConfig;

import java.util.Map;

/**
 * 配置信息存储管理类
 * Configuration Information Storage Management Class
 *
 * @author tag
 * 2018/11/8
 * */
public interface ConfigService1 {
    /**
     * 保存指定链的配置信息
     * Save configuration information for the specified chain
     *
     * @param bean     配置类/config bean
     * @param chainID  链ID/chain id
     * @return 保存是否成功/Is preservation successful?
     * @exception
     * */
    boolean save(ChainConfig bean, int chainID)throws Exception;

    /**
     * 查询某条链的配置信息
     * Query the configuration information of a chain
     *
     * @param chainID 链ID/chain id
     * @return 配置信息类/config bean
     * */
    ChainConfig get(int chainID);

    /**
     * 删除某条链的配置信息
     * Delete configuration information for a chain
     *
     * @param chainID 链ID/chain id
     * @return 删除是否成功/Delete success
     * */
    boolean delete(int chainID);

    /**
     * 获取当前节点所有的链信息
     * Get all the chain information of the current node
     *
     * @return 节点信息列表/Node information list
     * */
    Map<Integer, ChainConfig> getList();
}

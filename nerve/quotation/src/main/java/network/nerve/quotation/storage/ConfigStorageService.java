package network.nerve.quotation.storage;

import network.nerve.quotation.model.bo.ConfigBean;

import java.util.Map;

public interface ConfigStorageService {
    /**
     * Save configuration information for the specified chain
     * Save configuration information for the specified chain
     *
     * @param bean     Configuration class/config bean
     * @param chainID  chainID/chain id
     * @return Whether the save was successful/Is preservation successful?
     * @exception
     * */
    boolean save(ConfigBean bean, int chainID)throws Exception;

    /**
     * Query the configuration information of a certain chain
     * Query the configuration information of a chain
     *
     * @param chainID chainID/chain id
     * @return Configuration Information Class/config bean
     * */
    ConfigBean get(int chainID);

    /**
     * Delete configuration information for a certain chain
     * Delete configuration information for a chain
     *
     * @param chainID chainID/chain id
     * @return Whether the deletion was successful/Delete success
     * */
    boolean delete(int chainID);

    /**
     * Obtain all chain information of the current node
     * Get all the chain information of the current node
     *
     * @return Node Information List/Node information list
     * */
    Map<Integer, ConfigBean> getList();
}

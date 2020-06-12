package io.nuls.api.db;

import io.nuls.api.model.po.SymbolRegInfo;

import java.util.List;
import java.util.Optional;

/**
 * @Author: zhoulijun
 * @Time: 2020-03-09 14:22
 * @Description: 币种注册信息
 */
public interface SymbolRegService {

    /**
     * 系统启动时，所有依赖模块ready后触发
     * 将把配置的币种信息和从异构跨链处获取到的配置信息初始化到mongodb中
     * 如果配置与异构跨链获取到的数据有重复，配置优先级高
     */
    void updateSymbolRegList();

    void save(SymbolRegInfo info);

    SymbolRegInfo get(int assetChainId,int assetId);

    List<SymbolRegInfo> get(String symbol);

    Optional<SymbolRegInfo> getFirst(String symbol);

    List<SymbolRegInfo> getAll();

}

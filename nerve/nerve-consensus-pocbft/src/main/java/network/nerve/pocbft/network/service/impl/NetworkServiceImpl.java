package network.nerve.pocbft.network.service.impl;

import network.nerve.pocbft.model.bo.Chain;
import network.nerve.pocbft.network.model.ConsensusKeys;
import network.nerve.pocbft.network.model.ConsensusNet;
import network.nerve.pocbft.network.model.message.ConsensusIdentitiesMsg;
import network.nerve.pocbft.network.model.message.ConsensusShareMsg;
import network.nerve.pocbft.network.model.message.sub.ConsensusShare;
import network.nerve.pocbft.network.service.ConsensusNetService;
import network.nerve.pocbft.network.service.NetworkService;
import network.nerve.pocbft.utils.manager.ChainManager;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.model.ArraysTool;
import io.nuls.core.model.StringUtils;
import io.nuls.core.rpc.info.Constants;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.core.rpc.util.NulsDateUtils;
import network.nerve.pocbft.network.constant.NetworkCmdConstant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static network.nerve.pocbft.network.constant.NetworkCmdConstant.POC_IDENTITY_MESSAGE;

/**
 * @author lanjinsheng
 * @date 2019/10/17
 * @description
 */
@Component
public class NetworkServiceImpl implements NetworkService {
    @Autowired
    private ChainManager chainManager;
    @Autowired
    private ConsensusNetService consensusNetService;

    @Override
    public boolean connectPeer(int chainId, String nodeId) {
        Chain chain = chainManager.getChainMap().get(chainId);
        try {
            Map<String, Object> params = new HashMap<>(3);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            params.put("nodeId", nodeId);
            Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, "nw_addDirectConnect", params);
            chain.getLogger().debug("连接一个节点：{}, result:{}",nodeId,response.isSuccess());
            if (response.isSuccess()) {
                Map data = (Map) ((Map) response.getResponseData()).get("nw_addDirectConnect");
                return Boolean.valueOf(data.get("value").toString());
            }
        } catch (Exception e) {
            chain.getLogger().error("", e);
            return false;
        }
        return false;
    }

    @Override
    public boolean addIps(int chainId, String groupFlag, List<String> ips) {
        Chain chain = chainManager.getChainMap().get(chainId);
        try {
            Map<String, Object> params = new HashMap<>(5);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("module", ModuleE.CS.abbr);
            params.put("groupFlag", groupFlag);
            params.put("ips", ips);
            Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, "nw_addIps", params);
            chain.getLogger().debug("增加节点ips.size：{}，result:{}",ips.size(),response.isSuccess());
            if (response.isSuccess()) {
                Map data = (Map) ((Map) response.getResponseData()).get("nw_addIps");
                return Boolean.valueOf(data.get("value").toString());
            }
        } catch (Exception e) {
            chain.getLogger().error("", e);
            return false;
        }
        return false;
    }

    @Override
    public boolean removeIps(int chainId, String groupFlag, List<String> ips) {
        Chain chain = chainManager.getChainMap().get(chainId);
        try {
            Map<String, Object> params = new HashMap<>(5);
            params.put(Constants.CHAIN_ID, chainId);
            params.put("module", ModuleE.CS.abbr);
            params.put("groupFlag", groupFlag);
            params.put("ips", ips);
            Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, "nw_removeIps", params);
            chain.getLogger().info("删除节点ips.size：{}，result:{}",ips.size(),response.isSuccess());
            if (response.isSuccess()) {
                Map data = (Map) ((Map) response.getResponseData()).get("nw_removeIps");
                return Boolean.valueOf(data.get("value").toString());
            }
        } catch (Exception e) {
            chain.getLogger().error("", e);
            return false;
        }
        return false;
    }

    @Override
    public String getSelfNodeId(int chainId) {
        Chain chain = chainManager.getChainMap().get(chainId);
        try {
            Map<String, Object> params = new HashMap<>(2);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, NetworkCmdConstant.NW_GET_EXTRANET_IP, params);
            if (response.isSuccess()) {
                Map data = (Map) ((Map) response.getResponseData()).get(NetworkCmdConstant.NW_GET_EXTRANET_IP);
                return data.get("nodeId").toString();
            }
        } catch (Exception e) {
            chain.getLogger().error("", e);
            return null;
        }
        return null;
    }

    @Override
    public boolean sendIdentityMessage(int chainId, String peerNodeId, byte[] peerPubKey) {
        return sendIdentityMessage(chainId, peerNodeId, peerPubKey, false);
    }

    @Override
    public boolean sendIdentityMessage(int chainId, String peerNodeId, byte[] peerPubKey, boolean broadCast) {
        Chain chain = chainManager.getChainMap().get(chainId);
        try {
            Map<String, Object> params = new HashMap<>(5);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            List<String> ips = new ArrayList<>();
            ips.add(peerNodeId.split(":")[0]);
            params.put("ips", ips);
            params.put("isForward",false);
            ConsensusNet consensusNet = new ConsensusNet();
            ConsensusKeys consensusKeys = consensusNetService.getSelfConsensusKeys(chainId);
            if (null == consensusKeys) {
                return false;
            }
            consensusNet.setPubKey(consensusKeys.getPubKey());
            String selfNodeId = getSelfNodeId(chainId);
            if (null == selfNodeId) {
                return false;
            }
            consensusNet.setNodeId(selfNodeId);
            ConsensusIdentitiesMsg consensusIdentitiesMsg = new ConsensusIdentitiesMsg(consensusNet);
            consensusIdentitiesMsg.addEncryptNodes(peerPubKey);
            consensusIdentitiesMsg.getConsensusIdentitiesSub().setMessageTime(NulsDateUtils.getCurrentTimeSeconds());
            consensusIdentitiesMsg.getConsensusIdentitiesSub().setBroadcast(broadCast);
            consensusIdentitiesMsg.signDatas(consensusKeys.getPrivKey());
            params.put("messageBody", HexUtil.encode(consensusIdentitiesMsg.serialize()));
            params.put("command", NetworkCmdConstant.POC_IDENTITY_MESSAGE);
            boolean result = ResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, NetworkCmdConstant.NW_BROADCAST_CONSENSUS_NET, params).isSuccess();
            //chain.getLogger().debug("broadcast: " + NetworkCmdConstant.POC_IDENTITY_MESSAGE + ", success:" + result);
            return result;
        } catch (Exception e) {
            chain.getLogger().error("", e);
            return false;
        }
    }

    @Override
    public boolean sendDisConnectMessage(Chain chain, List<String> ips) {
        try {
            Map<String, Object> params = new HashMap<>(5);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("ips", ips);
            chain.getLogger().info("共识节点列表：{}", ips.toString());
            ConsensusNet consensusNet = new ConsensusNet();
            ConsensusKeys consensusKeys = consensusNetService.getSelfConsensusKeys(chain.getChainId());
            consensusNet.setPubKey(consensusKeys.getPubKey());
            ConsensusIdentitiesMsg consensusIdentitiesMsg = new ConsensusIdentitiesMsg(consensusNet);
            consensusIdentitiesMsg.getConsensusIdentitiesSub().setMessageTime(NulsDateUtils.getCurrentTimeSeconds());
            consensusIdentitiesMsg.signDatas(consensusKeys.getPrivKey());
            params.put("messageBody", HexUtil.encode(consensusIdentitiesMsg.serialize()));
            params.put("command", NetworkCmdConstant.POC_DIS_CONN_MESSAGE);
            boolean result = ResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, NetworkCmdConstant.NW_BROADCAST_CONSENSUS_NET, params).isSuccess();
            chain.getLogger().info("发送断联消息，ips.size：{}，result:{}",ips.size(),result);
            //chain.getLogger().debug("broadcast: " + NetworkCmdConstant.POC_DIS_CONN_MESSAGE + ", success:" + result);
            return result;
        } catch (Exception e) {
            chain.getLogger().error("", e);
            return false;
        }
    }

    @Override
    public boolean sendShareMessage(int chainId, String peerNodeId, byte[] peerPubKey) {
        Chain chain = chainManager.getChainMap().get(chainId);
        try {
            Map<String, Object> params = new HashMap<>(5);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            List<String> ips = new ArrayList<>();
            ips.add(peerNodeId.split(":")[0]);
            params.put("ips", ips);
            ConsensusShare consensusShare = new ConsensusShare();
            List<ConsensusNet> list = consensusNetService.getHadConnConsensusNetList(chain);
            ConsensusKeys consensusKeys = consensusNetService.getSelfConsensusKeys(chainId);
            if (null == consensusKeys) {
                return false;
            }
            //添加自身信息
            String selfNodeId = getSelfNodeId(chainId);
            if (StringUtils.isNotBlank(selfNodeId)) {
                ConsensusNet selfConsensusNet = new ConsensusNet(consensusKeys.getPubKey(), selfNodeId);
                list.add(selfConsensusNet);
            }
            if (null == list) {
                return false;
            }
            consensusShare.setShareList(list);
            ConsensusShareMsg consensusShareMsg = new ConsensusShareMsg(consensusShare, peerPubKey, consensusKeys.getPubKey(), consensusKeys.getPrivKey());
            params.put("messageBody", HexUtil.encode(consensusShareMsg.serialize()));
            params.put("command", NetworkCmdConstant.POC_SHARE_MESSAGE);
            boolean result = ResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, NetworkCmdConstant.NW_BROADCAST_CONSENSUS_NET, params).isSuccess();
            //chain.getLogger().debug("broadcast: " + NetworkCmdConstant.POC_SHARE_MESSAGE + ", success:" + result);
            return result;
        } catch (Exception e) {
            chain.getLogger().error("", e);
            return false;
        }
    }

    /**
     * 排除接收节点，将新增的共识节点分享给其他已经连接的节点
     *
     * @param chainId
     * @param excludeNode
     * @param shareConsensusNet
     * @return
     */
    @Override
    public boolean sendShareMessageExNode(int chainId, String excludeNode, ConsensusNet shareConsensusNet) {
        Chain chain = chainManager.getChainMap().get(chainId);
        try {
            Map<String, Object> params = new HashMap<>(5);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chainId);
            ConsensusShare consensusShare = new ConsensusShare();
            List<ConsensusNet> list = consensusNetService.getHadConnConsensusNetList(chain);
            String excludeIp = excludeNode.split(":")[0];
            List<ConsensusNet> shareList = new ArrayList<>();
            shareList.add(shareConsensusNet);
            ConsensusKeys consensusKeys = consensusNetService.getSelfConsensusKeys(chainId);
            if (null == consensusKeys) {
                return false;
            }
            consensusShare.setShareList(shareList);
            for (ConsensusNet consensusNet : list) {
                if (null == consensusNet || consensusNet.getNodeId() == null) {
                    continue;
                }
                String sendIp = consensusNet.getNodeId().split(":")[0];
                if (!excludeIp.equals(sendIp)) {
                    List<String> ips = new ArrayList<>();
                    ips.add(sendIp);
                    params.put("ips", ips);
                    ConsensusShareMsg consensusShareMsg = new ConsensusShareMsg(consensusShare, consensusNet.getPubKey(), consensusKeys.getPubKey(), consensusKeys.getPrivKey());
                    //chain.getLogger().debug("===============shareMessage identityList={} to peer ={}",shareConsensusNet.getNodeId(),sendIp);
                    params.put("messageBody", HexUtil.encode(consensusShareMsg.serialize()));
                    params.put("command", NetworkCmdConstant.POC_SHARE_MESSAGE);
                    boolean result = ResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, NetworkCmdConstant.NW_BROADCAST_CONSENSUS_NET, params).isSuccess();
                    //chain.getLogger().debug("send poc peer: " + NetworkCmdConstant.POC_SHARE_MESSAGE + ", success:" + result);
                }
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    @Override
    public List<String> getPocNodes(Chain chain) {
        try {
            Map<String, Object> params = new HashMap<>(3);
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("module", ModuleE.CS.abbr);
            params.put("groupFlag", NetworkCmdConstant.NW_GROUP_FLAG);
            Response response = ResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, "nw_getBusinessGroupNodes", params);
            if (response.isSuccess()) {
                Map data = (Map) ((Map) response.getResponseData()).get("nw_getBusinessGroupNodes");
                if (null != data.get("list")) {
                    return (List) data.get("list");
                }
            }
            return new ArrayList<>();
        } catch (Exception e) {
            chain.getLogger().error("", e);
        }
        return null;
    }

    @Override
    public boolean broadCastIdentityMsg(Chain chain, String command, String msgStr, String excludeNodeId) {
        try {
            Map<String, Object> params = new HashMap<>(5);
            params.put(Constants.VERSION_KEY_STR, "1.0");
            params.put(Constants.CHAIN_ID, chain.getChainId());
            params.put("excludeNodes", excludeNodeId);
            params.put("messageBody", msgStr);
            params.put("command", command);
            boolean success = ResponseMessageProcessor.requestAndResponse(ModuleE.NW.abbr, NetworkCmdConstant.NW_BROADCAST_JOIN_CONSENSUS, params).isSuccess();
//            //chain.getLogger().debug("broadcast: " + command + ", success:" + success);
            return success;
        } catch (Exception e) {
            chain.getLogger().error("", e);
            return false;
        }
    }

    @Override
    public boolean broadCastIdentityMsg(Chain chain) {
        try {
            if (consensusNetService.allConnected(chain)) {
                return true;
            }
            String nodeId = getSelfNodeId(chain.getChainId());
            if (StringUtils.isBlank(nodeId)) {
                chain.getLogger().info("本地ip地址端口获取失败");
                return false;
            }
            List<ConsensusNet> consensusSeedPubKeyList = consensusNetService.getAllConsensusNetList(chain);
            if (null != consensusSeedPubKeyList) {
                ConsensusKeys consensusKeys = consensusNetService.getSelfConsensusKeys(chain.getChainId());
                ConsensusNet selfConsensusNet = new ConsensusNet();
                selfConsensusNet.setPubKey(consensusKeys.getPubKey());
                selfConsensusNet.setNodeId(nodeId);
                ConsensusIdentitiesMsg consensusIdentitiesMsg = new ConsensusIdentitiesMsg(selfConsensusNet);
                consensusIdentitiesMsg.getConsensusIdentitiesSub().setBroadcast(true);
                boolean broadMessage = false;
                for (ConsensusNet consensusNet : consensusSeedPubKeyList) {
                    //如果已经建立连接的节点不需要再加密广播
                    if (!consensusNet.isHadConnect() && !ArraysTool.arrayEquals(consensusNet.getPubKey(), consensusKeys.getPubKey()) &&
                            consensusNet.getPubKey().length > 0) {
                        broadMessage = true;
                        try {
                            consensusIdentitiesMsg.addEncryptNodes(consensusNet.getPubKey());
                        } catch (Exception e) {
                            chain.getLogger().error(e);
                            return false;
                        }
                    }
                }
                if (broadMessage) {
                    consensusIdentitiesMsg.signDatas(consensusKeys.getPrivKey());
//                    //chain.getLogger().debug("===============send broadCastIdentityMsg");
                    return broadCastIdentityMsg(chain, POC_IDENTITY_MESSAGE, HexUtil.encode(consensusIdentitiesMsg.serialize()), null);
                }
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error("", e);
            return false;
        }
    }


}

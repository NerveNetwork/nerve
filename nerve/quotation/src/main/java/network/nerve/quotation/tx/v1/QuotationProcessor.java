/**
 * MIT License
 * <p>
 * Copyright (c) 2019-2020 nerve.network
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

package network.nerve.quotation.tx.v1;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.Transaction;
import io.nuls.base.protocol.TransactionProcessor;
import io.nuls.base.signture.P2PHKSignature;
import io.nuls.base.signture.TransactionSignature;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.ECKey;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;
import io.nuls.core.model.StringUtils;
import network.nerve.quotation.constant.QuotationConstant;
import network.nerve.quotation.constant.QuotationErrorCode;
import network.nerve.quotation.manager.ChainManager;
import network.nerve.quotation.model.bo.Chain;
import network.nerve.quotation.model.po.NodeQuotationPO;
import network.nerve.quotation.model.po.NodeQuotationWrapperPO;
import network.nerve.quotation.model.txdata.Price;
import network.nerve.quotation.model.txdata.Prices;
import network.nerve.quotation.model.txdata.Quotation;
import network.nerve.quotation.rpc.call.QuotationCall;
import network.nerve.quotation.storage.QuotationStorageService;
import network.nerve.quotation.util.CommonUtil;
import network.nerve.quotation.util.LoggerUtil;
import network.nerve.quotation.util.TimeUtil;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author: Loki
 * @date: 2019/11/25
 */
@Component("QuotationProcessorV1")
public class QuotationProcessor implements TransactionProcessor {

    @Autowired
    private ChainManager chainManager;
    @Autowired
    private QuotationStorageService quotationStorageService;

    @Override
    public int getType() {
        return TxType.QUOTATION;
    }

    @Override
    public Map<String, Object> validate(int chainId, List<Transaction> txs, Map<Integer, List<Transaction>> txMap, BlockHeader blockHeader) {
        if (txs.isEmpty()) {
            return null;
        }
        //业务数据校验
        Chain chain = null;
        Map<String, Object> result = null;
        try {
            chain = chainManager.getChain(chainId);
            NulsLogger log = chain.getLogger();
            String errorCode = null;
            result = new HashMap<>(QuotationConstant.INIT_CAPACITY_4);
            List<Transaction> failsList = new ArrayList<>();
            for (Transaction tx : txs) {
                //验证业务数据
                Quotation quotation = CommonUtil.getInstance(tx.getTxData(), Quotation.class);
                byte type = quotation.getType();
                if (QuotationConstant.QUOTE_TXDATA_TYPE != type) {
                    failsList.add(tx);
                    errorCode = QuotationErrorCode.TXDATA_ERROR.getCode();
                    log.error(QuotationErrorCode.TXDATA_ERROR.getMsg());
                }
                byte[] data = quotation.getData();
                if (null == data || data.length == 0) {
                    failsList.add(tx);
                    errorCode = QuotationErrorCode.TXDATA_EMPTY.getCode();
                    log.error(QuotationErrorCode.TXDATA_EMPTY.getMsg());
                }
                Prices prices = CommonUtil.getInstance(data, Prices.class);
                for (Price price : prices.getPrices()) {
                    if (StringUtils.isBlank(price.getKey()) || price.getValue() <= 0) {
                        failsList.add(tx);
                        errorCode = QuotationErrorCode.TXDATA_ERROR.getCode();
                        log.error(QuotationErrorCode.TXDATA_ERROR.getMsg());
                    }
                }
                if (tx.getCoinData() != null && tx.getCoinData().length != 0) {
                    failsList.add(tx);
                    errorCode = QuotationErrorCode.QUOTATION_COINDATA_NOT_EMPTY.getCode();
                    log.error(QuotationErrorCode.QUOTATION_COINDATA_NOT_EMPTY.getMsg());
                }
                //验证签名, 验证签名者在该区块时间是不是共识节点出块地址
                signatureValidation(chain, tx, blockHeader, quotation);
            }
            result.put("txList", failsList);
            result.put("errorCode", errorCode);
        } catch (Exception e) {
            errorLogProcess(chain, e);
            result.put("txList", txs);
            result.put("errorCode", QuotationErrorCode.SYS_UNKOWN_EXCEPTION.getCode());
        }
        return result;
    }

    @Override
    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus) {
        return commit(chainId, txs, blockHeader, syncStatus, true);
    }

    public boolean commit(int chainId, List<Transaction> txs, BlockHeader blockHeader, int syncStatus, boolean failRollback) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = null;
        try {
            chain = chainManager.getChain(chainId);
            //得到key前缀
            String keyPrefix = TimeUtil.toUTCDate(blockHeader.getTime());
            /**
             * 组装需要存的业务数据
             * key:token, value:该token的报价业务数据
             * 存db时将前缀与该key组合, 将值存入(或加入已有的)NodeQuotationsPO数据中
             */
            Map<String, List<NodeQuotationPO>> saveMap = new HashMap<>();
            for (Transaction tx : txs) {
                Quotation quotation = CommonUtil.getInstance(tx.getTxData(), Quotation.class);
                if (QuotationConstant.QUOTE_TXDATA_TYPE == quotation.getType()) {
                    Prices prices = CommonUtil.getInstance(quotation.getData(), Prices.class);
                    for (Price price : prices.getPrices()) {
                        List<NodeQuotationPO> list = saveMap.computeIfAbsent(price.getKey(), k -> new ArrayList<>());
                        NodeQuotationPO nodeQuotationPO = new NodeQuotationPO();
                        nodeQuotationPO.setTxHash(tx.getHash().toHex());
                        nodeQuotationPO.setBlockTime(tx.getTime());
                        nodeQuotationPO.setPrice(price.getValue());
                        nodeQuotationPO.setToken(price.getKey());
                        nodeQuotationPO.setAddress(getSignAddress(chain.getChainId(), tx.getTransactionSignature()));
                        list.add(nodeQuotationPO);
                    }
                }
            }
            for (Map.Entry<String, List<NodeQuotationPO>> entry : saveMap.entrySet()) {
                String key = CommonUtil.assembleKey(keyPrefix, entry.getKey());
                NodeQuotationWrapperPO nqWrapperPO = quotationStorageService.getNodeQuotationsBykey(chain, key);
                if (null != nqWrapperPO && null != nqWrapperPO.getList() && !nqWrapperPO.getList().isEmpty()) {
                    nqWrapperPO.getList().addAll(entry.getValue());
                } else {
                    nqWrapperPO = new NodeQuotationWrapperPO(entry.getValue());
                }
                quotationStorageService.saveNodeQuotation(chain, key, nqWrapperPO);
                for(NodeQuotationPO po : nqWrapperPO.getList()) {
                    chain.getLogger().info("[commit] 确认普通报价 General-quotation key:{}, dbKey:{}, price:{}, hash:{}",
                            entry.getKey(), key, (new BigDecimal(Double.toString(po.getPrice()))).toPlainString() , po.getTxHash());
                }
            }
            return true;
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            if (failRollback) {
                rollback(chainId, txs, blockHeader, false);
            }
            return false;
        }
    }

    @Override
    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader) {
        return rollback(chainId, txs, blockHeader, true);
    }

    public boolean rollback(int chainId, List<Transaction> txs, BlockHeader blockHeader, boolean failCommit) {
        if (txs.isEmpty()) {
            return true;
        }
        Chain chain = null;
        try {
            chain = chainManager.getChain(chainId);
            //组装key
            String keyPrefix = TimeUtil.toUTCDate(blockHeader.getTime());
            Map<String, List<NodeQuotationPO>> saveMap = new HashMap<>();
            for (Transaction tx : txs) {
                //验证业务数据
                Quotation quotation = CommonUtil.getInstance(tx.getTxData(), Quotation.class);
                if (quotation.getType() == QuotationConstant.QUOTE_TXDATA_TYPE) {
                    Prices prices = CommonUtil.getInstance(quotation.getData(), Prices.class);
                    for (Price price : prices.getPrices()) {
                        List<NodeQuotationPO> list = saveMap.computeIfAbsent(price.getKey(), k -> new ArrayList<>());
                        NodeQuotationPO nodeQuotationPO = new NodeQuotationPO();
                        nodeQuotationPO.setTxHash(tx.getHash().toHex());
                        nodeQuotationPO.setBlockTime(tx.getTime());
                        nodeQuotationPO.setPrice(price.getValue());
                        nodeQuotationPO.setToken(price.getKey());
                        nodeQuotationPO.setAddress(getSignAddress(chain.getChainId(), tx.getTransactionSignature()));
                        list.add(nodeQuotationPO);
                    }
                }
            }

            for (Map.Entry<String, List<NodeQuotationPO>> entry : saveMap.entrySet()) {
                String key = CommonUtil.assembleKey(keyPrefix, entry.getKey());
                NodeQuotationWrapperPO nodeQuotationWrapperPO = quotationStorageService.getNodeQuotationsBykey(chain, key);
                LoggerUtil.LOG.debug("start -{}", key);
                LoggerUtil.LOG.debug("{}",nodeQuotationWrapperPO.toString());
                List<NodeQuotationPO> list = nodeQuotationWrapperPO.getList();
                boolean rs = false;
                if (null != nodeQuotationWrapperPO && null != list) {
                    //数据库的数据
                    Iterator<NodeQuotationPO> it = list.iterator();
                    while (it.hasNext()) {
                        NodeQuotationPO nqPO = it.next();
                        //需要回滚的数据
                        List<NodeQuotationPO> rollbackList = entry.getValue();
                        for (NodeQuotationPO quotationRollback : rollbackList) {
                            if (nqPO.getTxHash().equals(quotationRollback.getTxHash())) {
                                it.remove();
                                rs = true;
                            }
                        }
                    }
                }
                if (rs) {
                    quotationStorageService.saveNodeQuotation(chain, key, nodeQuotationWrapperPO);
                }
                NodeQuotationWrapperPO end = quotationStorageService.getNodeQuotationsBykey(chain, key);
                LoggerUtil.LOG.debug("end -{}", key);
                LoggerUtil.LOG.debug("{}",end.toString());
            }
            return true;
        } catch (NulsException e) {
            errorLogProcess(chain, e);
            if (failCommit) {
                commit(chainId, txs, blockHeader,0, false);
            }
            return false;
        }
    }


    /**
     * 喂价交易签名验证
     * 1.验证交易签名者是否是共识节点
     * - 如果交易没有打包进入区块, 则验证签名者是否是当前轮次的共识节点
     * - 如果交易已经打包进入区块, 则验证签名者是否是该区块轮次中的共识节点
     * 2.验证签名正确性(签名者是否有该签名地址的私钥)
     *
     * @param tx
     * @return
     * @throws NulsException
     */
    private void signatureValidation(Chain chain, Transaction tx, BlockHeader blockHeader, Quotation quotation) throws NulsException {
        TransactionSignature transactionSignature = new TransactionSignature();
        transactionSignature.parse(tx.getTransactionSignature(), 0);
        if ((transactionSignature.getP2PHKSignatures() == null || transactionSignature.getP2PHKSignatures().size() == 0)) {
            throw new NulsException(QuotationErrorCode.TX_UNSIGNED);
        }
        boolean rs = false;
        for (P2PHKSignature signature : transactionSignature.getP2PHKSignatures()) {
            byte[] addressBytes = AddressTool.getAddress(signature.getPublicKey(), chain.getChainId());
            String address = AddressTool.getStringAddressByBytes(addressBytes);
            if (!QuotationCall.isConsensusNode(chain, blockHeader, address)) {
                throw new NulsException(QuotationErrorCode.NO_AUTHORITY_TO_SIGN);
            }
            if (!ECKey.verify(tx.getHash().getBytes(), signature.getSignData().getSignBytes(), signature.getPublicKey())) {
                throw new NulsException(QuotationErrorCode.SIGNATURE_ERROR);
            }
            if(Arrays.equals(quotation.getAddress(), AddressTool.getAddress(signature.getPublicKey(), chain.getChainId()))){
                //txData中的出块地址 是不是在签名地址列表中
                rs = true;
            }
        }
        if(!rs){
            throw new NulsException(QuotationErrorCode.NO_AUTHORITY_TO_SIGN);
        }
    }

    /**
     * 根据签名列表获取第一个签名的地址
     *
     * @param chainId
     * @param txSignatureData
     * @return
     * @throws NulsException
     */
    private String getSignAddress(int chainId, byte[] txSignatureData) throws NulsException {
        TransactionSignature transactionSignature = new TransactionSignature();
        transactionSignature.parse(txSignatureData, 0);
        if ((transactionSignature.getP2PHKSignatures() == null || transactionSignature.getP2PHKSignatures().size() == 0)) {
            throw new NulsException(QuotationErrorCode.TX_UNSIGNED);
        }
        byte[] addressBytes = AddressTool.getAddress(transactionSignature.getP2PHKSignatures().get(0).getPublicKey(), chainId);
        return AddressTool.getStringAddressByBytes(addressBytes);
    }


    private void errorLogProcess(Chain chain, Exception e) {
        if (chain == null) {
            LoggerUtil.LOG.error(e);
        } else {
            chain.getLogger().error(e);
        }
    }
}

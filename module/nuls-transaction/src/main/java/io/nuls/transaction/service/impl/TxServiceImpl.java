/*
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
package io.nuls.transaction.service.impl;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.*;
import io.nuls.base.protocol.TxRegisterDetail;
import io.nuls.base.signture.MultiSignTxSignature;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.core.constant.BaseConstant;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.constant.TxStatusEnum;
import io.nuls.core.constant.TxType;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.crypto.HexUtil;
import io.nuls.core.exception.NulsException;
import io.nuls.core.model.BigIntegerUtils;
import io.nuls.core.model.StringUtils;
import io.nuls.core.parse.JSONUtils;
import io.nuls.core.parse.SerializeUtils;
import io.nuls.core.rpc.model.ModuleE;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import io.nuls.transaction.cache.PackablePool;
import io.nuls.transaction.constant.TxConfig;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxContext;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.manager.TxManager;
import io.nuls.transaction.model.bo.*;
import io.nuls.transaction.model.dto.ModuleTxRegisterDTO;
import io.nuls.transaction.model.po.TransactionConfirmedPO;
import io.nuls.transaction.model.po.TransactionNetPO;
import io.nuls.transaction.model.po.TransactionUnconfirmedPO;
import io.nuls.transaction.rpc.call.*;
import io.nuls.transaction.service.ConfirmedTxService;
import io.nuls.transaction.service.TxService;
import io.nuls.transaction.storage.ConfirmedTxStorageService;
import io.nuls.transaction.storage.LockedAddressStorageService;
import io.nuls.transaction.storage.UnconfirmedTxStorageService;
import io.nuls.transaction.utils.LoggerUtil;
import io.nuls.transaction.utils.TxDuplicateRemoval;
import io.nuls.transaction.utils.TxUtil;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static io.nuls.transaction.constant.TxConstant.CACHED_SIZE;
import static io.nuls.transaction.constant.TxConstant.PACKING;
import static io.nuls.transaction.constant.TxContext.TX_MAX_SIZE;

/**
 * @author: Charlie
 * @date: 2018/11/22
 */
@Component
public class TxServiceImpl implements TxService {

    @Autowired
    private PackablePool packablePool;

    @Autowired
    private UnconfirmedTxStorageService unconfirmedTxStorageService;

    @Autowired
    private ConfirmedTxService confirmedTxService;

    @Autowired
    private ConfirmedTxStorageService confirmedTxStorageService;

    @Autowired
    private LockedAddressStorageService lockedAddressStorageService;
    @Autowired
    private TxConfig txConfig;

    private ExecutorService verifySignExecutor = ThreadUtils.createThreadPool(Runtime.getRuntime().availableProcessors(), CACHED_SIZE, new NulsThreadFactory(TxConstant.VERIFY_TX_SIGN_THREAD));

    @Override
    public boolean register(Chain chain, ModuleTxRegisterDTO moduleTxRegisterDto) {
        try {
            for (TxRegisterDetail txRegisterDto : moduleTxRegisterDto.getList()) {
                TxRegister txRegister = new TxRegister();
                txRegister.setModuleCode(moduleTxRegisterDto.getModuleCode());
                txRegister.setTxType(txRegisterDto.getTxType());
                txRegister.setSystemTx(txRegisterDto.getSystemTx());
                txRegister.setUnlockTx(txRegisterDto.getUnlockTx());
                txRegister.setVerifySignature(txRegisterDto.getVerifySignature());
                txRegister.setVerifyFee(txRegisterDto.getVerifyFee());
                txRegister.setPackProduce(txRegisterDto.getPackProduce());
                txRegister.setPackGenerate(txRegisterDto.getPackGenerate());
                chain.getTxRegisterMap().put(txRegister.getTxType(), txRegister);
                chain.getLogger().info("register:{}", JSONUtils.obj2json(txRegister));
            }
            List<Integer> delList = moduleTxRegisterDto.getDelList();
            if (!delList.isEmpty()) {
                delList.forEach(e -> chain.getTxRegisterMap().remove(e));
            }
            if (chain.getProcessTxStatus().get()) {
                // 通知区块有新交易注册(清理系统交易缓存)
                BlockCall.txRegisterNotify(chain);
            }
            return true;
        } catch (Exception e) {
            chain.getLogger().error(e);
        }
        return false;
    }

    @Override
    public void newBroadcastTx(Chain chain, TransactionNetPO txNet) {
        Transaction tx = txNet.getTx();
        if (!isTxExists(chain, tx.getHash())) {
            try {
                //执行交易基础验证
                verifyTransactionInCirculation(chain, tx, false);
                chain.getUnverifiedQueue().addLast(txNet);
            } catch (NulsException e) {
                chain.getLogger().error(e);
            } catch (IllegalStateException e) {
                chain.getLogger().error("UnverifiedQueue full!");
            }
        }
    }


    @Override
    public void newTx(Chain chain, Transaction tx) throws NulsException {
        try {
            if (!chain.getProcessTxStatus().get()) {
                //节点区块同步中或回滚中,暂停接纳新交易
                throw new NulsException(TxErrorCode.PAUSE_NEWTX);
            }
            NulsHash hash = tx.getHash();
            if (isTxExists(chain, hash)) {
                throw new NulsException(TxErrorCode.TX_ALREADY_EXISTS);
            }
            VerifyResult verifyResult = verify(chain, tx);
            if (!verifyResult.getResult()) {
                chain.getLogger().error("verify failed: type:{} - txhash:{}, code:{}",
                        tx.getType(), hash.toHex(), verifyResult.getErrorCode().getCode());
//                throw new NulsException(ErrorCode.init(verifyResult.getErrorCode().getCode()));
                throw new NulsException(verifyResult.getErrorCode());
            }
            VerifyLedgerResult verifyLedgerResult = LedgerCall.commitUnconfirmedTx(chain, RPCUtil.encode(tx.serialize()));
            if (!verifyLedgerResult.businessSuccess()) {

                String errorCode = verifyLedgerResult.getErrorCode() == null ? TxErrorCode.ORPHAN_TX.getCode() : verifyLedgerResult.getErrorCode().getCode();
                chain.getLogger().error(
                        "coinData verify fail - orphan: {}, - code:{}, type:{} - txhash:{}", verifyLedgerResult.getOrphan(),
                        errorCode, tx.getType(), hash.toHex());
                throw new NulsException(ErrorCode.init(errorCode));
            }
            if (chain.getPackaging().get()) {
                //如果map满了则不一定能加入待打包队列
                packablePool.add(chain, tx);
            }
            unconfirmedTxStorageService.putTx(chain.getChainId(), tx);
            //系统交易 不广播
            TxRegister txRegister = TxManager.getTxRegister(chain, tx.getType());
            if (txRegister.getSystemTx()) {
                return;
            }
            //广播完整交易
            boolean broadcastResult = false;
            for (int i = 0; i < 3; i++) {
                if (txRegister.getModuleCode().equals(ModuleE.CC.abbr)) {
                    broadcastResult = NetworkCall.forwardTxHash(chain, tx.getHash());
                } else {
                    broadcastResult = NetworkCall.broadcastTx(chain, tx);
                }

                if (broadcastResult) {
                    break;
                }
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    chain.getLogger().error(e);
                }
            }
            if (!broadcastResult) {
                throw new NulsException(TxErrorCode.TX_BROADCAST_FAIL);
            }
            //加入去重过滤集合,防止其他节点转发回来再次处理该交易
            TxDuplicateRemoval.insertAndCheck(hash.toHex());

        } catch (IOException e) {
            throw new NulsException(TxErrorCode.DESERIALIZE_ERROR);
        } catch (RuntimeException e) {
            chain.getLogger().error(e);
            throw new NulsException(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }


    @Override
    public TransactionConfirmedPO getTransaction(Chain chain, NulsHash hash) {
        TransactionUnconfirmedPO txPo = unconfirmedTxStorageService.getTx(chain.getChainId(), hash);
        if (null != txPo) {
            return new TransactionConfirmedPO(txPo.getTx(), -1L, TxStatusEnum.UNCONFIRM.getStatus());
        } else {
            return confirmedTxService.getConfirmedTransaction(chain, hash);
        }
    }

    @Override
    public boolean isTxExists(Chain chain, NulsHash hash) {
        boolean rs = unconfirmedTxStorageService.isExists(chain.getChainId(), hash);
        if (!rs) {
            rs = confirmedTxStorageService.isExists(chain.getChainId(), hash);
        }
        return rs;
    }

    /**
     * 交易合法流通与基础验证
     *
     * @param chain
     * @param tx
     * @param localTx 是否为本地发起的交易(允许本地模块发起系统级别交易)
     * @throws NulsException
     */
    public void verifyTransactionInCirculation(Chain chain, Transaction tx, boolean localTx) throws NulsException {
        TxRegister txRegister = TxManager.getTxRegister(chain, tx.getType());
        if (null == txRegister) {
            throw new NulsException(TxErrorCode.TX_TYPE_INVALID);
        }

        if (!localTx && txRegister.getSystemTx()) {
            throw new NulsException(TxErrorCode.SYS_TX_TYPE_NON_CIRCULATING);
        }
        baseValidateTx(chain, tx, txRegister);
    }

    @Override
    public VerifyResult verify(Chain chain, Transaction tx) {
        try {
            verifyTransactionInCirculation(chain, tx, true);
            TxRegister txRegister = TxManager.getTxRegister(chain, tx.getType());
            Map<String, Object> result = TransactionCall.txModuleValidator(chain, txRegister.getModuleCode(), RPCUtil.encode(tx.serialize()));
            List<String> txHashList = (List<String>) result.get("list");
            if (txHashList.isEmpty()) {
                return VerifyResult.success();
            } else {
                chain.getLogger().error("tx validator fail -type:{}, -hash:{} ", tx.getType(), tx.getHash().toHex());
                String errorCodeStr = (String) result.get("errorCode");
                ErrorCode errorCode = null == errorCodeStr ? TxErrorCode.SYS_UNKOWN_EXCEPTION : ErrorCode.init(errorCodeStr);
                return VerifyResult.fail(errorCode);
            }
        } catch (IOException e) {
            return VerifyResult.fail(TxErrorCode.SERIALIZE_ERROR);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return VerifyResult.fail(e.getErrorCode());
        } catch (Exception e) {
            chain.getLogger().error(e);
            return VerifyResult.fail(TxErrorCode.SYS_UNKOWN_EXCEPTION);
        }
    }

    @Override
    public void baseValidateTx(Chain chain, Transaction tx, TxRegister txRegister) throws NulsException {
        baseValidateTx(chain, tx, txRegister, null);
    }

    @Override
    public void baseValidateTx(Chain chain, Transaction tx, TxRegister txRegister, Long height) throws NulsException {
        if (null == tx) {
            throw new NulsException(TxErrorCode.TX_NOT_EXIST);
        }
        if (tx.getHash() == null || !tx.getHash().verify()) {
            throw new NulsException(TxErrorCode.HASH_ERROR);
        }
        if (!TxManager.contains(chain, tx.getType())) {
            throw new NulsException(TxErrorCode.TX_TYPE_INVALID);
        }
        if (tx.getTime() == 0L) {
            throw new NulsException(TxErrorCode.TX_DATA_VALIDATION_ERROR);
        }
        if (tx.size() > TX_MAX_SIZE) {
            throw new NulsException(TxErrorCode.TX_SIZE_TOO_LARGE);
        }

//        if (tx.getType() == TxType.REGISTER_AGENT || tx.getType() == TxType.DEPOSIT) {
//            throw new NulsException(TxErrorCode.TX_TYPE_INVALID);
//        }

        //验证签名
        try {
            validateTxSignature(tx, txRegister, chain);
        } catch (NulsException e) {
           throw e;
        }catch (Exception e) {
            throw new NulsException(TxErrorCode.SIGNATURE_ERROR);
        }
        
        //如果有coinData, 则进行验证,有一些交易(黄牌)没有coinData数据
        if (tx.getType() == TxType.FINAL_QUOTATION
                || tx.getType() == TxType.QUOTATION
                || tx.getType() == TxType.YELLOW_PUNISH
                || tx.getType() == TxType.VERIFIER_CHANGE
                || tx.getType() == TxType.VERIFIER_INIT
                || tx.getType() == TxType.REGISTERED_CHAIN_CHANGE) {
            if (null != tx.getCoinData() && tx.getCoinData().length > 0) {
                chain.getLogger().error("This transaction type does not allow the existence of CoinData. hash:{}, type:{}",
                        tx.getHash().toHex(), tx.getType());
                throw new NulsException(TxErrorCode.TX_VERIFY_FAIL);
            }
            return;
        }
        if (null == tx.getCoinData() && txRegister.getSystemTx()) {
            return;
        }
        CoinData coinData = null;
        try {
            coinData = TxUtil.getCoinData(tx);
        } catch (NulsException e) {
            throw new NulsException(TxErrorCode.COINDATA_NOT_FOUND);
        }
        validateCoinFromBase(chain, txRegister, coinData.getFrom());
        validateCoinToBase(chain, txRegister, coinData.getTo(), height);

        if (txRegister.getVerifyFee()) {
            validateFee(chain, tx, coinData, txRegister);
        }
    }

    /**
     * 验证签名 只需要验证,需要验证签名的交易(一些系统交易不用签名)
     * 验证签名数据中的公钥和from中是否匹配, 验证签名正确性
     *
     * @param tx
     * @throws NulsException
     */
    private void validateTxSignature(Transaction tx, TxRegister txRegister, Chain chain) throws NulsException {
        //只需要验证,需要验证签名的交易(一些系统交易不用签名)
        if (!txRegister.getVerifySignature() || txRegister.getModuleCode().equals(ModuleE.CC.abbr)) {
            //注册时不需要验证签名的交易(一些系统交易),以及跨链模块的交易(单独处理).
            return;
        }
        CoinData coinData = TxUtil.getCoinData(tx);
        if (null == coinData || null == coinData.getFrom() || coinData.getFrom().size() <= 0) {
            throw new NulsException(TxErrorCode.COINDATA_NOT_FOUND);
        }
        //获取交易签名者地址列表
        Set<String> addressSet = SignatureUtil.getAddressFromTX(tx, chain.getChainId());
        if (addressSet == null) {
            throw new NulsException(TxErrorCode.SIGNATURE_ERROR);
        }
        int chainId = chain.getChainId();
        byte[] multiSignAddress = null;
        if (tx.isMultiSignTx()) {
            /**
             * 如果是多签交易, 则先从签名对象中取出多签地址原始创建者的公钥列表和最小签名数,
             * 生成一个新的多签地址,来与交易from中的多签地址匹配，匹配不上这验证不通过.
             */
            MultiSignTxSignature multiSignTxSignature = new MultiSignTxSignature();
            multiSignTxSignature.parse(new NulsByteBuffer(tx.getTransactionSignature()));
            //验证签名者够不够最小签名数
            if (addressSet.size() < multiSignTxSignature.getM()) {
                throw new NulsException(TxErrorCode.INSUFFICIENT_SIGNATURES);
            }
            //签名者是否是多签账户创建者之一
            for (String address : addressSet) {
                boolean rs = false;
                for (byte[] bytes : multiSignTxSignature.getPubKeyList()) {
                    String addr = AddressTool.getStringAddressByBytes(AddressTool.getAddress(bytes, chainId));
                    if (address.equals(addr)) {
                        rs = true;
                        break;
                    }
                }
                if (!rs) {
                    throw new NulsException(TxErrorCode.SIGN_ADDRESS_NOT_MATCH_COINFROM);
                }
            }
            //生成一个多签地址
            List<String> pubKeys = new ArrayList<>();
            for (byte[] pubkey : multiSignTxSignature.getPubKeyList()) {
                pubKeys.add(HexUtil.encode(pubkey));
            }
            try {
                byte[] hash160 = SerializeUtils.sha256hash160(AddressTool.createMultiSigAccountOriginBytes(chainId, multiSignTxSignature.getM(), pubKeys));
                Address address = new Address(chainId, BaseConstant.P2SH_ADDRESS_TYPE, hash160);
                multiSignAddress = address.getAddressBytes();
            } catch (Exception e) {
                chain.getLogger().error(e);
                throw new NulsException(TxErrorCode.SIGNATURE_ERROR);
            }
        }
        for (CoinFrom coinFrom : coinData.getFrom()) {
            if (tx.isMultiSignTx()) {
                if (!Arrays.equals(coinFrom.getAddress(), multiSignAddress)) {
                    throw new NulsException(TxErrorCode.SIGNATURE_ERROR);
                }
            } else if (!addressSet.contains(AddressTool.getStringAddressByBytes(coinFrom.getAddress()))) {
                throw new NulsException(TxErrorCode.SIGN_ADDRESS_NOT_MATCH_COINFROM);
            }
            if (tx.getType() == TxType.STOP_AGENT) {
                //停止节点from中第一笔为签名地址, 只验证from中第一个
                break;
            }
        }
        if (!SignatureUtil.validateTransactionSignture(chainId, tx)) {
            throw new NulsException(TxErrorCode.SIGNATURE_ERROR);
        }
    }

    private void validateCoinFromBase(Chain chain, TxRegister txRegister, List<CoinFrom> listFrom) throws NulsException {
        // add by pierre at 2021/8/30 限制最大交易金额
        if (listFrom != null && !listFrom.isEmpty()) {
            for (CoinFrom coinFrom : listFrom) {
                BigInteger fromAmount = coinFrom.getAmount();
                if (fromAmount.compareTo(TxConstant.MAX_SUPPORT_AMOUNT) > 0) {
                    throw new NulsException(TxErrorCode.DATA_ERROR);
                }
            }
        }
        // end code by pierre
        int type = txRegister.getTxType();
        //coinBase交易/智能合约退还gas交易没有from
        if (type == TxType.COIN_BASE || type == TxType.CONTRACT_RETURN_GAS) {
            return;
        }
        if (null == listFrom || listFrom.size() == 0) {
            if (txRegister.getSystemTx()) {
                //系统交易允许为空,交由验证器进行验证
                return;
            }
            throw new NulsException(TxErrorCode.COINFROM_NOT_FOUND);
        }
        int chainId = chain.getConfig().getChainId();
        //验证支付方是不是属于同一条链
        Integer fromChainId = null;
        Set<String> uniqueCoin = new HashSet<>();
        byte[] existMultiSignAddress = null;
        for (CoinFrom coinFrom : listFrom) {
            byte[] addrBytes = coinFrom.getAddress();
            String addr = AddressTool.getStringAddressByBytes(addrBytes);
            //验证交易地址合法性,跨链模块交易需要取地址中的原始链id来验证
            int validAddressChainId = chainId;
            if (ModuleE.CC.abbr.equals(txRegister.getModuleCode())) {
                validAddressChainId = AddressTool.getChainIdByAddress(addrBytes);
            }
            if (!AddressTool.validAddress(validAddressChainId, addr)) {
                throw new NulsException(TxErrorCode.INVALID_ADDRESS);
            }
            if (null == existMultiSignAddress && AddressTool.isMultiSignAddress(addrBytes)) {
                existMultiSignAddress = addrBytes;
            }
            int addrChainId = AddressTool.getChainIdByAddress(addrBytes);
            BigInteger fromAmount = coinFrom.getAmount();
            if (fromAmount.compareTo(BigInteger.ZERO) < 0) {
                throw new NulsException(TxErrorCode.DATA_ERROR);
            }
            //所有from是否是同一条链的地址
            if (null == fromChainId) {
                fromChainId = addrChainId;
            } else if (fromChainId != addrChainId) {
                throw new NulsException(TxErrorCode.COINFROM_NOT_SAME_CHAINID);
            }
            //如果不是跨链交易，from中地址对应的链id必须发起链id，跨链交易在验证器中验证
            if (type != TxType.CROSS_CHAIN) {
                if (chainId != addrChainId) {
                    throw new NulsException(TxErrorCode.FROM_ADDRESS_NOT_MATCH_CHAIN);
                }
            }
            //验证账户地址,资产链id,资产id的组合唯一性
            int assetsChainId = coinFrom.getAssetsChainId();
            int assetsId = coinFrom.getAssetsId();
            // 此资产不可用
            if (assetsChainId == 5 && assetsId == 58) {
                throw new NulsException(TxErrorCode.DATA_ERROR);
            }
            boolean rs = uniqueCoin.add(addr + "-" + assetsChainId + "-" + assetsId + "-" + HexUtil.encode(coinFrom.getNonce()));
            if (!rs) {
                throw new NulsException(TxErrorCode.COINFROM_HAS_DUPLICATE_COIN);
            }
            //用户发出的[非停止节点,红牌]交易不允许from中有合约地址,如果from包含合约地址,那么这个交易一定是系统发出的,系统发出的交易不会走基础验证
            if (type != TxType.STOP_AGENT && type != TxType.RED_PUNISH && TxUtil.isLegalContractAddress(coinFrom.getAddress(), chain)) {
                chain.getLogger().error("Tx from cannot have contract address ");
                throw new NulsException(TxErrorCode.TX_FROM_CANNOT_HAS_CONTRACT_ADDRESS);
            }

            if (!txRegister.getUnlockTx() && coinFrom.getLocked() < 0) {
                chain.getLogger().error("This transaction type can not unlock the token");
                throw new NulsException(TxErrorCode.TX_VERIFY_FAIL);
            }
            if (StringUtils.isNotBlank(lockedAddressStorageService.find(chainId, addr))) {
                chain.getLogger().error("Address:{} is locked. ", addr);
                throw new NulsException(TxErrorCode.ADDRESS_LOCKED);
            }
        }
        if (null != existMultiSignAddress && type != TxType.STOP_AGENT && type != TxType.RED_PUNISH) {
            //如果from中含有多签地址,则表示该交易是多签交易,则必须满足,froms中只存在这一个多签地址
            for (CoinFrom coinFrom : listFrom) {
                if (!Arrays.equals(existMultiSignAddress, coinFrom.getAddress())) {
                    throw new NulsException(TxErrorCode.MULTI_SIGN_TX_ONLY_SAME_ADDRESS);
                }
            }
        }

    }

    private void validateCoinToBase(Chain chain, TxRegister txRegister, List<CoinTo> listTo, Long height) throws NulsException {
        // add by pierre at 2021/8/30 限制最大交易金额
        if (listTo != null && !listTo.isEmpty()) {
            for (CoinTo coinTo : listTo) {
                BigInteger toAmount = coinTo.getAmount();
                if (toAmount.compareTo(TxConstant.MAX_SUPPORT_AMOUNT) > 0) {
                    throw new NulsException(TxErrorCode.DATA_ERROR);
                }
            }
        }
        // end code by pierre
        String moduleCode = txRegister.getModuleCode();
        int type = txRegister.getTxType();
        if (type == TxType.COIN_BASE || ModuleE.SC.abbr.equals(moduleCode)) {
            return;
        }
        if (null == listTo || listTo.size() == 0) {
            if (txRegister.getSystemTx()) {
                //系统交易允许为空,交由验证器进行验证
                return;
            }
            throw new NulsException(TxErrorCode.COINTO_NOT_FOUND);
        }
        //验证收款方是不是属于同一条链
        Integer addressChainId = null;
        int txChainId = chain.getChainId();
        Set<String> uniqueCoin = new HashSet<>();
        for (CoinTo coinTo : listTo) {
            String addr = AddressTool.getStringAddressByBytes(coinTo.getAddress());

            //验证交易地址合法性,跨链模块交易需要取地址中的原始链id来验证
            int validAddressChainId = txChainId;
            if (ModuleE.CC.abbr.equals(txRegister.getModuleCode())) {
                validAddressChainId = AddressTool.getChainIdByAddress(coinTo.getAddress());
            }
            if (!AddressTool.validAddress(validAddressChainId, addr)) {
                throw new NulsException(TxErrorCode.INVALID_ADDRESS);
            }

            int chainId = validAddressChainId;
            if (null == addressChainId) {
                addressChainId = chainId;
            } else if (addressChainId != chainId) {
                throw new NulsException(TxErrorCode.COINTO_NOT_SAME_CHAINID);
            }
            BigInteger toAmount = coinTo.getAmount();
            height = null == height ? chain.getBestBlockHeight() : height;
            if (height >= TxContext.COINTO_PTL_HEIGHT_SECOND) {
                // 禁止锁定金额为0的to
                if (coinTo.getLockTime() < 0L && toAmount.compareTo(BigInteger.ZERO) <= 0) {
                    chain.getLogger().error("交易基础验证失败, 禁止锁定金额为0的to");
                    throw new NulsException(TxErrorCode.DATA_ERROR);
                }
            } else if (height >= TxContext.COINTO_PTL_HEIGHT_FIRST) {
                if (toAmount.compareTo(BigInteger.ZERO) <= 0) {
                    throw new NulsException(TxErrorCode.DATA_ERROR);
                }
            } else {
                if (toAmount.compareTo(BigInteger.ZERO) < 0) {
                    throw new NulsException(TxErrorCode.DATA_ERROR);
                }
            }
            //如果不是跨链交易，to中地址对应的链id必须发起交易的链id
            if (type != TxType.CROSS_CHAIN) {
                if (chainId != txChainId) {
                    throw new NulsException(TxErrorCode.TO_ADDRESS_NOT_MATCH_CHAIN);
                }
            }
            int assetsChainId = coinTo.getAssetsChainId();
            int assetsId = coinTo.getAssetsId();
            // 此资产不可用
            if (assetsChainId == 5 && assetsId == 58) {
                throw new NulsException(TxErrorCode.DATA_ERROR);
            }
            long lockTime = coinTo.getLockTime();
            //to里面地址、资产链id、资产id、锁定时间的组合不能重复
            boolean rs = uniqueCoin.add(addr + "-" + assetsChainId + "-" + assetsId + "-" + lockTime);
            if (!rs) {
                throw new NulsException(TxErrorCode.COINTO_HAS_DUPLICATE_COIN);
            }
            // 地址类型
            int addressType = AddressTool.getTypeByAddress(coinTo.getAddress());
            //合约地址接受NULS的交易只能是coinBase交易,调用合约交易,普通停止节点(合约停止节点交易是系统交易,不走基础验证)
            if (addressType == BaseConstant.CONTRACT_ADDRESS_TYPE) {
                boolean sysTx = txRegister.getSystemTx();
                if (!sysTx && type != TxType.COIN_BASE
                        && type != TxType.CALL_CONTRACT
                        && type != TxType.STOP_AGENT) {
                    chain.getLogger().error("contract data error: The contract does not accept transfers of this type{} of transaction.", type);
                    throw new NulsException(TxErrorCode.TX_DATA_VALIDATION_ERROR);
                }
            }
            // SWAP交易对地址不允许出现在非swap交易中
            if (addressType == BaseConstant.PAIR_ADDRESS_TYPE) {
                if (type != TxType.SWAP_TRADE
                        && type != TxType.SWAP_ADD_LIQUIDITY
                        && type != TxType.SWAP_REMOVE_LIQUIDITY
                        && type != TxType.SWAP_SYSTEM_DEAL
                ) {
                    chain.getLogger().error("swap data error: The swap pair address does not accept transfers of this type{} of transaction.", type);
                    throw new NulsException(TxErrorCode.TX_DATA_PAIR_ADDRESS_VALIDATION_ERROR);
                }
            }
            // STABLE-SWAP交易对地址不允许出现在非stable-swap交易中
            if (addressType == BaseConstant.STABLE_PAIR_ADDRESS_TYPE) {
                if (type != TxType.SWAP_TRADE_STABLE_COIN
                        && type != TxType.SWAP_ADD_LIQUIDITY_STABLE_COIN
                        && type != TxType.SWAP_REMOVE_LIQUIDITY_STABLE_COIN
                        && type != TxType.SWAP_SYSTEM_DEAL
                        && type != TxType.SWAP_TRADE
                        && type != TxType.SWAP_STABLE_LP_SWAP_TRADE
                ) {
                    chain.getLogger().error("stable-swap data error: The stable-swap pair address does not accept transfers of this type{} of transaction.", type);
                    throw new NulsException(TxErrorCode.TX_DATA_PAIR_ADDRESS_VALIDATION_ERROR);
                }
            }
            if (addressType == BaseConstant.FARM_ADDRESS_TYPE) {
                if (type != TxType.FARM_STAKE
                        && type != TxType.FARM_CREATE && type != TxType.FARM_UPDATE) {
                    chain.getLogger().error("swap data error: The swap farm address does not accept transfers of this type{} of transaction.", type);
                    throw new NulsException(TxErrorCode.TX_DATA_FARM_ADDRESS_VALIDATION_ERROR);
                }
            }
        }
    }

    /**
     * 验证交易手续费是否正确
     *
     * @param chain    链id
     * @param tx       tx
     * @param coinData
     * @return Result
     */
    private void validateFee(Chain chain, Transaction tx, CoinData coinData, TxRegister txRegister) throws NulsException {
        if (txRegister.getSystemTx()) {
            //系统交易没有手续费
            return;
        }
        int type = tx.getType();
//        int txSize = tx.getSize();
        int feeAssetChainId;
        int feeAssetId;
        if (type == TxType.CROSS_CHAIN && AddressTool.getChainIdByAddress(coinData.getFrom().get(0).getAddress()) != chain.getChainId()) {
            //为跨链交易并且不是交易发起链时,计算主网主资产为手续费NULS
            feeAssetChainId = txConfig.getMainChainId();
            feeAssetId = txConfig.getMainAssetId();
        } else {
            //计算主资产为手续费
            feeAssetChainId = chain.getConfig().getChainId();
            feeAssetId = chain.getConfig().getAssetId();
        }
        BigInteger fee = coinData.getFeeByAsset(feeAssetChainId, feeAssetId);
        if (BigIntegerUtils.isLessThan(fee, BigInteger.ZERO)) {
            throw new NulsException(TxErrorCode.INSUFFICIENT_FEE);
        }
        //根据交易大小重新计算手续费，用来验证实际手续费
        BigInteger targetFee;
        if (type == TxType.CROSS_CHAIN) {
            // update by lc 20200605
            int validSize = tx.getSize() - tx.getTransactionSignature().length;
            targetFee = TransactionFeeCalculator.getCrossTxFee(validSize);
        } else {
//            targetFee = TransactionFeeCalculator.getNormalTxFee(tx.getSize());
            targetFee = BigInteger.ZERO;
        }
        try {
            if (type == 45) {
                chain.getLogger().info("提案tx format: {}", tx.format());
                chain.getLogger().info("feeAssetChainId: {}, feeAssetId: {}, fee: {}, targetFee: {}, txSize: {}",
                        feeAssetChainId, feeAssetId, fee.toString(), targetFee.toString(), tx.getSize());
            }
        } catch (Exception e) {
            chain.getLogger().warn("日志调用失败[4]: {}", e.getMessage());
        }
        if (BigIntegerUtils.isLessThan(fee, targetFee)) {
            throw new NulsException(TxErrorCode.INSUFFICIENT_FEE);
        }
    }

    /**
     * packing verify ledger
     *
     * @param chain
     * @param batchProcessList
     * @param currentBatchPackableTxs
     * @param orphanTxSet
     * @param proccessContract        是否处理智能合约
     * @param orphanNoCount           (是否因为合约还回去而再次验证账本)孤儿交易还回去的时候 不计算还回去的次数
     * @throws NulsException
     */
    private void verifyLedger(Chain chain, List<String> batchProcessList, List<TxPackageWrapper> currentBatchPackableTxs,
                              Set<TxPackageWrapper> orphanTxSet, boolean proccessContract, boolean orphanNoCount) throws NulsException {
        //开始处理
        Map verifyCoinDataResult = LedgerCall.verifyCoinDataBatchPackaged(chain, batchProcessList);
        List<String> failHashs = (List<String>) verifyCoinDataResult.get("fail");
        List<String> orphanHashs = (List<String>) verifyCoinDataResult.get("orphan");
        if (!failHashs.isEmpty() || !orphanHashs.isEmpty()) {
            chain.getLogger().error("Package verify Ledger fail tx count:{}", failHashs.size());
            chain.getLogger().error("Package verify Ledger orphan tx count:{}", orphanHashs.size());

            Iterator<TxPackageWrapper> it = currentBatchPackableTxs.iterator();
            boolean backContract = false;
            removeAndGo:
            while (it.hasNext()) {
                TxPackageWrapper txPackageWrapper = it.next();
                Transaction transaction = txPackageWrapper.getTx();
                //去除账本验证失败的交易
                for (String hash : failHashs) {
                    String hashStr = transaction.getHash().toHex();
                    if (hash.equals(hashStr)) {
                        if (!backContract && proccessContract && TxManager.isUnSystemSmartContract(chain, transaction.getType())) {
                            //设置标志,如果是智能合约的非系统交易,未验证通过,则需要将所有非系统智能合约交易还回待打包队列.
                            backContract = true;
                        } else {
                            clearInvalidTx(chain, transaction);
                        }
                        it.remove();
                        continue removeAndGo;
                    }
                }
                //去除孤儿交易, 同时把孤儿交易放入孤儿池
                for (String hash : orphanHashs) {
                    String hashStr = transaction.getHash().toHex();
                    if (hash.equals(hashStr)) {
                        if (!backContract && proccessContract && TxManager.isUnSystemSmartContract(chain, transaction.getType())) {
                            //设置标志, 如果是智能合约的非系统交易,未验证通过,则需要将所有非系统智能合约交易还回待打包队列.
                            backContract = true;
                        } else {
                            //孤儿交易
                            if (orphanNoCount) {
                                //如果是因为合约还回去之后,验证账本为孤儿交易则不需要计数 直接还回
                                orphanTxSet.add(txPackageWrapper);
                            } else {
                                addOrphanTxSet(chain, orphanTxSet, txPackageWrapper);
                            }
                        }
                        it.remove();
                        continue removeAndGo;
                    }
                }
            }
            //如果有智能合约的非系统交易未验证通过,则需要将所有非系统智能合约交易还回待打包队列.
            if (backContract && proccessContract) {
                Iterator<TxPackageWrapper> its = currentBatchPackableTxs.iterator();
                while (its.hasNext()) {
                    TxPackageWrapper txPackageWrapper = it.next();
                    Transaction transaction = txPackageWrapper.getTx();
                    if (TxManager.isUnSystemSmartContract(chain, transaction.getType())) {
                        //如果是智能合约的非系统交易,未验证通过,则需要将所有非系统智能合约交易还回待打包队列.
                        packablePool.offerFirstOnlyHash(chain, transaction);
                        chain.setContractTxFail(true);
                        it.remove();
                    }
                }
            }
        }
    }

    /**
     * 处理智能合约交易 执行结果
     *
     * @param chain
     * @param packingTxList
     * @param orphanTxSet
     * @param contractGenerateTxs
     * @param blockHeight
     * @param contractBefore
     * @param stateRoot
     * @return 返回新生成的stateRoot
     * @throws IOException
     */
    private Map processContractResult(Chain chain, List<TxPackageWrapper> packingTxList, Set<TxPackageWrapper> orphanTxSet, List<String> contractGenerateTxs,
                                      long blockHeight, boolean contractBefore, String stateRoot) throws IOException {

        boolean hasTxbackPackablePool = false;
        /**当contractBefore通知失败,或者contractBatchEnd失败则需要将智能合约交易还回待打包队列*/
        boolean isRollbackPackablePool = false;
        if (!contractBefore) {
            isRollbackPackablePool = true;
        } else {
            try {
                Map<String, Object> map = ContractCall.contractPackageBatchEnd(chain, blockHeight);
                List<String> scNewList = (List<String>) map.get("txList");
                if (null != scNewList) {
                    /**
                     * 1.共识验证 如果有
                     * 2.如果只有智能合约的共识交易失败，isRollbackPackablePool=true
                     * 3.如果只有其他共识交易失败，单独删掉
                     * 4.混合 执行2.
                     */
                    List<String> scNewConsensusList = new ArrayList<>();
                    for (String scNewTx : scNewList) {
                        int scNewTxType = TxUtil.extractTxTypeFromTx(scNewTx);
                        if (scNewTxType == TxType.CONTRACT_CREATE_AGENT
                                || scNewTxType == TxType.CONTRACT_DEPOSIT
                                || scNewTxType == TxType.CONTRACT_CANCEL_DEPOSIT
                                || scNewTxType == TxType.CONTRACT_STOP_AGENT) {
                            scNewConsensusList.add(scNewTx);
                        }
                    }
                    if (!scNewConsensusList.isEmpty()) {
                        //收集共识模块所有交易, 加上新产生的智能合约共识交易，一起再次进行模块统一验证
                        TxRegister consensusTxRegister = null;
                        List<String> consensusList = new ArrayList<>();
                        for (TxPackageWrapper txPackageWrapper : packingTxList) {
                            Transaction tx = txPackageWrapper.getTx();
                            TxRegister txRegister = TxManager.getTxRegister(chain, tx.getType());
                            if (txRegister.getModuleCode().equals(ModuleE.CS.abbr)) {
                                consensusList.add(RPCUtil.encode(txPackageWrapper.getTx().serialize()));
                                if (null == consensusTxRegister) {
                                    consensusTxRegister = txRegister;
                                }
                            }
                        }
                        if (consensusTxRegister == null) {
                            consensusTxRegister = TxManager.getTxRegister(chain, TxType.REGISTER_AGENT);
                        }
                        consensusList.addAll(scNewConsensusList);
                        isRollbackPackablePool = processContractConsensusTx(chain, consensusTxRegister, consensusList, packingTxList, false);
                    }
                    if (!isRollbackPackablePool) {
                        contractGenerateTxs.addAll(scNewList);
                    }
                }
                String sr = (String) map.get("stateRoot");
                if (null != sr) {
                    stateRoot = sr;
                }
                if (!isRollbackPackablePool) {
                    //如果合约交易不需要全部放回待打包队列,就检查如果存在未执行的智能合约,则放回待打包队列,下次执行。
                    List<String> nonexecutionList = (List<String>) map.get("pendingTxHashList");
                    if (null != nonexecutionList && !nonexecutionList.isEmpty()) {
                        chain.getLogger().debug("contract pending tx count:{} ", nonexecutionList.size());
                        Iterator<TxPackageWrapper> iterator = packingTxList.iterator();
                        while (iterator.hasNext()) {
                            TxPackageWrapper txPackageWrapper = iterator.next();
                            for (String hash : nonexecutionList) {
                                if (hash.equals(txPackageWrapper.getTx().getHash().toHex())) {
                                    orphanTxSet.add(txPackageWrapper);
                                    //从可打包集合中删除
                                    iterator.remove();
                                    if (!hasTxbackPackablePool) {
                                        hasTxbackPackablePool = true;
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (NulsException e) {
                chain.getLogger().error(e);
                isRollbackPackablePool = true;
            }
        }
        if (isRollbackPackablePool) {
            Iterator<TxPackageWrapper> iterator = packingTxList.iterator();
            while (iterator.hasNext()) {
                TxPackageWrapper txPackageWrapper = iterator.next();
                if (TxManager.isUnSystemSmartContract(chain, txPackageWrapper.getTx().getType())) {
                    /**
                     * 智能合约出现需要加回待打包队列的情况,没有加回次数限制,
                     * 不需要比对TX_PACKAGE_ORPHAN_MAP的阈值,直接加入集合,可以与孤儿交易合用一个集合
                     */
                    orphanTxSet.add(txPackageWrapper);
                    //从可打包集合中删除
                    iterator.remove();
                    if (!hasTxbackPackablePool) {
                        hasTxbackPackablePool = true;
                    }
                }
            }
        }
        Map rs = new HashMap();
        rs.put("stateRoot", stateRoot);
        rs.put("hasTxbackPackablePool", hasTxbackPackablePool);
        return rs;
    }

    /**
     * 处理智能合约的共识交易
     *
     * @param chain
     * @param consensusTxRegister
     * @param consensusList
     * @param packingTxList
     * @param batchVerify
     * @return
     * @throws NulsException
     */
    private boolean processContractConsensusTx(Chain chain, TxRegister consensusTxRegister, List<String> consensusList, List<TxPackageWrapper> packingTxList, boolean batchVerify) throws NulsException {
        while (true) {
            List<String> txHashList = null;
            try {
                txHashList = TransactionCall.txModuleValidator(chain, consensusTxRegister.getModuleCode(), consensusList);
            } catch (NulsException e) {
                chain.getLogger().error("Package module verify failed -txModuleValidator Exception:{}, module-code:{}, count:{} , return count:{}",
                        BaseConstant.TX_VALIDATOR, consensusTxRegister.getModuleCode(), consensusList.size(), txHashList.size());
                txHashList = new ArrayList<>(consensusList.size());
                for (String txStr : consensusList) {
                    Transaction tx = TxUtil.getInstanceRpcStr(txStr, Transaction.class);
                    txHashList.add(tx.getHash().toHex());
                }
            }
            if (txHashList.isEmpty()) {
                //都执行通过
                return false;
            }
            if (batchVerify) {
                //如果是验证区块交易，有不通过的 直接返回
                return true;
            }
            Iterator<String> it = consensusList.iterator();
            while (it.hasNext()) {
                Transaction tx = TxUtil.getInstanceRpcStr(it.next(), Transaction.class);
                int type = tx.getType();
                for (String hash : txHashList) {
                    if (hash.equals(tx.getHash().toHex()) && (type == TxType.CONTRACT_CREATE_AGENT
                            || type == TxType.CONTRACT_DEPOSIT
                            || type == TxType.CONTRACT_CANCEL_DEPOSIT
                            || type == TxType.CONTRACT_STOP_AGENT)) {
                        //有智能合约交易不通过 则把所有智能合约交易返回待打包队列
                        return true;
                    }
                }
            }
            /**
             * 没有智能合约失败,只有普通共识交易失败的情况
             * 1.从待打包队列删除
             * 2.从模块统一验证集合中删除，再次验证，直到全部验证通过
             */
            for (int i = 0; i < txHashList.size(); i++) {
                String hash = txHashList.get(i);
                Iterator<TxPackageWrapper> its = packingTxList.iterator();
                while (its.hasNext()) {
                    /**冲突检测有不通过的, 执行清除和未确认回滚 从packingTxList删除*/
                    Transaction tx = its.next().getTx();
                    if (hash.equals(tx.getHash().toHex())) {
                        clearInvalidTx(chain, tx);
                        its.remove();
                    }
                }
                Iterator<String> itcs = consensusList.iterator();
                while (its.hasNext()) {
                    Transaction tx = TxUtil.getInstanceRpcStr(itcs.next(), Transaction.class);
                    if (hash.equals(tx.getHash().toHex())) {
                        itcs.remove();
                    }

                }
            }
        }
    }

    /**
     * 将孤儿交易加回待打包队列时, 要判断加了几次(因为下次打包时又验证为孤儿交易会再次被加回), 达到阈值就不再加回了
     */
    @Override
    public void addOrphanTxSet(Chain chain, Set<TxPackageWrapper> orphanTxSet, TxPackageWrapper txPackageWrapper) {
        NulsHash hash = txPackageWrapper.getTx().getHash();
        Integer count = chain.getTxPackageOrphanMap().get(hash);
//        if (count == null || count < TxConstant.PACKAGE_ORPHAN_MAXCOUNT) {
        if (count != null && count < TxConstant.PACKAGE_ORPHAN_MAXCOUNT) {
            orphanTxSet.add(txPackageWrapper);
            if (count == null) {
                count = 1;
            } else {
                count++;
            }
            if (chain.getTxPackageOrphanMap().size() > TxConstant.PACKAGE_ORPHAN_MAP_MAXCOUNT) {
                chain.getTxPackageOrphanMap().clear();
            }
            chain.getTxPackageOrphanMap().put(hash, count);
        } else {
            //不加回(丢弃), 同时删除map中的key,并清理
            chain.getLogger().debug("清理孤儿交易 hash:{}", hash.toHex());
            clearInvalidTx(chain, txPackageWrapper.getTx());
            chain.getTxPackageOrphanMap().remove(hash);
        }
    }

    /**
     * 将交易加回到待打包队列
     * 将孤儿交易(如果有),加入到验证通过的交易集合中,按取出的顺序排倒序,再依次加入待打包队列的最前端
     *
     * @param chain
     * @param txList      验证通过的交易
     * @param orphanTxSet 孤儿交易
     */
    @Override
    public void putBackPackablePool(Chain chain, List<TxPackageWrapper> txList, Set<TxPackageWrapper> orphanTxSet) {
        if (null == txList) {
            txList = new ArrayList<>();
        }
        if (null != orphanTxSet && !orphanTxSet.isEmpty()) {
            txList.addAll(orphanTxSet);
        }
        if (txList.isEmpty()) {
            return;
        }
        //孤儿交易排倒序, 全加回待打包队列去
        txList.sort(new Comparator<TxPackageWrapper>() {
            @Override
            public int compare(TxPackageWrapper o1, TxPackageWrapper o2) {
                return o1.compareTo(o2.getIndex());
            }
        });
        for (TxPackageWrapper txPackageWrapper : txList) {
            packablePool.offerFirstOnlyHash(chain, txPackageWrapper.getTx());
        }
        chain.getLogger().info("putBackPackablePool count:{}", txList.size());
    }

    @Override
    public void putBackPackablePool(Chain chain, Set<TxPackageWrapper> orphanTxSet) {
        putBackPackablePool(chain, null, orphanTxSet);
    }

    /**
     * 1.统一验证
     * 2a:如果没有不通过的验证的交易则结束!!
     * 2b.有不通过的验证时，moduleVerifyMap过滤掉不通过的交易.
     * 3.重新验证同一个模块中不通过交易后面的交易(包括单个verify和coinData)，再执行1.递归？
     *
     * @param moduleVerifyMap
     */
    @Override
    public List<String> txModuleValidatorPackable(Chain chain,
                                                  Map<String, List<String>> moduleVerifyMap,
                                                  List<TxPackageWrapper> packingTxList,
                                                  Set<TxPackageWrapper> orphanTxSet,
                                                  long height,
                                                  long blockTime) throws NulsException {
        Iterator<Map.Entry<String, List<String>>> it = moduleVerifyMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<String>> entry = it.next();
            List<String> moduleList = entry.getValue();
            if (moduleList.size() == 0) {
                //当递归中途模块交易被过滤完后会造成list为空,这时不需要再调用模块统一验证器
                it.remove();
                continue;
            }
            String moduleCode = entry.getKey();
            List<String> txHashList = null;
            try {
                txHashList = TransactionCall.txModuleValidator(chain, moduleCode, moduleList);
            } catch (NulsException e) {
                chain.getLogger().error("Package module verify failed -txModuleValidator Exception:{}, module-code:{}, count:{} , return count:{}",
                        BaseConstant.TX_VALIDATOR, moduleCode, moduleList.size(), txHashList.size());
                //出错则删掉整个模块的交易
                Iterator<TxPackageWrapper> its = packingTxList.iterator();
                while (its.hasNext()) {
                    Transaction tx = its.next().getTx();
                    TxRegister txRegister = TxManager.getTxRegister(chain, tx.getType());
                    if (txRegister.getModuleCode().equals(moduleCode)) {
                        clearInvalidTx(chain, tx);
                        its.remove();
                    }
                }
                continue;
            }
            if (null == txHashList || txHashList.isEmpty()) {
                //模块统一验证没有冲突的，从map中干掉
                it.remove();
                continue;
            }
            chain.getLogger().debug("[Package module verify failed] module:{}, module-code:{}, count:{} , return count:{}",
                    BaseConstant.TX_VALIDATOR, moduleCode, moduleList.size(), txHashList.size());
            /**冲突检测有不通过的, 执行清除和未确认回滚 从packingTxList删除*/
            for (int i = 0; i < txHashList.size(); i++) {
                String hash = txHashList.get(i);
                Iterator<TxPackageWrapper> its = packingTxList.iterator();
                while (its.hasNext()) {
                    Transaction tx = its.next().getTx();
                    if (hash.equals(tx.getHash().toHex())) {
                        clearInvalidTx(chain, tx);
                        its.remove();
                    }
                }
            }
        }

        if (moduleVerifyMap.isEmpty()) {
            /**
             * 处理需要打包时内部生成和删除的交易
             */
            Map<String, List> packProduceCallMap = packProduceProcess(chain, packingTxList);
            Map<String, List<String>> map = getNewProduceTx(chain, packProduceCallMap, packingTxList, height, blockTime);
            List<String> allRemoveList = map.get("allRemoveList");
            if (null != allRemoveList && !allRemoveList.isEmpty()) {
                // 移除对应的原始交易,再次验证
                for (int i = allRemoveList.size() - 1; i >= 0; i--) {
                    String hash = allRemoveList.get(i);
                    Iterator<TxPackageWrapper> its = packingTxList.iterator();
                    while (its.hasNext()) {
                        TxPackageWrapper txPackageWrapper = its.next();
                        Transaction tx = txPackageWrapper.getTx();
                        if (hash.equals(tx.getHash().toHex())) {
                            // 不需要比对TX_PACKAGE_ORPHAN_MAP的阈值,直接加入集合,可以与孤儿交易合用一个集合
                            orphanTxSet.add(txPackageWrapper);
                            its.remove();
                            chain.getLogger().info("打包时将原始交易还回待打包队列 hash:{}", hash);
                        }
                    }
                }
            } else {
                List<String> allNewlyList = map.get("allNewlyList");
                return allNewlyList;
            }
        }
        moduleVerifyMap = new HashMap<>(TxConstant.INIT_CAPACITY_16);
        verifyAgain(chain, moduleVerifyMap, packingTxList, orphanTxSet, false);
        return txModuleValidatorPackable(chain, moduleVerifyMap, packingTxList, orphanTxSet, height, blockTime);
    }

    /**
     * @param chain
     * @param moduleVerifyMap
     * @param packingTxList
     * @param orphanTxSet
     * @param orphanNoCount   (是否因为合约还回去而再次验证账本)孤儿交易还回去的时候 不计算还回去的次数
     * @throws NulsException
     */
    private void verifyAgain(Chain chain, Map<String, List<String>> moduleVerifyMap, List<TxPackageWrapper> packingTxList, Set<TxPackageWrapper> orphanTxSet, boolean orphanNoCount) throws NulsException {
        chain.getLogger().debug("------ verifyAgain 打包再次批量校验通知 ------");
        //向账本模块发送要批量验证coinData的标识
        LedgerCall.coinDataBatchNotify(chain);
        List<String> batchProcessList = new ArrayList<>();
        for (TxPackageWrapper txPackageWrapper : packingTxList) {
            if (TxManager.isSystemSmartContract(chain, txPackageWrapper.getTx().getType())) {
                //智能合约系统交易不需要验证账本
                continue;
            }
            batchProcessList.add(txPackageWrapper.getTxHex());
        }
        verifyLedger(chain, batchProcessList, packingTxList, orphanTxSet, true, orphanNoCount);

        for (TxPackageWrapper txPackageWrapper : packingTxList) {
            Transaction tx = txPackageWrapper.getTx();
            TxUtil.moduleGroups(chain, moduleVerifyMap, tx);
        }
    }

    /**
     * 打包交易时对应模块内部处理
     * 判断交易需要进行打包时的模块处理,发送原始交易,获取新生成交易
     * 如果出现异常,需要从打包列表中移除,原始交易
     *
     * @return
     * @throws NulsException
     */
    private Map<String, List> packProduceProcess(Chain chain, List<TxPackageWrapper> packingTxList) {
        Iterator<TxPackageWrapper> iterator = packingTxList.iterator();
        // K: 模块code V:{k: 原始交易hash, v:原始交易字符串}
        Map<String, List> packProduceCallMap = TxManager.getGroup(chain);
        while (iterator.hasNext()) {
            TxPackageWrapper txPackageWrapper = iterator.next();
            Transaction tx = txPackageWrapper.getTx();
            TxRegister txRegister = TxManager.getTxRegister(chain, tx.getType());
            if (txRegister.getPackProduce()) {
                List<TxPackageWrapper> listCallTxs = packProduceCallMap.computeIfAbsent(txRegister.getModuleCode(), k -> new ArrayList<>());
                listCallTxs.add(txPackageWrapper);
                LoggerUtil.LOG.debug("打包时模块内部处理生成, 原始交易hash:{},  moduleCode:{}", tx.getHash().toHex(), txRegister.getModuleCode());
            }
        }
        return packProduceCallMap;
    }

    private Map<String, List<String>> getNewProduceTx(Chain chain, Map<String, List> packProduceCallMap, List<TxPackageWrapper> packingTxList, long height, long blockTime) {
        List<String> allNewlyList = new ArrayList<>();
        List<String> allRemoveList = new ArrayList<>();
        Iterator<Map.Entry<String, List>> it = packProduceCallMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List> entry = it.next();
            List<TxPackageWrapper> moduleList = entry.getValue();
            List<String> txHexList = new ArrayList<>();
            for (TxPackageWrapper wrapper : moduleList) {
                txHexList.add(wrapper.getTxHex());
            }
            String moduleCode = entry.getKey();
            try {
                // 返回完整交易字符串
                Map<String, List<String>> map = TransactionCall.packProduce(chain, BaseConstant.TX_PACKPRODUCE, moduleCode, txHexList, height, blockTime, PACKING);
                List<String> newlyList = (List<String>) map.get("newlyList");
                List<String> rmHashList = (List<String>) map.get("rmHashList");
                if (null != newlyList && !newlyList.isEmpty()) {
                    allNewlyList.addAll(newlyList);
                    LoggerUtil.LOG.debug("打包时模块{}生成的交易数量:{}, moduleCode:{}", moduleCode, newlyList.size(), moduleCode);
                }
                if (null != rmHashList && !rmHashList.isEmpty()) {
                    allRemoveList.addAll(rmHashList);
                    LoggerUtil.LOG.debug("打包时模块{}需要移除的原始交易数量:{}, moduleCode:{}", moduleCode, rmHashList.size(), moduleCode);
                }
            } catch (NulsException e) {
                chain.getLogger().error("打包时模块:{} 处理生成交易异常", moduleCode);
                chain.getLogger().error(e);
                Iterator<TxPackageWrapper> iterator = packingTxList.iterator();
                while (iterator.hasNext()) {
                    TxPackageWrapper txPackageWrapper = iterator.next();
                    for (TxPackageWrapper wrapper : moduleList) {
                        if (txPackageWrapper.equals(wrapper)) {
                            // 出现异常清理掉该模块所有原始交易
                            clearInvalidTx(chain, txPackageWrapper.getTx());
                            iterator.remove();
                        }
                    }
                }
            }
        }
        Map<String, List<String>> map = new HashMap<>();
        map.put("allNewlyList", allNewlyList);
        map.put("allRemoveList", allRemoveList);
        return map;
    }

    /**
     * 基本的清理
     * 1.未确认库
     * 2.
     */
    @Override
    public void baseClearTx(Chain chain, Transaction tx) {
        unconfirmedTxStorageService.removeTx(chain.getChainId(), tx.getHash());
        packablePool.clearPackableMapTx(chain, tx);
    }


    @Override
    public void clearInvalidTx(Chain chain, Transaction tx) {
        clearInvalidTx(chain, tx, true);
    }

    @Override
    public void clearInvalidTx(Chain chain, Transaction tx, boolean changeStatus) {
        baseClearTx(chain, tx);
        //判断如果交易已被确认就不用调用账本清理了!!
        TransactionConfirmedPO txConfirmed = confirmedTxService.getConfirmedTransaction(chain, tx.getHash());
        if (txConfirmed == null) {
            try {
                //如果是清理机制调用, 则调用账本未确认回滚
                LedgerCall.rollBackUnconfirmTx(chain, RPCUtil.encode(tx.serialize()));
                if (changeStatus) {
                    //通知账本状态变更
                    LedgerCall.rollbackTxValidateStatus(chain, RPCUtil.encode(tx.serialize()));
                }
            } catch (NulsException e) {
                chain.getLogger().error(e);
            } catch (Exception e) {
                chain.getLogger().error(e);
            }
        }
    }

}

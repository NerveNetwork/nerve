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

import io.nuls.common.NerveCoreConfig;
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
import io.nuls.core.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.core.thread.ThreadUtils;
import io.nuls.core.thread.commom.NulsThreadFactory;
import io.nuls.transaction.cache.PackablePool;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxContext;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.manager.TxManager;
import io.nuls.transaction.model.bo.*;
import io.nuls.transaction.model.dto.AccountBlockDTO;
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
    private NerveCoreConfig txConfig;

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
                // Notify block of new transaction registration(Clearing system transaction cache)
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
                //Perform basic transaction verification
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
                //Node block synchronization or rollback,Suspend acceptance of new transactions
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
                //IfmapIf it is full, it may not necessarily be able to join the queue for packaging
                packablePool.add(chain, tx);
            }
            unconfirmedTxStorageService.putTx(chain.getChainId(), tx);
            //System transactions Not broadcasting
            TxRegister txRegister = TxManager.getTxRegister(chain, tx.getType());
            if (txRegister.getSystemTx()) {
                return;
            }
            //Broadcast complete transactions
            boolean broadcastResult = false;
            for (int i = 0; i < 3; i++) {
                if (ModuleE.CC.abbr.equals(ResponseMessageProcessor.TX_TYPE_MODULE_MAP.get(tx.getType()))) {
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
            //Add to the set of deduplication filters,Prevent other nodes from forwarding back and processing the transaction again
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
     * Legal circulation of transactions and basic verification
     *
     * @param chain
     * @param tx
     * @param localTx Is the transaction initiated locally(Allow local modules to initiate system level transactions)
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

        //Verify signature
        try {

            if (chain.getBestBlockHeight() >= TxContext.PROTOCOL_1_20_0) {
                validateTxSignatureProtocol20(tx, txRegister, chain);
            } else if (chain.getBestBlockHeight() >= TxContext.PROTOCOL_1_19_0) {
                validateTxSignatureProtocol19(tx, txRegister, chain);
            } else {
                validateTxSignature(tx, txRegister, chain);
            }
        } catch (NulsException e) {
            throw e;
        } catch (Exception e) {
            throw new NulsException(TxErrorCode.SIGNATURE_ERROR);
        }

        //If there is anycoinData, Then proceed with verification,There are some transactions(Yellow card)absencecoinDatadata
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
        validateCoinToBase(chain, txRegister, coinData.getTo(), height, tx.getType());

        if (txRegister.getVerifyFee()) {
            validateFee(chain, tx, coinData, txRegister);
        }
    }

    /**
     * Verify signature Just need to verify,Transactions that require signature verification(Some system transactions do not require signatures)
     * Verify the public key andfromDoes it match in the middle, Verify signature correctness
     *
     * @param tx
     * @throws NulsException
     */
    private void validateTxSignature(Transaction tx, TxRegister txRegister, Chain chain) throws NulsException {
        //Just need to verify,Transactions that require signature verification(Some system transactions do not require signatures)
        if (!txRegister.getVerifySignature() || ModuleE.CC.abbr.equals(ResponseMessageProcessor.TX_TYPE_MODULE_MAP.get(tx.getType()))) {
            //Transactions that do not require signature verification during registration(Some system transactions),And cross chain module transactions(individualization).
            return;
        }
        CoinData coinData = TxUtil.getCoinData(tx);
        if (null == coinData || null == coinData.getFrom() || coinData.getFrom().size() <= 0) {
            throw new NulsException(TxErrorCode.COINDATA_NOT_FOUND);
        }
        //Obtain a list of transaction signer addresses
        Set<String> addressSet = SignatureUtil.getAddressFromTX(tx, chain.getChainId());
        if (addressSet == null) {
            throw new NulsException(TxErrorCode.SIGNATURE_ERROR);
        }
        int chainId = chain.getChainId();
        byte[] multiSignAddress = null;
        if (tx.isMultiSignTx()) {
            /**
             * If it is a multi signature transaction, Then, first extract the public key list and minimum number of signatures of the original creator with multiple signed addresses from the signing object,
             * Generate a new multi signature address,To engage in transactionsfromMultiple address matches in, unable to match. This verification does not pass.
             */
            MultiSignTxSignature multiSignTxSignature = new MultiSignTxSignature();
            multiSignTxSignature.parse(new NulsByteBuffer(tx.getTransactionSignature()));
            //Verify if the signer is sufficient for the minimum number of signatures
            if (addressSet.size() < multiSignTxSignature.getM()) {
                throw new NulsException(TxErrorCode.INSUFFICIENT_SIGNATURES);
            }
            //Is the signer one of the creators of the multi signature account
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
            //Generate a multi signature address
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
            byte[] fromAddress = coinFrom.getAddress();
            if (tx.isMultiSignTx()) {
                if (!Arrays.equals(fromAddress, multiSignAddress)) {
                    throw new NulsException(TxErrorCode.SIGNATURE_ERROR);
                }
            } else if (!addressSet.contains(AddressTool.getStringAddressByBytes(fromAddress))) {
                throw new NulsException(TxErrorCode.SIGN_ADDRESS_NOT_MATCH_COINFROM);
            }
            if (tx.getType() == TxType.STOP_AGENT) {
                //Stop nodefromThe first one in the middle is the signature address, Only verifyfromFirst in the middle
                break;
            }
        }
        if (!SignatureUtil.validateTransactionSignture(chainId, tx)) {
            throw new NulsException(TxErrorCode.SIGNATURE_ERROR);
        }
    }

    private void validateTxSignatureProtocol19(Transaction tx, TxRegister txRegister, Chain chain) throws NulsException {
        //Just need to verify,Transactions that require signature verification(Some system transactions do not require signatures)
        if (!txRegister.getVerifySignature()) {
            //Transactions that do not require signature verification during registration(Some system transactions)
            return;
        }
        CoinData coinData = TxUtil.getCoinData(tx);
        if (null == coinData || null == coinData.getFrom() || coinData.getFrom().size() <= 0) {
            throw new NulsException(TxErrorCode.COINDATA_NOT_FOUND);
        }
        if (ModuleE.CC.abbr.equals(ResponseMessageProcessor.TX_TYPE_MODULE_MAP.get(tx.getType()))) {
            if (tx.getType() != TxType.CROSS_CHAIN) {
                // Cross chain transfer transactions for non local protocols of cross chain modules(individualization).
                return;
            }
            int fromChainId = AddressTool.getChainIdByAddress(coinData.getFrom().get(0).getAddress());
            // Cross chain module non native protocol cross chain transactions(individualization).
            if (chain.getChainId() != fromChainId) {
                return;
            }
        }
        //Obtain a list of transaction signer addresses
        Set<String> addressSet = SignatureUtil.getAddressFromTX(tx, chain.getChainId());
        if (addressSet == null) {
            throw new NulsException(TxErrorCode.SIGNATURE_ERROR);
        }
        int chainId = chain.getChainId();
        byte[] multiSignAddress = null;
        if (tx.isMultiSignTx()) {
            /**
             * If it is a multi signature transaction, Then, first extract the public key list and minimum number of signatures of the original creator with multiple signed addresses from the signing object,
             * Generate a new multi signature address,To engage in transactionsfromMultiple address matches in, unable to match. This verification does not pass.
             */
            MultiSignTxSignature multiSignTxSignature = new MultiSignTxSignature();
            multiSignTxSignature.parse(new NulsByteBuffer(tx.getTransactionSignature()));
            //Verify if the signer is sufficient for the minimum number of signatures
            if (addressSet.size() < multiSignTxSignature.getM()) {
                throw new NulsException(TxErrorCode.INSUFFICIENT_SIGNATURES);
            }
            //Is the signer one of the creators of the multi signature account
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
            //Generate a multi signature address
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
            byte[] fromAddress = coinFrom.getAddress();
            if (tx.isMultiSignTx()) {
                if (!Arrays.equals(fromAddress, multiSignAddress)) {
                    throw new NulsException(TxErrorCode.SIGNATURE_ERROR);
                }
            } else if (!addressSet.contains(AddressTool.getStringAddressByBytes(fromAddress))) {
                throw new NulsException(TxErrorCode.SIGN_ADDRESS_NOT_MATCH_COINFROM);
            }
            if (tx.getType() == TxType.STOP_AGENT) {
                //Stop nodefromThe first one in the middle is the signature address, Only verifyfromFirst in the middle
                break;
            }
        }
        do {
            int txType = tx.getType();
            // Pledge and Withdrawal of Pledge、DEXTransaction not verified locked address
            if (txType == TxType.DEPOSIT || txType == TxType.CANCEL_DEPOSIT || txType == TxType.STOP_AGENT) {
                break;
            }
            boolean needAccountManagerSign = false;
            for (CoinFrom coinFrom : coinData.getFrom()) {
                byte[] fromAddress = coinFrom.getAddress();
                AccountBlockDTO dto = AccountCall.getBlockAccount(chainId, AddressTool.getStringAddressByBytes(fromAddress));
                if (dto == null) {
                    continue;
                }
                int[] types = dto.getTypes();
                if (types == null) {
                    // Completely locked account, signature verification required
                    needAccountManagerSign = true;
                    break;
                } else {
                    // Transaction type whitelist
                    boolean whiteType = false;
                    for (int type : types) {
                        if (txType == type) {
                            whiteType = true;
                            break;
                        }
                    }
                    if (!whiteType) {
                        // Not on the whitelist of transaction types, signature verification is required
                        needAccountManagerSign = true;
                        break;
                    }
                }
            }
            if (needAccountManagerSign) {
                // Three fifths of the signature, read the locked account administrator public key from the configuration file, calculate the address, and`addressSet`Middle matching,>=60% Satisfy immediately
                int count = 0;
                for (String signedAddress : addressSet) {
                    if (TxContext.ACCOUNT_BLOCK_MANAGER_ADDRESS_SET.contains(signedAddress)) {
                        count++;
                    }
                }
                if (count < TxContext.ACCOUNT_BLOCK_MIN_SIGN_COUNT) {
                    throw new NulsException(TxErrorCode.ADDRESS_LOCKED, "address is blockAddress Exception");
                }
            }
        } while (false);
        if (!SignatureUtil.validateTransactionSignture(chainId, tx)) {
            throw new NulsException(TxErrorCode.SIGNATURE_ERROR);
        }
    }

    private void validateTxSignatureProtocol20(Transaction tx, TxRegister txRegister, Chain chain) throws NulsException {
        //Just need to verify,Transactions that require signature verification(Some system transactions do not require signatures)
        if (!txRegister.getVerifySignature()) {
            //Transactions that do not require signature verification during registration(Some system transactions)
            return;
        }
        CoinData coinData = TxUtil.getCoinData(tx);
        if (null == coinData || null == coinData.getFrom() || coinData.getFrom().size() <= 0) {
            throw new NulsException(TxErrorCode.COINDATA_NOT_FOUND);
        }
        if (ModuleE.CC.abbr.equals(ResponseMessageProcessor.TX_TYPE_MODULE_MAP.get(tx.getType()))) {
            if (tx.getType() != TxType.CROSS_CHAIN) {
                // Cross chain transfer transactions for non local protocols of cross chain modules(individualization).
                return;
            }
            int fromChainId = AddressTool.getChainIdByAddress(coinData.getFrom().get(0).getAddress());
            // Cross chain module non native protocol cross chain transactions(individualization).
            if (chain.getChainId() != fromChainId) {
                return;
            }
        }
        //Obtain a list of transaction signer addresses
        Set<String> addressSet = SignatureUtil.getAddressFromTX(tx, chain.getChainId());
        if (addressSet == null) {
            throw new NulsException(TxErrorCode.SIGNATURE_ERROR);
        }
        int chainId = chain.getChainId();
        byte[] multiSignAddress = null;
        if (tx.isMultiSignTx()) {
            /**
             * If it is a multi signature transaction, Then, first extract the public key list and minimum number of signatures of the original creator with multiple signed addresses from the signing object,
             * Generate a new multi signature address,To engage in transactionsfromMultiple address matches in, unable to match. This verification does not pass.
             */
            MultiSignTxSignature multiSignTxSignature = new MultiSignTxSignature();
            multiSignTxSignature.parse(new NulsByteBuffer(tx.getTransactionSignature()));
            //Verify if the signer is sufficient for the minimum number of signatures
            if (addressSet.size() < multiSignTxSignature.getM()) {
                throw new NulsException(TxErrorCode.INSUFFICIENT_SIGNATURES);
            }
            //Is the signer one of the creators of the multi signature account
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
            //Generate a multi signature address
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
            byte[] fromAddress = coinFrom.getAddress();
            if (tx.isMultiSignTx()) {
                if (!Arrays.equals(fromAddress, multiSignAddress)) {
                    throw new NulsException(TxErrorCode.SIGNATURE_ERROR);
                }
            } else if (!addressSet.contains(AddressTool.getStringAddressByBytes(fromAddress))) {
                throw new NulsException(TxErrorCode.SIGN_ADDRESS_NOT_MATCH_COINFROM);
            }
            if (tx.getType() == TxType.STOP_AGENT) {
                //Stop nodefromThe first one in the middle is the signature address, Only verifyfromFirst in the middle
                break;
            }
        }
        do {
            int txType = tx.getType();
            // Pledge and Withdrawal of Pledge、DEXTransaction not verified locked address
            if (txType == TxType.TRADING_ORDER || txType == TxType.TRADING_ORDER_CANCEL || txType == TxType.TRADING_DEAL || txType == TxType.ORDER_CANCEL_CONFIRM || txType == TxType.COIN_TRADING) {
                break;
            }
            if (txType == TxType.DEPOSIT || txType == TxType.CANCEL_DEPOSIT || txType == TxType.STOP_AGENT) {
                break;
            }
            boolean needAccountManagerSign = false;
            for (CoinFrom coinFrom : coinData.getFrom()) {
                byte[] fromAddress = coinFrom.getAddress();
                AccountBlockDTO dto = AccountCall.getBlockAccount(chainId, AddressTool.getStringAddressByBytes(fromAddress));
                if (dto == null) {
                    continue;
                }
                int[] types = dto.getTypes();
                if (types == null) {
                    // Completely locked account, signature verification required
                    needAccountManagerSign = true;
                    break;
                } else {
                    // Transaction type whitelist
                    boolean whiteType = false;
                    for (int type : types) {
                        if (txType == type) {
                            whiteType = true;
                            break;
                        }
                    }
                    if (!whiteType) {
                        // Not on the whitelist of transaction types, signature verification is required
                        needAccountManagerSign = true;
                        break;
                    }
                }
            }
            if (needAccountManagerSign) {
                // Three fifths of the signature, read the locked account administrator public key from the configuration file, calculate the address, and`addressSet`Middle matching,>=60% Satisfy immediately
                int count = 0;
                for (String signedAddress : addressSet) {
                    if (TxContext.ACCOUNT_BLOCK_MANAGER_ADDRESS_SET.contains(signedAddress)) {
                        count++;
                    }
                }
                if (count < TxContext.ACCOUNT_BLOCK_MIN_SIGN_COUNT) {
                    throw new NulsException(TxErrorCode.ADDRESS_LOCKED, "address is blockAddress Exception");
                }
            }
        } while (false);
        if (!SignatureUtil.validateTransactionSignture(chainId, tx)) {
            throw new NulsException(TxErrorCode.SIGNATURE_ERROR);
        }
    }

    private void validateCoinFromBase(Chain chain, TxRegister txRegister, List<CoinFrom> listFrom) throws NulsException {
        // add by pierre at 2021/8/30 Limit maximum transaction amount
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
        //coinBasetransaction/Smart contract refundgasTransaction not availablefrom
        if (type == TxType.COIN_BASE || type == TxType.CONTRACT_RETURN_GAS) {
            return;
        }
        if (null == listFrom || listFrom.size() == 0) {
            if (txRegister.getSystemTx()) {
                //System transactions are allowed to be empty,To be verified by a validator
                return;
            }
            throw new NulsException(TxErrorCode.COINFROM_NOT_FOUND);
        }
        int chainId = chain.getConfig().getChainId();
        //Verify if the payer belongs to the same chain
        Integer fromChainId = null;
        Set<String> uniqueCoin = new HashSet<>();
        byte[] existMultiSignAddress = null;
        for (CoinFrom coinFrom : listFrom) {
            byte[] addrBytes = coinFrom.getAddress();
            if (chain.getBestBlockHeight() < TxContext.PROTOCOL_1_19_0 && type != TxType.DEPOSIT && type != TxType.CANCEL_DEPOSIT && TxUtil.isBlockAddress(chain, addrBytes)) {
                throw new NulsException(TxErrorCode.ADDRESS_LOCKED, "address is blockAddress Exception");
            }
            String addr = AddressTool.getStringAddressByBytes(addrBytes);
            //Verify the legality of the transaction address,Cross chain module transactions require retrieving the original chain from the addressidTo verify
            int validAddressChainId = chainId;
            if (ModuleE.CC.abbr.equals(ResponseMessageProcessor.TX_TYPE_MODULE_MAP.get(type))) {
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
            //AllfromIs it the same address on the same chain
            if (null == fromChainId) {
                fromChainId = addrChainId;
            } else if (fromChainId != addrChainId) {
                throw new NulsException(TxErrorCode.COINFROM_NOT_SAME_CHAINID);
            }
            //If it's not a cross chain transaction,fromThe chain corresponding to the middle addressidChain must be initiatedidCross chain transactions are validated in validators
            if (type != TxType.CROSS_CHAIN) {
                if (chainId != addrChainId) {
                    throw new NulsException(TxErrorCode.FROM_ADDRESS_NOT_MATCH_CHAIN);
                }
            }
            //Verify account address,Asset Chainid,assetidThe combination uniqueness of
            int assetsChainId = coinFrom.getAssetsChainId();
            int assetsId = coinFrom.getAssetsId();
            // This asset is not available
            if (assetsChainId == 5 && assetsId == 58) {
                throw new NulsException(TxErrorCode.DATA_ERROR);
            }
            boolean rs = uniqueCoin.add(addr + "-" + assetsChainId + "-" + assetsId + "-" + HexUtil.encode(coinFrom.getNonce()));
            if (!rs) {
                throw new NulsException(TxErrorCode.COINFROM_HAS_DUPLICATE_COIN);
            }
            //User issued[Non stopping nodes,Red card]Transaction not allowedfromThere is a contract address in the middle,IffromInclude contract address,So this transaction must have been sent by the system,Transactions sent by the system will not go through basic verification
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
            //IffromContains multiple signed addresses,This indicates that the transaction is a multi signature transaction,Then it must be met,fromsOnly this multiple signed address exists in the system
            for (CoinFrom coinFrom : listFrom) {
                if (!Arrays.equals(existMultiSignAddress, coinFrom.getAddress())) {
                    throw new NulsException(TxErrorCode.MULTI_SIGN_TX_ONLY_SAME_ADDRESS);
                }
            }
        }

    }

    private void validateCoinToBase(Chain chain, TxRegister txRegister, List<CoinTo> listTo, Long height, int txType) throws NulsException {
        // add by pierre at 2021/8/30 Limit maximum transaction amount
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
                //System transactions are allowed to be empty,To be verified by a validator
                return;
            }
            throw new NulsException(TxErrorCode.COINTO_NOT_FOUND);
        }
        //Verify if the payee belongs to the same chain
        Integer addressChainId = null;
        int txChainId = chain.getChainId();
        Set<String> uniqueCoin = new HashSet<>();
        for (CoinTo coinTo : listTo) {

            if (chain.getBestBlockHeight() >= txConfig.getVersion1_39_0_height() && txType == TxType.TRANSFER) {
                if (coinTo.getLockTime() == -1) {
                    throw new NulsException(TxErrorCode.TX_LEDGER_VERIFY_FAIL);
                }
            }

            String addr = AddressTool.getStringAddressByBytes(coinTo.getAddress());

            //Verify the legality of the transaction address,Cross chain module transactions require retrieving the original chain from the addressidTo verify
            int validAddressChainId = txChainId;
            if (ModuleE.CC.abbr.equals(ResponseMessageProcessor.TX_TYPE_MODULE_MAP.get(type))) {
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
                // Prohibit locking amount of0ofto
                if (coinTo.getLockTime() < 0L && toAmount.compareTo(BigInteger.ZERO) <= 0) {
                    chain.getLogger().error("Transaction basic verification failed, Prohibit locking amount of0ofto");
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
            //If it's not a cross chain transaction,toThe chain corresponding to the middle addressidChains that must initiate transactionsid
            if (type != TxType.CROSS_CHAIN) {
                if (chainId != txChainId) {
                    throw new NulsException(TxErrorCode.TO_ADDRESS_NOT_MATCH_CHAIN);
                }
            }
            int assetsChainId = coinTo.getAssetsChainId();
            int assetsId = coinTo.getAssetsId();
            // This asset is not available
            if (assetsChainId == 5 && assetsId == 58) {
                throw new NulsException(TxErrorCode.DATA_ERROR);
            }
            long lockTime = coinTo.getLockTime();
            //toInside address、Asset Chainid、assetid、The combination of lock times cannot be repeated
            boolean rs = uniqueCoin.add(addr + "-" + assetsChainId + "-" + assetsId + "-" + lockTime);
            if (!rs) {
                throw new NulsException(TxErrorCode.COINTO_HAS_DUPLICATE_COIN);
            }
            // Address type
            int addressType = AddressTool.getTypeByAddress(coinTo.getAddress());
            //Contract address acceptanceNULSThe transaction can only becoinBasetransaction,Call contract transactions,Normal stop node(Contract stop node trading is a system transaction,Not going through basic verification)
            if (addressType == BaseConstant.CONTRACT_ADDRESS_TYPE) {
                boolean sysTx = txRegister.getSystemTx();
                if (!sysTx && type != TxType.COIN_BASE
                        && type != TxType.CALL_CONTRACT
                        && type != TxType.STOP_AGENT) {
                    chain.getLogger().error("contract data error: The contract does not accept transfers of this type{} of transaction.", type);
                    throw new NulsException(TxErrorCode.TX_DATA_VALIDATION_ERROR);
                }
            }
            // SWAPTransaction pair address not allowed to appear in nonswapIn transaction
            if (addressType == BaseConstant.PAIR_ADDRESS_TYPE) {
                if (type != TxType.SWAP_TRADE
                        && type != TxType.SWAP_ADD_LIQUIDITY
                        && type != TxType.SWAP_REMOVE_LIQUIDITY
                        && type != TxType.SWAP_SYSTEM_DEAL
                        && type != TxType.SWAP_TRADE_SWAP_STABLE_REMOVE_LP
                ) {
                    chain.getLogger().error("swap data error: The swap pair address does not accept transfers of this type{} of transaction.", type);
                    throw new NulsException(TxErrorCode.TX_DATA_PAIR_ADDRESS_VALIDATION_ERROR);
                }
            }
            // STABLE-SWAPTransaction pair address not allowed to appear in nonstable-swapIn transaction
            if (addressType == BaseConstant.STABLE_PAIR_ADDRESS_TYPE) {
                if (type != TxType.SWAP_TRADE_STABLE_COIN
                        && type != TxType.SWAP_ADD_LIQUIDITY_STABLE_COIN
                        && type != TxType.SWAP_REMOVE_LIQUIDITY_STABLE_COIN
                        && type != TxType.SWAP_SYSTEM_DEAL
                        && type != TxType.SWAP_TRADE
                        && type != TxType.SWAP_STABLE_LP_SWAP_TRADE
                        && type != TxType.SWAP_TRADE_SWAP_STABLE_REMOVE_LP
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
     * Verify if transaction fees are correct
     *
     * @param chain    chainid
     * @param tx       tx
     * @param coinData
     * @return Result
     */
    private void validateFee(Chain chain, Transaction tx, CoinData coinData, TxRegister txRegister) throws NulsException {
        if (txRegister.getSystemTx()) {
            //There is no transaction fee for system transactions
            return;
        }
        int type = tx.getType();
//        int txSize = tx.getSize();
        int feeAssetChainId;
        int feeAssetId;
        if (type == TxType.CROSS_CHAIN && AddressTool.getChainIdByAddress(coinData.getFrom().get(0).getAddress()) != chain.getChainId()) {
            //When initiating a chain for cross chain transactions and not for transactions,Calculate the main assets of the main network as transaction feesNULS
            feeAssetChainId = txConfig.getMainChainId();
            feeAssetId = txConfig.getMainAssetId();
        } else {
            //Calculate the main asset as a handling fee
            feeAssetChainId = chain.getConfig().getChainId();
            feeAssetId = chain.getConfig().getAssetId();
        }
        BigInteger fee = coinData.getFeeByAsset(feeAssetChainId, feeAssetId);
        if (BigIntegerUtils.isLessThan(fee, BigInteger.ZERO)) {
            throw new NulsException(TxErrorCode.INSUFFICIENT_FEE);
        }
        //Recalculate transaction fees based on transaction size to verify actual transaction fees
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
                chain.getLogger().info("proposaltx format: {}", tx.format());
                chain.getLogger().info("feeAssetChainId: {}, feeAssetId: {}, fee: {}, targetFee: {}, txSize: {}",
                        feeAssetChainId, feeAssetId, fee.toString(), targetFee.toString(), tx.getSize());
            }
        } catch (Exception e) {
            chain.getLogger().warn("Log call failed[4]: {}", e.getMessage());
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
     * @param proccessContract        Whether to handle smart contracts
     * @param orphanNoCount           (Is it necessary to verify the ledger again due to the return of the contract)When the orphan transaction was returned Not counting the number of times it is returned
     * @throws NulsException
     */
    private void verifyLedger(Chain chain, List<String> batchProcessList, List<TxPackageWrapper> currentBatchPackableTxs,
                              Set<TxPackageWrapper> orphanTxSet, boolean proccessContract, boolean orphanNoCount) throws NulsException {
        //Start processing
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
                //Remove transactions with failed ledger verification
                for (String hash : failHashs) {
                    String hashStr = transaction.getHash().toHex();
                    if (hash.equals(hashStr)) {
                        if (!backContract && proccessContract && TxManager.isUnSystemSmartContract(chain, transaction.getType())) {
                            //Set Flag,If it is a non system transaction of smart contracts,Not Verified Passed,Then all non system smart contract transactions need to be returned to the waiting queue for packaging.
                            backContract = true;
                        } else {
                            clearInvalidTx(chain, transaction);
                        }
                        it.remove();
                        continue removeAndGo;
                    }
                }
                //Remove orphan transactions, Simultaneously placing orphan transactions into the orphan pool
                for (String hash : orphanHashs) {
                    String hashStr = transaction.getHash().toHex();
                    if (hash.equals(hashStr)) {
                        if (!backContract && proccessContract && TxManager.isUnSystemSmartContract(chain, transaction.getType())) {
                            //Set Flag, If it is a non system transaction of smart contracts,Not Verified Passed,Then all non system smart contract transactions need to be returned to the waiting queue for packaging.
                            backContract = true;
                        } else {
                            //Orphan Trading
                            if (orphanNoCount) {
                                //If it's because after the contract is returned,Verifying that the ledger is an orphan transaction does not require counting Directly return it
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
            //If there are non system transactions of smart contracts that have not been verified,Then all non system smart contract transactions need to be returned to the waiting queue for packaging.
            if (backContract && proccessContract) {
                Iterator<TxPackageWrapper> its = currentBatchPackableTxs.iterator();
                while (its.hasNext()) {
                    TxPackageWrapper txPackageWrapper = it.next();
                    Transaction transaction = txPackageWrapper.getTx();
                    if (TxManager.isUnSystemSmartContract(chain, transaction.getType())) {
                        //If it is a non system transaction of smart contracts,Not Verified Passed,Then all non system smart contract transactions need to be returned to the waiting queue for packaging.
                        packablePool.offerFirstOnlyHash(chain, transaction);
                        chain.setContractTxFail(true);
                        it.remove();
                    }
                }
            }
        }
    }

    /**
     * When adding orphan transactions back to the pending packaging queue, To determine how many times it has been added(Because it was verified to be an orphan transaction during the next packaging, it will be added back again), Once the threshold is reached, it will no longer be added back
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
            //Do not add back(discard), Simultaneously deletemapMiddlekey,And clean up
            chain.getLogger().debug("Clearing up orphan transactions hash:{}", hash.toHex());
            clearInvalidTx(chain, txPackageWrapper.getTx());
            chain.getTxPackageOrphanMap().remove(hash);
        }
    }

    /**
     * Add the transaction back to the packaging queue
     * Trading Orphans(If there is any),Add to the verified transaction set,Sort in reverse order of removal,Then add the frontend of the queue to be packaged in sequence
     *
     * @param chain
     * @param txList      Verified transactions
     * @param orphanTxSet Orphan Trading
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
        //Orphan transactions in reverse order, Add all back to the waiting to be packed queue
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
     * 1.Unified verification
     * 2a:If there are no transactions that fail verification, end!!
     * 2b.When there are failed verifications,moduleVerifyMapFilter out transactions that do not pass.
     * 3.Re validate transactions in the same module that do not pass after the transaction(Including individualverifyandcoinData), execute again1.recursion？
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
                //When the module transaction is filtered out midway through recursion, it will causelistEmpty,At this point, there is no need to call the module's unified validator again
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
                //If there is an error, delete the entire transaction of the module
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
                //Module unified verification without conflicts, frommapKill it in the middle
                it.remove();
                continue;
            }
            chain.getLogger().debug("[Package module verify failed] module:{}, module-code:{}, count:{} , return count:{}",
                    BaseConstant.TX_VALIDATOR, moduleCode, moduleList.size(), txHashList.size());
            /**Conflict detection has failed, Execute clear and unconfirmed rollback frompackingTxListdelete*/
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
             * Processing transactions generated and deleted internally during packaging
             */
            Map<String, List> packProduceCallMap = packProduceProcess(chain, packingTxList);
            Map<String, List<String>> map = getNewProduceTx(chain, packProduceCallMap, packingTxList, height, blockTime);
            List<String> allRemoveList = map.get("allRemoveList");
            if (null != allRemoveList && !allRemoveList.isEmpty()) {
                // Remove the corresponding original transaction,Re validate
                for (int i = allRemoveList.size() - 1; i >= 0; i--) {
                    String hash = allRemoveList.get(i);
                    Iterator<TxPackageWrapper> its = packingTxList.iterator();
                    while (its.hasNext()) {
                        TxPackageWrapper txPackageWrapper = its.next();
                        Transaction tx = txPackageWrapper.getTx();
                        if (hash.equals(tx.getHash().toHex())) {
                            // No need for comparisonTX_PACKAGE_ORPHAN_MAPThe threshold for,Directly join the set,Can share a set with orphan transactions
                            orphanTxSet.add(txPackageWrapper);
                            its.remove();
                            chain.getLogger().info("Return the original transaction to the queue to be packaged during packaging hash:{}", hash);
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
     * @param orphanNoCount   (Is it necessary to verify the ledger again due to the return of the contract)When the orphan transaction was returned Not counting the number of times it is returned
     * @throws NulsException
     */
    private void verifyAgain(Chain chain, Map<String, List<String>> moduleVerifyMap, List<TxPackageWrapper> packingTxList, Set<TxPackageWrapper> orphanTxSet, boolean orphanNoCount) throws NulsException {
        chain.getLogger().debug("------ verifyAgain Batch verification notification for packaging again ------");
        //Send batch verification to the ledger modulecoinDataIdentification of
        LedgerCall.coinDataBatchNotify(chain);
        List<String> batchProcessList = new ArrayList<>();
        for (TxPackageWrapper txPackageWrapper : packingTxList) {
            if (TxManager.isSystemSmartContract(chain, txPackageWrapper.getTx().getType())) {
                //Smart contract system transactions do not require verification of ledgers
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
     * Internal processing of corresponding modules during packaging transactions
     * Module processing for determining the need for packaging in transactions,Send original transaction,Obtain newly generated transactions
     * If there is an abnormality,Need to be removed from the packing list,Original transaction
     *
     * @return
     * @throws NulsException
     */
    private Map<String, List> packProduceProcess(Chain chain, List<TxPackageWrapper> packingTxList) {
        Iterator<TxPackageWrapper> iterator = packingTxList.iterator();
        // K: modulecode V:{k: Original transactionhash, v:Original transaction string}
        Map<String, List> packProduceCallMap = TxManager.getGroup(chain);
        while (iterator.hasNext()) {
            TxPackageWrapper txPackageWrapper = iterator.next();
            Transaction tx = txPackageWrapper.getTx();
            TxRegister txRegister = TxManager.getTxRegister(chain, tx.getType());
            if (txRegister.getPackProduce()) {
                List<TxPackageWrapper> listCallTxs = packProduceCallMap.computeIfAbsent(txRegister.getModuleCode(), k -> new ArrayList<>());
                listCallTxs.add(txPackageWrapper);
                LoggerUtil.LOG.debug("Internal processing and generation of modules during packaging, Original transactionhash:{},  moduleCode:{}", tx.getHash().toHex(), txRegister.getModuleCode());
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
                // Return the complete transaction string
                Map<String, List<String>> map = TransactionCall.packProduce(chain, BaseConstant.TX_PACKPRODUCE, moduleCode, txHexList, height, blockTime, PACKING);
                List<String> newlyList = (List<String>) map.get("newlyList");
                List<String> rmHashList = (List<String>) map.get("rmHashList");
                if (null != newlyList && !newlyList.isEmpty()) {
                    allNewlyList.addAll(newlyList);
                    LoggerUtil.LOG.debug("Module during packaging{}Number of transactions generated:{}, moduleCode:{}", moduleCode, newlyList.size(), moduleCode);
                }
                if (null != rmHashList && !rmHashList.isEmpty()) {
                    allRemoveList.addAll(rmHashList);
                    LoggerUtil.LOG.debug("Module during packaging{}The number of original transactions that need to be removed:{}, moduleCode:{}", moduleCode, rmHashList.size(), moduleCode);
                }
            } catch (NulsException e) {
                chain.getLogger().error("Module during packaging:{} Handling transaction generation exceptions", moduleCode);
                chain.getLogger().error(e);
                Iterator<TxPackageWrapper> iterator = packingTxList.iterator();
                while (iterator.hasNext()) {
                    TxPackageWrapper txPackageWrapper = iterator.next();
                    for (TxPackageWrapper wrapper : moduleList) {
                        if (txPackageWrapper.equals(wrapper)) {
                            // If an exception occurs, clear all original transactions in the module
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
     * Basic cleaning
     * 1.Unconfirmed library
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
        //If the transaction has been confirmed, there is no need to call for ledger cleaning!!
        TransactionConfirmedPO txConfirmed = confirmedTxService.getConfirmedTransaction(chain, tx.getHash());
        if (txConfirmed == null) {
            try {
                //If it is a cleaning mechanism call, Call the unconfirmed rollback of the ledger
                LedgerCall.rollBackUnconfirmTx(chain, RPCUtil.encode(tx.serialize()));
                if (changeStatus) {
                    //Notify ledger status change
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

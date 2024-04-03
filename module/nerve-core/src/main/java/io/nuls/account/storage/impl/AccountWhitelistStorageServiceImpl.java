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

package io.nuls.account.storage.impl;

import io.nuls.account.constant.AccountErrorCode;
import io.nuls.account.constant.AccountStorageConstant;
import io.nuls.account.model.bo.tx.AccountBlockInfo;
import io.nuls.account.model.bo.tx.AccountWhitelistInfo;
import io.nuls.account.model.bo.tx.txdata.AccountWhitelistData;
import io.nuls.account.model.dto.AccountBlockDTO;
import io.nuls.account.model.po.AccountBlockExtendPO;
import io.nuls.account.model.po.AccountBlockPO;
import io.nuls.account.storage.AccountBlockStorageService;
import io.nuls.account.storage.AccountWhitelistStorageService;
import io.nuls.account.util.LoggerUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.exception.NulsRuntimeException;
import io.nuls.core.rockdb.service.RocksDBService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: qinyifeng
 */
@Component
public class AccountWhitelistStorageServiceImpl implements AccountWhitelistStorageService {


    @Override
    public boolean saveAccountList(AccountWhitelistData accounts) {
        if (null == accounts) {
            throw new NulsRuntimeException(AccountErrorCode.PARAMETER_ERROR);
        }
        AccountWhitelistInfo[] infos = accounts.getInfos();
        if (infos == null || infos.length == 0) {
            return true;
        }
        Map<byte[], byte[]> accountPoMap = new HashMap<>();
        try {
            for (AccountWhitelistInfo po : infos) {
                //Serializing objects asbyteArray storage
                accountPoMap.put(po.getAddress(), po.serialize());
            }
            return RocksDBService.batchPut(AccountStorageConstant.DB_NAME_ACCOUNT_WHITELIST, accountPoMap);
        } catch (Exception e) {
            LoggerUtil.LOG.error(e.getMessage());
            throw new NulsRuntimeException(AccountErrorCode.DB_SAVE_BATCH_ERROR);
        }
    }
    @Override
    public AccountWhitelistInfo getAccount(byte[] address) {
        byte[] accountBytes = RocksDBService.get(AccountStorageConstant.DB_NAME_ACCOUNT_WHITELIST, address);
        if (null == accountBytes) {
            return null;
        }
        AccountWhitelistInfo accountPo = new AccountWhitelistInfo();
        try {
            //takebyteDeserialize an array asAccountPoreturn
            accountPo.parse(accountBytes, 0);
        } catch (Exception e) {
            LoggerUtil.LOG.error(e.getMessage());
            throw new NulsRuntimeException(AccountErrorCode.DB_QUERY_ERROR);
        }
        return accountPo;
    }

}

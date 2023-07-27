package io.nuls.account.service;

import java.util.Map;

public interface SigMachineService {

    String request(Map<String, Object> params) throws Exception;

}

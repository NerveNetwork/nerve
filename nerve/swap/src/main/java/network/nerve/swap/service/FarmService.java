package network.nerve.swap.service;

import io.nuls.core.basic.Result;
import network.nerve.swap.model.dto.FarmInfoDTO;
import network.nerve.swap.model.dto.FarmInfoVO;
import network.nerve.swap.model.dto.FarmUserInfoDTO;
import network.nerve.swap.model.dto.FarmUserInfoVO;

import java.util.List;

/**
 * @author Niels
 */
public interface FarmService {

    Result<String> createFarm(String address, String stakeTokenStr, String syrupTokenStr, double syrupPerBlock, long startHeight, long lockedTime, double totalSyrupAmount, boolean modifiable, long withdrawLockTime, String password);

    Result<String> stake(String address, String farmHash, double amount, String password);

    Result<String> withdraw(String address, String farmHash, double amount, String password);

    Result<FarmInfoDTO> farmInfo(String farmHash);

    Result<FarmUserInfoDTO> farmUserInfo(String farmHash, String userAddress);

    Result<List<FarmInfoVO>> getFarmList(int chainId);

    Result<FarmInfoVO> farmDetail(int chainId,String farmHash);

    Result<FarmUserInfoVO> farmUserDetail(int chainId,String farmHash, String address);
}

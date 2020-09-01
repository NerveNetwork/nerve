pragma solidity ^0.5.5;

import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v2.5.0/contracts/token/ERC20/IERC20.sol";
import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v2.5.0/contracts/token/ERC20/SafeERC20.sol";
import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v2.5.0/contracts/math/SafeMath.sol";
import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v2.5.0/contracts/utils/Address.sol";
import "https://github.com/GNSPS/solidity-bytes-utils/blob/v0.0.8/contracts/BytesLib.sol";

contract NerveMultiSigWalletII {
    using Address for address;
    using SafeERC20 for IERC20;
    using SafeMath for uint256;
    using BytesLib for bytes;

    modifier isOwner{
        require(owner == msg.sender, "Only owner can execute it");
        _;
    }
    modifier isManager{
        require(managers[msg.sender] == 1, "Only manager can execute it");
        _;
    }
    bool public upgrade = false;
    address public upgradeContractAddress = address(0);
    // 最大管理员数量
    uint public max_managers = 15;
    // 最小签名比例 66%
    uint public rate = 66;
    // 签名字节长度
    uint public signatureLength = 65;
    // 比例分母
    uint constant DENOMINATOR = 100;
    // 当前交易的最小签名数量
    uint8 public current_min_signatures;
    address public owner;
    mapping(address => uint8) private seedManagers;
    address[] public seedManagerArray;
    mapping(address => uint8) private managers;
    address[] private managerArray;
    mapping(bytes32 => uint8) private completedKeccak256s;
    mapping(string => uint8) private completedTxs;

    constructor(address[] memory _managers) public{
        require(_managers.length <= max_managers, "Exceeded the maximum number of managers");
        owner = msg.sender;
        managerArray = _managers;
        for (uint8 i = 0; i < managerArray.length; i++) {
            managers[managerArray[i]] = 1;
            seedManagers[managerArray[i]] = 1;
            seedManagerArray.push(managerArray[i]);
        }
        require(managers[owner] == 0, "Contract creator cannot act as manager");
        // 设置当前交易的最小签名数量
        current_min_signatures = calMinSignatures(managerArray.length);
    }
    function() external payable {
        emit DepositFunds(msg.sender, msg.value);
    }


    function createOrSignWithdraw(string memory txKey, address payable to, uint256 amount, bool isERC20, address ERC20, bytes memory signatures) public isManager {
        require(to != address(0), "Withdraw: transfer to the zero address");
        require(amount > 0, "Withdrawal amount must be greater than 0");
        // 校验已经完成的交易
        require(completedTxs[txKey] == 0, "Transaction has been completed");
        // 校验提现金额
        if (isERC20) {
            validateTransferERC20(ERC20, to, amount);
        } else {
            require(address(this).balance >= amount, "This contract address does not have sufficient balance of ether");
        }
        // 校验签名
        bytes32 vHash = keccak256(abi.encodePacked(txKey, to, amount, isERC20, ERC20));
        require(completedKeccak256s[vHash] == 0, "Invalid signatures");
        address[] memory sManagers = splitSignatures(vHash, signatures);
        require(calValidSignatureCount(sManagers) >= current_min_signatures, "Insufficient number of signatures");
        // 执行转账
        if (isERC20) {
            transferERC20(ERC20, to, amount);
        } else {
            // 实际到账
            require(address(this).balance >= amount, "This contract address does not have sufficient balance of ether");
            to.transfer(amount);
            emit TransferFunds(to, amount);
        }
        emit TxWithdrawCompleted(txKey);
        // 保存交易数据
        completeTx(txKey, vHash, 1);
    }


    function createOrSignManagerChange(string memory txKey, address[] memory adds, address[] memory removes, uint8 count, bytes memory signatures) public isManager {
        require(adds.length > 0 || removes.length > 0, "There are no managers joining or exiting");
        // 校验已经完成的交易
        require(completedTxs[txKey] == 0, "Transaction has been completed");
        preValidateAddsAndRemoves(adds, removes);
        // 校验签名
        bytes32 vHash = keccak256(abi.encodePacked(txKey, adds, count, removes));
        require(completedKeccak256s[vHash] == 0, "Invalid signatures");
        address[] memory sManagers = splitSignatures(vHash, signatures);
        // 判断最小签名数量
        require(calValidSignatureCount(sManagers) >= current_min_signatures, "Insufficient number of signatures");
        // 变更管理员
        removeManager(removes);
        addManager(adds);
        // 更新当前交易的最小签名数
        current_min_signatures = calMinSignatures(managerArray.length);
        // add managerChange event
        emit TxManagerChangeCompleted(txKey);
        // 保存交易数据
        completeTx(txKey, vHash, 1);
    }

    function createOrSignUpgrade(string memory txKey, address upgradeContract, bytes memory signatures) public isManager {
        // 校验已经完成的交易
        require(completedTxs[txKey] == 0, "Transaction has been completed");
        require(!upgrade, "It has been upgraded");
        require(upgradeContract.isContract(), "The address is not a contract address");
        // 校验签名
        bytes32 vHash = keccak256(abi.encodePacked(txKey, upgradeContract));
        require(completedKeccak256s[vHash] == 0, "Invalid signatures");
        address[] memory sManagers = splitSignatures(vHash, signatures);
        require(calValidSignatureCount(sManagers) >= current_min_signatures, "Insufficient number of signatures");
        // 变更可升级
        upgrade = true;
        upgradeContractAddress = upgradeContract;
        // add managerChange event
        emit TxUpgradeCompleted(txKey);
        // 保存交易数据
        completeTx(txKey, vHash, 1);
    }

    function splitSignatures(bytes32 hash, bytes memory signatures) internal view returns (address[] memory){
        uint times = signatures.length.div(signatureLength);
        bytes[] memory array = new bytes[](times);
        address[] memory result = new address[](times);
        uint k = 0;
        uint8 j = 0;
        for (uint i = 0; i < times; i++) {
            bytes memory sign = signatures.slice(k, signatureLength);
            bool add = validateByArray(sign, array, j);
            if (add) {
                result[j] = ecrecovery(hash, sign);
                j++;
            }
            k += signatureLength;
        }
        uint n = times - j;
        assembly { mstore(result, sub(mload(result), n)) }
        delete array;
        return result;
    }

    function validateByArray(bytes memory mng, bytes[] memory validateArray, uint8 j) internal pure returns (bool) {
        uint len = validateArray.length;
        if(len > 0) {
            bytes memory temp;
            for (uint8 i = 0; i < len; i++) {
                temp = validateArray[i];
                if (temp.length == 0) {
                    break;
                }
                if (temp.equal(mng)) {
                    return false;
                }
            }
        }
        validateArray[j] = mng;
        return true;
    }

    function validateByArray(address mng, address[] memory validateArray, uint8 j) internal pure returns (bool) {
        uint len = validateArray.length;
        if(len > 0) {
            address temp;
            for (uint8 i = 0; i < len; i++) {
                temp = validateArray[i];
                if (temp == address(0)) {
                    break;
                }
                if (temp == mng) {
                    return false;
                }
            }
        }
        validateArray[j] = mng;
        return true;
    }

    function ecrecovery(bytes32 hash, bytes memory sig) internal view returns (address) {
        bytes32 r;
        bytes32 s;
        uint8 v;
        if (sig.length != signatureLength) {
            return address(0);
        }
        assembly {
            r := mload(add(sig, 32))
            s := mload(add(sig, 64))
            v := and(mload(add(sig, 65)), 255)
        }
        // https://github.com/ethereum/go-ethereum/issues/2053
        if (v < 27) {
            v += 27;
        }
        if (v != 27 && v != 28) {
            return address(0);
        }
        return ecrecover(hash, v, r, s);
    }

    function calValidSignatureCount(address[] memory signatureManagers) internal view returns (uint8){
        // 遍历已签名列表，筛选有效签名数量
        uint8 count = 0;
        uint len = signatureManagers.length;
        address mng;
        for (uint i = 0; i < len; i++) {
            mng = signatureManagers[i];
            if (managers[mng] > 0) {
                count++;
            }
        }
        return count;
    }

    function preValidateAddsAndRemoves(address[] memory adds, address[] memory removes) internal view {
        // 校验adds
        uint addLen = adds.length;
        address[] memory addArray = new address[](addLen + 1);
        uint8 j = 0;
        for (uint i = 0; i < addLen; i++) {
            address add = adds[i];
            require(managers[add] == 0, "The address list that is being added already exists as a manager");
            require(validateByArray(add, addArray, j), "Duplicate parameters for the address to join");
            j++;
        }
        require(validateByArray(owner, addArray, j), "Contract creator cannot act as manager");
        delete addArray;
        // 校验removes
        uint removeLen = removes.length;
        address[] memory removeArray = new address[](removeLen);
        j = 0;
        for (uint i = 0; i < removeLen; i++) {
            address remove = removes[i];
            require(seedManagers[remove] == 0, "Can't exit seed manager");
            require(managers[remove] == 1, "There are addresses in the exiting address list that are not manager");
            require(validateByArray(remove, removeArray, j), "Duplicate parameters for the address to exit");
        }
        delete removeArray;
        require(managerArray.length + addLen - removeLen <= max_managers, "Exceeded the maximum number of managers");
    }

    /*
     根据 `当前有效管理员数量` 和 `最小签名比例` 计算最小签名数量，向上取整
    */
    function calMinSignatures(uint managerCounts) internal view returns (uint8) {
        if (managerCounts == 0) {
            return 0;
        }
        uint numerator = rate * managerCounts + DENOMINATOR - 1;
        return uint8(numerator / DENOMINATOR);
    }
    function removeManager(address[] memory removes) internal {
        if (removes.length == 0) {
            return;
        }
        for (uint i = 0; i < removes.length; i++) {
            delete managers[removes[i]];
        }
        uint newLength = managerArray.length - removes.length;
        address[] memory tempManagers = new address[](newLength);
        // 遍历修改前管理员列表
        uint k = 0;
        for (uint i = 0; i < managerArray.length; i++) {
            if (managers[managerArray[i]] == 1) {
                tempManagers[k++] = managerArray[i];
            }
        }
        delete managerArray;
        managerArray = tempManagers;
    }
    function addManager(address[] memory adds) internal {
        if (adds.length == 0) {
            return;
        }
        for (uint i = 0; i < adds.length; i++) {
            address add = adds[i];
            if(managers[add] == 0) {
                managers[add] = 1;
                managerArray.push(add);
            }
        }
    }
    function completeTx(string memory txKey, bytes32 keccak256Hash, uint8 e) internal {
        completedTxs[txKey] = e;
        completedKeccak256s[keccak256Hash] = e;
    }
    function validateTransferERC20(address ERC20, address to, uint256 amount) internal view {
        require(to != address(0), "ERC20: transfer to the zero address");
        require(address(this) != ERC20, "Do nothing by yourself");
        require(ERC20.isContract(), "The address is not a contract address");
        IERC20 token = IERC20(ERC20);
        uint256 balance = token.balanceOf(address(this));
        require(balance >= amount, "No enough balance of token");
    }
    function transferERC20(address ERC20, address to, uint256 amount) internal {
        IERC20 token = IERC20(ERC20);
        uint256 balance = token.balanceOf(address(this));
        require(balance >= amount, "No enough balance of token");
        token.safeTransfer(to, amount);
    }
    function closeUpgrade() public isOwner {
        require(upgrade, "Denied");
        upgrade = false;
    }
    function upgradeContractS1() public isOwner {
        require(upgrade, "Denied");
        require(upgradeContractAddress != address(0), "ERROR: transfer to the zero address");
        address(uint160(upgradeContractAddress)).transfer(address(this).balance);
    }
    function upgradeContractS2(address ERC20) public isOwner {
        require(upgrade, "Denied");
        require(upgradeContractAddress != address(0), "ERROR: transfer to the zero address");
        require(address(this) != ERC20, "Do nothing by yourself");
        require(ERC20.isContract(), "The address is not a contract address");
        IERC20 token = IERC20(ERC20);
        uint256 balance = token.balanceOf(address(this));
        require(balance >= 0, "No enough balance of token");
        token.safeTransfer(upgradeContractAddress, balance);
    }
    function isCompletedTx(string memory txKey) public view returns (bool){
        return completedTxs[txKey] > 0;
    }
    function ifManager(address _manager) public view returns (bool) {
        return managers[_manager] == 1;
    }
    function allManagers() public view returns (address[] memory) {
        return managerArray;
    }
    event DepositFunds(address from, uint amount);
    event TransferFunds(address to, uint amount);
    event TxWithdrawCompleted(string txKey);
    event TxManagerChangeCompleted(string txKey);
    event TxUpgradeCompleted(string txKey);
}

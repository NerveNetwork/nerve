pragma solidity ^0.5.5;

import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v2.5.0/contracts/token/ERC20/IERC20.sol";
import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v2.5.0/contracts/token/ERC20/SafeERC20.sol";
import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v2.5.0/contracts/math/SafeMath.sol";
import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v2.5.0/contracts/utils/Address.sol";
import "https://github.com/GNSPS/solidity-bytes-utils/blob/v0.0.8/contracts/BytesLib.sol";

interface IERC20Minter {
    function mint(address to, uint256 amount) external;
    function burn(uint256 amount) external;
    function replaceMinter(address newMinter) external;
}

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
    // 最小管理员数量
    uint public min_managers = 3;
    // 最小签名比例 66%
    uint public rate = 66;
    // 签名字节长度
    uint public signatureLength = 65;
    // 比例分母
    uint constant DENOMINATOR = 100;
    // 当前合约版本
    uint8 constant VERSION = 2;
    // 当前交易的最小签名数量
    uint8 public current_min_signatures;
    address public owner;
    mapping(address => uint8) private seedManagers;
    address[] private seedManagerArray;
    mapping(address => uint8) private managers;
    address[] private managerArray;
    mapping(bytes32 => uint8) private completedKeccak256s;
    mapping(string => uint8) private completedTxs;
    mapping(address => uint8) private minterERC20s;

    constructor(address[] memory _managers) public{
        require(_managers.length <= max_managers, "Exceeded the maximum number of managers");
        require(_managers.length >= min_managers, "Not reaching the min number of managers");
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
        require(bytes(txKey).length == 64, "Fixed length of txKey: 64");
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
        bytes32 vHash = keccak256(abi.encodePacked(txKey, to, amount, isERC20, ERC20, VERSION));
        // 校验请求重复性
        require(completedKeccak256s[vHash] == 0, "Invalid signatures");
        // 校验签名
        require(validSignature(vHash, signatures), "Valid signatures fail");
        // 执行转账
        if (isERC20) {
            transferERC20(ERC20, to, amount);
        } else {
            // 实际到账
            require(address(this).balance >= amount, "This contract address does not have sufficient balance of ether");
            to.transfer(amount);
            emit TransferFunds(to, amount);
        }
        // 保存交易数据
        completeTx(txKey, vHash, 1);
        emit TxWithdrawCompleted(txKey);
    }


    function createOrSignManagerChange(string memory txKey, address[] memory adds, address[] memory removes, uint8 count, bytes memory signatures) public isManager {
        require(bytes(txKey).length == 64, "Fixed length of txKey: 64");
        require(adds.length > 0 || removes.length > 0, "There are no managers joining or exiting");
        // 校验已经完成的交易
        require(completedTxs[txKey] == 0, "Transaction has been completed");
        preValidateAddsAndRemoves(adds, removes);
        bytes32 vHash = keccak256(abi.encodePacked(txKey, adds, count, removes, VERSION));
        // 校验请求重复性
        require(completedKeccak256s[vHash] == 0, "Invalid signatures");
        // 校验签名
        require(validSignature(vHash, signatures), "Valid signatures fail");
        // 变更管理员
        removeManager(removes);
        addManager(adds);
        // 更新当前交易的最小签名数
        current_min_signatures = calMinSignatures(managerArray.length);
        // 保存交易数据
        completeTx(txKey, vHash, 1);
        // add event
        emit TxManagerChangeCompleted(txKey);
    }

    function createOrSignUpgrade(string memory txKey, address upgradeContract, bytes memory signatures) public isManager {
        require(bytes(txKey).length == 64, "Fixed length of txKey: 64");
        // 校验已经完成的交易
        require(completedTxs[txKey] == 0, "Transaction has been completed");
        require(!upgrade, "It has been upgraded");
        require(upgradeContract.isContract(), "The address is not a contract address");
        // 校验
        bytes32 vHash = keccak256(abi.encodePacked(txKey, upgradeContract, VERSION));
        // 校验请求重复性
        require(completedKeccak256s[vHash] == 0, "Invalid signatures");
        // 校验签名
        require(validSignature(vHash, signatures), "Valid signatures fail");
        // 变更可升级
        upgrade = true;
        upgradeContractAddress = upgradeContract;
        // 保存交易数据
        completeTx(txKey, vHash, 1);
        // add event
        emit TxUpgradeCompleted(txKey);
    }

    function validSignature(bytes32 hash, bytes memory signatures) internal view returns (bool) {
        require(signatures.length <= 975, "Max length of signatures: 975");
        // 获取签名列表对应的有效管理员,如果存在错误的签名、或者不是管理员的签名，就失败
        uint sManagersCount = getManagerFromSignatures(hash, signatures);
        // 判断最小签名数量
        return sManagersCount >= current_min_signatures;
    }

    function getManagerFromSignatures(bytes32 hash, bytes memory signatures) internal view returns (uint){
        uint signCount = 0;
        uint times = signatures.length.div(signatureLength);
        address[] memory result = new address[](times);
        uint k = 0;
        uint8 j = 0;
        for (uint i = 0; i < times; i++) {
            bytes memory sign = signatures.slice(k, signatureLength);
            address mAddress = ecrecovery(hash, sign);
            require(mAddress != address(0), "Signatures error");
            // 管理计数
            if (managers[mAddress] == 1) {
                signCount++;
                result[j++] = mAddress;
            }
            k += signatureLength;
        }
        // 验证地址重复性
        bool suc = repeatability(result);
        delete result;
        require(suc, "Signatures duplicate");
        return signCount;
    }

    function validateRepeatability(address currentAddress, address[] memory list) internal pure returns (bool) {
        address tempAddress;
        for (uint i = 0; i < list.length; i++) {
            tempAddress = list[i];
            if (tempAddress == address(0)) {
                break;
            }
            if (tempAddress == currentAddress) {
                return false;
            }
        }
        return true;
    }

    function repeatability(address[] memory list) internal pure returns (bool) {
        for (uint i = 0; i < list.length; i++) {
            address address1 = list[i];
            if (address1 == address(0)) {
                break;
            }
            for (uint j = i + 1; j < list.length; j++) {
                address address2 = list[j];
                if (address2 == address(0)) {
                    break;
                }
                if (address1 == address2) {
                    return false;
                }
            }
        }
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
            v := byte(0, mload(add(sig, 96)))
        }
        if(uint256(s) > 0x7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF5D576E7357A4501DDFE92F46681B20A0) {
            return address(0);
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

    function preValidateAddsAndRemoves(address[] memory adds, address[] memory removes) internal view {
        // 校验adds
        uint addLen = adds.length;
        for (uint i = 0; i < addLen; i++) {
            address add = adds[i];
            require(add != address(0), "ERROR: Detected zero address in adds");
            require(managers[add] == 0, "The address list that is being added already exists as a manager");
        }
        require(repeatability(adds), "Duplicate parameters for the address to join");
        // 校验合约创建者不能被添加
        require(validateRepeatability(owner, adds), "Contract creator cannot act as manager");
        // 校验removes
        require(repeatability(removes), "Duplicate parameters for the address to exit");
        uint removeLen = removes.length;
        for (uint i = 0; i < removeLen; i++) {
            address remove = removes[i];
            require(seedManagers[remove] == 0, "Can't exit seed manager");
            require(managers[remove] == 1, "There are addresses in the exiting address list that are not manager");
        }
        require(managerArray.length + adds.length - removes.length <= max_managers, "Exceeded the maximum number of managers");
    }

    /*
     根据 `当前有效管理员数量` 和 `最小签名比例` 计算最小签名数量，向上取整
    */
    function calMinSignatures(uint managerCounts) internal view returns (uint8) {
        require(managerCounts > 0, "Manager Can't empty.");
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
        // 遍历修改前管理员列表
        for (uint i = 0; i < managerArray.length; i++) {
            if (managers[managerArray[i]] == 0) {
                delete managerArray[i];
            }
        }
        uint tempIndex = 0x10;
        for (uint i = 0; i<managerArray.length; i++) {
            address temp = managerArray[i];
            if (temp == address(0)) {
                if (tempIndex == 0x10) tempIndex = i;
                continue;
            } else if (tempIndex != 0x10) {
                managerArray[tempIndex] = temp;
                tempIndex++;
            }
        }
        managerArray.length -= removes.length;
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
        if (isMinterERC20(ERC20)) {
            // 定制ERC20验证结束
            return;
        }
        IERC20 token = IERC20(ERC20);
        uint256 balance = token.balanceOf(address(this));
        require(balance >= amount, "No enough balance of token");
    }
    function transferERC20(address ERC20, address to, uint256 amount) internal {
        if (isMinterERC20(ERC20)) {
            // 定制的ERC20，跨链转入以太坊网络即增发
            IERC20Minter minterToken = IERC20Minter(ERC20);
            minterToken.mint(to, amount);
            return;
        }
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
        if (isMinterERC20(ERC20)) {
            // 定制的ERC20，转移增发销毁权限到新多签合约
            IERC20Minter minterToken = IERC20Minter(ERC20);
            minterToken.replaceMinter(upgradeContractAddress);
        }
    }

    // 是否定制的ERC20
    function isMinterERC20(address ERC20) public view returns (bool) {
        return minterERC20s[ERC20] > 0;
    }

    // 登记定制的ERC20
    function registerMinterERC20(address ERC20) public isOwner {
        require(address(this) != ERC20, "Do nothing by yourself");
        require(ERC20.isContract(), "The address is not a contract address");
        require(!isMinterERC20(ERC20), "This address has already been registered");
        minterERC20s[ERC20] = 1;
    }

    // 取消登记定制的ERC20
    function unregisterMinterERC20(address ERC20) public isOwner {
        require(isMinterERC20(ERC20), "This address is not registered");
        delete minterERC20s[ERC20];
    }

    // 从eth网络跨链转出资产(ETH or ERC20)
    function crossOut(string memory to, uint256 amount, address ERC20) public payable returns (bool) {
        address from = msg.sender;
        require(amount > 0, "ERROR: Zero amount");
        if (ERC20 != address(0)) {
            require(msg.value == 0, "ERC20: Does not accept Ethereum Coin");
            require(ERC20.isContract(), "The address is not a contract address");
            IERC20 token = IERC20(ERC20);
            uint256 allowance = token.allowance(from, address(this));
            require(allowance >= amount, "No enough amount for authorization");
            uint256 fromBalance = token.balanceOf(from);
            require(fromBalance >= amount, "No enough balance of the token");
            token.safeTransferFrom(from, address(this), amount);
            if (isMinterERC20(ERC20)) {
                // 定制的ERC20，从以太坊网络跨链转出token即销毁
                IERC20Minter minterToken = IERC20Minter(ERC20);
                minterToken.burn(amount);
            }
        } else {
            require(msg.value == amount, "Inconsistency Ethereum amount");
        }
        emit CrossOutFunds(from, to, amount, ERC20);
        return true;
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
    event CrossOutFunds(address from, string to, uint amount, address ERC20);
    event TransferFunds(address to, uint amount);
    event TxWithdrawCompleted(string txKey);
    event TxManagerChangeCompleted(string txKey);
    event TxUpgradeCompleted(string txKey);
}

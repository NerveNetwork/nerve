pragma solidity ^0.5.5;

import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v2.5.0/contracts/token/ERC20/IERC20.sol";
import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v2.5.0/contracts/token/ERC20/SafeERC20.sol";
import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v2.5.0/contracts/math/SafeMath.sol";
import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v2.5.0/contracts/utils/Address.sol";

contract MultiSigWallet {
    using Address for address;
    using SafeERC20 for IERC20;
    using SafeMath for uint256;

    modifier isOwner{
        require(owner == msg.sender, "Only owner can execute it");
        _;
    }
    modifier isManager{
        require(managers[msg.sender] == 1, "Only manager can execute it");
        _;
    }
    // 用于提现
    struct TxWithdraw {
        uint8 e;
        address creator;
        address payable to;
        uint256 amount;
        bool isERC20;
        address ERC20;
        Signature signature;
    }
    // 用于管理员变更
    struct TxManagerChange {
        uint8 e;
        address creator;
        address[] adds;
        address[] removes;
        Signature signature;
    }
    // 用于合约升级
    struct TxUpgrade {
        uint8 e;
        Signature signature;
    }
    struct Signature {
        uint8 signatureCount;
        address[] signed;
        mapping(address => uint8) signatures;
    }
    struct Validator {
        uint8 e;
        mapping(address => uint8) addsMap;
        mapping(address => uint8) removesMap;
    }
    bool public upgrade = false;
    // 最大管理员数量
    uint public max_managers = 15;
    // 最小签名比例 66%
    uint public rate = 66;
    // 比例分母
    uint constant DENOMINATOR = 100;
    string constant UPDATE_SEED_MANAGERS = "updateSeedManagers";
    // 当前提现交易的最小签名数量
    uint public current_withdraw_min_signatures;
    address public owner;
    mapping(address => uint8) private seedManagers;
    address[] public seedManagerArray;
    mapping(address => uint8) private managers;
    address[] private managerArray;
    mapping(string => TxWithdraw) private pendingTxWithdraws;
    mapping(string => TxManagerChange) private pendingTxManagerChanges;
    mapping(string => TxUpgrade) private pendingTxUpgrade;
    uint public pendingChangeCount = 0;
    mapping(string => uint8) private completedTxs;
    mapping(string => Validator) private validatorManager;

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
        // 设置当前提现交易的最小签名数量
        current_withdraw_min_signatures = calMinSignatures(managerArray.length);
    }
    function() external payable {
        emit DepositFunds(msg.sender, msg.value);
    }
    function createOrSignWithdraw(string memory txKey, address payable to, uint256 amount, bool isERC20, address ERC20) public isManager {
        require(to != address(0), "Withdraw: transfer to the zero address");
        require(amount > 0, "Withdrawal amount must be greater than 0");
        // 校验已经完成的交易
        require(completedTxs[txKey] == 0, "Transaction has been completed");
        // 若交易已创建，则签名交易
        if (pendingTxWithdraws[txKey].e != 0) {
            signTx(txKey);
            return;
        }
        if (isERC20) {
            validateTransferERC20(ERC20, to, amount);
        } else {
            require(address(this).balance >= amount, "This contract address does not have sufficient balance of ether");
        }
        TxWithdraw memory tx1;
        pendingTxWithdraws[txKey] = tx1;
        TxWithdraw storage _tx = pendingTxWithdraws[txKey];
        _tx.e = 1;
        _tx.creator = msg.sender;
        _tx.to = to;
        _tx.amount = amount;
        _tx.isERC20 = isERC20;
        _tx.ERC20 = ERC20;
        _tx.signature.signatureCount = 1;
        _tx.signature.signed.push(msg.sender);
        _tx.signature.signatures[msg.sender] = 1;
    }
    function signTx(string memory txKey) internal {
        TxWithdraw storage tx1 = pendingTxWithdraws[txKey];
        bool canWithdraw = isCompleteSign(tx1.signature, current_withdraw_min_signatures, 0);
        if (canWithdraw) {
            address[] memory signers = getSigners(tx1.signature);
            if (tx1.isERC20) {
                transferERC20(tx1.ERC20, tx1.to, tx1.amount);
            } else {
                // 实际到账
                uint transferAmount = tx1.amount;
                require(address(this).balance >= transferAmount, "This contract address does not have sufficient balance of ether");
                tx1.to.transfer(transferAmount);
                emit TransferFunds(tx1.to, transferAmount);
            }
            emit TxWithdrawCompleted(signers, txKey);
            // 移除暂存数据
            deletePendingTx(txKey, 1);
        }
    }
    function createOrSignManagerChange(string memory txKey, address[] memory adds, address[] memory removes) public isManager {
        require(adds.length > 0 || removes.length > 0, "There are no managers joining or exiting");
        // 校验已经完成的交易
        require(completedTxs[txKey] == 0, "Transaction has been completed");
        // 若交易已创建，则签名交易
        if (pendingTxManagerChanges[txKey].e != 0) {
            signTxManagerChange(txKey);
            return;
        }
        preValidateAddsAndRemoves(txKey, adds, removes, false);
        TxManagerChange memory tx1;
        pendingTxManagerChanges[txKey] = tx1;
        TxManagerChange storage _tx = pendingTxManagerChanges[txKey];
        _tx.e = 1;
        _tx.creator = msg.sender;
        _tx.adds = adds;
        _tx.removes = removes;
        _tx.signature.signed.push(msg.sender);
        _tx.signature.signatures[msg.sender] = 1;
        _tx.signature.signatureCount = 1;
        pendingChangeCount++;
    }
    function signTxManagerChange(string memory txKey) internal {
        TxManagerChange storage tx1 = pendingTxManagerChanges[txKey];
        address[] memory removes = tx1.removes;
        uint removeLengh = removes.length;
        if(removeLengh > 0) {
            for (uint i = 0; i < removeLengh; i++) {
                if (removes[i] == msg.sender) {
                    revert("Exiting manager cannot participate in manager change transactions");
                }
            }
        }
        bool canChange = isCompleteSign(tx1.signature, 0, removeLengh);
        if (canChange) {
            // 变更管理员
            removeManager(tx1.removes, false);
            addManager(tx1.adds, false);
            // 更新当前提现交易的最小签名数
            current_withdraw_min_signatures = calMinSignatures(managerArray.length);
            pendingChangeCount--;
            address[] memory signers = getSigners(tx1.signature);
            // add managerChange event
            emit TxManagerChangeCompleted(signers, txKey);
            // 移除暂存数据
            deletePendingTx(txKey, 2);
        }
    }
    function createOrSignUpgrade(string memory txKey) public isManager {
        // 校验已经完成的交易
        require(completedTxs[txKey] == 0, "Transaction has been completed");
        // 若交易已创建，则签名交易
        if (pendingTxUpgrade[txKey].e != 0) {
            signTxUpgrade(txKey);
            return;
        }
        TxUpgrade memory tx1;
        pendingTxUpgrade[txKey] = tx1;
        TxUpgrade storage _tx = pendingTxUpgrade[txKey];
        _tx.e = 1;
        _tx.signature.signed.push(msg.sender);
        _tx.signature.signatures[msg.sender] = 1;
        _tx.signature.signatureCount = 1;
    }
    function signTxUpgrade(string memory txKey) internal {
        TxUpgrade storage tx1 = pendingTxUpgrade[txKey];
        bool canUpgrade= isCompleteSign(tx1.signature, current_withdraw_min_signatures, 0);
        if (canUpgrade) {
            // 变更可升级
            upgrade = true;
            address[] memory signers = getSigners(tx1.signature);
            // add managerChange event
            emit TxUpgradeCompleted(signers, txKey);
            // 移除暂存数据
            deletePendingTx(txKey, 3);
        }
    }
    function isCompleteSign(Signature storage signature, uint min_signatures, uint removeLengh) internal returns (bool){
        bool complete = false;
        // 计算当前有效签名
        signature.signatureCount = calValidSignatureCount(signature);
        if( min_signatures == 0) {
            min_signatures = calMinSignatures(managerArray.length - removeLengh);
        }
        if (signature.signatureCount >= min_signatures) {
            complete = true;
        }
        if (!complete) {
            require(signature.signatures[msg.sender] == 0, "Duplicate signature");
            signature.signed.push(msg.sender);
            signature.signatures[msg.sender] = 1;
            signature.signatureCount++;
            if (signature.signatureCount >= min_signatures) {
                complete = true;
            }
        }
        return complete;
    }
    function calValidSignatureCount(Signature storage signature) internal returns (uint8){
        // 遍历已签名列表，筛选有效签名数量
        uint8 count = 0;
        uint len = signature.signed.length;
        for (uint i = 0; i < len; i++) {
            if (managers[signature.signed[i]] > 0) {
                count++;
            } else {
                delete signature.signatures[signature.signed[i]];
            }
        }
        return count;
    }
    function getSigners(Signature storage signature) internal returns (address[] memory){
        address[] memory signers = new address[](signature.signatureCount);
        // 遍历管理员列表，筛选已签名数组
        uint len = managerArray.length;
        uint k = 0;
        for (uint i = 0; i < len; i++) {
            if (signature.signatures[managerArray[i]] > 0) {
                signers[k++] = managerArray[i];
                delete signature.signatures[managerArray[i]];
            }
        }
        return signers;
    }
    function preValidateAddsAndRemoves(string memory txKey, address[] memory adds, address[] memory removes, bool _isOwner) internal {
        Validator memory _validator;
        validatorManager[txKey] = _validator;
        // 校验adds
        mapping(address => uint8) storage validateAdds = validatorManager[txKey].addsMap;
        uint addLen = adds.length;
        for (uint i = 0; i < addLen; i++) {
            address add = adds[i];
            require(managers[add] == 0, "The address list that is being added already exists as a manager");
            require(validateAdds[add] == 0, "Duplicate parameters for the address to join");
            validateAdds[add] = 1;
        }
        require(validateAdds[owner] == 0, "Contract creator cannot act as manager");
        // 校验removes
        mapping(address => uint8) storage validateRemoves = validatorManager[txKey].removesMap;
        uint removeLen = removes.length;
        for (uint i = 0; i < removeLen; i++) {
            address remove = removes[i];
            require(_isOwner || seedManagers[remove] == 0, "Can't exit seed manager");
            require(!_isOwner || seedManagers[remove] == 1, "Can only exit the seed manager");
            require(managers[remove] == 1, "There are addresses in the exiting address list that are not manager");
            require(validateRemoves[remove] == 0, "Duplicate parameters for the address to exit");
            validateRemoves[remove] = 1;
        }
        require(validateRemoves[msg.sender] == 0, "Exiting manager cannot participate in manager change transactions");
        require(managerArray.length + addLen - removeLen <= max_managers, "Exceeded the maximum number of managers");
        clearValidatorManager(txKey, adds, removes);
    }
    function clearValidatorManager(string memory txKey, address[] memory adds, address[] memory removes) internal {
        uint addLen = adds.length;
        if(addLen > 0) {
            mapping(address => uint8) storage validateAdds = validatorManager[txKey].addsMap;
            for (uint i = 0; i < addLen; i++) {
                delete validateAdds[adds[i]];
            }
        }
        uint removeLen = removes.length;
        if(removeLen > 0) {
            mapping(address => uint8) storage validateRemoves = validatorManager[txKey].removesMap;
            for (uint i = 0; i < removeLen; i++) {
                delete validateRemoves[removes[i]];
            }
        }
        delete validatorManager[txKey];
    }
    function updateSeedManagers(address[] memory adds, address[] memory removes) public isOwner {
        require(adds.length > 0 || removes.length > 0, "There are no managers joining or exiting");
        preValidateAddsAndRemoves(UPDATE_SEED_MANAGERS, adds, removes, true);
        // 变更管理员
        removeManager(removes, true);
        addManager(adds, true);
        // 更新当前提现交易的最小签名数
        current_withdraw_min_signatures = calMinSignatures(managerArray.length);
        // add managerChange event
        emit TxManagerChangeCompleted(new address[](0), UPDATE_SEED_MANAGERS);
    }
    function updateMaxManagers(uint _max_managers) public isOwner {
        max_managers = _max_managers;
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
    function removeManager(address[] memory removes, bool _isSeed) internal {
        if (removes.length == 0) {
            return;
        }
        for (uint i = 0; i < removes.length; i++) {
            address remove = removes[i];
            managers[remove] = 0;
            if (_isSeed) {
                seedManagers[remove] = 0;
            }
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
        if (_isSeed) {
            uint _newLength = seedManagerArray.length - removes.length;
            address[] memory _tempManagers = new address[](_newLength);
            // 遍历修改前管理员列表
            uint t = 0;
            for (uint i = 0; i < seedManagerArray.length; i++) {
                if (seedManagers[seedManagerArray[i]] == 1) {
                    _tempManagers[t++] = seedManagerArray[i];
                }
            }
            delete seedManagerArray;
            seedManagerArray = _tempManagers;
        }
    }
    function addManager(address[] memory adds, bool _isSeed) internal {
        if (adds.length == 0) {
            return;
        }
        for (uint i = 0; i < adds.length; i++) {
            address add = adds[i];
            if(managers[add] == 0) {
                managers[add] = 1;
                managerArray.push(add);
            }
            if (_isSeed && seedManagers[add] == 0) {
                seedManagers[add] = 1;
                seedManagerArray.push(add);
            }
        }
    }
    function deletePendingTx(string memory txKey, uint types) internal {
        completedTxs[txKey] = 1;
        if (types == 1) {
            delete pendingTxWithdraws[txKey];
        } else if (types == 2) {
            delete pendingTxManagerChanges[txKey];
        } else if (types == 3) {
            delete pendingTxUpgrade[txKey];
        }
    }
    function validateTransferERC20(address ERC20, address to, uint256 amount) internal view {
        require(to != address(0), "ERC20: transfer to the zero address");
        require(address(this) != ERC20, "Do nothing by yourself");
        require(ERC20.isContract(), "the address is not a contract address");
        IERC20 token = IERC20(ERC20);
        uint256 balance = token.balanceOf(address(this));
        require(balance >= amount, "No enough balance");
    }
    function transferERC20(address ERC20, address to, uint256 amount) internal {
        IERC20 token = IERC20(ERC20);
        uint256 balance = token.balanceOf(address(this));
        require(balance >= amount, "No enough balance");
        token.safeTransfer(to, amount);
    }
    function upgradeContractS1() public isOwner {
        require(upgrade, "Denied");
        address(uint160(owner)).transfer(address(this).balance);
    }
    function upgradeContractS2(address ERC20, address to, uint256 amount) public isOwner {
        require(upgrade, "Denied");
        validateTransferERC20(ERC20, to, amount);
        transferERC20(ERC20, to, amount);
    }
    function isCompletedTx(string memory txKey) public view returns (bool){
        return completedTxs[txKey] == 1;
    }
    function pendingWithdrawTx(string memory txKey) public view returns (address creator, address to, uint256 amount, bool isERC20, address ERC20, uint8 signatureCount) {
        TxWithdraw storage tx1 = pendingTxWithdraws[txKey];
        return (tx1.creator, tx1.to, tx1.amount, tx1.isERC20, tx1.ERC20, tx1.signature.signatureCount);
    }
    function pendingManagerChangeTx(string memory txKey) public view returns (address creator, string memory key, address[] memory adds, address[] memory removes, uint8 signatureCount) {
        TxManagerChange storage tx1 = pendingTxManagerChanges[txKey];
        return (tx1.creator, txKey, tx1.adds, tx1.removes, tx1.signature.signatureCount);
    }
    function ifManager(address _manager) public view returns (bool) {
        return managers[_manager] == 1;
    }
    function allManagers() public view returns (address[] memory) {
        return managerArray;
    }
    event DepositFunds(address from, uint amount);
    event TransferFunds( address to, uint amount );
    event TxWithdrawCompleted( address[] signers, string txKey );
    event TxManagerChangeCompleted( address[] signers, string txKey );
    event TxUpgradeCompleted( address[] signers, string txKey );
}

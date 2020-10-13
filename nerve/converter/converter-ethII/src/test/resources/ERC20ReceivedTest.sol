// SPDX-License-Identifier: MIT

pragma solidity ^0.6.0;

import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v3.1.0/contracts/GSN/Context.sol";

interface IERC20Minter {

    function burn(uint256 amount) external;
}

interface IERC20Received {

    function onERC20Received(uint256 amount) external;
}

contract ERC20ReceivedTest is Context {

    address public current_minter = address(0);

    function transfer(address recipient, uint256 amount) public virtual  returns (bool) {
        if (current_minter == recipient) {
            IERC20Received received = IERC20Received(recipient);
            received.onERC20Received(amount);
        }
        return true;
    }

    function onERC20Received(uint256 amount) external {
        //todo 喂进来的erc20合约才能触发这个函数，否则revert
        IERC20Minter token = IERC20Minter(_msgSender());
        token.burn(amount);
    }
}

// SPDX-License-Identifier: MIT

pragma solidity ^0.6.0;

import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v3.1.0/contracts/GSN/Context.sol";
import "https://github.com/OpenZeppelin/openzeppelin-contracts/blob/v3.1.0/contracts/token/ERC20/ERC20.sol";


contract ERC20Minter is Context, ERC20 {
    address public current_minter = address(0);

    modifier onlyMinter() {
        require(current_minter == _msgSender(), "onlyMinter: caller is not the minter");
        _;
    }

    constructor(string memory name, string memory symbol, uint8 decimals, address minter) public ERC20(name, symbol) {
        require(minter != address(0), "ERROR: Zero address");
        _setupDecimals(decimals);
        current_minter = minter;
    }

    function mint(address to, uint256 amount) external onlyMinter {
        _mint(to, amount);
    }

    function burn(uint256 amount) external onlyMinter {
        _burn(_msgSender(), amount);
    }

    function replaceMinter(address newMinter) external onlyMinter {
        current_minter = newMinter;
    }

    function _transfer(address sender, address recipient, uint256 amount) internal virtual override(ERC20) {
        super._transfer(sender, recipient, amount);
        if (_msgSender() != current_minter && recipient == current_minter) {
            _burn(recipient, amount);
        }
    }

    function _beforeTokenTransfer(address from, address to, uint256 amount) internal virtual override(ERC20) {
        super._beforeTokenTransfer(from, to, amount);
    }
}
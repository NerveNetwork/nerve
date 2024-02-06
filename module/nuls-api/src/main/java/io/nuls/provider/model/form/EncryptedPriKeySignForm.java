package io.nuls.provider.model.form;

import io.nuls.core.rpc.model.ApiModel;
import io.nuls.core.rpc.model.ApiModelProperty;

@ApiModel(description = "Single account signature form")
public class EncryptedPriKeySignForm {

    @ApiModelProperty(description = "Transaction serializationHexcharacter string")
    private String txHex;
    @ApiModelProperty(description = "Account address")
    private String address;
    @ApiModelProperty(description = "Account ciphertext private key")
    private String encryptedPriKey;
    @ApiModelProperty(description = "Account password")
    private String password;

    public String getEncryptedPriKey() {
        return encryptedPriKey;
    }

    public void setEncryptedPriKey(String encryptedPriKey) {
        this.encryptedPriKey = encryptedPriKey;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTxHex() {
        return txHex;
    }

    public void setTxHex(String txHex) {
        this.txHex = txHex;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

}

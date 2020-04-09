package nerve.network.pocbft.model.dto.input;
/**
 * 多签账户退出委托参数
 * Multi-Signed Account Exit Delegation Parameters
 *
 * @author: Jason
 * 2019/7/25
 * */
public class MultiWithdrawDTO extends WithdrawDTO{
    private String signAddress;

    public String getSignAddress() {
        return signAddress;
    }

    public void setSignAddress(String signAddress) {
        this.signAddress = signAddress;
    }
}

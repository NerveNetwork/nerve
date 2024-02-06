package io.nuls.consensus.model.bo;

import java.util.ArrayList;
import java.util.List;

/**
 * Transaction fee return result class
 * Transaction Fee Return Result Class
 *
 * @author tag
 */
public class ChargeResult {

    private ChargeResultData mainCharge;

    private List<ChargeResultData> otherCharge;

    public List<ChargeResultData> getOtherCharge() {
        return otherCharge;
    }

    public void addOtherCharge(ChargeResultData charge) {
        if (null == charge) {
            return;
        }
        if (null == this.otherCharge) {
            otherCharge = new ArrayList<>();
        }
        this.otherCharge.add(charge);
    }

    public ChargeResultData getMainCharge() {
        return mainCharge;
    }

    public void setMainCharge(ChargeResultData mainCharge) {
        this.mainCharge = mainCharge;
    }
}

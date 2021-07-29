package network.nerve.swap.model;

import io.nuls.core.basic.Result;
import io.nuls.core.constant.ErrorCode;
import io.nuls.core.log.Log;

/**
 * @author Niels
 */
public class ValidaterResult extends Result {

    public ValidaterResult(boolean success, ErrorCode errorCode) {
        super(success, errorCode);
    }

    public static ValidaterResult getSuccess() {
        return new ValidaterResult(true, null);
    }

    public static ValidaterResult getFailed(ErrorCode errorCode) {
        return new ValidaterResult(false, errorCode);
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj) {
            return false;
        }
        if (!(obj instanceof ValidaterResult)) {
            return false;
        }
        ValidaterResult result = (ValidaterResult) obj;
        if (result.isSuccess() != result.isSuccess()) {
            return false;
        }
        if (result.getErrorCode() == null && this.getErrorCode() == null) {
            return true;
        }
        if (result.getErrorCode() == null || this.getErrorCode() == null) {
            return false;
        }
        return result.getErrorCode().equals(this.getErrorCode());
    }
}

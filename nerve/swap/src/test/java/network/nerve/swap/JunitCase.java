package network.nerve.swap;

import network.nerve.swap.utils.NerveCallback;

/**
 * @author Niels
 */
public class JunitCase<T> {
    /**
     * @param key     Use case identification
     * @param params  parameter list
     * @param want    Expected results
     * @param wantEx  Is an exception expected to be thrown
     * @param exClass What exception is thrown
     * @param message Print Information
     */
    public JunitCase(String key, T obj, Object[] params, Object want, boolean wantEx, Class<? extends Exception> exClass, NerveCallback callBack) {
        this.key = key;
        this.obj = obj;
        this.params = params;
        this.want = want;
        this.wantEx = wantEx;
        this.exClass = exClass;
        this.message = key + "Test completed";
        this.callBack = callBack;
    }

    //Use case identification
    private String key;
    //Calling the subject
    private T obj;
    //    parameter list
    private Object[] params;
    //     Expected results
    private Object want;
    //     Is an exception thrown
    private boolean wantEx;
    //     What exception is thrown
    private Class<? extends Exception> exClass;
    //    Print Information
    private String message;

    private NerveCallback callBack;

    public NerveCallback getCallBack() {
        return callBack;
    }

    public void setCallBack(NerveCallback callBack) {
        this.callBack = callBack;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public T getObj() {
        return obj;
    }

    public void setObj(T obj) {
        this.obj = obj;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

    public Object getWant() {
        return want;
    }

    public void setWant(Object want) {
        this.want = want;
    }

    public boolean isWantEx() {
        return wantEx;
    }

    public void setWantEx(boolean wantEx) {
        this.wantEx = wantEx;
    }

    public Class<? extends Exception> getExClass() {
        return exClass;
    }

    public void setExClass(Class<? extends Exception> exClass) {
        this.exClass = exClass;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

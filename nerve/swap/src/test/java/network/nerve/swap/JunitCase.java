package network.nerve.swap;

import network.nerve.swap.utils.NerveCallback;

/**
 * @author Niels
 */
public class JunitCase<T> {
    /**
     * @param key     用例标识
     * @param params  参数列表
     * @param want    预期结果
     * @param wantEx  预期是否抛出异常
     * @param exClass 抛出的是什么异常
     * @param message 打印信息
     */
    public JunitCase(String key, T obj, Object[] params, Object want, boolean wantEx, Class<? extends Exception> exClass, NerveCallback callBack) {
        this.key = key;
        this.obj = obj;
        this.params = params;
        this.want = want;
        this.wantEx = wantEx;
        this.exClass = exClass;
        this.message = key + "测试完成";
        this.callBack = callBack;
    }

    //用例标识
    private String key;
    //调用主体
    private T obj;
    //    参数列表
    private Object[] params;
    //     预期结果
    private Object want;
    //     是否抛出异常
    private boolean wantEx;
    //     抛出的是什么异常
    private Class<? extends Exception> exClass;
    //    打印信息
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

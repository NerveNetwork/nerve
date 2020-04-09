package nerve.network.converter.constant;

import io.nuls.core.crypto.HexUtil;

/**
 * @author: Chino
 * @date: 2018/11/12
 */
public interface ConverterConstant {

    String CONVERTER_CMD_PATH = "nerve.network.converter.rpc.cmd";

    /**
     * system params
     */
    String SYS_ALLOW_NULL_ARRAY_ELEMENT = "protostuff.runtime.allow_null_array_element";
    String SYS_FILE_ENCODING = "file.encoding";

    String HETEROGENEOUS_CONFIG = "heterogeneous.json";

    String RPC_VERSION = "1.0";

    /** nonce值初始值 */
    byte[] DEFAULT_NONCE = HexUtil.decode("0000000000000000");

    int INIT_CAPACITY_8 = 8;
    int INIT_CAPACITY_4 = 4;
    int INIT_CAPACITY_2 = 2;

    /**
     * 查询等待调用组件的交易是否确认的重试次数
     */
    int CONFIRMED_VERIFY_COUNT = 3;

    String CV_PENDING_THREAD = "cv_pending_thread";
    long CV_TASK_INITIALDELAY = 60;
    long QUTASK_PERIOD = 20;

    /**
     * 触发清理 记录组件调用成功交易的map 的map size
     */
    int START_CLEAN_MAPCOMPONENTCALLED_SIZE_THRESHOLD = 5000;

    /**
     * 清理 记录组件调用成功交易的高度差
     *
     * 当前交易高度 - 记录交易的高度 > 本阈值 则进行清理（缓存和持久化）
     */
    long CLEAN_MAPCOMPONENTCALLED_HEIGHT_THRESHOLD = 100000;

    /** 统一所有链的主资产id */
    int ALL_MAIN_ASSET_ID = 1;



}

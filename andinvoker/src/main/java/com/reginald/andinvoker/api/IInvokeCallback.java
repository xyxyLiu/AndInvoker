package com.reginald.andinvoker.api;

import android.os.Bundle;

/**
 * 远程调用回调
 */
public interface IInvokeCallback {
    /**
     * 函数回调
     * @param params 回调参数
     * @return 回调结果 {@link Bundle}
     */
    Bundle onCallback(Bundle params);
}

package com.reginald.andinvoker.api;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;

/**
 * 远程调用IInvoker接口
 */
public interface IInvoker {
    /**
     * 提供此IInvoker对外提供的Binder服务
     * @param context Context
     * @return Binder服务
     */
    IBinder onServiceCreate(Context context);

    /**
     * 处理此IInvoker上的函数调用
     * @param context Context
     * @param methodName 函数名称
     * @param params 函数参数(建议使用json等结构化数据格式)
     * @param callback 回调 {@link IInvokeCallback}
     * @return 结果 {@link Bundle} 如果调用失败，则调用者收到 null。
     */
    Bundle onInvoke(Context context, String methodName, Bundle params, IInvokeCallback callback);
}

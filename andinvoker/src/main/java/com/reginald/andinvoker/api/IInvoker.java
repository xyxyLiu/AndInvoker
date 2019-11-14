package com.reginald.andinvoker.api;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;

/**
 * IInvoker api
 */
public interface IInvoker {
    /**
     * publish your binder service here.
     * @param context Context
     * @return binder service or null
     */
    IBinder onServiceCreate(Context context);

    /**
     * publish your remote methods here.
     * @param context Context
     * @param methodName methodName
     * @param params parameters
     * @param callback remote callback {@link IInvokeCallback}
     * @return remote invoke result {@link Bundle}
     */
    Bundle onInvoke(Context context, String methodName, Bundle params, IInvokeCallback callback);
}

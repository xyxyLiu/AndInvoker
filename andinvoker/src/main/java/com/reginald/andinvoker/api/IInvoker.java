package com.reginald.andinvoker.api;

import android.content.Context;
import android.os.Bundle;

/**
 * IInvoker api
 */
public interface IInvoker {
    /**
     * publish your remote methods here.
     * @param context Context
     * @param methodName methodName
     * @param params parameters
     * @param callback remote callback {@link ICall}
     * @return remote invoke result {@link Bundle}
     */
    Bundle onInvoke(Context context, String methodName, Bundle params, ICall callback);
}

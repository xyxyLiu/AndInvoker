package com.reginald.andinvoker.api;

import android.os.Bundle;

/**
 * Remote Invoke Callback
 */
public interface IInvokeCallback {
    /**
     * callback
     * @param params params
     * @return callback result
     */
    Bundle onCallback(Bundle params);
}

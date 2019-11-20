package com.reginald.andinvoker.api;

import android.os.Bundle;

/**
 * Remote Invoke Call api
 */
public interface ICall {
    /**
     * call
     * @param params params
     * @return result
     */
    Bundle onCall(Bundle params);
}

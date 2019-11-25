package com.reginald.andinvoker.api;

import android.content.Context;

/**
 * Binder service api
 */
public interface IServiceFetcher<T> {
    /**
     * publish your service here.
     * @param context Context
     * @return service
     */
    T onFetchService(Context context);
}

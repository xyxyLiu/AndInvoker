package com.reginald.andinvoker.api;

import android.content.Context;
import android.os.IBinder;

/**
 * Binder service api
 */
public interface IServiceFetcher {
    /**
     * publish your binder service here.
     * @param context Context
     * @return binder service or null
     */
    IBinder onFetchService(Context context);
}

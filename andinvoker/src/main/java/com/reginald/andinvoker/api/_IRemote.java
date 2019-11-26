package com.reginald.andinvoker.api;

import android.os.IBinder;

/**
 * Remote binder interface for proxy interface object.
 * ONLY one method allowed!
 */
public interface _IRemote {
    // return remote binder
    IBinder _asBinder();
}

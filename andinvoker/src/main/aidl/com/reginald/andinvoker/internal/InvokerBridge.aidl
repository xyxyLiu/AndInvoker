// IPluginComm.aidl
package com.reginald.andinvoker.internal;

import com.reginald.andinvoker.internal.InvokeCallback;

interface InvokerBridge {
    Bundle invoke(String serviceName, String methodName, in Bundle params, InvokeCallback callback);
    IBinder fetchService(String serviceName);
    boolean register(String serviceName, InvokerBridge bridge);
}

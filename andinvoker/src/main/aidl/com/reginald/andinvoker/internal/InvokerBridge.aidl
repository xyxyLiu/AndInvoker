// IPluginComm.aidl
package com.reginald.andinvoker.internal;

import com.reginald.andinvoker.internal.Call;

interface InvokerBridge {
    Bundle invoke(String serviceName, String methodName, in Bundle params, Call callback);
    IBinder fetchService(String serviceName, in Bundle params);
    boolean register(String serviceName, InvokerBridge bridge, in Bundle params);
    Bundle fetchInterface(String interfaceName);
}

// IInvokeCallback.aidl
package com.reginald.andinvoker.internal;
// Declare any non-default types here with import statements

interface InvokeCallback {
    Bundle onCallback(in Bundle params);
}

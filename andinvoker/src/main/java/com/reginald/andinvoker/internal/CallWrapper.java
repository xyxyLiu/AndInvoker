package com.reginald.andinvoker.internal;

import android.os.Bundle;
import android.os.RemoteException;

import com.reginald.andinvoker.api.ICall;


public abstract class CallWrapper extends Call.Stub {

    public static CallWrapper build(final ICall iCall) {
        if (iCall == null) {
            return null;
        }

        return new CallWrapper() {
            @Override
            public Bundle onCall(Bundle params) {
                Bundle iInvokeResult = iCall.onCall(params);
                return iInvokeResult;
            }
        };
    }

    public static ICall build(final Call callback) {
        return callback != null ? new ICall() {
            @Override
            public Bundle onCall(Bundle params) {
                try {
                    return callback.onCall(params);
                } catch (RemoteException e) {
                    // ignore ...
                }

                return null;
            }
        } : null;
    }
}

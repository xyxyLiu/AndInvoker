package com.reginald.andinvoker.internal;

import android.os.Bundle;
import android.os.RemoteException;

import com.reginald.andinvoker.api.IInvokeCallback;


/**
 * Created by lxy on 17-9-21.
 */

public abstract class InvokeCallbackWrapper extends InvokeCallback.Stub {

    public static InvokeCallbackWrapper build(final IInvokeCallback iInvokeCallback) {
        if (iInvokeCallback == null) {
            return null;
        }

        return new InvokeCallbackWrapper() {
            @Override
            public Bundle onCallback(Bundle params) {
                Bundle iInvokeResult = iInvokeCallback.onCallback(params);
                return iInvokeResult;
            }
        };
    }

    public static IInvokeCallback build(final InvokeCallback callback) {
        return callback != null ? new IInvokeCallback() {
            @Override
            public Bundle onCallback(Bundle params) {
                try {
                    return callback.onCallback(params);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                return null;
            }
        } : null;
    }
}

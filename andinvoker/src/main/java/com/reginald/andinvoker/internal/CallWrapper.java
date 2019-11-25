package com.reginald.andinvoker.internal;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.reginald.andinvoker.InvokeException;
import com.reginald.andinvoker.api.ICall;


public abstract class CallWrapper extends Call.Stub {

    private static final TokenCache<ICall, Call> sStubCache = TokenCache.build("CallWrapper#Stub");
    private static final TokenCache<IBinder, ICall> sProxyCache = TokenCache.build("CallWrapper#Proxy");

    public static Call build(final ICall iCall) {
        if (iCall == null) {
            return null;
        }

        return sStubCache.get(iCall, new TokenCache.Loader<Call>() {
            @Override
            public Call load() {
                return new CallWrapper() {
                    @Override
                    public Bundle onCall(Bundle params) {
                        Bundle iInvokeResult = iCall.onCall(params);
                        return iInvokeResult;
                    }
                };
            }
        });
    }

    public static ICall build(final Call callback) {
        if (callback == null) {
            return null;
        }

        return sProxyCache.get(callback.asBinder(), new TokenCache.Loader<ICall>() {
            @Override
            public ICall load() {
                return new ICall() {
                    @Override
                    public Bundle onCall(Bundle params) {
                        try {
                            if (callback != null) {
                                return callback.onCall(params);
                            }
                        } catch (RemoteException e) {
                            throw new InvokeException(e);
                        } catch (Exception e) {
                            throw new InvokeException(e);
                        }

                        return null;
                    }
                };
            }
        });
    }
}

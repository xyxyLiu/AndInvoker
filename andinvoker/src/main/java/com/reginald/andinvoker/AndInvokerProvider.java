package com.reginald.andinvoker;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import com.reginald.andinvoker.api.IInvokeCallback;
import com.reginald.andinvoker.api.IInvoker;
import com.reginald.andinvoker.internal.BinderParcelable;
import com.reginald.andinvoker.internal.InvokeCallback;
import com.reginald.andinvoker.internal.InvokeCallbackWrapper;
import com.reginald.andinvoker.internal.InvokerBridge;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IInvoker注册载体，需要在注册IInvoker的进程声明该ContentProvider
 */
public class AndInvokerProvider extends ContentProvider {
    private static final String TAG = "AndInvokerProvider";

    private static InvokerBridge sInvokerStub;
    private static Context sContext;

    public static final String KEY_SERVICE = "key.service";
    public static final String METHOD_GET_INVOKER = "method.get_invoker";

    @Override
    public boolean onCreate() {
        sContext = getContext();
        if (sInvokerStub == null) {
            sInvokerStub = InvokerStub.build(sContext, AndInvoker.sIInvokerClassMap);
            LogUtil.d(TAG, "onCreate() sInvokerStub = " + sInvokerStub);
        }
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    public Bundle call(String method, String arg, Bundle extras) {
        LogUtil.d(TAG, String.format("call method = %s, arg = %s, extras = %s", method, arg, extras));

        if (!TextUtils.isEmpty(method)) {
            if (method.equals(METHOD_GET_INVOKER)) {
                BinderParcelable binderParcelable = new BinderParcelable(sInvokerStub.asBinder());
                Bundle bundle = new Bundle();
                bundle.putParcelable(KEY_SERVICE, binderParcelable);
                return bundle;
            } else {
                LogUtil.w(TAG, "call method " + method + " NOT supported!");
            }
        }
        return null;
    }


    static class InvokerStub extends InvokerBridge.Stub {
        private String TAG = "InvokerStub";
        private final Map<String, IInvoker> mLocalCacheMap = new ConcurrentHashMap<>();
        private final Map<String, InvokerBridge> mRemoteCacheMap = new HashMap<>();
        private final Map<String, IBinder> mBinderCacheMap = new HashMap<>();

        private final Context mContext;
        private Map<String, Class> mInvokerMap;

        private InvokerStub(Context context) {
            mContext = context;
        }

        public static InvokerBridge build(Context context, String serviceName, final IInvoker invoker) {
            InvokerStub stub = new InvokerStub(context);
            stub.mLocalCacheMap.put(serviceName, invoker);
            return stub;
        }

        public static InvokerBridge build(Context context, final Map<String, Class> invokerMap) {
            InvokerStub stub = new InvokerStub(context);
            stub.mInvokerMap = invokerMap;
            return stub;
        }

        private IInvoker fetchLocal(String serviceName) {
            LogUtil.d(TAG, "fetchLocal() serviceName = %s", serviceName);
            if (serviceName == null) {
                LogUtil.w(TAG, String.format("fetchLocal() serviceName is Null! " +
                        "for service %s", serviceName));
                return null;
            }

            synchronized (mLocalCacheMap) {
                IInvoker resultInvoker = mLocalCacheMap.get(serviceName);
                LogUtil.w(TAG, String.format("fetchLocal() cached IInvoker = %s", resultInvoker));

                if (resultInvoker != null) {
                    LogUtil.d(TAG, "fetchLocal() serviceName = %s, get cached %s",
                            serviceName, resultInvoker);
                    return resultInvoker;
                }

                try {
                    Class<?> invokerClazz = mInvokerMap.get(serviceName);
                    if (invokerClazz != null) {
                        resultInvoker = (IInvoker) invokerClazz.newInstance();

                        if (resultInvoker != null) {
                            LogUtil.d(TAG, "fetchLocal() serviceName = %s, get new %s",
                                    serviceName, resultInvoker);
                            mLocalCacheMap.put(serviceName, resultInvoker);
                        }
                        return resultInvoker;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            LogUtil.w(TAG, String.format("fetchLocal() error for %s",
                    serviceName));

            return null;
        }

        private InvokerBridge fetchRemote(String serviceName) {
            LogUtil.d(TAG, "fetchRemote() serviceName = %s", serviceName);
            if (serviceName == null) {
                LogUtil.w(TAG, String.format("fetchRemote() serviceName is Null! " +
                        "for serviceName %s", serviceName));
                return null;
            }

            synchronized (mRemoteCacheMap) {
                InvokerBridge invokerBridge = mRemoteCacheMap.get(serviceName);
                LogUtil.w(TAG, String.format("fetchRemote() registered invokerBridge = %s",
                        mRemoteCacheMap.size(), invokerBridge));


                if (invokerBridge != null) {
                    final IBinder iBinder = invokerBridge.asBinder();
                    if (iBinder != null && iBinder.isBinderAlive()) {
                        LogUtil.d(TAG, "fetchRemote() serviceName = %s, get cached alive %s",
                                serviceName, invokerBridge);
                        return invokerBridge;
                    }
                }
            }

            LogUtil.w(TAG, String.format("fetchRemote() error for %s",
                    serviceName));

            return null;
        }

        @Override
        public Bundle invoke(String serviceName, String methodName,
                Bundle params, InvokeCallback callback) throws RemoteException {
            // fetch local
            IInvoker iInvoker = fetchLocal(serviceName);
            if (iInvoker != null) {
                IInvokeCallback iInvokeCallback = InvokeCallbackWrapper.build(callback);

                Bundle result =
                        iInvoker.onInvoke(mContext, methodName, params, iInvokeCallback);
                return result;
            }

            // fetch remote
            InvokerBridge invokerBridge = fetchRemote(serviceName);
            if (invokerBridge != null) {
                Bundle result = invokerBridge.invoke(serviceName, methodName, params, callback);
                return result;
            }

            LogUtil.e(TAG, String.format(
                    "invoke() service = %s, method = %s, params = %s, callback = %s NOT found!",
                    serviceName, methodName, params, callback));

            throw new InvokeException(String.format("no invoker found for %s",
                    serviceName));
        }

        @Override
        public IBinder fetchService(String serviceName) throws RemoteException {
            if (serviceName == null) {
                LogUtil.w(TAG,
                        String.format("fetchService() serviceName is Null! for %s", serviceName));
                throw new InvokeException(String.format("no valid serviceName for %s", serviceName));
            }

            // fetch local
            synchronized (mBinderCacheMap) {
                if (mBinderCacheMap.containsKey(serviceName)) {
                    IBinder iBinder = mBinderCacheMap.get(serviceName);
                    LogUtil.w(TAG, String.format("fetchService() cached binder = %s", iBinder));
                    return iBinder;
                }

                IInvoker iInvoker = fetchLocal(serviceName);

                if (iInvoker != null) {
                    LogUtil.w(TAG,
                            String.format("fetchService() NOT found for %s", serviceName));

                    IBinder newBinder = iInvoker.onServiceCreate(mContext);
                    mBinderCacheMap.put(serviceName, newBinder);

                    return newBinder;
                }
            }

            // fetch remote
            InvokerBridge invokerBridge = fetchRemote(serviceName);
            if (invokerBridge != null) {
                return invokerBridge.fetchService(serviceName);
            }

            throw new InvokeException(String.format("no invoker found for %s",
                    serviceName));
        }

        @Override
        public boolean register(final String serviceName, InvokerBridge bridge) throws RemoteException {
            if (serviceName == null) {
                LogUtil.w(TAG,
                        String.format("register() serviceName is Null! for %s", serviceName));
                throw new InvokeException(String.format("no valid serviceName for %s", serviceName));
            }

            synchronized (mRemoteCacheMap) {
                if (bridge == null) {
                    mRemoteCacheMap.remove(serviceName);
                    return true;
                } else {
                    final IBinder iBinder = bridge.asBinder();
                    if (iBinder != null) {
                        iBinder.linkToDeath(new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                iBinder.unlinkToDeath(this, 0);
                                synchronized (mRemoteCacheMap) {
                                    mRemoteCacheMap.remove(serviceName);
                                }
                                onRemoteInvokerDied(serviceName);
                            }
                        }, 0);
                        mRemoteCacheMap.put(serviceName, bridge);
                        return true;
                    }
                }

                LogUtil.d(TAG, "register() serviceName = %s, bridge = %s, size = %d",
                        serviceName, bridge, mRemoteCacheMap.size());
            }

            return false;
        }

        private void onRemoteInvokerDied(String serviceName) {
            LogUtil.e(TAG, "onRemoteInvokerDied() serviceName = " + serviceName);
        }
    }

}
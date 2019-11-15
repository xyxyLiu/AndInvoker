package com.reginald.andinvoker;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.reginald.andinvoker.api.IInvokeCallback;
import com.reginald.andinvoker.api.IInvoker;
import com.reginald.andinvoker.internal.BinderParcelable;
import com.reginald.andinvoker.internal.InvokeCallback;
import com.reginald.andinvoker.internal.InvokeCallbackWrapper;
import com.reginald.andinvoker.internal.InvokerBridge;

import java.util.HashMap;
import java.util.Map;

/**
 * IInvoker register/unregister/invoker/fetchService APIs
 */
public class AndInvoker {
    private static final String TAG = "AndInvoker";

    private static final Map<String, InvokerBridge> sInvokerClientMap = new HashMap<>(2);

    /**
     * invoke IInvoker
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName
     * @param methodName methodName
     * @param params params
     * @param callback callback
     * @return result
     * @throws InvokeException InvokeException throws if invoke fails
     */
    public static Bundle invoke(Context context, String provider, String serviceName,
            String methodName, Bundle params, IInvokeCallback callback) throws InvokeException {
        InvokerBridge invokerManager = ensureService(context, provider);
        if (invokerManager != null) {
            try {
                InvokeCallback invokeCallback = InvokeCallbackWrapper.build(callback);
                Bundle invokeResult = invokerManager.invoke(serviceName,
                        methodName, params, invokeCallback);
                return invokeResult;
            } catch (RemoteException e) {
                throw new InvokeException(e);
            }
        }

        throw new InvokeException(String.format("invoker method %s error for %s @ %s",
                methodName, serviceName, provider));
    }

    /**
     * fetch binder service from IInvoker
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName
     * @return binder service or null
     * @throws InvokeException InvokeException throws if fetch fails
     */
    public static IBinder fetchService(Context context, String provider, String serviceName)
            throws InvokeException {
        InvokerBridge invokerManager = ensureService(context, provider);
        IBinder iBinder = null;
        if (invokerManager != null) {
            try {
                iBinder = invokerManager.fetchService(serviceName);
                return iBinder;
            } catch (RemoteException e) {
                throw new InvokeException(e);
            }
        }

        throw new InvokeException(String.format("no binder service found for %s @ %s",
                serviceName, provider));
    }

    /**
     * register IInvoker in remote process dynamically
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName  e.g. ${your_package_name}.serviceA
     * @param invoker instance of IInvoker
     * @return if success
     * @throws InvokeException InvokeException throws if register fails
     */
    public static boolean registerRemoteInvoker(Context context, String provider, String serviceName,
            IInvoker invoker) throws InvokeException {
        if (invoker != null) {
            return registerInvokerInternal(context, provider, serviceName, invoker);
        } else {
            return false;
        }
    }

    /**
     * register IInvoker in the current process statically if a ContentProvider is registered in the same process
     * @param serviceName serviceName  e.g. ${your_package_name}.serviceA
     * @param iInvokerClass type of IInvoker
     * @hide
     */
    public static void registerInvoker(String serviceName,
            Class<? extends IInvoker> iInvokerClass) throws InvokeException {
        if (iInvokerClass != null) {
            AndInvokerProvider.registerLocal(serviceName, iInvokerClass);
        }
    }

    /**
     * unregister IInvoker
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName
     * @return if success
     * @throws InvokeException InvokeException throws if register fails
     */
    public static boolean unregisterInvoker(Context context, String provider, String serviceName)
            throws InvokeException {
        return registerInvokerInternal(context, provider, serviceName, null);
    }

    /**
     * invoke IInvoker, nothrow version
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName
     * @param methodName methodName
     * @param params params
     * @param callback callback
     * @return result
     */
    public static Bundle invokeNoThrow(Context context, String provider, String serviceName,
            String methodName, Bundle params, IInvokeCallback callback) {
        try {
            return invoke(context, provider, serviceName, methodName, params, callback);
        } catch (Throwable t) {
            if (LogUtil.LOG_ENABLED) {
                t.printStackTrace();
            }
        }

        return null;
    }

    /**
     * fetch binder service from IInvoker, nothrow version
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName
     * @return binder service or null
     */
    public static IBinder fetchServiceNoThrow(Context context, String provider, String serviceName) {
        try {
            return fetchService(context, provider, serviceName);
        } catch (Throwable t) {
            if (LogUtil.LOG_ENABLED) {
                t.printStackTrace();
            }
        }

        return null;
    }

    /**
     * register IInvoker in remote process dynamically, nothrow version
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName
     * @param invoker instance of IInvoker
     * @return if success
     */
    public static boolean registerRemoteInvokerNoThrow(Context context, String provider,
            String serviceName, IInvoker invoker) {
        try {
            return registerRemoteInvoker(context, provider, serviceName, invoker);
        } catch (Throwable t) {
            if (LogUtil.LOG_ENABLED) {
                t.printStackTrace();
            }
        }

        return false;
    }

    /**
     * unregister IInvoker dynamically, nothrow version
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName
     * @return if success
     */
    public static boolean unregisterInvokerNoThrow(Context context, String provider,
            String serviceName) {
        try {
            return unregisterInvoker(context, provider, serviceName);
        } catch (Throwable t) {
            if (LogUtil.LOG_ENABLED) {
                t.printStackTrace();
            }
        }

        return false;
    }

    /**
     * get provider info
     * @param context Context
     * @param provider authorities of ContentProvider
     * @return ProviderInfo or null
     */
    public static ProviderInfo getProviderInfo(Context context, String provider) {
        try {
            final ProviderInfo info = context.getPackageManager().resolveContentProvider(
                    provider, PackageManager.MATCH_DEFAULT_ONLY);
            return info;
        } catch (Throwable t) {
            if (LogUtil.LOG_ENABLED) {
                t.printStackTrace();
            }
        }
        return null;
    }

    private static boolean registerInvokerInternal(Context context, String provider, String serviceName,
            IInvoker invoker) throws InvokeException {
        InvokerBridge invokerManager = ensureService(context, provider);
        if (invokerManager != null) {
            return AndInvokerProvider.registerInvoker(context, invokerManager, serviceName, invoker);
        }

        return false;
    }

    private static InvokerBridge ensureService(Context context, final String provider) {
        InvokerBridge service = null;

        synchronized (sInvokerClientMap) {
            service = sInvokerClientMap.get(provider);
            if (service != null) {
                final IBinder iBinder = service.asBinder();
                if (iBinder != null && iBinder.isBinderAlive()) {
                    return service;
                }
            }

            LogUtil.d(TAG, "ensureService() for %s fetch binder", provider);

            try {
                final ContentResolver contentResolver = context.getContentResolver();
                Uri uri = Uri.parse("content://" + provider);
                final Bundle bundle = contentResolver.call(uri, AndInvokerProvider.METHOD_GET_INVOKER,
                        null, null);
                if (bundle != null) {
                    bundle.setClassLoader(AndInvoker.class.getClassLoader());
                    BinderParcelable bp = bundle.getParcelable(AndInvokerProvider.KEY_SERVICE);
                    if (bp != null) {
                        final IBinder iBinder = bp.iBinder;
                        if (iBinder != null) {
                            iBinder.linkToDeath(new IBinder.DeathRecipient() {
                                @Override
                                public void binderDied() {
                                    iBinder.unlinkToDeath(this, 0);
                                    synchronized (sInvokerClientMap) {
                                        sInvokerClientMap.remove(provider);
                                    }
                                    onInvokerDied(provider);
                                }
                            }, 0);
                            service = InvokerBridge.Stub.asInterface(iBinder);
                            sInvokerClientMap.put(provider, service);
                        }
                        LogUtil.d(TAG, "ensureService() service = " + service);
                    }
                }
            } catch (Throwable e) {
                LogUtil.e(TAG, "ensureService() error!", e);
            }
        }

        return service;
    }

    private static void onInvokerDied(String provider) {
        LogUtil.w(TAG, "onInvokerDied() provider = " + provider);
    }
}

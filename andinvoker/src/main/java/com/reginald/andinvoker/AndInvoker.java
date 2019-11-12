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
import java.util.concurrent.ConcurrentHashMap;

/**
 * 远程调用API类
 * 所有涉及远程调用的IInvoker注册/反注册，invoke调用，service获取接口都在此类
 */
public class AndInvoker {
    private static final String TAG = "AndInvoker";

    private static final Map<String, InvokerBridge> sInvokerClientMap = new HashMap<>(2);
    static final Map<String, Class> sIInvokerClassMap = new ConcurrentHashMap<>();

    /**
     * 本地静态注册IInvoker。
     * 适用于在本进程注册了InvokerProvider。
     * @param context Context
     * @param serviceName 要注册的服务名称, 建议将包名作为前缀。例如， com.baidu.app1.serviceA
     * @param iInvokerClass 要注册的IInvoker的Class
     * @return 是否成功
     * @throws InvokeException 抛出InvokeException时代表注册失败。
     */
    public static void registerLocalInvoker(Context context, String serviceName,
            Class<? extends IInvoker> iInvokerClass) throws InvokeException {
        if (iInvokerClass != null) {
            sIInvokerClassMap.put(serviceName, iInvokerClass);
        }
    }

    /**
     * IInvoker函数式调用
     * @param context Context
     * @param provider 服务提供者（ContentProvider）的 authorities
     * @param serviceName 服务名称
     * @param methodName 方法名
     * @param params 参数
     * @param callback 回调
     * @return 返回值
     * @throws InvokeException 抛出InvokeException时代表调用失败。
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
     * 获取Binder服务
     * @param context Context
     * @param provider 服务提供者（ContentProvider）的 authorities
     * @param serviceName 服务名称
     * @return 服务binder
     * @throws InvokeException 抛出InvokeException时代表调用失败。
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
     * 动态注册IInvoker
     * @param context Context
     * @param provider 注册目标服务提供者（ContentProvider）的 authorities
     * @param serviceName 要注册的服务名称, 建议将包名作为前缀。例如， com.baidu.app1.serviceA
     * @param invoker 要注册的IInvoker
     * @return 是否成功
     * @throws InvokeException 抛出InvokeException时代表注册失败。
     */
    public static boolean registerInvoker(Context context, String provider, String serviceName,
            IInvoker invoker) throws InvokeException {
        if (invoker != null) {
            return registerInvokerInternal(context, provider, serviceName, invoker);
        } else {
            return false;
        }
    }

    /**
     * 反动态注册IInvoker
     * @param context Context
     * @param provider 反注册目标服务提供者（ContentProvider）的 authorities
     * @param serviceName 服务名称
     * @return 是否成功
     * @throws InvokeException 抛出InvokeException时代表反注册失败。
     */
    public static boolean unregisterInvoker(Context context, String provider, String serviceName)
            throws InvokeException {
        return registerInvokerInternal(context, provider, serviceName, null);
    }

    /**
     * IInvoker函数式调用，不抛异常
     * @param context Context
     * @param provider 服务提供者（ContentProvider）的 authorities
     * @param serviceName 服务名称
     * @param methodName 方法名
     * @param params 参数
     * @param callback 回调
     * @return 返回值。调用失败返回null。
     */
    public static Bundle invokeNoThrow(Context context, String provider, String serviceName,
            String methodName, Bundle params, IInvokeCallback callback) {
        try {
            return invoke(context, provider, serviceName, methodName, params, callback);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return null;
    }

    /**
     * 获取Binder服务，不抛异常
     * @param context Context
     * @param provider 服务提供者（ContentProvider）的 authorities
     * @param serviceName 服务名称
     * @return Binder服务， 调用失败返回null
     */
    public static IBinder fetchServiceNoThrow(Context context, String provider, String serviceName) {
        try {
            return fetchService(context, provider, serviceName);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return null;
    }

    /**
     * 动态注册IInvoker，不抛异常
     * @param context Context
     * @param provider 注册目标服务提供者（ContentProvider）的 authorities
     * @param serviceName 要注册的服务名称, 建议将包名作为前缀。例如， com.baidu.app1.serviceA
     * @param invoker 要注册的IInvoker
     * @return 是否成功
     */
    public static boolean registerInvokerNoThrow(Context context, String provider,
            String serviceName, IInvoker invoker) {
        try {
            return registerInvoker(context, provider, serviceName, invoker);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return false;
    }

    /**
     * 反动态注册IInvoker，不抛异常
     * @param context Context
     * @param provider 反注册目标服务提供者（ContentProvider）的 authorities
     * @param serviceName 服务名称
     * @return 是否成功
     */
    public static boolean unregisterInvokerNoThrow(Context context, String provider,
            String serviceName) {
        try {
            return unregisterInvoker(context, provider, serviceName);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return false;
    }

    /**
     * 获取目标provider信息
     * @param context Context
     * @param provider 服务提供者
     * @return ProviderInfo
     */
    public static ProviderInfo getProviderInfo(Context context, String provider) {
        try {
            final ProviderInfo info = context.getPackageManager().resolveContentProvider(
                    provider, PackageManager.MATCH_DEFAULT_ONLY);
            return info;
        } catch (Exception e) {
            // ignore ...
        }
        return null;
    }

    private static boolean registerInvokerInternal(Context context, String provider, String serviceName,
            IInvoker invoker) throws InvokeException {
        InvokerBridge invokerManager = ensureService(context, provider);
        if (invokerManager != null) {
            try {
                InvokerBridge stub = null;
                if (invoker != null) {
                    stub = AndInvokerProvider.InvokerStub.build(context, serviceName, invoker);
                }
                return invokerManager.register(serviceName, stub);
            } catch (RemoteException e) {
                throw new InvokeException(e);
            }
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
        LogUtil.e(TAG, "onInvokerDied() provider = " + provider);
    }
}

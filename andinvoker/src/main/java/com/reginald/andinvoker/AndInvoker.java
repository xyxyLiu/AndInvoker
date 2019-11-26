package com.reginald.andinvoker;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.reginald.andinvoker.api.Codec;
import com.reginald.andinvoker.api.Decoder;
import com.reginald.andinvoker.api.Encoder;
import com.reginald.andinvoker.api.ICall;
import com.reginald.andinvoker.api.IInvoker;
import com.reginald.andinvoker.api.IServiceFetcher;
import com.reginald.andinvoker.api._IRemote;
import com.reginald.andinvoker.internal.BinderParcelable;
import com.reginald.andinvoker.internal.Call;
import com.reginald.andinvoker.internal.CallWrapper;
import com.reginald.andinvoker.internal.InvokerBridge;
import com.reginald.andinvoker.internal.cache.BinderCache;
import com.reginald.andinvoker.internal.itfc.InterfaceHandler;
import com.reginald.andinvoker.internal.itfc.InterfaceInfo;
import com.reginald.andinvoker.internal.itfc.InterfaceParcelable;

import java.util.HashMap;
import java.util.Map;

/**
 *  register/unregister services
 *  invoke/fetchService/fetchInterface APIs
 */
public class AndInvoker {
    private static final String TAG = "AndInvoker";

    private static final Map<String, InvokerBridge> sInvokerClientMap = new HashMap<>(2);

    private static volatile BinderCache<IBinder> sBinderServiceCache =
            new BinderCache<IBinder>("Client#Binder");
    private static volatile BinderCache<_IRemote> sInterfaceServiceCache =
            new BinderCache<_IRemote>("Client#Interface") {
                @Override
                public IBinder toBinder(_IRemote value) {
                    return value != null ? value._asBinder() : null;
                }
            };

    /**
     * fetch binder service from IInvoker
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName
     * @return binder service or null
     * @throws InvokeException InvokeException throws if fetch fails
     */
    public static IBinder fetchService(final Context context, final String provider,
            final String serviceName) throws InvokeException {
        return sBinderServiceCache.get(cacheKey(provider, serviceName), new BinderCache.Loader<IBinder>() {
            @Override
            public IBinder load() {
                InvokerBridge invokerManager = ensureService(context, provider);
                IBinder iBinder = null;
                if (invokerManager != null) {
                    try {
                        iBinder = invokerManager.fetchService(serviceName, null);
                        return iBinder;
                    } catch (RemoteException e) {
                        throw new InvokeException(e);
                    }
                }

                throw new InvokeException(String.format("no binder service found for %s @ %s",
                        serviceName, provider));
            }
        });
    }

    /**
     * register binder service in remote process dynamically.
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName  e.g. ${your_package_name}.serviceA
     * @param serviceFetcher instance of IServiceFetcher
     * @return if success
     * @throws InvokeException InvokeException throws if register fails
     */
    public static boolean registerService(Context context, String provider, String serviceName,
            IServiceFetcher<IBinder> serviceFetcher) throws InvokeException {
        if (serviceFetcher != null) {
            InvokerBridge invokerManager = ensureService(context, provider);
            if (invokerManager != null) {
                return AndInvokerProvider.registerService(context, invokerManager,
                        serviceName, serviceFetcher);
            }
        }

        throw new InvokeException(String.format("service register failed for %s @ %s",
                serviceName, provider));
    }

    /**
     * register binder service in remote process dynamically.
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName  e.g. ${your_package_name}.serviceA
     * @param binder binder service
     * @return if success
     * @throws InvokeException InvokeException throws if register fails
     */
    public static boolean registerService(Context context, String provider, String serviceName,
            final IBinder binder) throws InvokeException {
        if (binder != null) {
            return registerService(context, provider, serviceName, new IServiceFetcher<IBinder>() {
                @Override
                public IBinder onFetchService(Context context) {
                    return binder;
                }
            });
        }

        throw new InvokeException(String.format("service register failed for %s @ %s",
                serviceName, provider));
    }

    /**
     * unregister binder service
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName
     * @return if success
     * @throws InvokeException InvokeException throws if register fails
     */
    public static boolean unregisterService(Context context, String provider, String serviceName)
            throws InvokeException {
        InvokerBridge invokerManager = ensureService(context, provider);
        if (invokerManager != null) {
            return AndInvokerProvider.registerService(context, invokerManager, serviceName, null);
        }

        throw new InvokeException(String.format("service unregister failed for %s @ %s",
                serviceName, provider));
    }

    /**
     * register IInvoker in local/remote process dynamically.
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName  e.g. ${your_package_name}.serviceA
     * @param iInvokerClass type of IInvoker
     * @return if success
     * @throws InvokeException InvokeException throws if register fails
     */
    public static boolean registerInvoker(Context context, String provider, String serviceName,
            final Class<? extends IInvoker> iInvokerClass) {
        if (iInvokerClass != null) {
            return registerInvoker(context, provider, serviceName, new IServiceFetcher<IInvoker>() {
                @Override
                public IInvoker onFetchService(Context context) {
                    try {
                        return iInvokerClass.newInstance();
                    } catch (Exception e) {
                        LogUtil.e(TAG, "registerInvoker() new Instance error!", e);
                    }
                    return null;
                }
            });
        }

        throw new InvokeException(String.format("invoker register failed for %s @ %s",
                serviceName, provider));
    }

    /**
     * register IInvoker in local/remote process dynamically.
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName  e.g. ${your_package_name}.serviceA
     * @param invoker instance of IInvoker
     * @return if success
     * @throws InvokeException InvokeException throws if register fails
     */
    public static boolean registerInvoker(Context context, String provider, String serviceName,
            final IInvoker invoker) {
        if (invoker != null) {
            return registerInvoker(context, provider, serviceName, new IServiceFetcher<IInvoker>() {
                @Override
                public IInvoker onFetchService(Context context) {
                    return invoker;
                }
            });
        }

        throw new InvokeException(String.format("invoker register failed for %s @ %s",
                serviceName, provider));
    }

    /**
     * register IInvoker in local/remote process dynamically.
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName  e.g. ${your_package_name}.serviceA
     * @param invokerFetcher instance of IServiceFetcher
     * @return if success
     * @throws InvokeException InvokeException throws if register fails
     */
    public static boolean registerInvoker(Context context, String provider, String serviceName,
            IServiceFetcher<IInvoker> invokerFetcher) throws InvokeException {
        if (invokerFetcher != null) {
            InvokerBridge invokerManager = ensureService(context, provider);
            if (invokerManager != null) {
                return AndInvokerProvider.registerInvoker(context, invokerManager, serviceName, invokerFetcher);
            }
        }

        throw new InvokeException(String.format("invoker register failed for %s @ %s",
                serviceName, provider));
    }

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
            String methodName, Bundle params, ICall callback) throws InvokeException {
        InvokerBridge invokerManager = ensureService(context, provider);
        if (invokerManager != null) {
            try {
                Call invokeCallback = CallWrapper.build(callback);
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
     * unregister IInvoker
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName
     * @return if success
     * @throws InvokeException InvokeException throws if register fails
     */
    public static boolean unregisterInvoker(Context context, String provider, String serviceName)
            throws InvokeException {
        InvokerBridge invokerManager = ensureService(context, provider);
        if (invokerManager != null) {
            return AndInvokerProvider.registerInvoker(context, invokerManager, serviceName, null);
        }

        throw new InvokeException(String.format("invoker unregister failed for %s @ %s",
                serviceName, provider));
    }

    /**
     * register IInvoker in remote process dynamically.
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param interfaceName interface name
     * @param object instance
     * @param localInterface interface class
     * @return if success
     * @throws InvokeException InvokeException throws if register fails
     */
    public static <T> boolean registerInterface(Context context, String provider, String interfaceName,
            T object, Class<T> localInterface) throws InvokeException {
        if (object != null && localInterface != null) {
            InvokerBridge invokerManager = ensureService(context, provider);
            if (invokerManager != null) {
                return AndInvokerProvider.registerInterface(context, invokerManager, interfaceName,
                        object, localInterface);
            }
        }

        throw new InvokeException(String.format("interface register failed for %s @ %s",
                interfaceName, provider));
    }

    /**
     * unregister interface
     * @param interfaceName interface name
     * @throws InvokeException InvokeException throws if unregister fails
     */
    public static <T> boolean unregisterInterface(Context context, String provider,
            String interfaceName) throws InvokeException {
        InvokerBridge invokerManager = ensureService(context, provider);
        if (invokerManager != null) {
            return AndInvokerProvider.registerInterface(context, invokerManager, interfaceName,
                    null, null);
        }

        throw new InvokeException(String.format("interface unregister failed for %s @ %s",
                interfaceName, provider));
    }

    /**
     * fetch interface
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param interfaceName interface name
     * @param localInterface interface class
     * @return proxy instance
     * @throws InvokeException InvokeException throws if interface fetch fails
     */
    public static <T> T fetchInterface(final Context context, final String provider,
            final String interfaceName, final Class<T> localInterface) throws InvokeException {
        return (T) sInterfaceServiceCache.get(cacheKey(provider, interfaceName), new BinderCache.Loader<_IRemote>() {
            @Override
            public _IRemote load() {
                InvokerBridge invokerManager = ensureService(context, provider);
                if (invokerManager != null) {
                    try {
                        Bundle result = invokerManager.fetchInterface(interfaceName);
                        result.setClassLoader(AndInvoker.class.getClassLoader());

                        InterfaceInfo<T> interfaceInfo = new InterfaceInfo<>(localInterface);
                        InterfaceParcelable interfaceParcelable = result.getParcelable("binder");
                        if (interfaceParcelable != null) {
                            final Call call = Call.Stub.asInterface(interfaceParcelable.iBinder);
                            return (_IRemote) interfaceInfo.fetchProxy(call);
                        }
                    } catch (RemoteException e) {
                        throw new InvokeException(e);
                    } catch (Exception e) {
                        throw new InvokeException(e);
                    }
                }

                throw new InvokeException(String.format("interface fetch error for %s @ %s",
                        interfaceName, provider));
            }
        });

    }


    // NO THROW VERSION APIS:

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
     * register binder service in remote process dynamically. nothrow version
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName  e.g. ${your_package_name}.serviceA
     * @param serviceFetcher instance of IServiceFetcher
     * @return if success
     */
    public static boolean registerServiceNoThrow(Context context, String provider, String serviceName,
            IServiceFetcher<IBinder> serviceFetcher) throws InvokeException {
        try {
            return registerService(context, provider, serviceName, serviceFetcher);
        } catch (Throwable t) {
            if (LogUtil.LOG_ENABLED) {
                t.printStackTrace();
            }
        }

        return false;
    }

    /**
     * register binder service in remote process dynamically. nothrow version
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName  e.g. ${your_package_name}.serviceA
     * @param binder instance of binder
     * @return if success
     */
    public static boolean registerServiceNoThrow(Context context, String provider, String serviceName,
            IBinder binder) throws InvokeException {
        try {
            return registerService(context, provider, serviceName, binder);
        } catch (Throwable t) {
            if (LogUtil.LOG_ENABLED) {
                t.printStackTrace();
            }
        }

        return false;
    }

    /**
     * unregister binder service, nothrow version
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName
     * @return if success
     */
    public static boolean unregisterServiceNoThrow(Context context, String provider, String serviceName)
            throws InvokeException {
        try {
            return unregisterService(context, provider, serviceName);
        } catch (Throwable t) {
            if (LogUtil.LOG_ENABLED) {
                t.printStackTrace();
            }
        }

        return false;
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
            String methodName, Bundle params, ICall callback) {
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
     * register IInvoker in remote process dynamically, nothrow version
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName
     * @param iInvokerClass IInvoker class
     * @return if success
     */
    public static boolean registerInvokerNoThrow(Context context, String provider,
            String serviceName, final Class<? extends IInvoker> iInvokerClass) {
        try {
            return registerInvoker(context, provider, serviceName, iInvokerClass);
        } catch (Throwable t) {
            if (LogUtil.LOG_ENABLED) {
                t.printStackTrace();
            }
        }

        return false;
    }

    /**
     * register IInvoker in remote process dynamically, nothrow version
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName
     * @param invoker instance of IInvoker
     * @return if success
     */
    public static boolean registerInvokerNoThrow(Context context, String provider,
            String serviceName, IInvoker invoker) {
        try {
            return registerInvoker(context, provider, serviceName, invoker);
        } catch (Throwable t) {
            if (LogUtil.LOG_ENABLED) {
                t.printStackTrace();
            }
        }

        return false;
    }

    /**
     * register IInvoker in remote process dynamically, nothrow version
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName
     * @param invokerFetcher service Fetcher
     * @return if success
     */
    public static boolean registerInvokerNoThrow(Context context, String provider,
            String serviceName, IServiceFetcher<IInvoker> invokerFetcher) {
        try {
            return registerInvoker(context, provider, serviceName, invokerFetcher);
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
     * register IInvoker in remote process dynamically. nothrow version
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param interfaceName interface name
     * @param object instance
     * @param localInterface interface class
     * @return if success
     */
    public static <T> boolean registerInterfaceNoThrow(Context context, String provider, String interfaceName,
            T object, Class<T> localInterface) throws InvokeException {
        try {
            return registerInterface(context, provider, interfaceName, object, localInterface);
        } catch (Throwable t) {
            if (LogUtil.LOG_ENABLED) {
                t.printStackTrace();
            }
        }

        return false;
    }

    /**
     * unregister interface, nothrow version
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param interfaceName interface name
     */
    public static <T> boolean unregisterInterfaceNoThrow(Context context, String provider,
            String interfaceName) throws InvokeException {
        try {
            return unregisterInterface(context, provider, interfaceName);
        } catch (Throwable t) {
            if (LogUtil.LOG_ENABLED) {
                t.printStackTrace();
            }
        }

        return false;
    }

    /**
     * fetch interface, nothrow version
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param interfaceName interface name
     * @param localInterface interface class
     * @return proxy instance
     */
    public static <T> T fetchInterfaceNoThrow(Context context, String provider, final String interfaceName,
            final Class<T> localInterface) throws InvokeException {
        try {
            return fetchInterface(context, provider, interfaceName, localInterface);
        } catch (Throwable t) {
            if (LogUtil.LOG_ENABLED) {
                t.printStackTrace();
            }
        }

        return null;
    }

    /**
     * check whether the remote of the binder/proxy is alive
     * @param obj binder/proxy object
     * @return alive
     */
    public static boolean isRemoteAlive(Object obj) {
        if (obj instanceof IBinder) {
            return ((IBinder) obj).isBinderAlive();
        }

        if (obj instanceof _IRemote) {
            IBinder binder = ((_IRemote) obj)._asBinder();
            if (binder != null) {
                return binder.isBinderAlive();
            }
        }

        return false;
    }

    /**
     * add a Codec to serialize/deserialize custom class.
     * @param srcClass src class type
     * @param remoteClass encoded class type which can be written to Parcel
     * @param codec Codec
     */
    public static <S, R> void appendCodec(Class<S> srcClass, Class<R> remoteClass, Codec<S, R> codec) {
        InterfaceHandler.addCodec(srcClass, remoteClass, codec);
    }

    /**
     * add an Encoder to serialize custom class.
     * @param srcClass src class type
     * @param remoteClass encoded class type which can be written to Parcel
     * @param encoder encoder
     */
    public static <S, R> void appendEncoder(Class<S> srcClass, Class<R> remoteClass, Encoder<S, R> encoder) {
        InterfaceHandler.addEncoder(srcClass, remoteClass, encoder);
    }

    /**
     * add an Decoder to deserialize encoded class.
     * @param remoteClass encoded class type read from Parcel
     * @param srcClass desired src class type
     * @param decoder decoder
     */
    public static <R, S> void appendDecoder(Class<R> remoteClass, Class<S> srcClass, Decoder<R, S> decoder) {
        InterfaceHandler.addDecoder(remoteClass, srcClass, decoder);
    }

    /**
     * disable client binder/interface cache.
     */
    public static void noClientCache() {
        sBinderServiceCache = BinderCache.noCache();
        sInterfaceServiceCache = BinderCache.noCache();
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

    /**
     * ipc protocol version
     * @return protocol version
     */
    public static int getProtocolVersion() {
        return BuildConfig.PROTOCAL_VERSION;
    }

    /**
     * sdk version name
     * @return version name
     */
    public static String getSDKVersion() {
        return BuildConfig.SDK_VERSION;
    }

    private static String cacheKey(String provider, String name) {
        return String.format("[p=%s,s=%s]", provider, name);
    }

    private static InvokerBridge ensureService(Context context, final String provider) {
        InvokerBridge service = null;

        synchronized (sInvokerClientMap) {
            service = sInvokerClientMap.get(provider);
            if (service != null) {
                final IBinder iBinder = service.asBinder();
                if (iBinder != null && iBinder.isBinderAlive()) {
                    return service;
                } else {
                    service = null;
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
                    final int pid = bundle.getInt(AndInvokerProvider.KEY_PID, -1);
                    final int uid = bundle.getInt(AndInvokerProvider.KEY_UID, -1);
                    final int remoteProtocolVersion = bundle.getInt(
                            AndInvokerProvider.KEY_PROTOCOL_VERSION, -1);

                    checkProtocol(getProtocolVersion(), remoteProtocolVersion);

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
                                    onProcessDied(provider, pid, uid);
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

    private static void checkProtocol(int localProtocol, int remoteProtocol) throws InvokeException {
        // TODO checkversion
    }

    private static void onProcessDied(String provider, int pid, int uid) {
        LogUtil.w(TAG, "onProcessDied() provider = %s, pid = %d, uid = %d",
                provider, pid, uid);
    }
}

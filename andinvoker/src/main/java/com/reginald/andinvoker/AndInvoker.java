package com.reginald.andinvoker;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.reginald.andinvoker.api.Codec;
import com.reginald.andinvoker.api.Decoder;
import com.reginald.andinvoker.api.Encoder;
import com.reginald.andinvoker.api.ICall;
import com.reginald.andinvoker.api.IInvoker;
import com.reginald.andinvoker.api.IServiceFetcher;
import com.reginald.andinvoker.internal.BinderParcelable;
import com.reginald.andinvoker.internal.Call;
import com.reginald.andinvoker.internal.CallWrapper;
import com.reginald.andinvoker.internal.InvokerBridge;
import com.reginald.andinvoker.internal.itfc.InterfaceHandler;
import com.reginald.andinvoker.internal.itfc.InterfaceInfo;
import com.reginald.andinvoker.internal.itfc.InterfaceParcelable;

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
                iBinder = invokerManager.fetchService(serviceName, null);
                return iBinder;
            } catch (RemoteException e) {
                throw new InvokeException(e);
            }
        }

        throw new InvokeException(String.format("no binder service found for %s @ %s",
                serviceName, provider));
    }


    /**
     * register IServiceFetcher in the current process statically if a ContentProvider is registered in the same process
     * @param serviceName serviceName  e.g. ${your_package_name}.serviceA
     * @param serviceFetcher instance of IServiceFetcher
     */
    public static void registerService(String serviceName, IServiceFetcher serviceFetcher) {
        if (serviceFetcher != null) {
            AndInvokerProvider.registerLocalService(serviceName, serviceFetcher);
        }
    }

    /**
     * register binder service in the current process statically if a ContentProvider is registered in the same process
     * @param serviceName serviceName  e.g. ${your_package_name}.serviceA
     * @param binderServiceClass class of binder service
     */
    public static void registerService(String serviceName, final Class<? extends Binder> binderServiceClass)
            throws InvokeException {
        if (binderServiceClass != null) {
            AndInvokerProvider.registerLocalService(serviceName, new IServiceFetcher() {
                @Override
                public IBinder onFetchService(Context context) {
                    try {
                        Binder binder = binderServiceClass.newInstance();
                        return binder;
                    } catch (Exception e) {
                        if (LogUtil.LOG_ENABLED) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }
            });
        }
    }

    /**
     * register binder service in remote process dynamically. Not Recommended!
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName  e.g. ${your_package_name}.serviceA
     * @param binder instance of binder service
     * @return if success
     * @throws InvokeException InvokeException throws if register fails
     */
    public static boolean registerRemoteService(Context context, String provider, String serviceName,
            Binder binder) throws InvokeException {
        if (binder != null) {
            InvokerBridge invokerManager = ensureService(context, provider);
            if (invokerManager != null) {
                return AndInvokerProvider.registerService(context, invokerManager,
                        serviceName, binder);
            }
        }

        return false;
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

        return false;
    }

    /**
     * register IInvoker in the current process statically if a ContentProvider is registered in the same process
     * @param serviceName serviceName  e.g. ${your_package_name}.serviceA
     * @param iInvokerClass type of IInvoker
     */
    public static void registerInvoker(String serviceName,
            Class<? extends IInvoker> iInvokerClass) {
        if (iInvokerClass != null) {
            AndInvokerProvider.registerLocalInvoker(serviceName, iInvokerClass);
        }
    }

    /**
     * register IInvoker in the current process statically if a ContentProvider is registered in the same process
     * @param serviceName serviceName  e.g. ${your_package_name}.serviceA
     * @param invoker instance of IInvoker
     */
    public static void registerInvoker(String serviceName, IInvoker invoker) {
        if (invoker != null) {
            AndInvokerProvider.registerLocalInvoker(serviceName, invoker);
        }
    }

    /**
     * register IInvoker in remote process dynamically. Not Recommended!
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
            InvokerBridge invokerManager = ensureService(context, provider);
            if (invokerManager != null) {
                return AndInvokerProvider.registerInvoker(context, invokerManager, serviceName, invoker);
            }
        }

        return false;
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

        return false;
    }

    /**
     * register local interface
     * @param interfaceName interface name
     * @param object instance
     * @param localInterface interface class
     */
    public static <T> void registerInterface(String interfaceName, T object, Class<T> localInterface) {
        AndInvokerProvider.registerLocalInterface(interfaceName, object, localInterface);
    }

    /**
     * register IInvoker in remote process dynamically. Not Recommended!
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param interfaceName interface name
     * @param object instance
     * @param localInterface interface class
     * @return if success
     * @throws InvokeException InvokeException throws if register fails
     */
    public static <T> boolean registerRemoteInterface(Context context, String provider, String interfaceName,
            T object, Class<T> localInterface) throws InvokeException {
        if (object != null && localInterface != null) {
            InvokerBridge invokerManager = ensureService(context, provider);
            if (invokerManager != null) {
                return AndInvokerProvider.registerInterface(context, invokerManager, interfaceName,
                        object, localInterface);
            }
        }

        return false;
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

        return false;
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
    public static <T> T fetchInterface(Context context, String provider, final String interfaceName,
            final Class<T> localInterface) throws InvokeException {
        if (interfaceName != null) {
            InvokerBridge invokerManager = ensureService(context, provider);
            if (invokerManager != null) {
                try {
                    Bundle result = invokerManager.fetchInterface(interfaceName);
                    result.setClassLoader(AndInvoker.class.getClassLoader());

                    InterfaceInfo<T> interfaceInfo = new InterfaceInfo<>(localInterface);
                    InterfaceParcelable interfaceParcelable = result.getParcelable("binder");
                    if (interfaceParcelable != null) {
                        final Call call = Call.Stub.asInterface(interfaceParcelable.iBinder);
                        return interfaceInfo.fetchProxy(call);
                    }
                } catch (RemoteException e) {
                    throw new InvokeException(e);
                } catch (Exception e) {
                    throw new InvokeException(e);
                }
            }
        }

        throw new InvokeException(String.format("interface fetch error for %s @ %s",
                interfaceName, provider));
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
     * register binder service in remote process dynamically. Not Recommended! nothrow version
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param serviceName serviceName  e.g. ${your_package_name}.serviceA
     * @param binder instance of binder service
     * @return if success
     */
    public static boolean registerRemoteServiceNoThrow(Context context, String provider, String serviceName,
            Binder binder) throws InvokeException {
        try {
            return registerRemoteService(context, provider, serviceName, binder);
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
     * register IInvoker in remote process dynamically. Not Recommended! nothrow version
     * @param context Context
     * @param provider authorities of ContentProvider
     * @param interfaceName interface name
     * @param object instance
     * @param localInterface interface class
     * @return if success
     */
    public static <T> boolean registerRemoteInterfaceNoThrow(Context context, String provider, String interfaceName,
            T object, Class<T> localInterface) throws InvokeException {
        try {
            return registerRemoteInterface(context, provider, interfaceName, object, localInterface);
        } catch (Throwable t) {
            if (LogUtil.LOG_ENABLED) {
                t.printStackTrace();
            }
        }

        return false;
    }

    /**
     * unregister interface, nothrow version
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
     * add a Codec to serialize/deserialize custom class.
     * @param srcClass
     * @param remoteClass
     * @param codec
     * @param <S>
     * @param <R>
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
                                    onProcessDied(provider);
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

    private static void onProcessDied(String provider) {
        LogUtil.w(TAG, "onProcessDied() provider = " + provider);
    }
}

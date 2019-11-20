package com.reginald.andinvoker;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import com.reginald.andinvoker.api.ICall;
import com.reginald.andinvoker.api.IInvoker;
import com.reginald.andinvoker.api.IServiceFetcher;
import com.reginald.andinvoker.internal.BinderParcelable;
import com.reginald.andinvoker.internal.Call;
import com.reginald.andinvoker.internal.CallWrapper;
import com.reginald.andinvoker.internal.InvokerBridge;
import com.reginald.andinvoker.internal.itfc.InterfaceInfo;
import com.reginald.andinvoker.internal.itfc.InterfaceParcelable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ContentProvider used for internal rpc. You must register this in AndroidManifest.xml for the process where IInovker registered
 */
public class AndInvokerProvider extends ContentProvider {
    private static final String TAG = "AndInvokerProvider";

    private static InvokerStub sInvokerStub;
    private static final Map<String, IServiceFetcher> sRegisteredServiceFetcher =
            new ConcurrentHashMap<>();
    private static final Map<String, Class<? extends IInvoker>> sRegisteredIInvokerClass =
            new ConcurrentHashMap<>();
    private static final Map<String, InterfaceInfo<?>> sRegisteredInterfaces =
            new ConcurrentHashMap<>();

    public static final String KEY_SERVICE = "key.service";
    public static final String METHOD_GET_INVOKER = "method.get_invoker";

    private static final String KEY_REMOTE_BRIDGE_TYPE = "ai_remote_bridge_type";

    private static final int REMOTE_BRIDGE_TYPE_BINDER = 1;
    private static final int REMOTE_BRIDGE_TYPE_INVOKER = 2;
    private static final int REMOTE_BRIDGE_TYPE_INTERFACE = 3;

    private static Bundle buildRemoteParams(int type) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_REMOTE_BRIDGE_TYPE, type);
        return bundle;
    }

    @Override
    public boolean onCreate() {
        if (sInvokerStub == null) {
            sInvokerStub = new InvokerStub(getContext());
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

    static boolean registerLocalService(String serviceName, IServiceFetcher serviceFetcher) {
        if (sInvokerStub != null) {
            sInvokerStub.registerLocalService(serviceName, serviceFetcher);
            return true;
        }

        return false;
    }

    static boolean registerService(Context context, InvokerBridge invokerBridge, String serviceName,
            final Binder binder) throws InvokeException {
        if (invokerBridge == sInvokerStub) {
            return registerLocalService(serviceName, new IServiceFetcher() {
                @Override
                public IBinder onFetchService(Context context) {
                    return binder;
                }
            });
        } else {
            try {
                InvokerStub stub = null;
                if (binder != null) {
                    stub = new InvokerStub(context);
                    stub.mLocalBinderCacheMap.put(serviceName, binder);
                }
                return invokerBridge.register(serviceName, stub,
                        buildRemoteParams(REMOTE_BRIDGE_TYPE_BINDER));
            } catch (RemoteException e) {
                throw new InvokeException(e);
            }
        }
    }

    static void registerLocalInvoker(String serviceName, Class<? extends IInvoker> iInvokerClass) {
        if (iInvokerClass != null) {
            sRegisteredIInvokerClass.put(serviceName, iInvokerClass);
        }
    }


    static boolean registerLocalInvoker(String serviceName, IInvoker invoker) {
        if (sInvokerStub != null) {
            sInvokerStub.registerLocalInvoker(serviceName, invoker);
            return true;
        }

        return false;
    }

    static boolean registerInvoker(Context context, InvokerBridge invokerBridge, String serviceName,
            IInvoker invoker) throws InvokeException {
        if (invokerBridge == sInvokerStub) {
            return registerLocalInvoker(serviceName, invoker);
        } else {
            try {
                InvokerStub stub = null;
                if (invoker != null) {
                    stub = new InvokerStub(context);
                    stub.mLocalInvokerCacheMap.put(serviceName, invoker);
                }
                return invokerBridge.register(serviceName, stub,
                        buildRemoteParams(REMOTE_BRIDGE_TYPE_INVOKER));
            } catch (RemoteException e) {
                throw new InvokeException(e);
            }
        }
    }

    static <T> boolean registerLocalInterface(String serviceName, T object, Class<T> clazz) {
        if (sInvokerStub != null) {
            InterfaceInfo interfaceInfo = null;
            if (object != null && clazz != null) {
                interfaceInfo = new InterfaceInfo(object, clazz);
            }
            sInvokerStub.registerLocalInterface(serviceName, interfaceInfo);
            return true;
        }

        return false;
    }

    static <T> boolean registerInterface(Context context, InvokerBridge invokerBridge, String serviceName,
            T object, Class<T> clazz) throws InvokeException {
        if (invokerBridge == sInvokerStub) {
            return registerLocalInterface(serviceName, object, clazz);
        } else {
            try {
                InvokerStub stub = null;
                if (object != null && clazz != null) {
                    stub = new InvokerStub(context);
                    stub.mLocalInterfaceCacheMap.put(serviceName, new InterfaceInfo<T>(object, clazz));
                }
                return invokerBridge.register(serviceName, stub,
                        buildRemoteParams(REMOTE_BRIDGE_TYPE_INTERFACE));
            } catch (RemoteException e) {
                throw new InvokeException(e);
            }
        }
    }

    private static class InvokerStub extends InvokerBridge.Stub {
        private String TAG = "InvokerStub";

        private final Map<String, IInvoker> mLocalInvokerCacheMap = new ConcurrentHashMap<>();
        private final Map<String, BridgeRecord> mRemoteInvokerCacheMap = new ConcurrentHashMap<>();
        private final Map<String, IBinder> mLocalBinderCacheMap = new ConcurrentHashMap<>();
        private final Map<String, BridgeRecord> mRemoteBinderCacheMap = new ConcurrentHashMap<>();
        private final Map<String, InterfaceInfo<?>> mLocalInterfaceCacheMap = new ConcurrentHashMap<>();
        private final Map<String, BridgeRecord> mRemoteInterfaceCacheMap = new ConcurrentHashMap<>();

        private final Context mContext;

        private InvokerStub(Context context) {
            mContext = context;
        }

        private IServiceFetcher fetchLocalService(String serviceName) {
            LogUtil.d(TAG, "fetchLocalService() serviceName = %s", serviceName);
            if (serviceName == null) {
                LogUtil.w(TAG, String.format("fetchLocalService() serviceName is Null! " +
                        "for service %s", serviceName));
                return null;
            }

            IServiceFetcher serviceFetcher = sRegisteredServiceFetcher.get(serviceName);

            if (serviceFetcher == null) {
                LogUtil.w(TAG, String.format("fetchLocalService() no serviceFetcher found for %s",
                        serviceName));
            }

            return serviceFetcher;
        }

        private IInvoker fetchLocalInvoker(String serviceName) {
            LogUtil.d(TAG, "fetchLocalInvoker() serviceName = %s", serviceName);
            if (serviceName == null) {
                LogUtil.w(TAG, String.format("fetchLocalInvoker() serviceName is Null! " +
                        "for service %s", serviceName));
                return null;
            }

            synchronized (mLocalInvokerCacheMap) {
                IInvoker resultInvoker = mLocalInvokerCacheMap.get(serviceName);
                LogUtil.w(TAG, String.format("fetchLocalInvoker() cached IInvoker = %s", resultInvoker));

                if (resultInvoker != null) {
                    LogUtil.d(TAG, "fetchLocalInvoker() serviceName = %s, get cached %s",
                            serviceName, resultInvoker);
                    return resultInvoker;
                }

                try {
                    Class<? extends IInvoker> invokerClazz = sRegisteredIInvokerClass.get(serviceName);
                    if (invokerClazz != null) {
                        resultInvoker = invokerClazz.newInstance();

                        if (resultInvoker != null) {
                            LogUtil.d(TAG, "fetchLocalInvoker() serviceName = %s, " +
                                            "new invoker from class %s",
                                    serviceName, invokerClazz);
                            mLocalInvokerCacheMap.put(serviceName, resultInvoker);
                        }
                        return resultInvoker;
                    }
                } catch (Throwable t) {
                    if (LogUtil.LOG_ENABLED) {
                        t.printStackTrace();
                    }
                }
            }

            LogUtil.w(TAG, String.format("fetchLocalInvoker() no invoker found for %s",
                    serviceName));

            return null;
        }

        private InterfaceInfo<?> fetchLocalInterface(String interfaceName) {
            LogUtil.d(TAG, "fetchLocalInterface() interfaceName = %s", interfaceName);
            if (interfaceName == null) {
                LogUtil.w(TAG, String.format("fetchLocalInterface() serviceName is Null! " +
                        "for interface %s", interfaceName));
                return null;
            }

            InterfaceInfo<?> interfaceInfo = mLocalInterfaceCacheMap.get(interfaceName);

            if (interfaceInfo == null) {
                interfaceInfo = sRegisteredInterfaces.get(interfaceName);
            }

            if (interfaceInfo == null) {
                LogUtil.w(TAG, String.format("fetchLocalInterface() no interface found for %s",
                        interfaceName));
            }

            return interfaceInfo;
        }

        private InvokerBridge fetchRemoteBridge(String name,
                Map<String, BridgeRecord> remoteBridges) {
            LogUtil.d(TAG, "fetchRemoteBridge() name = %s", name);
            if (name == null) {
                LogUtil.w(TAG, String.format("fetchRemoteBridge() name is Null! " +
                        "for serviceName %s", name));
                return null;
            }

            InvokerBridge invokerBridge = null;

            synchronized (remoteBridges) {
                BridgeRecord bridgeRecord = remoteBridges.get(name);
                invokerBridge = bridgeRecord != null ? bridgeRecord.bridge : null;
                LogUtil.w(TAG, String.format("fetchRemoteBridge() registered invokerBridge = %s",
                        remoteBridges.size(), invokerBridge));
            }

            if (invokerBridge != null) {
                final IBinder iBinder = invokerBridge.asBinder();
                if (iBinder != null && iBinder.isBinderAlive()) {
                    LogUtil.d(TAG, "fetchRemoteBridge() name = %s, get cached alive %s",
                            name, invokerBridge);
                    return invokerBridge;
                }
            }

            LogUtil.w(TAG, String.format("fetchRemoteBridge() no invoker found for %s",
                    name));

            return null;
        }

        @Override
        public IBinder fetchService(String serviceName, Bundle params) throws RemoteException {
            if (serviceName == null) {
                LogUtil.w(TAG,
                        String.format("fetchService() serviceName is Null! for %s", serviceName));
                throw new InvokeException(String.format("no valid serviceName for %s", serviceName));
            }

            IBinder cachedBinder = fetchCachedBinder(serviceName);
            if (cachedBinder != null) {
                return cachedBinder;
            }

            // fetch local
            synchronized (mLocalBinderCacheMap) {
                cachedBinder = fetchCachedBinder(serviceName);
                if (cachedBinder != null) {
                    return cachedBinder;
                }

                IServiceFetcher serviceFetcher = fetchLocalService(serviceName);
                if (serviceFetcher != null) {
                    LogUtil.w(TAG,
                            String.format("fetchService() NOT found for %s", serviceName));

                    IBinder newBinder = serviceFetcher.onFetchService(mContext);
                    mLocalBinderCacheMap.put(serviceName, newBinder);
                    return newBinder;
                }
            }

            // fetch remote
            InvokerBridge invokerBridge = fetchRemoteBridge(serviceName, mRemoteBinderCacheMap);
            if (invokerBridge != null) {
                return invokerBridge.fetchService(serviceName, null);
            }

            throw new InvokeException(String.format("no service found for %s",
                    serviceName));
        }

        @Override
        public Bundle invoke(String serviceName, String methodName,
                Bundle params, Call callback) throws RemoteException {
            // fetch local
            IInvoker iInvoker = fetchLocalInvoker(serviceName);
            if (iInvoker != null) {
                ICall iInvokeCallback = CallWrapper.build(callback);

                Bundle result =
                        iInvoker.onInvoke(mContext, methodName, params, iInvokeCallback);
                return result;
            }

            // fetch remote
            InvokerBridge invokerBridge = fetchRemoteBridge(serviceName, mRemoteInvokerCacheMap);
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
        public Bundle fetchInterface(String interfaceName) throws RemoteException {
            LogUtil.d(TAG, "fetchInterface() interfaceName = %s", interfaceName);
            if (interfaceName == null) {
                LogUtil.w(TAG, String.format("fetchInterface() serviceName is Null!"));
                return null;
            }

            InterfaceInfo<?> interfaceInfo = fetchLocalInterface(interfaceName);
            if (interfaceInfo != null) {
                Bundle result = new Bundle();
                Call call = interfaceInfo.fetchStub();
                if (call != null) {
                    result.putParcelable("binder", new InterfaceParcelable(call.asBinder()));
                    return result;
                }
            }

            InvokerBridge invokerBridge = fetchRemoteBridge(interfaceName, mRemoteInterfaceCacheMap);
            if (invokerBridge != null) {
                return invokerBridge.fetchInterface(interfaceName);
            }

            throw new InvokeException(String.format("no interface found for %s", interfaceName));
        }

        @Override
        public boolean register(final String serviceName, InvokerBridge bridge, Bundle params)
                throws RemoteException {
            if (serviceName == null) {
                LogUtil.w(TAG,
                        String.format("register() serviceName is Null! for %s", serviceName));
                throw new InvokeException(String.format("no valid serviceName for %s", serviceName));
            }

            Map<String, BridgeRecord> remoteBridge = null;

            if (params != null) {
                int bridgeType = params.getInt(KEY_REMOTE_BRIDGE_TYPE);
                if (bridgeType == REMOTE_BRIDGE_TYPE_BINDER) {
                    remoteBridge = mRemoteBinderCacheMap;
                } else if (bridgeType == REMOTE_BRIDGE_TYPE_INVOKER) {
                    remoteBridge = mRemoteInvokerCacheMap;
                } else if (bridgeType == REMOTE_BRIDGE_TYPE_INTERFACE) {
                    remoteBridge = mRemoteInterfaceCacheMap;
                }
            }

            if (remoteBridge == null) {
                LogUtil.w(TAG,
                        String.format("register() remoteBridge is Null! for %s", serviceName));
                throw new InvokeException(String.format("no valid remoteBridge found for %s", serviceName));
            }

            LogUtil.w(TAG,
                    String.format("register() serviceName = %s, remoteBridgeMap = %s, bridge = %s, params = %s",
                            serviceName, remoteBridge, bridge, params));

            synchronized (remoteBridge) {
                if (bridge == null) {
                    // remove remote registered ONLY
                    remoteBridge.remove(serviceName);
                    return true;
                } else {
                    final IBinder iBinder = bridge.asBinder();
                    if (iBinder != null) {
                        // remove old bridge first
                        BridgeRecord oldBridge = remoteBridge.remove(serviceName);
                        if (oldBridge != null) {
                            oldBridge.unlinkToDeath();
                            LogUtil.d(TAG, "register() serviceName = %s, remove old bridge %s",
                                    serviceName, oldBridge);
                        }

                        // add new bridge
                        BridgeRecord br = new BridgeRecord(serviceName, bridge);
                        br.linkToDeath();
                        remoteBridge.put(serviceName, br);
                        return true;
                    }
                }
            }

            return false;
        }

        private void registerLocalInvoker(String serviceName, IInvoker iInvoker) {
            if (iInvoker == null) {
                // remove local registered
                synchronized (mLocalInvokerCacheMap) {
                    mLocalInvokerCacheMap.remove(serviceName);
                }
                sRegisteredIInvokerClass.remove(serviceName);

                // remove remote registered
                synchronized (mRemoteInvokerCacheMap) {
                    mRemoteInvokerCacheMap.remove(serviceName);
                }
            } else {
                synchronized (mLocalInvokerCacheMap) {
                    mLocalInvokerCacheMap.put(serviceName, iInvoker);
                }
            }

            LogUtil.d(TAG, "registerLocalInvoker() for serviceName = %s, iInvoker = %s",
                    serviceName, iInvoker);
        }

        private void registerLocalService(String serviceName, IServiceFetcher serviceFetcher) {
            if (serviceFetcher == null) {
                sRegisteredServiceFetcher.remove(serviceName);

                // remove remote registered
                synchronized (mRemoteBinderCacheMap) {
                    mRemoteBinderCacheMap.remove(serviceName);
                }

                // remove binder service registered
                synchronized (mLocalBinderCacheMap) {
                    mLocalBinderCacheMap.remove(serviceName);
                }

            } else {
                sRegisteredServiceFetcher.put(serviceName, serviceFetcher);
            }

            LogUtil.d(TAG, "registerLocalService() for serviceName = %s, serviceFetcher = %s",
                    serviceName, serviceFetcher);
        }

        private void registerLocalInterface(String serviceName, InterfaceInfo<?> interfaceInfo) {
            if (interfaceInfo == null) {
                sRegisteredInterfaces.remove(serviceName);

                // remove local registered
                synchronized (mLocalInterfaceCacheMap) {
                    mLocalInterfaceCacheMap.remove(serviceName);
                }

                // remove remote registered
                synchronized (mRemoteInterfaceCacheMap) {
                    mRemoteInterfaceCacheMap.remove(serviceName);
                }
            } else {
                sRegisteredInterfaces.put(serviceName, interfaceInfo);
            }

            LogUtil.d(TAG, "registerLocalInterface() for serviceName = %s, interfaceInfo = %s",
                    serviceName, interfaceInfo);
        }

        private IBinder fetchCachedBinder(String serviceName) {
            IBinder cachedBinder = mLocalBinderCacheMap.get(serviceName);
            if (cachedBinder != null) {
                LogUtil.w(TAG, String.format("fetchCachedBinder() cached binder = %s", cachedBinder));
                return cachedBinder;
            }

            return null;
        }

        private class BridgeRecord {
            public final String serviceName;
            public final InvokerBridge bridge;
            public IBinder iBinder;
            private BinderDeath mBd;

            public BridgeRecord(String serviceName, InvokerBridge invokerBridge) {
                this.serviceName = serviceName;
                this.bridge = invokerBridge;
                this.iBinder = invokerBridge.asBinder();
            }

            public void linkToDeath() throws RemoteException {
                synchronized (this) {
                    if (iBinder != null && mBd == null) {
                        mBd = new BinderDeath(this);
                        iBinder.linkToDeath(mBd, 0);
                    }
                }
            }

            public void unlinkToDeath() {
                synchronized (this) {
                    if (iBinder != null && mBd != null) {
                        iBinder.unlinkToDeath(mBd, 0);
                        iBinder = null;
                        mBd = null;
                    }
                }
            }

            private class BinderDeath implements IBinder.DeathRecipient {
                private final BridgeRecord mBr;

                BinderDeath(BridgeRecord br) {
                    mBr = br;
                }

                public void binderDied() {
                    synchronized (mBr) {
                        if (mBr.iBinder == null) {
                            return;
                        }

                        mBr.iBinder.unlinkToDeath(this, 0);
                        mBr.iBinder = null;

                        onRemoteInvokerDied(serviceName);
                    }
                }
            }
        }

        private void onRemoteInvokerDied(String serviceName) {
            LogUtil.w(TAG, "onRemoteInvokerDied() serviceName = " + serviceName);

            synchronized (mRemoteBinderCacheMap) {
                mRemoteBinderCacheMap.remove(serviceName);
            }

            synchronized (mRemoteInvokerCacheMap) {
                mRemoteInvokerCacheMap.remove(serviceName);
            }

            synchronized (mLocalBinderCacheMap) {
                mLocalBinderCacheMap.remove(serviceName);
            }
        }
    }
}

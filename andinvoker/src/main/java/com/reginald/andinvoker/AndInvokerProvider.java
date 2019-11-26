package com.reginald.andinvoker;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
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
 * ContentProvider used for internal ipc. You must register this in AndroidManifest.xml for the process where IInovker registered
 */
public class AndInvokerProvider extends ContentProvider {
    private static final String TAG = "AndInvokerProvider";

    public static final String KEY_SERVICE = "key.service";
    public static final String KEY_PID = "key.pid";
    public static final String KEY_UID = "key.uid";
    public static final String KEY_PROTOCOL_VERSION = "key.pv";

    public static final String METHOD_GET_INVOKER = "method.get_invoker";

    private static final String KEY_REMOTE_BRIDGE_TYPE = "ai_remote_bridge_type";

    private static final int REMOTE_BRIDGE_TYPE_BINDER = 1;
    private static final int REMOTE_BRIDGE_TYPE_INVOKER = 2;
    private static final int REMOTE_BRIDGE_TYPE_INTERFACE = 3;

    private InvokerStub mInvokerStub;

    private static Bundle buildRemoteParams(int type) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_REMOTE_BRIDGE_TYPE, type);
        return bundle;
    }

    @Override
    public boolean onCreate() {
        if (mInvokerStub == null) {
            mInvokerStub = new InvokerStub(getContext());
            LogUtil.d(TAG, "onCreate() sInvokerStub = " + mInvokerStub);
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
                BinderParcelable binderParcelable = new BinderParcelable(mInvokerStub.asBinder());
                Bundle bundle = new Bundle();
                bundle.putParcelable(KEY_SERVICE, binderParcelable);
                bundle.putInt(KEY_PID, Process.myPid());
                bundle.putInt(KEY_UID, Process.myUid());
                bundle.putInt(KEY_PROTOCOL_VERSION, AndInvoker.getProtocolVersion());
                return bundle;
            } else {
                LogUtil.w(TAG, "call method " + method + " NOT supported!");
            }
        }
        return null;
    }

    static boolean registerService(Context context, InvokerBridge invokerBridge, String serviceName,
            final IServiceFetcher<IBinder> serviceFetcher) throws InvokeException {
        if (invokerBridge instanceof InvokerStub.Stub) {
            ((InvokerStub) invokerBridge).registerLocalService(serviceName, serviceFetcher);
            return true;
        } else {
            try {
                InvokerStub stub = null;
                if (serviceFetcher != null) {
                    stub = new InvokerStub(context);
                    stub.registerLocalService(serviceName, serviceFetcher);
                }
                return invokerBridge.register(serviceName, stub,
                        buildRemoteParams(REMOTE_BRIDGE_TYPE_BINDER));
            } catch (RemoteException e) {
                throw new InvokeException(e);
            }
        }
    }

    static boolean registerInvoker(Context context, InvokerBridge invokerBridge, String serviceName,
            final IServiceFetcher<IInvoker> invokerFetcher) throws InvokeException {
        if (invokerBridge instanceof InvokerStub.Stub) {
            ((InvokerStub) invokerBridge).registerLocalInvoker(serviceName, invokerFetcher);
            return true;
        } else {
            try {
                InvokerStub stub = null;
                if (invokerFetcher != null) {
                    stub = new InvokerStub(context);
                    stub.registerLocalInvoker(serviceName, invokerFetcher);
                }
                return invokerBridge.register(serviceName, stub,
                        buildRemoteParams(REMOTE_BRIDGE_TYPE_INVOKER));
            } catch (RemoteException e) {
                throw new InvokeException(e);
            }
        }
    }

    static <T> boolean registerInterface(Context context, InvokerBridge invokerBridge, String serviceName,
            T object, Class<T> clazz) throws InvokeException {
        InterfaceInfo interfaceInfo = null;
        if (object != null && clazz != null) {
            interfaceInfo = new InterfaceInfo<T>(object, clazz);
        }

        if (invokerBridge instanceof InvokerStub.Stub) {
            ((InvokerStub) invokerBridge).registerLocalInterface(serviceName, interfaceInfo);
            return true;
        } else {
            try {
                InvokerStub stub = null;
                if (object != null && clazz != null) {
                    stub = new InvokerStub(context);
                    stub.registerLocalInterface(serviceName, interfaceInfo);
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

        private final Map<String, IServiceFetcher<IBinder>> mRegisteredServiceFetcher =
                new ConcurrentHashMap<>();
        private final Map<String, IServiceFetcher<IInvoker>> mRegisteredIInvokerFetcher =
                new ConcurrentHashMap<>();
        private final Map<String, InterfaceInfo<?>> mRegisteredInterfaces =
                new ConcurrentHashMap<>();

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

        private IServiceFetcher<IBinder> fetchLocalService(String serviceName) {
            LogUtil.d(TAG, "fetchLocalService() serviceName = %s", serviceName);
            if (serviceName == null) {
                LogUtil.w(TAG, String.format("fetchLocalService() serviceName is Null! " +
                        "for service %s", serviceName));
                return null;
            }

            IServiceFetcher serviceFetcher = mRegisteredServiceFetcher.get(serviceName);

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
                    IServiceFetcher<IInvoker> invokerFetcher = mRegisteredIInvokerFetcher.get(serviceName);
                    if (invokerFetcher != null) {
                        resultInvoker = invokerFetcher.onFetchService(mContext);

                        if (resultInvoker != null) {
                            LogUtil.d(TAG, "fetchLocalInvoker() serviceName = %s, " +
                                            "new invoker from invokerFetcher %s",
                                    serviceName, invokerFetcher);
                            mLocalInvokerCacheMap.put(serviceName, resultInvoker);
                            return resultInvoker;
                        } else {
                            throw new InvokeException(String.format("invoker fetch null for %s",
                                    serviceName));
                        }
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

            synchronized (mLocalInterfaceCacheMap) {
                InterfaceInfo<?> interfaceInfo = mLocalInterfaceCacheMap.get(interfaceName);

                if (interfaceInfo != null) {
                    return interfaceInfo;
                }

                interfaceInfo = mRegisteredInterfaces.get(interfaceName);
                if (interfaceInfo != null) {
                    mLocalInterfaceCacheMap.put(interfaceName, interfaceInfo);
                    return interfaceInfo;
                } else {
                    return null;
                }
            }
        }

        private InvokerBridge fetchRemoteBridge(String name,
                Map<String, BridgeRecord> remoteBridges) {
            LogUtil.d(TAG, "fetchRemoteBridge() name = %s, remoteBridges = %s",
                    name, remoteBridges);
            if (name == null || remoteBridges == null) {
                LogUtil.w(TAG, String.format("fetchRemoteBridge() name or remoteBridges is Null! " +
                        "for serviceName %s", name));
                return null;
            }

            InvokerBridge invokerBridge = null;

            synchronized (remoteBridges) {
                BridgeRecord bridgeRecord = remoteBridges.get(name);
                invokerBridge = bridgeRecord != null ? bridgeRecord.bridge : null;
                LogUtil.w(TAG, String.format("fetchRemoteBridge() registered invokerBridge(size = %d) = %s",
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

            LogUtil.w(TAG, String.format("fetchRemoteBridge() no bridge found for %s",
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

                IServiceFetcher<IBinder> serviceFetcher = fetchLocalService(serviceName);
                if (serviceFetcher != null) {
                    LogUtil.w(TAG,
                            String.format("fetchService() NOT found for %s", serviceName));

                    IBinder newBinder = serviceFetcher.onFetchService(mContext);
                    if (newBinder != null) {
                        mLocalBinderCacheMap.put(serviceName, newBinder);
                        return newBinder;
                    } else {
                        throw new InvokeException(String.format("binder fetch null for %s",
                                serviceName));
                    }
                }
            }

            // fetch remote
            InvokerBridge invokerBridge = fetchRemoteBridge(serviceName, mRemoteBinderCacheMap);
            if (invokerBridge != null) {
                return invokerBridge.fetchService(serviceName, null);
            }

            throw new InvokeException(String.format("no binder service found for %s",
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

            throw new InvokeException(String.format("no invoker found for %s", serviceName));
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
            int callingPid = getCallingPid();
            int callingUid = getCallingUid();

            Map<String, BridgeRecord> remoteBridge = null;
            int bridgeType = -1;

            if (params != null) {
                bridgeType = params.getInt(KEY_REMOTE_BRIDGE_TYPE, -1);
                remoteBridge = getBridgeMap(bridgeType);
            }

            if (remoteBridge == null) {
                LogUtil.w(TAG,
                        String.format("register() remoteBridge is Null! for %s", serviceName));
                throw new InvokeException(String.format("no valid remoteBridge found for %s", serviceName));
            }

            LogUtil.d(TAG,
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
                        BridgeRecord br = new BridgeRecord(serviceName, bridge, bridgeType,
                                callingPid, callingUid);
                        br.linkToDeath();
                        remoteBridge.put(serviceName, br);
                        return true;
                    }
                }
            }

            throw new InvokeException(String.format("remote register error for %s!", serviceName));
        }

        private Map<String, BridgeRecord> getBridgeMap(int bridgeType) {
            if (bridgeType == REMOTE_BRIDGE_TYPE_BINDER) {
                return mRemoteBinderCacheMap;
            } else if (bridgeType == REMOTE_BRIDGE_TYPE_INVOKER) {
                return mRemoteInvokerCacheMap;
            } else if (bridgeType == REMOTE_BRIDGE_TYPE_INTERFACE) {
                return mRemoteInterfaceCacheMap;
            }

            return null;
        }

        private void registerLocalService(String serviceName, IServiceFetcher<IBinder> serviceFetcher) {
            if (serviceFetcher == null) {
                mRegisteredServiceFetcher.remove(serviceName);

                // remove remote registered
                synchronized (mRemoteBinderCacheMap) {
                    mRemoteBinderCacheMap.remove(serviceName);
                }

                // remove binder service registered
                synchronized (mLocalBinderCacheMap) {
                    mLocalBinderCacheMap.remove(serviceName);
                }

            } else {
                mRegisteredServiceFetcher.put(serviceName, serviceFetcher);
            }

            LogUtil.d(TAG, "registerLocalService() for serviceName = %s, serviceFetcher = %s",
                    serviceName, serviceFetcher);
        }

        private void registerLocalInvoker(String serviceName, IServiceFetcher<IInvoker> serviceFetcher) {
            if (serviceFetcher == null) {
                // remove local registered
                synchronized (mLocalInvokerCacheMap) {
                    mLocalInvokerCacheMap.remove(serviceName);
                }
                mRegisteredIInvokerFetcher.remove(serviceName);

                // remove remote registered
                synchronized (mRemoteInvokerCacheMap) {
                    mRemoteInvokerCacheMap.remove(serviceName);
                }
            } else {
                mRegisteredIInvokerFetcher.put(serviceName, serviceFetcher);
            }

            LogUtil.d(TAG, "registerLocalInvoker() for serviceName = %s, serviceFetcher = %s",
                    serviceName, serviceFetcher);
        }

        private void registerLocalInterface(String serviceName, InterfaceInfo<?> interfaceInfo) {
            if (interfaceInfo == null) {
                mRegisteredInterfaces.remove(serviceName);

                // remove local registered
                synchronized (mLocalInterfaceCacheMap) {
                    mLocalInterfaceCacheMap.remove(serviceName);
                }

                // remove remote registered
                synchronized (mRemoteInterfaceCacheMap) {
                    mRemoteInterfaceCacheMap.remove(serviceName);
                }
            } else {
                mRegisteredInterfaces.put(serviceName, interfaceInfo);
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
            public final int pid;
            public final int uid;
            public final int bridgeType;
            public IBinder iBinder;
            private BinderDeath mBd;

            public BridgeRecord(String serviceName, InvokerBridge invokerBridge, int bridgeType,
                    int callingPid, int callingUid) {
                this.serviceName = serviceName;
                this.bridge = invokerBridge;
                this.bridgeType = bridgeType;
                this.iBinder = invokerBridge.asBinder();
                this.pid = callingPid;
                this.uid = callingUid;
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

            @Override
            public String toString() {
                if (LogUtil.LOG_ENABLED) {
                    return String.format("BridgeRecord[ serviceName = %s, bridge =%s, bridgeType = %d, " +
                                    "pid = %d, uid = %s ]",
                            serviceName, bridge, bridgeType, pid, uid);
                } else {
                    return super.toString();
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

                        onRemoteBridgeDied(mBr);
                    }
                }
            }
        }

        private void onRemoteBridgeDied(BridgeRecord bridgeRecord) {
            LogUtil.w(TAG, "onRemoteBridgeDied() bridgeRecord = " + bridgeRecord);

            Map<String, BridgeRecord> map = getBridgeMap(bridgeRecord.bridgeType);

            if (map != null) {
                synchronized (map) {
                    map.remove(bridgeRecord.serviceName);
                }
            }
        }
    }
}

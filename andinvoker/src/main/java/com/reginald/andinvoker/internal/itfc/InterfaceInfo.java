package com.reginald.andinvoker.internal.itfc;

import android.os.IBinder;

import com.reginald.andinvoker.LogUtil;
import com.reginald.andinvoker.internal.Call;
import com.reginald.andinvoker.internal.TokenCache;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InterfaceInfo<T> {
    private static final String TAG = "InterfaceInfo";

    private static final Map<Class<?>, TokenCache<Object, Call>> sStubCache =
            new ConcurrentHashMap<>();
    private static final Map<Class<?>, TokenCache<IBinder, Object>> sProxyCache =
            new ConcurrentHashMap<>();

    public final Class<T> interfaceClass;
    public final T object;

    private volatile Map<String, Method> interfaceMethods;
    private final Object interfaceMethodsLock = new Object();

    private volatile Call mStub;
    private volatile T mProxy;

    // TODO need cache for interfaces
    public InterfaceInfo(Class<T> clazz) {
        this(null, clazz);
    }

    public InterfaceInfo(T obj, Class<T> clazz) {
        interfaceClass = clazz;
        object = obj;
    }

    public Method fetchMethod(String methodName) {
        initMethods();
        return interfaceMethods.get(methodName);
    }

    public Call fetchStub() {
        if (object == null) {
            return null;
        }

        if (mStub == null) {
            synchronized (this) {
                if (mStub == null) {
                    TokenCache<Object, Call> cache = sStubCache.get(interfaceClass);
                    if (cache == null) {
                        cache = TokenCache.build("InterfaceInfo#Stub#" + interfaceClass.getSimpleName());
                        sStubCache.put(interfaceClass, cache);
                    }

                    mStub = cache.get(object, new TokenCache.Loader<Call>() {
                        @Override
                        public Call load() {
                            return InterfaceHandler.buildStub(InterfaceInfo.this);
                        }
                    });
                }
            }
        }

        LogUtil.d(TAG, "fetchStub() mStub = %s", mStub);
        return mStub;
    }

    public T fetchProxy(final Call callback) {
        if (callback == null) {
            return null;
        }

        if (mProxy == null) {
            synchronized (this) {
                if (mProxy == null) {
                    TokenCache<IBinder, Object> cache = sProxyCache.get(interfaceClass);
                    if (cache == null) {
                        cache = TokenCache.build("InterfaceInfo#Proxy#" + interfaceClass.getSimpleName());
                        sProxyCache.put(interfaceClass, cache);
                    }

                    mProxy = (T) cache.get(callback.asBinder(), new TokenCache.Loader<Object>() {
                        @Override
                        public Object load() {
                            return InterfaceHandler.buildProxy(InterfaceInfo.this, callback);
                        }
                    });
                }
            }
        }

        LogUtil.d(TAG, "fetchProxy() callback = %s, mProxy = %s",
                callback, mProxy);
        return mProxy;
    }

    private void initMethods() {
        if (interfaceMethods == null) {
            synchronized (interfaceMethodsLock) {
                if (interfaceMethods == null) {
                    interfaceMethods = new ConcurrentHashMap<>();
                    Method[] declaredMethods = interfaceClass.getDeclaredMethods();
                    if (declaredMethods != null) {
                        for (Method method : declaredMethods) {
                            interfaceMethods.put(method.getName(), method);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        if (LogUtil.LOG_ENABLED) {
            return String.format("InterfaceInfo[ interfaceClass = %s, object = %s, methods = %s ]",
                    interfaceClass, object, interfaceMethods);
        } else {
            return super.toString();
        }
    }
}
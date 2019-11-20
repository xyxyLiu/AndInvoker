package com.reginald.andinvoker.internal.itfc;

import com.reginald.andinvoker.internal.Call;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InterfaceInfo<T> {
    private static final String TAG = "InterfaceInfo";

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
                    mStub = InterfaceHandler.buildStub(this);
                }
            }
        }

        return mStub;
    }

    public T fetchProxy(Call callback) {
        if (callback == null) {
            return null;
        }

        if (mProxy == null) {
            synchronized (this) {
                if (mProxy == null) {
                    mProxy = InterfaceHandler.buildProxy(this, callback);
                }
            }
        }

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
}
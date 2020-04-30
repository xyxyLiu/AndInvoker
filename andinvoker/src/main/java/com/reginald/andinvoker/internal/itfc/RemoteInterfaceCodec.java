package com.reginald.andinvoker.internal.itfc;

import com.reginald.andinvoker.internal.LogUtil;
import com.reginald.andinvoker.api.Codec;
import com.reginald.andinvoker.api.RemoteInterface;
import com.reginald.andinvoker.internal.Call;

import java.lang.annotation.Annotation;

public class RemoteInterfaceCodec implements Codec<Object, InterfaceParcelable> {
    private static final String TAG = "RemoteInterfaceCodec";

    @Override
    public boolean handleDecode(InterfaceParcelable obj, Class<Object> clazz, Annotation[] annotations) {
        return isRemoteInterface(clazz, annotations);
    }

    @Override
    public Object decode(InterfaceParcelable obj, Class<Object> clazz) {
        InterfaceInfo<Object> interfaceInfo = new InterfaceInfo(clazz);
        Object decodedObj = interfaceInfo.fetchProxy(Call.Stub.asInterface(obj.iBinder));

        LogUtil.d(TAG, "decode() src = %s, clazz = %s -> %s",
                obj, clazz, decodedObj);
        return decodedObj;
    }

    @Override
    public boolean handleEncode(Object src, Class<Object> clazz, Annotation[] annotations) {
        return isRemoteInterface(clazz, annotations);
    }

    @Override
    public InterfaceParcelable encode(Object src, Class<Object> clazz) {
        InterfaceInfo interfaceInfo = new InterfaceInfo(src, clazz);
        Call stub = interfaceInfo.fetchStub();
        if (stub != null) {
            InterfaceParcelable encodedInterface = new InterfaceParcelable(stub.asBinder());
            LogUtil.d(TAG, "encode() src = %s, clazz = %s -> %s",
                    src, clazz, stub);
            return encodedInterface;
        }
        return null;
    }

    private static boolean isRemoteInterface(Class<Object> clazz, Annotation[] annotations) {
        if (!clazz.isInterface()) {
            return false;
        }

        Annotation[] classAnnotations = clazz.getAnnotations();
        if (classAnnotations != null) {
            for (Annotation annotation : classAnnotations) {
                if (annotation instanceof RemoteInterface) {
                    return true;
                }
            }
        }

        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof RemoteInterface) {
                    return true;
                }
            }
        }

        return false;
    }
}

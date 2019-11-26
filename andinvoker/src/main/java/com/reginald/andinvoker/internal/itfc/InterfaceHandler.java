package com.reginald.andinvoker.internal.itfc;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import com.reginald.andinvoker.AndInvoker;
import com.reginald.andinvoker.InvokeException;
import com.reginald.andinvoker.LogUtil;
import com.reginald.andinvoker.api.Codec;
import com.reginald.andinvoker.api.Decoder;
import com.reginald.andinvoker.api.Encoder;
import com.reginald.andinvoker.api._IRemote;
import com.reginald.andinvoker.internal.Call;
import com.reginald.andinvoker.internal.CallWrapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class InterfaceHandler {
    private static final String TAG = "InterfaceHandler";

    private static final List<EncoderEntry<?, ?>> ENCODERS = new ArrayList<>();
    private static final List<DecoderEntry<?, ?>> DECODERS = new ArrayList<>();

    static {
        addCodec(Object.class, InterfaceParcelable.class, new RemoteInterfaceCodec());
    }

    public static <S, R> void addCodec(Class<S> srcClass, Class<R> remoteClass, Codec<S, R> codec) {
        ENCODERS.add(new EncoderEntry<S, R>(srcClass, remoteClass, codec));
        DECODERS.add(new DecoderEntry<R, S>(remoteClass, srcClass, codec));
    }

    public static <S, R> void addEncoder(Class<S> srcClass, Class<R> remoteClass, Encoder<S, R> encoder) {
        ENCODERS.add(new EncoderEntry<S, R>(srcClass, remoteClass, encoder));
    }

    public static <R, S> void addDecoder(Class<R> remoteClass, Class<S> srcClass, Decoder<R, S> decoder) {
        DECODERS.add(new DecoderEntry<R, S>(remoteClass, srcClass, decoder));
    }

    private static Bundle bundle(InterfaceCallInfo callInfo) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("callInfo", callInfo);
        return bundle;
    }

    private static InterfaceCallInfo unbundle(Bundle bundle) {
        bundle.setClassLoader(AndInvoker.class.getClassLoader());
        InterfaceCallInfo callInfo = bundle.getParcelable("callInfo");
        return callInfo;
    }

    private static void handleEncodeParams(Object[] params, Class[] types, Annotation[][] annotations) {
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] != null) {
                    params[i] = encodeInternal(params[i], types[i], annotations[i]);
                }
            }
        }
    }

    private static void handleDecodeParams(Object[] params, Class[] types, Annotation[][] annotations) {
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] != null) {
                    params[i] = decodeInternal(params[i], types[i], annotations[i]);
                }
            }
        }
    }

    private static <S> Object encodeInternal(S obj, Class<S> srcClass, Annotation[] annotations) {
        for (EncoderEntry<?, ?> encoderEntry : ENCODERS) {
            if (encoderEntry.handles(obj, srcClass)) {
                Encoder<S, ?> encoder = (Encoder<S, ?>) encoderEntry.encoder;
                if (encoder.handleEncode(obj, srcClass, annotations)) {
                    Object result = encoder.encode(obj, srcClass);
                    LogUtil.d(TAG, "encodeInternal() obj = %s, srcClass = %s  ->  result = %s",
                            obj, srcClass, result);
                    return result;
                }
            }
        }

        return obj;
    }

    private static <R, S> S decodeInternal(R obj, Class<S> srcClass, Annotation[] annotations) {
        for (DecoderEntry<?, ?> decoderEntry : DECODERS) {
            Class<?> remoteClass = obj.getClass();
            if (decoderEntry.handles(obj, remoteClass, srcClass)) {
                Decoder<R, S> decoder = (Decoder<R, S>) decoderEntry.decoder;
                if (decoder.handleDecode(obj, srcClass, annotations)) {
                    S result = decoder.decode(obj, srcClass);
                    LogUtil.d(TAG, "decodeInternal() obj = %s, remoteClass = %s, " +
                            "srcClass = %s  ->  %s", obj, remoteClass, srcClass, result);
                    return result;
                }
            }
        }

        return (S) obj;
    }

    static <T> Call buildStub(final InterfaceInfo<T> interfaceInfo) {
        LogUtil.d(TAG, "buildStub() for interfaceInfo = %s", interfaceInfo);

        CallWrapper callWrapper = new CallWrapper() {
            @Override
            public Bundle onCall(Bundle params) {
                InterfaceCallInfo callInfo = InterfaceHandler.unbundle(params);
                if (callInfo != null) {
                    String methodName = callInfo.methodName;
                    Method method = interfaceInfo.fetchMethod(methodName);
                    LogUtil.d(TAG, "interface call: callInfo = %s, interfaceInfo = %s, method = %s",
                            callInfo, interfaceInfo, method);
                    if (method != null) {
                        try {
                            InterfaceHandler.handleDecodeParams(callInfo.args,
                                    method.getParameterTypes(), method.getParameterAnnotations());

                            Object result = method.invoke(interfaceInfo.object, callInfo.args);

                            Object[] resultArgs = new Object[]{result};
                            InterfaceHandler.handleEncodeParams(resultArgs, new Class[]{method.getReturnType()},
                                    new Annotation[][]{method.getAnnotations()});
                            InterfaceCallInfo resultInfo = new InterfaceCallInfo(callInfo);
                            resultInfo.args = resultArgs;
                            return InterfaceHandler.bundle(resultInfo);
                        } catch (Throwable t) {
                            throw new InvokeException(t);
                        }
                    }
                }

                throw new InvokeException(String.format("no remote interface methods found for %s",
                        callInfo));
            }
        };

        return callWrapper;
    }

    static <T> T buildProxy(final InterfaceInfo<T> interfaceInfo, final Call call) {
        if (call != null) {
            LogUtil.d(TAG, "buildProxy() for interfaceInfo = %s, call = %s", interfaceInfo, call);
            InvocationHandler invocationHandler = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    try {
                        LogUtil.d(TAG, "invoke on method = %s, args = %s", method, args);
                        IBinder remoteBinder = call.asBinder();
                        if (method.getDeclaringClass() == Object.class) {
                            return method.invoke(remoteBinder, args);
                        }

                        if (method.getDeclaringClass() == _IRemote.class) {
                            // return binder anyway
                            return remoteBinder;
                        }

                        handleEncodeParams(args, method.getParameterTypes(), method.getParameterAnnotations());
                        InterfaceCallInfo callInfo = new InterfaceCallInfo(null,
                                interfaceInfo.interfaceClass.getName(), method.getName(), args);
                        LogUtil.d(TAG, "interface proxy call: callInfo = %s", callInfo);
                        Bundle bundle = InterfaceHandler.bundle(callInfo);
                        Bundle result = call.onCall(bundle);
                        InterfaceCallInfo resultCallInfo = InterfaceHandler.unbundle(result);
                        if (resultCallInfo != null) {
                            Object[] resultObj = resultCallInfo.args;
                            if (resultObj != null && resultObj.length == 1) {
                                handleDecodeParams(resultObj, new Class[]{method.getReturnType()},
                                        new Annotation[][]{method.getAnnotations()});
                                return resultObj[0];
                            } else {
                                return null;
                            }
                        }
                    } catch (RemoteException e) {
                        if (LogUtil.LOG_ENABLED) {
                            e.printStackTrace();
                        }
                        throw new InvokeException(e);
                    } catch (Throwable t) {
                        if (LogUtil.LOG_ENABLED) {
                            t.printStackTrace();
                        }
                        throw new InvokeException(t);
                    }

                    throw new InvokeException("remote interface invoke error!");
                }
            };
            try {
                T instance = (T) Proxy.newProxyInstance(AndInvoker.class.getClassLoader(),
                        new Class[]{interfaceInfo.interfaceClass, _IRemote.class}, invocationHandler);
                return instance;
            } catch (Exception e) {
                throw new InvokeException("remote interface proxy init error!");
            }
        }
        return null;
    }

    private static class EncoderEntry<S, R> {
        private final Class<S> srcClass;
        private final Class<R> remoteClass;
        private final Encoder<S, R> encoder;

        public EncoderEntry(Class<S> srcClass, Class<R> remoteClass,
                Encoder<S, R> encoder) {
            this.srcClass = srcClass;
            this.remoteClass = remoteClass;
            this.encoder = encoder;
        }

        public boolean handles(Object src, Class<?> srcClass) {
            return this.srcClass.isAssignableFrom(srcClass);
        }
    }

    private static class DecoderEntry<R, S> {
        private final Class<S> srcClass;
        private final Class<R> remoteClass;
        private final Decoder<R, S> decoder;

        public DecoderEntry(Class<R> remoteClass, Class<S> srcClass,
                Decoder<R, S> encoder) {
            this.srcClass = srcClass;
            this.remoteClass = remoteClass;
            this.decoder = encoder;
        }

        public boolean handles(Object remoteObj, Class<?> remoteClass, Class<?> srcClass) {
            return this.remoteClass.isAssignableFrom(remoteClass) &&
                    this.srcClass.isAssignableFrom(srcClass);
        }
    }
}

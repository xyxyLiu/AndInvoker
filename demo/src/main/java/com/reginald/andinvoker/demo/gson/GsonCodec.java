package com.reginald.andinvoker.demo.gson;

import com.google.gson.Gson;
import com.reginald.andinvoker.api.Codec;

import java.lang.annotation.Annotation;

public class GsonCodec implements Codec<Object, String> {
    private static final String TAG = "GsonCodec";

    @Override
    public boolean handleDecode(String obj, Class<Object> clazz, Annotation[] annotations) {
        return handles(clazz, annotations);
    }

    @Override
    public Object decode(String obj, Class<Object> clazz) {
        Gson gson = new Gson();
        return gson.fromJson(obj, clazz);
    }

    @Override
    public boolean handleEncode(Object src, Class<Object> clazz, Annotation[] annotations) {
        return handles(clazz, annotations);
    }

    @Override
    public String encode(Object src, Class<Object> clazz) {
        Gson gson = new Gson();
        return gson.toJson(src, clazz);
    }

    private static boolean handles(Class<Object> clazz, Annotation[] annotations) {
        if (clazz.isPrimitive()) {
            return false;
        }

        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof MyGson) {
                    return true;
                }
            }
        }

        return false;
    }
}

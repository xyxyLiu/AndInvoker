package com.reginald.andinvoker.api;

import java.lang.annotation.Annotation;

/**
 * a decoder which deserialize a object from remote serialized class type R to src class type S
 * @param <R> serialized class type
 * @param <S> src class type
 */
public interface Decoder<R, S> {

    /**
     * check whether the object can be handled by this Decoder
     * @param obj remote serialized object
     * @param clazz desired src class type
     * @param annotations annotation of parameter in method.
     * @return can be handled
     */
    boolean handleDecode(R obj, Class<S> clazz, Annotation[] annotations);

    /**
     * deserialize
     * @param obj remote serialized object
     * @param clazz desired src class type
     * @return deserialized object
     */
    S decode(R obj, Class<S> clazz);
}

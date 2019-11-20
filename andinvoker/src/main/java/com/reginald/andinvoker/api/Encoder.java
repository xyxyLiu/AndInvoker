package com.reginald.andinvoker.api;

import java.lang.annotation.Annotation;

/**
 * a encoder which serialize a object from src class type S to serialized class type R
 * @param <S> src class type
 * @param <R> serialized class type
 */
public interface Encoder<S, R> {
    /**
     * check whether the src can be handled by this Encoder
     * @param src src object
     * @param clazz src class type
     * @param annotations annotation of parameter in method.
     * @return can be handled
     */
    boolean handleEncode(S src, Class<S> clazz, Annotation[] annotations);

    /**
     * serialize
     * @param src src object
     * @param clazz src class type
     * @return serialized object
     */
    R encode(S src, Class<S> clazz);
}




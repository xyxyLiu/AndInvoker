package com.reginald.andinvoker.api;

/**
 * an Encoder/Decoder which can serialize/deserialize custom class.
 * @param <S> src class type
 * @param <R> serialized class type
 */
public interface Codec<S, R> extends Encoder<S, R>, Decoder<R, S> {
}

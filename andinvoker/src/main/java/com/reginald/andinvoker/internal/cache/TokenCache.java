package com.reginald.andinvoker.internal.cache;

import com.reginald.andinvoker.LogUtil;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

public class TokenCache<K, V> {
    private static final String TAG = "TokenCache";

    private final WeakHashMap<K, WeakReference<V>> mCache = new WeakHashMap<>();
    private final Object mLock = new Object();

    private String mName;
    private int mMaxCacheSize;

    public static <K, V> TokenCache<K, V> build(String name) {
        return new TokenCache<>(name, 1024);
    }

    private TokenCache(String name, int maxSize) {
        mName = name;
        mMaxCacheSize = maxSize;
    }

    private String getTag() {
        return TAG + "(" + mName + ")";
    }

    public V get(K key, Loader<V> loader) {
        synchronized (mLock) {
            WeakReference<V> valueRef = mCache.get(key);
            LogUtil.d(getTag(), "get cache item: key = %s, valueRef = %s, size = %d",
                    key, valueRef, mCache.size());
            V value = valueRef != null ? valueRef.get() : null;

            if (value != null) {
                return value;
            }

            if (loader != null) {
                value = loader.load();
                if (value != null) {
                    if (mCache.size() < mMaxCacheSize) {
                        mCache.put(key, new WeakReference<>(value));
                    } else {
                        LogUtil.e(getTag(), "cache exceeded max %d!", mMaxCacheSize);
                    }
                }
            }

            return value;
        }
    }

    public V remove(K key) {
        synchronized (mLock) {
            WeakReference<V> removedRef = mCache.remove(key);
            return removedRef != null ? removedRef.get() : null;
        }
    }

    public interface Loader<T> {
        T load();
    }
}

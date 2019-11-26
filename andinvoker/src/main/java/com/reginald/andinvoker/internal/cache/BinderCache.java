package com.reginald.andinvoker.internal.cache;

import android.os.IBinder;
import android.os.RemoteException;

import com.reginald.andinvoker.LogUtil;

import java.util.HashMap;

public class BinderCache<T> {
    private static final String TAG = "BinderCache";

    private final HashMap<String, BinderRecord> mRemoteServiceCache = new HashMap<>();
    private final String mName;

    public static <T> BinderCache<T> noCache() {
        return new BinderCache<T>("NOCACHE") {
            @Override
            public T get(String key, Loader<T> loader) {
                return loader.load();
            }
        };
    }

    public BinderCache(String name) {
        mName = name;
    }

    public T get(String key, Loader<T> loader) {
        synchronized (mRemoteServiceCache) {
            BinderRecord br = mRemoteServiceCache.get(key);
            if (br != null) {
                IBinder binder = toBinder(br.binderable);
                if (binder != null && binder.isBinderAlive()) {
                    LogUtil.d(getTag(), "get cached for " + key + " value = " + br.binderable);
                    return br.binderable;
                } else {
                    mRemoteServiceCache.remove(key);
                }
            }

            T binderable = loader.load();
            LogUtil.d(getTag(), "create for " + key + " value = " + binderable);
            if (binderable != null) {
                br = new BinderRecord(binderable);
                try {
                    br.linkToDeath();
                    mRemoteServiceCache.put(key, br);
                    return binderable;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    public IBinder toBinder(T value) {
        if (value instanceof IBinder) {
            return (IBinder) value;
        }
        return null;
    }

    public void clear() {
        synchronized (mRemoteServiceCache) {
            mRemoteServiceCache.clear();
        }
    }

    private String getTag() {
        return TAG + "(" + mName + ")";
    }

    private class BinderRecord {
        public T binderable;
        private BinderDeath mBd;

        public BinderRecord(T binderable) {
            this.binderable = binderable;
        }

        public void linkToDeath() throws RemoteException {
            synchronized (this) {
                if (binderable != null && mBd == null) {
                    mBd = new BinderDeath(this);
                    IBinder binder = toBinder(binderable);
                    if (binder != null) {
                        binder.linkToDeath(mBd, 0);
                    }
                }
            }
        }

        public void unlinkToDeath() {
            synchronized (this) {
                IBinder binder = toBinder(binderable);
                if (binder != null && mBd != null) {
                    binder.unlinkToDeath(mBd, 0);
                    binderable = null;
                    mBd = null;
                }
            }
        }

        private class BinderDeath implements IBinder.DeathRecipient {
            private final BinderRecord mBr;

            BinderDeath(BinderRecord br) {
                mBr = br;
            }

            public void binderDied() {
                synchronized (mBr) {
                    IBinder binder = toBinder(mBr.binderable);
                    if (binder == null) {
                        return;
                    }

                    binder.unlinkToDeath(this, 0);
                    mBr.binderable = null;
                }
            }
        }
    }

    public interface Loader<T> {
        T load();
    }
}
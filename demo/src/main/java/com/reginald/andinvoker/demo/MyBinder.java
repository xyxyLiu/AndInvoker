package com.reginald.andinvoker.demo;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

public class MyBinder extends IMyBinder.Stub {

    private Context mContext;

    public MyBinder(Context context) {
        mContext = context;
    }

    @Override
    public boolean myMethod(String inParam, String[] outParam) throws RemoteException {

        Log.d(CommonUtils.getTag(mContext), String.format("onInvoke() in [process %s] : inParam = %s, outParam = %s",
                CommonUtils.getCurrentProcessName(mContext), inParam, outParam));

        if (outParam != null && outParam.length >= 1) {
            outParam[0] = "received";
        }

        return true;
    }

}

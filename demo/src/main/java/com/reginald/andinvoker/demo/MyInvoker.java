package com.reginald.andinvoker.demo;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.reginald.andinvoker.api.ICall;
import com.reginald.andinvoker.api.IInvoker;

import static com.reginald.andinvoker.demo.CommonUtils.getTag;

public class MyInvoker implements IInvoker {
    @Override
    public Bundle onInvoke(Context context, String methodName, Bundle params, ICall callback) {
        Log.d(getTag(context), String.format("onInvoke() in [process %s] : methodName = %s, params = %s, callback = %s",
                CommonUtils.getCurrentProcessName(context), methodName, unparse(params), callback));
        if (callback != null) {
            Bundle data = new Bundle();
            data.putString("param", "callback from " + getTag(context));
            callback.onCall(data);
        }
        return new Bundle();
    }


    private static Bundle unparse(Bundle bundle) {
        bundle.size();
        return bundle;
    }
}
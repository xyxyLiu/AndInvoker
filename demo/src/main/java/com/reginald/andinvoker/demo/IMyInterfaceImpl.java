package com.reginald.andinvoker.demo;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

public class IMyInterfaceImpl implements IMyInterface {
    private Context mContext;

    public IMyInterfaceImpl(Context context) {
        mContext = context;
    }

    @Override
    public String testBasicTypes(int i, long l, String s, MyItem item, Bundle bundle,
            IBinder binder, List<Boolean> list) {
        Log.d(CommonUtils.getTag(mContext), String.format("testBasicTypes() in [process %s] : " +
                        "i = %d, l = %d, s = %s, item = %s, binder = %s, list = %s",
                CommonUtils.getCurrentProcessName(mContext), i, l, s, item, binder, list));

        return "return_" + s;
    }

    @Override
    public void testVoid() {
        Log.d(CommonUtils.getTag(mContext), String.format("testVoid() in [process %s] : ",
                CommonUtils.getCurrentProcessName(mContext)));
    }

    @Override
    public IMyInterface testInterface(IMyInterface paramInterface) {
        Log.d(CommonUtils.getTag(mContext), String.format("testInterface() in [process %s] : " +
                        "paramInterface = %s",
                CommonUtils.getCurrentProcessName(mContext), paramInterface));

        paramInterface.testBasicTypes(2, 3L, "test paramInterface",
                new MyItem("test MyItem"), CommonUtils.newBundle("test bundle"),
                new MyBinder(mContext), null);

        return new IMyInterfaceImpl(mContext) {
            @Override
            public String testBasicTypes(int i, long l, String s, MyItem item, Bundle bundle,
                    IBinder binder, List<Boolean> list) {
                Log.d(CommonUtils.getTag(mContext), String.format("testInterface() return in [process %s] :" +
                                "i = %d, l = %d, s = %s, item = %s, bundle = %s, binder = %s, list = %s",
                        CommonUtils.getCurrentProcessName(mContext), i, l, s, item,
                        CommonUtils.unparse(bundle), binder, list));

                return "interface_return_" + s;
            }
        };
    }
}

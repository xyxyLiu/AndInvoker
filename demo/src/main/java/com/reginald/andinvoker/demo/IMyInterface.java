package com.reginald.andinvoker.demo;

import android.os.Bundle;
import android.os.IBinder;

import com.reginald.andinvoker.api.RemoteInterface;
import com.reginald.andinvoker.demo.gson.MyGson;

import java.util.List;

@RemoteInterface
public interface IMyInterface {

    String testBasicTypes(int i, long l, String s, @MyGson MyItem item, Bundle bundle,
            IBinder binder, List<Boolean> list);

    void testVoid();

    IMyInterface testInterface(IMyInterface paramInterface);
}

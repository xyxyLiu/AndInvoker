package com.reginald.andinvoker.demo;

import android.app.Application;
import android.content.Context;
import android.os.IBinder;
import android.util.Log;

import com.reginald.andinvoker.AndInvoker;
import com.reginald.andinvoker.api.IServiceFetcher;
import com.reginald.andinvoker.demo.gson.GsonCodec;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    private String remoteProcessSuffix() {
        String processSuffix = CommonUtils.getCurrentProcessName(this);
        String localProcess = processSuffix.replace(getPackageName(), "");
        Log.d(TAG, "onCreate() localProcess " + localProcess);
        if (localProcess.equals(Providers.A.PROCESS)) {
            return Providers.B.SUFFIX;
        } else if (localProcess.equals(Providers.B.PROCESS)) {
            return Providers.A.SUFFIX;
        }

        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerMyServices();
        Log.d(TAG, "onCreate() in " + CommonUtils.getTag(this));
    }

    private void registerMyServices() {
        String remoteProcess = remoteProcessSuffix();
        Log.d(TAG, "registerMyServices() remoteProcess = " + remoteProcess);

        // register custom gson codec
        AndInvoker.appendCodec(Object.class, String.class, new GsonCodec());

        // register binder service in local process
        AndInvoker.registerService("serviceName_binder", new IServiceFetcher() {
            @Override
            public IBinder onFetchService(Context context) {
                return new MyBinder(context);
            }
        });

        // register binder service in remote process is not recommended in practice
        AndInvoker.registerRemoteService(this, this.getPackageName() + remoteProcess,
                "serviceName_binder_remoteRegister_to_" + remoteProcess,
                new MyBinder(this));

        // register invoker in local process
        AndInvoker.registerInvoker("serviceName_invoker", MyInvoker.class);

        // register invoker in remote process is not recommended in practice
        AndInvoker.registerRemoteInvoker(this, this.getPackageName() + remoteProcess,
                "serviceName_invoker_remoteRegister_to_" + remoteProcess,
                new MyInvoker());

        // register interface in local process
        AndInvoker.registerInterface("serviceName_interface",
                new IMyInterfaceImpl(this), IMyInterface.class);

        // register interface in remote process
        AndInvoker.registerRemoteInterface(this, this.getPackageName() + remoteProcess, "serviceName_interface_remoteRegister_to_" + remoteProcess,
                new IMyInterfaceImpl(this), IMyInterface.class);
    }
}

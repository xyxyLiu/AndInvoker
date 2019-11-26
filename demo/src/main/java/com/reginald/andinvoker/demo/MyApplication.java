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

    private String localProcessSuffix() {
        String processSuffix = CommonUtils.getCurrentProcessName(this);
        String localProcess = processSuffix.replace(getPackageName(), "");
        if (localProcess.equals(Providers.A.PROCESS)) {
            return Providers.A.SUFFIX;
        } else if (localProcess.equals(Providers.B.PROCESS)) {
            return Providers.B.SUFFIX;
        }

        return null;
    }

    private String remoteProcessSuffix() {
        String localProcess = localProcessSuffix();
        Log.d(TAG, "onCreate() localProcess " + localProcess);
        if (localProcess.equals(Providers.A.SUFFIX)) {
            return Providers.B.SUFFIX;
        } else if (localProcess.equals(Providers.B.SUFFIX)) {
            return Providers.A.SUFFIX;
        }

        return null;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.d(TAG, "attachBaseContext() in " + CommonUtils.getTag(this));

        // register your local service here so that remote clients can fetch the service when the process starting.
        registerMyLocalServices();

        // NEVER register remote services in attachBaseContext() !!!
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() in " + CommonUtils.getTag(this));

        // register remote services
        registerMyRemoteServices();
    }

    private void registerMyLocalServices() {
        String localProcess = localProcessSuffix();
        Log.d(TAG, "registerMyLocalServices() localProcess = " + localProcess);

        // register custom gson codec
        AndInvoker.appendCodec(Object.class, String.class, new GsonCodec());
        //AndInvoker.noClientCache();

        // register binder service in local process
        AndInvoker.registerService(this, this.getPackageName() + localProcess,
                "serviceName_binder", new IServiceFetcher() {
                    @Override
                    public IBinder onFetchService(Context context) {
                        return new MyBinder(context);
                    }
                });

        // register invoker in local process
        AndInvoker.registerInvoker(this, this.getPackageName() + localProcess,
                "serviceName_invoker", MyInvoker.class);

        // register interface in local process
        AndInvoker.registerInterface(this, this.getPackageName() + localProcess,
                "serviceName_interface", new IMyInterfaceImpl(this), IMyInterface.class);
    }

    private void registerMyRemoteServices() {
        String remoteProcess = remoteProcessSuffix();
        Log.d(TAG, "registerMyRemoteServices() remoteProcess = " + remoteProcess);

        // register binder service in remote process is not recommended in practice
        AndInvoker.registerService(this, this.getPackageName() + remoteProcess,
                "serviceName_binder_remoteRegister_to_" + remoteProcess,
                new MyBinder(this));

        // register invoker in remote process is not recommended in practice
        AndInvoker.registerInvoker(this, this.getPackageName() + remoteProcess,
                "serviceName_invoker_remoteRegister_to_" + remoteProcess,
                new MyInvoker());

        // register interface in remote process
        AndInvoker.registerInterface(this, this.getPackageName() + remoteProcess, "serviceName_interface_remoteRegister_to_" + remoteProcess,
                new IMyInterfaceImpl(this), IMyInterface.class);


        // TODO BINDER INTERFACE 效率对比！！！
    }
}

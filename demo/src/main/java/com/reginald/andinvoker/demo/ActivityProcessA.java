package com.reginald.andinvoker.demo;

import android.os.Bundle;

import com.reginald.andinvoker.AndInvoker;

public class ActivityProcessA extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // register invoker in local process
        AndInvoker.registerInvoker("serviceName", MyInvoker.class);

        // register invoker in remote process is not recommended in practice
        AndInvoker.registerRemoteInvoker(this, getPackageName() + ".process.b",
                "serviceName_remoteRegister", new MyInvoker());
    }
}

package com.reginald.andinvoker.demo;

import android.os.Bundle;

import com.reginald.andinvoker.AndInvoker;

public class ActivityProcessB extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndInvoker.registerInvoker(this, getPackageName() + ".process.b",
                "serviceName", new BaseInvoker());
    }
}

package com.reginald.andinvoker.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.reginald.andinvoker.AndInvoker;
import com.reginald.andinvoker.InvokeException;
import com.reginald.andinvoker.api.ICall;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class BaseActivity extends Activity {
    private static final int INVOKE_TIMES = 1;
    private static AtomicInteger sInvokeTimes = new AtomicInteger(0);

    private IMyInterface localInterface;
    private IMyInterface remoteInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        localInterface = new IMyInterfaceImpl(BaseActivity.this);

        setContentView(R.layout.activity_base);

        TextView title = findViewById(R.id.title);
        title.setText(getTag());

        Button btn1 = findViewById(R.id.btn1);
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(BaseActivity.this, ActivityProcessA.class);
                startActivity(intent);
            }
        });

        Button btn2 = findViewById(R.id.btn2);
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(BaseActivity.this, ActivityProcessB.class);
                startActivity(intent);
            }
        });
        Button btn3 = findViewById(R.id.btn3);
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testProcess(Providers.A.SUFFIX);
            }
        });
        Button btn4 = findViewById(R.id.btn4);
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testProcess(Providers.B.SUFFIX);
            }
        });

        Button btn5 = findViewById(R.id.btn5);
        btn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Process.killProcess(Process.myPid());
            }
        });

        Button btn6 = findViewById(R.id.btn6);
        btn6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testUnregister(Providers.A.SUFFIX);
            }
        });
    }

    private void testProcess(String processSuffix) {
        for (int i = 0; i < INVOKE_TIMES; i++) {
            String provider = getPackageName() + processSuffix;
            invoke("serviceName_invoker", provider);
            invoke("serviceName_invoker_remoteRegister_to_" + processSuffix, provider);
            invokeInterface("serviceName_interface", provider);
            invokeInterface("serviceName_interface_remoteRegister_to_" + processSuffix, provider);
            fetchBinder("serviceName_binder", provider);
            fetchBinder("serviceName_binder_remoteRegister_to_" + processSuffix, provider);
        }
    }

    private void testUnregister(String processSuffix) {
        String provider = getPackageName() + processSuffix;

        if (!AndInvoker.unregisterServiceNoThrow(BaseActivity.this, provider,
                "serviceName_binder")) {
            Log.e(getTag(), String.format("unregisterService in error!provider = %s, serviceName = %s",
                    provider, "serviceName_binder"));
        }

        if (!AndInvoker.unregisterInvokerNoThrow(BaseActivity.this, provider, "serviceName_invoker")) {
            Log.e(getTag(), String.format("unregisterInvoker in error! provider = %s, serviceName = %s",
                    provider, "serviceName_invoker"));
        }

        if (!AndInvoker.unregisterInterfaceNoThrow(BaseActivity.this, provider, "serviceName_interface")) {
            Log.e(getTag(), String.format("unregisterInterfaceerror! provider = %s, serviceName = %s",
                    provider, "serviceName_interface"));
        }
    }

    private void fetchBinder(final String serviceName, final String provider) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                IBinder binderService = AndInvoker.fetchServiceNoThrow(BaseActivity.this,
                        provider, serviceName);
                if (binderService == null) {
                    Log.e(getTag(), String.format("fetchBinder() fetch ERROR : provider = %s, serviceName = %s",
                            provider, serviceName));
                }

                IMyBinder myBinder = IMyBinder.Stub.asInterface(binderService);
                if (myBinder != null) {
                    String inParam = "inParam";
                    String[] outParam = new String[1];
                    try {
                        boolean result = myBinder.myMethod(inParam, outParam);
                        Log.d(getTag(), String.format("fetchBinder() binder call: provider = %s, " +
                                        "serviceName = %s, myBinder = %s(remote: %s), inParam = %s, outParam = %s, result = %s",
                                provider, serviceName, myBinder, myBinder.asBinder(), inParam, outParam, result));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

    }

    private void invoke(final String serviceName, final String provider) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bundle params = new Bundle();
                params.putString("param", "invoke from " + getTag());
                String methodName = "methodName";
                ICall callback = new ICall() {
                    @Override
                    public Bundle onCall(Bundle params) {
                        Log.d(getTag(), String.format("onCallback() in %s: params = %s",
                                getTag(), CommonUtils.unparse(params)));
                        return null;
                    }
                };

                int invokeTime = sInvokeTimes.addAndGet(1);
                Log.d(getTag(), String.format("invoke() start at (%d) : " +
                                "provider = %s, serviceName = %s, methodName = %s, params = %s, callback = %s",
                        invokeTime, provider, serviceName, methodName, CommonUtils.unparse(params), callback));
                if (AndInvoker.invokeNoThrow(BaseActivity.this, provider, serviceName,
                        methodName, params, callback) == null) {
                    Log.e(getTag(), String.format("invoke() fetch ERROR : " +
                                    "provider = %s, serviceName = %s, methodName = %s, params = %s, callback = %s",
                            provider, serviceName, methodName, CommonUtils.unparse(params), callback));
                }

                Log.d(getTag(), String.format("invoke() finished at (%d) : " +
                                "provider = %s, serviceName = %s, methodName = %s, params = %s, callback = %s",
                        invokeTime, provider, serviceName, methodName, CommonUtils.unparse(params), callback));
            }
        }).start();

    }

    private void invokeInterface(final String serviceName, final String provider) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String interfaceName = serviceName;
                    IMyInterface myInterface = remoteInterface;
                    if (myInterface == null) {
                        myInterface = AndInvoker.fetchInterfaceNoThrow(BaseActivity.this, provider,
                                interfaceName, IMyInterface.class);
                        remoteInterface = myInterface;
                    }

                    Log.d(getTag(), String.format("invokeInterface() start : provider = %s, " +
                            "interfaceName = %s, interfaceObj = %s", provider, interfaceName, myInterface));
                    String result = null;
                    if (myInterface == null) {
                        Log.e(getTag(), String.format("invokeInterface() fetch ERROR: " +
                                "provider = %s, serviceName = %s", provider, serviceName));
                        return;
                    }
                    result = myInterface.testBasicTypes(1, 200L, "test", new MyItem("test"),
                            CommonUtils.newBundle("test bundle"),
                            new MyBinder(BaseActivity.this),
                            Arrays.asList(true, false, true));
                    Log.d(getTag(), String.format("invokeInterface() testBasicTypes finish: provider = %s, " +
                            "result = %s", provider, result));

                    myInterface.testVoid();
                    Log.d(getTag(), String.format("invokeInterface() testVoid finish", provider));


                    IMyInterface returnInterface = null;
                    returnInterface = myInterface.testInterface(localInterface);
                    returnInterface.testBasicTypes(1, 4L, "test result of testInterface",
                            new MyItem("test result of testInterface"),
                            CommonUtils.newBundle("test bundle"),
                            new MyBinder(BaseActivity.this), null);

                    Log.d(getTag(), String.format("invokeInterface() testInterface finish: provider = %s, returnInterface = %s",
                            provider, returnInterface));
                } catch (InvokeException e) {
                    e.printStackTrace();
                    Log.e(getTag(), String.format("invokeInterface() remote invoke ERROR: " +
                            "provider = %s, serviceName = %s", provider, serviceName));
                    remoteInterface = null;
                }
            }
        }).start();

    }

    private String getTag() {
        return CommonUtils.getTag(this);
    }

}

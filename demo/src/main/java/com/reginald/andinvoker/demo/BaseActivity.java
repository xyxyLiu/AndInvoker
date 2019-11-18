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
import com.reginald.andinvoker.api.IInvokeCallback;

import java.util.concurrent.atomic.AtomicInteger;

public class BaseActivity extends Activity {
    private static final int INVOKE_TIMES = 1;
    private static AtomicInteger sInvokeTimes = new AtomicInteger(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                for (int i = 0; i < INVOKE_TIMES; i++) {
                    invoke("serviceName", getPackageName() + ".process.a");
                    invoke("serviceName_remoteRegister_from_b",
                            getPackageName() + ".process.a");
                    fetchBinder("serviceName", getPackageName() + ".process.a");
                    fetchBinder("serviceName_remoteRegister_from_b", getPackageName() + ".process.a");
                }
            }
        });
        Button btn4 = findViewById(R.id.btn4);
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i = 0; i < INVOKE_TIMES; i++) {
                    invoke("serviceName", getPackageName() + ".process.b");
                    invoke("serviceName_remoteRegister_from_a",
                            getPackageName() + ".process.b");
                    fetchBinder("serviceName", getPackageName() + ".process.b");
                    fetchBinder("serviceName_remoteRegister_from_a",
                            getPackageName() + ".process.b");
                }
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
                String provider = getPackageName() + ".process.a";
                String serviceName = "serviceName";
                if (!AndInvoker.unregisterInvokerNoThrow(BaseActivity.this, provider, serviceName)) {
                    Log.e(getTag(), String.format("unregisterInvoker in [process %s] error!" +
                                    "provider = %s, serviceName = %s",
                            CommonUtils.getCurrentProcessName(BaseActivity.this), provider, serviceName));
                }
            }
        });
    }

    private void fetchBinder(final String serviceName, final String provider) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                IBinder binderService = AndInvoker.fetchServiceNoThrow(BaseActivity.this,
                        provider, serviceName);
                if (binderService == null) {
                    Log.e(getTag(), String.format("fetchBinder() ERROR in [process %s] : " +
                                    "provider = %s, serviceName = %s",
                            CommonUtils.getCurrentProcessName(BaseActivity.this), provider, serviceName));
                }

                IMyBinder myBinder = IMyBinder.Stub.asInterface(binderService);
                if (myBinder != null) {
                    String inParam = "inParam";
                    String[] outParam = new String[1];
                    try {
                        boolean result = myBinder.myMethod(inParam, outParam);
                        Log.d(getTag(), String.format("fetchBinder() binder call in [process %s] : provider = %s, " +
                                        "serviceName = %s, myBinder = %s(remote: %s), inParam = %s, outParam = %s, result = %s",
                                CommonUtils.getCurrentProcessName(BaseActivity.this), provider, serviceName,
                                myBinder, myBinder.asBinder(), inParam, outParam, result));
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
                IInvokeCallback callback = new IInvokeCallback() {
                    @Override
                    public Bundle onCallback(Bundle params) {
                        Log.d(getTag(), String.format("onCallback() in %s: params = %s",
                                getTag(), unparse(params)));
                        return null;
                    }
                };

                int invokeTime = sInvokeTimes.addAndGet(1);
                Log.d(getTag(), String.format("invoke() start in [process %s](%d) : " +
                                "provider = %s, serviceName = %s, methodName = %s, params = %s, callback = %s",
                        CommonUtils.getCurrentProcessName(BaseActivity.this), invokeTime,
                        provider, serviceName, methodName, unparse(params), callback));
                if (AndInvoker.invokeNoThrow(BaseActivity.this, provider, serviceName,
                        methodName, params, callback) == null) {
                    Log.e(getTag(), String.format("invoke() ERROR in [process %s] : " +
                                    "provider = %s, serviceName = %s, methodName = %s, params = %s, callback = %s",
                            CommonUtils.getCurrentProcessName(BaseActivity.this), provider, serviceName, methodName, unparse(params), callback));
                }

                Log.d(getTag(), String.format("invoke() finished in [process %s](%d) : " +
                                "provider = %s, serviceName = %s, methodName = %s, params = %s, callback = %s",
                        CommonUtils.getCurrentProcessName(BaseActivity.this), invokeTime,
                        provider, serviceName, methodName, unparse(params), callback));
            }
        }).start();

    }

    private String getTag() {
        return CommonUtils.getTag(this);
    }

    private static Bundle unparse(Bundle bundle) {
        bundle.size();
        return bundle;
    }
}

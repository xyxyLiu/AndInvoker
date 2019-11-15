package com.reginald.andinvoker.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.reginald.andinvoker.AndInvoker;
import com.reginald.andinvoker.api.IInvokeCallback;

public class BaseActivity extends Activity {

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
                invoke("serviceName", getPackageName() + ".process.a");
                invoke("serviceName_remoteRegister", getPackageName() + ".process.a");
            }
        });
        Button btn4 = findViewById(R.id.btn4);
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                invoke("serviceName", getPackageName() + ".process.b");
                invoke("serviceName_remoteRegister", getPackageName() + ".process.b");
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

    private void invoke(String serviceName, String provider) {
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
        Log.d(getTag(), String.format("invoke() in [process %s] : " +
                        "provider = %s, serviceName = %s, methodName = %s, params = %s, callback = %s",
                CommonUtils.getCurrentProcessName(this), provider, serviceName, methodName, unparse(params), callback));
        if (AndInvoker.invokeNoThrow(BaseActivity.this, provider, serviceName,
                methodName, params, callback) == null) {
            Log.e(getTag(), String.format("invoke() ERROR in [process %s] : " +
                            "provider = %s, serviceName = %s, methodName = %s, params = %s, callback = %s",
                    CommonUtils.getCurrentProcessName(this), provider, serviceName, methodName, unparse(params), callback));
        }
    }

    private String getTag() {
        return getTag(this);
    }

    private static String getTag(Context context) {
        return "process[" + CommonUtils.getCurrentProcessName(context) + "]";
    }

    private static Bundle unparse(Bundle bundle) {
        bundle.size();
        return bundle;
    }
}

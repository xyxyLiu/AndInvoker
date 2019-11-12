package com.reginald.andinvoker.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.reginald.andinvoker.AndInvoker;
import com.reginald.andinvoker.api.IInvokeCallback;
import com.reginald.andinvoker.api.IInvoker;

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
                invoke(getPackageName() + ".process.a");
            }
        });
        Button btn4 = findViewById(R.id.btn4);
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                invoke(getPackageName() + ".process.b");
            }
        });

        Button btn5 = findViewById(R.id.btn5);
        btn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Process.killProcess(Process.myPid());
            }
        });
    }

    private void invoke(String provider) {
        Bundle params = new Bundle();
        params.putString("param", "invoke from " + getTag());
        String serviceName = "serviceName";
        String methodName = "methodName";
        IInvokeCallback callback = new IInvokeCallback() {
            @Override
            public Bundle onCallback(Bundle params) {
                Log.d(getTag(), String.format("onCallback() in %s: params = %s",
                        getTag(), unparse(params)));
                return null;
            }
        };
        Log.d(getTag(), String.format("invoke() in [process %s] : serviceName = %s, methodName = %s, params = %s, callback = %s",
                CommonUtils.getCurrentProcessName(this), serviceName, methodName, unparse(params), callback));
        AndInvoker.invoke(BaseActivity.this, provider, serviceName,
                methodName, params, callback);
    }

    class BaseInvoker implements IInvoker {

        @Override
        public IBinder onServiceCreate(Context context) {
            return new Binder();
        }

        @Override
        public Bundle onInvoke(Context context, String methodName, Bundle params, IInvokeCallback callback) {
            Log.d(getTag(), String.format("onInvoke() in [process %s] : methodName = %s, params = %s, callback = %s",
                    CommonUtils.getCurrentProcessName(context), methodName, unparse(params), callback));
            if (callback != null) {
                Bundle data = new Bundle();
                data.putString("param", "callback from " + getTag());
                callback.onCallback(data);
            }
            return new Bundle();
        }
    }

    private String getTag() {
        return "process[" + CommonUtils.getCurrentProcessName(this) + "]";
    }

    private Bundle unparse(Bundle bundle) {
        bundle.size();
        return bundle;
    }
}

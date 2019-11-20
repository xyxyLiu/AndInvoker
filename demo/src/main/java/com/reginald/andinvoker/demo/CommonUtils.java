package com.reginald.andinvoker.demo;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class CommonUtils {

    private static volatile String sProcessName;

    public static String getTag(Context context) {
        return "process[" + CommonUtils.getCurrentProcessName(context) + "]";
    }

    public static String getCurrentProcessName(Context cxt) {
        String processName = sProcessName;
        if (processName == null) {
            // try AMS first
            final int pid = android.os.Process.myPid();
            processName = getProcessNameByActivityManager(cxt, pid);
            if (!TextUtils.isEmpty(processName)) {
                return processName;
            }

            // try "/proc"
            processName = getProcessNameByProc(pid);
            sProcessName = processName;
        }

        return processName;
    }

    private static String getProcessNameByActivityManager(Context cxt, int pid) {
        ActivityManager am = getActivityManager(cxt);
        List<ActivityManager.RunningAppProcessInfo> runningApps = getRunningAppProcesses(am);
        for (ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
            if (procInfo.pid == pid) {
                return procInfo.processName;
            }
        }
        return null;
    }

    private static String getProcessNameByProc(int pid) {
        String cmdlineFile = "/proc/" + pid + "/cmdline";
        String processName = readFileAsString(cmdlineFile);
        if (processName != null) {
            processName = processName.trim();
        }
        return processName;
    }

    private static ActivityManager getActivityManager(Context context) {
        ActivityManager am = null;
        try {
            am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        } catch (Throwable e) {
            // sometimes RuntimeException may be thrown caused by Activity manager has died
        }
        return am;
    }

    private static List<ActivityManager.RunningAppProcessInfo> getRunningAppProcesses(ActivityManager am) {
        List<ActivityManager.RunningAppProcessInfo> runProcessList = null;
        try {
            runProcessList = am.getRunningAppProcesses();
        } catch (Exception e) {
            // NullPointerException or IndexOutOfBoundsException may be thrown on some devices
            // in the implementation of ActivityManager#getRunningAppProcesses().
        }
        return runProcessList;
    }

    private static String readFileAsString(String filename) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filename);
            return readStreamAsString(fis);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(fis);
        }
        return null;
    }

    private static String readStreamAsString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder result = new StringBuilder();
        String line = null;

        boolean first = true;
        while ((line = reader.readLine()) != null) {
            if (!first) {
                result.append('\n');
            } else {
                first = false;
            }
            result.append(line);
        }

        return result.toString();
    }

    private static void close(Closeable target) {
        try {
            if (target != null) {
                target.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Bundle newBundle(String value) {
        Bundle bundle = new Bundle();
        bundle.putString("key", value);
        return bundle;
    }

    public static Bundle unparse(Bundle bundle) {
        bundle.size();
        return bundle;
    }

}

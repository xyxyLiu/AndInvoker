package com.reginald.andinvoker.internal;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;

public class LogUtil {
    public static boolean LOG_ENABLED = false;
    public static boolean ALWAYS_SHOW_ERROR = false;

    public static final String TAG = "AndInvoker";

    public static void d(String tag, String msg) {
        if (LOG_ENABLED) {
            Log.d(TAG, getLogMsg(tag, msg));
        }
    }

    public static void d(String tag, String formatMsg, Object... args) {
        if (LOG_ENABLED) {
            d(tag, String.format(formatMsg, args));
        }
    }

    public static void i(String tag, String msg) {
        if (LOG_ENABLED) {
            Log.i(TAG, getLogMsg(tag, msg));
        }
    }

    public static void i(String tag, String formatMsg, Object... args) {
        if (LOG_ENABLED) {
            i(tag, String.format(formatMsg, args));
        }
    }

    public static void w(String tag, String msg) {
        if (LOG_ENABLED) {
            Log.w(TAG, getLogMsg(tag, msg));
        }
    }

    public static void w(String tag, String formatMsg, Object... args) {
        if (LOG_ENABLED) {
            w(tag, String.format(formatMsg, args));
        }
    }

    public static void w(String tag, String msg, Throwable e) {
        if (LOG_ENABLED) {
            Log.w(TAG, getLogMsg(tag, msg + " Exception: " + getExceptionMsg(e)));
        }
    }

    public static void w(String tag, Throwable e, String formatMsg, Object... args) {
        if (LOG_ENABLED) {
            w(tag, String.format(formatMsg, args), e);
        }
    }

    public static void e(String tag, String msg) {
        if (LOG_ENABLED || ALWAYS_SHOW_ERROR) {
            Log.e(TAG, getLogMsg(tag, msg));
        }
    }

    public static void e(String tag, String formatMsg, Object... args) {
        if (LOG_ENABLED || ALWAYS_SHOW_ERROR) {
            e(tag, String.format(formatMsg, args));
        }
    }

    public static void e(String tag, String msg, Throwable e) {
        if (LOG_ENABLED || ALWAYS_SHOW_ERROR) {
            Log.e(TAG, getLogMsg(tag, msg + " Exception: " + getExceptionMsg(e)));
        }
    }

    public static void e(String tag, Throwable e, String formatMsg, Object... args) {
        if (LOG_ENABLED || ALWAYS_SHOW_ERROR) {
            e(tag, String.format(formatMsg, args), e);
        }
    }

    private static String getLogMsg(String subTag, String msg) {
        return "[" + subTag + "] " + msg;
    }

    private static String getExceptionMsg(Throwable e) {
        StringWriter sw = new StringWriter(1024);
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }
}

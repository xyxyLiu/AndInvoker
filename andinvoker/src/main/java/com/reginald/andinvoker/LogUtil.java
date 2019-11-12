package com.reginald.andinvoker;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by lxy on 18-3-5.
 */

class LogUtil {
    public static final boolean LOG_ENABLED = false;
    public static final boolean ALWAYS_SHOW_ERROR = true;
    /**
     * 默认的文库日志Tag标签
     */
    public static final String TAG = "AndInvoker";

    public static final SimpleDateFormat sSimpleDateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

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

    public static LogFileHandle initLog(String file) {
        LogUtil.d(TAG, "initLogHandle for %s", file);
        return new LogFileHandle(file);
    }

    public static class LogFileHandle {
        public final String file;
        public long size;

        private static final int BUFFER_SIZE = 4096;
        private BufferedWriter mWriter;

        private LogFileHandle(String filePath) {
            file = filePath;
            initWriter();
        }

        private void initWriter() {
            close();
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new FileWriter(file, true), BUFFER_SIZE);
                mWriter = writer;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void logd(String text) {
            write(false, text);
        }

        public void loge(String text) {
            write(true, text);
        }

        private void write(boolean isError, String text) {
            if (mWriter != null) {
                try {
                    String date = sSimpleDateFormat.format(new Date(System.currentTimeMillis()));
                    String content = String.format("%s%s  %s\n", date, isError ? "[Error]" : "", text);
                    size += content.length();
                    mWriter.write(content);
                    mWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    mWriter = null;
                    initWriter();
                }
            }
        }

        public void close() {
            if (mWriter != null) {
                try {
                    mWriter.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

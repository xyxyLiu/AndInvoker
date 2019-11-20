package com.reginald.andinvoker.demo;

import android.os.Process;

public class MyItem {

    public String name;
    public int srcPid;

    public MyItem(String name) {
        this.name = name;
        this.srcPid = Process.myPid();
    }

    @Override
    public String toString() {
        return String.format("MyItem{ name = %s, srcPid = %d}", name, srcPid);
    }
}

package com.reginald.andinvoker.demo;

import com.reginald.andinvoker.AndInvokerProvider;

public class Providers {

    public static class A extends AndInvokerProvider {
        public static final String PROCESS = ":a";
        public static final String SUFFIX = ".process.a";
    }

    public static class B extends AndInvokerProvider {
        public static final String PROCESS = ":b";
        public static final String SUFFIX = ".process.b";
    }

}

// IInvokeCallback.aidl
package com.reginald.andinvoker.demo;
// Declare any non-default types here with import statements

interface IMyBinder {
    boolean myMethod(in String inParam, out String[] outParam);
}

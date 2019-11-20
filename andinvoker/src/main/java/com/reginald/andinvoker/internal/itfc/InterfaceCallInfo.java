package com.reginald.andinvoker.internal.itfc;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

public class InterfaceCallInfo implements Parcelable {
    public String interfaceName;
    public String interfaceClass;
    public String methodName;

    public Object[] args;

    public static final Creator<InterfaceCallInfo> CREATOR = new Creator<InterfaceCallInfo>() {
        @Override
        public InterfaceCallInfo createFromParcel(Parcel parcel) {
            return new InterfaceCallInfo(parcel);
        }

        @Override
        public InterfaceCallInfo[] newArray(int i) {
            return new InterfaceCallInfo[0];
        }
    };

    public InterfaceCallInfo(String interfaceName, String interfaceClass, String methodName, Object[] args) {
        this.interfaceName = interfaceName;
        this.interfaceClass = interfaceClass;
        this.methodName = methodName;
        this.args = args;
    }

    public InterfaceCallInfo(InterfaceCallInfo callInfo) {
        this.interfaceName = callInfo.interfaceName;
        this.interfaceClass = callInfo.interfaceClass;
        this.methodName = callInfo.methodName;
        this.args = callInfo.args;
    }

    public InterfaceCallInfo(Parcel parcel) {
        interfaceName = parcel.readString();
        interfaceClass = parcel.readString();
        methodName = parcel.readString();
        args = parcel.readArray(InterfaceCallInfo.class.getClassLoader());
    }

    @Override
    public String toString() {
        return String.format("CallInfo [ interfaceName = %s, interfaceClass = %s, " +
                        "methodName = %s, args = %s ]",
                interfaceName, interfaceClass, methodName, args != null ? Arrays.asList(args) : null);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(interfaceName);
        parcel.writeString(interfaceClass);
        parcel.writeString(methodName);
        parcel.writeArray(args);
    }
}

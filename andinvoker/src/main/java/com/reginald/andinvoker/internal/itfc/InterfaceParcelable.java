package com.reginald.andinvoker.internal.itfc;

import android.os.IBinder;
import android.os.Parcel;

import com.reginald.andinvoker.internal.BinderParcelable;

public class InterfaceParcelable extends BinderParcelable {

    public InterfaceParcelable(IBinder iBinder) {
        super(iBinder);
    }

    public InterfaceParcelable(Parcel parcel) {
        super(parcel);
    }

    public static final Creator<BinderParcelable> CREATOR = new Creator<BinderParcelable>() {
        @Override
        public InterfaceParcelable createFromParcel(Parcel parcel) {
            return new InterfaceParcelable(parcel);
        }

        @Override
        public InterfaceParcelable[] newArray(int i) {
            return new InterfaceParcelable[0];
        }
    };
}

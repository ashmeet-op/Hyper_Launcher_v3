package net.kdt.pojavlaunch.extra;

import androidx.annotation.NonNull;


public interface ExtraListener<T> {


    @SuppressWarnings("SameReturnValue")
    boolean onValueSet(String key, @NonNull T value);

}

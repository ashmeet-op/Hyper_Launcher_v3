package net.kdt.pojavlaunch;

import androidx.annotation.Keep;


@Keep
public class Logger {

    public static native void appendToLog(String text);



    public static native void begin(String logFilePath);


    @Keep
    public interface eventLogListener {
        void onEventLogged(String text);
    }


    public static native void setLogListener(eventLogListener logListener);
}

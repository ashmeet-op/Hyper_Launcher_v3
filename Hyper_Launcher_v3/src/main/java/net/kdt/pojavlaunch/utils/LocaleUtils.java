package net.kdt.pojavlaunch.utils;


import static com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.DEFAULT_PREF;
import static com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_FORCE_ENGLISH;
import static com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences.PREF_LANGUAGE;

import android.content.*;
import android.content.res.*;
import android.os.Build;
import android.os.LocaleList;

import androidx.preference.*;
import java.util.*;

public class LocaleUtils extends ContextWrapper {

    public LocaleUtils(Context base) {
        super(base);
    }

    public static ContextWrapper setLocale(Context context) {
        if (DEFAULT_PREF == null) {
            DEFAULT_PREF = PreferenceManager.getDefaultSharedPreferences(context);



            PREF_FORCE_ENGLISH = DEFAULT_PREF.getBoolean("force_english", false);
            PREF_LANGUAGE = DEFAULT_PREF.getString("app_language", "en");
        }

        Locale locale;
        if (PREF_FORCE_ENGLISH) {
            locale = Locale.ENGLISH;
        } else if (PREF_LANGUAGE != null && !PREF_LANGUAGE.equals("system")) {
            if (PREF_LANGUAGE.contains("-")) {
                String[] parts = PREF_LANGUAGE.split("-");
                locale = new Locale(parts[0], parts[1]);
            } else {
                locale = new Locale(PREF_LANGUAGE);
            }
        } else {


            return new LocaleUtils(context);
        }

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();

        configuration.setLocale(locale);
        Locale.setDefault(locale);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            configuration.setLocales(localeList);
        }

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            context = context.createConfigurationContext(configuration);
        }

        return new LocaleUtils(context);
    }
}

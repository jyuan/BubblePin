package bubblepin.com.bubblepin.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceUtil {

    public static final String GUIDE_PAGE = "Show_Gulde_page";
    public static final String LOGIN_INFO = "is_login_in";

    /**
     * save boolean type of data into SharePreferences
     *
     * @param context the application or acitvity
     * @param key     unique key
     * @param value   the value belongs to key
     */
    public static void setBoolean(Context context, String key, boolean value) {
        SharedPreferences preferences = context.getSharedPreferences(
                "preference", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * get boolean type of data into SharePreferences
     *
     * @param context the application or acitvity
     * @param key     unique key
     * @return the result of the key
     */
    public static boolean getBoolean(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(
                "preference", Context.MODE_PRIVATE);
        return preferences.getBoolean(key, false);
    }
}

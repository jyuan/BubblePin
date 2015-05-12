package bubblepin.com.bubblepin;

import android.app.Activity;
import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;

import com.metaio.sdk.MetaioDebug;
import com.metaio.tools.io.AssetsManager;
import com.parse.Parse;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class MyApplication extends Application {

    private static final String PREFS_NAME = "BUBBLEPIN_APPLICATION";

    private List<Activity> activityList = new LinkedList<Activity>();

    // singleton
    // not thread safe
    private static MyApplication instance;

    public static MyApplication getInstance() {
        if (null == instance) {
            instance = new MyApplication();
        }
        return instance;
    }

    public void addActivity(Activity activity) {
        activityList.add(activity);
    }

    public void exit() {
        for (Activity activity : activityList) {
            activity.finish();
        }
        System.exit(0);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // use to initialize parse
        initParse();

        // use to initialize metaioSDK
        Log.i(PREFS_NAME, "initialize success.");
        AssetsExtracter task = new AssetsExtracter();
        task.execute(0);
    }

    // the code below is required and reference from Office Document
    private void initParse() {
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, getString(R.string.parse_applicationId), getString(R.string.parse_clientId));
    }

    private class AssetsExtracter extends AsyncTask<Integer, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Integer... params) {
            try {
                // Extract all assets except Menu. Overwrite existing files for debug build only.
                final String[] ignoreList = {"Menu", "webkit", "sounds", "images", "webkitsec"};
                AssetsManager.extractAllAssets(getApplicationContext(), "", ignoreList, BuildConfig.DEBUG);
            } catch (IOException e) {
                MetaioDebug.printStackTrace(Log.ERROR, e);
                return false;
            }
            return true;
        }
    }
}

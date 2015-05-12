package bubblepin.com.bubblepin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import bubblepin.com.bubblepin.adapter.ViewPagerAdapter;
import bubblepin.com.bubblepin.util.PreferenceUtil;


public class WelcomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        MyApplication.getInstance().addActivity(this);

        // initial page according to SharePreference
        boolean isLogin = PreferenceUtil.getBoolean(this, PreferenceUtil.LOGIN_INFO);
        Log.i(getClass().getSimpleName(), "does the user login? " + isLogin);
        if (isLogin && ParseUser.getCurrentUser() != null) {
            startActivity(new Intent(this, GoogleMapActivity.class));
            finish();
        } else {
            // does the first time to initial this application
            boolean isShow = PreferenceUtil.getBoolean(this, PreferenceUtil.GUIDE_PAGE);
            Log.i(getClass().getSimpleName(), "the first time to initial this application? " + isShow);
            if (isShow) {
                startActivity(new Intent(this, InitialActivity.class));
                finish();
            } else {
                initGuideViewPage();
            }
        }
        // test
//        initGuideViewPage();
    }

    /**
     * Reference online
     */
    private void initGuideViewPage() {
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        LayoutInflater inflater = LayoutInflater.from(this);

        List<View> viewList = new ArrayList<View>(4);
        viewList.add(inflater.inflate(R.layout.intro_page1, null));
        viewList.add(inflater.inflate(R.layout.intro_page2, null));
        viewList.add(inflater.inflate(R.layout.intro_page3, null));
        viewList.add(inflater.inflate(R.layout.intro_page4, null));

        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(this, viewList);
        viewPager.setAdapter(viewPagerAdapter);
    }

}

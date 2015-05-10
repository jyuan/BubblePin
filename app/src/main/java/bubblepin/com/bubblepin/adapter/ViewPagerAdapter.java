package bubblepin.com.bubblepin.adapter;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ImageView;

import java.util.List;

import bubblepin.com.bubblepin.InitialActivity;
import bubblepin.com.bubblepin.R;
import bubblepin.com.bubblepin.util.PreferenceUtil;

public class ViewPagerAdapter extends PagerAdapter {

    private List<View> viewList;
    private Activity context;

    public ViewPagerAdapter(Activity context, List<View> views) {
        this.viewList = views;
        this.context = context;
    }

    @Override
    public int getCount() {
        return viewList.size();
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == arg1;
    }

    @Override
    public int getItemPosition(Object object) {
        return super.getItemPosition(object);
    }

    @Override
    public void destroyItem(View arg0, int arg1, Object arg2) {
        ((ViewPager) arg0).removeView(viewList.get(arg1));
    }

    /**
     * instantiation pages, if it's the last page, get the button and setOnClickListener click event
     */
    @Override
    public Object instantiateItem(View view, int which) {
        ((ViewPager) view).addView(viewList.get(which), 0);

        if (which == viewList.size() - 1) {
            ImageView button = (ImageView) view.findViewById(R.id.intro_page_submit);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PreferenceUtil.setBoolean(context, PreferenceUtil.GUIDE_PAGE, true);
                    context.startActivity(new Intent(context, InitialActivity.class));
                    context.finish();
                }
            });
        }
        return viewList.get(which);
    }
}

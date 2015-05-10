package bubblepin.com.bubblepin.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import bubblepin.com.bubblepin.R;
import bubblepin.com.bubblepin.util.ParseUtil;

public class FilterAdapter extends BaseAdapter {

    public static final String CATEGORY_ID = "id";
    public static final String CATEGORY_NAME = "name";
    public static final String CATEGORY_SELECTED = "isSelected";

    private List<Map<String, Object>> list;
    private Context context;

    public FilterAdapter(Context context, List<Map<String, Object>> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        final Category category;
        if (null == convertView) {
            category = new Category();
            convertView = LayoutInflater.from(context).inflate(R.layout.filter_list_item, null);
            category.categoryName = (TextView) convertView.findViewById(R.id.filter_category_name);
            category.filterButton = (ImageView) convertView.findViewById(R.id.filter_choose_button);
            if (!(Boolean) list.get(position).get(CATEGORY_SELECTED)) {
                category.filterButton.setImageResource(R.drawable.filter_off_button);
            }
            convertView.setTag(category);
        } else {
            category = (Category) convertView.getTag();
        }
        category.categoryName.setText(String.valueOf(list.get(position).get(CATEGORY_NAME)));

        category.filterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if ((Boolean) list.get(position).get(CATEGORY_SELECTED)) {
                        category.filterButton.setImageResource(R.drawable.filter_off_button);
                        list.get(position).put(CATEGORY_SELECTED, false);
                        ParseUtil.updateSelectedFilter((String) list.get(position).get(CATEGORY_ID), false);
                    } else {
                        category.filterButton.setImageResource(R.drawable.filter_on_button);
                        list.get(position).put(CATEGORY_SELECTED, true);
                        ParseUtil.updateSelectedFilter((String) list.get(position).get(CATEGORY_ID), true);
                    }
                } catch (com.parse.ParseException e) {
                    Log.e(getClass().getSimpleName(), "set category error: " + e.getMessage());
                }
            }
        });
        return convertView;
    }

    public class Category {
        public ImageView filterButton;
        public TextView categoryName;
    }
}

package bubblepin.com.bubblepin.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;

import java.util.List;
import java.util.Map;

import bubblepin.com.bubblepin.R;
import bubblepin.com.bubblepin.util.ParseUtil;
import bubblepin.com.bubblepin.util.RoundImageView;

public class ContactAdapter extends BaseAdapter {

    private List<Map<String, Object>> list;
    private Context context;

    public ContactAdapter(Context context, List<Map<String, Object>> list) {
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
        final Friend friend;
        if (null == convertView) {
            friend = new Friend();
            convertView = LayoutInflater.from(context).inflate(R.layout.contact_list_item, null);
            friend.userPhoto = (RoundImageView) convertView.findViewById(R.id.contact_user_photo);
            friend.username = (TextView) convertView.findViewById(R.id.contact_username);
            friend.city = (TextView) convertView.findViewById(R.id.contact_city);
            convertView.setTag(friend);
        } else {
            friend = (Friend) convertView.getTag();
        }
        friend.username.setText(String.valueOf(list.get(position).get(ParseUtil.USER_NICKNAME)));
        friend.city.setText(String.valueOf(list.get(position).get(ParseUtil.USER_CITY)));

        if (list.get(position).get(ParseUtil.USER_PHOTO) != null) {
            ParseFile parseFile = (ParseFile) list.get(position).get(ParseUtil.USER_PHOTO);
            getUserPhotoFromParse(friend, parseFile);
        } else {
            friend.userPhoto.setImageResource(R.drawable.user_photo_intial);
        }
        return convertView;
    }

    /**
     * download user photo from parse
     */
    private void getUserPhotoFromParse(final Friend people, final ParseFile parseFile) {
        parseFile.getDataInBackground(new GetDataCallback() {
            @Override
            public void done(byte[] bytes, ParseException e) {
                if (null == e) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 8;
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                    people.userPhoto.setImageBitmap(bmp);
                }
            }
        });
    }

    private class Friend {
        public RoundImageView userPhoto;
        public TextView username;
        public TextView city;
    }
}

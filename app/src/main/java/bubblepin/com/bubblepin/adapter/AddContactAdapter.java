package bubblepin.com.bubblepin.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;

import java.util.List;
import java.util.Map;

import bubblepin.com.bubblepin.GoogleMapActivity;
import bubblepin.com.bubblepin.R;
import bubblepin.com.bubblepin.contactModule.ContactActivity;
import bubblepin.com.bubblepin.util.ParseUtil;
import bubblepin.com.bubblepin.util.RoundImageView;

public class AddContactAdapter extends BaseAdapter {

    private List<Map<String, Object>> list;
    private Context context;

    private final String currentID;

    public AddContactAdapter(Context context, List<Map<String, Object>> list) {
        this.context = context;
        this.list = list;
        ParseUser currentUser = ParseUser.getCurrentUser();
        currentID = currentUser.getObjectId();
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
        final People people;
        if (null == convertView) {
            people = new People();
            convertView = LayoutInflater.from(context).inflate(R.layout.add_contact_list_item, null);
            people.username = (TextView) convertView.findViewById(R.id.add_contract_username);
            people.userPhoto = (RoundImageView) convertView.findViewById(R.id.add_contract_user_photo);
            people.addButton = (ImageView) convertView.findViewById(R.id.add_contract_status);
            people.addButtonLinearLayout = (LinearLayout)
                    convertView.findViewById(R.id.add_contract_layout);
            convertView.setTag(people);
        } else {
            people = (People) convertView.getTag();
        }
        final String userID = String.valueOf(list.get(position).get(ParseUtil.OBJECT_ID));

        people.username.setText(String.valueOf(list.get(position).get(ParseUtil.USER_NICKNAME)));
        if (list.get(position).get(ParseUtil.USER_PHOTO) != null) {
            ParseFile parseFile = (ParseFile) list.get(position).get(ParseUtil.USER_PHOTO);
            getUserPhotoFromParse(people, parseFile);
        } else {
            people.userPhoto.setImageResource(R.drawable.user_photo_intial);
        }

        boolean isFriend = (Boolean) list.get(position).get(ParseUtil.FRIENDS_ISFRIEND);
        if (isFriend) {
            people.addButton.setImageResource(R.drawable.add_contract_added);
        }

        people.addButtonLinearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isFriend = (Boolean) list.get(position).get(ParseUtil.FRIENDS_ISFRIEND);
                boolean isConfirmed = (Boolean) list.get(position).get(ParseUtil.FRIENDS_COMFIRM);
                Log.i(getClass().getSimpleName(), "is Friends?" + String.valueOf(isFriend));
                Log.i(getClass().getSimpleName(), "is Friends?" + String.valueOf(isConfirmed));
                // if it's friends or the current users send add friends request
                if (isFriend) {
                    if (isConfirmed) {
                        showDeleteDialog(position, userID, people);
                    } else {
                        // in the situation: the current users send add friend request
                        deleteFriendRequest(position, userID, people);
                    }
                } else {
                    // if not friend: send friend request
                    sendFriendRequest(position, userID, people);
                }
            }
        });
        return convertView;
    }

    private void showDeleteDialog(final int position, final String userID, final People people) {
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(context);
        alertDialogBuilder.setTitle("Delete Friend");
        alertDialogBuilder
                .setMessage("Notice: unable to revert")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteFriendRequest(position, userID, people);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void deleteFriendRequest(int position, final String userID, final People people) {
        try {
            ParseUtil.deleteFriend(currentID, userID);
            updateMapAndFriendList();
            people.addButton.setImageResource(R.drawable.add_contract_button);
            list.get(position).put(ParseUtil.FRIENDS_ISFRIEND, false);
            list.get(position).put(ParseUtil.FRIENDS_COMFIRM, false);
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(), "delete friend request error:" + e.getMessage());
        }
    }

    private void sendFriendRequest(int position, final String userID, People people) {
        Log.i(getClass().getSimpleName(), currentID + " and " + userID + " are not friend");
        try {
            ParseUtil.addFriends(userID);
            people.addButton.setImageResource(R.drawable.add_contract_added);
            list.get(position).put(ParseUtil.FRIENDS_ISFRIEND, true);
            list.get(position).put(ParseUtil.FRIENDS_COMFIRM, false);
            updateMapAndFriendList();
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(), "send friend request error: " + e.getMessage());
        }
    }

    /**
     * download user photo from parse
     */
    private void getUserPhotoFromParse(final People people, final ParseFile parseFile) {
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

    /**
     * add or delete friends: update GoogleMap
     */
    private void updateMapAndFriendList() {
        GoogleMapActivity.refreshUpdateMarkers();
        ContactActivity.refreshUpdateFriendList();
    }

    public class People {
        public RoundImageView userPhoto;
        public LinearLayout addButtonLinearLayout;
        public ImageView addButton;
        public TextView username;
    }
}

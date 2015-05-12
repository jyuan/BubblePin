package bubblepin.com.bubblepin.contactModule;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bubblepin.com.bubblepin.GoogleMapActivity;
import bubblepin.com.bubblepin.MyApplication;
import bubblepin.com.bubblepin.R;
import bubblepin.com.bubblepin.adapter.ContactAdapter;
import bubblepin.com.bubblepin.profileModule.ProfileActivity;
import bubblepin.com.bubblepin.util.ParseUtil;
import bubblepin.com.bubblepin.util.SlideListView;


public class ContactActivity extends ActionBarActivity
        implements SwipeRefreshLayout.OnRefreshListener, SlideListView.RemoveListener {

    private static final String LIST_INSTANCE_STATE =  "state";
    private Parcelable parcelable = null;

    private static boolean isRefresh = false;

    private TextView totalFriend;
    private SlideListView listView;

    private LinearLayout progressLayout;
    private SwipeRefreshLayout swipeLayout;

    private List<Map<String, Object>> list = new ArrayList<>();

    private boolean isInitial = true;

    private int contactNumber;
    private int countFriend = 0;
    private final String currentUserID = ParseUser.getCurrentUser().getObjectId();

    public static void refreshUpdateFriendList() {
        isRefresh = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_contact);

        MyApplication.getInstance().addActivity(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        progressLayout = (LinearLayout) findViewById(R.id.progressBar_layout);
        totalFriend = (TextView) findViewById(R.id.contact_total);
        listView = (SlideListView) findViewById(R.id.contact_list);
        listView.setRemoveListener(this);

        // set up swipeLayout
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.ptr_layout);
        swipeLayout.setOnRefreshListener(this);

        getFriendDataFromParse();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, Object> map = (HashMap<String, Object>) listView.getItemAtPosition(position);
                String userID = String.valueOf(map.get(ParseUtil.OBJECT_ID));
                Intent intent = new Intent();
                intent.setClass(ContactActivity.this, ProfileActivity.class);
                intent.putExtra(ProfileActivity.USER_ID, userID);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRefresh) {
            getFriendDataFromParse();
            isRefresh = false;
            isInitial = true;
        }
//        if (parcelable != null) {
//            listView.onRestoreInstanceState(parcelable);
//        }
//        parcelable = null;
    }

    /**
     * Shows the progress UI or not
     */
    private void showProgress(final boolean show) {
        progressLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void getFriendDataFromParse() {
        if (isInitial) {
            showProgress(true);
        }
        list.clear();
        contactNumber = 0;

        final String currentUserId = ParseUser.getCurrentUser().getObjectId();

        ParseQuery<ParseObject> query = ParseUtil.getAllFriendsQueryFromUser(currentUserId);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(final List<ParseObject> results, ParseException e) {
                if (null == e) {
                    contactNumber = results.size();
                    if (contactNumber == 0) {
                        changeToNoFriendView();
                    } else {
                        countFriend = 0;
                        for (ParseObject object : results) {
                            countFriend++;
                            String userID;
                            if (!object.get(ParseUtil.FRIENDS_FROM).equals(currentUserId)) {
                                userID = String.valueOf(object.get(ParseUtil.FRIENDS_FROM));
                            } else {
                                userID = String.valueOf(object.get(ParseUtil.FRIENDS_TO));
                            }
                            getUserInfo(userID);
                        }
                    }
                } else {
                    Log.e(getClass().getSimpleName(), "get user friends list error: "
                            + e.getMessage());
                }
            }
        });
    }

    private void getUserInfo(String userID) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(ParseUtil.OBJECT_ID, userID);
        query.getFirstInBackground(new GetCallback<ParseUser>() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                if (null == e) {
                    Map<String, Object> map = new HashMap<>();

                    // handle user photo
                    ParseFile parseFile = parseUser.getParseFile(ParseUtil.USER_PHOTO);
                    if (null == parseFile) {
                        map.put(ParseUtil.USER_PHOTO, null);
                    } else {
                        map.put(ParseUtil.USER_PHOTO, parseUser.getParseFile(ParseUtil.USER_PHOTO));
                    }
                    map.put(ParseUtil.OBJECT_ID, parseUser.getObjectId());
                    map.put(ParseUtil.USER_NICKNAME, parseUser.get(ParseUtil.USER_NICKNAME));
                    map.put(ParseUtil.USER_CITY, ParseUtil.getUserLocation(parseUser));
                    list.add(map);

                    if (countFriend == contactNumber) {
                        Log.i(getClass().getSimpleName(), "get all the friends data");
                        Collections.sort(list, new Comparator<Map<String, Object>>() {
                            @Override
                            public int compare(Map<String, Object> map1, Map<String, Object> map2) {
                                return String.valueOf(map1.get(ParseUtil.USER_NICKNAME)).
                                        compareToIgnoreCase(String.valueOf(map2.get(ParseUtil.USER_NICKNAME)));
                            }
                        });

                        if (isInitial) {
                            showProgress(false);
                            isInitial = false;
                        } else {
                            swipeLayout.setRefreshing(false);
                        }
                        showListView();
                    }
                } else {
                    Log.e(getClass().getSimpleName(), "get user info error: " + e.getMessage());
                }
            }
        });
    }

    private void showListView() {
        totalFriend.setText(String.valueOf(contactNumber) + " CONTACTS");
        ContactAdapter adapter = new ContactAdapter(ContactActivity.this, list);
        listView.setAdapter(adapter);
    }

    private void changeToNoFriendView() {
        totalFriend.setText(getResources().getString(R.string.contact_no_friends));
        showProgress(false);
        isInitial = false;
        swipeLayout.setRefreshing(false);
    }

    @Override
    public void onRefresh() {
        getFriendDataFromParse();
    }

    @Override
    public void removeItem(SlideListView.RemoveDirection direction, final int position) {
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.delete_friend_title));
        alertDialogBuilder
                .setMessage(getString(R.string.delete_friend_message))
                .setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            String userID = String.valueOf(list.get(position).get(ParseUtil.OBJECT_ID));
                            ParseUtil.deleteFriend(currentUserID, userID);
                            list.remove(list.get(position));
                            if (list.size() == 0) {
                                changeToNoFriendView();
                            } else {
                                contactNumber--;
                                showListView();
                            }
                            GoogleMapActivity.refreshUpdateMarkers();
                        } catch (ParseException e) {
                            Log.e(getClass().getSimpleName(), "delete this friend error: " + e.getMessage());
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_contact, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_contact_add:
                startActivity(new Intent(ContactActivity.this, AddContactActivity.class));
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

//    @Override
//    public void onSaveInstanceState(Bundle state) {
//        super.onSaveInstanceState(state);
//        state.putParcelable(LIST_INSTANCE_STATE, listView.onSaveInstanceState());
//    }
//
//    @Override
//    protected void onRestoreInstanceState(Bundle state) {
//        super.onRestoreInstanceState(state);
//        parcelable = state.getParcelable(LIST_INSTANCE_STATE);
//    }
}

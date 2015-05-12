package bubblepin.com.bubblepin.filterModule;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import bubblepin.com.bubblepin.MyApplication;
import bubblepin.com.bubblepin.R;
import bubblepin.com.bubblepin.profileModule.ProfileActivity;
import bubblepin.com.bubblepin.util.ParseUtil;
import bubblepin.com.bubblepin.util.SlideListView;

public class FilterDetailActivity extends ActionBarActivity
        implements SlideListView.RemoveListener {

    private TextView textView;
    private SlideListView listView;
    private LinearLayout progressBarLayout;

    private SimpleAdapter adapter;
    private List<Map<String, Object>> list = new ArrayList<>();
    private String categoryID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MyApplication.getInstance().addActivity(this);

        Intent intent = getIntent();
        categoryID = intent.getStringExtra(ParseUtil.FILTER_CATEGORY_ID);
        String categoryName = intent.getStringExtra(ParseUtil.FILTER_CATEGORY_NAME);

        setTitle(categoryName);
        setContentView(R.layout.activity_filter_detail);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        progressBarLayout = (LinearLayout) findViewById(R.id.progressBar_layout);

        textView = (TextView) findViewById(R.id.filter_no_contact);
        listView = (SlideListView) findViewById(R.id.filter_contact_list);
        listView.setRemoveListener(this);

        initialAddContact();
        initialListView();

    }

    private void changeView(boolean flag) {
        textView.setVisibility(flag ? View.GONE : View.VISIBLE);
        listView.setVisibility(flag ? View.VISIBLE : View.GONE);
    }

    private void initialListView() {
        list.clear();
        try {
            List<ParseUser> parseUsers = ParseUtil.getUserInCategory(categoryID);
            if (parseUsers.size() != 0) {
                changeView(true);
                for (ParseUser parseUser : parseUsers) {
                    addItemIntoList(parseUser);
                }
            } else {
                changeView(false);
            }
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(), "get user in category error: " + e.getMessage());
        }

        showListView();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, Object> map = (HashMap<String, Object>) listView.getItemAtPosition(position);
                String userID = String.valueOf(map.get(ParseUtil.OBJECT_ID));
                Intent intent = new Intent();
                intent.setClass(FilterDetailActivity.this, ProfileActivity.class);
                intent.putExtra(ProfileActivity.USER_ID, userID);
                startActivity(intent);
            }
        });
    }

    private void addItemIntoList(ParseUser parseUser) {
        Map<String, Object> map = new HashMap<>();
        map.put(ParseUtil.OBJECT_ID, parseUser.getObjectId());
        map.put(ParseUtil.USER_NICKNAME, parseUser.get(ParseUtil.USER_NICKNAME));
        list.add(map);
    }

    private void showListView() {
        String[] from = new String[]{ParseUtil.USER_NICKNAME};
        int[] to = new int[]{R.id.filter_contact_name};
        adapter = new SimpleAdapter(this,
                list, R.layout.filter_contact_list_item, from, to);
        listView.setAdapter(adapter);
    }

    private void initialAddContact() {
        RelativeLayout addContact = (RelativeLayout) findViewById(R.id.filter_detail_add_contact_layout);
        addContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addContactDialog();
            }
        });
    }

    private void addContactDialog() {
        final List<Map<String, Object>> contactList = new ArrayList<>();

        showProgress(true);
        final String currentUserID = ParseUser.getCurrentUser().getObjectId();
        ParseQuery<ParseObject> parseQuery = ParseUtil.getAllFriendsQueryFromUser(currentUserID);
        parseQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if (null == e) {
                    final List<String> friendsID = new LinkedList<>();
                    for (ParseObject object : list) {
                        String userID;
                        if (object.get(ParseUtil.FRIENDS_FROM).equals(currentUserID)) {
                            userID = String.valueOf(object.get(ParseUtil.FRIENDS_TO));
                        } else {
                            userID = String.valueOf(object.get(ParseUtil.FRIENDS_FROM));
                        }
                        friendsID.add(userID);
                    }
                    Log.i(getClass().getSimpleName(), "friends List: " + friendsID.toString());

                    final int[] count = new int[1];
                    // get user info
                    for (String userID : friendsID) {
                        ParseQuery<ParseUser> queryUser = ParseUser.getQuery();
                        queryUser.getInBackground(userID, new GetCallback<ParseUser>() {
                            @Override
                            public void done(ParseUser parseUser, ParseException e) {
                                count[0]++;
                                if (null == e) {
                                    Map<String, Object> map = new HashMap<>();
                                    map.put(ParseUtil.OBJECT_ID, parseUser.getObjectId());
                                    map.put(ParseUtil.USER_NICKNAME, parseUser.get(ParseUtil.USER_NICKNAME));
                                    contactList.add(map);
                                    if (count[0] == friendsID.size()) {
                                        initialFriendListView(contactList);
                                    }
                                } else {
                                    showProgress(false);
                                    Log.e(getClass().getSimpleName(), "get friend info error: " + e.getMessage());
                                }
                            }
                        });
                    }
                } else {
                    showProgress(false);
                    Log.e(getClass().getSimpleName(), "get friends list error: " + e.getMessage());
                }
            }
        });
    }

    private void initialFriendListView(final List<Map<String, Object>> contactList) {
        String[] from = new String[]{ParseUtil.USER_NICKNAME};
        int[] to = new int[]{R.id.filter_contact_name};
        SimpleAdapter simpleAdapter = new SimpleAdapter(FilterDetailActivity.this,
                contactList, R.layout.filter_contact_list_item, from, to);

        showProgress(false);

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(FilterDetailActivity.this);
        builderSingle.setTitle(getString(R.string.filter_select_one_friend));
        builderSingle.setAdapter(simpleAdapter,
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i(getClass().getSimpleName(), "friend info: " + contactList.get(which));
                        String userID = (String) contactList.get(which).get(ParseUtil.OBJECT_ID);
                        try {
                            if (ParseUtil.isFriendExistInCategory(categoryID, userID)) {
                                list.add(contactList.get(which));
                                ParseUtil.saveFriendIntoCategory(categoryID, userID);
                                changeView(true);
                                showListView();
                                adapter.notifyDataSetChanged();
                            } else {
                                showToast(getResources().getString(R.string.friend_exist_in_category));
                            }
                        } catch (ParseException e) {
                            Log.e(getClass().getSimpleName(), "add friends error: " + e.getMessage());
                        }
                    }
                });
        builderSingle.show();
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    public void showProgress(final boolean show) {
        progressBarLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void removeItem(SlideListView.RemoveDirection direction, final int position) {
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.filter_delete_user_title));
        alertDialogBuilder
                .setMessage(getString(R.string.filter_delete_user_message))
                .setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            String userID = String.valueOf(list.get(position).get(ParseUtil.OBJECT_ID));
                            ParseUtil.deleteFriendInCategory(categoryID, userID);
                            list.remove(list.get(position));
                            if (list.size() == 0) {
                                changeView(false);
                            } else {
                                showListView();
                            }
                            adapter.notifyDataSetChanged();
                        } catch (ParseException e) {
                            Log.e(getClass().getSimpleName(), "delete this user error: " + e.getMessage());
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

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_filter_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

package bubblepin.com.bubblepin.contactModule;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bubblepin.com.bubblepin.MyApplication;
import bubblepin.com.bubblepin.R;
import bubblepin.com.bubblepin.adapter.AddContactAdapter;
import bubblepin.com.bubblepin.profileModule.ProfileActivity;
import bubblepin.com.bubblepin.util.ParseUtil;

public class AddContactActivity extends ActionBarActivity
        implements SwipeRefreshLayout.OnRefreshListener {

    private static boolean isInitial = true;
    private static final String currentID = ParseUser.getCurrentUser().getObjectId();

    private ListView listView;

    private LinearLayout progressLayout;
    private SwipeRefreshLayout swipeLayout;

    private List<Map<String, Object>> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        MyApplication.getInstance().addActivity(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.ptr_layout);
        swipeLayout.setOnRefreshListener(this);

        progressLayout = (LinearLayout) findViewById(R.id.progressBar_layout);
        listView = (ListView) findViewById(R.id.add_contract_list);

        isInitial = true;
        getUserDataFromParse();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, Object> map = (HashMap<String, Object>) listView.getItemAtPosition(position);
                String userID = String.valueOf(map.get(ParseUtil.OBJECT_ID));
                Intent intent = new Intent();
                intent.setClass(AddContactActivity.this, ProfileActivity.class);
                intent.putExtra(ProfileActivity.USER_ID, userID);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onRefresh() {
        getUserDataFromParse();
    }

    /**
     * Shows the progress UI or not
     */
    private void showProgress(final boolean show) {
        progressLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void getUserDataFromParse() {
        if (isInitial) {
            showProgress(true);
        }
        list.clear();
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereNotEqualTo(ParseUtil.OBJECT_ID, currentID);
        query.orderByAscending(ParseUtil.USER_NICKNAME);

        final int[] flag = new int[1];

        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> parseUsers, ParseException e) {
                if (e == null) {
                    for (ParseUser parseUser : parseUsers) {
                        addItemIntoFriendsMap(parseUser, flag, parseUsers.size());
                    }
                } else {
                    Log.e(getClass().getSimpleName(), "ger user data error: " + e.getMessage());
                }
            }
        });
    }

    private void addItemIntoFriendsMap(ParseUser parseUser, final int[] flag, final int size) {
        final Map<String, Object> map = new HashMap<>();
        final ParseFile parseFile = parseUser.getParseFile(ParseUtil.USER_PHOTO);
        if (null == parseFile) {
            map.put(ParseUtil.USER_PHOTO, null);
        } else {
            map.put(ParseUtil.USER_PHOTO, parseFile);
        }
        map.put(ParseUtil.OBJECT_ID, parseUser.getObjectId());
        map.put(ParseUtil.USER_NICKNAME, parseUser.get(ParseUtil.USER_NICKNAME));

        // check whether the two users are friend
        ParseQuery<ParseObject> query =
                ParseUtil.isFriendQuery(currentID, parseUser.getObjectId());
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                if (null == e) {
                    map.put(ParseUtil.FRIENDS_ISFRIEND, true);
                    map.put(ParseUtil.FRIENDS_COMFIRM,
                            parseObject.get(ParseUtil.FRIENDS_COMFIRM));
                    Log.i(getClass().getSimpleName(), map.toString());
                } else {
                    if (e.getCode() == ParseException.OBJECT_NOT_FOUND) {
                        // Object did not exist on the parse backend
                        map.put(ParseUtil.FRIENDS_ISFRIEND, false);
                        map.put(ParseUtil.FRIENDS_COMFIRM, false);
                    } else {
                        Log.e(getClass().getSimpleName(), "add info into map error: " + e.getMessage());
                    }
                }
                list.add(map);
                flag[0]++;
                if (flag[0] == size) {
                    AddContactAdapter adapter = new AddContactAdapter(AddContactActivity.this, list);
                    listView.setAdapter(adapter);
                    if (isInitial) {
                        showProgress(false);
                        isInitial = false;
                    } else {
                        swipeLayout.setRefreshing(false);
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_contact, menu);
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

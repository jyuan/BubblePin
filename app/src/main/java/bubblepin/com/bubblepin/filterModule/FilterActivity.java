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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bubblepin.com.bubblepin.GoogleMapActivity;
import bubblepin.com.bubblepin.MyApplication;
import bubblepin.com.bubblepin.R;
import bubblepin.com.bubblepin.adapter.FilterAdapter;
import bubblepin.com.bubblepin.util.ParseUtil;
import bubblepin.com.bubblepin.util.SlideListView;


public class FilterActivity extends ActionBarActivity implements SlideListView.RemoveListener {

    private SlideListView listView;
    private TextView textView;

    private FilterAdapter adapter;
    private List<Map<String, Object>> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        MyApplication.getInstance().addActivity(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        textView = (TextView) findViewById(R.id.filter_no_category);
        listView = (SlideListView) findViewById(R.id.filter_list);
        listView.setRemoveListener(this);

        addCategoryInitial();
        try {
            listDataInitial();
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(), "get category error: " + e.getMessage());
        }

        ImageView submitButton = (ImageView) findViewById(R.id.filter_submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateMap();
                finish();
            }
        });
    }

    /**
     * add or delete friends: update GoogleMap
     */
    private void updateMap() {
        GoogleMapActivity.refreshUpdateMarkers();
    }

    private void addCategoryInitial() {
        ImageView addCategoryButton = (ImageView) findViewById(R.id.filter_add_category);
        addCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCategoryDialog();
            }
        });
    }

    /**
     * Dialog to add a new Category
     */
    private void addCategoryDialog() {
        final EditText editText = new EditText(this);
        editText.setHint(getResources().getString(R.string.filter_add_category_hint));
        editText.setBackground(null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.filter_new_category))
                .setView(editText)
                .setPositiveButton(getString(R.string.create), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String name = editText.getText().toString();
                        try {
                            if (ParseUtil.isDuplicateCategory(name)) {
                                dialog.dismiss();
                                addItemIntoListView(ParseUtil.saveCategory(name));
                                adapter.notifyDataSetChanged();
                                showToast(getString(R.string.create) + " " +  name + " " +
                                        getString(R.string.filter_create_category_success));
                            } else {
                                showToast(getString(R.string.filter_duplicate_category_name));
                            }
                        } catch (ParseException e) {
                            Log.e(getClass().getSimpleName(), "save category error: " + e.getMessage());
                            dialog.dismiss();
                            showToast(getString(R.string.filter_get_category_error) + e.getMessage());
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }

    private void listDataInitial() throws ParseException {
        list.clear();
        List<ParseObject> parseObjects = ParseUtil.getCategory();
        if (parseObjects.size() != 0) {
            changeView(true);
            for (ParseObject parseObject : parseObjects) {
                addItemIntoListView(parseObject);
            }
            adapter = new FilterAdapter(FilterActivity.this, list);
            listView.setAdapter(adapter);
        } else {
            changeView(false);
        }
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Map<String, Object> map = (HashMap<String, Object>) listView.getItemAtPosition(position);
                String categoryID = String.valueOf(map.get(FilterAdapter.CATEGORY_ID));
                String categoryName = String.valueOf(map.get(FilterAdapter.CATEGORY_NAME));
                Intent intent = new Intent();
                intent.setClass(FilterActivity.this, FilterDetailActivity.class);
                intent.putExtra(ParseUtil.FILTER_CATEGORY_ID, categoryID);
                intent.putExtra(ParseUtil.FILTER_CATEGORY_NAME, categoryName);
                startActivity(intent);
            }
        });
    }

    private void addItemIntoListView(ParseObject parseObject) {
        Map<String, Object> map = new HashMap<>();
        map.put(FilterAdapter.CATEGORY_ID, parseObject.getObjectId());
        map.put(FilterAdapter.CATEGORY_NAME, parseObject.get(ParseUtil.FILTER_CATEGORY_NAME));
        map.put(FilterAdapter.CATEGORY_SELECTED, parseObject.get(ParseUtil.FILTER_IS_SELECTED));
        list.add(map);
    }

    private void changeView(boolean flag) {
        textView.setVisibility(flag ? View.GONE : View.VISIBLE);
        listView.setVisibility(flag ? View.VISIBLE : View.GONE);
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_filter, menu);
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

    @Override
    public void removeItem(SlideListView.RemoveDirection direction, final int position) {
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getResources().getString(R.string.filter_delete_category));
        alertDialogBuilder
                .setMessage(getResources().getString(R.string.filter_remove_item))
                .setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            ParseUtil.deleteCategory(String.valueOf(list.get(position).get(FilterAdapter.CATEGORY_ID)));
                            list.remove(list.get(position));
                            adapter.notifyDataSetChanged();
                        } catch (ParseException e) {
                            Log.e(getClass().getSimpleName(), "delete the category error: " + e.getMessage());
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
}

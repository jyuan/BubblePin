package bubblepin.com.bubblepin;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.ProgressCallback;

import java.util.Date;

import bubblepin.com.bubblepin.util.ParseUtil;
import bubblepin.com.bubblepin.util.RoundImageView;


public class MemoryDetailActivity extends ActionBarActivity {

    // Memory TextView
    private TextView titleTextView, descriptionTextView,
            addressTextView, typeTextView, dateTextView, nameTextView;
    // No Media TextView
    private TextView noMediaTextView;

    // ImageView from Parse
    private ImageView memoryImage;
    private RoundImageView userPhoto;

    private ProgressDialog progressDialog;

    private String address, date, type, title, description;
    private String memoryAuthorUserId;

    private ParseObject memoryParseObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_detail);

        MyApplication.getInstance().addActivity(this);

        Intent intent = getIntent();
        String memoryID = intent.getExtras().getString(GoogleMapActivity.MEMORY_ID);
        memoryAuthorUserId = intent.getExtras().getString(GoogleMapActivity.USER_ID);

        progressDialog = new ProgressDialog(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById();

        getMemoryAuthorInfo(memoryAuthorUserId);
        getMemoryDataFromParse(memoryID);
    }

    private void findViewById() {
        noMediaTextView = (TextView) findViewById(R.id.memory_detail_no_media);
        titleTextView = (TextView) findViewById(R.id.memory_detail_title);
        descriptionTextView = (TextView) findViewById(R.id.memory_detail_description);
        addressTextView = (TextView) findViewById(R.id.memory_detail_address);
        typeTextView = (TextView) findViewById(R.id.memory_detail_type);
        dateTextView = (TextView) findViewById(R.id.memory_detail_date);
        nameTextView = (TextView) findViewById(R.id.memory_detail_name);

        memoryImage = (ImageView) findViewById(R.id.memory_Detail_image);
        userPhoto = (RoundImageView) findViewById(R.id.memory_detail_user_photo);
    }

    /**
     * get the author info
     */
    private void getMemoryAuthorInfo(final String userId) {
        try {
            ParseUser parseUser = ParseUtil.getUserInfo(userId);
            String name = parseUser.getString(ParseUtil.USER_NICKNAME);
            nameTextView.setText(name);

            ParseFile parseFile = parseUser.getParseFile(ParseUtil.USER_PHOTO);
            if (parseFile != null) {
                getUserPhotoFromParse(parseFile);
            }
        } catch (ParseException e) {
            Log.i(getClass().getSimpleName(), "get memory author info Error: " + e.getMessage());
        }
    }

    /**
     * Method used to download specific information from cloud server
     *
     * @param objectId specific objectId
     */
    private void getMemoryDataFromParse(String objectId) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(ParseUtil.MEMORY);
        query.getInBackground(objectId, new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if (null == e) {
                    memoryParseObject = object;
                    Log.i(getClass().getSimpleName(), "ID: " + object.getObjectId());

                    address = object.getString(ParseUtil.MEMORY_ADDRESS);
                    type = object.getString(ParseUtil.MEMORY_MEDIA_TYPE);
                    title = object.getString(ParseUtil.MEMORY_TITLE);
                    description = object.getString(ParseUtil.MEMORY_INTRODUCTION);

                    Date memoryDate = object.getDate(ParseUtil.MEMORY_MEMORY_DATE);
                    date = ParseUtil.getDateWithoutTime(memoryDate);

                    titleTextView.setText(title);
                    addressTextView.setText(address);
                    typeTextView.setText(type);
                    descriptionTextView.setText(description);
                    dateTextView.setText(date);

                    if (!type.equals(ParseUtil.TEXT)) {
                        ParseFile parseFile = (ParseFile) object.get(ParseUtil.MEMORY_IMAGE);
                        getFileFromParse(parseFile);
                    }
                }
            }
        });
    }

    /**
     * download user photo from parse
     */
    private void getUserPhotoFromParse(final ParseFile parseFile) {
        parseFile.getDataInBackground(new GetDataCallback() {
            @Override
            public void done(byte[] bytes, ParseException e) {
                if (null == e) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 8;
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                    userPhoto.setImageBitmap(bmp);
                } else {
                    showToast(e.getMessage());
                }
            }
        });
    }

    /**
     * Memory Type: has media
     */
    private void changeToMediaMemoryDetail() {
        noMediaTextView.setVisibility(View.GONE);
        memoryImage.setVisibility(View.VISIBLE);
    }

    /**
     * download file from parse
     */
    private void getFileFromParse(final ParseFile parseFile) {
        parseFile.getDataInBackground(new GetDataCallback() {
            @Override
            public void done(byte[] bytes, ParseException e) {
                if (null == e) {
                    progressDialog.dismiss();
                    changeToMediaMemoryDetail();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                    memoryImage.setImageBitmap(bmp);
                } else {
                    showToast(e.getMessage());
                }
            }
        }, new ProgressCallback() {
            @Override
            public void done(Integer integer) {
                progressDialog.setMax(100);
                progressDialog.setMessage(getString(R.string.download) + parseFile.getName());
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setProgress(integer);
                progressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                parseFile.cancel();
                            }
                        });
                progressDialog.show();
            }
        });
    }

    private void showToast(String info) {
        Toast.makeText(this, info, Toast.LENGTH_LONG).show();
    }

    /**
     * show dialog to double check for the delete event
     */
    private void deleteDialog() {
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.delete));
        alertDialogBuilder
                .setMessage(getString(R.string.delete_memory))
                .setPositiveButton(getString(R.string.detail), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int which) {
                        // delete memory
                        try {
                            memoryParseObject.delete();
                        } catch (ParseException e) {
                            Log.e(getClass().getSimpleName(), "delete memory error: " + e.getMessage());
                        }
                        dialog.dismiss();
                        MemoryDetailActivity.this.finish();
                        updateMap();
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

    private void updateMap() {
        GoogleMapActivity.refreshUpdateMarkers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_memory_detail, menu);

        MenuItem delete = menu.findItem(R.id.menu_memory_detail_delete);

        if (ParseUtil.isCurrentLoginUser(memoryAuthorUserId)) {
            delete.setVisible(true);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_memory_detail_delete:
                deleteDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

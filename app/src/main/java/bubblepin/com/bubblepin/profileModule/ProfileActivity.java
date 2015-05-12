package bubblepin.com.bubblepin.profileModule;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.parse.CountCallback;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.ProgressCallback;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bubblepin.com.bubblepin.MemoryDetailActivity;
import bubblepin.com.bubblepin.MyApplication;
import bubblepin.com.bubblepin.R;
import bubblepin.com.bubblepin.googleMapCluster.BubblePinClusterItem;
import bubblepin.com.bubblepin.util.ImageUtil;
import bubblepin.com.bubblepin.util.ParseUtil;
import bubblepin.com.bubblepin.util.PreferenceUtil;
import bubblepin.com.bubblepin.util.RoundImageView;

/**
 * Set up Google Map and Current Location:
 * http://developer.android.com/training/location/retrieve-current.html
 * https://developers.google.com/maps/documentation/android/map
 */
public class ProfileActivity extends ActionBarActivity implements OnMapReadyCallback,
        View.OnClickListener, ClusterManager.OnClusterItemClickListener,
        ClusterManager.OnClusterClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String MEMORY_ID = "memoryID";
    public static final String USER_ID = "userId";

    private static final int CAPTURE_IMAGE = 1;
    private static final int UPLOAD_IMAGE = 2;
    private static boolean refresh = false;

    private RoundImageView userPhoto;
    private ImageUtil imageUtil;
    private TextView totalMemoriesTextView, recentAddedTextView, contactNumberTextView,
            usernameTextView, locationTextView;

    private ProgressDialog progressDialog;
    private LinearLayout progressBar;

    // Provides the entry point to Google Play services.
    private GoogleApiClient googleApiClient;

    private GoogleMap googleMap;
    private double latitude;
    private double longitude;

    // private final Map<Marker, String> markers = new HashMap<>();
    private Map<BubblePinClusterItem, String> clusterItems = new HashMap<>();
    private ClusterManager<BubblePinClusterItem> clusterManager;

    // profile user ID
    private String userID;
    // login userID
    private final String currentUserID = ParseUser.getCurrentUser().getObjectId();

    /**
     * Update the user info if the user submit in the ProfileEditActivity
     */
    public static void refreshUserInfo() {
        refresh = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        MyApplication.getInstance().addActivity(this);

        Intent intent = getIntent();
        userID = intent.getStringExtra(USER_ID);

        imageUtil = new ImageUtil(this);

        progressDialog = new ProgressDialog(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        buildGoogleApiClient();
        findViewById();

        try {
            getUserDataFromParse(userID);
        } catch (ParseException e) {
            Log.e(getClass().getSimpleName(), "get user Info error: " + e.getMessage());
        }
        getSummaryDataFromParse(userID);
    }

    @Override
    public void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        googleApiClient.disconnect();
        googleMap.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (refresh) {
            try {
                getUserDataFromParse(userID);
            } catch (ParseException e) {
                Log.e(getClass().getSimpleName(),
                        "on Resume method, get user data error: " + e.getMessage());
                refresh = false;
            }
        }
    }

    private void findViewById() {
        locationTextView = (TextView) findViewById(R.id.profile_location);
        totalMemoriesTextView = (TextView) findViewById(R.id.profile_total_memory);
        recentAddedTextView = (TextView) findViewById(R.id.profile_recent_added);
        contactNumberTextView = (TextView) findViewById(R.id.profile_contact);
        usernameTextView = (TextView) findViewById(R.id.profile_username);
        progressBar = (LinearLayout) findViewById(R.id.progressBar_layout);

        // only the profile is current login user, the user can change the user photo
        userPhoto = (RoundImageView) findViewById(R.id.profile_user_photo);
        if (ParseUtil.isCurrentLoginUser(userID)) {
            userPhoto.setOnClickListener(this);
        } else {
            userPhoto.setClickable(false);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.profile_user_photo:
                chooseSource();
                break;
        }
    }

    private void getUserDataFromParse(String userID) throws ParseException {
        ParseUser parseUser = ParseUtil.getUserInfo(userID);
        locationTextView.setText(ParseUtil.getUserLocation(parseUser));
        if (!refresh) {
            usernameTextView.setText((String) parseUser.get(ParseUtil.USER_NICKNAME));
            ParseFile parseFile = (ParseFile) parseUser.get(ParseUtil.USER_PHOTO);
            if (parseFile != null) {
                getUserPhotoFromParse(parseFile);
            }
        }
        refresh = false;
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

    private void getSummaryDataFromParse(String userID) {
        ParseQuery<ParseObject> queryContact = ParseUtil.getAllFriendsQueryFromUser(userID);
        queryContact.countInBackground(new CountCallback() {
            public void done(int count, ParseException e) {
                if (e == null) {
                    Log.i(getClass().getSimpleName(), "contact count: " + count);
                    contactNumberTextView.setText(String.valueOf(count));
                } else {
                    Log.e(getClass().getSimpleName(), "count contact number error: " + e.getMessage());
                }
            }
        });

        ParseQuery<ParseObject> queryTotalMemories = ParseUtil.getAllMemoriesQueryFromUser(userID);
        queryTotalMemories.countInBackground(new CountCallback() {
            public void done(int count, ParseException e) {
                if (e == null) {
                    Log.i(getClass().getSimpleName(), "All memories count: " + count);
                    totalMemoriesTextView.setText(String.valueOf(count));
                } else {
                    Log.e(getClass().getSimpleName(), "count total memories error: " + e.getMessage());
                }
            }
        });

        ParseQuery<ParseObject> queryRencently = ParseUtil.getRecentMemoriesQueryFromUser(userID);
        queryRencently.countInBackground(new CountCallback() {
            public void done(int count, ParseException e) {
                if (e == null) {
                    Log.i(getClass().getSimpleName(), "Recent memories count: " + count);
                    recentAddedTextView.setText(String.valueOf(count));
                } else {
                    Log.e(getClass().getSimpleName(), "count recently memories error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Reference from official Document
     */
    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    /**
     * initialize google map engine and mark all memories as markers
     */
    private void createMap() {
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.profile_map);
        mapFragment.getMapAsync(this);
        googleMap = mapFragment.getMap();
    }

    /**
     * Method used to initialize all markers
     */
    private void initMarks() {
        // get all memories from current user
        if (ParseUtil.isCurrentLoginUser(userID)) {
            getAllMemoriesFromUser();
        } else {
            // get all memories from the selected User (Filter and Friend)
            getAllPublicMemoriesFromUser();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setMyLocationEnabled(true);

        clusterManager = new ClusterManager<>(this, googleMap);
        clusterManager.setOnClusterItemClickListener(this);

        googleMap.setOnCameraChangeListener(clusterManager);
        googleMap.setOnMarkerClickListener(clusterManager);
        UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setCompassEnabled(true);
        Log.i(getClass().getSimpleName(), "latitude: " + String.valueOf(latitude));
        Log.i(getClass().getSimpleName(), "longitude: " + String.valueOf(longitude));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(latitude, longitude), 15));

        initMarks();
    }

    /**
     * Once Connected, get the current location and create the map
     *
     * @param bundle
     */
    @Override
    public void onConnected(Bundle bundle) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(
                googleApiClient);
        if (location != null) {
            Log.i(getClass().getSimpleName(), "get current location");
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            createMap();
        } else {
            Log.i(getClass().getSimpleName(), "doesn't get current location");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(getClass().getSimpleName(),
                "Connection failed: ConnectionResult.getErrorCode() = "
                        + connectionResult.getErrorCode());
    }

    /**
     * get all the memories from current login user
     */
    private void getAllMemoriesFromUser() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(ParseUtil.MEMORY);
        query.whereEqualTo(ParseUtil.MEMORY_USRE_OBJECT_ID, currentUserID);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if (null == e) {
                    Log.i(getClass().getSimpleName(), "size of the list: " + list.size());
                    if (list.size() != 0) {
                        addMemoryIntoCluster(list);
                    }
                } else {
                    Log.e(getClass().getSimpleName(),
                            "get memories from user error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * get all the memories from Select User (Filter and Friend), only public memories will shows
     */
    private void getAllPublicMemoriesFromUser() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery(ParseUtil.MEMORY);
        query.whereEqualTo(ParseUtil.MEMORY_USRE_OBJECT_ID, userID);
        query.whereNotEqualTo(ParseUtil.MEMORY_PRIVACY, "Only me");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if (null == e) {
                    Log.i(getClass().getSimpleName(), "size of the list: " + list.size());
                    if (list.size() != 0) {
                        addMemoryIntoCluster(list);
                    }
                } else {
                    Log.e(getClass().getSimpleName(),
                            "get memories from user error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * add memory into Map
     *
     * @param parseObjects list of memories parseObject
     */
    private void addMemoryIntoCluster(List<ParseObject> parseObjects) {
        for (ParseObject parseObject : parseObjects) {
            String objectId = parseObject.getObjectId();
            ParseGeoPoint geoPoint = parseObject.getParseGeoPoint(ParseUtil.MEMORY_GEOPOINT);
            BubblePinClusterItem bubblePinClusterItem =
                    new BubblePinClusterItem(geoPoint.getLatitude(), geoPoint.getLongitude());
            clusterManager.addItem(bubblePinClusterItem);
            clusterItems.put(bubblePinClusterItem, objectId);
        }
        googleMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    @Override
    public boolean onClusterItemClick(ClusterItem clusterItem) {
        String objectId = clusterItems.get(clusterItem);
        if (null == objectId) {
            return false;
        }
        getMemoryBriefInfo(objectId);
        return true;
    }

    @Override
    public boolean onClusterClick(Cluster cluster) {
        showToast("Zoom in to get each Memory");
        return true;
    }

    private void getMemoryBriefInfo(final String objectId) {
        showProgress(true);
        ParseQuery<ParseObject> query = ParseQuery.getQuery(ParseUtil.MEMORY);
        query.getInBackground(objectId, new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                final String mediaType = object.getString(ParseUtil.MEMORY_MEDIA_TYPE);
                final String title = object.getString(ParseUtil.MEMORY_TITLE);

                Date memoryDate = object.getDate(ParseUtil.MEMORY_MEMORY_DATE);
                final String date = ParseUtil.getDateWithoutTime(memoryDate);

                final String userId = object.getString(ParseUtil.MEMORY_USRE_OBJECT_ID);
                ParseQuery<ParseUser> queryUser = ParseUser.getQuery();
                try {
                    queryUser.get(userId);
                } catch (ParseException e1) {
                    e1.printStackTrace();
                    showProgress(false);
                }
                queryUser.getFirstInBackground(new GetCallback<ParseUser>() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        if (null == e) {
                            showProgress(false);
                            String username = parseUser.getString(ParseUtil.USER_NICKNAME);
                            String message = "Author: " + username + "\nMedia Type: " +
                                    mediaType + "\nMemory Date: " + date;
                            createNewDialog(objectId, title, message, userId);
                        }
                    }
                });
            }
        });
    }

    /**
     * Method used to create a new dialog to print out stored data on cloud
     * server, and give user brief information of the memory
     */
    private void createNewDialog(final String objectId, String title,
                                 String message, final String userId) {
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder
                .setMessage(message)
                .setPositiveButton(getString(R.string.detail), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(ProfileActivity.this, MemoryDetailActivity.class);
                        intent.putExtra(MEMORY_ID, objectId);
                        intent.putExtra(USER_ID, userId);
                        startActivity(intent);
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

    private void chooseSource() {
        CharSequence colors[] = new CharSequence[]{"Take Photo", "Choose from Library"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Profile Picture");
        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    addCamera();
                } else {
                    addPicture();
                }
            }
        });
        builder.show();
    }

    /**
     * start a new intent to perform and request image capture
     * and also create a new file to store current captured image
     * file on SD card
     */
    private void addCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File imageFile = imageUtil.generateImageFile();
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
        startActivityForResult(intent, CAPTURE_IMAGE);
    }

    /**
     * choose image from gallery
     */
    private void addPicture() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent,
                getString(R.string.testImage_uploadTitle)), UPLOAD_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(getClass().getSimpleName(), "requestCode: " + String.valueOf(requestCode));
        Log.i(getClass().getSimpleName(), "resultCode: " + String.valueOf(resultCode));

        int source = 0;
        String imagePath = null;
        if (resultCode == RESULT_OK) {
            if (requestCode == CAPTURE_IMAGE) {
                File imageFile = imageUtil.getImageFile();
                imagePath = imageFile.getAbsolutePath();
                source = CAPTURE_IMAGE;

            } else if (requestCode == UPLOAD_IMAGE) {
                Uri imageUri = data.getData();
                imagePath = imageUtil.getRealPathFromURI(Build.VERSION.SDK_INT, imageUri);
                source = UPLOAD_IMAGE;
            }
            if (requestCode == CAPTURE_IMAGE || requestCode == UPLOAD_IMAGE) {
                // compress the image
                Bitmap original = BitmapFactory.decodeFile(imagePath);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                original.compress(Bitmap.CompressFormat.JPEG, 10, out);
                userPhoto.setImageBitmap(original);
                try {
                    saveImage(out.toByteArray(), source, new File(imagePath).getName());
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Two sources:
     * 1. Save image file from taking photo on SD card and also upload the image
     * on Parse cloud server(www.parse.com)
     * 2. Upload the file from gallery on Parse cloud server(www.parse.com)
     *
     * @param out    byte array of the file
     * @param source from camera or gallery
     * @param name   the name of the photo
     * @throws IOException
     */
    private void saveImage(final byte[] out, int source, String name)
            throws IOException {
        final ParseFile parseFile;

        if (source == CAPTURE_IMAGE) {
            parseFile = new ParseFile(name, out);
        } else {
            parseFile = new ParseFile(imageUtil.generateImageFileName(), out);
        }

        parseFile.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (null == e) {
                    progressDialog.dismiss();
                    try {
                        ParseUtil.saveUserPhoto(parseFile);
                        showToast("upload photo success!");
                    } catch (ParseException e1) {
                        showToast(e1.getMessage());
                        Log.e(getClass().getSimpleName(), "save image error: " + e1.getMessage());
                    }
                } else {
                    showToast(e.getMessage());
                }
            }
        }, new ProgressCallback() {
            @Override
            public void done(Integer integer) {
                progressDialog.setMax(100);
                progressDialog.setMessage("Uploading photo...");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setProgress(integer);
                progressDialog.setButton(ProgressDialog.BUTTON_POSITIVE, "Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                parseFile.cancel();
                            }
                        });
                progressDialog.show();
            }
        });
    }

    private void logoutDialog() {
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.logout));
        alertDialogBuilder
                .setMessage(getString(R.string.logout_message))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ParseUser.logOut();
                        PreferenceUtil.setBoolean(ProfileActivity.this, PreferenceUtil.LOGIN_INFO, false);
                        MyApplication.getInstance().exit();
                    }
                })
                .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
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

    private void showProgress(boolean flag) {
        progressBar.setVisibility(flag ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile, menu);

        MenuItem editMenuItem = menu.findItem(R.id.profile_edit);
        MenuItem logoutMenuItem = menu.findItem(R.id.profile_logout);

        if (ParseUtil.isCurrentLoginUser(userID)) {
            editMenuItem.setVisible(true);
            logoutMenuItem.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.profile_edit:
                startActivity(new Intent(this, ProfileEditActivity.class));
                return true;
            case R.id.profile_logout:
                logoutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

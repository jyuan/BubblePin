package bubblepin.com.bubblepin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import bubblepin.com.bubblepin.addMemoryModule.AddMemoryActivity;
import bubblepin.com.bubblepin.contactModule.ContactActivity;
import bubblepin.com.bubblepin.filterModule.FilterActivity;
import bubblepin.com.bubblepin.googleMapCluster.BubblePinClusterItem;
import bubblepin.com.bubblepin.loginModule.SignUpActivity;
import bubblepin.com.bubblepin.metaioSDKLocationModule.MetaioSDKLocationBasedARModule;
import bubblepin.com.bubblepin.profileModule.ProfileActivity;
import bubblepin.com.bubblepin.util.LocationUpdateUtil;
import bubblepin.com.bubblepin.util.ParseUtil;


public class GoogleMapActivity extends Activity implements
        View.OnClickListener, ClusterManager.OnClusterItemClickListener,
        ClusterManager.OnClusterClickListener {

    public static final String LATITUDE = "Latitude";
    public static final String LONGITUDE = "Longitude";
    public static final String ADDRESS = "address";
    public static final String MEMORY_ID = "memoryID";
    public static final String USER_ID = "userID";

    private static final int ADD_NEW_MEMORY = 1;
    private static final int SCAN_INTERVAL_INITIAL = 1000;
    private static final int SCAN_INTERVAL = 1000 * 20;

    // markers update status
    private static boolean updateMarkers = true;
    // markers update status (only assigned within onResume method)
    private boolean isUpdateMarkers = true;

    private boolean isFirstInitial = true;

    private GoogleMap googleMap;
    private ParseGeoPoint parseGeoPoint;
    private String address;

    private LinearLayout progressBarLayout;

    private Handler handler = new Handler();
    private LocationUpdateUtil locationUpdateUtil;

    // private final Map<Marker, String> markers = new HashMap<>();
    private final Map<BubblePinClusterItem, String> clusterItems = new HashMap<>();
    private ClusterManager<BubblePinClusterItem> clusterManager;

    // login userID
    String currentUserID = ParseUser.getCurrentUser().getObjectId();

    public static void refreshUpdateMarkers() {
        updateMarkers = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MyApplication.getInstance().addActivity(this);

        // check whether it is from SignUp Activity
        if (getIntent().hasExtra(SignUpActivity.SIGN_UP_SUCCESS)) {
            signUpSuccess();
        }

        locationUpdateUtil = new LocationUpdateUtil(this);
        locationUpdateUtil.buildGoogleApiClient();
        handler.postDelayed(scanLocation, 0);

        if (isValidGooglePlayService()) {
            setContentView(R.layout.activity_google_map);
        }
        initialUI();
    }

    private void initialUI() {
        ImageView switchToMetaioButton = (ImageView) findViewById(R.id.switch_button);
        ImageView addMemoryButton = (ImageView) findViewById(R.id.add_memory);
        TextView filterButton = (TextView) findViewById(R.id.google_map_filter);
        TextView contractButton = (TextView) findViewById(R.id.google_map_contact);

        progressBarLayout = (LinearLayout) findViewById(R.id.progressBar_layout);
        RelativeLayout profileLayout = (RelativeLayout) findViewById(R.id.google_map_profile);

        switchToMetaioButton.setOnClickListener(this);
        addMemoryButton.setOnClickListener(this);
        filterButton.setOnClickListener(this);
        contractButton.setOnClickListener(this);
        profileLayout.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        locationUpdateUtil.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        locationUpdateUtil.resume();
        isFirstInitial = true;
        Log.i(getClass().getSimpleName(),
                "updateMarkers in onResume method: " + updateMarkers);
        if (updateMarkers) {
            isUpdateMarkers = true;
            initMarks();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationUpdateUtil.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationUpdateUtil.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(scanLocation);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.switch_button:
                startActivity(new Intent(this, MetaioSDKLocationBasedARModule.class));
                break;
            case R.id.add_memory:
                if (parseGeoPoint != null) {
                    Intent intent = new Intent(this, AddMemoryActivity.class);
                    intent.putExtra(LATITUDE, parseGeoPoint.getLatitude());
                    intent.putExtra(LONGITUDE, parseGeoPoint.getLongitude());
                    intent.putExtra(ADDRESS, address);
                    startActivityForResult(intent, ADD_NEW_MEMORY);
                } else {
                    showToast("Didn't get the Current Address, Please wait for few more seconds");
                }
                break;
            case R.id.google_map_filter:
                startActivity(new Intent(this, FilterActivity.class));
                break;
            case R.id.google_map_contact:
                startActivity(new Intent(this, ContactActivity.class));
                break;
            case R.id.google_map_profile:
                String userID = ParseUser.getCurrentUser().getObjectId();
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.putExtra(ProfileActivity.USER_ID, userID);
                startActivity(intent);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    /**
     * initialize google map engine and mark all memories as markers
     */
    private void createMap() {
        if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

            if (googleMap != null) {
                googleMap.setMyLocationEnabled(true);

                clusterManager = new ClusterManager<>(this, googleMap);
                clusterManager.setOnClusterItemClickListener(this);
                clusterManager.setOnClusterClickListener(this);

                googleMap.setOnCameraChangeListener(clusterManager);
                googleMap.setOnMarkerClickListener(clusterManager);
                UiSettings uiSettings = googleMap.getUiSettings();
                uiSettings.setZoomControlsEnabled(true);
                uiSettings.setCompassEnabled(true);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(parseGeoPoint.getLatitude(),
                                parseGeoPoint.getLongitude()), 15));
            }
        }
    }

    /**
     * Method used to initialize all markers
     */
    private void initMarks() {
        if (googleMap != null && isUpdateMarkers) {
            Log.i(getClass().getSimpleName(), "initialize the markers.");
            updateMarkers = false;
            isUpdateMarkers = false;
//            googleMap.clear();
//            markers.clear();
            clusterItems.clear();
            clusterManager.clearItems();

            // get all memories from current user
            getAllMemoriesFromUser();
            // get all memories from the selected User (Filter and Friend)
            getAllPublicMemoriesFromUser();
        }
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
                addMemoryIntoCluster(list);
            }
        });
    }

    /**
     * get all the memories from Select User (Filter and Friend), only public memories will shows
     */
    private void getAllPublicMemoriesFromUser() {
        try {
            List<String> list = new LinkedList<>();
            list.addAll(ParseUtil.getSelectedUser(currentUserID));
            for (String userID : list) {
                ParseQuery<ParseObject> query = ParseQuery.getQuery(ParseUtil.MEMORY);
                query.whereEqualTo(ParseUtil.MEMORY_USRE_OBJECT_ID, userID);
                query.whereNotEqualTo(ParseUtil.MEMORY_PRIVACY, "Only me");
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> list, ParseException e) {
                        addMemoryIntoCluster(list);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "get memories error: " + e.getMessage());
        }
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

    /**
     * Method used to generate a marker by given specific name and location
     *
     * @param parseGeoPoint marker location
     * @return new generated marker
     */
//    private Marker addNewMarker(ParseGeoPoint parseGeoPoint) {
//        double latitude = parseGeoPoint.getLatitude();
//        double longitude = parseGeoPoint.getLongitude();
//        return googleMap.addMarker(new MarkerOptions().position(
//                new LatLng(latitude, longitude)));
//    }

    /**
     * Method used to check whether now google play service is available
     *
     * @return true if current google play service is available, false if not
     */
    private boolean isValidGooglePlayService() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status == ConnectionResult.SUCCESS) {
            return true;
        } else {
            Log.e(getClass().getSimpleName(), "Google Play Service is not available.");
            GooglePlayServicesUtil.getErrorDialog(status, this, 10).show();
            return false;
        }
    }

//    @Override
//    public boolean onMarkerClick(Marker marker) {
//        String objectId = markers.get(marker);
//        if (null == objectId) {
//            return false;
//        }
//        getMemoryBriefInfo(objectId);
//        return true;
//    }

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
                }
                queryUser.getFirstInBackground(new GetCallback<ParseUser>() {
                    @Override
                    public void done(ParseUser parseUser, ParseException e) {
                        if (null == e) {
                            String username = parseUser.getString(ParseUtil.USER_NICKNAME);
                            String message = "Author: " + username + "\nMedia Type: " +
                                    mediaType + "\nMemory Date: " + date;
                            showProgress(false);
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
                        Intent intent = new Intent(GoogleMapActivity.this, MemoryDetailActivity.class);
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

    /**
     * New thread that regularly fetch current location
     * further operation -> make this step as a background service if necessary
     */
    Runnable scanLocation = new Runnable() {

        @Override
        public void run() {
            if (isFirstInitial) {
                isFirstInitial = false;
                Location location = locationUpdateUtil.getCurrentLocation();
                address = locationUpdateUtil.getAddress();
                if (location != null) {
                    parseGeoPoint = ParseUtil.getParseGeoPoint(location);
                    createMap();
                    initMarks();
                }
                handler.postDelayed(this, SCAN_INTERVAL_INITIAL);
            } else {
                Location location = locationUpdateUtil.getCurrentLocation();
                address = locationUpdateUtil.getAddress();
                if (location != null) {
                    parseGeoPoint = ParseUtil.getParseGeoPoint(location);
                    createMap();
                    initMarks();
                }
                handler.postDelayed(this, SCAN_INTERVAL);
            }
        }
    };

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
        double lat = cluster.getPosition().latitude;
        double lng = cluster.getPosition().longitude;
        showToast("Memories at " +
                locationUpdateUtil.getAddress(lat, lng));
        return true;
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    public void showProgress(boolean show) {
        progressBarLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * show dialog to remind SignUp success and auto-login
     */
    private void signUpSuccess() {
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(getString(R.string.sign_up));
        alertDialogBuilder
                .setMessage("Sign Up Success!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
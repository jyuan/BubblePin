package bubblepin.com.bubblepin.metaioSDKLocationModule;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;

import com.metaio.cloud.plugin.util.MetaioCloudUtils;
import com.metaio.sdk.ARELInterpreterAndroidJava;
import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.AnnotatedGeometriesGroupCallback;
import com.metaio.sdk.jni.EGEOMETRY_FOCUS_STATE;
import com.metaio.sdk.jni.IAnnotatedGeometriesGroup;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.IRadar;
import com.metaio.sdk.jni.ImageStruct;
import com.metaio.sdk.jni.LLACoordinate;
import com.metaio.sdk.jni.Rotation;
import com.metaio.sdk.jni.SensorValues;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.tools.io.AssetsManager;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;

import bubblepin.com.bubblepin.GoogleMapActivity;
import bubblepin.com.bubblepin.MemoryDetailActivity;
import bubblepin.com.bubblepin.R;
import bubblepin.com.bubblepin.util.ParseUtil;

/**
 * Module from Metaio Android SDK
 * I focus on the data intersection
 */
public class MetaioSDKLocationBasedARModule extends ARViewActivity {

    private final static String TAG = "LOCATION_BASE";

    private IAnnotatedGeometriesGroup annotatedGeometriesGroup;

    private MyAnnotatedGeometriesGroupCallback annotatedGeometriesGroupCallback;

    private List<MetaioSDKMemory> memories = new ArrayList<>();

    private IRadar iRadar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set GPS tracking configuration
        boolean result = metaioSDK.setTrackingConfiguration("GPS", false);
        MetaioDebug.log("Tracking data loaded: " + result);
    }

    @Override
    protected void onDestroy() {
        // Break circular reference of Java objects
        if (annotatedGeometriesGroup != null) {
            annotatedGeometriesGroup.registerCallback(null);
        }
        if (annotatedGeometriesGroupCallback != null) {
            annotatedGeometriesGroupCallback.delete();
            annotatedGeometriesGroupCallback = null;
        }
        super.onDestroy();
    }

    @Override
    public void onDrawFrame() {
        if (metaioSDK != null && mSensors != null) {
            SensorValues sensorValues = mSensors.getSensorValues();

            float heading = 0.0f;
            if (sensorValues.hasAttitude()) {
                float m[] = new float[9];
                sensorValues.getAttitude().getRotationMatrix(m);

                Vector3d v = new Vector3d(m[6], m[7], m[8]);
                v.normalize();

                heading = (float) (-Math.atan2(v.getY(), v.getX()) - Math.PI / 2.0);
            }

            Rotation rot = new Rotation((float) (Math.PI / 2.0), 0.0f, -heading);
            for (MetaioSDKMemory memory : memories) {
                if (memory.getiGeometry() != null) {
                    memory.getiGeometry().setRotation(rot);
                }
            }
        }
        super.onDrawFrame();
    }

    public void onButtonClick(View v) {
        finish();
    }

    /**
     * Method used to initialize all LLACoordinates
     */
    private List<MemoryLocation> initMemoryLLACoordinate() throws ParseException {
        List<MemoryLocation> res = new ArrayList<>();
        List<ParseObject> parseObjects = ParseUtil.getAllMemories();

        for (ParseObject parseObject : parseObjects) {
            String objectId = parseObject.getObjectId();
            String address = parseObject.getString("address");
            ParseGeoPoint geoPoint = parseObject.getParseGeoPoint("geoPoint");
            res.add(new MemoryLocation(geoPoint, objectId, address));
        }
        return res;
    }

    @Override
    protected int getGUILayout() {
        return R.layout.activity_metaio_sdklocation;
    }

    @Override
    protected IMetaioSDKCallback getMetaioSDKCallbackHandler() {
        return null;
    }

    @Override
    protected void loadContents() {
        annotatedGeometriesGroup = metaioSDK.createAnnotatedGeometriesGroup();
        annotatedGeometriesGroupCallback = new MyAnnotatedGeometriesGroupCallback();
        annotatedGeometriesGroup.registerCallback(annotatedGeometriesGroupCallback);

        // Clamp geometries' Z position to range [5000;200000] no matter how close or far they are
        // away.
        // This influences minimum and maximum scaling of the geometries (easier for development).
        metaioSDK.setLLAObjectRenderingLimits(200, 200);

        // Set render frustum accordingly
        metaioSDK.setRendererClippingPlaneLimits(10, 220000);


        List<MemoryLocation> memoryLocations = null;
        try {
            memoryLocations = initMemoryLLACoordinate();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        inform("memoryLocations size: " + memoryLocations.size());

        for (MemoryLocation memoryLocation : memoryLocations) {
            String objectId = memoryLocation.getObjectId();
            String address = memoryLocation.getAddress();
            ParseGeoPoint geoPoint = memoryLocation.getParseGeoPoint();
            LLACoordinate coordinate = new LLACoordinate(geoPoint.getLatitude(), geoPoint.getLongitude(), 0, 0);
            IGeometry coordinateGeo = createPOIGeometry(coordinate);
            coordinateGeo.setName(objectId);
            inform("Geometry inserted: " + coordinateGeo);
            annotatedGeometriesGroup.addGeometry(coordinateGeo, address);
            MetaioSDKMemory curMemory = new MetaioSDKMemory(objectId, coordinateGeo);
            memories.add(curMemory);
        }

        // create radar
        iRadar = metaioSDK.createRadar();
        iRadar.setBackgroundTexture(AssetsManager.getAssetPathAsFile(getApplicationContext(),
                "radar.png"));
        iRadar.setObjectsDefaultTexture(AssetsManager.getAssetPathAsFile(getApplicationContext(),
                "yellow.png"));
        iRadar.setRelativeToScreen(IGeometry.ANCHOR_TL);

        // add geometries to the radar
        for (MetaioSDKMemory memory : memories) {
            IGeometry geo = memory.getiGeometry();
            inform("Radar geo: " + geo);
            iRadar.add(geo);
        }
    }

    private IGeometry createPOIGeometry(LLACoordinate lla) {
        final File path =
                AssetsManager.getAssetPathAsFile(getApplicationContext(),
                        "ExamplePOI.obj");
        if (path != null) {
            IGeometry geo = metaioSDK.createGeometry(path);
            geo.setTranslationLLA(lla);
            geo.setLLALimitsEnabled(true);
            geo.setScale(100);
            return geo;
        } else {
            inform("Missing files for POI geometry");
            return null;
        }
    }

    @Override
    protected void onGeometryTouched(final IGeometry geometry) {
        inform("Geometry selected: " + geometry);
        inform("Geo name:" + geometry.getName());
        mSurfaceView.queueEvent(new Runnable() {

            @Override
            public void run() {
                iRadar.setObjectsDefaultTexture(AssetsManager.getAssetPathAsFile(getApplicationContext(),
                        "yellow.png"));
                iRadar.setObjectTexture(geometry, AssetsManager.getAssetPathAsFile(getApplicationContext(),
                        "red.png"));
                annotatedGeometriesGroup.setSelectedGeometry(geometry);
            }
        });

        String objectId = geometry.getName();
        getMemoryBriefInfo(objectId);
    }

    private void getMemoryBriefInfo(final String objectId) {
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
                        Intent intent = new Intent(MetaioSDKLocationBasedARModule.this, MemoryDetailActivity.class);
                        intent.putExtra(GoogleMapActivity.MEMORY_ID, objectId);
                        intent.putExtra(GoogleMapActivity.USER_ID, userId);
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

    private void inform(String info) {
        MetaioDebug.log(Log.ERROR, info);
        Log.i(TAG, info);
    }

    final class MyAnnotatedGeometriesGroupCallback extends AnnotatedGeometriesGroupCallback {
        Bitmap mAnnotationBackground, mEmptyStarImage, mFullStarImage;
        int mAnnotationBackgroundIndex;
        ImageStruct texture;
        String[] textureHash = new String[1];
        TextPaint mPaint;
        Lock geometryLock;


        Bitmap inOutCachedBitmaps[] = new Bitmap[]{mAnnotationBackground, mEmptyStarImage, mFullStarImage};
        int inOutCachedAnnotationBackgroundIndex[] = new int[]{mAnnotationBackgroundIndex};

        public MyAnnotatedGeometriesGroupCallback() {
            mPaint = new TextPaint();
            mPaint.setFilterBitmap(true); // enable dithering
            mPaint.setAntiAlias(true); // enable anti-aliasing
        }

        @Override
        public IGeometry loadUpdatedAnnotation(IGeometry geometry, Object userData, IGeometry existingAnnotation) {
            if (userData == null) {
                return null;
            }

            if (existingAnnotation != null) {
                // We don't update the annotation if e.g. distance has changed
                return existingAnnotation;
            }

            String title = (String) userData; // as passed to addGeometry
            LLACoordinate location = geometry.getTranslationLLA();
            float distance = (float) MetaioCloudUtils.getDistanceBetweenTwoCoordinates(location, mSensors.getLocation());
            Bitmap thumbnail = BitmapFactory.decodeResource(getResources(), R.drawable.metaio_icon);
            try {
                texture =
                        ARELInterpreterAndroidJava.getAnnotationImageForPOI(title, title, distance, "5", thumbnail,
                                null,
                                metaioSDK.getRenderSize(), MetaioSDKLocationBasedARModule.this,
                                mPaint, inOutCachedBitmaps, inOutCachedAnnotationBackgroundIndex, textureHash);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (thumbnail != null)
                    thumbnail.recycle();
                thumbnail = null;
            }

            mAnnotationBackground = inOutCachedBitmaps[0];
            mEmptyStarImage = inOutCachedBitmaps[1];
            mFullStarImage = inOutCachedBitmaps[2];
            mAnnotationBackgroundIndex = inOutCachedAnnotationBackgroundIndex[0];

            IGeometry resultGeometry = null;

            if (texture != null) {
                if (geometryLock != null) {
                    geometryLock.lock();
                }

                try {
                    // Use texture "hash" to ensure that SDK loads new texture if texture changed
                    resultGeometry = metaioSDK.createGeometryFromImage(textureHash[0], texture, true, false);
                } finally {
                    if (geometryLock != null) {
                        geometryLock.unlock();
                    }
                }
            }

            return resultGeometry;
        }

        @Override
        public void onFocusStateChanged(IGeometry geometry, Object userData, EGEOMETRY_FOCUS_STATE oldState,
                                        EGEOMETRY_FOCUS_STATE newState) {
            inform("onFocusStateChanged for " + userData + ", " + oldState + "->" + newState);
        }
    }

    private class MemoryLocation {

        private final ParseGeoPoint parseGeoPoint;
        private final String objectId;
        private final String address;

        private MemoryLocation(ParseGeoPoint parseGeoPoint, String objectId, String address) {
            this.parseGeoPoint = parseGeoPoint;
            this.objectId = objectId;
            this.address = address;
        }

        public ParseGeoPoint getParseGeoPoint() {
            return parseGeoPoint;
        }

        public String getObjectId() {
            return objectId;
        }

        public String getAddress() {
            return address;
        }
    }
}
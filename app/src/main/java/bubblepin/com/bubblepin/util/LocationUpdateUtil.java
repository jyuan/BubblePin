package bubblepin.com.bubblepin.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.List;
import java.util.Locale;

/**
 * Reference from: https://developer.android.com/training/location/receive-location-updates.html
 * but mark it as a class rather than in Activity
 */
public class LocationUpdateUtil implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    // The desired interval for location updates. Inexact. Updates may be more or less frequent.
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    // The fastest rate for active location updates. Exact. Updates will never be more frequent
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Provides the entry point to Google Play services.
    private GoogleApiClient googleApiClient;

    // Stores parameters for requests to the FusedLocationProviderApi.
    private LocationRequest locationRequest;

    // Represents a geographical location.
    private Location location;

    // Tracks the status of the location updates request. Value changes when the user presses the
    // Start Updates and Stop Updates buttons.
    private Boolean requestingLocationUpdates;

    // Specific current context which current Util class belongs to.
    private Context context;

    public LocationUpdateUtil(Context context) {
        this.context = context;
        requestingLocationUpdates = false;
    }

    public synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    @Override
    public void onLocationChanged(Location location) {
        LocationUpdateUtil.this.location = location;
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (location == null) {
            location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        }
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(getClass().getSimpleName(), "Connection suspended");
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(getClass().getSimpleName(),
                "Connection failed: ConnectionResult.getErrorCode() = "
                        + connectionResult.getErrorCode());
    }

    /**
     * Sets up the location request.
     */
    private void createLocationRequest() {
        locationRequest = new LocationRequest();

        // Sets the inexact desired interval for active location updates.
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the exact fastest rate for active location updates.
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    private void startLocationUpdates() {
        // Call LocationListener
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, locationRequest, this);
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        // remove LocationListener.
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    /**
     * get current location.
     *
     * @return current location
     */
    public Location getCurrentLocation() {
        return location;
    }

    /**
     * Reference from online
     * Return current human readable address based on current location.
     *
     * @return human readable address string
     */
    public String getAddress() {
        // Return current human readable address
//        return getAddress(location.getLatitude(), location.getLongitude());
        Geocoder geocoder = new Geocoder(context.getApplicationContext(), Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                return String.format("%s, %s, %s",
                        address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "",
                        address.getLocality(), address.getCountryName());
            }
            return "n/a";
        } catch (Exception e) {
            Log.i(getClass().getSimpleName(), "Get Address Error");
            return "n/a";
        }
    }

    /**
     * Reference from online
     * Return current human readable address based on current location.
     *
     * @return human readable address string
     */
    public String getAddress(double lat, double lng) {
        Geocoder geocoder = new Geocoder(context.getApplicationContext(), Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                return String.format("%s, %s, %s",
                        address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "",
                        address.getLocality(), address.getCountryName());
            } else {
                return "n/a";
            }
        } catch (Exception e) {
            Log.i(getClass().getSimpleName(), "Get Address Error");
            return "n/a";
        }
    }

    public void start() {
        googleApiClient.connect();
    }

    public void resume() {
        if (googleApiClient.isConnected() && requestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    public void pause() {
        if (googleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    public void stop() {
        googleApiClient.disconnect();
    }
}

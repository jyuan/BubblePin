package bubblepin.com.bubblepin.googleMapCluster;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class BubblePinClusterItem implements ClusterItem {

    private final LatLng position;

    public BubblePinClusterItem(double lat, double lng) {
        position = new LatLng(lat, lng);
    }

    @Override
    public LatLng getPosition() {
        return position;
    }
}

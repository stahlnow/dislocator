package com.stahlnow.android.dislocator;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.data.kml.KmlContainer;
import com.google.maps.android.data.kml.KmlLayer;
import com.google.maps.android.data.kml.KmlPlacemark;
import com.google.maps.android.data.kml.KmlPoint;
import com.stahlnow.android.dislocator.model.Bone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class MyBoneReader {

    private static final String TAG = MyBoneReader.class.getSimpleName();


    public List<Bone> readKML(InputStream inputStream, GoogleMap map) throws IOException, XmlPullParserException {
        List<Bone> items = new ArrayList<>();
        KmlLayer layer = new KmlLayer(map, inputStream, DislocatorApplication.getAppContext());

        layer.addLayerToMap();

        KmlContainer kmlContainer= layer.getContainers().iterator().next();
        if (kmlContainer == null) {
            Log.d(TAG, "container is null");
        }

        // check for nested <Folder>
        if (kmlContainer.hasContainers()) {
            kmlContainer= kmlContainer.getContainers().iterator().next();
        }

        for (KmlPlacemark placemark : kmlContainer.getPlacemarks()) {
            String name = null;
            String address = null;
            String description = null;
            LatLng latLng = null;

            if (placemark.getProperty("name") != null) {
                name = placemark.getProperty("name");
            }

            if (placemark.getProperty("address") != null) {
                address = placemark.getProperty("address");
            }

            if (placemark.getProperty("description") != null) {
                description = placemark.getProperty("description");
            }

            if (placemark.getGeometry() != null) {
                KmlPoint point = (KmlPoint) placemark.getGeometry();
                latLng = point.getGeometryObject();
            }

            Bone bone = new Bone(name, address, description, latLng);

            // try to set location from address
            /*
            if (latLng == null) {
                bone.setLocationFromAddress();
            }

            // try to set address from location
            if (address == null) {
                bone.setAddressFromLocation();
            }
            */

            items.add(bone);

        }

        layer.removeLayerFromMap();

        return items;
    }



    /*
     * This matches only once in whole input,
     * so Scanner.next returns whole InputStream as a String.
     * http://stackoverflow.com/a/5445161/2183804
     */
    private static final String REGEX_INPUT_BOUNDARY_BEGINNING = "\\A";

    public List<Bone> readJSON(InputStream inputStream) throws JSONException {
        List<Bone> items = new ArrayList<>();
        String json = new Scanner(inputStream).useDelimiter(REGEX_INPUT_BOUNDARY_BEGINNING).next();
        JSONObject all = new JSONObject(json);
        JSONArray features = all.getJSONArray("features");
        for (int i = 0; i < features.length(); i++) {
            String name = null;
            String address = null;
            String description = null;
            LatLng latLng = null;

            JSONObject object = features.getJSONObject(i);

            JSONObject geom = object.getJSONObject("geometry");
            JSONArray coords = geom.getJSONArray("coordinates");
            double lng = coords.getDouble(0);
            double lat = coords.getDouble(1);
            latLng = new LatLng(lat, lng);

            JSONObject properties = object.getJSONObject("properties");

            if (!properties.isNull("name")) {
                name = properties.getString("name");
            }
            if (!object.isNull("description")) {
                description = object.getString("snippet");
            }
            items.add(new Bone(name, address, description, latLng));
        }
        return items;
    }

}


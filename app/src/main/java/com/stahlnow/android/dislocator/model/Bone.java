package com.stahlnow.android.dislocator.model;

import android.location.Address;
import android.location.Geocoder;
import android.text.TextUtils;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.stahlnow.android.dislocator.DislocatorApplication;
import com.stahlnow.android.dislocator.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Bone implements ClusterItem {
    private LatLng mPosition;
    private String mName;
    private String mAddress;
    private String mDescription;

    private int mMarkerImage;

    public Bone(LatLng latLng) {
        mPosition = latLng;
        mMarkerImage = R.drawable.marker_image;
    }

    public Bone(String name, String address, String description, LatLng latLng) {
        setTitle(name);
        setAddress(address);
        setDescription(description);
        setPosition(latLng);
        // marker image (this could be set individually instead of same id
        mMarkerImage = R.drawable.marker_image;
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }


    @Override
    public String getTitle() {
        return mName;
    }

    public String getName() {
        return getTitle();
    }

    @Override
    public String getSnippet() {
        ArrayList<String> text = new ArrayList<String>();
        if (mDescription!= null) text.add(mDescription);
        if (mAddress != null) text.add(mAddress);
        return  TextUtils.join(System.getProperty("line.separator"), text);
    }

    public String getAddress() {
        return mAddress;
    }


    public void setTitle(String title) {
        mName = title;
    }
    public void setName(String name) { setTitle(name);}
    public void setDescription(String description) {
        mDescription = description;
    }
    public void setAddress(String address) { mAddress = address; }
    public void setPosition(LatLng position) { mPosition = position; }
    public int getMarkerImage() { return mMarkerImage; }


    public String setAddressFromLocation() {

        if (mPosition != null && DislocatorApplication.isNetworkAvailable()) {
            Geocoder coder = new Geocoder(DislocatorApplication.getAppContext());
            List<Address> addresses;


            try {
                addresses = coder.getFromLocation(mPosition.latitude, mPosition.longitude, 2);
                if (addresses == null || addresses.isEmpty()) {
                    return mAddress;
                }
                Address address = addresses.get(0);
                ArrayList<String> addressFragments = new ArrayList<String>();

                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    addressFragments.add(address.getAddressLine(i));
                }

                setAddress(TextUtils.join(System.getProperty("line.separator"), addressFragments));

                //Log.d(TAG, addr);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return mAddress;
    }

    public LatLng setLocationFromAddress() {

        if (mAddress != null && !mAddress.isEmpty() && DislocatorApplication.isNetworkAvailable()) {
            Geocoder coder = new Geocoder(DislocatorApplication.getAppContext());
            List<Address> addresses;


            try {
                // May throw an IOException
                addresses = coder.getFromLocationName(mAddress, 3);
                if (addresses == null || addresses.isEmpty()) {
                    return null;
                }
                Address location = addresses.get(0);
                location.getLatitude();
                location.getLongitude();

                setPosition(new LatLng(location.getLatitude(), location.getLongitude()));

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return mPosition;
    }

}

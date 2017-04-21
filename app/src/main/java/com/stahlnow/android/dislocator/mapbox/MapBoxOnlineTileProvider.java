package com.stahlnow.android.dislocator.mapbox;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.android.gms.maps.model.UrlTileProvider;


public class MapBoxOnlineTileProvider extends UrlTileProvider {

    private static final String FORMAT;

    static {
        FORMAT = "https://api.tiles.mapbox.com/v4/%s/%d/%d/%d@2x.png?access_token=%s";
    }

    // ------------------------------------------------------------------------
    // Instance Variables
    // ------------------------------------------------------------------------

    private boolean mHttpsEnabled;

    private String mMapIdentifier;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    public MapBoxOnlineTileProvider(String mapIdentifier) {
        this(mapIdentifier, false);
    }

    public MapBoxOnlineTileProvider(String mapIdentifier, boolean https) {
        super(256, 256);

        this.mHttpsEnabled = https;
        this.mMapIdentifier = mapIdentifier;
    }

    // ------------------------------------------------------------------------
    // Public Methods
    // ------------------------------------------------------------------------

    /**
     * The MapBox map identifier being used by this provider.
     *
     * @return the MapBox map identifier being used by this provider.
     */
    public String getMapIdentifier() {
        return this.mMapIdentifier;
    }

    /**
     * Sets the identifier of the MapBox hosted map you wish to use.
     *
     * @param aMapIdentifier the identifier of the map.
     */
    public void setMapIdentifier(String aMapIdentifier) {
        this.mMapIdentifier = aMapIdentifier;
    }

    /**
     * Whether this provider will use HTTPS when requesting tiles.
     *
     * @return {@link true} if HTTPS is enabled on this provider.
     */
    public boolean isHttpsEnabled() {
        return this.mHttpsEnabled;
    }

    /**
     * Sets whether this provider should use HTTPS when requesting tiles.
     *
     * @param enabled
     */
    public void setHttpsEnabled(boolean enabled) {
        this.mHttpsEnabled = enabled;
    }

    @Override
    public URL getTileUrl(int x, int y, int z) {
        try {
            //String protocol = this.mHttpsEnabled ? "https" : "http";
            return new URL(String.format(FORMAT, this.mMapIdentifier, z, x, y, "sk.eyJ1Ijoic3RhaGwiLCJhIjoiY2oxcG8yeHl2MDAwNjJxbXpsMno2czJnZCJ9.5UYncB0OY6oFyJQC8QHaGg"));
        }
        catch (MalformedURLException e) {
            return null;
        }
    }

}

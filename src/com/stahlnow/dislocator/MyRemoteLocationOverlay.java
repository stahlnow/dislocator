/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.stahlnow.dislocator;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.overlay.Circle;
import org.mapsforge.android.maps.overlay.Marker;
import org.mapsforge.android.maps.overlay.Overlay;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.GeoPoint;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.util.MercatorProjection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;

/**
 * A thread-safe {@link Overlay} implementation to display a {@link Circle} and a {@link Drawable} at the user's current
 * location.
 */
public class MyRemoteLocationOverlay implements LocationListener, Overlay {
        private static final int UPDATE_DISTANCE = 0;
        private static final int UPDATE_INTERVAL = 1000;

        /**
         * @param location
         *            the location whose geographical coordinates should be converted.
         * @return a new GeoPoint with the geographical coordinates taken from the given location.
         */
        public static GeoPoint locationToGeoPoint(Location location) {
                return new GeoPoint(location.getLatitude(), location.getLongitude());
        }

        private static Paint getDefaultCircleFill() {
                return getPaint(Style.FILL, Color.BLUE, 48);
        }

        private static Paint getDefaultCircleStroke() {
                Paint paint = getPaint(Style.STROKE, Color.BLUE, 128);
                paint.setStrokeWidth(2);
                return paint;
        }

        private static Paint getPaint(Style style, int color, int alpha) {
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setStyle(style);
                paint.setColor(color);
                paint.setAlpha(alpha);
                return paint;
        }

        private boolean centerAtNextFix;
        private final Circle circle;
        private Location lastLocation;
        private final LocationManager locationManager;
        private final MapView mapView;
        private final Marker marker;
        private boolean myLocationEnabled;
        private boolean snapToLocationEnabled;
        
        private GeoPoint remote;
        private GeoPoint local;

        /**
         * Constructs a new {@code MyLocationOverlay} with the given drawable and the default circle paints.
         * 
         * @param context
         *            a reference to the application context.
         * @param mapView
         *            the {@code MapView} on which the location will be displayed.
         * @param drawable
         *            a drawable to display at the current location (might be null).
         */
        public MyRemoteLocationOverlay(Context context, MapView mapView, Drawable drawable, GeoPoint remote, GeoPoint local) {
                this(context, mapView, drawable, getDefaultCircleFill(), getDefaultCircleStroke(), remote, local);
        }

        /**
         * Constructs a new {@code MyLocationOverlay} with the given drawable and circle paints.
         * 
         * @param context
         *            a reference to the application context.
         * @param mapView
         *            the {@code MapView} on which the location will be displayed.
         * @param drawable
         *            a drawable to display at the current location (might be null).
         * @param circleFill
         *            the {@code Paint} used to fill the circle that represents the current location (might be null).
         * @param circleStroke
         *            the {@code Paint} used to stroke the circle that represents the current location (might be null).
         */
        public MyRemoteLocationOverlay(Context context, MapView mapView, Drawable drawable, Paint circleFill, Paint circleStroke, GeoPoint remote, GeoPoint local) {
                this.mapView = mapView;
                this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                this.marker = new Marker(null, drawable);
                this.circle = new Circle(null, 0, circleFill, circleStroke);
                this.remote = remote;
                this.local = local;
        }

        /**
         * Stops the receiving of location updates. Has no effect if location updates are already disabled.
         */
        public synchronized void disableMyLocation() {
                if (this.myLocationEnabled) {
                        this.myLocationEnabled = false;
                        this.locationManager.removeUpdates(this);
                        this.mapView.getOverlayController().redrawOverlays();
                }
        }

        @Override
        public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas) {
                if (!this.myLocationEnabled) {
                        return;
                }

                double canvasPixelLeft = MercatorProjection.longitudeToPixelX(boundingBox.minLongitude, zoomLevel);
                double canvasPixelTop = MercatorProjection.latitudeToPixelY(boundingBox.maxLatitude, zoomLevel);
                Point canvasPosition = new Point(canvasPixelLeft, canvasPixelTop);
                this.circle.draw(boundingBox, zoomLevel, canvas, canvasPosition);
                this.marker.draw(boundingBox, zoomLevel, canvas, canvasPosition);
        }

        /**
         * Enables the receiving of location updates from the most accurate {@link LocationProvider} available.
         * 
         * @param centerAtFirstFix
         *            whether the map should be centered to the first received location fix.
         * @return true if at least one location provider was found, false otherwise.
         */
        public synchronized boolean enableMyLocation(boolean centerAtFirstFix) {
                if (!enableBestAvailableProvider()) {
                        return false;
                }

                this.centerAtNextFix = centerAtFirstFix;
                return true;
        }

        /**
         * @return the most-recently received location fix (might be null).
         */
        public synchronized Location getLastLocation() {
                return this.lastLocation;
        }

        /**
         * @return true if the map will be centered at the next received location fix, false otherwise.
         */
        public synchronized boolean isCenterAtNextFix() {
                return this.centerAtNextFix;
        }

        /**
         * @return true if the receiving of location updates is currently enabled, false otherwise.
         */
        public synchronized boolean isMyLocationEnabled() {
                return this.myLocationEnabled;
        }

        /**
         * @return true if the snap-to-location mode is enabled, false otherwise.
         */
        public synchronized boolean isSnapToLocationEnabled() {
                return this.snapToLocationEnabled;
        }

        @Override
        public void onLocationChanged(Location location) {
                synchronized (this) {
                	
                        this.lastLocation = location;
                        
                        GeoPoint geoPoint = locationToGeoPoint(location);

                        /* hack remote location marker to reflect local changes */
                        double localLatDiff = geoPoint.latitude - local.latitude;
                        location.setLatitude(remote.latitude + localLatDiff);
                        double localLongDiff = geoPoint.longitude - local.longitude;
                        location.setLongitude(remote.longitude + localLongDiff);
                        // and again convert location
                        geoPoint = locationToGeoPoint(location);
                        /* end of hack */
                        
                        
                        this.marker.setGeoPoint(geoPoint);
                        this.circle.setGeoPoint(geoPoint);
                        this.circle.setRadius(location.getAccuracy());

                        if (this.centerAtNextFix || this.snapToLocationEnabled) {
                                this.centerAtNextFix = false;
                                this.mapView.getMapViewPosition().setCenter(geoPoint);
                        }
                }
                this.mapView.getOverlayController().redrawOverlays();
        }

        @Override
        public void onProviderDisabled(String provider) {
                enableBestAvailableProvider();
        }

        @Override
        public void onProviderEnabled(String provider) {
                enableBestAvailableProvider();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
                // do nothing
        }

        /**
         * @param snapToLocationEnabled
         *            whether the map should be centered at each received location fix.
         */
        public synchronized void setSnapToLocationEnabled(boolean snapToLocationEnabled) {
                this.snapToLocationEnabled = snapToLocationEnabled;
        }

        private synchronized boolean enableBestAvailableProvider() {
                disableMyLocation();

                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                String bestAvailableProvider = this.locationManager.getBestProvider(criteria, true);
                if (bestAvailableProvider == null) {
                        return false;
                }

                this.locationManager.requestLocationUpdates(bestAvailableProvider, UPDATE_INTERVAL, UPDATE_DISTANCE, this);
                this.myLocationEnabled = true;
                return true;
        }
}
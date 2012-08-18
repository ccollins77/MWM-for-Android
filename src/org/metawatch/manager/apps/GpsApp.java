package org.metawatch.manager.apps;

import org.metawatch.manager.Application;
import org.metawatch.manager.CoordinateConvert;
import org.metawatch.manager.CoordinateConvert.LatLongRef;
import org.metawatch.manager.CoordinateConvert.OsGridRef;
import org.metawatch.manager.FontCache;
import org.metawatch.manager.Idle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;

/**
 * This app shows GPS location (lat & long or SD) and current velocity according to GPS.
 * @author william
 *
 */
public class GpsApp extends ApplicationBase implements LocationListener {
	
	public final static String APP_ID = "org.metawatch.manager.apps.GpsApp";
	
	protected LocationManager locationManager;
	
	protected boolean gpsEnabled = true;
	protected int gpsStatus;
	protected Location location;
	protected Context context;

	@Override
	public AppData getInfo() {
		return new AppData() {{
			id = APP_ID;
			name = "GPS";
			
			supportsDigital = true;
			supportsAnalog = false;
		
			pageSettingKey = "GpsApp";
			pageSettingAttribute = "gpsApp";
		}};
	}

	@Override
	public void activate(Context context, int watchType) {
		this.context = context;
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		Criteria c = new Criteria();
		c.setAccuracy(Criteria.ACCURACY_FINE);
		c.setSpeedRequired(true);
		String name = locationManager.getBestProvider(c, true);

		locationManager.requestLocationUpdates(name, 5000, 10, this);
	}

	@Override
	public void deactivate(Context context, int watchType) {
		this.context = null;
		locationManager.removeUpdates(this);
	}

	@Override
	public Bitmap update(Context context, boolean preview, int watchType) {
		Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);	
		
		final TextPaint paintSmall = new TextPaint();
		paintSmall.setColor(Color.BLACK);
		paintSmall.setTextSize(FontCache.instance(context).Small.size);
		paintSmall.setTypeface(FontCache.instance(context).Small.face);
		
		double lat = location == null ? 0 : location.getLatitude();
		double lng = location == null ? 0 : location.getLongitude();
		
		OsGridRef gridRef = CoordinateConvert.latLongToOs(new LatLongRef(lat, lng));
		
		StaticLayout latlong = new StaticLayout(String.format("%f\n%f\n%s\n%s",
				lat, lng, gpsEnabled ? "Enabled" : "Disabled",
						gridRef.toString()),
				paintSmall, 96, Alignment.ALIGN_NORMAL, 1, 0, false);
		latlong.draw(canvas);
		
		return bitmap;
	}

	@Override
	public int buttonPressed(Context context, int id) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void onLocationChanged(Location location) {
		this.location = location;
		if (appState == ApplicationBase.ACTIVE_IDLE)
			Idle.updateIdle(context, true);
		else if (appState == ApplicationBase.ACTIVE_POPUP)
			Application.updateAppMode(context);
	}

	public void onProviderDisabled(String provider) {
		gpsEnabled = false;
		if (appState == ApplicationBase.ACTIVE_IDLE)
			Idle.updateIdle(context, true);
		else if (appState == ApplicationBase.ACTIVE_POPUP)
			Application.updateAppMode(context);
	}

	public void onProviderEnabled(String provider) {
		gpsEnabled = true;
		if (appState == ApplicationBase.ACTIVE_IDLE)
			Idle.updateIdle(context, true);
		else if (appState == ApplicationBase.ACTIVE_POPUP)
			Application.updateAppMode(context);
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		gpsStatus = status;
		if (appState == ApplicationBase.ACTIVE_IDLE)
			Idle.updateIdle(context, true);
		else if (appState == ApplicationBase.ACTIVE_POPUP)
			Application.updateAppMode(context);
	}
}

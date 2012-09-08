package org.metawatch.manager.apps;

import java.util.Iterator;

import org.metawatch.manager.Application;
import org.metawatch.manager.CoordinateConvert;
import org.metawatch.manager.CoordinateConvert.LatLongRef;
import org.metawatch.manager.CoordinateConvert.OsGridRef;
import org.metawatch.manager.FontCache;
import org.metawatch.manager.Idle;
import org.metawatch.manager.MetaWatchService;
import org.metawatch.manager.MetaWatchService.WatchType;
import org.metawatch.manager.Protocol;
import org.metawatch.manager.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
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
public class GpsApp extends ApplicationBase implements LocationListener, Listener {
	
	public final static String APP_ID = "org.metawatch.manager.apps.GpsApp";
	
	protected LocationManager locationManager;
	
	protected boolean gpsEnabled = true;
	protected int gpsStatusCode;
	protected GpsStatus gpsStatus;
	protected Location location;
	protected Context context;
	
	static final int MODE_OS = 1;
	
	private int mode;
	
	private int precision = 6;
	
	static final int CHANGE_PRECISION = 10;
	
	TextPaint paintSmall, paintMedium, paintLarge;

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
		mode = MODE_OS;
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		Criteria c = new Criteria();
		c.setAccuracy(Criteria.ACCURACY_FINE);
		c.setSpeedRequired(true);
		final String name = locationManager.getBestProvider(c, true);
		
		
		MetaWatchService.getUiThreadHandler().post(new Runnable() {
			public void run() {
				locationManager.requestLocationUpdates(name, 5000, 10, GpsApp.this);
				locationManager.addGpsStatusListener(GpsApp.this);
			}
		});
		
		paintSmall = new TextPaint();
		paintSmall.setColor(Color.BLACK);
		paintSmall.setTextSize(FontCache.instance(context).Small.size);
		paintSmall.setTypeface(FontCache.instance(context).Small.face);
		paintMedium = new TextPaint();
		paintMedium.setColor(Color.BLACK);
		paintMedium.setTextSize(FontCache.instance(context).Medium.size);
		paintMedium.setTypeface(FontCache.instance(context).Medium.face);
		paintLarge = new TextPaint();
		paintLarge.setColor(Color.BLACK);
		paintLarge.setTextSize(FontCache.instance(context).Large.size);
		paintLarge.setTypeface(FontCache.instance(context).Large.face);
		
		if (watchType == WatchType.DIGITAL) {
			Protocol.enableButton(2, 1, CHANGE_PRECISION, MetaWatchService.WatchBuffers.APPLICATION); // right bottom - press
		}
	}

	@Override
	public void deactivate(Context context, int watchType) {
		this.context = null;
		paintSmall = null;
		
		MetaWatchService.getUiThreadHandler().post(new Runnable() {
			public void run() {
				locationManager.removeUpdates(GpsApp.this);
				locationManager.removeGpsStatusListener(GpsApp.this);
			}
		});
		if (watchType == WatchType.DIGITAL) {
			Protocol.disableButton(2, 1, MetaWatchService.WatchBuffers.APPLICATION);
		}
	}

	@Override
	public Bitmap update(Context context, boolean preview, int watchType) {
		Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);	
		
		int satellitesUsed = 0, satellites = 0;
		if(gpsStatus != null) {
			Iterator<GpsSatellite> it = gpsStatus.getSatellites().iterator();
			while(it.hasNext()) {
				GpsSatellite sat = it.next();
				if(sat.usedInFix()) satellitesUsed++;
				satellites++;
			}
		}
		
		if(gpsEnabled) {
			if(satellitesUsed > 0)
				canvas.drawBitmap(Utils.getBitmap(context, "gps.png"), 5, 5, null);
			else
				canvas.drawBitmap(Utils.getBitmap(context, "gps_nosignal.png"), 5, 5, null);
			
			double lat = location == null ? 0 : location.getLatitude();
			double lng = location == null ? 0 : location.getLongitude();
			
			switch(mode) {
			case MODE_OS:
				OsGridRef gridRef = CoordinateConvert.latLongToOs(new LatLongRef(lat, lng));
				canvas.save();
				StaticLayout latlong = new StaticLayout(String.format("%s\n%s",
						gridRef.getLetters(),
						gridRef.getNumbers(precision)),
						paintLarge, 96, Alignment.ALIGN_CENTER, 1, 0, false);
				canvas.translate(0, 20);
				latlong.draw(canvas);
				canvas.restore();
				break;
			}
			
			canvas.save();
			StaticLayout latlong = new StaticLayout(String.format("%f\n%f",
					lat, lng),
					paintMedium, 91, Alignment.ALIGN_OPPOSITE, 1, 0, false);
			canvas.translate(0, 91 - latlong.getHeight());
			latlong.draw(canvas);
			canvas.restore();
			
			canvas.save();
			StaticLayout satCount = new StaticLayout(String.format("%d / %d",
					satellitesUsed, satellites),
					paintLarge, 50, Alignment.ALIGN_NORMAL, 1, 0, false);
			canvas.translate(5, 91 - satCount.getHeight());
			satCount.draw(canvas);
			canvas.restore();
		} else {
			canvas.drawBitmap(Utils.getBitmap(context, "gps_off.png"), 5, 5, null);
			canvas.save();
			StaticLayout disabled = new StaticLayout("GPS Disabled",
					paintLarge, 86, Alignment.ALIGN_CENTER, 1, 0, false);
			canvas.translate(5, 20);
			disabled.draw(canvas);
			canvas.restore();
		}
		
		return bitmap;
	}

	@Override
	public int buttonPressed(Context context, int id) {
		switch(id) {
		case CHANGE_PRECISION:
			precision += 2;
			if(precision > 10) precision = 6;
			return BUTTON_USED;
		}
		return BUTTON_NOT_USED;
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

	public void onStatusChanged(String provider, int status, Bundle extras) { }

	public void onGpsStatusChanged(int status) {
		gpsStatusCode = status;
		gpsStatus = locationManager.getGpsStatus(null);
		if (appState == ApplicationBase.ACTIVE_IDLE)
			Idle.updateIdle(context, true);
		else if (appState == ApplicationBase.ACTIVE_POPUP)
			Application.updateAppMode(context);
	}
}

package com.example.busme;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.LocationManager;

public class LocationTracker extends BroadcastReceiver {
	public static final int LOCATION_UPDATE_REQUEST = 3354897;
	public static final String LOCATION_BROADCAST_INTENT = "LOC_GEN_BROADCAST";

	public static IntentFilter getLocationBroadcastIntentFilter() {
		IntentFilter yo = new IntentFilter(LOCATION_BROADCAST_INTENT);
		yo.addAction(LOCATION_BROADCAST_INTENT);
		return yo;
	}

	@Override
	public void onReceive(Context c, Intent i) {
		LocationManager lm = (LocationManager) c
				.getSystemService(Context.LOCATION_SERVICE);
		Intent locationIntent = new Intent(LOCATION_BROADCAST_INTENT);
		PendingIntent pi = PendingIntent.getBroadcast(c,
				LOCATION_UPDATE_REQUEST, locationIntent,
				PendingIntent.FLAG_CANCEL_CURRENT);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		lm.requestSingleUpdate(criteria, pi);
	}

}

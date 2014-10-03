package com.example.busme;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public class BusDataCollector {
	private static Context context;

	private BusDataCollector() {
	}

	public static void initialize(Context c) {
		if (context == null) {
			context = c;
		} else {
			Log.e("ERROR", "MainModel was instantiated twice");
		}
	}

	// public static ArrayList<MainListViewItem> getCardsForQuery(String start,
	// String destination) {
	// return null;
	// }

	@Deprecated
	public static ArrayList<MainListViewItem> getCardsForQuery(
			String routeStart, String routeEnd) {
		if (context == null) {
			return null;
		}
		LocationManager locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		Criteria crit = new Criteria();
		String provider = locationManager.getBestProvider(crit, true);
		Location loc = locationManager.getLastKnownLocation(provider);

		ArrayList<MainListViewItem> results = new ArrayList<MainListViewItem>();
		JSONArray buses;
		if (routeStart.contentEquals(MainModel.LOCATION_UNSPECIFIED)
				&& routeEnd.contentEquals(MainModel.LOCATION_UNSPECIFIED)) {
			// This should query the database for the user's default suggestions
			buses = MainModel.getJSONArrayForURL("/routes/default/" + MainModel.getDeviceId() + "/"
					+ loc.getLatitude() + "/" + loc.getLongitude() + "/"
					+ routeEnd.replace(" ", "_"));
		} else if (routeStart.contentEquals(MainModel.LOCATION_UNSPECIFIED)
				|| routeStart.contentEquals(MainModel.CURRENT_LOCATION)) {
			buses = MainModel.getJSONArrayForURL("/routes/fromcurrent/" + MainModel.getDeviceId() + "/"
					+ loc.getLatitude() + "/" + loc.getLongitude() + "/"
					+ routeEnd.replace(" ", "_"));
		} else {
			// This should query the data base for suggestions based on a
			// specified start and destination
			buses = MainModel.getJSONArrayForURL("/routes/fromcustom/" + MainModel.getDeviceId() + "/"
					+ routeStart.replace(" ", "_") + "/"
					+ routeEnd.replace(" ", "_"));
		}

		JSONObject currentRoute;
		for (int i = 0; i < buses.length(); i++) {
			try {
				currentRoute = buses.getJSONObject(i);
				if (currentRoute.has("err")) {
					// Toast.makeText(c, "hello", Toast.LENGTH_SHORT).show(); //
					// TODO test this
					return null;
				}
				results.add(new MainListViewItem(
						currentRoute.getString("next_bus"),
						currentRoute.getString("route_numbers"),
						currentRoute.getString("start"),
						currentRoute.getString("destination"),
						Double.parseDouble(currentRoute.getString("start_lat")),
						Double.parseDouble(currentRoute.getString("start_lng")),
						Double.parseDouble(currentRoute.getString("dest_lat")),
						Double.parseDouble(currentRoute.getString("dest_lng")),
						currentRoute.getString("travel_time")));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		if(results.size() == 0){
			return null;
		}
		return results;
	}
}

package com.example.busme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;

import com.google.android.gms.maps.model.LatLng;

public class MainModel {
	public static final String CURRENT_LOCATION = "";
	public static final String LOCATION_UNSPECIFIED = "";
	private static final String BASE_URL = "http://www.theseedok.com/api";
	private Context c;

	public MainModel(Context c) {
		this.c = c;
	}

	/**
	 * Gets JSON from the server at "BASE_URL + apiURL"
	 * 
	 * @param apiURL
	 * @return
	 */
	private JSONArray getJSONArrayForURL(String apiURL) {
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(BASE_URL + apiURL);
			HttpResponse response = client.execute(request);

			// Get the response
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent(), "UTF-8"));
			StringBuilder builder = new StringBuilder();
			for (String line = null; (line = reader.readLine()) != null;) {
				builder.append(line).append("\n");
			}
			System.out.println("builder tostring" + builder.toString());
			JSONTokener tokener = new JSONTokener(builder.toString());
			JSONArray finalResult = new JSONArray(tokener);
			return finalResult;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public ArrayList<MainListViewItem> getCardsForQuery(String routeStart,
			String routeEnd) {
		LocationManager locationManager = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
		Criteria crit = new Criteria();
		String provider = locationManager.getBestProvider(crit, true);
		Location loc = locationManager.getLastKnownLocation(provider);
		
		ArrayList<MainListViewItem> results = new ArrayList<MainListViewItem>();
		JSONArray buses;
		if (routeStart.contentEquals(LOCATION_UNSPECIFIED)
				&& routeEnd.contentEquals(LOCATION_UNSPECIFIED)) {
			// This should query the database for the user's default suggestions
			buses = getJSONArrayForURL("/routes/default/" + "5" + "/"
					+ loc.getLatitude() + "/" + loc.getLongitude() + "/"
					+ routeEnd.replace(" ","_"));
		} else if (routeStart.contentEquals(LOCATION_UNSPECIFIED)
				|| routeStart.contentEquals(CURRENT_LOCATION)) {
			buses = getJSONArrayForURL("/routes/fromcurrent/"
					+ loc.getLatitude() + "/" + loc.getLongitude() + "/"
					+ routeEnd.replace(" ","_"));
		} else {
			// This should query the data base for suggestions based on a
			// specified start and destination
			buses = getJSONArrayForURL("/routes/fromcustom/"
					+ routeStart.replace(" ", "_") + "/" + routeEnd.replace(" ","_"));
		}
		
		System.out.print("dude buses" + buses.length());
		JSONObject currentRoute;
		for (int i = 0; i < buses.length(); i++) {
			try {
				currentRoute = buses.getJSONObject(i);
				results.add(new MainListViewItem(Integer.parseInt(currentRoute
						.getString("next_bus")), Integer.parseInt(currentRoute
						.getString("route_number")), currentRoute
						.getString("start"), currentRoute
						.getString("destination"),
						Double.parseDouble(currentRoute.getString("start_lat")),
						Double.parseDouble(currentRoute.getString("start_lng")),
						Double.parseDouble(currentRoute.getString("dest_lat")),
						Double.parseDouble(currentRoute.getString("dest_lng")),
						currentRoute.getString("travel_time")
						));
				System.out.println("dude currentroute" + currentRoute.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		return results;
	}
}

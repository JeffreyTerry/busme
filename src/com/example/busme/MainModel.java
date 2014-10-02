package com.example.busme;

import java.io.BufferedReader;
import java.io.FileOutputStream;
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
import android.util.Log;
import android.widget.Toast;

public class MainModel {
	public static final String ROUTE_LINE_DATA_FILE_BASE_NAME = "route_lines_";
	public static final String STOP_LOCATION_DATA_FILE = "stop_locations";
	public static final String STOP_ID_DATA_FILE = "stop_ids";
	public static final String CURRENT_LOCATION = "";
	public static final String LOCATION_UNSPECIFIED = "";
	private static final String BASE_URL = "http://www.theseedok.com/api";
	private static Context c;

	public MainModel(Context c) {
		if (MainModel.c == null) {
			MainModel.c = c;
		} else {
			Log.e("ERROR", "MainModel was instantiated twice");
		}
	}

	/**
	 * Gets a JSON object from the server at "BASE_URL + apiURL"
	 * 
	 * @param apiURL
	 * @return
	 */
	public static JSONObject getJSONObjectForURL(String apiURL) {
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
			JSONTokener tokener = new JSONTokener(builder.toString());
			JSONObject finalResult = new JSONObject(tokener);
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

	/**
	 * Gets a JSON array from the server at "BASE_URL + apiURL"
	 * 
	 * @param apiURL
	 * @return
	 */
	public static JSONArray getJSONArrayForURL(String apiURL) {
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

	public static void saveToFile(String data, String filename) {
		try {
			FileOutputStream fos = c.openFileOutput(filename,
					Context.MODE_PRIVATE);
			fos.write(data.getBytes());
			fos.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public static String readFromFile(String filename) {
		try {
			BufferedReader bis = new BufferedReader(new InputStreamReader(
					c.openFileInput(filename)));
			String next;
			String result = "";
			while ((next = bis.readLine()) != null) {
				result += next;
			}
			if(result.contentEquals("")) {
				return null;
			} else {
				return result;
			}
		} catch (IOException e1) {
			return null;
		}
	}

	public ArrayList<MainListViewItem> getCardsForQuery(String routeStart,
			String routeEnd) {
		LocationManager locationManager = (LocationManager) c
				.getSystemService(Context.LOCATION_SERVICE);
		Criteria crit = new Criteria();
		String provider = locationManager.getBestProvider(crit, true);
		Location loc = locationManager.getLastKnownLocation(provider);

		ArrayList<MainListViewItem> results = new ArrayList<MainListViewItem>();
		JSONArray buses;
		if (routeStart.contentEquals(LOCATION_UNSPECIFIED)
				&& routeEnd.contentEquals(LOCATION_UNSPECIFIED)) {
			// This should query the database for the user's default suggestions
			buses = getJSONArrayForURL("/routes/default/"
					+ MainActivity.getId() + "/" + loc.getLatitude() + "/"
					+ loc.getLongitude() + "/" + routeEnd.replace(" ", "_"));
		} else if (routeStart.contentEquals(LOCATION_UNSPECIFIED)
				|| routeStart.contentEquals(CURRENT_LOCATION)) {
			buses = getJSONArrayForURL("/routes/fromcurrent/"
					+ MainActivity.getId() + "/" + loc.getLatitude() + "/"
					+ loc.getLongitude() + "/" + routeEnd.replace(" ", "_"));
		} else {
			// This should query the data base for suggestions based on a
			// specified start and destination
			buses = getJSONArrayForURL("/routes/fromcustom/"
					+ MainActivity.getId() + "/" + routeStart.replace(" ", "_")
					+ "/" + routeEnd.replace(" ", "_"));
		}

		JSONObject currentRoute;
		for (int i = 0; i < buses.length(); i++) {
			try {
				currentRoute = buses.getJSONObject(i);
				if (currentRoute.has("err")) {
//					Toast.makeText(c, "hello", Toast.LENGTH_SHORT).show(); // TODO test this
					return results;
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

		return results;
	}
}

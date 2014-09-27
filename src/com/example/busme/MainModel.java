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

public class MainModel {
	public static final String LOCATION_UNSPECIFIED = "";
	private static final String BASE_URL = "http://theseedok.com/api";

	public MainModel() {
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
		ArrayList<MainListViewItem> results = new ArrayList<MainListViewItem>();
		if (routeStart.contentEquals(LOCATION_UNSPECIFIED)
				&& routeEnd.contentEquals(LOCATION_UNSPECIFIED)) {
			// This should query the database for the user's default suggestions
			results.add(new MainListViewItem(35, 11, "Yolo Hall",
					"Juice Center"));
			results.add(new MainListViewItem(15, 11, "Yolo Hall",
					"South Hill Business Park"));
			results.add(new MainListViewItem(10, 11, "Blaze Center",
					"Juice Hall"));
			results.add(new MainListViewItem(5, 10, "Blaze Center",
					"South Hill Business Park"));
			results.add(new MainListViewItem(27, 11, "Gates Hall", "Juice Hall"));
			results.add(new MainListViewItem(15, 10, "Gates Hall",
					"South Hill Business Park"));
		} else if (routeStart.contentEquals(LOCATION_UNSPECIFIED)) {
			// This should query the data base for suggestions based on a
			// specified destination
			String currentLocation = "Your location";
			return getCardsForQuery(currentLocation, routeEnd);
		} else {
			// This should query the data base for suggestions based on a
			// specified start and destination
			JSONArray routes = getJSONArrayForURL("/route");
			JSONObject currentRoute;
			for (int i = 0; i < routes.length(); i++) {
				try {
					currentRoute = routes.getJSONObject(i);
					results.add(new MainListViewItem(Integer
							.parseInt(currentRoute.getString("next_bus")),
							Integer.parseInt(currentRoute
									.getString("route_number")), currentRoute
									.getString("start"), currentRoute
									.getString("destination")));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return results;
	}
}

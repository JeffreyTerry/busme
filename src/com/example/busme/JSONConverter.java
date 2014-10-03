package com.example.busme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLng;

public class JSONConverter {
	/**
	 * Puts {stopname: [lat, lng], ...} items in a JSONObject in a HashMap and
	 * returns the HashMap
	 * 
	 * @param json
	 *            The JSON to parse
	 * @return A HashMap<String, LatLng> of the results
	 */
	public static HashMap<String, LatLng> convertStopLatLngsToHashMap(
			JSONObject json) {
		// list of LatLng of the selected route.
		HashMap<String, LatLng> map = new HashMap<String, LatLng>();
		if (json != null) {
			Iterator<?> keys = json.keys();

			while (keys.hasNext()) {
				String key = (String) keys.next();
				try {
					JSONArray array = (JSONArray) json.get(key);
					LatLng stopLocation = new LatLng(array.getDouble(0),
							array.getDouble(1));
					map.put(key, stopLocation);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

		}
		return map;
	}

	/**
	 * Puts {stopname: stopid, ...} items in a JSONObject in a HashMap and
	 * returns the HashMap
	 * 
	 * @param json
	 *            The JSON to parse
	 * @return A HashMap<String, LatLng> of the results
	 */
	public static HashMap<String, String> convertStopIdsToHashMap(
			JSONObject json) {
		// list of LatLng of the selected route.
		HashMap<String, String> map = new HashMap<String, String>();
		if (json != null) {
			Iterator<?> keys = json.keys();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				try {
					map.put(key, json.getString(key));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

		}
		return map;
	}

	/**
	 * Converts a JSONArray of [lat, lng] number pairs into an ArrayList of
	 * LatLng objects
	 * 
	 * @param array
	 *            The JSONArray to convert
	 * @return The new ArrayList
	 */
	public static ArrayList<LatLng> convertRouteArrayToHashMap(JSONArray array) {
		// list of LatLng of the selected route.
		ArrayList<LatLng> list = new ArrayList<LatLng>();
		if (array != null) {
			for (int i = 0; i < array.length(); i++) {
				try {
					JSONArray innerArray = array.getJSONArray(i);
					list.add(new LatLng(innerArray.getDouble(0), innerArray
							.getDouble(1)));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return list;
	}

}

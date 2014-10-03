package com.example.busme;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class BusDataCollector {
	private static Context context;
	private static Geocoder geocoder;
	private static HashMap<String, LatLng> stopToLatLngs = null;
	private static HashMap<String, String> stopToTcatIds = null;
	private static final int NUMBER_OF_NEARBY_STOPS_TO_LOOK_AT = 2;
	private static final int NUMBER_OF_FUTURE_DATES_TO_QUERY = 4;

	private BusDataCollector() {
	}

	public static void initialize(Context c) {
		if (context == null) {
			context = c;
			if(Geocoder.isPresent()) {
				geocoder = new Geocoder(context);
			} else {
				geocoder = null;
			}
			try {
				stopToLatLngs = JSONConverter.convertStopLatLngsToHashMap(new JSONObject(MainModel.getStopToLatLngDictionaryData()));
				stopToTcatIds = JSONConverter.convertStopIdsToHashMap(new JSONObject(MainModel.getStopToTcatIdDictionaryData()));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			Log.e("ERROR", "MainModel was initialized twice");
		}
	}
	
	/**
	 * This returns a set of cards for different user queries. It should never be called on the Main UI thread.
	 * @param routeStart  A string query for the route's starting point. Can be an address, a place in Ithaca, or a bus stop name. If the start is equal to MainModel.LOCATION_UNSPECIFIED, the user's current location will be used as a starting point.
	 * @param routeEnd    A string query for the route's starting point. Can be an address, a place in Ithaca, or a bus stop name. If the end is equal to MainModel.LOCATION_UNSPECIFIED and the start is also equal to MainModel.LOCATION_UNSPECIFIED, the user's default cards will be returned. If only the end is equal to MainModel.LOCATION_UNSPECIFIED, the cards returned will be for all buses coming to the start stop.
	 * @return
	 */
	public static ArrayList<MainListViewItem> getCardsForQuery(
			String routeStart, String routeEnd) {
		if(context == null) {
			return null;
		}
		if(routeStart.contentEquals(MainModel.LOCATION_UNSPECIFIED)) {
			if(routeEnd.contentEquals(MainModel.LOCATION_UNSPECIFIED)) {
				// this should grab the user's default cards TODO
				return null;
			} else {
				// this should grab cards based on the current location
				LocationManager locationManager = (LocationManager) context
						.getSystemService(Context.LOCATION_SERVICE);
				Criteria crit = new Criteria();
				String provider = locationManager.getBestProvider(crit, true);
				Location currentLocation = locationManager.getLastKnownLocation(provider);
				
				LatLng endLatLng = getLatLngForSearchTerms(routeEnd);
				if(endLatLng == null) {
					return null;
				}
				
				return getCardsForLatLngs(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), endLatLng);
			}
		} else if(routeEnd.contentEquals(MainModel.LOCATION_UNSPECIFIED)) {
			// this should grab cards showing buses coming out of a single location TODO
			return null;
		} else {
			// this should grab cards based on a start and an end location
			LatLng startLatLng = getGeocodedLatLng(routeStart);
			LatLng endLatLng = getGeocodedLatLng(routeEnd);
			
			if(startLatLng == null || endLatLng == null) {
				return null;
			}
			
			return getCardsForLatLngs(startLatLng, endLatLng);
		}
	}
	
	private static LatLng getLatLngForSearchTerms(String query) {
		String bestMatch = null;
		int bestScore = 0;
		
		String[] terms = query.split(" ");
		int currentScore;
		for(String stop: stopToLatLngs.keySet()) {
			currentScore = 0;
			for(int i = 0; i < terms.length; i++) {
				if(stop.toLowerCase(Locale.US).contains(terms[i].toLowerCase(Locale.US))) {
					currentScore++;
				}
			}
			if(currentScore > bestScore) {
				bestScore = currentScore;
				bestMatch = stop;
			}
		}
		if(bestScore != 0) {
			return stopToLatLngs.get(bestMatch);
		} else {
			return getGeocodedLatLng(query);
		}
	}
	
	private static LatLng getGeocodedLatLng(String query) {
		if(geocoder != null) {
			try {
				List<Address> results = geocoder.getFromLocationName(query + " Ithaca, NY", 1);
				if(results.size() > 0) {
					return new LatLng(results.get(0).getLatitude(), results.get(0).getLongitude());
				} else {
					return null;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			try {
				JSONObject response = MainModel.getJSONObjectForURL(getGeocoderUrlForLocationName(query));
				JSONArray results = response.getJSONArray("results");
				if(results.length() > 0) {
					JSONObject locationJSON = results.getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
					return new LatLng(locationJSON.getDouble("lat"), locationJSON.getDouble("lng"));
				} else {
					return null;
				}
			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			}
		}
	}
	
	private static String getGeocoderUrlForLocationName(String name) {
		return "http://maps.googleapis.com/maps/api/geocode/json?address="+ name +"&sensor=true";
	}
	
	private static ArrayList<String> findClosestStopsToLatLng(LatLng loc, int maxResults) {
		// step 1: compute the closest stops using a priority queue
		PriorityQueue<StopDistancePair> closestPairs = new PriorityQueue<StopDistancePair>();
		StopDistancePair currentPair;
		float[] distanceResultHolder = new float[3];
		for(String stop: stopToLatLngs.keySet()) {
			currentPair = new StopDistancePair(stop, distanceBetweenLatLngs(stopToLatLngs.get(stop), loc, distanceResultHolder));
			closestPairs.add(currentPair);
			if(closestPairs.size() > maxResults) {
				closestPairs.remove();
			}
		}
		
		// step 2: put those stops into an ArrayList and return them
		ArrayList<String> results = new ArrayList<String>();
		for(StopDistancePair pair: closestPairs) {
			results.add(pair.stop);
		}
		return results;
	}

	private static double distanceBetweenLatLngs(LatLng start, LatLng end, float[] results) {
		Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results);
		return (double) results[0];
	}
	
	private static class StopDistancePair implements Comparable<StopDistancePair> {
		public String stop;
		public double distance;
		public StopDistancePair(String s, double d) {
			stop = s;
			distance = d;
		}
		@Override
		public int compareTo(StopDistancePair another) {
			return ((Double) distance).compareTo(another.distance);
		}
	}
	
	
	/**
	 * This method queries the TCAT website and parses the response for route data
	 * @param start
	 * @param end
	 * @return
	 */
	private static ArrayList<MainListViewItem> getCardsForLatLngs(LatLng start, LatLng end) {
		// TODO
		ArrayList<String> closestStopsToStart = findClosestStopsToLatLng(start, NUMBER_OF_NEARBY_STOPS_TO_LOOK_AT);
		ArrayList<String> closestStopsToEnd = findClosestStopsToLatLng(end, NUMBER_OF_NEARBY_STOPS_TO_LOOK_AT);

		TimeZone easternTime = TimeZone.getTimeZone("GMT-4:00");
		Calendar now = Calendar.getInstance(easternTime);
		Calendar[] datesToQuery = new Calendar[NUMBER_OF_FUTURE_DATES_TO_QUERY];
		int gapBetweenDates = 10 * 60;
		for(int i = 0; i < datesToQuery.length; i++) {
			datesToQuery[i] = (Calendar) now.clone();
			now.roll(Calendar.SECOND, gapBetweenDates);
		}
		
		/**
		var numOfResultsToReturn = 2;
        if(options.hasOwnProperty('numberOfNearbyStopsToLookAt')) {
            numOfResultsToReturn = options.numberOfNearbyStopsToLookAt;
        }
        closest_stops = findClosestStops(start_lat, start_lng, dest_lat, dest_lng, numOfResultsToReturn);
        var results = [];
        var numResultsReturned = 0;
        // this looks at routes up to 30 minutes in advance
        var numberOfTimesToQuery = 4;
        if(options.hasOwnProperty('numberOfTimesToQuery')) {
            numberOfTimesToQuery = options.numberOfTimesToQuery;
        }
        
        var timesToQuery = [];
        var now = new Date();
        var TEN_MINUTES = 10 * 60 * 1000;
        for(var i = 0; i < numberOfTimesToQuery; i++) {
            timesToQuery.push(new Date(now.getTime() + ((now.getTimezoneOffset() * 60 * 1000) - (240 * 60 * 1000)) + i * TEN_MINUTES));
        }
        for(var i = 0; i < numOfResultsToReturn; i++) {
            for(var j = 0; j < numOfResultsToReturn; j++) {
                for(var k = 0; k < timesToQuery.length; k++) {
                    getNextBusForStops(closest_stops[0][i][0], closest_stops[1][j][0], timesToQuery[k], function(err, response){
                        if(!err) {
                            addRoutesIfRelevant(results, response);
                        }
                        numResultsReturned++;
                        if(numResultsReturned == (numOfResultsToReturn * numOfResultsToReturn * timesToQuery.length)) {
                            if(results.length == 0){
                                cb({'err': 'no routes found'});
                            } else {
                                console.log(uid, start_lat, start_lng, destination);
                                if(!options || !options.hasOwnProperty('ignoreSearchInDatabase') || !options.ignoreSearchInDatabase) {
                                    saveSearch(uid, start_lat, start_lng, destination);
                                }
                                cb(undefined, results);
                            }
                        }
                    });
                }
            }
        }
    }**/
		
		return null;
	}
	

	// public static ArrayList<MainListViewItem> getCardsForQuery(String start,
	// String destination) {
	// return null;
	// }
//
//	@Deprecated
//	public static ArrayList<MainListViewItem> getCardsForQuery(
//			String routeStart, String routeEnd) {
//		if (context == null) {
//			return null;
//		}
//		LocationManager locationManager = (LocationManager) context
//				.getSystemService(Context.LOCATION_SERVICE);
//		Criteria crit = new Criteria();
//		String provider = locationManager.getBestProvider(crit, true);
//		Location loc = locationManager.getLastKnownLocation(provider);
//
//		ArrayList<MainListViewItem> results = new ArrayList<MainListViewItem>();
//		JSONArray buses;
//		if (routeStart.contentEquals(MainModel.LOCATION_UNSPECIFIED)
//				&& routeEnd.contentEquals(MainModel.LOCATION_UNSPECIFIED)) {
//			// This should query the database for the user's default suggestions
//			buses = MainModel.getJSONArrayForURL("/routes/default/" + MainModel.getDeviceId() + "/"
//					+ loc.getLatitude() + "/" + loc.getLongitude() + "/"
//					+ routeEnd.replace(" ", "_"));
//		} else if (routeStart.contentEquals(MainModel.LOCATION_UNSPECIFIED)
//				|| routeStart.contentEquals(MainModel.CURRENT_LOCATION)) {
//			buses = MainModel.getJSONArrayForURL("/routes/fromcurrent/" + MainModel.getDeviceId() + "/"
//					+ loc.getLatitude() + "/" + loc.getLongitude() + "/"
//					+ routeEnd.replace(" ", "_"));
//		} else {
//			// This should query the data base for suggestions based on a
//			// specified start and destination
//			buses = MainModel.getJSONArrayForURL("/routes/fromcustom/" + MainModel.getDeviceId() + "/"
//					+ routeStart.replace(" ", "_") + "/"
//					+ routeEnd.replace(" ", "_"));
//		}
//
//		JSONObject currentRoute;
//		for (int i = 0; i < buses.length(); i++) {
//			try {
//				currentRoute = buses.getJSONObject(i);
//				if (currentRoute.has("err")) {
//					// Toast.makeText(c, "hello", Toast.LENGTH_SHORT).show(); //
//					// test this
//					return null;
//				}
//				results.add(new MainListViewItem(
//						currentRoute.getString("next_bus"),
//						currentRoute.getString("route_numbers"),
//						currentRoute.getString("start"),
//						currentRoute.getString("destination"),
//						Double.parseDouble(currentRoute.getString("start_lat")),
//						Double.parseDouble(currentRoute.getString("start_lng")),
//						Double.parseDouble(currentRoute.getString("dest_lat")),
//						Double.parseDouble(currentRoute.getString("dest_lng")),
//						currentRoute.getString("travel_time")));
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//		}
//		if(results.size() == 0){
//			return null;
//		}
//		return results;
//	}
}

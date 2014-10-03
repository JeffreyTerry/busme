package com.example.busme;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.TimeZone;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
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
	private static final int NUMBER_OF_NEARBY_STOPS_TO_LOOK_AT = 1; // 2
	private static final int NUMBER_OF_FUTURE_DATES_TO_QUERY = 4; // 4

	private BusDataCollector() {
	}

	public static void initialize(Context c) {
		if (context == null) {
			context = c;
			if (Geocoder.isPresent()) {
				geocoder = new Geocoder(context);
			} else {
				geocoder = null;
			}
			try {
				stopToLatLngs = JSONConverter
						.convertStopLatLngsToHashMap(new JSONObject(MainModel
								.getStopToLatLngDictionaryData()));
				stopToTcatIds = JSONConverter
						.convertStopIdsToHashMap(new JSONObject(MainModel
								.getStopToTcatIdDictionaryData()));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			Log.e("ERROR", "MainModel was initialized twice");
		}
	}

	/**
	 * This returns a set of cards for different user queries. It should never
	 * be called on the Main UI thread.
	 * 
	 * @param routeStart
	 *            A string query for the route's starting point. Can be an
	 *            address, a place in Ithaca, or a bus stop name. If the start
	 *            is equal to MainModel.LOCATION_UNSPECIFIED, the user's current
	 *            location will be used as a starting point.
	 * @param routeEnd
	 *            A string query for the route's starting point. Can be an
	 *            address, a place in Ithaca, or a bus stop name. If the end is
	 *            equal to MainModel.LOCATION_UNSPECIFIED and the start is also
	 *            equal to MainModel.LOCATION_UNSPECIFIED, the user's default
	 *            cards will be returned. If only the end is equal to
	 *            MainModel.LOCATION_UNSPECIFIED, the cards returned will be for
	 *            all buses coming to the start stop.
	 * @return
	 */
	public static ArrayList<MainListViewItem> getCardsForQuery(
			String routeStart, String routeEnd) {
		if (context == null) {
			return null;
		}
		Log.d("route", routeStart + ", " + routeEnd);
		if (routeStart.contentEquals(MainModel.LOCATION_UNSPECIFIED)) {
			if (routeEnd.contentEquals(MainModel.LOCATION_UNSPECIFIED)) {
				// this should grab the user's default cards from TCAT's
				// servers, not BusMe's servers TODO
				return getDefaultCardsFromBusMeServer();
			} else {
				// this should grab cards based on the current location
				LocationManager locationManager = (LocationManager) context
						.getSystemService(Context.LOCATION_SERVICE);
				Criteria crit = new Criteria();
				String provider = locationManager.getBestProvider(crit, true);
				Location currentLocation = locationManager
						.getLastKnownLocation(provider);

				LatLng endLatLng = getLatLngForSearchTerms(routeEnd);
				if (endLatLng == null) {
					return null;
				}

				return getCardsForLatLngsFromTCATServer(
						new LatLng(currentLocation.getLatitude(),
								currentLocation.getLongitude()), endLatLng);
			}
		} else if (routeEnd.contentEquals(MainModel.LOCATION_UNSPECIFIED)) {
			// this should grab cards showing buses coming out of a single
			// location TODO
			return null;
		} else {
			Log.d("yolo", "yolocrackers");
			// this should grab cards based on a start and an end location
			LatLng startLatLng = getLatLngForSearchTerms(routeStart);
			LatLng endLatLng = getLatLngForSearchTerms(routeEnd);

			if (startLatLng == null || endLatLng == null) {
				return null;
			}

			return getCardsForLatLngsFromTCATServer(startLatLng, endLatLng);
		}
	}

	private static LatLng getLatLngForSearchTerms(String query) {
		String bestMatch = null;
		int bestScore = 0;

		String[] terms = query.split(" ");
		int currentScore;
		for (String stop : stopToLatLngs.keySet()) {
			currentScore = 0;
			for (int i = 0; i < terms.length; i++) {
				if (stop.toLowerCase(Locale.US).contains(
						terms[i].toLowerCase(Locale.US))) {
					currentScore++;
				}
			}
			if (currentScore > bestScore) {
				bestScore = currentScore;
				bestMatch = stop;
			}
		}
		if (bestScore != 0) {
			return stopToLatLngs.get(bestMatch);
		} else {
			return getGeocodedLatLng(query);
		}
	}

	private static LatLng getGeocodedLatLng(String query) {
		if (geocoder != null) {
			try {
				List<Address> results = geocoder.getFromLocationName(query
						+ " Ithaca, NY", 1);
				if (results.size() > 0) {
					return new LatLng(results.get(0).getLatitude(), results
							.get(0).getLongitude());
				} else {
					return null;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			try {
				JSONObject response = MainModel
						.getJSONObjectForURL(getGeocoderUrlForLocationName(query));
				JSONArray results = response.getJSONArray("results");
				if (results.length() > 0) {
					JSONObject locationJSON = results.getJSONObject(0)
							.getJSONObject("geometry")
							.getJSONObject("location");
					return new LatLng(locationJSON.getDouble("lat"),
							locationJSON.getDouble("lng"));
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
		return "http://maps.googleapis.com/maps/api/geocode/json?address="
				+ name + "&sensor=true";
	}

	private static ArrayList<String> findClosestStopsToLatLng(LatLng loc,
			int maxResults) {
		// step 1: compute the closest stops using a priority queue
		PriorityQueue<StopDistancePair> closestPairs = new PriorityQueue<StopDistancePair>();
		StopDistancePair currentPair;
		float[] distanceResultHolder = new float[3];
		for (String stop : stopToLatLngs.keySet()) {
			currentPair = new StopDistancePair(stop, distanceBetweenLatLngs(
					stopToLatLngs.get(stop), loc, distanceResultHolder));
			closestPairs.add(currentPair);
			if (closestPairs.size() > maxResults) {
				closestPairs.remove();
			}
		}

		// step 2: put those stops into an ArrayList and return them
		ArrayList<String> results = new ArrayList<String>();
		for (StopDistancePair pair : closestPairs) {
			results.add(pair.stop);
		}
		return results;
	}

	private static double distanceBetweenLatLngs(LatLng start, LatLng end,
			float[] results) {
		Location.distanceBetween(start.latitude, start.longitude, end.latitude,
				end.longitude, results);
		return (double) results[0];
	}

	private static class StopDistancePair implements
			Comparable<StopDistancePair> {
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
	 * This method queries the TCAT website for multiple route options based on
	 * the start and end LatLngs
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	private static ArrayList<MainListViewItem> getCardsForLatLngsFromTCATServer(
			LatLng start, LatLng end) {
		// step 1: get the stops to query for
		ArrayList<String> closestStopsToStart = findClosestStopsToLatLng(start,
				NUMBER_OF_NEARBY_STOPS_TO_LOOK_AT);
		ArrayList<String> closestStopsToEnd = findClosestStopsToLatLng(end,
				NUMBER_OF_NEARBY_STOPS_TO_LOOK_AT);

		// step 2: get the dates to query for
		TimeZone easternTime = TimeZone.getTimeZone("GMT-4:00");
		Calendar now = Calendar.getInstance(easternTime);
		Calendar[] datesToQuery = new Calendar[NUMBER_OF_FUTURE_DATES_TO_QUERY];
		int gapBetweenDates = 10 * 60;
		for (int i = 0; i < datesToQuery.length; i++) {
			datesToQuery[i] = (Calendar) now.clone();
			now.roll(Calendar.SECOND, gapBetweenDates);
		}

		// step 3: make the queries
		MainListViewItem currentCard;
		HashSet<MainListViewItem> cardsToReturn = new HashSet<MainListViewItem>();
		for (int i = 0; i < closestStopsToStart.size(); i++) {
			for (int j = 0; j < closestStopsToEnd.size(); j++) {
				for (int k = 0; k < datesToQuery.length; k++) {
					currentCard = getCardForStopsFromTCATServer(
							closestStopsToStart.get(i),
							closestStopsToEnd.get(j), datesToQuery[k]);
					if (currentCard != null) {
						cardsToReturn.add(currentCard);
					}
				}
			}
		}

		// step 4: return the cards and save the search to the user history on
		// the server
		if (cardsToReturn.size() == 0) {
			return null;
		} else {
			now = Calendar.getInstance(easternTime);
			saveQueryToDatabase(start, end, now);
			return new ArrayList<MainListViewItem>(cardsToReturn);
		}
	}

	/**
	 * This method queries the TCAT website and parses the response for route
	 * data
	 * 
	 * @param start
	 * @param end
	 * @param date
	 * @return
	 */
	private static MainListViewItem getCardForStopsFromTCATServer(String start, String end, Calendar date) {
		HttpClient client = new DefaultHttpClient();
		String postURL = ("http://tcat.nextinsight.com/index.php");
		HttpPost post = new HttpPost(postURL);
		try {
		    List<NameValuePair> pairs = new ArrayList<NameValuePair>();
//            'start': stopToTcatIdDictionary[start],
//            'end': stopToTcatIdDictionary[dest],
//            'day': time.getDay(),
//            'departure': 0,
//            'starthours': time.getHours() % 12,
//            'startminutes': time.getMinutes(),
//            'startampm': (Math.floor(time.getHours() / 12) == 0? 0: 1),
//            'customer': 1,
//            'sort': 1,
//            'transfers': 0,
//            'addr': '',
//            'city': 'Ithaca',
//            'radius': .25
		    pairs.add(new BasicNameValuePair("wml", ""));
		    pairs.add(new BasicNameValuePair("addrO", ""));
		    pairs.add(new BasicNameValuePair("latO", ""));
		    pairs.add(new BasicNameValuePair("lonO", ""));
		    pairs.add(new BasicNameValuePair("addrD", ""));
		    pairs.add(new BasicNameValuePair("latD", ""));
		    pairs.add(new BasicNameValuePair("lonD", ""));
		    pairs.add(new BasicNameValuePair("origin", ""));
		    pairs.add(new BasicNameValuePair("destination", ""));
		    pairs.add(new BasicNameValuePair("search", "search"));
		    pairs.add(new BasicNameValuePair("fulltext", ""));
		    pairs.add(new BasicNameValuePair("radiusO", ""));
		    pairs.add(new BasicNameValuePair("radiusD", ""));
		    pairs.add(new BasicNameValuePair("addressid1", ""));
		    pairs.add(new BasicNameValuePair("addressid2", ""));
		    pairs.add(new BasicNameValuePair("start", start));
		    pairs.add(new BasicNameValuePair("end", end));
		    pairs.add(new BasicNameValuePair("day", "" + (date.get(Calendar.DAY_OF_WEEK) - 1)));  // 0 - 6
		    pairs.add(new BasicNameValuePair("departure", "0"));
		    pairs.add(new BasicNameValuePair("starthours", "" + date.get(Calendar.HOUR)));
		    pairs.add(new BasicNameValuePair("startminutes", "" + date.get(Calendar.MINUTE)));
		    pairs.add(new BasicNameValuePair("startampm", "" + date.get(Calendar.AM_PM)));
		    pairs.add(new BasicNameValuePair("customer", "1"));
		    pairs.add(new BasicNameValuePair("sort", "1"));
		    pairs.add(new BasicNameValuePair("transfers", "0"));
		    pairs.add(new BasicNameValuePair("addr", ""));
		    pairs.add(new BasicNameValuePair("city", "Ithaca"));
		    pairs.add(new BasicNameValuePair("radius", "0.25"));
		    UrlEncodedFormEntity uefe = new UrlEncodedFormEntity(pairs);
		    post.setEntity(uefe);
		    // Execute the HTTP Post Request
		    HttpResponse response = client.execute(post);
		    // Convert the response into a String
		    HttpEntity resEntity = response.getEntity();
		    
		    if (resEntity != null) {
		    	InputStream is = resEntity.getContent();
		    	Log.d("here", "blaze");
		    	  BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
		    	  StringBuilder str = new StringBuilder();

		    	  String line = null;
		    	  try {
		    	    while ((line = bufferedReader.readLine()) != null) {
		    	      str.append(line + "\n");
		    	    }
		    	  } catch (IOException e) {
		    	    throw new RuntimeException(e);
		    	  } finally {
		    	    try {
		    	      is.close();
		    	    } catch (IOException e) {
		    	      //tough luck...
		    	    }
		    	  }
			    	Log.d("here", "swag");
		    }
		} catch (UnsupportedEncodingException uee) {
		    uee.printStackTrace();
		} catch (ClientProtocolException cpe) {
		    cpe.printStackTrace();
		} catch (IOException ioe) {
		    ioe.printStackTrace();
		}
		return new MainListViewItem("01:30 PM", "69", "somewhere", "somewhere", -1, -1, -1, -1, "0");
	}

	@Deprecated
	private static ArrayList<MainListViewItem> getDefaultCardsFromBusMeServer() {
		if (context == null) {
			return null;
		}
		LocationManager locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		Criteria crit = new Criteria();
		String provider = locationManager.getBestProvider(crit, true);
		Location loc = locationManager.getLastKnownLocation(provider);

		ArrayList<MainListViewItem> results = new ArrayList<MainListViewItem>();
		// This should query the database for the user's default suggestions
		JSONArray buses = MainModel.getJSONArrayForURL(MainModel.BASE_URL
				+ "/routes/default/" + MainModel.getDeviceId() + "/"
				+ loc.getLatitude() + "/" + loc.getLongitude() + "/");

		if (buses == null) {
			return null;
		}
		JSONObject currentRoute;
		for (int i = 0; i < buses.length(); i++) {
			try {
				currentRoute = buses.getJSONObject(i);
				if (currentRoute.has("err")) {
					// Toast.makeText(c, "hello", Toast.LENGTH_SHORT).show(); //
					// test this
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
		if (results.size() == 0) {
			return null;
		}
		return results;
	}

	private static void saveQueryToDatabase(LatLng start, LatLng end,
			Calendar date) {
		// TODO
	}
}

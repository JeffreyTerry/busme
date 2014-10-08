package com.example.busme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.database.SQLException;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

/**
 * This class handles all interactions with the TCAT server.
 * 
 * @author jterry
 * 
 */
public class BusDataController {
	private int NUMBER_OF_NEARBY_STOPS_TO_LOOK_AT = 2; // 2
	// TODO there's currently a bug in the date shifting
	private static final int NUMBER_OF_FUTURE_DATES_TO_QUERY = 1; // 4
	private Context context;
	private MainModel mainModel;
	private MainDatabaseController mainDatabaseController;
	private Geocoder geocoder;
	private LatLng currentLatLng;
	private HashMap<String, LatLng> stopToLatLngs = null;
	private HashMap<String, String> stopToTcatIds = null;

	public BusDataController(Context c, MainModel m) {
		context = c;
		if (Geocoder.isPresent()) {
			geocoder = new Geocoder(context);
		} else {
			geocoder = null;
		}
		mainModel = m;
		mainDatabaseController = new MainDatabaseController(context);
	}

	private boolean initializeStopData() {
		if (stopToLatLngs != null && stopToTcatIds != null) {
			return true;
		}
		try {
			String stopToLatLngDictionaryData = mainModel
					.getStopToLatLngDictionaryData();
			if (stopToLatLngDictionaryData != null) {
				stopToLatLngs = JSONConverter
						.convertStopLatLngsToHashMap(new JSONObject(
								stopToLatLngDictionaryData));
			}

			String stopToTcatIdDictionaryData = mainModel
					.getStopToTcatIdDictionaryData();
			if (stopToTcatIdDictionaryData != null) {
				stopToTcatIds = JSONConverter
						.convertStopIdsToHashMap(new JSONObject(
								stopToTcatIdDictionaryData));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if (stopToLatLngs != null && stopToTcatIds != null) {
			return true;
		} else {
			return false;
		}
	}
	
	public ArrayList<MainListViewItem> getCardsForQuery(String routeStart,
			String routeEnd) {
		return getCardsForQuery(routeStart, routeEnd, false);
	}

	/**
	 * This returns a set of cards for different user queries. It should never
	 * be called on the Main UI thread.
	 * 
	 * @param routeStartx
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
	public ArrayList<MainListViewItem> getCardsForQuery(String routeStart,
			String routeEnd, boolean grabbingDefaultCards) {
		if (context == null) {
			return null;
		}
		if (!initializeStopData()) {
			ArrayList<MainListViewItem> results = new ArrayList<MainListViewItem>();
			results.add(MainListViewItem.STOP_DATA_MISSING_ERROR_ITEM);
			return results;
		}
		if (routeStart.contentEquals(MainModel.LOCATION_UNSPECIFIED)
				|| routeStart.contentEquals(MainModel.LOCATION_CURRENT)) {
			if (routeStart.contentEquals(MainModel.LOCATION_UNSPECIFIED)
					&& routeEnd.contentEquals(MainModel.LOCATION_UNSPECIFIED)) {
				// this should grab the user's default cards from TCAT's
				// servers
				NUMBER_OF_NEARBY_STOPS_TO_LOOK_AT--;
				ArrayList<MainListViewItem> result = getDefaultCardsFromTCATServer();
				NUMBER_OF_NEARBY_STOPS_TO_LOOK_AT++;
				return result;
			} else if (routeStart.contentEquals(MainModel.LOCATION_CURRENT)
					&& routeEnd.contentEquals(MainModel.LOCATION_UNSPECIFIED)) {
				// this should grab cards based on all buses coming out of the
				// user's current location
				LocationManager locationManager = (LocationManager) context
						.getSystemService(Context.LOCATION_SERVICE);
				Criteria crit = new Criteria();
				String provider = locationManager.getBestProvider(crit, true);
				Location currentLocation = locationManager
						.getLastKnownLocation(provider);

				currentLatLng = new LatLng(currentLocation.getLatitude(),
						currentLocation.getLongitude());

				return getCardsForStartLatLngFromTCATServer(new LatLng(
						currentLocation.getLatitude(),
						currentLocation.getLongitude()));
			} else {
				// this should grab cards based on the current location and a
				// specific destination
				LocationManager locationManager = (LocationManager) context
						.getSystemService(Context.LOCATION_SERVICE);
				Criteria crit = new Criteria();
				String provider = locationManager.getBestProvider(crit, true);
				Location currentLocation = locationManager
						.getLastKnownLocation(provider);

				currentLatLng = new LatLng(currentLocation.getLatitude(),
						currentLocation.getLongitude());

				LatLng endLatLng = getLatLngForSearchTerms(routeEnd);
				if (endLatLng == null) {
					return null;
				}

				ArrayList<MainListViewItem> result = getCardsForLatLngsFromTCATServer(
						new LatLng(currentLocation.getLatitude(),
								currentLocation.getLongitude()), endLatLng);

				if (result == null) {
					if(!grabbingDefaultCards) {
						((Activity) context).runOnUiThread(new Runnable() {
							public void run() {
								Toast.makeText(
										context,
										"Specific route not found.\nSearching similar routes...",
										Toast.LENGTH_LONG).show();
							}
						});
					}
					return getCardsForQuery(routeStart,
							MainModel.LOCATION_UNSPECIFIED, grabbingDefaultCards);
				} else {
					return result;
				}
			}
		} else if (routeEnd.contentEquals(MainModel.LOCATION_UNSPECIFIED)) {
			// this should grab cards based on all buses coming out of a
			// specified start location
			LatLng startLatLng = getLatLngForSearchTerms(routeStart);
			if (startLatLng == null) {
				return null;
			}
			return getCardsForStartLatLngFromTCATServer(startLatLng);
		} else {
			// this should grab cards based on a start and an end location
			LatLng startLatLng = getLatLngForSearchTerms(routeStart);
			LatLng endLatLng = getLatLngForSearchTerms(routeEnd);

			if (startLatLng == null || endLatLng == null) {
				return null;
			}

			ArrayList<MainListViewItem> result = getCardsForLatLngsFromTCATServer(
					startLatLng, endLatLng);
			if (result == null) {
				if(!grabbingDefaultCards) {
					((Activity) context).runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(
									context,
									"Specific route not found.\nSearching similar routes...",
									Toast.LENGTH_LONG).show();
						}
					});
				}
				return getCardsForQuery(routeStart,
						MainModel.LOCATION_UNSPECIFIED, grabbingDefaultCards);
			} else {
				return result;
			}
		}
	}

	private LatLng getLatLngForSearchTerms(String query) {
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

	private LatLng getGeocodedLatLng(String query) {
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

	private String getGeocoderUrlForLocationName(String name) {
		return "http://maps.googleapis.com/maps/api/geocode/json?address="
				+ name + "&sensor=true";
	}

	private ArrayList<String> findClosestStopsToLatLng(LatLng loc,
			int maxResults) {
		if (loc == null) {
			return new ArrayList<String>();
		}
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

	private double distanceBetweenLatLngs(LatLng start, LatLng end,
			float[] results) {
		Location.distanceBetween(start.latitude, start.longitude, end.latitude,
				end.longitude, results);
		return (double) results[0];
	}

	private class StopDistancePair implements Comparable<StopDistancePair> {
		public String stop;
		public double distance;

		public StopDistancePair(String s, double d) {
			stop = s;
			distance = d;
		}

		@Override
		public int compareTo(StopDistancePair another) {
			// kind of a hack to get the priority queue using this class to
			// remove the largest nodes rather than the smallest nodes
			return ((Double) another.distance).compareTo(distance);
		}
	}

	private ArrayList<MainListViewItem> getDefaultCardsFromTCATServer() {
		if (context == null) {
			return null;
		}

		ArrayList<MainListViewItem> results = new ArrayList<MainListViewItem>();
		ArrayList<String[]> relevantSearches = new ArrayList<String[]>();
		ArrayList<String[]> relevantSearchStopIds;
		try {
			mainDatabaseController.open();
			relevantSearchStopIds = mainDatabaseController
					.getRelevantSearchStopIds();
			mainDatabaseController.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		} finally {
			mainDatabaseController.close();
		}
		if (relevantSearchStopIds == null) {
			return null;
		} else {
			String currentStartId, currentEndId;
			String[] nextRelevantSearch;
			for (int i = 0; i < relevantSearchStopIds.size(); i++) {
				currentStartId = relevantSearchStopIds.get(i)[0];
				currentEndId = relevantSearchStopIds.get(i)[1];
				nextRelevantSearch = new String[2];
				if (currentStartId
						.contentEquals(MainModel.LOCATION_UNSPECIFIED)) {
					nextRelevantSearch[0] = currentStartId;
				}
				if (currentEndId.contentEquals(MainModel.LOCATION_UNSPECIFIED)) {
					nextRelevantSearch[1] = currentEndId;
				}
				for (String stopName : stopToTcatIds.keySet()) {
					if (stopToTcatIds.get(stopName).contentEquals(
							currentStartId)) {
						nextRelevantSearch[0] = stopName;
					} else if (stopToTcatIds.get(stopName).contentEquals(
							currentEndId)) {
						nextRelevantSearch[1] = stopName;
					}
				}
				if (nextRelevantSearch[0] != null
						&& nextRelevantSearch[1] != null) {
					relevantSearches.add(nextRelevantSearch);
				}
			}
		}

		// TODO remove these results based on relevance (date, location, etc...)
		relevantSearches = trimArrayListToSize(relevantSearches, 5);

		int numberOfResultsToKeepForEachQuery = 1;
		String[] nextSearch;
		ArrayList<MainListViewItem> nextResults = new ArrayList<MainListViewItem>();
		for (int i = 0; i < relevantSearches.size(); i++) {
			nextSearch = relevantSearches.get(i);
			if (nextSearch[0].contentEquals(MainDatabaseController.NULL_QUERY)) {
				nextResults = getCardsForQuery(MainModel.LOCATION_UNSPECIFIED,
						nextSearch[1], true);
				if (nextResults != null) {
					Collections.sort(nextResults,
							MainListViewItem.DEFAULT_COMPARATOR);
					nextResults = trimArrayListToSize(nextResults,
							numberOfResultsToKeepForEachQuery);
					for (int j = 0; j < nextResults.size(); j++) {
						if (!results.contains(nextResults.get(j))) {
							results.add(nextResults.get(j));
						}
					}
				}
			} else if (nextSearch[1]
					.contentEquals(MainDatabaseController.NULL_QUERY)) {
				nextResults = getCardsForQuery(nextSearch[0],
						MainModel.LOCATION_UNSPECIFIED, true);
				if (nextResults != null) {
					Collections.sort(nextResults,
							MainListViewItem.DEFAULT_COMPARATOR);
					nextResults = trimArrayListToSize(nextResults,
							numberOfResultsToKeepForEachQuery);
					for (int j = 0; j < nextResults.size(); j++) {
						if (!results.contains(nextResults.get(j))) {
							results.add(nextResults.get(j));
						}
					}
				}
			} else if (!nextSearch[0]
					.contentEquals(MainDatabaseController.NULL_QUERY)
					&& !nextSearch[1]
							.contentEquals(MainDatabaseController.NULL_QUERY)) {
				nextResults = getCardsForQuery(nextSearch[0], nextSearch[1], true);
				if (nextResults != null) {
					Collections.sort(nextResults,
							MainListViewItem.DEFAULT_COMPARATOR);
					nextResults = trimArrayListToSize(nextResults,
							numberOfResultsToKeepForEachQuery);
					for (int j = 0; j < nextResults.size(); j++) {
						if (!results.contains(nextResults.get(j))) {
							results.add(nextResults.get(j));
						}
					}
				}
			}
		}

		if (results.size() == 0) {
			return null;
		}
		return results;
	}

	/**
	 * This method queries the TCAT website for multiple route options based on
	 * the start and end LatLngs
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	private ArrayList<MainListViewItem> getCardsForLatLngsFromTCATServer(
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
			if (start == currentLatLng) {
				saveEndQueryToDatabase(closestStopsToEnd.get(0));
			} else {
				saveStartEndQueryToDatabase(closestStopsToStart.get(0),
						closestStopsToEnd.get(0));
			}
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
	private MainListViewItem getCardForStopsFromTCATServer(String start,
			String end, Calendar date) {
		HttpClient client = new DefaultHttpClient();
		String postURL = ("http://tcat.nextinsight.com/index.php");
		HttpPost post = new HttpPost(postURL);
		try {
			List<NameValuePair> pairs = new ArrayList<NameValuePair>();
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
			pairs.add(new BasicNameValuePair("start", stopToTcatIds.get(start)));
			pairs.add(new BasicNameValuePair("end", stopToTcatIds.get(end)));
			pairs.add(new BasicNameValuePair("day", ""
					+ (date.get(Calendar.DAY_OF_WEEK) - 1))); // 0 - 6
			pairs.add(new BasicNameValuePair("departure", "0"));
			pairs.add(new BasicNameValuePair("starthours", ""
					+ date.get(Calendar.HOUR)));
			pairs.add(new BasicNameValuePair("startminutes", ""
					+ date.get(Calendar.MINUTE)));
			pairs.add(new BasicNameValuePair("startampm", ""
					+ date.get(Calendar.AM_PM)));
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
				BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(is));
				StringBuilder str = new StringBuilder(
						(int) (resEntity.getContentLength() / 8));
				String line = null;
				try {
					while ((line = bufferedReader.readLine()) != null) {
						str.append(line + "\n");
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				String built = str.toString();
				Pattern resultSectionPattern = Pattern
						.compile("(leftColSub)[\\s\\S]*(rightColSub)");
				Matcher resultSectionMatcher = resultSectionPattern
						.matcher(built);
				if (!resultSectionMatcher.find()) {
					return null;
				} else {
					try {
						String responseBody = resultSectionMatcher.group(0);
						responseBody = responseBody.replaceAll(
								"<sup>(\\w)*<\\/sup>", "");
						responseBody = responseBody.replaceAll(":<\\/strong>",
								"</strong>");
						responseBody = responseBody.replaceAll(":<\\/b>",
								"</b>");

						Pattern busStartPattern = Pattern
								.compile("<[^<]*<[^<]*Board the[^<]*<[^<]*<[^<]*<a\\shref=\"\\/stops\\/(\\w)*\">[^<]*<\\/a>(<br>[\\w\\s,]*)?");
						Matcher busStartMatcher = busStartPattern
								.matcher(responseBody);
						busStartMatcher.find();
						String nextBusStartBody = busStartMatcher.group(0);

						Pattern busEndPattern = Pattern
								.compile("<[^<]*<[^<]*Get off at[^<]*<a\\shref=\"\\/stops\\/(\\w)*\">[^<]*<\\/a>");
						Matcher busEndMatcher = busEndPattern
								.matcher(responseBody);
						busEndMatcher.find();
						String nextBusEndBody = busEndMatcher.group(0);

						Pattern travelTimePattern = Pattern
								.compile("[Ee]stimated\\s*[Tt]rip\\s*[Tt]ime:[\\s\\w]*");
						Matcher travelTimeMatcher = travelTimePattern
								.matcher(responseBody);
						travelTimeMatcher.find();
						String travelTimeBody = travelTimeMatcher.group(0);

						String nextBusStartName = getNextBusStartName(nextBusStartBody);
						String nextBusEndName = getNextBusEndName(nextBusEndBody);
						String nextBusStartTime = getNextBusStartTime(nextBusStartBody);
						String nextBusRouteNumbers = getNextBusRouteNumbers(nextBusStartBody);
						String nextBusTravelTime = getNextBusTravelTime(travelTimeBody);
						LatLng nextBusStartLatLng = stopToLatLngs
								.get(nextBusStartName);
						LatLng nextBusEndLatLng = stopToLatLngs
								.get(nextBusEndName);
						return new MainListViewItem(nextBusStartTime,
								nextBusRouteNumbers, nextBusStartName,
								nextBusEndName, nextBusStartLatLng.latitude,
								nextBusStartLatLng.longitude,
								nextBusEndLatLng.latitude,
								nextBusEndLatLng.longitude, nextBusTravelTime,
								stopToTcatIds.get(start),
								stopToTcatIds.get(end));
					} catch (Exception e) {
						e.printStackTrace();
						return null;
					}
				}
			}
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		} catch (ClientProtocolException cpe) {
			cpe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
	}

	private ArrayList<MainListViewItem> getCardsForStartLatLngFromTCATServer(
			LatLng start) {
		// step 1: get the stops to query for
		ArrayList<String> closestStopsToStart = findClosestStopsToLatLng(start,
				NUMBER_OF_NEARBY_STOPS_TO_LOOK_AT);

		// step 2: make the queries
		ArrayList<MainListViewItem> currentCards;
		HashSet<MainListViewItem> cardsToReturn = new HashSet<MainListViewItem>();
		for (int i = 0; i < closestStopsToStart.size(); i++) {
			currentCards = getCardsForStopFromTCATServer(closestStopsToStart
					.get(i));
			if (currentCards != null) {
				cardsToReturn.addAll(currentCards);
			}
		}

		// step 3: return the cards and save the search to the user history on
		// the server
		if (cardsToReturn.size() == 0) {
			return null;
		} else {
			saveStartQueryToDatabase(closestStopsToStart.get(0));
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
	private ArrayList<MainListViewItem> getCardsForStopFromTCATServer(
			String start) {
		HttpClient client = new DefaultHttpClient();
		if (!stopToTcatIds.containsKey(start)) {
			Log.i("Data error", start
					+ " not found in stop to tcat id dictionary");
			return null;
		}
		HttpGet get = new HttpGet("http://tcat.nextinsight.com/stops/"
				+ stopToTcatIds.get(start));
		try {
			// Execute the HTTP Post Request
			HttpResponse response = client.execute(get);
			// Convert the response into a String
			HttpEntity resEntity = response.getEntity();

			if (resEntity != null) {
				InputStream is = resEntity.getContent();
				BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(is));
				StringBuilder str = new StringBuilder(
						(int) (resEntity.getContentLength() / 8));
				String line = null;
				try {
					while ((line = bufferedReader.readLine()) != null) {
						str.append(line + "\n");
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				String built = str.toString();

				Pattern resultSectionPattern = Pattern
						.compile("(leftColSub)[\\s\\S]*(<div\\s*id=\"rightColSub\">\\s*<h1>\\s*More\\s*info...\\s*<\\/h1>)");
				Matcher resultSectionMatcher = resultSectionPattern
						.matcher(built);

				Pattern noResultPattern = Pattern
						.compile("There are no more arrivals at this stop today");
				Matcher noResultMatcher = noResultPattern.matcher(built);

				if (noResultMatcher.find() || !resultSectionMatcher.find()) {
					return null;
				} else {
					try {
						String responseBody = resultSectionMatcher.group(0);
						responseBody = responseBody.replaceAll(
								"<sup>(\\w)*<\\/sup>", "");
						responseBody = responseBody.replaceAll(":<\\/strong>",
								"</strong>");
						responseBody = responseBody.replaceAll(":<\\/b>",
								"</b>");

						String nextBusRouteStartTimes = getNextBusRouteStartTimes(responseBody);
						String nextBusRouteNumbers = getNextBusRouteNumbers(responseBody);
						String nextBusRouteDirections = getNextBusRouteDirections(responseBody);

						LatLng nextBusStartLatLng = new LatLng(0, 0);
						if (stopToLatLngs.containsKey(start)) {
							nextBusStartLatLng = stopToLatLngs.get(start);
						}

						String[] nextStartTimes = nextBusRouteStartTimes
								.split(",");
						String[] nextNumbers = nextBusRouteNumbers.split(",");
						String[] nextDirections = nextBusRouteDirections
								.split(",");
						ArrayList<MainListViewItem> results = new ArrayList<MainListViewItem>();
						for (int i = 0; i < nextStartTimes.length; i++) {
							results.add(new MainListViewItem(nextStartTimes[i],
									nextNumbers[i], start, nextDirections[i],
									nextBusStartLatLng.latitude,
									nextBusStartLatLng.longitude,
									nextBusStartLatLng.latitude,
									nextBusStartLatLng.longitude,
									MainListViewItem.TRAVEL_TIME_UNKNOWN,
									stopToTcatIds.get(start),
									MainDatabaseController.NULL_QUERY));
						}
						return results;
					} catch (Exception e) {
						e.printStackTrace();
						return null;
					}
				}
			}
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
		} catch (ClientProtocolException cpe) {
			cpe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		ArrayList<MainListViewItem> results = new ArrayList<MainListViewItem>();
		results.add(MainListViewItem.NO_ROUTE_FOUND_ERROR_ITEM);
		return results;
	}

	private String getNextBusStartName(String nextBusStartBody) {
		Pattern nextBusStartNamePattern = Pattern
				.compile("<a\\shref=\"\\/stops\\/(\\w)*\">[^<]*<\\/a>");
		Matcher nextBusStartNameMatcher = nextBusStartNamePattern
				.matcher(nextBusStartBody);
		nextBusStartNameMatcher.find();
		String nextBusStartNameUnstripped = nextBusStartNameMatcher.group(0);

		Pattern nextBusStartNameStripperPattern = Pattern
				.compile(">[(\\S)(\\s)]*<");
		Matcher nextBusStartNameStripperMatcher = nextBusStartNameStripperPattern
				.matcher(nextBusStartNameUnstripped);
		nextBusStartNameStripperMatcher.find();
		nextBusStartNameUnstripped = nextBusStartNameStripperMatcher.group(0);
		return nextBusStartNameUnstripped.substring(1,
				nextBusStartNameUnstripped.length() - 1);
	}

	private String getNextBusEndName(String nextBusEndBody) {
		Pattern nextBusEndNamePattern = Pattern
				.compile("<a\\shref=\"\\/stops\\/(\\w)*\">[^<]*<\\/a>");
		Matcher nextBusEndNameMatcher = nextBusEndNamePattern
				.matcher(nextBusEndBody);
		nextBusEndNameMatcher.find();
		String nextBusEndNameUnstripped = nextBusEndNameMatcher.group(0);

		Pattern nextBusEndNameStripperPattern = Pattern
				.compile(">[(\\S)(\\s)]*<");
		Matcher nextBusEndNameStripperMatcher = nextBusEndNameStripperPattern
				.matcher(nextBusEndNameUnstripped);
		nextBusEndNameStripperMatcher.find();
		nextBusEndNameUnstripped = nextBusEndNameStripperMatcher.group(0);
		return nextBusEndNameUnstripped.substring(1,
				nextBusEndNameUnstripped.length() - 1);
	}

	private String getNextBusStartTime(String nextBusStartBody) {
		Pattern getNextBusStartTimePattern = Pattern
				.compile("(\\d)*:(\\d)*((\\s)*)+((\\bAM\\b)|(\\bPM\\b))");
		Matcher getNextBusStartTimeMatcher = getNextBusStartTimePattern
				.matcher(nextBusStartBody);
		getNextBusStartTimeMatcher.find();
		String startTime = getNextBusStartTimeMatcher.group(0);
		if (startTime.indexOf(':') <= 1) {
			startTime = "0" + startTime;
		}
		return startTime;
	}

	private String getNextBusRouteNumbers(String nextBusStartBody) {
		Pattern getNextBusRouteNumbersPattern = Pattern
				.compile("(Route)(\\s)*(\\d)(\\d)*");
		Matcher getNextBusRouteNumbersMatcher = getNextBusRouteNumbersPattern
				.matcher(nextBusStartBody);
		ArrayList<String> numbersUnstripped = new ArrayList<String>();
		while (getNextBusRouteNumbersMatcher.find()) {
			numbersUnstripped.add(getNextBusRouteNumbersMatcher.group(0));
		}

		Pattern nextBusRouteNumbersStripperPattern = Pattern.compile("(\\d)*$");
		Matcher nextBusRouteNumbersStripperMatcher;
		String result = "";
		for (int i = 0; i < numbersUnstripped.size(); i++) {
			nextBusRouteNumbersStripperMatcher = nextBusRouteNumbersStripperPattern
					.matcher(numbersUnstripped.get(i));
			nextBusRouteNumbersStripperMatcher.find();
			result += nextBusRouteNumbersStripperMatcher.group(0) + ",";
		}

		return result.substring(0, result.length() - 1);
	}

	private String getNextBusTravelTime(String travelTime) {
		Pattern hoursPattern = Pattern.compile("\\d*\\s*hour");
		Matcher nextBusTravelStartHoursMatcher = hoursPattern
				.matcher(travelTime);
		int hours = 0;
		if (nextBusTravelStartHoursMatcher.find()) {
			String nextBusTravelStartHours = nextBusTravelStartHoursMatcher
					.group(0);

			Pattern hoursStripperPattern = Pattern.compile("\\d*");
			Matcher hoursStripperMatcher = hoursStripperPattern
					.matcher(nextBusTravelStartHours);
			hoursStripperMatcher.find();
			nextBusTravelStartHours = hoursStripperMatcher.group(0);
			hours = Integer.parseInt(nextBusTravelStartHours);
		}

		Pattern minutesPattern = Pattern.compile("\\d*\\s*minutes");
		Matcher nextBusTravelStartMinutesMatcher = minutesPattern
				.matcher(travelTime);
		nextBusTravelStartMinutesMatcher.find();
		String nextBusTravelStartMinutesUnstripped = nextBusTravelStartMinutesMatcher
				.group(0);
		Pattern minutesStripperPattern = Pattern.compile("\\d*");
		Matcher nextBusTravelStartMinutesStripperMatcher = minutesStripperPattern
				.matcher(nextBusTravelStartMinutesUnstripped);
		nextBusTravelStartMinutesStripperMatcher.find();
		String nextBusTravelStartMinutes = nextBusTravelStartMinutesStripperMatcher
				.group(0);

		int minutes = Integer.parseInt(nextBusTravelStartMinutes);
		return "" + (minutes + hours * 60);
	}

	private String getNextBusRouteStartTimes(String nextBusStartBody) {
		Pattern getNextBusRouteStartTimesPattern = Pattern
				.compile("(\\d)+:\\d\\d\\s*[(PM)|(AM)]*");
		Matcher getNextBusRouteStartTimesMatcher = getNextBusRouteStartTimesPattern
				.matcher(nextBusStartBody);
		ArrayList<String> startTimesUnpadded = new ArrayList<String>();
		while (getNextBusRouteStartTimesMatcher.find()) {
			startTimesUnpadded.add(getNextBusRouteStartTimesMatcher.group(0));
		}

		String result = "";
		String toAdd = "";
		for (int i = 0; i < startTimesUnpadded.size(); i++) {
			if (startTimesUnpadded.get(i).indexOf(":") == 1) {
				toAdd = "0" + startTimesUnpadded.get(i);
				if (toAdd.indexOf("AM") == 5 || toAdd.indexOf("PM") == 5) {
					toAdd = toAdd.substring(0, 5) + " " + toAdd.substring(5);
				}
			} else {
				toAdd = startTimesUnpadded.get(i);
				if (toAdd.indexOf("AM") == 5 || toAdd.indexOf("PM") == 5) {
					toAdd = toAdd.substring(0, 5) + " " + toAdd.substring(5);
				}
			}
			result += toAdd + ",";
		}

		return result.substring(0, result.length() - 1);
	}

	private String getNextBusRouteDirections(String nextBusStartBody) {
		Pattern getNextBusRouteDirectionsPattern = Pattern
				.compile("(Route)(\\s)*(\\d)(\\d)*[^<]*");
		Matcher getNextBusRouteDirectionsMatcher = getNextBusRouteDirectionsPattern
				.matcher(nextBusStartBody);
		ArrayList<String> directionsUnstripped = new ArrayList<String>();
		while (getNextBusRouteDirectionsMatcher.find()) {
			directionsUnstripped.add(getNextBusRouteDirectionsMatcher.group(0));
		}

		String[] blacklist = { "express", "sunday", "monday", "tuesday",
				"wednesday", "thursday", "friday", "saturday", "weekdays",
				"weekends", "am", "pm", "-" };
		ArrayList<Integer> indicesToRemove = new ArrayList<Integer>();
		ArrayList<Integer> numbersOfCharactersToRemove = new ArrayList<Integer>();
		String directionLowercase, nextDirectionsUnstripped;
		int currentIndex;
		for (int j = 0; j < directionsUnstripped.size(); j++) {
			indicesToRemove.clear();
			numbersOfCharactersToRemove.clear();
			nextDirectionsUnstripped = directionsUnstripped.get(j);
			if (nextDirectionsUnstripped.indexOf("-") > 0
					&& directionsUnstripped.get(j).charAt(
							nextDirectionsUnstripped.indexOf("-") - 1) != ' ') {
				nextDirectionsUnstripped = nextDirectionsUnstripped.substring(
						0, nextDirectionsUnstripped.indexOf("-"))
						+ " - "
						+ nextDirectionsUnstripped
								.substring(nextDirectionsUnstripped
										.indexOf("-") + 1);
			}
			directionLowercase = nextDirectionsUnstripped
					.toLowerCase(Locale.US);
			for (int i = 0; i < blacklist.length; i++) {
				currentIndex = directionLowercase.indexOf((blacklist[i]));
				if (currentIndex != -1
						&& (!blacklist[i].contentEquals("-") || indicesToRemove
								.size() >= 2)) {
					indicesToRemove.add(currentIndex);
					numbersOfCharactersToRemove.add(blacklist[i].length() + 1);
				}
			}
			for (int i = 0; i < indicesToRemove.size(); i++) {
				for (int k = i + 1; k < indicesToRemove.size(); k++) {
					if (indicesToRemove.get(k) > indicesToRemove.get(i)) {
						indicesToRemove.set(k, indicesToRemove.get(k)
								- numbersOfCharactersToRemove.get(i));
					}
				}
				// System.out.println(nextDirectionsUnstripped + ", "
				// + indicesToRemove.get(i) + ", "
				// + numbersOfCharactersToRemove.get(i));
				nextDirectionsUnstripped = new String(
						nextDirectionsUnstripped.substring(0,
								indicesToRemove.get(i))
								+ nextDirectionsUnstripped
										.substring(indicesToRemove.get(i)
												+ numbersOfCharactersToRemove
														.get(i)));
			}
			directionsUnstripped.set(j, nextDirectionsUnstripped);
		}
		int indexToRemove, numberOfCharactersToRemove;
		String[] blacklist2 = { "bound" };
		for (int j = 0; j < directionsUnstripped.size(); j++) {
			indexToRemove = -1;
			numberOfCharactersToRemove = 0;
			directionLowercase = directionsUnstripped.get(j).toLowerCase(
					Locale.US);
			for (int i = 0; i < blacklist2.length; i++) {
				indexToRemove = directionLowercase.indexOf((blacklist2[i]));

				// this makes sure we don't remove standalone words
				if (indexToRemove != 0 && indexToRemove != -1) {
					if (directionLowercase.charAt(indexToRemove - 1) == ' ') {
						indexToRemove = -1;
					}
				}

				if (indexToRemove != -1) {
					numberOfCharactersToRemove = blacklist2[i].length();
					break;
				}
			}
			if (indexToRemove != -1) {
				directionsUnstripped.set(
						j,
						directionsUnstripped.get(j).substring(0, indexToRemove)
								+ directionsUnstripped.get(j).substring(
										indexToRemove
												+ numberOfCharactersToRemove));
			}

		}

		String result = "";
		for (int i = 0; i < directionsUnstripped.size(); i++) {
			if (directionsUnstripped.get(i).length() > 9) {
				result += directionsUnstripped.get(i).substring(9) + ",";
			}
		}

		return result.substring(0, result.length() - 1);
	}

	private void saveStartQueryToDatabase(String start) {
		try {
			System.out.println("H:i " + start + ", " + stopToTcatIds.get(start) + ", " + stopToTcatIds.containsValue(start));
			mainDatabaseController.open();
			if (stopToTcatIds.containsKey(start)) {
				mainDatabaseController.addStartSearch(stopToTcatIds.get(start));
			}
			mainDatabaseController.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			mainDatabaseController.close();
		}
	}

	private void saveEndQueryToDatabase(String end) {
		try {
			mainDatabaseController.open();
			if (stopToTcatIds.containsKey(end)) {
				mainDatabaseController.addEndSearch(stopToTcatIds.get(end));
			}
			mainDatabaseController.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			mainDatabaseController.close();
		}
	}

	private void saveStartEndQueryToDatabase(String start, String end) {
		try {
			mainDatabaseController.open();
			if (stopToTcatIds.containsKey(start)
					&& stopToTcatIds.containsKey(end)) {
				mainDatabaseController.addStartEndSearch(
						stopToTcatIds.get(start), stopToTcatIds.get(end));
			}
			mainDatabaseController.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			mainDatabaseController.close();
		}
	}

	public void removeStartQueryFromDatabase(String startid) {
		try {
			mainDatabaseController.open();
			mainDatabaseController.deleteStartSearch(startid);
			mainDatabaseController.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mainDatabaseController.close();
		}
	}

	public void removeEndQueryFromDatabase(String endid) {
		try {
			mainDatabaseController.open();
			mainDatabaseController.deleteEndSearch(endid);
			mainDatabaseController.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mainDatabaseController.close();
		}
	}

	public void removeStartEndQueryFromDatabase(String startid, String endid) {
		try {
			mainDatabaseController.open();
			mainDatabaseController.deleteStartEndSearch(startid, endid);
			mainDatabaseController.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			mainDatabaseController.close();
		}
	}

	private <E> ArrayList<E> trimArrayListToSize(ArrayList<E> list, int size) {
		if (size < 0) {
			throw new IllegalArgumentException("size must be nonnegative");
		}
		while (list.size() > size) {
			list.remove(list.size() - 1);
		}
		return list;
	}
}

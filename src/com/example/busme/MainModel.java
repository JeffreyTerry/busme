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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.Log;

public class MainModel {
	public static final String NEW_CARDS_BROADCAST = "com.example.busme.newcards";
	public static final String ERROR_EXTRA = "error";
	public static final String CARD_ERROR_NO_ROUTES = "No routes found";
	public static final String LOCATION_UNSPECIFIED = "";
	public static final String BASE_URL = "http://www.theseedok.com/api";
	public static final String ROUTE_LINE_DATA_FILE_BASE_NAME = "route_lines_";

	private static final String STOP_TO_LOCATION_DATA_FILE = "stop_locations";
	private static final String STOP_TO_ID_DATA_FILE = "stop_ids";
	private static final String NULL_DEVICE_ID = "9876";
	private static final String NULL_DATA_VERSION = "-1";
	private static String deviceId = NULL_DEVICE_ID;
	private static String dataVersion = NULL_DATA_VERSION; // used to keep the
															// app's data in
															// sync with the
															// server's data
	private static Context context = null;
	private static SharedPreferences sharedPreferences;
	private static MainController mainController;

	private MainModel() {
	}

	/**
	 * This must be called before the model is used
	 * 
	 * @param c
	 */
	public static void initialize(Context c, MainController mc) {
		if (context == null) {
			context = c;
			mainController = mc;
			BusDataCollector.initialize(c);
		} else {
			Log.e("ERROR", "MainModel was instantiated twice");
		}

		// load shared preferences
		sharedPreferences = context.getSharedPreferences("com.example.busme",
				Context.MODE_PRIVATE);

		// check to make sure our device id and stop data is valid
		new Thread(new DeviceIdChecker()).start();
		new Thread(new DataVersionChecker()).start();
	}

	/**
	 * Gets a JSON object from the server at "BASE_URL + apiURL"
	 * 
	 * @param apiURL
	 * @return
	 */
	public static JSONObject getJSONObjectForURL(String url) {
		if (context == null) {
			return null;
		}
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
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
	public static JSONArray getJSONArrayForURL(String url) {
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
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

	/**
	 * Save data to a private file
	 * 
	 * @param data
	 * @param filename
	 */
	public static void saveToFile(String data, String filename) {
		if (context == null) {
			return;
		}
		try {
			FileOutputStream fos = context.openFileOutput(filename,
					Context.MODE_PRIVATE);
			fos.write(data.getBytes());
			fos.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	public static String readFromFile(String filename) {
		if (context == null) {
			return null;
		}
		try {
			BufferedReader bis = new BufferedReader(new InputStreamReader(
					context.openFileInput(filename)));
			String next;
			String result = "";
			while ((next = bis.readLine()) != null) {
				result += next;
			}
			if (result.contentEquals("")) {
				return null;
			} else {
				return result;
			}
		} catch (IOException e1) {
			return null;
		}
	}

	public static String getRouteCoordinateData(int routeNumber) {
		String data = readFromFile(MainModel.ROUTE_LINE_DATA_FILE_BASE_NAME
				+ routeNumber);
		if (data == null) {
			return null;
		} else {
			return data;
		}
	}

	public static void saveRouteData(String data, int routeNumber) {
		MainModel.saveToFile(data, MainModel.ROUTE_LINE_DATA_FILE_BASE_NAME
				+ routeNumber);
	}

	public static String getStopToLatLngDictionaryData() {
		String data = readFromFile(STOP_TO_LOCATION_DATA_FILE);
		if (data.contentEquals("")) {
			return null;
		} else {
			return data;
		}
	}

	public static void saveStopToLatLngDictionaryData(String data) {
		MainModel.saveToFile(data, STOP_TO_LOCATION_DATA_FILE);
	}

	public static String getStopToTcatIdDictionaryData() {
		String data = readFromFile(STOP_TO_ID_DATA_FILE);
		if (data.contentEquals("")) {
			return null;
		} else {
			return data;
		}
	}

	public static void saveStopToTcatIdDictionaryData(String data) {
		MainModel.saveToFile(data, STOP_TO_ID_DATA_FILE);
	}

	public static String getDeviceId() {
		return deviceId;
	}

	// ////// CARD STUFF ///////
	private static void sendCardsToController(ArrayList<MainListViewItem> cards) {
		if (cards != null) {
			sendCardsToController(cards, null);
		} else {
			sendCardsToController(cards, CARD_ERROR_NO_ROUTES);
		}
	}

	/**
	 * This method sends a JSONArray of cards to the controller
	 * 
	 * @param cardsJSON
	 */
	private static void sendCardsToController(
			ArrayList<MainListViewItem> cards, String errorCode) {
		if (mainController == null) {
			return;
		}

		if (errorCode == null) {
			mainController.setCards(cards);
		} else {
			mainController.makeError(errorCode);
		}
	}

	/**
	 * This generates a new set of cards and then sends them to the controller
	 */
	public static void generateDefaultCards() {
		((Activity) context).runOnUiThread(new Runnable() {
			public void run() {
				new CardGenerator().execute(LOCATION_UNSPECIFIED,
						LOCATION_UNSPECIFIED);
			}
		});
	}

	public static void generateCardsForQuery(String start, String destination) {
		final String finalStart = start;
		final String finalDestination = destination;
		((Activity) context).runOnUiThread(new Runnable() {
			public void run() {
				new CardGenerator().execute(finalStart, finalDestination);
			}
		});
	}

	private static class CardGenerator extends
			AsyncTask<String, Void, ArrayList<MainListViewItem>> {
		@Override
		protected void onPreExecute() {
			mainController.setCardsLoading(true);
			super.onPreExecute();
		}

		@Override
		protected ArrayList<MainListViewItem> doInBackground(
				String... endpoints) {
			if (endpoints.length == 2) {
				return BusDataCollector.getCardsForQuery(endpoints[0],
						endpoints[1]);
			} else {
				return null;
			}
		}

		@Override
		protected void onPostExecute(ArrayList<MainListViewItem> cards) {
			mainController.setCardsLoading(false);
			sendCardsToController(cards);
			super.onPostExecute(cards);
		}
	}

	// ///////////// STARTUP CHECKS ////////////////
	private static class DeviceIdChecker implements Runnable {
		private String getNewDeviceId() {
			try {
				return getJSONObjectForURL(BASE_URL + "/getdeviceid")
						.getString("id");
			} catch (JSONException e) {
				e.printStackTrace();
				return NULL_DEVICE_ID;
			}
		}

		private boolean idIsStillValid(String id) {
			JSONObject result = getJSONObjectForURL(BASE_URL
					+ "/checkdeviceid/" + id);
			try {
				return result.getBoolean("valid");
			} catch (JSONException e) {
				e.printStackTrace();
				return false;
			}
		}

		@Override
		public void run() {
			try {
				deviceId = sharedPreferences.getString("device_id", "");
				if (deviceId.contentEquals("") || !idIsStillValid(deviceId)) {
					deviceId = getNewDeviceId();
					Editor editor = sharedPreferences.edit();
					editor.putString("device_id", deviceId);
					editor.commit();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static class DataVersionChecker implements Runnable {
		private JSONObject getNewStopToLatLngDictionary() {
			return getJSONObjectForURL(BASE_URL + "/data/stops/dictionary/ids");
		}

		private JSONObject getNewStopToTCATIdDictionary() {
			return getJSONObjectForURL(BASE_URL + "/data/stops/dictionary/ids");
		}

		/**
		 * Checks that we have the same data as the server
		 * 
		 * @param version
		 *            The version string for our data
		 * @return
		 */
		private boolean dataIsStillValid(String version) {
			JSONObject result = getJSONObjectForURL(BASE_URL
					+ "/checkdataversion/" + version);
			try {
				return result.getBoolean("valid");
			} catch (JSONException e) {
				e.printStackTrace();
				return false;
			}
		}

		private String getServerDataVersion() {
			JSONObject result = getJSONObjectForURL(BASE_URL
					+ "/getdataversion/");
			try {
				return result.getString("version");
			} catch (JSONException e) {
				e.printStackTrace();
				return NULL_DATA_VERSION;
			}
		}

		@Override
		public void run() {
			try {
				JSONObject stopToLatLngs = null, stopToTCATIds = null;
				dataVersion = sharedPreferences.getString("data_version", "");
				if (dataVersion.contentEquals("")
						|| !dataIsStillValid(dataVersion)) {
					stopToLatLngs = getNewStopToLatLngDictionary();
					stopToTCATIds = getNewStopToTCATIdDictionary();
					dataVersion = getServerDataVersion();

					Editor editor = sharedPreferences.edit();
					editor.putString("data_version", dataVersion);
					editor.commit();

					try {
						saveStopToLatLngDictionaryData(stopToLatLngs.toString());
						saveStopToTcatIdDictionaryData(stopToTCATIds.toString());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				generateDefaultCards();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

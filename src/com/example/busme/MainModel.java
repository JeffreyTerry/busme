package com.example.busme;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

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
	public static final String LOCATION_CURRENT = "jdksCurrentLOcation";
	public static final String LOCATION_UNSPECIFIED = "";
	public static final String BASE_URL = "http://www.theseedok.com/api";
	public static final String ROUTE_LINE_DATA_FILE_BASE_NAME = "route_lines_";

	private static final String STOP_TO_LOCATION_DATA_FILE = "stop_locations";
	private static final String STOP_TO_ID_DATA_FILE = "stop_ids";
	private static final String NULL_DEVICE_ID = "9876";
	private static final String NULL_DATA_VERSION = "-1";
	private String deviceId = NULL_DEVICE_ID;
	// used to keep the app's data in sync with the server's data
	private String dataVersion = NULL_DATA_VERSION;
	private Context context = null;
	private SharedPreferences sharedPreferences = null;
	private MainController mainController;
	private BusDataController busDataController;

	public MainModel(Context c, MainController mc, boolean loadDefaultCards) {
		if (context == null) {
			context = c;
			mainController = mc;
			busDataController = new BusDataController(context, this);
		} else {
			Log.e("ERROR", "MainModel was instantiated twice");
		}

		// check to make sure our device id and stop data is valid
		new Thread(new DeviceIdChecker()).start();
		new Thread(new DataVersionChecker(loadDefaultCards)).start();
	}

	/**
	 * Gets a JSON object from the server at "BASE_URL + apiURL"
	 * 
	 * @param apiURL
	 * @return
	 */
	public static JSONObject getJSONObjectForURL(String url) {
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
	public void saveToFile(String data, String filename) {
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

	public String readFromFile(String filename) {
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

	public String getStopToLatLngDictionaryData() {
		String data = readFromFile(STOP_TO_LOCATION_DATA_FILE);
		return data;
	}

	public void saveStopToLatLngDictionaryData(String data) {
		saveToFile(data, STOP_TO_LOCATION_DATA_FILE);
	}

	public String getStopToTcatIdDictionaryData() {
		String data = readFromFile(STOP_TO_ID_DATA_FILE);
		return data;
	}

	public void saveStopToTcatIdDictionaryData(String data) {
		saveToFile(data, STOP_TO_ID_DATA_FILE);
	}

	public String getDeviceId() {
		return deviceId;
	}

	// ////// CARD STUFF ///////
	private void sendCardsToController(ArrayList<MainListViewItem> cards) {
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
	public void removeStartEndQueryFromDatabase(String startid, String endid) {
		busDataController.removeStartEndQueryFromDatabase(startid, endid);
	}

	public void removeStartQueryFromDatabase(String startid) {
		busDataController.removeStartQueryFromDatabase(startid);
	}

	public void removeEndQueryFromDatabase(String endid) {
		busDataController.removeEndQueryFromDatabase(endid);
	}

	private void sendCardsToController(ArrayList<MainListViewItem> cards,
			String errorCode) {
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
	public void generateDefaultCards() {
		((Activity) context).runOnUiThread(new Runnable() {
			public void run() {
				new CardGenerator().execute(LOCATION_UNSPECIFIED,
						LOCATION_UNSPECIFIED);
			}
		});
	}

	public void generateCardsForQuery(String start, String destination) {
		final String finalStart = start;
		final String finalDestination = destination;
		((Activity) context).runOnUiThread(new Runnable() {
			public void run() {
				new CardGenerator().execute(finalStart, finalDestination);
			}
		});
	}

	private class CardGenerator extends
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
				ArrayList<MainListViewItem> cards = busDataController
						.getCardsForQuery(endpoints[0], endpoints[1]);
				if (cards == null) {
					return null;
				} else {
					Collections
							.sort(cards, MainListViewItem.DEFAULT_COMPARATOR);
					return cards;
				}
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
	private class DeviceIdChecker implements Runnable {
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
				if (sharedPreferences == null) {
					sharedPreferences = context.getSharedPreferences(
							"com.example.busme", Context.MODE_PRIVATE);
				}

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

	private class DataVersionChecker implements Runnable {
		private boolean loadDefaultCards;

		public DataVersionChecker(boolean loadDefaultCards) {
			this.loadDefaultCards = loadDefaultCards;
		}

		private JSONObject getNewStopToLatLngDictionary() {
			return getJSONObjectForURL(BASE_URL
					+ "/data/stops/dictionary/latlngs");
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
				if (sharedPreferences == null) {
					sharedPreferences = context.getSharedPreferences(
							"com.example.busme", Context.MODE_PRIVATE);
				}

				JSONObject stopToLatLngs = null, stopToTCATIds = null;
				dataVersion = sharedPreferences.getString("data_version", "");
				dataVersion = "-1";
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
				if (loadDefaultCards) {
					generateDefaultCards();
				} else {
					generateCardsForQuery(MainModel.LOCATION_CURRENT, MainModel.LOCATION_UNSPECIFIED);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

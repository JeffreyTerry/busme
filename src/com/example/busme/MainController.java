package com.example.busme;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

public class MainController implements OnEditorActionListener,
		OnItemClickListener, SwipeDismisserListView.DismissCallbacks,
		OnTouchListener {
	// public static final long LOCATION_UPDATE_FREQUENCY = 120000L;
	// public static final long TWO_HOURS = 7200000L;
	// public static final long LOCATION_UPDATE_TIMEOUT = TWO_HOURS
	// / LOCATION_UPDATE_FREQUENCY;
	// public static final int ALARM_REQUEST_CODE = 984375;
	// public static final double BUS_SPEED_THRESHOLD = 5.0;
	// private int numberOfLocationUpdates;
	public static final String ET_SEARCH_FROM_EXTRA = "search_from_et";
	public static final String ET_SEARCH_TO_EXTRA = "search_to_et";

	// These elements belong to MainActivity
	private ListView mainActivityListView = null;
	private EditText etSearchFrom, etSearchTo;
	private SwipeDismisserListView swipeDismisserListView;

	// These elements belong to SearchActivity
	private ListView searchActivityListView = null;
	private EditText etStartQuery, etDestinationQuery;

	// These elements are present in both MainActivity and SearchActivity
	private MainListViewAdapter listViewAdapter;

	private Context context;
	private MainModel mainModel;
	private HashMap<String, LatLng> stopToLatLngs = null;
	private HashMap<String, String> stopToTcatIds = null;
	private float x1;

	public MainController(Context c, boolean loadDefaultCards) {
		mainModel = new MainModel(c, this, loadDefaultCards);

		context = c;
		createMainListViewAdapter();

		// numberOfLocationUpdates = 1;
		// c.registerReceiver(this,
		// LocationTracker.getLocationBroadcastIntentFilter());
		// startAlarmBroadcaster();
	}

	public void setSearchActivityEtStart(EditText etStart) {
		this.etStartQuery = etStart;
		this.etStartQuery.setOnEditorActionListener(this);
	}

	public void setSearchActivityEtDestination(EditText etDestination) {
		this.etDestinationQuery = etDestination;
		this.etDestinationQuery.setOnEditorActionListener(this);
	}

	public void setSearchActivityListView(ListView v) {
		searchActivityListView = v;
		searchActivityListView.setAdapter(listViewAdapter);
		searchActivityListView.setOnItemClickListener(this);
	}

	public void setMainActivityEtSearchFrom(EditText etSearchFrom) {
		this.etSearchFrom = etSearchFrom;
		this.etSearchFrom.setOnTouchListener(this);
	}

	public void setMainActivityEtSearchTo(EditText etSearchTo) {
		this.etSearchTo = etSearchTo;
		this.etSearchTo.setOnTouchListener(this);
	}

	public void setMainActivityListView(ListView v) {
		mainActivityListView = v;
		mainActivityListView.setAdapter(listViewAdapter);
		mainActivityListView.setOnItemClickListener(this);
		swipeDismisserListView = new SwipeDismisserListView(
				mainActivityListView, this);
		mainActivityListView.setOnTouchListener(this);
		mainActivityListView.setOnScrollListener(swipeDismisserListView
				.makeScrollListener());
	}

	private void createMainListViewAdapter() {
		listViewAdapter = new MainListViewAdapter(context,
				new ArrayList<MainListViewItem>());
	}

	public MainModel getMainModel() {
		return mainModel;
	}

	private void launchSearchActivity(String launchButtonSource) {
		Intent i = new Intent(context, MainSearchActivity.class);
		i.putExtra("launch_button_source", launchButtonSource);
		context.startActivity(i);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:

			switch (v.getId()) {
			case R.id.etSearchFrom:
				launchSearchActivity(ET_SEARCH_FROM_EXTRA);
				break;
			case R.id.etSearchTo:
				launchSearchActivity(ET_SEARCH_TO_EXTRA);
				break;
			default:
				x1 = event.getX();
				swipeDismisserListView.onTouch(v, event);
				break;
			}
		case MotionEvent.ACTION_UP:
			swipeDismisserListView.onTouch(v, event);
			break;
		case MotionEvent.ACTION_MOVE:
			if (event.getX() > x1) {
				swipeDismisserListView.onTouch(v, event);
			}
			break;
		case MotionEvent.ACTION_CANCEL:
			if (event.getX() > x1) {
				swipeDismisserListView.onTouch(v, event);
			}
			break;
		}
		return false;
	}

	@Override
	public boolean canDismiss(int position) {
		return !(position == 0 || position == listViewAdapter.getCount() - 1);
	}

	/* When a card is dismissed, notify server */
	public void onDismiss(ListView listView, int[] reverseSortedPositions) {
		MainListViewItem itemDismissed = null;
		for (int position : reverseSortedPositions) {
			if (position != 0 && position != listViewAdapter.getCount() - 1) {
				itemDismissed = listViewAdapter.getItem(position + 1);
				listViewAdapter.remove(itemDismissed);
			}
		}
		new Thread(new DatabaseCardRemovalTask(itemDismissed)).start();

		listViewAdapter.notifyDataSetChanged();
	}

	/**
	 * Opens up a detail view
	 */
	@Override
	public void onItemClick(AdapterView<?> av, View v, int position, long l) {
		MainListViewItem item = listViewAdapter.getItem(position + 1);
		if (item == MainListViewItem.NULL_ITEM) {
			return;
		}
		Intent i = new Intent(context, ListItemDetailActivity.class);
		i.putExtra("time", item.getNextBusTime());
		i.putExtra("start", item.getRouteStart());
		i.putExtra("destination", item.getRouteDestination());
		i.putExtra("routeNumbers", item.getRouteNumbers());
		i.putExtra("startLat", item.getStartLat());
		i.putExtra("startLng", item.getStartLng());
		i.putExtra("destLat", item.getDestLat());
		i.putExtra("destLng", item.getDestLng());
		i.putExtra("travelTime", item.getTravelTime());
		context.startActivity(i);
	}

	/**
	 * Hide the keyboard
	 */
	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		if (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
			InputMethodManager in = (InputMethodManager) context
					.getSystemService(Context.INPUT_METHOD_SERVICE);

			in.hideSoftInputFromWindow(v.getApplicationWindowToken(),
					InputMethodManager.HIDE_NOT_ALWAYS);
			v.clearFocus();
			View nextView;
			switch (v.getId()) {
			case R.id.etStartQuery:
				nextView = v.focusSearch(View.FOCUS_DOWN).focusSearch(
						View.FOCUS_DOWN);
				if (nextView != null) {
					nextView.requestFocus();
				}

				makeSearchQuery();
				break;
			case R.id.etDestinationQuery:
				nextView = v.focusSearch(View.FOCUS_DOWN);
				if (nextView != null) {
					nextView.requestFocus();
				}

				makeSearchQuery();
				break;
			default:
				break;
			}

			// Must return true here to consume event
			return true;

		}
		return false;
	}

	private void makeSearchQuery() {
		String startQuery = etStartQuery.getText().toString();
		if (startQuery.contentEquals("")) {
			startQuery = MainModel.LOCATION_CURRENT;
		}
		String endQuery = etDestinationQuery.getText().toString();
		if (endQuery.contentEquals("")) {
			endQuery = MainModel.LOCATION_UNSPECIFIED;
		}
		mainModel.generateCardsForQuery(startQuery, endQuery);
	}

	public void setCardsLoading(boolean loading) {
		listViewAdapter.setLoading(loading);
	}

	public void setCards(ArrayList<MainListViewItem> cards) {
		listViewAdapter.clear();
		listViewAdapter.addAll(cards);
	}

	public void makeError(String errorCode) {
		Toast.makeText(context, errorCode, Toast.LENGTH_SHORT).show();
	}

	/**
	 * Stolen from BusDataHandler
	 * 
	 * @return
	 */
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

	private class DatabaseCardRemovalTask implements Runnable {
		MainListViewItem cardToRemove = null;

		public DatabaseCardRemovalTask(MainListViewItem card) {
			cardToRemove = card;
		}

		@Override
		public void run() {
			if (cardToRemove == null) {
				return;
			} else {
				if (initializeStopData()) {
					if (stopToTcatIds.containsKey(cardToRemove.getRouteStart())
							&& stopToTcatIds.containsKey(cardToRemove
									.getRouteDestination())) {
						mainModel.removeStartEndQueryFromDatabase(
								cardToRemove.getRouteStart(),
								cardToRemove.getRouteDestination());
					} else if (stopToTcatIds.containsKey(cardToRemove
							.getRouteStart())) {
						mainModel.removeStartQueryFromDatabase(cardToRemove
								.getRouteStart());
					} else if (stopToTcatIds.containsKey(cardToRemove
							.getRouteDestination())) {
						mainModel.removeEndQueryFromDatabase(cardToRemove
								.getRouteDestination());
					}
				}
			}
		}
	}

	// private void resetLocationUpdateCount() {
	// numberOfLocationUpdates = 0;
	// startAlarmBroadcaster();
	// }
	//
	// private void startAlarmBroadcaster() {
	// AlarmManager am = (AlarmManager) context
	// .getSystemService(Context.ALARM_SERVICE);
	// Intent i = new Intent(context, LocationTracker.class);
	// PendingIntent updateLocationIntent = PendingIntent.getBroadcast(
	// context, ALARM_REQUEST_CODE, i,
	// PendingIntent.FLAG_CANCEL_CURRENT);
	// am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
	// + LOCATION_UPDATE_FREQUENCY, LOCATION_UPDATE_FREQUENCY,
	// updateLocationIntent);
	// }
	//
	// private void stopAlarmBroadcaster() {
	// AlarmManager am = (AlarmManager) context
	// .getSystemService(Context.ALARM_SERVICE);
	// Intent i = new Intent(context, LocationTracker.class);
	// PendingIntent updateLocationIntent = PendingIntent.getBroadcast(
	// context, ALARM_REQUEST_CODE, i,
	// PendingIntent.FLAG_CANCEL_CURRENT);
	//
	// am.cancel(updateLocationIntent);
	// }
	// /**
	// * This pushes location updates to our server if the phone is believed to
	// be
	// * on a bus
	// */
	// @Override
	// public void onReceive(Context context, Intent intent) {
	// numberOfLocationUpdates++;
	// if (numberOfLocationUpdates > LOCATION_UPDATE_TIMEOUT) {
	// stopAlarmBroadcaster();
	// return;
	// }
	// Location location = (Location) intent
	// .getParcelableExtra(LocationManager.KEY_LOCATION_CHANGED);
	// if (location.getSpeed() >= BUS_SPEED_THRESHOLD) {
	// // TODO send this location to the server
	// }
	// }
}

package com.example.busme;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLng;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
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
	private EditText etStart, etDestination;
	private MainListViewAdapter mainListViewAdapter;
	private Context context;
	private SwipeDismisserListView swipeDismisserListView;
	private HashMap<String, LatLng> stopToLatLngs = null;
	private HashMap<String, String> stopToTcatIds = null;
	private float x1;

	public MainController(Context c) {
		MainModel.initialize(c, this);

		context = c;
		createMainListViewAdapter();

		// numberOfLocationUpdates = 1;
		// c.registerReceiver(this,
		// LocationTracker.getLocationBroadcastIntentFilter());
		// startAlarmBroadcaster();
	}

	public void setEtStart(EditText etStart) {
		this.etStart = etStart;
		this.etStart.setOnEditorActionListener(this);
	}

	public void setEtDestination(EditText etDestination) {
		this.etDestination = etDestination;
		this.etDestination.setOnEditorActionListener(this);
	}

	public void setListView(ListView v) {
		v.setAdapter(mainListViewAdapter);
		v.setOnItemClickListener(this);
		swipeDismisserListView = new SwipeDismisserListView(v, this);
		v.setOnTouchListener(this);
		v.setOnScrollListener(swipeDismisserListView.makeScrollListener());
	}

	private void createMainListViewAdapter() {
		mainListViewAdapter = new MainListViewAdapter(context,
				new ArrayList<MainListViewItem>());
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			x1 = event.getX();
			swipeDismisserListView.onTouch(v, event);
		case MotionEvent.ACTION_UP:
			swipeDismisserListView.onTouch(v, event);
			v.performClick();
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
		return !(position == 0 || position == mainListViewAdapter.getCount() - 1);
	}

	/* When a card is dismissed, notify server */
	public void onDismiss(ListView listView, int[] reverseSortedPositions) {
		MainListViewItem itemDismissed = null;
		for (int position : reverseSortedPositions) {
			if (position != 0 && position != mainListViewAdapter.getCount() - 1) {
				itemDismissed = mainListViewAdapter.getItem(position + 1);
				mainListViewAdapter.remove(itemDismissed);
			}
		}
		new Thread(new DatabaseCardRemovalTask(itemDismissed)).start();

		mainListViewAdapter.notifyDataSetChanged();
	}

	/**
	 * Opens up a detail view
	 */
	@Override
	public void onItemClick(AdapterView<?> av, View v, int position, long l) {
		MainListViewItem item = mainListViewAdapter.getItem(position + 1);
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
			case R.id.etStart:
				nextView = v.focusSearch(View.FOCUS_DOWN).focusSearch(
						View.FOCUS_DOWN);
				if (nextView != null) {
					nextView.requestFocus();
				}
				break;
			case R.id.etDestination:
				nextView = v.focusSearch(View.FOCUS_DOWN);
				if (nextView != null) {
					nextView.requestFocus();
				}
				break;
			default:
				break;
			}

			String startQuery = etStart.getText().toString();
			if (startQuery.contentEquals("")) {
				startQuery = MainModel.LOCATION_CURRENT;
			}
			String endQuery = etDestination.getText().toString();
			if (endQuery.contentEquals("")) {
				endQuery = MainModel.LOCATION_UNSPECIFIED;
			}
			MainModel.generateCardsForQuery(startQuery, endQuery);

			// Must return true here to consume event
			return true;

		}
		return false;
	}

	public void setCardsLoading(boolean loading) {
		mainListViewAdapter.setLoading(loading);
	}

	public void setCards(ArrayList<MainListViewItem> cards) {
		mainListViewAdapter.clear();
		mainListViewAdapter.addAll(cards);
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
			String stopToLatLngDictionaryData = MainModel
					.getStopToLatLngDictionaryData();
			if (stopToLatLngDictionaryData != null) {
				stopToLatLngs = JSONConverter
						.convertStopLatLngsToHashMap(new JSONObject(
								stopToLatLngDictionaryData));
			}

			String stopToTcatIdDictionaryData = MainModel
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
				if(initializeStopData()){
					if(stopToTcatIds.containsKey(cardToRemove.getRouteStart()) && stopToTcatIds.containsKey(cardToRemove.getRouteDestination())) {
						BusDataHandler.removeStartEndQueryFromDatabase(cardToRemove.getRouteStart(), cardToRemove.getRouteDestination());
					} else if(stopToTcatIds.containsKey(cardToRemove.getRouteStart())) {
						BusDataHandler.removeStartQueryFromDatabase(cardToRemove.getRouteStart());
					} else if(stopToTcatIds.containsKey(cardToRemove.getRouteDestination())) {
						BusDataHandler.removeEndQueryFromDatabase(cardToRemove.getRouteDestination());
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

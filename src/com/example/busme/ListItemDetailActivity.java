package com.example.busme;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.SparseIntArray;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class ListItemDetailActivity extends Activity implements
		LocationListener {
	private static final int DEFAULT_CAMERA_ZOOM = 12;
	private static SparseIntArray routeColors;
	private GoogleMap gmap;
	private Bundle extras;
	private ArrayList<LatLng> markerPoints;
	private JSONArray routeCoordinates;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.map_activity);

		markerPoints = new ArrayList<LatLng>();

		initializeRouteColors();
		initializeMapFragment();
		initializeFonts();
	}

	private void initializeRouteColors() {
		routeColors = new SparseIntArray();
		// routes [10, 11, 13, 14, 15, 17, 20, 21, 30, 31, 32, 36, 37, 40, 41,
		// 43, 51, 52, 53, 65, 67, 70, 72, 75, 77, 81, 82, 83, 90, 92, 93]
		routeColors.put(10, Color.CYAN);
		routeColors.put(11, Color.GREEN);
		routeColors.put(13, Color.RED);
		routeColors.put(14, Color.BLUE);
		routeColors.put(15, Color.YELLOW);
		routeColors.put(17, Color.MAGENTA);
		routeColors.put(20, Color.CYAN);
		routeColors.put(21, Color.GREEN);
		routeColors.put(30, Color.RED);
		routeColors.put(31, Color.BLUE);
		routeColors.put(32, Color.YELLOW);
		routeColors.put(36, Color.MAGENTA);
		routeColors.put(37, Color.CYAN);
		routeColors.put(40, Color.GREEN);
		routeColors.put(41, Color.RED);
		routeColors.put(43, Color.BLUE);
		routeColors.put(51, Color.YELLOW);
		routeColors.put(52, Color.MAGENTA);
		routeColors.put(53, Color.CYAN);
		routeColors.put(65, Color.GREEN);
		routeColors.put(67, Color.RED);
		routeColors.put(70, Color.BLUE);
		routeColors.put(72, Color.YELLOW);
		routeColors.put(75, Color.MAGENTA);
		routeColors.put(77, Color.CYAN);
		routeColors.put(81, Color.GREEN);
		routeColors.put(82, Color.RED);
		routeColors.put(83, Color.BLUE);
		routeColors.put(90, Color.YELLOW);
		routeColors.put(92, Color.MAGENTA);
		routeColors.put(93, Color.CYAN);
	}

	private void initializeFonts() {
		String bebas = "BebasNeue.otf";
		String exo = "Exo-Regular.otf";
		String ubuntu = "Ubuntu-Title.ttf";

		Typeface Bebas = Typeface.createFromAsset(getAssets(), bebas);
		Typeface Exo = Typeface.createFromAsset(getAssets(), exo);
		Typeface Ubuntu = Typeface.createFromAsset(getAssets(), ubuntu);

		TextView bd1 = (TextView) findViewById(R.id.board1);
		TextView bd2 = (TextView) findViewById(R.id.board2);
		TextView trv1 = (TextView) findViewById(R.id.travel1);
		TextView trv2 = (TextView) findViewById(R.id.travel2);
		TextView dest1 = (TextView) findViewById(R.id.destination1);
		TextView dest2 = (TextView) findViewById(R.id.destination2);
		TextView percentage = (TextView) findViewById(R.id.percent);

		bd1.setTypeface(Exo);
		bd2.setTypeface(Exo);
		trv1.setTypeface(Exo);
		trv2.setTypeface(Exo);
		dest1.setTypeface(Exo);
		dest2.setTypeface(Exo);
		percentage.setTypeface(Ubuntu);

		// UPDATING THE DATA

		// boarding time
		String startTime = extras.getString("time");
		bd2.setText(startTime);

		// travel time
		int travelTime = Integer.parseInt(extras.getString("travelTime"));
		trv2.setText(extras.getString("travelTime") + " min");

		// time of arrival
		SimpleDateFormat dateFormatter = new SimpleDateFormat("h:mm a",
				Locale.US);
		try {
			Date date;
			date = dateFormatter.parse(startTime);
			date = new Date(date.getTime() + travelTime * 60 * 1000);
			if (("" + travelTime).equals(MainListViewItem.TRAVEL_TIME_UNKNOWN)) {
				dest2.setText(startTime);
			} else {
				dest2.setText(dateFormatter.format(date));
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private void initializeMapFragment() {
		extras = this.getIntent().getExtras();

		gmap = ((MapFragment) this.getFragmentManager().findFragmentById(
				R.id.fgmap)).getMap();

		LatLng startLatLng = new LatLng(extras.getDouble("startLat"),
				extras.getDouble("startLng"));
		LatLng destLatLng = new LatLng(extras.getDouble("destLat"),
				extras.getDouble("destLng"));

		markerPoints.add(startLatLng);
		markerPoints.add(destLatLng);

		Location location = updateLocation();

		if(location != null) {
			LatLng current = new LatLng(location.getLatitude(),
					location.getLongitude());

			synchronized (this) {
				float[] distance = new float[3];
				Location.distanceBetween(current.latitude, current.longitude,
						(startLatLng.latitude + destLatLng.latitude) / 2,
						(startLatLng.longitude + destLatLng.longitude) / 2,
						distance);
				if (distance[0] < 35000) {
					markerPoints.add(current);
				}
			}
		} else {
			Toast.makeText(this, "We were unable to determine your location", Toast.LENGTH_SHORT).show();
		}

		// adding start & destination markers
		MarkerOptions startOptions = new MarkerOptions().position(startLatLng);
		MarkerOptions endOptions = new MarkerOptions().position(destLatLng);

		startOptions.title(extras.getString("start"));
		startOptions.icon(BitmapDescriptorFactory
				.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
		startOptions.snippet("Start Location");

		endOptions.title(extras.getString("destination"));
		endOptions.icon(BitmapDescriptorFactory
				.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
		endOptions.snippet("Destination");

		Marker startMarker = gmap.addMarker(startOptions);
		Marker destMarker = gmap.addMarker(endOptions);
		startMarker.showInfoWindow();

		// requests route data from server
		int[] routeNumbers = extras.getIntArray("routeNumbers");
		for (int i = 0; i < routeNumbers.length; i++) {
			new GetRouteDataTask().execute(routeNumbers[i]);
		}

		CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(
				getDefaultLatLngBounds(), 20, 20, 5);
		gmap.moveCamera(cu);
		gmap.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_CAMERA_ZOOM));

	}

	public synchronized LatLngBounds getDefaultLatLngBounds() {
		LatLngBounds.Builder b = new LatLngBounds.Builder();
		for (int i = 0; i < markerPoints.size(); i++) {
			b.include(markerPoints.get(i));
		}
		return b.build();
	}

	public Location updateLocation() {
		LocationManager locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
		Criteria crit = new Criteria();
		crit.setAccuracy(Criteria.ACCURACY_FINE);
		String provider = locationManager.getBestProvider(crit, true);
		Location location = locationManager.getLastKnownLocation(provider);

		if (location != null) {
			onLocationChanged(location);
		}
		locationManager.requestSingleUpdate(provider, this,
				Looper.getMainLooper());
		return location;
	}

	@Override
	protected void onPause() {
		super.onPause();
		gmap.setMyLocationEnabled(false);
	}

	@Override
	protected void onResume() {
		super.onResume();
		gmap.setMyLocationEnabled(true);
		updateLocation();
	}

	@Override
	public void onLocationChanged(Location location) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public String getRouteCoordinateData(int routeNumber) {
		String data = readFromFile(MainModel.ROUTE_LINE_DATA_FILE_BASE_NAME + routeNumber);
		return data;
	}

	public void saveRouteCoordinateData(String data, int routeNumber) {
		saveToFile(data, MainModel.ROUTE_LINE_DATA_FILE_BASE_NAME + routeNumber);
	}

	/**
	 * Stolen from MainModel
	 * @param data
	 * @param filename
	 */
	public void saveToFile(String data, String filename) {
		try {
			FileOutputStream fos = this.openFileOutput(filename,
					Context.MODE_PRIVATE);
			fos.write(data.getBytes());
			fos.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Stolen from MainModel
	 * @param filename
	 */
	public String readFromFile(String filename) {
		try {
			BufferedReader bis = new BufferedReader(new InputStreamReader(
					this.openFileInput(filename)));
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


	private class GetRouteDataTask extends
			AsyncTask<Integer, Void, ArrayList<LatLng>> {
		private int routeNumber;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected ArrayList<LatLng> doInBackground(Integer... routeNumbers) {
			this.routeNumber = routeNumbers[0];
			try {
				String routeData = getRouteCoordinateData(routeNumber);
				if (routeData != null) {
					routeCoordinates = new JSONArray(routeData);
				} else {
					routeCoordinates = MainModel
							.getJSONArrayForURL(MainModel.BASE_URL
									+ "/data/route/" + routeNumber);
					saveRouteCoordinateData(routeCoordinates.toString(),
							routeNumber);
				}
				return JSONConverter
						.convertRouteArrayToHashMap(routeCoordinates);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(ArrayList<LatLng> result) {
			super.onPostExecute(result);
			if (result == null) {
				return;
			}
			PolylineOptions plOptions;
			if (routeColors.indexOfKey(routeNumber) >= 0) {
				plOptions = new PolylineOptions().width(15).color(
						routeColors.get(routeNumber));
			} else {
				plOptions = new PolylineOptions().width(15).color(Color.WHITE);
			}

			for (int i = 0; i < result.size(); i++) {
				plOptions.add(result.get(i));
			}
			gmap.addPolyline(plOptions);
		}
	}
}

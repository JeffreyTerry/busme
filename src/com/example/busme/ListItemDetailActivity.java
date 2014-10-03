package com.example.busme;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.widget.TextView;

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
	private static SparseIntArray routeColors;
	private GoogleMap gmap;
	private Bundle extras;
	ArrayList<LatLng> markerPoints;
	private JSONArray routeCoordinates;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.map_activity);
		initializeRouteColors();
		initializeMapFragment();

		markerPoints = new ArrayList<LatLng>();
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
			dest2.setText(dateFormatter.format(date));
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private void initializeMapFragment() {
		extras = this.getIntent().getExtras();

		gmap = ((MapFragment) this.getFragmentManager().findFragmentById(
				R.id.fgmap)).getMap();
		gmap.setMyLocationEnabled(true);

		LocationManager locationManager = (LocationManager) this
				.getSystemService(LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		String provider = locationManager.getBestProvider(criteria, true);
		Location location = locationManager.getLastKnownLocation(provider);

		locationManager.requestLocationUpdates(provider, 20000, 0, this);

		LatLng current = new LatLng(location.getLatitude(),
				location.getLongitude());
		LatLng startLatLng = new LatLng(extras.getDouble("startLat"),
				extras.getDouble("startLng"));
		LatLng destLatLng = new LatLng(extras.getDouble("destLat"),
				extras.getDouble("destLng"));

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
			new QueryTask().execute(routeNumbers[i]);
		}

		LatLngBounds.Builder b = new LatLngBounds.Builder();
		// b.include(current);
		b.include(startLatLng);
		b.include(destLatLng);
		LatLngBounds bounds = b.build();

		CameraUpdate cu = CameraUpdateFactory
				.newLatLngBounds(bounds, 20, 20, 5);
		gmap.moveCamera(cu);
		gmap.animateCamera(CameraUpdateFactory.zoomTo(13));

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

	private class QueryTask extends AsyncTask<Integer, Void, ArrayList<LatLng>> {
		private int routeNumber;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected ArrayList<LatLng> doInBackground(Integer... routeNumbers) {
			this.routeNumber = routeNumbers[0];
			try {
				String routeData = MainModel.getRouteCoordinateData(routeNumber);
				if (routeData != null) {
					routeCoordinates = new JSONArray(routeData);
				} else {
					routeCoordinates = MainModel
							.getJSONArrayForURL(MainModel.BASE_URL + "/data/route/" + routeNumber);
					MainModel.saveRouteData(routeCoordinates.toString(), routeNumber);
				}
				return JSONConverter.convertRouteArrayToHashMap(routeCoordinates);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(ArrayList<LatLng> result) {
			super.onPostExecute(result);
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

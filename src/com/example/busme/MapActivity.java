package com.example.busme;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
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

public class MapActivity extends Activity implements LocationListener {

	private GoogleMap gmap;
	private Bundle extras;
	ArrayList<LatLng> markerPoints;
	private JSONArray routeCoordinates;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.map_activity);
		initializeMapFragment();

		markerPoints = new ArrayList<LatLng>();
		initializeFonts();
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
		new QueryTask().execute();

		LatLngBounds.Builder b = new LatLngBounds.Builder();
		// b.include(current);
		b.include(startLatLng);
		b.include(destLatLng);
		LatLngBounds bounds = b.build();

		CameraUpdate cu = CameraUpdateFactory
				.newLatLngBounds(bounds, 20, 20, 5);
		gmap.moveCamera(cu);
		gmap.animateCamera(CameraUpdateFactory.zoomTo(14));

	}

	private ArrayList<LatLng> parseJSONArray(JSONArray array) {
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

	private class QueryTask extends AsyncTask<String, Void, ArrayList<LatLng>> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected ArrayList<LatLng> doInBackground(String... args) {
			try {
				int routeNumber = extras.getInt("routeNumber");
				routeCoordinates = MainModel.getJSONArrayForURL("/data/route/"
						+ routeNumber);
				return parseJSONArray(routeCoordinates);
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(ArrayList<LatLng> result) {
			super.onPostExecute(result);
			PolylineOptions plOptions = new PolylineOptions().width(15).color(
					Color.CYAN);

			for (int i = 0; i < result.size(); i++) {
				plOptions.add(result.get(i));
			}
			gmap.addPolyline(plOptions);
		}
	}
}

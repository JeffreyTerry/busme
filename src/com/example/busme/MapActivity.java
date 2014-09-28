package com.example.busme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;

import android.util.Log;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapActivity extends Activity implements LocationListener {

	private GoogleMap gmap;
	private Bundle extras;
	ArrayList<LatLng> markerPoints;
	private static final String BASE_URL = "http://www.theseedok.com/api";
	private JSONArray routeCoordinates;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.map_activity);
		initializeMapFragment();

		markerPoints = new ArrayList<LatLng>();
		initializeFonts();
	}
	
	private void initializeFonts(){
        String bebas = "BebasNeue.otf";
        String exo = "Exo-Regular.otf";
        String ubuntu = "Ubuntu-Title.ttf";
        
        Typeface Bebas = Typeface.createFromAsset(getAssets(), bebas);
        Typeface Exo = Typeface.createFromAsset(getAssets(), exo);
        Typeface Ubuntu = Typeface.createFromAsset(getAssets(), ubuntu);
        
        TextView busNum = (TextView) findViewById(R.id.busNum);
        TextView arrivalTime = (TextView) findViewById(R.id.arrivalTime);
        TextView bd1 = (TextView) findViewById(R.id.board1);
        TextView bd2 = (TextView) findViewById(R.id.board2);
        TextView trv1 = (TextView) findViewById(R.id.travel1);
        TextView trv2 = (TextView) findViewById(R.id.travel2);
        TextView dest1 = (TextView) findViewById(R.id.destination1);
        TextView dest2 = (TextView) findViewById(R.id.destination2);
        TextView percentage = (TextView) findViewById(R.id.percent);
        
        busNum.setTypeface(Bebas);
        bd1.setTypeface(Exo);
        bd2.setTypeface(Exo);
        trv1.setTypeface(Exo);
        trv2.setTypeface(Exo);
        dest1.setTypeface(Exo);
        dest2.setTypeface(Exo);
        arrivalTime.setTypeface(Exo);
        percentage.setTypeface(Ubuntu);
        
        
        
	}

	private void initializeMapFragment() {
		// TODO Auto-generated method stub
		extras = this.getIntent().getExtras();

		gmap = ((MapFragment) this.getFragmentManager().findFragmentById(
				R.id.fgmap)).getMap();
		gmap.setMyLocationEnabled(true);

		LocationManager locationManager = (LocationManager) this
				.getSystemService(LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		String provider = locationManager.getBestProvider(criteria, true);
		Location location = locationManager.getLastKnownLocation(provider);

		if (location != null) {
			onLocationChanged(location);
		}
		locationManager.requestLocationUpdates(provider, 20000, 0, this);
		
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
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return list;
	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		
		LatLngBounds.Builder b = new LatLngBounds.Builder();
		b.include(new LatLng(location.getLatitude(), location.getLongitude()));
		b.include(new LatLng(extras.getDouble("startLat"), extras.getDouble("startLng")));
		b.include(new LatLng(extras.getDouble("destLat"), extras.getDouble("destLng")));
		LatLngBounds bounds = b.build();
		
		CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 25,25,5);
		gmap.moveCamera(cu);
		gmap.animateCamera(cu);

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	/**
	 * Gets JSON from the server at "BASE_URL + apiURL"
	 * 
	 * @param apiURL
	 * @return
	 */
	private JSONArray getJSONArrayForURL(String apiURL) {
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(BASE_URL + apiURL);
			HttpResponse response = client.execute(request);

			// Get the response
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent(), "UTF-8"));
			StringBuilder builder = new StringBuilder();
			for (String line = null; (line = reader.readLine()) != null;) {
				builder.append(line).append("\n");
			}
			System.out.println("builder tostring" + builder.toString());
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


	private class QueryTask extends AsyncTask<String, Void, ArrayList<LatLng>> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected ArrayList<LatLng> doInBackground(String... args) {
			try {
				routeCoordinates = getJSONArrayForURL("/data/route/11");
				return parseJSONArray(routeCoordinates);
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(ArrayList<LatLng> result) {
			super.onPostExecute(result);
			PolylineOptions plOptions = new PolylineOptions()
			.width(15)
			.color(Color.CYAN);

			for (int i = 0; i < result.size(); i++) {
				plOptions.add(result.get(i));
			}
			gmap.addPolyline(plOptions);
		}
	}
}

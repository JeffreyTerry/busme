package com.example.busme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class MainViewMapFragment extends Fragment implements LocationListener {

	GoogleMap gmap;
	FragmentTransaction fragmentTransaction;
	private static final String BASE_URL = "http://www.theseedok.com/api";
	private JSONObject routeCoordinates;
	
	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View android = inflater
				.inflate(R.layout.mainmap_frag, container, false);

		initializeMap();

		return android;
	}

	private void initializeMap() {
		
		gmap = ((SupportMapFragment) this.getFragmentManager().findFragmentById(R.id.fgmapMain)).getMap();
		gmap.setMyLocationEnabled(true);
		
		LocationManager locationManager = (LocationManager) this.getActivity().getSystemService(this.getActivity().LOCATION_SERVICE);
		Criteria crit = new Criteria();
		crit.setAccuracy(Criteria.ACCURACY_FINE);
		String provider = locationManager.getBestProvider(crit, true);
		Location location = locationManager.getLastKnownLocation(provider);
		
		if (location != null) {
			onLocationChanged(location);
		}
		locationManager.requestLocationUpdates(provider, 20000, 0, this);
		
		//adding the initial marker
		MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude()));
		markerOptions.title("You are here!");
		Marker locationMarker = gmap.addMarker(markerOptions);
		locationMarker.showInfoWindow();
		
		
		
	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
		
		gmap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
		gmap.animateCamera(CameraUpdateFactory.zoomTo(14));
	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	
	
	private class QueryTask extends AsyncTask<String, Void, ArrayList<LatLng>> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected ArrayList<LatLng> doInBackground(String... args) {
			try {
				routeCoordinates = getJSONObjectForURL("/data/allstops");
				return parseJSONObject(routeCoordinates);
			} catch (Exception e) {
				return null;
			}
		}

		@Override
		protected void onPostExecute(ArrayList<LatLng> result) {
			super.onPostExecute(result);
			
			
		}
	}
	

	
	private ArrayList<LatLng> parseJSONObject(JSONObject dict) {
		// list of LatLng of the selected route.
		ArrayList<LatLng> list = new ArrayList<LatLng>();
		if (dict != null) {
	        Iterator<?> keys = dict.keys();
	        
	        while( keys.hasNext() ){
	            String key = (String) keys.next();
	            try {
					if( dict.get(key) instanceof JSONObject ){
						JSONArray array = (JSONArray) dict.get(key);
						LatLng stopLocation = new LatLng(array.getDouble(0), array.getDouble(1));
						list.add(stopLocation); 
					} 
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
			
		}
		return list;
	}
	
	/**
	 * Gets JSON from the server at "BASE_URL + apiURL"
	 * 
	 * @param apiURL
	 * @return
	 */
	private JSONObject getJSONObjectForURL(String apiURL) {
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
	
	
	
	

}

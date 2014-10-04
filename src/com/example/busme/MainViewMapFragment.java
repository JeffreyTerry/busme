package com.example.busme;

import java.util.HashMap;

import org.json.JSONObject;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
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
import com.google.android.gms.maps.model.MarkerOptions;

public class MainViewMapFragment extends Fragment implements LocationListener {

	GoogleMap gmap;
	FragmentTransaction fragmentTransaction;
	private JSONObject routeCoordinates;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View android = inflater
				.inflate(R.layout.mainmap_frag, container, false);

		initializeMap();

		return android;
	}

	private void initializeMap() {

		gmap = ((SupportMapFragment) this.getFragmentManager()
				.findFragmentById(R.id.fgmapMain)).getMap();
		// gmap.setMyLocationEnabled(true);

		LocationManager locationManager = (LocationManager) this.getActivity()
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

		new QueryTask().execute();

	}

	@Override
	public void onLocationChanged(Location location) {
		LatLng latLng = new LatLng(location.getLatitude(),
				location.getLongitude());

		gmap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
		gmap.animateCamera(CameraUpdateFactory.zoomTo(17));
	}

	@Override
	public void onProviderDisabled(String arg0) {
	}

	@Override
	public void onProviderEnabled(String arg0) {
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	private class QueryTask extends
			AsyncTask<String, Void, HashMap<String, LatLng>> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected HashMap<String, LatLng> doInBackground(String... args) {
			try {
				String stopData = MainModel.getStopToLatLngDictionaryData();
				if (stopData != null) {
					routeCoordinates = new JSONObject(stopData);
				} else {
					routeCoordinates = MainModel
							.getJSONObjectForURL(MainModel.BASE_URL
									+ "/data/stops/dictionary/latlngs");
					MainModel.saveStopToLatLngDictionaryData(routeCoordinates
							.toString());
				}
				return JSONConverter
						.convertStopLatLngsToHashMap(routeCoordinates);
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(HashMap<String, LatLng> result) {
			super.onPostExecute(result);
			if (result == null) {
				return;
			}
			for (String key : result.keySet()) {
				// adding the initial marker
				MarkerOptions markerOptions = new MarkerOptions()
						.position(new LatLng(result.get(key).latitude, result
								.get(key).longitude));
				// .icon(BitmapDescriptorFactory
				// .defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
				markerOptions.title(key);

				gmap.addMarker(markerOptions);
			}
		}
	}
}

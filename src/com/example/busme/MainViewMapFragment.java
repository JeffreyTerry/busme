package com.example.busme;

import java.util.ArrayList;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainViewMapFragment extends Fragment implements LocationListener {
	private GoogleMap gmap;
	private JSONObject stopCoordinates;
	private MainModel mainModel;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View android = inflater
				.inflate(R.layout.mainmap_frag, container, false);
		
		mainModel = ((MainActivity) this.getActivity()).getMainController().getMainModel();

		initializeMap();

		return android;
	}

	private void initializeMap() {
		gmap = ((SupportMapFragment) this.getFragmentManager()
				.findFragmentById(R.id.fgmapMain)).getMap();
	}

	@Override
	public void onLocationChanged(Location location) {
		LatLng latLng = new LatLng(location.getLatitude(),
				location.getLongitude());

		gmap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
		gmap.animateCamera(CameraUpdateFactory.zoomTo(16));
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
		gmap.setMyLocationEnabled(true);
		initializeStopCoordinates();
		updateLocation();
	}

	@Override
	public void onPause() {
		super.onPause();
		gmap.setMyLocationEnabled(false);
	}

	public void initializeStopCoordinates() {
		if (stopCoordinates == null) {
			new LoadStopCoordinatesTask().execute();
		}
	}

	public void updateLocation() {
		LocationManager locationManager = (LocationManager) MainViewMapFragment.this
				.getActivity().getSystemService(Context.LOCATION_SERVICE);
		Criteria crit = new Criteria();
		crit.setAccuracy(Criteria.ACCURACY_FINE);
		String provider = locationManager.getBestProvider(crit, true);
		Location location = locationManager.getLastKnownLocation(provider);

		if (location != null) {
			onLocationChanged(location);
		}
		locationManager.requestSingleUpdate(provider,
				MainViewMapFragment.this, Looper.getMainLooper());
	}

	private class LoadStopCoordinatesTask extends
			AsyncTask<String, Void, ArrayList<MarkerOptions>> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected ArrayList<MarkerOptions> doInBackground(String... args) {
			try {
				String stopData = mainModel.getStopToLatLngDictionaryData();
				if (stopData != null) {
					stopCoordinates = new JSONObject(stopData);
				} else {
					stopCoordinates = MainModel
							.getJSONObjectForURL(MainModel.BASE_URL
									+ "/data/stops/dictionary/latlngs");
					mainModel.saveStopToLatLngDictionaryData(stopCoordinates
							.toString());
				}
				HashMap<String, LatLng> result = JSONConverter
						.convertStopLatLngsToHashMap(stopCoordinates);
				if (result == null) {
					return null;
				}
				ArrayList<String> stopNames = new ArrayList<String>(result.keySet());
				MarkerOptions nextMarkerOptions;
				ArrayList<MarkerOptions> allMarkerOptions = new ArrayList<MarkerOptions>();
				for (int i = 0; i < stopNames.size(); i++) {
					nextMarkerOptions = new MarkerOptions();
					nextMarkerOptions.position(new LatLng(result.get(stopNames.get(i)).latitude,
							result.get(stopNames.get(i)).longitude));
					// .icon(BitmapDescriptorFactory
					// .defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
					nextMarkerOptions.title(stopNames.get(i));
					allMarkerOptions.add(nextMarkerOptions);
				}
				return allMarkerOptions;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(ArrayList<MarkerOptions> result) {
			super.onPostExecute(result);
			if(result == null) {
				return;
			} else {
				for(int i = 0; i < result.size(); i++){
					gmap.addMarker(result.get(i));
				}
			}
		}
	}
}

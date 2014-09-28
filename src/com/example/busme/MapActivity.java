package com.example.busme;

import android.app.Activity;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;

public class MapActivity extends Activity implements LocationListener {

	private GoogleMap gmap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.map_activity);

		initializeMapFragment();
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
        
        busNum.setTypeface(Bebas);
        arrivalTime.setTypeface(Exo);
        
        
	}

	private void initializeMapFragment() {
		// TODO Auto-generated method stub
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

	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		LatLng latLng = new LatLng(location.getLatitude(),
				location.getLongitude());

		gmap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
		gmap.animateCamera(CameraUpdateFactory.zoomTo(20));
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

}

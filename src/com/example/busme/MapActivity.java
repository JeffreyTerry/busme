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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapActivity extends Activity implements LocationListener {

	private GoogleMap gmap;
	private Bundle extras;

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
		
		
		System.out.println("dude lat" + extras.getDouble("startLat"));
		
		//adding start & destination markers
		MarkerOptions startOptions = new MarkerOptions().position(new LatLng(
				extras.getDouble("startLat"),
				extras.getDouble("startLng")));
		
		MarkerOptions endOptions = new MarkerOptions().position(new LatLng(
				extras.getDouble("destLat"),
				extras.getDouble("destLng")));
		
		
		
		startOptions.title(extras.getString("destination"));
		endOptions.title(extras.getString("start"));
		Marker startMarker = gmap.addMarker(startOptions);
		Marker destMarker = gmap.addMarker(endOptions);
		startMarker.showInfoWindow();
		destMarker.showInfoWindow();
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

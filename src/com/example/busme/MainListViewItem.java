package com.example.busme;

public class MainListViewItem {
	public static final MainListViewItem NULL_ITEM = new MainListViewItem(-1, -1, "", "", -1, -1, -1, -1, "");
	private int time;
	private int routeNumber;
	private double start_lat;
	private double start_lng;
	private double dest_lat;
	private double dest_lng;
	private String routeStart;
	private String routeDestination;
	private String travel_time;

	public MainListViewItem(int t, int route, String start, String destination, double start_lat, double start_lng, double dest_lat, double dest_lng, String travel_time) {
		time = t;
		routeNumber = route;
		routeStart = start;
		routeDestination = destination;
		this.start_lat = start_lat;
		this.start_lng = start_lng;
		this.dest_lat = dest_lat;
		this.dest_lng = dest_lng;
		this.travel_time = travel_time;
	}

	public int getTime() {
		return time;
	}

	public int getRouteNumber() {
		return routeNumber;
	}
	
	public double getStartLat() {
		return start_lat;
	}
	
	public double getStartLng() {
		return start_lng;
	}
	
	public double getDestLat() {
		return dest_lat;
	}
	
	public double getDestLng() {
		return dest_lng;
	}
	
	
	public String getRouteStart() {
		return routeStart;
	}

	public String getRouteDestination() {
		return routeDestination;
	}
	
	public String getTravelTime() {
		return travel_time;
	}
}

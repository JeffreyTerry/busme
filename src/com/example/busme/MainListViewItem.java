package com.example.busme;

public class MainListViewItem {
	private int time;
	private int routeNumber;
	private String routeStart;
	private String routeDestination;

	public MainListViewItem(int t, int route, String start, String destination) {
		time = t;
		routeNumber = route;
		routeStart = start;
		routeDestination = destination;
	}

	public int getTime() {
		return time;
	}

	public int getRouteNumber() {
		return routeNumber;
	}

	public String getRouteStart() {
		return routeStart;
	}

	public String getRouteDestination() {
		return routeDestination;
	}
}

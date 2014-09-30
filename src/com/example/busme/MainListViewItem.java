package com.example.busme;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.util.Log;

public class MainListViewItem {
	public static final MainListViewItem NULL_ITEM = new MainListViewItem(
			"1:00 PM", -1, "", "", -1, -1, -1, -1, "");
	private String nextBusTime;
	private int routeNumber;
	private double start_lat;
	private double start_lng;
	private double dest_lat;
	private double dest_lng;
	private String routeStart;
	private String routeDestination;
	private String travel_time;
	private SimpleDateFormat dateFormatter;
	private Calendar calendar;

	public MainListViewItem(String nextBusTime, int routeNumber, String start,
			String destination, double start_lat, double start_lng,
			double dest_lat, double dest_lng, String travel_time) {
		this.nextBusTime = nextBusTime;
		this.routeNumber = routeNumber;
		routeStart = start;
		routeDestination = destination;
		this.start_lat = start_lat;
		this.start_lng = start_lng;
		this.dest_lat = dest_lat;
		this.dest_lng = dest_lng;
		this.travel_time = travel_time;

		TimeZone tz = TimeZone.getTimeZone("GMT+05:00");
		calendar = Calendar.getInstance(tz);
		dateFormatter = new SimpleDateFormat("HH:mm a", Locale.US);
	}

	public long getMinutesUntilNextBus() {
		try {
			Log.d("time", nextBusTime);
			Date nextBusDate = dateFormatter.parse(nextBusTime);
			Date now = calendar.getTime();
			long timeUntilNextBus = nextBusDate.getTime() - now.getTime();
			return timeUntilNextBus / 1000;
		} catch (ParseException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public String getNextBusTime() {
		return nextBusTime;
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

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
			"01:00 PM", -1, "", "", -1, -1, -1, -1, "");
	private String nextBusTimeString;
	private int routeNumber;
	private double start_lat;
	private double start_lng;
	private double dest_lat;
	private double dest_lng;
	private String routeStart;
	private String routeDestination;
	private String travel_time;
	private SimpleDateFormat dateFormatter;
	private Calendar now, nextBusDate;
	private TimeZone easternTime;

	/**
	 * 
	 * @param nextBusTimeString  Formatted as "01:00 PM"
	 * @param routeNumber
	 * @param start
	 * @param destination
	 * @param start_lat
	 * @param start_lng
	 * @param dest_lat
	 * @param dest_lng
	 * @param travel_time
	 */
	public MainListViewItem(String nextBusTimeString, int routeNumber, String start,
			String destination, double start_lat, double start_lng,
			double dest_lat, double dest_lng, String travel_time) {
		this.nextBusTimeString = nextBusTimeString;
		this.routeNumber = routeNumber;
		routeStart = start;
		routeDestination = destination;
		this.start_lat = start_lat;
		this.start_lng = start_lng;
		this.dest_lat = dest_lat;
		this.dest_lng = dest_lng;
		this.travel_time = travel_time;

		easternTime = TimeZone.getTimeZone("GMT+05:00");
		nextBusDate = Calendar.getInstance(easternTime);
		dateFormatter = new SimpleDateFormat("HH:mm a", Locale.US);
	}

	public long getMinutesUntilNextBus() {
		try {
			nextBusDate.setTime(dateFormatter.parse(nextBusTimeString));
			now = Calendar.getInstance(easternTime);
			int hoursNext = nextBusDate.get(Calendar.HOUR_OF_DAY);
			int hoursNow = now.get(Calendar.HOUR_OF_DAY);
			// if the next bus is coming tomorrow
			if(hoursNow > 12 && hoursNext < 12) {
				hoursNext += 24;
			}
			int minutesNext = nextBusDate.get(Calendar.MINUTE);
			int minutesNow = now.get(Calendar.MINUTE);
			
			return (hoursNext - hoursNow) * 60 + (minutesNext - minutesNow);
		} catch (ParseException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public String getNextBusTime() {
		return nextBusTimeString;
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

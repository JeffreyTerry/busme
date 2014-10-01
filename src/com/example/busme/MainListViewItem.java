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
	private Calendar now;
	private TimeZone easternTime;
	private int hoursNext, minutesNext;
	private String amOrPm;
	

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

		easternTime = TimeZone.getTimeZone("GMT+20:00");
		try {
			hoursNext = Integer.parseInt(nextBusTimeString.substring(0, 2)) % 12;
			minutesNext = Integer.parseInt(nextBusTimeString.substring(3, 5));
			amOrPm = nextBusTimeString.substring(nextBusTimeString.length() - 2);
		} catch(Exception e){
			hoursNext = 0;
			minutesNext = 0;
			amOrPm = "AM";
		}
	}

	public long getMinutesUntilNextBus() {
		now = Calendar.getInstance(easternTime);
		int hoursNow = now.get(Calendar.HOUR) % 12;
		// if the next bus is coming tomorrow
		if((amOrPm.contentEquals("AM") && now.get(Calendar.AM_PM) == Calendar.PM) || (amOrPm.contentEquals("PM") && now.get(Calendar.AM_PM) == Calendar.AM)) {
			if(hoursNext < 12){
				hoursNext += 12;
			}
		}
		
		int minutesNow = now.get(Calendar.MINUTE);

		Log.d("now", now.toString());
		Log.d("hours next", "" + hoursNext);
		Log.d("hours now", "" + hoursNow);
		Log.d("minutes next", "" + minutesNext);
		Log.d("minutes now", "" + minutesNow);
		Log.d("am or pm", amOrPm);
		Log.d("fuck", ""+now.get(Calendar.AM_PM));
		Log.d("fuck", ""+Calendar.AM);
		Log.d("fuck", ""+Calendar.PM);
			
		return (hoursNext - hoursNow) * 60 + (minutesNext - minutesNow);
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

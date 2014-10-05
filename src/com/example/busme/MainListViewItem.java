package com.example.busme;

import java.util.Calendar;
import java.util.TimeZone;

public class MainListViewItem {
	public static final MainListViewItem NULL_ITEM = new MainListViewItem(
			"01:00 PM", "169", "nowhere", "nowhere", -1, -1, -1, -1, "0");
	public static final MainListViewItem NO_ROUTE_FOUND_ERROR_ITEM = new MainListViewItem(
			"01:00 PM", "170", "no routes found", "no routes found", -1, -1, -1, -1, "0");
	public static final MainListViewItem DATA_PARSE_ERROR_ITEM = new MainListViewItem(
			"01:00 PM", "171", "data parse error", "data parse error", -1, -1, -1, -1, "0");
	public static final MainListViewItem STOP_DATA_MISSING_ERROR_ITEM = new MainListViewItem(
			"01:00 PM", "172", "stop data missing", "stop data missing", -1, -1, -1, -1, "0");
	public static final String TRAVEL_TIME_UNKNOWN = "0";
	public static final double NULL_LATITUDE = 0;
	public static final double NULL_LONGITUDE = 0;
	private String nextBusTimeString;
	private int[] routeNumbers;
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
	public MainListViewItem(String nextBusTimeString, String routeNumberCSV, String start,
			String destination, double start_lat, double start_lng,
			double dest_lat, double dest_lng, String travel_time) {
		this.nextBusTimeString = nextBusTimeString;
		String[] routeNumberStrings = routeNumberCSV.split(",");
		if(routeNumberStrings.length == 0) {
			this.routeNumbers = new int[1];
			this.routeNumbers[0] = 0;
		} else {
			this.routeNumbers = new int[routeNumberStrings.length];
			for(int i = 0; i < routeNumberStrings.length; i++) {
				routeNumbers[i] = Integer.parseInt(routeNumberStrings[i]);
			}
		}
		routeStart = start;
		routeDestination = destination;
		this.start_lat = start_lat;
		this.start_lng = start_lng;
		this.dest_lat = dest_lat;
		this.dest_lng = dest_lng;
		this.travel_time = travel_time;

		easternTime = TimeZone.getTimeZone("GMT-4:00");
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
		return (hoursNext - hoursNow) * 60 + (minutesNext - minutesNow);
	}

	public String getNextBusTime() {
		return nextBusTimeString;
	}

	public int[] getRouteNumbers() {
		return routeNumbers;
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

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof MainListViewItem)) {
			return false;
		}
		
		MainListViewItem otherItem = (MainListViewItem) o;
		if(routeNumbers.length != otherItem.routeNumbers.length) {
			return false;
		}
		boolean routeNumbersAreEqual = true;
		for(int i = 0; i < routeNumbers.length; i++){
			if(routeNumbers[i] != otherItem.routeNumbers[i]){
				routeNumbersAreEqual = false;
			}
		}
		return nextBusTimeString.contentEquals(otherItem.nextBusTimeString) && routeNumbersAreEqual;
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + nextBusTimeString.hashCode();
		for(int i = 0; i < routeNumbers.length; i++) {
			result = 31 * result + routeNumbers[i];
		}
		return result;
	}
	
	
}

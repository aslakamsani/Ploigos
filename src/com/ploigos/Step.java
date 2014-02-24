package com.ploigos;

// co-ordinates and instruction for each leg of the journey
public class Step {
	private double lat;
	private double lng;
	private String instruct;
	
	public Step(double lat, double lng, String instruct){
		this.lat = lat;
		this.lng = lng;
		this.instruct = instruct;
	}
	
	public Step(String lat, String lng, String instruct){
		this.lat = Double.parseDouble(lat);
		this.lng = Double.parseDouble(lng);
		this.instruct = instruct;
	}
	
	public double getLat() {
		return lat;
	}
	
	public double getLng() {
		return lng;
	}
	
	public String getInstruct() {
		return instruct;
	}
}

package com.ploigos;

import java.util.ArrayList;

// contains co-ordinates and instructions for a destination
public class ArrayListData {
	ArrayList<String> latitude;
	ArrayList<String> longitude;
	ArrayList<String> instructions;
	
	public ArrayListData(){
		
	}
	
	public ArrayListData(ArrayList<String> lat, ArrayList<String> lng,
						 ArrayList<String> instruct){
		latitude = lat;
		longitude = lng;
		instructions = instruct;
	}
	
	public ArrayList<String> getLatitude(){
		return latitude;
	}
	
	public ArrayList<String> getLongitude(){
		return longitude;
	}
	
	public ArrayList<String> getInstructions(){
		return instructions;
	}
}	

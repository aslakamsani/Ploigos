package com.ploigos;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.twotoasters.jazzylistview.JazzyHelper;
import com.twotoasters.jazzylistview.JazzyListView;

public class LocationList extends Activity {
	JazzyListView mList;
	
	// information about waypoints
	ArrayList<String> names = new ArrayList();
	ArrayList<String> latitude = new ArrayList();
	ArrayList<String> longitude = new ArrayList();
	
	private int mCurrentTransitionEffect = JazzyHelper.WAVE;
	private static final String KEY_TRANSITION_EFFECT = "transition_effect";
	SharedPreferences data;
	SharedPreferences.Editor editor;
	String strData;
	private static final String key = "MySharedData";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// set up list with co-ordinates and names
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list);
		data = getSharedPreferences(key, 0);
		strData = data.getString("data", null);
		if (strData != null) {

		String p = "(name:)([-A-Za-z0-9\\s]{1,})[^(lat)]";
		String p2 = "(lat:)([-A-Za-z0-9\\.]{1,})[^(long)]";
		String p3 = "(long:)([-A-Za-z0-9\\.]{1,})[^(name)]";

		Pattern pattern1 = Pattern.compile(p);
		Pattern pattern2 = Pattern.compile(p2);
		Pattern pattern3 = Pattern.compile(p3);

		Matcher m = pattern1.matcher(strData);
		Matcher m2 = pattern2.matcher(strData);
		Matcher m3 = pattern3.matcher(strData);

		while (m.find()) {
			names.add(m.group(2));
		}
		while (m2.find()) {
			latitude.add(m2.group(2));
		}
		while (m3.find()) {
			longitude.add(m3.group(2));
		}

		// create list
		mList = (JazzyListView) findViewById(android.R.id.list);
		mList.setAdapter(new ListAdapter(this, R.layout.row, names,latitude,longitude));
		setupJazziness(mCurrentTransitionEffect);
		}
	}

	private void setupJazziness(int effect) {
		mCurrentTransitionEffect = effect;
		mList.setTransitionEffect(mCurrentTransitionEffect);
	}

}

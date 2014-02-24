package com.ploigos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.metaio.sdk.MetaioDebug;
import com.metaio.tools.io.AssetsManager;

public class MainActivity extends Activity implements OnClickListener {

	AssetsExtracter task;

	Button way; // save a waypoint here
	Button navigate; // go to a waypoint
	Button custom; // save an address as a waypoint
	
	Intent wayActivity;
	Intent navigateActivity;
	EditText input2;
	EditText input;
	Button submit;
	Dialog dialog;

	SharedPreferences data;
	SharedPreferences.Editor editor;
	String strData;
	public static final boolean NATIVE = true;

	private static final String key = "MySharedData";
	LocationManager manager;
	Typeface tf;

	// set up starting screen
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		tf = Typeface.createFromAsset(getAssets(), "font/hnt.otf");

		way = (Button) findViewById(R.id.b1);
		navigate = (Button) findViewById(R.id.b3);
		custom = (Button) findViewById(R.id.b2);

		way.setTypeface(tf);
		custom.setTypeface(tf);
		navigate.setTypeface(tf);
		way.setOnClickListener(this);
		navigate.setOnClickListener(this);
		custom.setOnClickListener(this);

		navigateActivity = new Intent(this, LocationList.class);

		MetaioDebug.enableLogging(BuildConfig.DEBUG);

		task = new AssetsExtracter();
		task.execute(0);
	}

	@Override
	public void onClick(View arg0) {
		switch (arg0.getId()) {
		// to create a waypoint here
		case R.id.b1:
			dialog = new Dialog(this, R.style.myBackgroundStyle);
			dialog.setContentView(R.layout.dialog);
			dialog.setTitle("Set Way Point");

			dialog.show();

			input = (EditText) dialog.findViewById(R.id.input);
			submit = (Button) dialog.findViewById(R.id.confirm);
			submit.setTypeface(tf);

			submit.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// get a name and then save the current location
					if (input.getText().toString() != null
							&& !input.getText().toString().equals("")) {
						manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
						Location location = manager
								.getLastKnownLocation(LocationManager.GPS_PROVIDER);

						data = getSharedPreferences(key, 0);
						strData = data.getString("data", null);

						String title = input.getText().toString();

						editor = data.edit();
						String append = "name:" + title + ";lat:"
								+ location.getLatitude() + ";long:"
								+ location.getLongitude() + ";";
						editor.putString("data", strData + append);
						editor.commit();
					}
					dialog.hide();
				}
			});
			break;
			
		// to save an address for a waypoint
		case R.id.b2:
			dialog = new Dialog(this, R.style.myBackgroundStyle);
			dialog.setContentView(R.layout.customdialog);
			dialog.setTitle("Set Way Point");
			dialog.show();
			input = (EditText) dialog.findViewById(R.id.input);
			input2 = (EditText) dialog.findViewById(R.id.input2);

			submit = (Button) dialog.findViewById(R.id.confirm);
			submit.setTypeface(tf);
			submit.setOnClickListener(new OnClickListener() {
				@Override
				// get a name and address, talk to Google Maps to convert to co-ordinates,
				// and save the waypoint
				public void onClick(View v) {
					if (input.getText().toString() != null
							&& !input.getText().toString().equals("")
							&& input2.getText().toString() != null
							&& !input2.getText().toString().equals("")) {
						data = getSharedPreferences(key, 0);
						strData = data.getString("data", null);
						String title = input.getText().toString();
						String address = input2.getText().toString();
						String result = address.replace(' ', '+');
						String[] a = new String[3];
						a[0] = "http://maps.googleapis.com/maps/api/geocode/json?address="
								+ result + "&sensor=true";
						a[1] = title;
						a[2] = result;
						new ConvertAddressCoordinates().execute(a);
					}
				}
			});
			break;
			
		// to navigate, start up the camera
		case R.id.b3:
			// skips saved waypoints for testing, goes to hardcoded point
			// TODO: need to start up list here to chose from waypoints
			startActivity(navigateActivity);
			break;
		}
	}

	// talks to Google Maps to convert an address to GPS co-ordinates
	class ConvertAddressCoordinates extends AsyncTask<String, Void, String> {
		InputStream is;
		String json;
		String append;

		@Override
		protected String doInBackground(String... params) {
			try {
				DefaultHttpClient httpClient = new DefaultHttpClient();
				HttpPost httpPost = new HttpPost(params[0]);
				HttpResponse httpResponse = httpClient.execute(httpPost);
				HttpEntity httpEntity = httpResponse.getEntity();
				is = httpEntity.getContent();

			} catch (UnsupportedEncodingException e) {
			} catch (ClientProtocolException e) {
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is, "utf8"), 8);
				StringBuilder sb = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
				is.close();
				
				// parse JSON to determine co-ordinates
				json = sb.toString();
				JSONObject total = new JSONObject(json);
				JSONArray results = total.getJSONArray("results");
				JSONObject ob = results.getJSONObject(0);
				JSONObject geo = ob.getJSONObject("geometry");
				JSONObject location = geo.getJSONObject("location");
				String lat = location.getString("lat");
				String lng = location.getString("lng");
				append = "name:" + params[1] + ";lat:" + lat + ";long:" + lng
						+ ";";
			} catch (Exception e) {
				e.printStackTrace();
			}
			return append;

		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			editor = data.edit();
			editor.putString("data", strData + append);
			editor.commit();
			dialog.hide();

		}
	}

	/**
	 * This task extracts all the assets to an external or internal location to
	 * make them accessible to metaio SDK
	 */
	private class AssetsExtracter extends AsyncTask<Integer, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Integer... params) {
			try {
				// Extract all assets and overwrite existing files if debug
				// build
				AssetsManager.extractAllAssets(getApplicationContext(), true);
			} catch (IOException e) {
				MetaioDebug.printStackTrace(Log.ERROR, e);
				return false;
			}

			return true;
		}

		
	}
}
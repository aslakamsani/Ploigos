package com.ploigos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

class ListAdapter extends ArrayAdapter<String> {

	private final LayoutInflater inflater;
	private final Resources res;
	private final int itemLayout;
	
	// holding information about waypoint
	private ArrayList<String> name = new ArrayList<String>();
	private ArrayList<String> latitude = new ArrayList<String>();
	private ArrayList<String> longitude = new ArrayList<String>();
		
	private Intent navigation;
	private Context c;
	ArrayList<Step> mapSteps = new ArrayList<Step>();
	LocationManager manager;
	
	// for talking to Google Maps
	private static final String GMAPS_SRC = "http://maps.googleapis.com/maps/api/directions/json?origin=";
	private static final String GMAPS_DEST = "&destination=";
	private static final String GMAPS_OPTIONS = "&sensor=true&mode=walking";
	String serialLat = "";
	String serialLong = "";
	String serialInst = "";
	
	private Typeface tf; 

	public ListAdapter(Context context, int itemLayout,
			ArrayList<String> names, ArrayList<String> latitudes,
			ArrayList<String> longitudes) {
		super(context, itemLayout, R.id.rowContent, names);
		name = names;
		c = context;
		latitude = latitudes;
		longitude = longitudes;
		inflater = LayoutInflater.from(context);
		res = context.getResources();
		this.itemLayout = itemLayout;
		tf = Typeface.createFromAsset(context.getAssets(), "font/hnt.otf");

		navigation = new Intent(context, ARActivity.class);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			convertView = inflater.inflate(itemLayout, null);
			holder = new ViewHolder(convertView);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		// set name of waypoint
		holder.text.setTypeface(tf);
		holder.text.setText(name.get(position));
		final int pos = position;
		
		// if clicked, ask Google Maps for directions
		holder.text.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				manager = (LocationManager) c
						.getSystemService(Context.LOCATION_SERVICE);
				Location location = manager
						.getLastKnownLocation(LocationManager.GPS_PROVIDER);

				String url = GMAPS_SRC + location.getLatitude() + ","
						+ location.getLongitude() + GMAPS_DEST
						+ latitude.get(pos) + "," + longitude.get(pos)
						+ GMAPS_OPTIONS;
				navigation.putExtra("name", name.get(pos));
				navigation.putExtra("latitude", latitude.get(pos));
				navigation.putExtra("longitude", longitude.get(pos));
				new GMaps().execute(url);
			
			}
		});

		return convertView;
	}

	static class ViewHolder {
		final TextView text;

		ViewHolder(View view) {
			text = (TextView) view.findViewById(R.id.rowContent);
		}
	}

	// requests Google Maps for directions
	// TODO: Redundant code. After Thread issue is resolved, delete other
	// 		 GMaps tasks.
	class GMaps extends AsyncTask<String, Void, Void> {
		InputStream is;
		String json;

		@Override
		protected Void doInBackground(String... params) {
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
				
				// parse JSON for directions
				json = sb.toString();
				JSONObject total = new JSONObject(json);
				JSONArray routes = total.getJSONArray("routes");
				JSONObject ob = routes.getJSONObject(0);
				JSONArray legs = ob.getJSONArray("legs");
				JSONObject ob2 = legs.getJSONObject(0);
				JSONArray steps = ob2.getJSONArray("steps");
				for (int i = 0; i < steps.length(); i++) {
					JSONObject object = steps.getJSONObject(i);
					JSONObject end = object.getJSONObject("end_location");
					String ins = object.getString("html_instructions");
					String instruct = ins.replaceAll("<]^>]*>", "");

					mapSteps.add(new Step(end.getString("lat"), end
							.getString("lng"), instruct));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			for (int i = 0; i < mapSteps.size(); i++) {
				System.out.println(mapSteps.get(i).getLat());
				serialLat += "" + mapSteps.get(i).getLat() + ";";
				serialLong += "" + mapSteps.get(i).getLng() + ";";
				serialInst += "" + mapSteps.get(i).getInstruct() + ";";
			}

			// start serially showing directions
			navigation.putExtra("serialLat",serialLat);
			navigation.putExtra("serialLong",serialLong);
			navigation.putExtra("serialInst",serialInst);
			c.startActivity(navigation);
		}

	}
}

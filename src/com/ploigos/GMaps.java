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

import android.os.AsyncTask;

// Google Maps parsing moved here for ease.
// TODO: Redundant code in ARActivity.java needs to be deleted.
public class GMaps extends AsyncTask<String, Void, Void> {
	InputStream is;
	String json;
	ArrayList<String> latitude = new ArrayList<String>();
	ArrayList<String> longitude = new ArrayList<String>();
	ArrayList<String> instructions = new ArrayList<String>();
	
	@Override
	protected Void doInBackground(String... params) {
		// call to Google Maps
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
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					is, "utf8"), 8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
			
			// Parse JSON for directions to destination
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
				String answer = ins.replaceAll("<]^>]*>", "");
				latitude.add(end.getString("lat"));
				longitude.add(end.getString("lng"));
				instructions.add(answer);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		new ArrayListData(latitude, longitude, instructions);
	}
	
}

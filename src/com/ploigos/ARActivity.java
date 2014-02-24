package com.ploigos;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.IRadar;
import com.metaio.sdk.jni.LLACoordinate;
import com.metaio.tools.io.AssetsManager;

public class ARActivity extends ARViewActivity implements Callback {

	private static final String GMAPS_SRC = "http://maps.googleapis.com/maps/api/directions/json?origin=";
	private static final String GMAPS_DEST = "&destination=";
	private static final String GMAPS_OPTIONS = "&sensor=true&mode=walking";
	private static final int DELAY = 5000; // five seconds
	private static final double LAT_THRESH = 0.00009;
	private static final double LNG_THRESH = 0.00009;
	Thread thread;
	Handler handle;
	IRadar radar; // radar to show points in relation to user
	// IGeometry magNorth = null;

	IGeometry destination; // waypoint
	LLACoordinate destCoor; // co-ordinates for waypoint
	String destName; // name of waypoint

	LLACoordinate curCoor; // user's co-ordinates
	double curHeading; // compass bearing
	int index = 0;
	IGeometry notice;
	String serialLat; // latitude
	String serialLong; // longitude
	String serialInst; // instructions
	boolean mapsExecuted = false;
	ArrayList<Double> lats = new ArrayList();
	ArrayList<Double> lngs = new ArrayList();
	ArrayList<String> insts = new ArrayList();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent i = getIntent();
		// destName = i.getStringExtra("name");
		// double lat = Double.parseDouble(i.getStringExtra("latitude"));
		// double lng = Double.parseDouble(i.getStringExtra("longitude"));
		serialLat = i.getStringExtra("serialLat");
		serialLong = i.getStringExtra("serialLong");
		serialInst = i.getStringExtra("serialInst");
		
		// take in all instructions and co-ordinates from Google Maps
		String [] latArray = serialLat.split(";");
		String [] longArray = serialLong.split(";");
		String [] instsArray = serialInst.split(";");
		System.out.println(Arrays.toString(latArray));
		System.out.println(Arrays.toString(longArray));

		for (int a = 0; a <latArray.length;a++){
			lats.add(Double.parseDouble(latArray[a]));
			lngs.add(Double.parseDouble(longArray[a]));
			insts.add(instsArray[a]);
		}
		
		/* HARDCODED TEST CO-ORDINATE */
		destName = "DEST";
		double lat = 34.014838;
		double lng = -118.493015;
		destCoor = new LLACoordinate(lat, lng, 0, 0);
		
		handle = new Handler(this);
		// Set GPS tracking configuration
		// The GPS tracking configuration must be set on user-interface thread
		boolean result = metaioSDK.setTrackingConfiguration("GPS");
		MetaioDebug.log("Tracking data loaded: " + result);
	}

	@Override
	protected int getGUILayout() {
		// activity this is attached to
		return R.layout.activity_ar;
	}

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler() {
		return null;
	}

	@Override
	protected void loadContents() {
		try {
			metaioSDK.setLLAObjectRenderingLimits(200, 500);

			String filepath = AssetsManager
					.getAssetPath("PloigosAssets/Assets/POI_bg.png");

			if (filepath != null) {
				// magnetic north
				// magNorth =
				// metaioSDK.createGeometryFromImage(createBillboardTexture("NORTH"),
				// true);
				// magNorth.setTranslationLLA(new LLACoordinate(83.95, -120.72,
				// 0 , 0));

				// load destination
				destination = metaioSDK.createGeometryFromImage(
						createBillboardTexture(destName), true);
				destination.setTranslationLLA(destCoor);
				destination.setScale(2);
				
				
			}

			// displays destination with user's FOV on a circle
			radar = metaioSDK.createRadar();
			radar.setBackgroundTexture(AssetsManager
					.getAssetPath("PloigosAssets/Assets/radar.png"));
			radar.setObjectsDefaultTexture(AssetsManager
					.getAssetPath("PloigosAssets/Assets/yellow.png"));
			// radar.setObjectTexture(magNorth,
			// AssetsManager.getAssetPath("PloigosAssets/Assets/red.png"));
			radar.setRelativeToScreen(IGeometry.ANCHOR_TL);

			// radar.add(magNorth);
			radar.add(destination);
//			navigate();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// TODO Add option for larger billboards for directions
	private String createBillboardTexture(String billBoardTitle) {
		try {
			final String texturepath = getCacheDir() + "/" + billBoardTitle
					+ ".png";
			Paint mPaint = new Paint();

			// Load background image (256x128), and make a mutable copy
			Bitmap billboard = null;

			// reading billboard background
			String filepath = AssetsManager
					.getAssetPath("PloigosAssets/Assets/POI_bg.png");
			Bitmap mBackgroundImage = BitmapFactory.decodeFile(filepath);

			billboard = mBackgroundImage.copy(Bitmap.Config.ARGB_8888, true);

			Canvas c = new Canvas(billboard);

			mPaint.setColor(Color.WHITE);
			mPaint.setTextSize(24);
			mPaint.setTypeface(Typeface.DEFAULT);

			float y = 40;
			float x = 30;

			// Draw POI name
			if (billBoardTitle.length() > 0) {
				String n = billBoardTitle.trim();

				final int maxWidth = 160;

				int i = mPaint.breakText(n, true, maxWidth, null);
				c.drawText(n.substring(0, i), x, y, mPaint);

				// Draw second line if valid
				if (i < n.length()) {
					n = n.substring(i);
					y += 20;
					i = mPaint.breakText(n, true, maxWidth, null);

					if (i < n.length()) {
						i = mPaint.breakText(n, true, maxWidth - 20, null);
						c.drawText(n.substring(0, i) + "...", x, y, mPaint);
					} else {
						c.drawText(n.substring(0, i), x, y, mPaint);
					}
				}

			}

			// writing file
			try {
				FileOutputStream out = new FileOutputStream(texturepath);
				billboard.compress(Bitmap.CompressFormat.PNG, 90, out);
				MetaioDebug.log("Texture file is saved to " + texturepath);
				return texturepath;
			} catch (Exception e) {
				MetaioDebug.log("Failed to save texture file");
				e.printStackTrace();
			}

			billboard.recycle();
			billboard = null;

		} catch (Exception e) {
			MetaioDebug.log("Error creating billboard texture: "
					+ e.getMessage());
			MetaioDebug.printStackTrace(Log.DEBUG, e);
			return null;
		}
		return null;
	}

	// show new waypoints as per instructions from Maps
	// TODO: Redundant code, need to resolve Thread error first
	private void navigate() {
		LLACoordinate stepCoor = new LLACoordinate(lats.get(0), lngs.get(0), 0, 0);
		if (stepCoor == null)
			System.out.println("STEPCOOR NULL");

		String ins = insts.get(0).toLowerCase();

		if (ins == null)
			System.out.println("INSTRUCT NULL");

		// custom, simpler instructions
		if (ins.contains("turn right"))
			ins = "Turn right here.";
		else if (ins.contains("turn left"))
			ins = "Turn left here.";
		else if (ins.contains("keep right") || ins.contains("slight right"))
			ins = "Keep right here.";
		else if (ins.contains("keep left") || ins.contains("slight left"))
			ins = "Keep left here.";
		else
			ins = "Come here.";

		System.out.println("POST INSTRUCT NULL");
		if (metaioSDK == null)
			System.out.println("METAIO NULL");

		notice = metaioSDK.createGeometryFromImage(createBillboardTexture(ins),
				true);
		notice.setScale(2);
		if (notice == null)
			System.out.println("NULL NOTICE");
		notice.setTranslationLLA(stepCoor);
		
		// add to radar
		radar.setObjectTexture(notice,
				AssetsManager.getAssetPath("PloigosAssets/Assets/yellow.png"));
		radar.add(notice);
	}

	// get instructions from Google Maps
	// TODO: Redundant code. Delete if possible. Class moved to seperate file.
	class GMaps extends AsyncTask<String, Void, Void> {
		InputStream is;
		String json;
		ArrayList<Step> mapSteps = new ArrayList<Step>();

		@Override
		protected Void doInBackground(String... params) {
			// talk to Google Maps
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
				
				// parse JSON to extract directions to destination
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
			navigate();
		}

	}

	// TODO: Redundant code. Resolve Thread issue. Handler may just complicate things.
	public boolean handleMessage(Message message) {
		index = message.getData().getInt("index");
		if (index < lats.size()){
			radar.remove(notice);
	
			LLACoordinate stepCoor = new LLACoordinate(lats.get(index),
					lngs.get(index), 0, 0);
			if (stepCoor == null)
				System.out.println("STEPCOOR NULL");
	
			String ins = insts.get(index).toLowerCase();
	
			if (ins == null)
				System.out.println("INSTRUCT NULL");
	
			// simpler instructions for waypoint
			if (ins.contains("turn right"))
				ins = "Turn right here.";
			else if (ins.contains("turn left"))
				ins = "Turn left here.";
			else if (ins.contains("keep right") || ins.contains("slight right"))
				ins = "Keep right here.";
			else if (ins.contains("keep left") || ins.contains("slight left"))
				ins = "Keep left here.";
			else
				ins = "Come here.";
	
			System.out.println("POST INSTRUCT NULL");
	
			notice = metaioSDK.createGeometryFromImage(createBillboardTexture(ins),
					true);
			if (notice == null)
				System.out.println("NULL NOTICE");
			notice.setTranslationLLA(stepCoor);
			
			// add to radar
			radar.setObjectTexture(notice,
					AssetsManager.getAssetPath("PloigosAssets/Assets/yellow.png"));
			radar.add(notice);
		}
		return true;
	}

	// loop to determine if user is close enough to a goal to create an new instruction
	// TODO: Resolve Thread issue. Waypoint cannot be created in a thread. Infinite loop
	//		 problem also. Threshold for user being close enough may be too specific. 
//	private void loop() {
//		thread = new Thread(new Runnable() {
//			public void run() {
//				int i = 0;
//				while (true) {
//					curCoor = mSensors.getLocation();
//					while (Math.abs(curCoor.getLatitude()
//							- lats.get(i)) > LAT_THRESH
//							&& Math.abs(curCoor.getLongitude()
//									- lngs.get(i)) > LNG_THRESH) {
//						long now = System.currentTimeMillis();
//						long later = now + DELAY;
//						while (later != System.currentTimeMillis()) {
//						}
//					}
//					i++;
//					Bundle bundle = new Bundle();
//					bundle.putInt("index", i);
//					Message message = new Message();
//					message.setData(bundle);
//					handle.sendMessage(message);
//				}
//			}
//		});
//		thread.start();
//
//	}

	@Override
	protected void onGeometryTouched(final IGeometry geometry) {
		MetaioDebug.log("Geometry selected: " + geometry);
		mSurfaceView.queueEvent(new Runnable() {
			@Override
			public void run() {
				if (geometry.equals(destination)) {
				}
			}
		});
	}
}

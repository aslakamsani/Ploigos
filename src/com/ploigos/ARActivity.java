package com.ploigos;

import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.IRadar;
import com.metaio.sdk.jni.LLACoordinate;
import com.metaio.tools.io.AssetsManager;

public class ARActivity extends ARViewActivity{
	
	IRadar radar;
	IGeometry wayPoint;
	LLACoordinate curLoc;
	double heading;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		// Set GPS tracking configuration
		// The GPS tracking configuration must be set on user-interface thread
		boolean result = metaioSDK.setTrackingConfiguration("GPS");  
		MetaioDebug.log("Tracking data loaded: " + result);
	}
	
	@Override
	protected int getGUILayout() {
		// TODO Auto-generated method stub
		
		// activity this is attached to
		return R.layout.activity_ar;
	}

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void loadContents() {		
		try{
			metaioSDK.setLLAObjectRenderingLimits(300, 400);
			
			String filepath = AssetsManager.getAssetPath("PloigosAssets/Assets/POI_bg.png");

			if(filepath != null){
				// load all waypoints from database that have not been disabled		
				wayPoint = metaioSDK.createGeometryFromImage(createBillboardTexture("UCSD"), true);
				LLACoordinate llaWayPoint = new LLACoordinate(32.877491, -117.235276, 0 , 0);
				wayPoint.setTranslationLLA(llaWayPoint);
			}
			
			radar = metaioSDK.createRadar();
			radar.setBackgroundTexture(AssetsManager.getAssetPath("PloigosAssets/Assets/radar.png"));
			radar.setObjectsDefaultTexture(AssetsManager.getAssetPath("PloigosAssets/Assets/yellow.png"));
			radar.setRelativeToScreen(IGeometry.ANCHOR_TL);
			
			
			radar.add(wayPoint);
			
//			curLoc = mSensors.getLocation();
//			heading = mSensors.getHeading();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private String createBillboardTexture(String billBoardTitle)
	{
		try
		{
			final String texturepath = getCacheDir() + "/" + billBoardTitle + ".png";
			Paint mPaint = new Paint();

			// Load background image (256x128), and make a mutable copy
			Bitmap billboard = null;

			//reading billboard background
			String filepath = AssetsManager.getAssetPath("PloigosAssets/Assets/POI_bg.png");
			Bitmap mBackgroundImage = BitmapFactory.decodeFile(filepath);

			billboard = mBackgroundImage.copy(Bitmap.Config.ARGB_8888, true);


			Canvas c = new Canvas(billboard);

			mPaint.setColor(Color.WHITE);
			mPaint.setTextSize(24);
			mPaint.setTypeface(Typeface.DEFAULT);

			float y = 40;
			float x = 30;

			// Draw POI name
			if (billBoardTitle.length() > 0)
			{
				String n = billBoardTitle.trim();

				final int maxWidth = 160;

				int i = mPaint.breakText(n, true, maxWidth, null);
				c.drawText(n.substring(0, i), x, y, mPaint);

				// Draw second line if valid
				if (i < n.length())
				{
					n = n.substring(i);
					y += 20;
					i = mPaint.breakText(n, true, maxWidth, null);

					if (i < n.length())
					{
						i = mPaint.breakText(n, true, maxWidth - 20, null);
						c.drawText(n.substring(0, i) + "...", x, y, mPaint);
					} else
					{
						c.drawText(n.substring(0, i), x, y, mPaint);
					}
				}

			}


			// writing file
			try
			{
				FileOutputStream out = new FileOutputStream(texturepath);
				billboard.compress(Bitmap.CompressFormat.PNG, 90, out);
				MetaioDebug.log("Texture file is saved to "+texturepath);
				return texturepath;
			} catch (Exception e) {
				MetaioDebug.log("Failed to save texture file");
				e.printStackTrace();
			}

			billboard.recycle();
			billboard = null;

		} catch (Exception e)
		{
			MetaioDebug.log("Error creating billboard texture: " + e.getMessage());
			MetaioDebug.printStackTrace(Log.DEBUG, e);
			return null;
		}
		return null;
	}
	
	@Override
	protected void onGeometryTouched(final IGeometry geometry) 
	{
		MetaioDebug.log("Geometry selected: "+geometry);
		
		mSurfaceView.queueEvent(new Runnable()
		{

			@Override
			public void run() 
			{
				radar.setObjectsDefaultTexture(AssetsManager.getAssetPath("TutorialLocationBasedAR/Assets/yellow.png"));
				radar.setObjectTexture(geometry, AssetsManager.getAssetPath("TutorialLocationBasedAR/Assets/red.png"));
			}
		
				
		});
	}
}

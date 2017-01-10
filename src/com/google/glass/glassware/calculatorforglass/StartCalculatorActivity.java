package com.google.glass.glassware.calculatorforglass;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.touchpad.GestureDetector.BaseListener;

public class StartCalculatorActivity extends Activity   implements BaseListener {

	public 		static		String	 			TAG 			= PerformCalculatorActivity.TAG;
	private 				Context				mContext		= null;
	private 				GestureDetector 	mGestureDetector;
	private 				AudioManager		mAudioManager 	= null;

	private final DialogInterface.OnClickListener mOnClickListener =
			new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int button) {
			startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
			finish();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;

		// Announce. 
		announce();

		// Initiate the audio manager.
		mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

		// Initiate the Gesture Detector.
		mGestureDetector = new GestureDetector(this);
		mGestureDetector.setBaseListener(this);

		// Check connectivity.
		if (!isNetworkConnected()) {
			mAudioManager.playSoundEffect(Sounds.ERROR);
			new CalculatorAlertDialog(mContext, 
					R.drawable.ic_cloud_sad_100,
					R.string.no_connectivity,
					R.string.no_connectivity_footnote,
					mOnClickListener).show();
			return;
		}

		// Start the speech recognizer and the calculator activity.
		Intent intent = new Intent("android.intent.action.PERFORM_CALCULATOR_ACTIVITY");
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(intent);
		finish();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (getIntent().getBooleanExtra("PLEASE_EXIT", false)) {   
			finish();
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event)  {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			onBackPressed();
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onBackPressed() {
		Log.i(TAG, "[INFO] OnBackPressed was called.");
		super.onBackPressed();
		exitGlassware();
	}

	/**
	 * Exit.
	 */
	private void exitGlassware() {
		Log.i(TAG, "[INFO] User selected to exit glassware.");
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.putExtra("PLEASE_EXIT", true);
		startActivity(intent);
		finish();
	}

	/**
	 * Announce.
	 */
	private void announce() {
		Log.i(TAG, "[INFO] *******************************************");
		Log.i(TAG, "[INFO] ***   Launched Calculator for Glassª    ***");
		Log.i(TAG, "[INFO] *******************************************");
	}

	/**
	 * Check if the network is connected. 
	 **/
	private boolean isNetworkConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni == null) {
			// There are no active networks.
			Log.e(TAG, "[ERROR] No network connectivity.");
			return false;
		} else
			Log.i(TAG, "[INFO] The device is connected to the network.");
		return true;
	}

	@Override
	public boolean onGesture(Gesture gesture) {
		if (gesture == Gesture.TAP) {
			Log.w(TAG, "[WARN] User tapped while launching calculator. Playing the Disallowed sound.");
			//Play the tap sound.
			mAudioManager.playSoundEffect(Sounds.DISALLOWED);
			return true;
		} else if (gesture == Gesture.TWO_TAP) {
			// do something on two finger tap
			Log.w(TAG, "[WARN] User tapped with two fingers while launching calculator. Playing the Disallowed sound.");
			mAudioManager.playSoundEffect(Sounds.DISALLOWED);
			return true;			
		} else if (gesture == Gesture.SWIPE_DOWN) {
			// do something on left (backwards) swipe
			Log.i(TAG, "[WARN] User Swiped down.");
			onBackPressed();
			return true;
		}
		return false;
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (mGestureDetector != null) {
			return mGestureDetector.onMotionEvent(event);
		}
		return false;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();	
	}
}

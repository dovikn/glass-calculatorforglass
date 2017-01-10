package com.google.glass.glassware.calculatorforglass;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;

/**
 * The Calculator Main Activity.
 */
public class CalculatorMenuActivity extends Activity {

	public 		static 				String 				TAG				= "calculatorforglass";
	public 		static 		final 	String 				LIVE_CARD_ID	= "calculatorforglass";
	public 		static 		final 	String 				EXTRA_FROM_TIMELINE = "from_timeline";
	private 						GestureDetector 	mGestureDetector;
	private 						AudioManager        mAudioManager 	= null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// Detect user input (in this case, only TAP is handled).
		mGestureDetector = createGestureDetector(this);

		// Init the Audio Manager.
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		// Open the option menu if this is started from a LiveCard.
		if (getIntent().getBooleanExtra(EXTRA_FROM_TIMELINE, false)) {
			Log.i(TAG,"[INFO] EXTRA_FROM_TIMELINE");
			mAudioManager.playSoundEffect(Sounds.TAP);
			openOptionsMenu();
		}
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		mAudioManager.playSoundEffect(Sounds.TAP);
		openOptionsMenu();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (getIntent().getBooleanExtra("PLEASE_EXIT", false)) {   
			finish();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.calculatorforglass_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.op_relaunch:
			relaunch();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		// Nothing to do here.
	}

	/**
	 * Re-launch.
	 */
	private void relaunch() {
		Intent intent = getBaseContext().getPackageManager().getLaunchIntentForPackage( getBaseContext().getPackageName() );
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);

	}

	/**
	 * Create Gesture detector. 
	 */
	private GestureDetector createGestureDetector(Context context) {
		GestureDetector gestureDetector = new GestureDetector(context);
		gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
			@Override
			public boolean onGesture(Gesture gesture) {
				if (gesture == Gesture.TAP) {
					mAudioManager.playSoundEffect(Sounds.TAP);
					openOptionsMenu();
					return true;
				} else if (gesture == Gesture.TWO_TAP) {
					return true;
				} else if (gesture == Gesture.SWIPE_RIGHT) {
					return true;
				} else if (gesture == Gesture.SWIPE_LEFT) {
					return true;
				}
				return false;
			}
		});
		gestureDetector.setFingerListener(new GestureDetector.FingerListener() {
			@Override
			public void onFingerCountChanged(int previousCount, int currentCount) {
				// do something on finger count changes
			}
		});
		gestureDetector.setScrollListener(new GestureDetector.ScrollListener() {
			@Override
			public boolean onScroll(float displacement, float delta, float velocity) {
				// do something on scrolling
				return true;
			}
		});
		return gestureDetector;
	}

	/*
	 * Send generic motion events to the gesture detector.
	 */
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
}
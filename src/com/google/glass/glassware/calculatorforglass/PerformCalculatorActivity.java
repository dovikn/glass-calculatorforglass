package com.google.glass.glassware.calculatorforglass;

import java.math.BigDecimal;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.widget.CardBuilder;

/**
 * Uploads the search query, downloads and displays the results. 
 * @author dovik
 */
public class PerformCalculatorActivity extends Activity{

	public static   		String 				TAG 				= CalculatorMenuActivity.TAG;
	private static final 	String 				LIVE_CARD_ID		= CalculatorMenuActivity.LIVE_CARD_ID;
	private 				LiveCard 			mLiveCard;
	private 				String 				mSpokenText 		= null;	
	private 				double 				mQueryTime			= 0;
	private 				GestureDetector 	mGestureDetector	= null;
	private 				AudioManager		mAudioManager 		= null;
	private 				TextToSpeech 		mSpeech				= null;
	private 				String				mDisplayedResult	= null;
	private static final 	int					MAX_DECIMAL_PLACES  = 2;
	private static final 	int 				SPEECH_REQUEST 		= 0;
	private static 			boolean				mAnswerDisplayed	= false;

	private final DialogInterface.OnClickListener mOnClickListener =
			new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int button) {
			startActivity(new Intent("android.intent.action.PERFORM_CALCULATOR_ACTIVITY"));
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		// Detect user input (in this case, only TAP is handled).
		mGestureDetector = createGestureDetector(this);

		// Init the Audio Manager.
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		// Init the LiveCard.
		initLiveCard();

		// Display speech recognizer.
		displaySpeechRecognizer();

		// Even though the text-to-speech engine is only used in response to a menu action, we
		// initialize it when the application starts so that we avoid delays that could occur
		// if we waited until it was needed to start it up.
		mSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				// Do nothing.
			}
		});
		mAnswerDisplayed = false;
	}

	/**
	 * Display speech recognizer.
	 */
	private void displaySpeechRecognizer() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		String prompt = this.getResources().getString(R.string.zero_value);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
		//intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		//intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
        //        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivityForResult(intent, SPEECH_REQUEST);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d(TAG,"[DEBUG] OnActivityResult()");
		if (requestCode == SPEECH_REQUEST && resultCode == RESULT_OK) {
			List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			mSpokenText = results.get(0);
			if (mSpokenText == null || mSpokenText.length() == 0) {
				mQueryTime = 0.0;
				Log.e(TAG, "[ERROR] the query is invalid.");
				mAudioManager.playSoundEffect(Sounds.ERROR);
				new CalculatorAlertDialog(this, 
						R.drawable.ic_warning_100,
						R.string.no_results,
						R.string.tap_to_calc_again,
						mOnClickListener).show();
				mAnswerDisplayed = true;
				return;
			}
		
			// Calculate.
			calculate();
		} else if (requestCode == SPEECH_REQUEST && resultCode == RESULT_CANCELED) {
			Log.w(TAG, "[WARN] Result Code was RESULT_CANCELED.");
			exitGlassware();
		} else {
			Log.w(TAG, "[WARN] Result code was neither RESULT_OK nor RESULT_CANCELED.");
			exitGlassware();
		}
	}

	/**
	 * Start Calculation.
	 */
	private void calculate () {

		// Use WolframAlpha API
		WolframAlphaSearchHelper waHelper = new WolframAlphaSearchHelper(mSpokenText);
		waHelper.runSearch();

		// Wait and display the result.
		waitAndDisplayTheCalculationResults(waHelper);
	}

	/**
	 * Sleep.
	 * */
	private static void calcSleep (long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			Log.e(TAG, "[ERROR] InterruptedException was thrown: " + e.getMessage()); 
		}
	}

	/**
	 * waitAndDisplayTheCalculationResults.
	 */
	private void waitAndDisplayTheCalculationResults(WolframAlphaSearchHelper helper) {

		String result = null;
		int i = 0;
		for (i= 0; i<5; i++) {
			result = helper.getResults();
			if (null == result) {
				// in the last iteration, if the result is still null. give up.
				if (i==4) {
					mQueryTime = helper.getCalculationTime();
					Log.w(TAG, "[WARN] No results, notifying the user .");
					mAudioManager.playSoundEffect(Sounds.ERROR);
					new CalculatorAlertDialog(this, 
							R.drawable.ic_warning_100,
							R.string.no_results,
							R.string.tap_to_calc_again,
							mOnClickListener).show();
					mAnswerDisplayed = true;
					return;
				}
				Log.i(TAG, "[INFO] Uploaded the Query, Waiting for the results ( iteration no. " + i);
				calcSleep(2000);	
				continue;
			} else {

				// remove "..." if they exist
				if (null != result && result.contains("...")) {
					result = result.substring(0, result.length() - 3);
				}

				try 
				{
					Long.parseLong(result);
					mDisplayedResult = result;

				} catch(NumberFormatException e)
				{	
					try
					{
						BigDecimal bc =  new BigDecimal(result);
						mDisplayedResult = bc.setScale(MAX_DECIMAL_PLACES,BigDecimal.ROUND_FLOOR).toString();
					}
					catch(NumberFormatException ex)
					{
						Log.w(TAG, "[WARN] The result is not a Number or a BigDecimal.");
						mDisplayedResult = result;
					}
				}
				mQueryTime = helper.getCalculationTime();

				// use the short version
				String input = helper.getInput();
				if (null == input || input.length() == 0) {
					input = mSpokenText + "?";
				} else {
					input = input + "=";
				}

				// Gray out the query.
				int endSpannableInput = input.length();
				String displayString = input + "\n" + mDisplayedResult;
				Spannable inputToSpan = new SpannableString(displayString);        
				inputToSpan.setSpan(new ForegroundColorSpan(Color.GRAY), 0, endSpannableInput, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				setContentView(new GlassTuggableView(this, 
						new CardBuilder(this, CardBuilder.Layout.TEXT) 
				.setText(inputToSpan)
				.setFootnote(R.string.tap_to_calc_again)
				.getView()));

				mAnswerDisplayed = true;
				// Read the results out loud.
				readAnswerAloud();
				return;
			}
		}
		return;
	}

	/**
	 * Exit Glassware.
	 */
	private void exitGlassware() {

		Log.i(TAG,"[INFO] Exiting Glass App...");

		// Print summary.
		printSummary();

		// Go home.
		Intent homeIntent= new Intent(Intent.ACTION_MAIN);
		homeIntent.addCategory(Intent.CATEGORY_HOME);
		homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(homeIntent);

		// Handle the Livecard.
		if ( null == mLiveCard) {
			Log.i(TAG,"[INFO] LiveCard is null.");
		} else if (mLiveCard.isPublished()) {
			mLiveCard.unpublish();
			Log.d(TAG,"[DEBUG] Card is unpublished!");
		} else {
			Log.i(TAG,"[INFO] Card was not published.");
		}
		mLiveCard = null;
		
		if (!this.isFinishing()) {
			finish();
		}
		return;
	}

	/*
	 * Create a new LiveCard.
	 */
	private void initLiveCard() {
		if (mLiveCard == null) {

			mLiveCard = new LiveCard(this, LIVE_CARD_ID);

			mLiveCard.setDirectRenderingEnabled(true);

			// Enable direct rendering.
			mLiveCard.setDirectRenderingEnabled(true);

			// Set callback
			mLiveCard.getSurfaceHolder().addCallback(new CalculatorLiveCardRenderer());

			// Set action
			Intent intent = new Intent(this, CalculatorMenuActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			mLiveCard.setAction(PendingIntent.getActivity(this, 0, intent, 0));

			mLiveCard.publish(LiveCard.PublishMode.REVEAL);
			Log.i(TAG, "[INFO] Published LiveCard.");
		} else {	
			// LiveCard already published
			Log.w(TAG, "[WARN] Card was published already.");
			// Navigate to the card 
			if (!mLiveCard.isPublished()) {
				mLiveCard.publish(PublishMode.REVEAL);
			} else {
				mLiveCard.navigate();
			}
			return;
		}
	}

	@Override
	public void onBackPressed() {
		Log.i(TAG, "[INFO] onBackPressed is called.");
		mAudioManager.playSoundEffect(Sounds.DISMISSED);
		mAnswerDisplayed = false;
		exitGlassware();
	}

	/**
	 * Prints out the output.
	 */
	private void printSummary () {
		
		// If the query was empty, no need for statistics.
		if (mSpokenText == null) {
			return;
		}

		Log.i(TAG, "[INFO] ******************** Summary ********************");
		Log.i(TAG, "[INFO] ***  Query:     " + mSpokenText + "?");
		Log.i(TAG, "[INFO] ***  Result:    " + mDisplayedResult + ".");
		Log.i(TAG, "[INFO] ***  Calc-time: " + mQueryTime+ " (miliseconds).");
		Log.i(TAG, "[INFO] *************************************************");
	}

	/**
	 * Create Gesture detector. 
	 */
	private GestureDetector createGestureDetector(Context context) {
		GestureDetector gestureDetector = new GestureDetector(context);
		//Create a base listener for generic gestures
		gestureDetector.setBaseListener( new GestureDetector.BaseListener() {
			@Override
			public boolean onGesture(Gesture gesture) {
				if (gesture == Gesture.TAP) {
					Log.d(TAG, "[DEBUG] User tapped with one finger");
					mAudioManager.playSoundEffect(Sounds.TAP);
					openOptionsMenu();
					return true;
				} else if (gesture == Gesture.TWO_TAP) {
					Log.d(TAG, "[DEBUG] User tapped with two fingers");
					mAudioManager.playSoundEffect(Sounds.TAP);
					openOptionsMenu();
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
			Log.i(TAG, "[INFO] User selected to retry..");
			mAnswerDisplayed = false;
			displaySpeechRecognizer();
			return true;
		case R.id.op_readaloud:
			Log.i(TAG, "[INFO] User selected read aloud..");
			readAnswerAloud();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		// Nothing to do here.
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

	/**
	 * Read the current heading aloud using the text-to-speech engine.
	 */
	public void readAnswerAloud() {

		String textToSpeech = "";
		if (mDisplayedResult == null || mDisplayedResult.length() == 0) {
			textToSpeech = getResources().getString(R.string.unable_to_read_aloud);
		} else {
			textToSpeech = getResources().getString(R.string.read_aloud_prefix);
			textToSpeech = textToSpeech + " " + mDisplayedResult;
		}
		mSpeech.speak(textToSpeech, TextToSpeech.QUEUE_FLUSH, null);
		return;
	}	

	@Override
	protected void onDestroy() {
		Log.d(TAG,"[DEBUG] onDesroy()!");
		super.onDestroy();	
		// Shut down the TextToSpeech.
		mSpeech.shutdown();
	}

	@Override
	protected void onStop() {
		Log.d(TAG,"[DEBUG] onStop() is called.");
		super.onStop();
		if (isFinishing()) {
			return;
		}
		
		if (mAnswerDisplayed) {
			// Handle the Livecard.
			if ( null != mLiveCard && mLiveCard.isPublished()) {
				Log.i(TAG,"[INFO] Card is unpublished!");
				mAnswerDisplayed = false;
				mLiveCard.unpublish();
				if (!isDestroyed() && !isFinishing()) {
					finish();
				}
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();		
	}

	@Override
	protected void onRestart() {
		super.onRestart();		
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}
}
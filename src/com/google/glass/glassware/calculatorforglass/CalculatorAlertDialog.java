package com.google.glass.glassware.calculatorforglass;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.view.MotionEvent;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.widget.CardBuilder;

public class CalculatorAlertDialog extends Dialog {

	private final DialogInterface.OnClickListener mOnClickListener;
	private final AudioManager mAudioManager;
	private final GestureDetector mGestureDetector;

	/** Handles the tap gesture to call the dialog's {@code OnClickListener} if one is provided. */
	private final GestureDetector.BaseListener mBaseListener = new GestureDetector.BaseListener() {

		@Override
		public boolean onGesture(Gesture gesture) {

			if (gesture == Gesture.TAP) {
				mAudioManager.playSoundEffect(Sounds.TAP);
				dismiss();
				if (mOnClickListener != null) {
					// Since Glass dialogs do not have buttons,
					// the index passed to onClick is always 0.
					mOnClickListener.onClick(CalculatorAlertDialog.this, 0);
				}
				return true;
			}
			if (gesture == Gesture.SWIPE_DOWN) {
				exitGlassware();
			}
			return false;
		}
	};

	public CalculatorAlertDialog(Context context, int iconResId, int textResId, int footnoteResId,DialogInterface.OnClickListener onClickListener) {
		super(context);

		mOnClickListener = onClickListener;
		mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		mGestureDetector = new GestureDetector(context).setBaseListener(mBaseListener);

		setContentView(new CardBuilder(context, CardBuilder.Layout.ALERT)
		.setIcon(iconResId)
		.setText(textResId)
		.setFootnote(footnoteResId)
		.getView());
	}

	/** Overridden to let the gesture detector handle a possible tap event. */
	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		return mGestureDetector.onMotionEvent(event) || super.onGenericMotionEvent(event);
	}
	
	/**
	 * Exit Glassware.
	 */
	private void exitGlassware() {
		// Go home.
		Intent homeIntent= new Intent(Intent.ACTION_MAIN);
		homeIntent.addCategory(Intent.CATEGORY_HOME);
		homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |Intent.FLAG_ACTIVITY_CLEAR_TASK);
		getContext().startActivity(homeIntent);
		return;
	}
}
package com.google.glass.glassware.calculatorforglass;

import com.wolfram.alpha.WAEngine;
import com.wolfram.alpha.WAException;
import com.wolfram.alpha.WAPlainText;
import com.wolfram.alpha.WAPod;
import com.wolfram.alpha.WAQuery;
import com.wolfram.alpha.WAQueryResult;
import com.wolfram.alpha.WASubpod;
import android.os.AsyncTask;
import android.util.Log;

public class WolframAlphaSearchHelper {


	private String		mTAG 					= CalculatorMenuActivity.TAG;
	private String 		mParsedSeachQuery 		= null;
	private	String		WOLFRAM_ALPHA_APPID		= "VUQ3XP-AG8LR3XV4P";
	private long		mStartTime 				= 0; 
	private long 		mEndTime 				= 0;
	private String 		mSearchQuery 			= null;
	private String 		mRawResult 				= null;
	private double 		mQueryTime				= 0;
	private String		mInput					= "";
	
	public WolframAlphaSearchHelper(String parsedSearchQuery) {
		mParsedSeachQuery = parsedSearchQuery;
	}

	public String getParsedSeachQuery() {
		return mParsedSeachQuery;
	}

	public void setParsedSeachQuery(String mParsedSeachQuery) {
		this.mParsedSeachQuery = mParsedSeachQuery;
	}

	public  void runSearch() {
		new GoogleSearchTask(mRawResult).execute(mSearchQuery);
	}
	
	public String getResults() {
		return mRawResult;
	}
	
	public double getCalculationTime () {
		return mQueryTime;
	}
	
	public String getInput() {
		return mInput;
	}
	
	/**
	 * Inner class to run the Search.
	 */
	private class GoogleSearchTask extends AsyncTask<String, Void, String> {

		public GoogleSearchTask(String query) {
			mSearchQuery = query;
		}

		protected String doInBackground(String... urls) {

			mStartTime = System.currentTimeMillis();

			// Use "pi" as the default query, or caller can supply it as the lone command-line argument.
			String input = getParsedSeachQuery();

			// The WAEngine is a factory for creating WAQuery objects,
			// and it also used to perform those queries. You can set properties of
			// the WAEngine (such as the desired API output format types) that will
			// be inherited by all WAQuery objects created from it. Most applications
			// will only need to crete one WAEngine object, which is used throughout
			// the life of the application.
			WAEngine engine = new WAEngine();

			// These properties will be set in all the WAQuery objects created from this WAEngine.
			engine.setAppID(WOLFRAM_ALPHA_APPID);
			engine.addFormat("plaintext");

			// Create the query.
			WAQuery query = engine.createQuery();

			// Set properties of the query.
			query.setInput(input);

			try {
				// For educational purposes, print out the URL we are about to send:
				Log.i(mTAG, "[INFO] Query URL:");
				Log.i(mTAG, "[INFO] " + engine.toURL(query) + "\n");

				// This sends the URL to the Wolfram|Alpha server, gets the XML result
				// and parses it into an object hierarchy held by the WAQueryResult object.
				WAQueryResult queryResult = engine.performQuery(query);

				if (queryResult.isError()) {
					Log.e(mTAG, "[ERROR] Query ERROR: error code: " + queryResult.getErrorCode() +"\n" +
							"ERROR message: " + queryResult.getErrorMessage() + "\n");
					mRawResult = "ERROR (" +  queryResult.getErrorCode() + ")";
				} else if (!queryResult.isSuccess()) {
					Log.i(mTAG, "[INFO] Query was not understood; no results available.");
					mRawResult = "Query was misunderstood";
					
				} else {
					boolean isDecimal = false;
					// Got a result.
					Log.i(mTAG, "[INFO] Successful query. Pods follow:\n");
					for (WAPod pod : queryResult.getPods()) {
						if (!pod.isError()) {
							Log.i(mTAG, "[INFO] " + pod.getTitle());
							Log.i(mTAG, "[INFO] -----------------");
							for (WASubpod subpod : pod.getSubpods()) {
								for (Object element : subpod.getContents()) {
									if (element instanceof WAPlainText) {
										Log.i(mTAG, "[INFO] " + ((WAPlainText) element).getText() + "\n");
										if (pod.getTitle().equalsIgnoreCase("Exact result")){
											if (!isDecimal) {
												mRawResult = ((WAPlainText) element).getText();
												mEndTime = System.currentTimeMillis();
												mQueryTime = (mEndTime-mStartTime);
											}
										}
										
										if (pod.getTitle().equalsIgnoreCase("Input")){
											mInput = ((WAPlainText) element).getText();
										}
										
										if (pod.getTitle().equalsIgnoreCase("Result")){
											mRawResult = ((WAPlainText) element).getText();
											mEndTime = System.currentTimeMillis();
											mQueryTime = (mEndTime-mStartTime);
										}
										
										if (pod.getTitle().equalsIgnoreCase("Decimal approximation")){
											mRawResult = ((WAPlainText) element).getText();
											mEndTime = System.currentTimeMillis();
											mQueryTime = (mEndTime-mStartTime);
											isDecimal = true;
										}
										
										if (pod.getTitle().equalsIgnoreCase("Decimal form")){
											mRawResult = ((WAPlainText) element).getText();
											mEndTime = System.currentTimeMillis();
											mQueryTime = (mEndTime-mStartTime);
											isDecimal = true;
										}
									}
								}
							}	
						}
					}
					// We ignored many other types of Wolfram|Alpha output, such as warnings, assumptions, etc.
					// These can be obtained by methods of WAQueryResult or objects deeper in the hierarchy.
					
				}
			} catch (WAException e) {
				Log.e(mTAG, "[ERROR]" + e.getMessage());
			}
			return mRawResult;
		}

		protected void onPostExecute(String result) {

			if (null == result) {
				Log.i(mTAG,"[DEBUG] Query result was empty." );
			}
			mRawResult = result;	
			
			Log.i(mTAG, "[INFO] Raw Result: " + mRawResult + "\n");
			Log.i(mTAG, "[INFO] Query Time: " + mQueryTime + " miliseconds)");

			//onPostDownload
			//run something
		}
	}
}

package com.liquidware.networkedserial.app;

import android.os.AsyncTask;
import android.util.Log;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

public class Tweet {
    private static final String TAG = "Tweet";

    /** Name to store the users access token */
    private static final String PREF_ACCESS_TOKEN = "";
    /** Name to store the users access token secret */
    private static final String PREF_ACCESS_TOKEN_SECRET = "";
    /** Consumer Key generated when you registered your app at https://dev.twitter.com/apps/ */
    private static final String CONSUMER_KEY = "";
    /** Consumer Secret generated when you registered your app at https://dev.twitter.com/apps/  */
    private static final String CONSUMER_SECRET = ""; // XXX Encode in your app
    /** Twitter4j object */
    private final Twitter mTwitter;

    /** Called when the activity is first created. */
    public Tweet(String message) {
            Log.i(TAG, "Loading TweetToTwitterActivity");

            // Load the twitter4j helper
            mTwitter = new TwitterFactory().getInstance();
            Log.i(TAG, "Got Twitter4j");

            // Tell twitter4j that we want to use it with our app
            mTwitter.setOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET);
            Log.i(TAG, "Inflated Twitter4j");

            //tweet
            new postMessage().execute(message);
    }

    /**
     * Login and tweet
     */
    public class postMessage extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... message) {
            loginAuthorisedUser();
            tweetMessage(message[0]);
            return null;
        }
    }

    /**
     * The user had previously given our app permission to use Twitter</br>
     * Therefore we retrieve these credentials and fill out the Twitter4j helper
     */
    private void loginAuthorisedUser() {
            String token = PREF_ACCESS_TOKEN;
            String secret = PREF_ACCESS_TOKEN_SECRET;

            // Create the twitter access token from the credentials we got previously
            AccessToken at = new AccessToken(token, secret);

            mTwitter.setOAuthAccessToken(at);

            Log.d(TAG, "Welcome back user.");
    }

    /**
     * Send a tweet on your timeline, with a //Toast msg for success or failure
     */
    private void tweetMessage(String message) {
            try {
                    mTwitter.updateStatus(message);
                    Log.d(TAG, "Tweet successful!");
            } catch (TwitterException e) {
                e.printStackTrace();
            }
    }
}

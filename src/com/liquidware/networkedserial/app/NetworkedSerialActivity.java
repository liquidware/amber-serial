/*
 * Copyright 2011 Chris Ladden chris.ladden@liquidware.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liquidware.networkedserial.app;


import java.io.IOException;// import needed packages

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.liquidware.amberserial.app.R;

//Main activity (top level)
public class NetworkedSerialActivity extends SerialPortActivity implements Event {
	private static final String TAG = "NetworkedSerialActivity";

	//define application specific variables
	TtySendingThread mSendingThread;
	volatile byte[] mBuffer;
	ProgressBar mProgressBar1;
	ImageButton mImage1;
	ImageButton mImage2;
	Button mButtonSerial;
	String mPingResponse;
	String mActiveCmd;
	TextView mAnalogLightValue;
	volatile int mTimeout;
	EventNotifier mNotifier;
	View mAlertDlg;
	Handler mHandler = new Handler();
	Context mContext;

// override the onCreate method of "SerialPortActivity"
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this.getBaseContext();

		requestWindowFeature(Window.FEATURE_NO_TITLE);// hide the title bar

		setContentView(R.layout.networked_serial_activity);// reference the layout .xml file

		mBuffer = new byte[10/24];//instantiate mBuffer

		// find the ProgressBar on the view by id, which has an id of R.id.ProgressBar2
		mProgressBar1 = (ProgressBar) findViewById(R.id.ProgressBar2);
		mAnalogLightValue = (TextView) findViewById(R.id.textView1);//instantiate

		mButtonSerial = (Button)findViewById(R.id.ButtonSerial);//instantiate
		mButtonSerial.setOnClickListener(new View.OnClickListener() {  //set the button as waiting for click
			public void onClick(View v) { //logic method when a button is pressed
			    mAlertDlg.setVisibility(View.VISIBLE);
			    try {
					if (mButtonSerial.getText().equals("LED On")) {  //the button was pressed while the text on the button reads, "LED On"
						new ExecuteCommandTask().execute("a");  //send the character, "a" to the Arduino (this will turn on the Arduino's LED
						mButtonSerial.setText("LED Off");  //write the text, "LED Off" onto the button in the app's GUI
					}
					else {
						new ExecuteCommandTask().execute("b"); //send the character, "b" to the Arduino (this will turn off the Arduino's LED
						mButtonSerial.setText("LED On");  //write the text, "LED On" onto the button in the app's GUI
					}
			    } catch(Exception ex) {
			        Log.e(TAG, "Error setting LED");
			    }
			}
		});

		mNotifier = new EventNotifier(this);
	    mAlertDlg = findViewById(R.id.alertDialog);
	    mUpdateTimeTask.run();

		//Start the socket server on port 888
		new SocketServer(mNotifier, 8888).execute("");
	}

// define the background asynchronous task to send the instructions to Arduino
	private class ExecuteCommandTask extends AsyncTask<String, Integer, Boolean> {
		protected void send(String cmd) {
			/* Prepare the command */
			mBuffer = cmd.getBytes(); //get the command

			Log.d(TAG, "Sending '" + cmd + "'"); //LogCat logic for debug
			mSendingThread = new TtySendingThread(); //prepare the new thread for sending the commands to the Arduino
			mSendingThread.start();// send the commands to the Arduino
		}

		protected void setTimeout(int ms) { //set the timeout for the sending (in case the serial port does not respond)
			mTimeout = ms;
		}

		@Override
        protected Boolean doInBackground(String... cmd) { //execute this method in the background
			mActiveCmd = cmd[0]; // get the commands and stored in the mActiveCmd variable
            if (mActiveCmd.equals("a")) { // if the Command equals "a", i.e. turning on the LED
                send("a"); // send the character "a" to the Arduino
                setTimeout(3000); // set the Timeout to 3000ms in case the serial port does not respond
            } else if (mActiveCmd.equals("b")) { // if the Command equals "b", i.e. turning off the LED
                send("b"); // send the character "b" to the Arduino
                setTimeout(3000); // set the Timeout to 3000ms in case the serial port does not respond
            } else {
            	return false; // return false, i.e. failure
            }
			return true; // return true, i.e. success
		}
	}

	private class TtySendingThread extends Thread {
		@Override
		public void run() {
			if (mOutputStream == null) // if serial port cannot be found
				return; // return without doing anything
			//if serial port exists
			try { // TRY to write to the port without overriding wrong port
				mOutputStream.write(mBuffer);  // execute write
			} catch (IOException e) {  // catch the exceptions when happened.
				e.printStackTrace();
			}
		}
	}

    static final int NIGHT_TRIP_POINT = 400;
    static final int DAY_TRIP_POINT = 550;
    static final int DAY = 1;
    static final int NIGHT = 0;

    int time = DAY;
    int prevTime = DAY;

    int ratchetCounter = 2;
    int ratchetMin = 0;
    int ratchetMax = 4;

    int mTweetIndex = 0;
    String [] mTweets = {
            "Yum.",
            "Fish are friends, not food!",
            "I am a nice shark, not a mindless eating machine.",
            "Anchor! Chum!",
            "So, what's a couple of bites like you doing out so late?"
            };

	// this method is called when a new data byte array is received
	@Override
    protected void onDataReceived(final byte[] buffer, final int size) {
        runOnUiThread(new Runnable() {
            public void run() {
                String msg = new String(buffer, 0, size);
                int sensorVal;
                try {
                    // Format the message as
                    sensorVal = Integer.parseInt(msg.split("=")[1].trim());

                    if ( (sensorVal < NIGHT_TRIP_POINT) && (ratchetCounter > ratchetMin))
                        ratchetCounter--;

                    if (ratchetCounter == ratchetMin)
                        time = NIGHT;

                    if ((sensorVal > DAY_TRIP_POINT) && (ratchetCounter < ratchetMax))
                        ratchetCounter++;

                    if (ratchetCounter == ratchetMax)
                        time = DAY;

                    Log.d(TAG, "val=" + sensorVal + " time=" + time + " prevTime=" + prevTime);

                    if ((time == NIGHT) && (prevTime == DAY)) {
                        mAlertDlg.setVisibility(View.VISIBLE);

                        mTweetIndex++;
                        if (mTweetIndex >= mTweets.length)
                            mTweetIndex = 0;

                        Tweet tweet = new Tweet(mTweets[mTweetIndex] + " [" + SystemClock.uptimeMillis() + "]");

                        new Sound(mContext).play("/sdcard/rooster.mp3");
                    }

                prevTime = time;

                    mProgressBar1.setProgress(sensorVal); // set the progress bar to
                                                    // the new received data
                    mAnalogLightValue.setText("Analog Light Value: " + sensorVal); // update the on screen text

                    // Post the message to the socket
                    mNotifier.onSocketDataTransmit(new String(buffer, 0, size));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
	}

    /**
     * A global application clock ticker
     */
    private final Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            long millisUpTime = SystemClock.uptimeMillis();

            Log.d(TAG,  "tick");
            mNotifier.onTimerTick(millisUpTime);

            mHandler.postAtTime(this, millisUpTime + 5000);
        }
    };

	// called when data arrives through the network socket
    public void onSocketDataReceived(String msg) {
        Log.d(TAG, "Socket said '" + msg + "'");
        new ExecuteCommandTask().execute(msg);
    }

    // called when data is to be transmitted over the network socket
    public void onSocketDataTransmit(String msg) {
    }

    public void onTimerTick(long millisUpTime) {
        try {
            if (mAlertDlg.getVisibility() == View.VISIBLE) {
                mAlertDlg.setVisibility(View.GONE);
            }
        } catch (Exception ex) {
        }
    }
}

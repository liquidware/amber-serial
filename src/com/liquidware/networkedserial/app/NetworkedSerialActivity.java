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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
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

// override the onCreate method of "SerialPortActivity"
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);// hide the title bar

		setContentView(R.layout.networked_serial_activity);// reference the layout .xml file
		mBuffer = new byte[1024];//instantiate mBuffer

		// find the ProgressBar on the view by id, which has an id of R.id.ProgressBar2
		mProgressBar1 = (ProgressBar) findViewById(R.id.ProgressBar2);
		mAnalogLightValue = (TextView) findViewById(R.id.textView1);//instantiate

		mButtonSerial = (Button)findViewById(R.id.ButtonSerial);//instantiate
		mButtonSerial.setOnClickListener(new View.OnClickListener() {  //set the button as waiting for click
			public void onClick(View v) { //logic method when a button is pressed
				if (mSerialPort != null) {
					if (mButtonSerial.getText().equals("LED On")) {  //the button was pressed while the text on the button reads, "LED On"
						new ExecuteCommandTask().execute("a");  //send the character, "a" to the Arduino (this will turn on the Arduino's LED
						mButtonSerial.setText("LED Off");  //write the text, "LED Off" onto the button in the app's GUI
					}
					else {
						new ExecuteCommandTask().execute("b"); //send the character, "b" to the Arduino (this will turn off the Arduino's LED
						mButtonSerial.setText("LED On");  //write the text, "LED On" onto the button in the app's GUI
					}
				} else {
					Toast.makeText(getApplicationContext(), "Error: serial not ready", 1).show();// error logic for when the port can not be found
				}
			}
		});

		mNotifier = new EventNotifier(this);
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

	// this method is called when a new data byte array is received
	@Override
    protected void onDataReceived(final byte[] buffer, final int size) {
		runOnUiThread(new Runnable() {
			public void run() {
			    String msg = new String(buffer, 0, size);
			    int val = 0;
			    try {
			        val = Integer.parseInt(msg);
			    } catch (Exception ex) {
			    }
				mProgressBar1.setProgress(val); // set the progress bar to the new received data
				mAnalogLightValue.setText("Analog Light Value: " + (new String(buffer, 0, size))); // update the text displayed on the screen
				//Post the message to the socket
		        mNotifier.onSocketDataTransmit(new String(buffer, 0, size));
			}
		});


	}

	// called when data arrives through the network socket
    public void onSocketDataReceived(String msg) {
        Log.d(TAG, "Socket said '" + msg + "'");
        new ExecuteCommandTask().execute(msg);
    }

    // called when data is to be transmitted over the network socket
    public void onSocketDataTransmit(String msg) {
    }

    public void onTimerTick(long millisUpTime) {
    }
}

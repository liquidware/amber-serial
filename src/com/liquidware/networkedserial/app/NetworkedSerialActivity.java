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

import java.io.File;
import java.io.IOException;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.liquidware.networkedserial.app.R;

public class NetworkedSerialActivity extends SerialPortActivity {
	private static final String TAG = "NetworkedSerialActivity";

	private static final String mLocalDir = Environment.getExternalStorageDirectory().toString() + "/out";
	
	private static final int CMD_SUCCESS  = -1;
	private static final int CMD_TIMEOUT  = -2;

	SendingThread mSendingThread;
	volatile byte[] mBuffer;
	ProgressBar mProgressBar1;
	ImageButton mImage1;
	ImageButton mImage2;
	Button mButtonSerial;
	String mPingResponse;
	String mActiveCmd;
	TextView mAnalogLightValue;
	volatile String mReceptionBuffer;
	volatile StringBuffer mStringBuffer;
	volatile String mExpectedResult;
	volatile boolean mIsExpectedResult;
	volatile boolean mShowSerialInput;
	volatile int mTimeout;

	public void setUIDisabled() {
		mButtonSerial.setEnabled(false);
	}

	public void setUIEnabled() {
		mButtonSerial.setEnabled(true);
	}
	
	private void setupKeyGuardPower() {
	    PowerManager.WakeLock wl;
	    
        try {
            Log.w(TAG, "Disabling keyguard");
            KeyguardManager keyguardManager = (KeyguardManager)getSystemService(Activity.KEYGUARD_SERVICE);
            KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
            lock.disableKeyguard();
        } catch (Exception ex) {
            Log.e(TAG, ex.getStackTrace().toString());
            ex.printStackTrace();
        }
        
        try {
            Log.w(TAG, "Acquiring wake lock");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Tag");
        } catch (Exception ex) {
            Log.e(TAG, ex.getStackTrace().toString());
            ex.printStackTrace();
        }
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        //WindowManager.LayoutParams.FLAG_FULLSCREEN);
 
		setContentView(R.layout.networked_serial_activity);
		mBuffer = new byte[1024];
		mStringBuffer = new StringBuffer(500000);
		mShowSerialInput = true;
		
		setupKeyGuardPower();
		
		mProgressBar1 = (ProgressBar) findViewById(R.id.ProgressBar2);
		mAnalogLightValue = (TextView) findViewById(R.id.textView1);
		
		mButtonSerial = (Button)findViewById(R.id.ButtonSerial);
		mButtonSerial.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (mSerialPort != null) {
					if (mButtonSerial.getText().equals("LED On")) {
						new ExecuteCommandTask().execute("a");
						mButtonSerial.setText("LED Off");
//						Toast.makeText(getApplicationContext(), "LED On!", 1).show();
						
					}
					else {
						new ExecuteCommandTask().execute("b");
						mButtonSerial.setText("LED On");
//						Toast.makeText(getApplicationContext(), "LED Off!", 1).show();
						
					}
				} else {
					Toast.makeText(getApplicationContext(), "Error: serial not ready", 1).show();
				}
			}
		});  
		  		
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN); 
	}

	private class ExecuteCommandTask extends AsyncTask<String, Integer, Boolean> {

		protected void onPreExecute() {
			setUIDisabled();
		}

		protected void send(String cmd) {
			/* Prepare the command */	
			mReceptionBuffer = "";
			mStringBuffer.delete(0, mStringBuffer.length());
			mIsExpectedResult = false;
			mBuffer = cmd.getBytes();

			Log.d(TAG, "Sending '" + cmd + "'");
			mSendingThread = new SendingThread();
			mSendingThread.start();
		}

		protected void setTimeout(int ms) {
			mTimeout = ms;
		}
		
		public boolean prepareLocalDirectory(String path) {
			File dir = new File(path);
			Util.deleteEntireDirectory(dir);
			dir.mkdirs();
			
			return true;
		}

		protected Boolean doInBackground(String... cmd) {
			boolean r = true;
			
			mActiveCmd = cmd[0];
            
			prepareLocalDirectory(mLocalDir);
            
            if (mActiveCmd.equals("a")) {
                int count = 1;
            	while(count-- > 0) {
            		send("a");
            	};
                setTimeout(3000);
            } else if (mActiveCmd.equals("b")) {
                int count = 1;
            	while(count-- > 0) {
            		send("b");
            	};
                setTimeout(3000);
            }
			return r;
		}

		protected void onProgressUpdate(Integer... progress) {
			
			//Handle the UI progress
			if (progress[0] == CMD_SUCCESS) {
				Toast.makeText(getApplicationContext(), "Success!", 1).show();
			} else if (progress[0] == CMD_TIMEOUT){
			    //do nothing
				//Toast.makeText(getApplicationContext(), "Error: timeout running command.", 1).show();
			} else {
				//just update the UI with some progress.
			}
		}

		protected void onPostExecute(Boolean result) {
			setUIEnabled();
		}
	}

	private class SendingThread extends Thread {
		@Override
		public void run() {
			if (mOutputStream == null)
				return;

			try {
				mOutputStream.write(mBuffer);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	protected void onDataReceived(final byte[] buffer, final int size) {
		runOnUiThread(new Runnable() {
			public void run() {
				if (mStringBuffer == null)
					return;
				mStringBuffer.append(new String(buffer, 0, size));
				mProgressBar1.setProgress(Integer.parseInt(new String(buffer, 0, size)));
				mAnalogLightValue.setText("Analog Light Value: " + (new String(buffer, 0, size)));
			}
		});
	}
}

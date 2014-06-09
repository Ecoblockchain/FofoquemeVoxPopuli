package me.fofoque.bttest;
 
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ToggleButton;

import android.telephony.SmsMessage;

public class BTTestActivity extends Activity {
	private static final String TAG = "BTTestTag ";
	private static final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final String BLUETOOTH_ADDRESS = "00:14:03:06:19:00";

	private ToggleButton buttonLED;

	private SMSReceiver mSMS = null;
	private BluetoothSocket myBTSocket = null;
	private InputStream mInputStream = null;
	private OutputStream mOutputStream = null;

	// listen for intent sent by broadcast of SMS signal
	public class SMSReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = intent.getExtras();
			SmsMessage[] msgs = null;

			if (bundle != null) {
				Object[] pdus = (Object[]) bundle.get("pdus");
				msgs = new SmsMessage[pdus.length];

				if (msgs.length > 0) {
					// read only the most recent
					msgs[0] = SmsMessage.createFromPdu((byte[]) pdus[0]);
					String smsMessageText = msgs[0].getMessageBody().toString();
					String phoneNum = msgs[0].getOriginatingAddress().toString();
					Log.d(TAG, "got sms: "+smsMessageText);
					Log.d(TAG, "from: "+phoneNum);

					// only send to motor if it's from a real number
					if(phoneNum.length() > 5) {
						sendToMotor();
					}
				}
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// BT
		initBluetoothStreams(BluetoothAdapter.getDefaultAdapter());

		// SMS
		mSMS = (mSMS == null)?(new SMSReceiver()):mSMS;
		registerReceiver(mSMS, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));

		// GUI
		setContentView(R.layout.main);
		buttonLED = (ToggleButton) findViewById(R.id.ledButton);
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mSMS);
		try{
			if(mInputStream != null) mInputStream.close();
			if(mOutputStream != null) mOutputStream.close();
			if(myBTSocket != null) myBTSocket.close();
		}
		catch(IOException e){}
		super.onDestroy();
	}

	private void initBluetoothStreams(BluetoothAdapter myBTA){
		BluetoothDevice myBTDevice = myBTA.getRemoteDevice(BLUETOOTH_ADDRESS);
		try{
			// if there's a non-null socket... disconnect
			if(myBTSocket != null) myBTSocket.close();

			// then (re)start the socket
			myBTSocket = myBTDevice.createRfcommSocketToServiceRecord(SERIAL_UUID);
			myBTSocket.connect();
			mOutputStream = myBTSocket.getOutputStream();
			mInputStream = myBTSocket.getInputStream();
		}
		catch(Exception e){
			Log.e(TAG, "Couldn't open streams !!");
			Log.e(TAG, e.toString());
		}

		// check BT streams
		if(mOutputStream != null && mInputStream != null){
			Log.d(TAG, "BT Helper Stream init was successful");
		}
	}

	private void sendToMotor(){
		if ((mOutputStream != null) && (mInputStream != null)){
			byte[] buffer = {'F', 'Q', (byte)0xff, (byte)0xff};
			try{
				mOutputStream.write(buffer);
			}
			catch(IOException e){
				Log.e(TAG, "serial write failed", e);
				mInputStream = null;
				mOutputStream = null;
				initBluetoothStreams(BluetoothAdapter.getDefaultAdapter());
			}
		}
		else{
			initBluetoothStreams(BluetoothAdapter.getDefaultAdapter());
		}

	}

	public void blinkLED(View v){
		byte[] buffer = {'L', 'E', 'D', 0x0};
		if(buttonLED.isChecked()){
			buffer[3] = 0x1;
		}
		else{
			buffer[3] = 0x0;
		}
 
		if (mOutputStream != null) {
			try {
				mOutputStream.write(buffer);
			}
			catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
	}
}

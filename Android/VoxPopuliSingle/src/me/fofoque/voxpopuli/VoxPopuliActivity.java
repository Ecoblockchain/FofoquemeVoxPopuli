package me.fofoque.voxpopuli;
 
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ToggleButton;

import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.Engine;
import android.telephony.SmsMessage;

public class VoxPopuliActivity extends Activity implements TextToSpeech.OnInitListener {
	// TAG is used to debug in Android logcat console
	private static final String TAG = "VoxPopTag ";
	private static final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final String BLUETOOTH_ADDRESS = "00:14:03:06:19:99";

	private ToggleButton buttonLED;

	private BluetoothSocket myBTSocket = null;
	private InputStream mInputStream = null;
	private OutputStream mOutputStream = null;
	private boolean isWaitingForMotor = false;

	private TextToSpeech mTTS = null;
	private SMSReceiver mSMS = null;

	private String smsMessageText = "";

	private class MotorMessage {
		public String msg;
		public int pan, tilt;
		public MotorMessage(String m, int p, int t){
			msg = m;
			pan = p&0xff;
			tilt = t&0xff;
		}
	}
	// queue for messages
	private Queue<MotorMessage> msgQueue = null;

	// listen for intent sent by broadcast of SMS signal
	// if it gets a new SMS, clean it up a little bit and put on queue
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
					smsMessageText = msgs[0].getMessageBody().toString();
					String phoneNum = msgs[0].getOriginatingAddress().toString();
					Log.d(TAG, "Client MainAction got sms: "+smsMessageText);
					Log.d(TAG, "from: "+phoneNum);

					// only write if it's from a real number
					if(phoneNum.length() > 5) {
						// clean up the @/# if it's there...
						smsMessageText = smsMessageText.replaceAll("[@#]?", "");
						smsMessageText = smsMessageText.replaceAll("[():]+", "");

						// add to queue
						int pan = (new Random()).nextInt(255);
						int tilt = (new Random()).nextInt(255);
						Log.d(TAG, "Adding: ("+smsMessageText+","+pan+","+tilt+") to queue");
						msgQueue.offer(new MotorMessage(smsMessageText, pan, tilt));
						checkQueues();
					}
				}
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "Serial: "+Build.SERIAL);
		Log.d(TAG, "BT Address: "+BLUETOOTH_ADDRESS);

		// Bluetooth
		// from : http://stackoverflow.com/questions/6565144/android-bluetooth-com-port
		BluetoothAdapter myBTAdapter = BluetoothAdapter.getDefaultAdapter();
		// this shouldn't happen...
		if (!myBTAdapter.isEnabled()) {
			//make sure the device's bluetooth is enabled
			Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBluetooth, 12345);
		}
		else{
			bluetoothInitHelper(myBTAdapter);
		}

		// FFQ
		mTTS = (mTTS == null)?(new TextToSpeech(this, this)):mTTS;
		mSMS = (mSMS == null)?(new SMSReceiver()):mSMS;
		msgQueue = (msgQueue == null)?(new LinkedList<MotorMessage>()):msgQueue;
		registerReceiver(mSMS, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));

		checkQueues();

		// GUI
		setContentView(R.layout.main);
		buttonLED = (ToggleButton) findViewById(R.id.ledButton);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ((requestCode == 12345) && (resultCode == RESULT_OK)) {
				this.bluetoothInitHelper(BluetoothAdapter.getDefaultAdapter());
		}
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mSMS);
		if(mTTS != null) mTTS.shutdown();
		try{
			if(mInputStream != null) mInputStream.close();
			if(mOutputStream != null) mOutputStream.close();
			if(myBTSocket != null) myBTSocket.close();
		}
		catch(IOException e){}
		super.onDestroy();
	}

	// from OnInitListener interface
	public void onInit(int status){
		// set the package and language for tts
		//   these are the values for Luciana
		mTTS.setEngineByPackageName("com.svox.classic");
		mTTS.setLanguage(new Locale("pt_BR"));

		// slow her down a little...
		mTTS.setSpeechRate(0.66f);
		mTTS.setPitch((new Random()).nextFloat()*0.5f+0.5f);

		// attach listener
		mTTS.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener(){
			@Override
			public void onUtteranceCompleted (String utteranceId){
				// check if there are things to be said
				VoxPopuliActivity.this.checkQueues();
			}
		});
		Log.d(TAG, "TTS ready! "+mTTS.getLanguage().toString());
	}

	private void bluetoothBondCheck(){
		// get whether the BTDevice is bonded
		if((myBTSocket != null) && (myBTSocket.getRemoteDevice() != null)){
			// if the device is not bonded, try to bond again...
			while(myBTSocket.getRemoteDevice().getBondState() == BluetoothDevice.BOND_NONE){
				Log.d(TAG, "from BT BondCheck: BTDevice not bonded, trying to re-bond");
				bluetoothInitHelper(BluetoothAdapter.getDefaultAdapter());
			}
		}
	}

	private void bluetoothInitHelper(BluetoothAdapter myBTA){
		Log.d(TAG, "From BT Helper");
		// get a device
		BluetoothDevice myBTDevice = myBTA.getRemoteDevice(BLUETOOTH_ADDRESS);
		// get a socket and stream
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
			Log.e(TAG, "Couldn't open streams !!?");
			Log.e(TAG, e.toString());
		}

		// check BT streams
		if(mOutputStream != null && mInputStream != null){
			Log.d(TAG, "BT Helper Stream init was successful");
		}
	}

	private void checkQueues(){
		// if already doing something, return
		if(mTTS.isSpeaking() || isWaitingForMotor){
			return;
		}

		// check queue for new messages
		if(msgQueue.peek() != null){
			Log.d(TAG, "there's msg");
			bluetoothBondCheck();
			final MotorMessage nextMessage = msgQueue.poll();
			if ((mOutputStream != null) && (mInputStream != null)){
				Log.d(TAG, "there's arduino");
				Thread motorThread = new Thread(new Runnable(){
				    @Override
				    public void run() {
				    	byte[] buffer = {'F', 'Q', (byte)nextMessage.pan, (byte)nextMessage.tilt};
						try{
							long startWaitMillis = System.currentTimeMillis();
							mOutputStream.write(buffer);
							Log.d(TAG, "wrote to motors");
							isWaitingForMotor = true;
							while((mInputStream.available() < 2) && (System.currentTimeMillis() - startWaitMillis < 4000)){
								Thread.sleep(100);
							}
							isWaitingForMotor = false;
							if((mInputStream.read() == 'G') && (mInputStream.read() == 'O')){
								Log.d(TAG, "got response from arduino");
								playMessage(nextMessage.msg);
							}
							else{
								Log.d(TAG, "got timeout");
								playMessage(nextMessage.msg);
							}
						}
						catch(IOException e){
							Log.e(TAG, "read or write failed", e);
							mInputStream = null;
							mOutputStream = null;
							bluetoothInitHelper(BluetoothAdapter.getDefaultAdapter());
						}
						catch(NullPointerException e){}
						catch(InterruptedException e){}
				    }
				});
				motorThread.start();
			}
			// BT streams are null, connect again
			else{
				bluetoothInitHelper(BluetoothAdapter.getDefaultAdapter());
			}
		}
	}

	private void playMessage(String msg){
		Log.d(TAG, "playing TTS message");
		HashMap<String,String> foo = new HashMap<String,String>();
		foo.put(Engine.KEY_PARAM_UTTERANCE_ID, "1234");
		// pause before and afterwards.
		mTTS.speak(". . "+msg+" . . ", TextToSpeech.QUEUE_ADD, foo);
	}

	public void testMegaphone(View v){
		String msg = "testando o megafone. não é?";
		int panAndTilt = 128;
		((LinkedList<MotorMessage>)msgQueue).addFirst(new MotorMessage(msg, panAndTilt, panAndTilt));
		checkQueues();
	}

	public void quitActivity(View v){
		finish();
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
			Log.d(TAG, "stream not null. sending button message");
			try {
				mOutputStream.write(buffer);
			}
			catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
	}
}

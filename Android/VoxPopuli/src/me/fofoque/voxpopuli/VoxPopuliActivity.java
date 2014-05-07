package me.fofoque.voxpopuli;
 
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.ToggleButton;

import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.Engine;
import android.telephony.SmsMessage;

import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

public class VoxPopuliActivity extends Activity implements TextToSpeech.OnInitListener {
 
	// TAG is used to debug in Android logcat console
	private static final String TAG = "VoxPop ";
	private static final String VOICE_MESSAGE_STRING = "!!!FFQMEVOXPOPULI!!!";
	private static final String VOICE_MESSAGE_URL = "http://server/latest.mp3";
	private static final String OSC_OUT_ADDRESS = "server.local";
	private static final int OSC_OUT_PORT = 8888;
	private static final int OSC_IN_PORT = 8989;
	private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";

	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;
	private ToggleButton buttonLED;

	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;
 
	private TextToSpeech mTTS = null;
	private MediaPlayer mAudioPlayer = null;
	private SMSReceiver mSMS = null;
	
	private OSCPortIn mOscIn = null;
	private OSCPortOut mOscOut = null;
	OSCListener mOscListener = new OSCListener() {
		public void acceptMessage(Date time, OSCMessage message) {
			System.out.println("Message received!: "+message.getAddress()+" "+message.getArguments());
			// read: msg, pan, tilt from osc
			String msg = (String)(message.getArguments().get(0));
			byte pan = ((Byte)(message.getArguments().get(1))).byteValue();
			byte tilt = ((Byte)(message.getArguments().get(2))).byteValue();

			if(msg == VOICE_MESSAGE_STRING){
				((LinkedList<MotorMessage>)msgQueue).addFirst(new MotorMessage(msg, pan, tilt));
			}
			else {
				msgQueue.offer(new MotorMessage(msg, pan, tilt));
			}
			checkQueues();
		}
	};

	private class MotorMessage {
		public String msg;
		public byte pan, tilt;
		public MotorMessage(String m, byte p, byte t){
			msg = m;
			pan = p;
			tilt = t;
		}
	}
	// queue for messages
	private Queue<MotorMessage> msgQueue = null;

	// listen for intent sent by broadcast of SMS signal
	// if it gets a new SMS
	//  clean it up a little bit and send to server
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
					String message = msgs[0].getMessageBody().toString();
					String phoneNum = msgs[0].getOriginatingAddress().toString();
					Log.d(TAG, "Client MainAction got sms: "+message);
					Log.d(TAG, "from: "+phoneNum);

					// only write if it's from a real number
					if(phoneNum.length() > 5) {
						// clean up the @/# if it's there...
						message = message.replaceAll("[@#]?", "");
						message = message.replaceAll("[():]+", "");
						
						// send to server
						OSCMessage oscMsg = new OSCMessage("/ffqmesms");
						oscMsg.addArgument(new String(message));
						try{
							mOscOut.send(oscMsg);
						}
						catch(IOException e){}
					}
				}
			}
		}
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} 
					else {
						Log.d(TAG, "permission denied for accessory " + accessory);
					}
					mPermissionRequestPending = false;
				}
			} 
			else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
 
		// ADK USB FOO
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);
		
		// FFQ
		mTTS = (mTTS == null)?(new TextToSpeech(this, this)):mTTS;
		mAudioPlayer = (mAudioPlayer == null)?(new MediaPlayer()):mAudioPlayer;
		mSMS = (mSMS == null)?(new SMSReceiver()):mSMS;
		msgQueue = (msgQueue == null)?(new LinkedList<MotorMessage>()):msgQueue;
		registerReceiver(mSMS, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
		mAudioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		mAudioPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				mp.stop();
				VoxPopuliActivity.this.checkQueues();
			}
		});

		// OSC
		try{
			mOscIn = (mOscIn == null)?(new OSCPortIn(OSC_IN_PORT)):mOscIn;
			mOscIn.addListener("/ffqmevox", mOscListener);
			mOscIn.startListening();
		}
		catch(SocketException e){}

		try{
			mOscOut = (mOscOut == null)?(new OSCPortOut(InetAddress.getByName(OSC_OUT_ADDRESS),OSC_OUT_PORT)):mOscOut;
		}
		catch(SocketException e){}
		catch(UnknownHostException e){}

		// GUI
		setContentView(R.layout.main);
		buttonLED = (ToggleButton) findViewById(R.id.ledButton);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mInputStream != null && mOutputStream != null) {
			return;
		}

		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} 
			else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory,mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		}
		else {
			Log.d(TAG, "mAccessory is null");
		}
	}
 
	@Override
	public void onPause() {
		super.onPause();
		closeAccessory();
	}
 
	@Override
	public void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		unregisterReceiver(mSMS);
		if(mTTS != null) mTTS.shutdown();
		if (mAudioPlayer != null) mAudioPlayer.release();
		if(mOscIn != null) mOscIn.close();
		if(mOscOut != null) mOscOut.close();
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
		mTTS.setPitch(1.0f);

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

	private void checkQueues(){
		if(mTTS.isSpeaking() || mAudioPlayer.isPlaying()){
			return;
		}

		if(msgQueue.peek() != null){
			MotorMessage nextMessage = msgQueue.poll();
			if(msgQueue.peek() == null){
				// TODO: tell server we just got empty?
			}
			if (mOutputStream != null) {
				byte[] buffer = {(byte)0xff, (byte)0x93, (byte)nextMessage.pan, (byte)nextMessage.tilt};
				try {
					long startWaitMillis = System.currentTimeMillis();
					mOutputStream.write(buffer);
					mAudioPlayer.prepare();
					while((mInputStream.available() < 1) && (System.currentTimeMillis() - startWaitMillis < 4000)){
						Thread.sleep(100);
					}
					if(mInputStream.read() == (byte)0xf9){
						playMessage(nextMessage.msg);
					}
				} 
				catch (IOException e) {
					Log.e(TAG, "read or write failed", e);
				}
				catch(InterruptedException e){
					Log.e(TAG, "thread sleep failed", e);
				}
			}
		}
	}

	private void playMessage(String msg){
		if(msg == VOICE_MESSAGE_STRING){
			try{
				mAudioPlayer.setDataSource(VOICE_MESSAGE_URL);
				mAudioPlayer.start();
			}
			catch(IOException e){
				Log.e(TAG, "failed to open stream", e);
			}
		}
		else{
			HashMap<String,String> foo = new HashMap<String,String>();
			foo.put(Engine.KEY_PARAM_UTTERANCE_ID, "1234");
			// pause before and afterwards.
			mTTS.speak(". . "+msg+" . . ", TextToSpeech.QUEUE_ADD, foo);
		}
	}

	private void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Log.d(TAG, "accessory opened");
		}
		else {
			Log.d(TAG, "accessory open fail");
		}
	}

	private void closeAccessory() {
		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		}
		catch (IOException e) {}
		finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}
 
	public void blinkLED(View v){
 
		byte[] buffer = new byte[1];
 
		if(buttonLED.isChecked())
			buffer[0]=(byte)1; // button says on, light is off
		else
			buffer[0]=(byte)0; // button says off, light is on
 
		if (mOutputStream != null) {
			try {
				mOutputStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
	}
}

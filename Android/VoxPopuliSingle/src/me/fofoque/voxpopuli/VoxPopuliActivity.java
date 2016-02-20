package me.fofoque.voxpopuli;
 
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.Engine;
import android.speech.tts.UtteranceProgressListener;
import android.telephony.SmsMessage;

import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

public class VoxPopuliActivity extends Activity implements TextToSpeech.OnInitListener {
	// TAG is used to debug in Android logcat console
	private static final String TAG = "!!!VOXPOP!!! ";
	private static final String TTS_ENGINE_PACKAGE_NAME = "com.svox.classic";

	private TextToSpeech mTTS = null;
	private SMSReceiver mSMS = null;
	private TwitterStream mTwitterStream = null;
	private MediaPlayer mAudioPlayer = null;

	private String smsMessageText = "";

	// queue for messages
	private Queue<String> msgQueue = null;

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
					Log.d(TAG, "Got sms: "+smsMessageText+" from: "+phoneNum);

					// only write if it's from a real number
					if(phoneNum.length() > 5) {
						// clean up the @/# if it's there...
						smsMessageText = smsMessageText.replaceAll("[@#]?", "");
						smsMessageText = smsMessageText.replaceAll("[():]+", "");

						// add to queue
						Log.d(TAG, "Adding: ("+smsMessageText+") to queue");
						msgQueue.offer(smsMessageText);
						checkQueues();
					}
				}
			}
		}
	}

	StatusListener mTwitterStatusListener = new StatusListener(){
        public void onStatus(Status status) {
			String twitterMessageText = status.getText();

			// clean up the @/# if it's there...
			twitterMessageText = twitterMessageText.replaceAll("[@#]?", "");
			twitterMessageText = twitterMessageText.replaceAll("[():]+", "");

			// add to queue
			Log.d(TAG, "Adding: ("+twitterMessageText+") to queue");
			msgQueue.offer(twitterMessageText);
			checkQueues();
        }
        public void onScrubGeo(long userId, long upToStatusId) {}
        public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {}
        public void onTrackLimitationNotice(int numberOfLimitedStatuses) {}
        public void onStallWarning(StallWarning warning){}
        public void onException(Exception ex) { ex.printStackTrace();}
    };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// play silence
		playSilence();

		// FFQ
		mAudioPlayer = (mAudioPlayer == null)?(new MediaPlayer()):mAudioPlayer;
		msgQueue = (msgQueue == null)?(new LinkedList<String>()):msgQueue;

		mTTS = (mTTS == null)?(new TextToSpeech(this, this, TTS_ENGINE_PACKAGE_NAME)):mTTS;

		mSMS = (mSMS == null)?(new SMSReceiver()):mSMS;
		registerReceiver(mSMS, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));

		mTwitterStream = (mTwitterStream == null)?(new TwitterStreamFactory().getInstance()):mTwitterStream;
		mTwitterStream.addListener(mTwitterStatusListener);
		mTwitterStream.filter("pizza", "jesus");

		checkQueues();

		// GUI
		setContentView(R.layout.main);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mSMS);
		if(mTTS != null) mTTS.shutdown();
		if (mAudioPlayer != null) mAudioPlayer.release();
		super.onDestroy();
	}

	// from OnInitListener interface
	public void onInit(int status){
		// set the language for tts (this is the value for Luciana)
		// TODO: en_UK ??
		mTTS.setLanguage(new Locale("pt_BR"));

		// slow her down a little...
		mTTS.setSpeechRate(0.66f);
		mTTS.setPitch((new Random()).nextFloat()*0.5f+0.5f);

		// attach listener
		mTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
			@Override
			public void onStart(String utteranceId) {}
			@Override
			public void onError(String utteranceId) {}
			@Override
			public void onDone(String utteranceId) {
				playSilence();
				// check if there are other things to be said
				VoxPopuliActivity.this.checkQueues();
			}
		});
		Log.d(TAG, "TTS ready! "+mTTS.getLanguage().toString());
	}

	private void checkQueues(){
		// if already doing something, return
		if(mTTS.isSpeaking()){
			return;
		}

		// check queue for new messages
		if(msgQueue.peek() != null){
			Log.d(TAG, "There's message in queue. Playing!");
			playMessage(msgQueue.poll());
		}
	}

	private void playSilence(){
		if(mAudioPlayer != null) mAudioPlayer.release();
		mAudioPlayer = new MediaPlayer();
		mAudioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try{
			mAudioPlayer.setDataSource(getAssets().openFd("silence.mp3").getFileDescriptor());
		}
		catch(IOException e){}
		mAudioPlayer.setLooping(true);
		mAudioPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				Log.d(TAG, "silent media prepared");
				mp.start();
			}
		});
		mAudioPlayer.prepareAsync();
	}

	private void playMessage(String msg){
		Log.d(TAG, "playing TTS message");
		HashMap<String,String> foo = new HashMap<String,String>();
		foo.put(Engine.KEY_PARAM_UTTERANCE_ID, "1234");
		// stop silence
		mAudioPlayer.pause();
		mAudioPlayer.stop();
		if(mAudioPlayer != null) mAudioPlayer.release();
		// pause before and afterwards.
		mTTS.speak(". . "+msg+" . . ", TextToSpeech.QUEUE_ADD, foo);
	}

	public void testMegaphone(View v){
		String msg = "testando o megafone. não é?";
		((LinkedList<String>)msgQueue).addFirst(msg);
		checkQueues();
	}

	public void quitActivity(View v){
		finish();
	}
}

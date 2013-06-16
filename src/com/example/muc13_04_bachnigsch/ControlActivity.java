package com.example.muc13_04_bachnigsch;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.ModelUtil;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.UDAServiceType;
import org.teleal.cling.support.avtransport.callback.GetPositionInfo;
import org.teleal.cling.support.avtransport.callback.Pause;
import org.teleal.cling.support.avtransport.callback.Play;
import org.teleal.cling.support.avtransport.callback.Seek;
import org.teleal.cling.support.avtransport.callback.Stop;
import org.teleal.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.teleal.cling.support.avtransport.lastchange.AVTransportVariable;
import org.teleal.cling.support.avtransport.lastchange.AVTransportVariable.CurrentTrackMetaData;
import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.lastchange.LastChange;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.PositionInfo;
import org.teleal.cling.support.model.SeekMode;
import org.teleal.cling.support.model.TransportState;
import org.teleal.cling.support.model.item.Item;
import org.teleal.cling.support.model.item.MusicTrack;
import org.teleal.cling.support.renderingcontrol.callback.GetVolume;
import org.teleal.cling.support.renderingcontrol.callback.SetMute;
import org.teleal.cling.support.renderingcontrol.callback.SetVolume;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.example.muc13_04_bachnigsch.callback.Next;
import com.example.muc13_04_bachnigsch.callback.Previous;
import com.example.muc13_04_bachnigsch.helper.AppData;
import com.example.muc13_04_bachnigsch.helper.MyUpnpServiceImpl;

public class ControlActivity extends Activity implements OnSeekBarChangeListener {



	@Override
	protected void onPause() {
		super.onPause();
		
		if (null != mTimer){
				mTimerTask.cancel();
				mTimer.cancel();
				mTimer = null;
		}		
	}



	@Override
	protected void onResume() {
		super.onResume();
		
		if (null == mTimer){
			mTimer = new Timer();
			mTimer.scheduleAtFixedRate(mTimerTask, 0, 1000);
		}
	}

	public static final String TAG = ControlActivity.class.getName();
	private Device mDevice = null;
	private AndroidUpnpService mUpnpService;
	private Service mAVService = null;
	private Service rcService = null;
	private int volume;
	private boolean mute_state = false;
	private Animation anim = new AlphaAnimation(0.0f, 1.0f);
	private String absTime;
	private int progress;
	

	private SubscriptionCallback mSubscriptionCallback;
	//private SubscriptionCallback mSubscriptionCallback2;

	private ServiceConnection mServiceConnection = new ServiceConnection() {
	
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.e(TAG, "Service disconnected");
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mUpnpService = (AndroidUpnpService) service; // get service binder
			
			// erstmaliges lautstaerke abfragen
			getVolume();
			
			if (null != mSubscriptionCallback)
				mUpnpService.getControlPoint().execute(mSubscriptionCallback);
		}
	};

	private TextView mPlayingText;
	private TextView mTitleText;
	private TextView mArtistText;
	private TextView mAlbumText;
	private TextView mVolumeText;
	private TextView mTimeText;
	private Button mPlayButton;
	private Button mStopButton;
	private Button mMuteButton;
	private SeekBar mSeekBar;
	private MusicTrack mCurrentTrack = null; // holds current Track
	private Date mCurrentDuration = null;
	private TransportState mCurrentTransportState = null;
	
	private Timer mTimer = null;
	
	
	private TimerTask mTimerTask = new TimerTask(){

		@Override
		public void run() {
			ActionCallback positionAction = new GetPositionInfo(mAVService) {
				@Override
				public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2) {
					if (BuildConfig.DEBUG)
						Log.d(TAG, arg1.getResponseDetails());
				}
				@Override
				public void received(ActionInvocation arg0, PositionInfo position) {
					//System.out.println("Position: " + Integer.toString(position.getElapsedPercent()));
					mSeekBar.setProgress(position.getElapsedPercent());	
					absTime = position.getAbsTime();
				}
			};
			
			if(mUpnpService != null){
				mUpnpService.getControlPoint().execute(positionAction);
			}
			
			
			
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					updateView();

				}
			});
			
		}
		
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_control);
		// Show the Up button in the action bar.
		setupActionBar();

		// find TextViews
		mPlayingText = (TextView) findViewById(R.id.playingText);
		mTitleText = (TextView) findViewById(R.id.titleText);
		mArtistText = (TextView) findViewById(R.id.artistText);
		mAlbumText = (TextView) findViewById(R.id.albumText);
		mVolumeText = (TextView) findViewById(R.id.volumeText);
		mTimeText = (TextView) findViewById(R.id.timeText);
		// find buttons
		mPlayButton = (Button) findViewById(R.id.playButton);
		mStopButton = (Button) findViewById(R.id.stopButton);
		mMuteButton = (Button) findViewById(R.id.muteButton);
		// find seekbar and set listener
		mSeekBar = (SeekBar)findViewById(R.id.seekBar);
		mSeekBar.setOnSeekBarChangeListener(this);
		

		// get selected device
		mDevice = AppData.getInstance().getCurrentDevice();
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Control: " + mDevice.getDisplayString());

		// bind to service
		getApplicationContext().bindService(
				new Intent(this, MyUpnpServiceImpl.class), mServiceConnection,
				Context.BIND_AUTO_CREATE);

		// get AVTransportService
		mAVService = mDevice.findService(new UDAServiceType("AVTransport"));
		// get RenderingControlService
		rcService = mDevice.findService(new UDAServiceType("RenderingControl"));

		
		// listen for changes
		mSubscriptionCallback = new SubscriptionCallback(mAVService, 600) {

			@Override
			protected void failed(GENASubscription arg0, UpnpResponse arg1,
					Exception arg2, String arg3) {
				Log.e(TAG, createDefaultFailureMessage(arg1, arg2));

			}

			@Override
			protected void eventsMissed(GENASubscription arg0, int arg1) {
				Log.i(TAG, "Missed events: " + arg1);

			}

			@Override
			protected void eventReceived(GENASubscription arg0) {
				Log.i(TAG, "Event: " + arg0.getCurrentSequence().getValue());
								
				try {
					LastChange lastChange = new LastChange(
							new AVTransportLastChangeParser(), arg0
									.getCurrentValues().get("LastChange")
									.toString());
					updateStatus(lastChange);
				} catch (Exception e) {
					e.printStackTrace();
					Log.e(TAG, "Error parsing LastChange");
				}

			}

			@Override
			protected void established(GENASubscription arg0) {
				Log.i(TAG, "Established: " + arg0.getSubscriptionId());

			}

			@Override
			protected void ended(GENASubscription arg0, CancelReason arg1,
					UpnpResponse arg2) {

			}
		};
		
		
		
	}
	

	
	

	private void updateStatus(LastChange lastChange) {

		// extract TrackMetaData
		CurrentTrackMetaData currentTrack = lastChange.getEventedValue(0,
				AVTransportVariable.CurrentTrackMetaData.class);
		if (null != currentTrack) {
			DIDLParser dParser = new DIDLParser();
			try {
				Log.i(TAG, currentTrack.getValue());
				DIDLContent dContent = dParser.parse(currentTrack.getValue());
				List<Item> blubb = dContent.getItems();
				MusicTrack mt = new MusicTrack(blubb.get(0));
				mCurrentTrack = mt;
				if (null != mt.getFirstArtist()){
					Log.i(TAG,
							"CurrentTrack: " + mt.getTitle() + " "
									+ mt.getAlbum() + " "
									+ mt.getFirstArtist().getName());
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// extract TransportState
		AVTransportVariable.TransportState transportState = lastChange
				.getEventedValue(0, AVTransportVariable.TransportState.class);
		if (null != transportState) {
			mCurrentTransportState = transportState.getValue();
			Log.i(TAG, "TransportState: " + mCurrentTransportState.getValue());
			
			
			
		}

		// extract duration of Track
		AVTransportVariable.CurrentTrackDuration currentDuration = lastChange
				.getEventedValue(0,
						AVTransportVariable.CurrentTrackDuration.class);
		if (null != currentDuration) {
			try {
				Date duration = new SimpleDateFormat("HH:mm:ss")
						.parse(currentDuration.getValue());
				if(!duration.equals(new SimpleDateFormat("mm:ss").parse("00:00")))
					mCurrentDuration = new SimpleDateFormat("HH:mm:ss").parse(currentDuration.getValue());
				Log.i(TAG,
						"CurrentDuration: "
								+ new SimpleDateFormat("mm:ss")
										.format(mCurrentDuration));
			} catch (ParseException e) {
				e.printStackTrace();
			}	
		}

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				updateView();

			}
		});
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {

		getActionBar().setDisplayHomeAsUpEnabled(true);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.control, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	// updates stuff on view
	// needs to be called from UI-Thread
	private void updateView() {
		if (null != mCurrentTrack && null != mCurrentDuration && null != mCurrentTrack.getFirstArtist()) {
			mTitleText.setText(mCurrentTrack.getTitle() + " ("+ new SimpleDateFormat("m:ss").format(mCurrentDuration)+ ")");
			mArtistText.setText(mCurrentTrack.getFirstArtist().getName());
			mAlbumText.setText(mCurrentTrack.getAlbum());	
		}

		if (null != mCurrentTransportState) {
			switch (mCurrentTransportState) {
			case PLAYING:
				mPlayButton.setEnabled(true);
				mPlayButton.setText("||");
				mStopButton.setEnabled(true);
				mPlayingText.setText("Now playing: ");
				unblink(mPlayingText);
				unblink(mTitleText);
				unblink(mArtistText);
				unblink(mAlbumText);
				unblink(mVolumeText);
				break;
			case PAUSED_PLAYBACK:
				mPlayButton.setEnabled(true);
				mPlayButton.setText("Play");
				mStopButton.setEnabled(true);
				mPlayingText.setText("Now paused: ");
				blink(mPlayingText);
				blink(mTitleText);
				blink(mArtistText);
				blink(mAlbumText);
				blink(mVolumeText);
				break;
			case STOPPED:
				mPlayButton.setEnabled(true);
				mPlayButton.setText("Play");
				mStopButton.setEnabled(false);
				break;
			default:			
			}
			
		}
		mTimeText.setText(absTime + " (" + Integer.toString(progress) + "%)");
		mVolumeText.setText("Volume: " + volume);
		
		if(mute_state){
			mMuteButton.setText("Unmute");
			mVolumeText.setText("Volume: muted");
		}
		else if (!mute_state) {
			mMuteButton.setText("Mute");
			mVolumeText.setText("Volume: " + volume);
		}
		
	}

	/**
	 * gets called when user presses "Play" Button
	 */
	public void onPlay(View view) {
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Play");

		if (mAVService != null) {

			if (mCurrentTransportState == null
					|| mCurrentTransportState == TransportState.STOPPED
					|| mCurrentTransportState == TransportState.PAUSED_PLAYBACK) {
				ActionCallback playAction = new Play(mAVService) {

					@Override
					public void failure(ActionInvocation arg0,
							UpnpResponse arg1, String arg2) {
						if (BuildConfig.DEBUG)
							Log.d(TAG, arg1.getResponseDetails());

					}
				};

				mUpnpService.getControlPoint().execute(playAction);
			}

			if (null != mCurrentTransportState
					&& mCurrentTransportState == TransportState.PLAYING) {
				ActionCallback playAction = new Pause(mAVService) {

					@Override
					public void failure(ActionInvocation arg0,
							UpnpResponse arg1, String arg2) {
						if (BuildConfig.DEBUG)
							Log.d(TAG, arg1.getResponseDetails());

					}
				};

				mUpnpService.getControlPoint().execute(playAction);
			}
		}
	}

	/**
	 * gets called when user presses "Stop" Button
	 */
	public void onStop(View view) {
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Stop");

		if (mAVService != null) {

			ActionCallback stopAction = new Stop(mAVService) {

				@Override
				public void failure(ActionInvocation arg0, UpnpResponse arg1,
						String arg2) {
					if (BuildConfig.DEBUG)
						Log.d(TAG, arg1.getResponseDetails());
				}
			};

			mUpnpService.getControlPoint().execute(stopAction);
		}
	}

/**
	 * gets called when user presses "<<" Button
	 */
	public void onPrevious(View view) {
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Previous");

		ActionCallback previousAction = new Previous(mAVService) {

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1,
					String arg2) {
				if (BuildConfig.DEBUG)
					Log.d(TAG, arg1.getResponseDetails());
			}
		};

		mUpnpService.getControlPoint().execute(previousAction);
	}

	/**
	 * gets called when user presses ">>" Button
	 */
	public void onNext(View view) {
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Next");

		ActionCallback nextAction = new Next(mAVService) {

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1,
					String arg2) {
				if (BuildConfig.DEBUG)
					Log.d(TAG, arg1.getResponseDetails());

			}
		};

		mUpnpService.getControlPoint().execute(nextAction);
	}
	
	

	
	
	
	public void getVolume(){
		// Get Volume out of RenderingControlService
		ActionCallback volumeAction = new GetVolume(rcService){
			@Override
			public void received(ActionInvocation actioonInvocation, int receivedVolume) {
				System.out.println("GetVolume holt vom Service: " + receivedVolume);
				volume = receivedVolume;
			}

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2) {
				if (BuildConfig.DEBUG)
					Log.d(TAG, arg1.getResponseDetails());	
			}
		};
		
		mUpnpService.getControlPoint().execute(volumeAction);
	}
	
	
	
	/**
	 * gets called when user presses "Plus" Button
	 */
	public void onPlus(View view) {
		
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Plus");
		if (rcService != null) {

			getVolume();
			
			volume +=5;
			ActionCallback plusAction = new SetVolume(rcService, Long.valueOf(volume)) {
				@Override
				public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2) {
					if (BuildConfig.DEBUG)
						Log.d(TAG, arg1.getResponseDetails());	
				}
			};
		

			mUpnpService.getControlPoint().execute(plusAction);
			
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					updateView();

				}
			});

		}
	}
	

	
	
	
	public void onMinus(View view) {
		
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Minus");
		if (rcService != null) {

			getVolume();
			
			volume -=5;
			ActionCallback minusAction = new SetVolume(rcService, Long.valueOf(volume)) {
				@Override
				public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2) {
					if (BuildConfig.DEBUG)
						Log.d(TAG, arg1.getResponseDetails());	
				}
			};
			
			mUpnpService.getControlPoint().execute(minusAction);
			
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					updateView();

				}
			});
		}
	}
	
	
	
	public void onMute(View view) {
		
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Mute");
		if (rcService != null) {


			
			ActionCallback setMuteAction = new SetMute(rcService, !mute_state) {

				@Override
				public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2) {
					if (BuildConfig.DEBUG)
						Log.d(TAG, arg1.getResponseDetails());		
				}	
			};

			mUpnpService.getControlPoint().execute(setMuteAction);
					
			if(mute_state){
				mute_state = false;
			}
			else if (!mute_state) {
				mute_state = true;
			}
			
			
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					updateView();

				}
			});
		}
	}


	public void blink(TextView textView){
		anim.setDuration(500); //You can manage the time of the blink with this parameter
		anim.setStartOffset(20);
		anim.setRepeatMode(Animation.REVERSE);
		anim.setRepeatCount(Animation.INFINITE);
		textView.startAnimation(anim);
	}
	
	public void unblink(TextView textView){
		textView.clearAnimation();
	}

	
	
	

	
	
	/**
	 *  SeekBar Stuff
	 */
	

	@Override
	public void onProgressChanged(SeekBar seekBar, final int progress_received, boolean fromUser) {
		
		ActionCallback positionAction = new GetPositionInfo(mAVService) {
			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2) {
				if (BuildConfig.DEBUG)
					Log.d(TAG, arg1.getResponseDetails());
			}
			@Override
			public void received(ActionInvocation arg0, PositionInfo position) {
				//System.out.println("Position: " + Integer.toString(position.getElapsedPercent()));
				mSeekBar.setProgress(position.getElapsedPercent());	
				absTime = position.getAbsTime();
			}
		};
		mUpnpService.getControlPoint().execute(positionAction);
		
		progress = progress_received;
		
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				mTimeText.setText(absTime + " (" + Integer.toString(progress) + "%)");
			}

		
		
		});
		
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		
		int aimPosition = seekBar.getProgress();	
		int durationTime = 60 * mCurrentDuration.getMinutes() + mCurrentDuration.getSeconds();
		int jumpTime = durationTime * aimPosition / 100;
		
		
		// In benoetigtes Format umwandeln
		String targetTime = ModelUtil.toTimeString(new Long(Math.round(jumpTime)).intValue());

		ActionCallback seekAction = new Seek(mAVService, SeekMode.REL_TIME, targetTime) {

			@Override
			public void failure(ActionInvocation arg0, UpnpResponse arg1,
					String arg2) {
				if (BuildConfig.DEBUG)
					Log.d(TAG, arg1.getResponseDetails());
			}
		};
		mUpnpService.getControlPoint().execute(seekAction);
		
	}

}

package com.example.muc13_04_bachnigsch;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.controlpoint.SubscriptionCallback;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.gena.CancelReason;
import org.teleal.cling.model.gena.GENASubscription;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.UDAServiceType;
import org.teleal.cling.support.avtransport.callback.Pause;
import org.teleal.cling.support.avtransport.callback.Play;
import org.teleal.cling.support.avtransport.callback.Stop;
import org.teleal.cling.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.teleal.cling.support.avtransport.lastchange.AVTransportVariable;
import org.teleal.cling.support.avtransport.lastchange.AVTransportVariable.CurrentTrackMetaData;
import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.lastchange.LastChange;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.TransportState;
import org.teleal.cling.support.model.item.Item;
import org.teleal.cling.support.model.item.MusicTrack;

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
import android.widget.Button;
import android.widget.TextView;

import com.example.muc13_04_bachnigsch.callback.Next;
import com.example.muc13_04_bachnigsch.callback.Previous;
import com.example.muc13_04_bachnigsch.helper.AppData;
import com.example.muc13_04_bachnigsch.helper.MyUpnpServiceImpl;

public class ControlActivity extends Activity {

	public static final String TAG = ControlActivity.class.getName();
	private Device mDevice = null;
	private AndroidUpnpService mUpnpService;
	private Service mAVService = null;
	private SubscriptionCallback mSubscriptionCallback;
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.e(TAG, "Service disconnected");

		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mUpnpService = (AndroidUpnpService) service; // get service binder

			if (null != mSubscriptionCallback)
				mUpnpService.getControlPoint().execute(mSubscriptionCallback);
		}
	};

	private TextView mTitleText;
	private TextView mArtistText;
	private TextView mAlbumText;
	private Button mPlayButton;
	private Button mStopButton;
	private MusicTrack mCurrentTrack = null; // holds current Track
	private Date mCurrentDUration = null;
	private TransportState mCurrentTransportState = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_control);
		// Show the Up button in the action bar.
		setupActionBar();

		// find TextViews
		mTitleText = (TextView) findViewById(R.id.titleText);
		mArtistText = (TextView) findViewById(R.id.artistText);
		mAlbumText = (TextView) findViewById(R.id.albumText);
		// find buttons
		mPlayButton = (Button) findViewById(R.id.playButton);
		mStopButton = (Button) findViewById(R.id.stopButton);

		// get selected device
		mDevice = AppData.getInstance().getCurrentDevice();
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Control: " + mDevice.getDisplayString());

		// bind to service
		getApplicationContext().bindService(
				new Intent(this, MyUpnpServiceImpl.class), mServiceConnection,
				Context.BIND_AUTO_CREATE);

		// TODO: unbind service and take care of all that stuff

		// get AVTransportService
		mAVService = mDevice.findService(new UDAServiceType("AVTransport"));

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
				if (null != mt.getFirstArtist())
					Log.i(TAG,
							"CurrentTrack: " + mt.getTitle() + " "
									+ mt.getAlbum() + " "
									+ mt.getFirstArtist().getName());
			} catch (Exception e) {
				// TODO Auto-generated catch block
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
					mCurrentDUration = new SimpleDateFormat("HH:mm:ss").parse(currentDuration.getValue());
				Log.i(TAG,
						"CurrentDuration: "
								+ new SimpleDateFormat("mm:ss")
										.format(mCurrentDUration));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
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
		if (null != mCurrentTrack && null != mCurrentDUration
				&& null != mCurrentTrack.getFirstArtist()) {
			mTitleText.setText(mCurrentTrack.getTitle() + " ("
					+ new SimpleDateFormat("m:ss").format(mCurrentDUration)
					+ ")");
			mArtistText.setText(mCurrentTrack.getFirstArtist().getName());
			mAlbumText.setText(mCurrentTrack.getAlbum());
		}

		if (null != mCurrentTransportState) {
			switch (mCurrentTransportState) {
			case PLAYING:
				mPlayButton.setEnabled(true);
				mPlayButton.setText("||");
				mStopButton.setEnabled(true);
				break;
			case PAUSED_PLAYBACK:
				mPlayButton.setEnabled(true);
				mPlayButton.setText("Play");
				mStopButton.setEnabled(true);
				break;
			case STOPPED:
				mPlayButton.setEnabled(true);
				mPlayButton.setText("Play");
				mStopButton.setEnabled(false);
				break;
			default:
			}
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

}

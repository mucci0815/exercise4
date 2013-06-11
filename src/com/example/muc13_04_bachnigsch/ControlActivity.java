package com.example.muc13_04_bachnigsch;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.UDAServiceType;
import org.teleal.cling.support.avtransport.callback.Play;
import org.teleal.cling.support.avtransport.callback.Stop;

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

import com.example.muc13_04_bachnigsch.callback.Next;
import com.example.muc13_04_bachnigsch.callback.Previous;
import com.example.muc13_04_bachnigsch.helper.AppData;
import com.example.muc13_04_bachnigsch.helper.MyUpnpServiceImpl;

public class ControlActivity extends Activity {

	public static final String TAG = ControlActivity.class.getName();
	private Device mDevice = null;
	private AndroidUpnpService mUpnpService;
	private Service mAVService = null;
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.e(TAG, "Service disconnected");

		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mUpnpService = (AndroidUpnpService) service; // get service binder

		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_control);
		// Show the Up button in the action bar.
		setupActionBar();

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

	/**
	 * gets called when user presses "Play" Button
	 */
	public void onPlay(View view) {
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Play");

		if (mAVService != null) {

			ActionCallback playAction = new Play(mAVService) {

				@Override
				public void failure(ActionInvocation arg0, UpnpResponse arg1,
						String arg2) {
					if (BuildConfig.DEBUG)
						Log.d(TAG, arg1.getResponseDetails());

				}
			};

			mUpnpService.getControlPoint().execute(playAction);
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
			Log.d(TAG, "Stop");

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
			Log.d(TAG, "Stop");

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

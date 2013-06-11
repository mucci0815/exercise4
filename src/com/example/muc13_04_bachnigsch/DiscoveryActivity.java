package com.example.muc13_04_bachnigsch;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.android.AndroidUpnpServiceImpl;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.registry.DefaultRegistryListener;
import org.teleal.cling.registry.Registry;

import com.example.muc13_04_bachnigsch.helper.AppData;
import com.example.muc13_04_bachnigsch.helper.MyUpnpServiceImpl;

import android.app.ListActivity;
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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class DiscoveryActivity extends ListActivity {

	public static final String TAG = DiscoveryActivity.class.getName();

	private ArrayAdapter<DeviceDisplay> mListAdapter;
	private AndroidUpnpService mUpnpService;
	private BrowseRegistryListener mRegistryListener = new BrowseRegistryListener();
	private ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mUpnpService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mUpnpService = (AndroidUpnpService) service;

			// refresh list with known devices
			mListAdapter.clear();
			for (Device device : mUpnpService.getRegistry().getDevices()) {
				mRegistryListener.deviceAdded(device);
			}

			// getting ready for future device advertisements
			mUpnpService.getRegistry().addListener(mRegistryListener);

			// search asynchronously
			mUpnpService.getControlPoint().search();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_discovery);
		// Show the Up button in the action bar.
		setupActionBar();

		mListAdapter = new ArrayAdapter<DeviceDisplay>(this,
				android.R.layout.simple_list_item_1);
		setListAdapter(mListAdapter);

		getApplicationContext().bindService(
				new Intent(this, MyUpnpServiceImpl.class), mServiceConnection,
				Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mUpnpService != null) {
			mUpnpService.getRegistry().removeListener(mRegistryListener);
		}

		getApplicationContext().unbindService(mServiceConnection);
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		// pause UPnP service's registry
		if(null != mUpnpService)
			mUpnpService.getRegistry().pause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		// resume UPnP service's registry
		if(null != mUpnpService && mUpnpService.getRegistry().isPaused())
			mUpnpService.getRegistry().resume();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if(BuildConfig.DEBUG)
			Log.d(TAG, "Clicked: "+mListAdapter.getItem(position).toString());
		
		// start new ControlActivity and pass device
		AppData.getInstance().setCurrentDevice(mListAdapter.getItem(position).getDevice());
		startActivity(new Intent(this, ControlActivity.class));
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
//		getMenuInflater().inflate(R.menu.discovery, menu);
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

	protected class DeviceDisplay {
		Device device;

		public DeviceDisplay(Device device) {
			this.device = device;
		}

		public Device getDevice() {
			return device;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			DeviceDisplay that = (DeviceDisplay) o;
			return device.equals(that.device);
		}

		@Override
		public int hashCode() {
			return device.hashCode();
		}

		@Override
		public String toString() {
			// Display a little star while the device is being loaded
			return device.isFullyHydrated() ? device.getDisplayString()
					: device.getDisplayString() + " *";
		}
	}

	class BrowseRegistryListener extends DefaultRegistryListener {

		@Override
		public void remoteDeviceDiscoveryStarted(Registry registry,
				RemoteDevice device) {
			deviceAdded(device);
		}

		@Override
		public void remoteDeviceDiscoveryFailed(Registry registry,
				final RemoteDevice device, final Exception ex) {
			runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(
							DiscoveryActivity.this,
							"Discovery failed of '"
									+ device.getDisplayString()
									+ "': "
									+ (ex != null ? ex.toString()
											: "Couldn't retrieve device/service descriptors"),
							Toast.LENGTH_LONG).show();
				}
			});
			deviceRemoved(device);
		}

		@Override
		public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
			deviceAdded(device);
		}

		@Override
		public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
			deviceRemoved(device);
		}

		@Override
		public void localDeviceAdded(Registry registry, LocalDevice device) {
			deviceAdded(device);
		}

		@Override
		public void localDeviceRemoved(Registry registry, LocalDevice device) {
			deviceRemoved(device);
		}

		public void deviceAdded(final Device device) {
			runOnUiThread(new Runnable() {
				public void run() {
					DeviceDisplay d = new DeviceDisplay(device);
					int position = mListAdapter.getPosition(d);
					if (position >= 0) {
						// Device already in the list, re-set new value at same
						// position
						mListAdapter.remove(d);
						mListAdapter.insert(d, position);
					} else {
						mListAdapter.add(d);
					}
				}
			});
		}

		public void deviceRemoved(final Device device) {
			runOnUiThread(new Runnable() {
				public void run() {
					mListAdapter.remove(new DeviceDisplay(device));
				}
			});
		}
	}

}

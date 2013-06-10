package com.example.muc13_04_bachnigsch;

import org.teleal.cling.android.AndroidUpnpService;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.registry.RegistryListener;

import com.example.muc13_04_bachnigsch.helper.MyUpnpServiceImpl;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

public class DiscoveryActivity extends ListActivity {
	
	public static final String TAG = DiscoveryActivity.class.getName();
	
	private ArrayAdapter<DeviceDisplay> mListAdapter;
	private AndroidUpnpService mUpnpService;
	private RegistryListener mRegistryListener;
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
			for(Device device : mUpnpService.getRegistry().getDevices()) {
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
//		setContentView(R.layout.activity_discovery);
		// Show the Up button in the action bar.
		setupActionBar();
		
		mListAdapter = new ArrayAdapter<DeviceDisplay>(this, android.R.layout.simple_list_item_1);
		setListAdapter(mListAdapter);
		
		getApplicationContext().bindService(
				new Intent(this, MyUpnpServiceImpl.class), mServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if(mUpnpService != null) {
			mUpnpService.getRegistry().removeListener(mRegistryListener);
		}
		
		getApplicationContext().unbindService(mServiceConnection);
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
		getMenuInflater().inflate(R.menu.discovery, menu);
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
	        if (this == o) return true;
	        if (o == null || getClass() != o.getClass()) return false;
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
	        return device.isFullyHydrated() ? device.getDisplayString() : device.getDisplayString() + " *";
	    }
	}

}

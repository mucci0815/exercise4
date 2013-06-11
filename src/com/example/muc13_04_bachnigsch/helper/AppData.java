/**
 * 
 */
package com.example.muc13_04_bachnigsch.helper;

import org.teleal.cling.model.meta.Device;

/**
 * @author Martin Bach
 * @author Maximilian Nigsch
 *
 */
public class AppData {
	private static AppData mInstance = null;
	private Device mDevice = null;
	
	private AppData() {}
	
	public static AppData getInstance() {
		if(mInstance == null)
			mInstance = new AppData();
		return mInstance;
	}
	
	public Device getCurrentDevice() {
		return mDevice;
	}
	
	public void setCurrentDevice(Device device) {
		mDevice = device;
	}
}

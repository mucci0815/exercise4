/**
 * 
 */
package com.example.muc13_04_bachnigsch.helper;

import org.teleal.cling.android.AndroidUpnpServiceConfiguration;
import org.teleal.cling.android.AndroidUpnpServiceImpl;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UDAServiceType;

import android.net.wifi.WifiManager;

/**
 * Own implementation of UPnP Service, extends AndroidUpnpServiceImpl
 * 
 * overrides crreateConfiguration so only desired devices are found
 * 
 * @author Martin Bach
 * @author Maximilian Nigsch
 *
 */
public class MyUpnpServiceImpl extends AndroidUpnpServiceImpl {

	@Override
	protected AndroidUpnpServiceConfiguration createConfiguration(
			WifiManager wifiManager) {
		
		return new AndroidUpnpServiceConfiguration(wifiManager) {

			@Override
			public ServiceType[] getExclusiveServiceTypes() {
				return new ServiceType[] {
						new UDAServiceType("AVTransport"),
						new UDAServiceType("RenderingControl")// we only want to display AVTransport services
				};
			}
			
		};
	}

}

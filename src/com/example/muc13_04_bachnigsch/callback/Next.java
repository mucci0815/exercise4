/**
 * 
 */
package com.example.muc13_04_bachnigsch.callback;

import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.UnsignedIntegerFourBytes;

/**
 * @author Martin Bach
 * @author Maximilian Nigsch
 *
 */
public abstract class Next extends ActionCallback {

	public Next(Service service) {
		super(new ActionInvocation(service.getAction("Next")));
		getActionInvocation().setInput("InstanceID", new UnsignedIntegerFourBytes(0));
	}

	
	@Override
	public void success(ActionInvocation arg0) {
		

	}

}

package de.uhd.ifi.se.pcm.bppcm.resources;

import de.uhd.ifi.se.pcm.bppcm.organizationenvironmentmodel.DeviceResource;
import edu.kit.ipd.sdq.eventsim.api.IRequest;
import edu.kit.ipd.sdq.eventsim.api.Procedure;

public interface IDeviceResource {

	void acquire(IRequest request, DeviceResource passiveResouce, int num,
	            Procedure onGrantedCallback);
	boolean release(IRequest request, DeviceResource passiveResouce, int num);
}

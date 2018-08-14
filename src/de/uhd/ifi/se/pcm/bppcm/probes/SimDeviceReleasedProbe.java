package de.uhd.ifi.se.pcm.bppcm.probes;

import de.uhd.ifi.se.pcm.bppcm.probes.configurations.SimDeviceResourceProbeConfiguration;
import de.uhd.ifi.se.pcm.bppcm.resources.SimDeviceResource;
import edu.kit.ipd.sdq.eventsim.measurement.Measurement;
import edu.kit.ipd.sdq.eventsim.measurement.MeasuringPoint;
import edu.kit.ipd.sdq.eventsim.measurement.annotation.Probe;
import edu.kit.ipd.sdq.eventsim.measurement.probe.AbstractProbe;
import edu.kit.ipd.sdq.eventsim.resources.entities.SimPassiveResource;
import edu.kit.ipd.sdq.eventsim.resources.entities.SimulatedProcess;
import edu.kit.ipd.sdq.eventsim.resources.listener.IPassiveResourceListener;

@Probe(type = SimDeviceResource.class, property = "release_time")
public class SimDeviceReleasedProbe extends AbstractProbe<SimDeviceResource, SimDeviceResourceProbeConfiguration>{

	public SimDeviceReleasedProbe(MeasuringPoint<SimDeviceResource> p,
			SimDeviceResourceProbeConfiguration configuration) {
		super(p, configuration);
		
		SimDeviceResource resource = p.getElement();
		resource.addListener(new IPassiveResourceListener() {
			
			@Override
			public void request(SimulatedProcess process, long num) {
				// nothing to do
			}

			@Override
			public void acquire(SimulatedProcess process, long num) {
				// nothing to do
			}

			@Override
			public void release(SimulatedProcess process, long num) {
				double simTime = process.getModel().getSimulationControl().getCurrentSimulationTime();

				Measurement<SimDeviceResource> m = new Measurement<>("CURRENT_TIME",
						getMeasuringPoint(), process, simTime, simTime);

				// store
				cache.put(m);

				// notify
				measurementListener.forEach(l -> l.notify(m));
			}
		});
	}

}

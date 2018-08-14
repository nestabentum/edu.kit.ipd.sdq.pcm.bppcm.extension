package de.uhd.ifi.se.pcm.bppcm.probes;

import de.uhd.ifi.se.pcm.bppcm.probes.configurations.SimDeviceResourceProbeConfiguration;
import de.uhd.ifi.se.pcm.bppcm.resources.SimDeviceResource;
import edu.kit.ipd.sdq.eventsim.measurement.Measurement;
import edu.kit.ipd.sdq.eventsim.measurement.MeasuringPoint;
import edu.kit.ipd.sdq.eventsim.measurement.annotation.Probe;
import edu.kit.ipd.sdq.eventsim.measurement.probe.AbstractProbe;
import edu.kit.ipd.sdq.eventsim.resources.entities.SimulatedProcess;
import edu.kit.ipd.sdq.eventsim.resources.listener.IPassiveResourceListener;

@Probe(type = SimDeviceResource.class, property = "resource_demand")
public class SimDeviceResourceDemandProbe extends AbstractProbe<SimDeviceResource, SimDeviceResourceProbeConfiguration>{

	public SimDeviceResourceDemandProbe(MeasuringPoint<SimDeviceResource> p,
			SimDeviceResourceProbeConfiguration configuration) {
		super(p, configuration);
		
		SimDeviceResource resource = p.getElement();
		resource.addListener(new IPassiveResourceListener() {

            @Override
            public void request(SimulatedProcess process, long num) {
                // build measurement
                double simTime = resource.getModel().getSimulationControl().getCurrentSimulationTime();
                Measurement<SimDeviceResource> m = new Measurement<>("RESOURCE_DEMAND", getMeasuringPoint(), process,
                        num, simTime);

                // notify
                measurementListener.forEach(l -> l.notify(m));
            }

            @Override
            public void release(SimulatedProcess process, long num) {
                // not relevant for this probe
            }

            @Override
            public void acquire(SimulatedProcess process, long num) {
                // not relevant for this probe
            }
		});
	}
	
}

package de.uhd.ifi.se.pcm.bppcm.probes;

import org.palladiosimulator.pcm.usagemodel.AbstractUserAction;

import edu.kit.ipd.sdq.eventsim.interpreter.listener.ITraversalListener;
import edu.kit.ipd.sdq.eventsim.measurement.Measurement;
import edu.kit.ipd.sdq.eventsim.measurement.MeasuringPoint;
import edu.kit.ipd.sdq.eventsim.measurement.annotation.Probe;
import edu.kit.ipd.sdq.eventsim.measurement.probe.AbstractProbe;
import edu.kit.ipd.sdq.eventsim.workload.WorkloadMeasurementConfiguration;
import edu.kit.ipd.sdq.eventsim.workload.entities.User;
/**
 * This class probe's the exit time of actor steps.
 * @author Nesta Bentum
 *
 * @param <E> The type that is probed.
 */
@Probe(type = AbstractUserAction.class, property = "after")
public class ActorStepExitProbe<E extends AbstractUserAction> extends AbstractProbe<E, WorkloadMeasurementConfiguration> {

	public ActorStepExitProbe(MeasuringPoint<E> p, WorkloadMeasurementConfiguration cfg) {
		super(p, cfg);
		
		configuration.getWorkloadModel().getTraversalListeners().addTraversalListener(getMeasuringPoint().getElement(), new ITraversalListener<AbstractUserAction, User>() {
			
			@Override
			public void before(AbstractUserAction action, User user) {
				// TODO Auto-generated method stub
				  // process the currently observed measurement only when it originates from a
                // measurement context
                // equal to or more specific than this probe's measurement context.
                if (!p.equalsOrIsMoreSpecific(getMeasuringPoint())) {
                    return;
                }

                // build measurement
                double simTime = configuration.getSimulationModel().getSimulationControl().getCurrentSimulationTime();
                Measurement<E> m = new Measurement<>("CURRENT_TIME", getMeasuringPoint(), user, simTime,
                        simTime);

                // store
                cache.put(m);

                // notify
                measurementListener.forEach(l -> l.notify(m));
			}
			
			@Override
			public void after(AbstractUserAction action, User user) {
				// TODO Auto-generated method stub
				
			}
		});
	}

}

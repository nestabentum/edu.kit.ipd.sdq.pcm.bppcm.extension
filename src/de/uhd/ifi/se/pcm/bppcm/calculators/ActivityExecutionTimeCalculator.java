package de.uhd.ifi.se.pcm.bppcm.calculators;

import de.uhd.ifi.se.pcm.bppcm.bpusagemodel.Activity;

import edu.kit.ipd.sdq.eventsim.measurement.calculator.AbstractBinaryCalculator;
import edu.kit.ipd.sdq.eventsim.measurement.probe.IProbe;
import edu.kit.ipd.sdq.eventsim.measurement.Measurement;
import edu.kit.ipd.sdq.eventsim.measurement.MeasuringPoint;
import edu.kit.ipd.sdq.eventsim.measurement.MeasuringPointPair;
import edu.kit.ipd.sdq.eventsim.measurement.Pair;
import edu.kit.ipd.sdq.eventsim.measurement.annotation.Calculator;
import edu.kit.ipd.sdq.eventsim.measurement.annotation.ProbePair;
/**
 * This calculator calculates the execution time of activities.
 * @author Nesta Bentum
 *
 */
@Calculator(metric = "executiontime_of_activity", type = Pair.class, fromType = Activity.class, toType = Activity.class,
	intendedProbes = {@ProbePair(from = "before", to = "after")})
public class ActivityExecutionTimeCalculator extends AbstractBinaryCalculator<Activity, Activity>  {

	@Override
	public Measurement<Pair<Activity, Activity>> calculate(Measurement<Activity> from, Measurement<Activity> to) {
		double when = to.getWhen();
		double executionTime = to.getValue() - from.getValue();
		
		MeasuringPoint<Pair<Activity, Activity>> mp = new MeasuringPointPair<>(from.getWhere(), to.getWhere(), "executiontime_of_activity", to.getWhere().getContexts());

		return new Measurement<Pair<Activity,Activity>>("EXECUTIONTIME_OF_ACTIVITY", mp, to.getWho(), executionTime, when);
	}

	@Override
	public void setup(IProbe<Activity> from, IProbe<Activity> to) {
		//make sure none of the measurements is null
		if(!(from == null || to == null)) {
		from.enableCaching();
			to.forEachMeasurement(measurement-> {
				notify(calculate(from.getLastMeasurementOf(measurement.getWho()), measurement));
			});
		}
		
	}

}

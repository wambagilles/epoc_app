package fr.emn.premode;

import org.testng.Assert;
import org.testng.annotations.Test;

import fr.emn.premode.Scheduler;
import fr.emn.premode.Planning;
import fr.emn.premode.center.SchedulingProblem;
import fr.emn.premode.objectives.Objective;

public class SchedulerTest {

	/**
	 * test setting the profit to a default value making the system crash.
	 */
	@Test
	public void testBug() {
		SchedulingProblem m = new SchedulingProblem();
		m.server("server").maxPower = 1000;
		m.objective = Objective.PROFITPOWER;
		m.addHPC("a", 0, 2, 2, 2, 10);
		Planning r = Scheduler.solv(m);
		Assert.assertNotNull(r);
	}

	@Test
	public void testSolvingValue() {
		SchedulingProblem m = new SchedulingProblem();
		m.server("server").maxPower = 1000;
		m.nameWeb("w1").add(50, 120);
		m.nbIntervals = 2;
		Assert.assertNotNull(Scheduler.solv(m));
	}
}

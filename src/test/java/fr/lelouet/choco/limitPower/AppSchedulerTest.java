package fr.lelouet.choco.limitPower;

import org.testng.Assert;
import org.testng.annotations.Test;

import fr.lelouet.choco.limitpower.AppScheduler;
import fr.lelouet.choco.limitpower.SchedulingResult;
import fr.lelouet.choco.limitpower.model.Objective;
import fr.lelouet.choco.limitpower.model.SchedulingProblem;

public class AppSchedulerTest {

	/**
	 * test setting the profit to a default value making the system crash.
	 */
	@Test
	public void testBug() {
		SchedulingProblem m = new SchedulingProblem();
		m.server("server").maxPower = 1000;
		m.objective = Objective.PROFITPOWER;
		m.addHPC("a", 0, 2, 2, 2, 10);
		SchedulingResult r = AppScheduler.solv(m);
		Assert.assertNotNull(r);
	}

	@Test
	public void testSolvingValue() {
		SchedulingProblem m = new SchedulingProblem();
		m.server("server").maxPower = 1000;
		m.nameWeb("w1").add(50, 120);
		m.nbIntervals = 2;
		Assert.assertNotNull(AppScheduler.solv(m));
	}
}

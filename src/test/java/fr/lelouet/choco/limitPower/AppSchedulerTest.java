package fr.lelouet.choco.limitPower;

import static fr.lelouet.choco.limitpower.AppScheduler.solv;

import org.testng.Assert;
import org.testng.annotations.Test;

import fr.lelouet.choco.limitpower.AppScheduler;
import fr.lelouet.choco.limitpower.SchedulingModel;
import fr.lelouet.choco.limitpower.SchedulingModel.Objective;
import fr.lelouet.choco.limitpower.SchedulingResult;

public class AppSchedulerTest {

	/**
	 * test setting the profit to a default value making the system crash.
	 */
	@Test
	public void testBug() {
		SchedulingModel m = new SchedulingModel();
		m.server("server").maxPower = 1000;
		m.objective = Objective.PROFIT_POWER;
		m.addHPC("a", 0, 2, 2, 2, 10);
		SchedulingResult r = solv(m);
		Assert.assertNotNull(r);
	}

	@Test
	public void testSolvingValue() {
		SchedulingModel m = new SchedulingModel();
		m.server("server").maxPower = 1000;
		m.nameWeb("w1").add(50, 120);
		m.nbIntervals = 2;
		m.setPower(1000);
		Assert.assertNotNull(AppScheduler.solv(m));
	}
}

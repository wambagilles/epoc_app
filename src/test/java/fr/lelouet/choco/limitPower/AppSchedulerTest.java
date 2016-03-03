package fr.lelouet.choco.limitPower;

import static fr.lelouet.choco.limitpower.AppScheduler.solv;

import org.testng.Assert;
import org.testng.annotations.Test;

import fr.lelouet.choco.limitpower.AppScheduler;
import fr.lelouet.choco.limitpower.Model;
import fr.lelouet.choco.limitpower.Model.Objective;
import fr.lelouet.choco.limitpower.Result;

public class AppSchedulerTest {

	/**
	 * test setting the profit to a default value making the system crash.
	 */
	@Test
	public void testBug() {
		Model m = new Model();
		m.objective = Objective.PROFIT_POWER;
		m.addHPC("a", 0, 2, 2, 2, 10);
		Result r = solv(m);
		Assert.assertNotNull(r);
	}

	@Test
	public void testSolvingValue() {
		Model m = new Model();
		m.nameWeb("w1").add(50, 120);
		m.nbIntervals = 2;
		m.maxPower = 1000;
		Assert.assertNotNull(AppScheduler.solv(m));
	}
}

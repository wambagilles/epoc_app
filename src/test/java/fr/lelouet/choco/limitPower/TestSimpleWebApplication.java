package fr.lelouet.choco.limitPower;

import static fr.lelouet.choco.limitpower.AppScheduler.solv;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import fr.lelouet.choco.limitpower.AppScheduler;
import fr.lelouet.choco.limitpower.SchedulingModel;
import fr.lelouet.choco.limitpower.SchedulingResult;

public class TestSimpleWebApplication {

	AppScheduler s;
	SchedulingResult r;
	SchedulingModel m;

	@BeforeMethod
	public void cleanup() {
		m = new SchedulingModel();
	}

	@Test
	public void testOnePassingWeb() {
		m.nameWeb("a").add(1, 1).add(5, 3);

		m.maxPower = 2;
		m.nbIntervals = 1;
		r = solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.webModes.get("a").get(0).power, 1);
		Assert.assertEquals(r.profit, 1);

		// now we allow 5 power use, so we can have a profit of 3
		m.maxPower = 5;
		m.nbIntervals = 1;
		r = solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.webModes.get("a").get(0).power, 5);
		Assert.assertEquals(r.profit, 3);
	}

	/**
	 * two web apps, with increasing power/benefit both. The first web app (a)
	 * consumes one less power than the second (b), for the same profit.
	 */
	@Test
	public void testConcurrentWebApps() {
		m.nameWeb("a").add(1, 1).add(5, 2).add(17, 6);
		m.nameWeb("b").add(2, 1).add(6, 2).add(18, 6);

		m.maxPower = 3;
		m.nbIntervals = 1;
		r = solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.profit, 2);

		m.maxPower = 7;
		r = solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.profit, 3);

		m.maxPower = 11;
		r = solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.profit, 4);

		m.maxPower = 40;
		r = solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.profit, 12);
	}

}

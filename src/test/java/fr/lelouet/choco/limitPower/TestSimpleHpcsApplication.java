package fr.lelouet.choco.limitPower;

import static fr.lelouet.choco.limitpower.AppScheduler.solv;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import fr.lelouet.choco.limitpower.SchedulingModel;
import fr.lelouet.choco.limitpower.SchedulingResult;
import fr.lelouet.choco.limitpower.model.HPC;

public class TestSimpleHpcsApplication {

	SchedulingResult r;
	SchedulingModel m;

	@BeforeMethod
	public void cleanup() {
		m = new SchedulingModel();
		m.server("server").maxPower = 1000;
	}

	@Test
	public void testSingleIntervalApp() {

		HPC h = new HPC(0, 1, 1, 1, 0);
		m.addHPC("a", h);

		// the HPC task fits perfectly in the 1-duration, 1-power intervals
		m.maxPower = 1;
		m.nbIntervals = 1;
		r = solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.profit, 1, "" + r);
	}

	@Test
	public void testSimpleApp() {

		HPC h = new HPC(0, 2, 2, 1, 1);
		m.addHPC("a", h);

		// the HPC task fits perfectly in the 2-duration, 2-power intervals
		m.maxPower = 2;
		m.nbIntervals = 3;
		r = solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.profit, 1, "" + r);

		// not enough power : profit is 0
		m.maxPower = 1;
		r = solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.profit, 0, "" + r);

		// not enough intervals : profit is 0
		m.maxPower = 2;
		m.nbIntervals = 1;
		r = solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.profit, 0, "" + r);
	}
	
	@Test
	public void testTwoApps() {
		HPC a = new HPC(0, 2, 2, 2, 10);
		m.addHPC("a", a);
		HPC b = new HPC(0, 2, 2, 1, 10);
		m.addHPC("c", b);

		// enough power * duration to fit both at the same time
		m.maxPower = 4;
		m.nbIntervals = 2;
		r = solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.profit, 3, "" + r);

		// enough power * duration to fit both sequentially
		m.maxPower = 2;
		m.nbIntervals = 4;
		r = solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.profit, 3, "" + r);

		// enough power * duration to fit one sequentially
		m.maxPower = 2;
		m.nbIntervals = 2;
		r = solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.profit, 2, "" + r);

	}

}

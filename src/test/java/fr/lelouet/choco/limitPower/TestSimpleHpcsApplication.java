package fr.lelouet.choco.limitPower;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import fr.lelouet.choco.limitpower.AppScheduler;
import fr.lelouet.choco.limitpower.SchedulingResult;
import fr.lelouet.choco.limitpower.model.HPC;
import fr.lelouet.choco.limitpower.model.SchedulingProblem;
import fr.lelouet.choco.limitpower.model.SchedulingProblem.Server;

public class TestSimpleHpcsApplication {

	SchedulingResult r;
	SchedulingProblem m;
	Server server;

	@BeforeMethod
	public void cleanup() {
		m = new SchedulingProblem();
		server = m.server("server");
	}

	@Test
	public void testSingleIntervalApp() {

		HPC h = new HPC(0, 1, 1, 1, 0);
		m.addHPC("a", h);

		// the HPC task fits perfectly in the 1-duration, 1-power intervals
		server.maxPower = 1;
		m.nbIntervals = 1;
		r = AppScheduler.solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.profit, 1, "" + r);
	}

	@Test
	public void testSimpleApp() {

		HPC h = new HPC(0, 2, 2, 1, 0);
		m.addHPC("a", h);

		// the HPC task fits perfectly in the 2-duration, 2-power intervals
		server.maxPower = 2;
		m.nbIntervals = 2;
		r = AppScheduler.solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.profit, 1, "" + r);

		// not enough power : profit is 0
		server.maxPower = 1;
		r = AppScheduler.solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.profit, 0, "" + r);

		// not enough intervals : profit is 0
		server.maxPower = 2;
		m.nbIntervals = 1;
		r = AppScheduler.solv(m);
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
		server.maxPower = 4;
		m.nbIntervals = 2;
		r = AppScheduler.solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.profit, 3, "" + r);

		// enough power * duration to fit both sequentially
		server.maxPower = 2;
		m.nbIntervals = 4;
		r = AppScheduler.solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.profit, 3, "" + r);

		// enough power * duration to fit one sequentially
		server.maxPower = 2;
		m.nbIntervals = 2;
		r = AppScheduler.solv(m);
		Assert.assertNotNull(r);
		Assert.assertEquals(r.profit, 2, "" + r);

	}

}

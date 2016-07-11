package fr.emn.premode;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import fr.emn.premode.Scheduler;
import fr.emn.premode.Planning;
import fr.emn.premode.center.SchedulingProblem;
import fr.emn.premode.objectives.Objective;

/**
 * test the solver with objective being profit+power use
 *
 * @author Guillaume Le Louët
 *
 */
public class TestObjectivePower {

	Planning r;
	SchedulingProblem m;

	@BeforeMethod
	public void cleanup() {
		m = new SchedulingProblem();
		m.server("server").maxPower = 1000;
		m.objective = Objective.PROFITPOWER;
	}

	/**
	 * one app with 1 power and 1 benefit, 2 intervals ; problem with 1 power, but
	 * only one intervals. The best solution is to schedule the app on the one
	 * interval
	 */
	@Test
	public void testOneAppOneInterval() {
		m.setPower(0, 1);
		m.nbIntervals = 1;
		m.addHPC("a", 0, 2, 1, 1, 10);
		r = Scheduler.solv(m);
		Assert.assertEquals(r.hpcStarts.get("a"), Arrays.asList(new Integer[] { 0 }), "result is " + r);
	}

	/**
	 * one app with 1 power and 1 benefit, 2 intervals ; problem with 1 power, but
	 * only one intervals. The best solution is to schedule the app on the one
	 * interval
	 */
	@Test
	public void testOneAppTwoInterval() {
		m.setPower(0, 1);
		m.setPower(1, 1);
		m.nbIntervals = 2;
		m.addHPC("a", 0, 3, 1, 1, 10);
		r = Scheduler.solv(m);
		Assert.assertEquals(r.hpcStarts.get("a"), Arrays.asList(new Integer[] { 0, 1 }), "result is " + r);
	}

	/**
	 * two apps with 2 and 3 power and benefit=power, 2 intervals ; problem with 3
	 * power, 3 intervals. The best solution is to schedule the 3-power first then
	 * the 2-power on one interval
	 */
	@Test
	public void testTwoApps() {
		m.getServer("server").maxPower = 3;
		m.nbIntervals = 3;
		m.addHPC("a", 0, 2, 2, 2, 10);
		m.addHPC("b", 0, 2, 3, 3, 10);
		r = Scheduler.solv(m);
		Assert.assertEquals(r.hpcStarts.get("a").size(), 1, "result is " + r);
		Assert.assertEquals(r.hpcStarts.get("b").size(), 2, "result is " + r);
	}

}

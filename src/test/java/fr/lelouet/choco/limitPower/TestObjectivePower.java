package fr.lelouet.choco.limitPower;

import static fr.lelouet.choco.limitpower.AppScheduler.solv;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import fr.lelouet.choco.limitpower.SchedulingModel;
import fr.lelouet.choco.limitpower.SchedulingModel.Objective;
import fr.lelouet.choco.limitpower.Result;

/**
 * test the solver with objective being profit+power use
 * 
 * @author Guillaume Le Louët
 *
 */
public class TestObjectivePower {

	Result r;
	SchedulingModel m;

	@BeforeMethod
	public void cleanup() {
		m = new SchedulingModel();
		m.objective = Objective.PROFIT_POWER;
	}

	/**
	 * two apps with 2 and 3 power and benefit=power, 2 intervals ; problem with 3
	 * power, 3 intervals. The best solution is to schedule the 3-power first then
	 * the 2-power on one interval
	 */
	@Test
	public void testTwoApps() {
		m.maxPower = 3;
		m.nbIntervals = 3;
		m.addHPC("a", 0, 2, 2, 2, 10);
		m.addHPC("b", 0, 2, 3, 3, 10);
		r = solv(m);
		Assert.assertEquals(r.hpcStarts.get("a"), Arrays.asList(new Integer[] { 2 }));
		Assert.assertEquals(r.hpcStarts.get("b"), Arrays.asList(0, 1));
	}

}

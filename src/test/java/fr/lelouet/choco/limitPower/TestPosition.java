package fr.lelouet.choco.limitPower;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import fr.lelouet.choco.limitpower.AppScheduler;
import fr.lelouet.choco.limitpower.SchedulingModel;
import fr.lelouet.choco.limitpower.SchedulingResult;

public class TestPosition {

	SchedulingResult r;
	SchedulingModel m;

	@BeforeMethod
	public void cleanup() {
		m = new SchedulingModel();
		m.server("s1").maxPower = 1000;
	}

	@Test
	// two intervals, one hpc app. the app should simply be put.
	public void testSimpleHPCApp() {
		m.nbIntervals = 2;
		m.addHPC("hpc", 0, 2, 1, 1, -1);
		r = AppScheduler.solv(m);
		Assert.assertEquals(r.profit, 1);
		Assert.assertEquals(r.appHosters.get("hpc"), Arrays.asList("s1", "s1"));
	}

	@Test
	// 3 intervals, one server with 2 power cap, one with 1 power cap.<br />
	// the HPC consumes 1 power.
	// the web has two modes, gain=power*2-1, power=0 or 1
	// one reduction by 1 power at the 2d interval (itv 1)
	public void testScheduling2apps() {
		m.nbIntervals = 3;
		m.setPower(3);
		m.setPower(1, 2);
		m.addHPC("hpc", 0, 2, 1, 4, -1);
		m.addWeb("web", 1, 1);
		m.addWeb("web", 2, 3);
		m.server("s0").maxPower=1;
		m.server("s1").maxPower=2;
		r = AppScheduler.solv(m);
		Assert.assertEquals(r.appHosters.get("web"), Arrays.asList("s1", "s1", "s1"), "" + r);
		Assert.assertEquals(r.appHosters.get("hpc"), Arrays.asList("s0", null, "s0"), "" + r);
	}

}

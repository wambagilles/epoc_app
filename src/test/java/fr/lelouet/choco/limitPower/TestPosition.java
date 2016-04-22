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
		m.server("s0").maxPower = 1;
		m.server("s1").maxPower = 2;
		r = AppScheduler.solv(m);
		Assert.assertEquals(r.appHosters.get("web"), Arrays.asList("s1", "s1", "s1"), "" + r);
		Assert.assertEquals(r.appHosters.get("hpc"), Arrays.asList("s0", null, "s0"), "" + r);
	}

	/**
	 * <p>
	 * this time 3 servers. s1 has 1 power, s2 2, s3 has 3 power. total power is
	 * 6 , but limited to 5 at interval 1 (the 2nd)
	 * </p>
	 * <p>
	 * 3 web apps, each with 3 modes. each consumes 1 pwr and gets one benefits
	 * at mode 1. On mode 2 each app consumes 2 power and 3, 4, 5 profit; On
	 * mode 3 each app consumes 3 power and 5,6,7 profit
	 * </p>
	 */
	@Test
	public void moreComplexWebProblem() {
		m.nbIntervals=3;
		m.server("s1").maxPower = 1;
		m.server("s2").maxPower = 2;
		m.server("s3").maxPower = 3;
		m.setPower(6);
		m.setPower(1, 5);

		m.addWeb("w0", 1, 1);
		m.addWeb("w1", 1, 1);
		m.addWeb("w2", 1, 1);

		m.addWeb("w0", 2, 3);
		m.addWeb("w1", 2, 4);
		m.addWeb("w2", 2, 5);

		m.addWeb("w0", 3, 5);
		m.addWeb("w1", 3, 6);
		m.addWeb("w2", 3, 7);

		r = AppScheduler.solv(m);
		//TODO
		System.out.println(""+r);
		
	}

}

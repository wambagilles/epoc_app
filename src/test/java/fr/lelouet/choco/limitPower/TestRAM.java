package fr.lelouet.choco.limitPower;

import org.testng.Assert;
import org.testng.annotations.Test;

import fr.lelouet.choco.limitpower.AppScheduler;
import fr.lelouet.choco.limitpower.SchedulingResult;
import fr.lelouet.choco.limitpower.model.SchedulingProblem;
import gnu.trove.map.hash.TObjectIntHashMap;

public class TestRAM {

	/**
	 * complex problem one 1-interval that should have only one best solution : w0 on s0, w1 and w2 on s1, no app on s0
	 */
	@Test
	public void testScheduling() {
		SchedulingProblem m = new SchedulingProblem();
		m.nbIntervals = 1;

		m.addWeb("w0", 1, 2);
		m.addWeb("w0", 2, 3);
		m.addWeb("w0", 6, 6);

		m.addWeb("w1", 1, 3);
		m.addWeb("w1", 2, 5);
		m.addWeb("w1", 6, 7);

		m.addWeb("w2", 1, 4);
		m.addWeb("w2", 2, 6);
		m.addWeb("w2", 6, 10);

		m.server("s0").maxPower = 1;
		m.server("s1").maxPower = 5;
		m.server("s2").maxPower = 10;

		TObjectIntHashMap<String> ram = new TObjectIntHashMap<>();
		ram.put("w0", 3);
		ram.put("w1", 3);
		ram.put("w2", 3);
		ram.put("s0", 10);
		ram.put("s1", 6);
		ram.put("s2", 3);
		m.setResource("ram", ram::get);

		SchedulingResult r = AppScheduler.solv(m);
		Assert.assertEquals(r.profit, 18);
		Assert.assertEquals(r.webModes.get("w0").get(0).profit, 3);
		Assert.assertEquals(r.webModes.get("w1").get(0).profit, 5);
		Assert.assertEquals(r.webModes.get("w2").get(0).profit, 10);
		Assert.assertEquals(r.appHosters.get("w0").get(0), "s1");
		Assert.assertEquals(r.appHosters.get("w1").get(0), "s1");
		Assert.assertEquals(r.appHosters.get("w2").get(0), "s2");

	}

}

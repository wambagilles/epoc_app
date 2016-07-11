package fr.emn.premode.center.parser.groovy;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Test;

import fr.emn.premode.center.HPC;
import fr.emn.premode.center.PowerMode;
import fr.emn.premode.center.SchedulingProblem;
import fr.emn.premode.center.parser.groovy.GroovyParser;

public class GroovyParserTest {

	@Test
	public void simpleTest() {
		GroovyParser test = new GroovyParser();
		SchedulingProblem m = test.getModel();

		test.parseLine("web[\"w0\"].add(2,3)");
		test.parseLine("web[\"w0\"].add(6,8)");
		Assert.assertEquals(m.getWebPowerModes("w0"), Arrays.asList(new PowerMode(2, 3), new PowerMode(6, 8)));

		test.parseLine("hpc[\"h3\"].power(2).start(1).duration(3).profit(4).deadline(6)");
		Assert.assertEquals(m.getHPC("h3"), new HPC(1, 3, 2, 4, 6));

		test.parseLine("power[2]=3;power[4]=1");
		Assert.assertEquals(m.getPower(2), 3);
		Assert.assertEquals(m.getPower(4), 1);
	}

}

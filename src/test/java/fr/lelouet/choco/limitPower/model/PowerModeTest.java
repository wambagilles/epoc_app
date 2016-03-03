package fr.lelouet.choco.limitPower.model;

import org.testng.Assert;
import org.testng.annotations.Test;

import fr.lelouet.choco.limitpower.model.PowerMode;

public class PowerModeTest {

	@Test
	public void testEquals() {
		int power = 10, profit=20;
		PowerMode base = new PowerMode(10, 20);
		for(int po=power-1;po<=power+1;po++)
			for(int pr=profit-1;pr<=profit+1;pr++)
				if(po==power && pr==profit) Assert.assertEquals(new PowerMode(po, pr), base);
				else Assert.assertNotEquals(new PowerMode(po, pr), base);
	}

}

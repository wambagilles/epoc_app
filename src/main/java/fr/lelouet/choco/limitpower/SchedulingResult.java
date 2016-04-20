package fr.lelouet.choco.limitpower;

import java.util.HashMap;
import java.util.List;

import fr.lelouet.choco.limitpower.model.PowerMode;

public class SchedulingResult {

	public SchedulingResult() {
	}

	public HashMap<String, List<PowerMode>> webModes = new HashMap<>();

	public HashMap<String, List<Integer>> hpcStarts = new HashMap<>();

	/** for each application name, at each intervale slot the application node */
	public HashMap<String, List<String>> appHosters = new HashMap<>();

	public int profit;

	@Override
	public String toString() {
		return "" + profit + webModes + hpcStarts + appHosters;
	}

}
